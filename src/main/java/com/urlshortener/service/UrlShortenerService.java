package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.UrlStatsResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    /** Words that must never become short codes because they collide with real routes. */
    public static final Set<String> RESERVED = Set.of(
            "api", "actuator", "static", "error", "health", "index",
            "favicon", "webjars", "admin", "login", "register", "stats");

    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length:6}")
    private int shortCodeLength;

    @Value("${app.default-expiry-days:365}")
    private int defaultExpiryDays;

    /** What we keep in the cache: enough to redirect AND to re-check expiry on every hit. */
    public record CachedTarget(String originalUrl, LocalDateTime expiresAt) {
        public boolean isExpired() {
            return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
        }
    }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request, String username) {
        validateTargetUrl(request.getUrl());
        String shortCode = resolveShortCode(request);

        int days = request.getExpiryDays() != null ? request.getExpiryDays() : defaultExpiryDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(days);

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(expiresAt)
                .createdBy(username)
                .isActive(true)
                .build();

        urlMappingRepository.save(mapping);
        log.info("Created short URL: {} -> {} (owner: {})", shortCode, request.getUrl(), username);

        return ShortenResponse.builder()
                .shortCode(shortCode)
                .shortUrl(baseUrl + "/" + shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(expiresAt)
                .createdAt(mapping.getCreatedAt())
                .build();
    }

    /**
     * Cached lookup. The controller must check {@link CachedTarget#isExpired()} after calling
     * this, because a cache hit skips the method body. (Checking inside this class would not
     * work: calls within the same class bypass the Spring cache proxy.)
     */
    @Cacheable(value = "urlMappings", key = "#shortCode")
    @Transactional(readOnly = true)
    public CachedTarget getTarget(String shortCode) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (mapping.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired: " + shortCode);
        }
        return new CachedTarget(mapping.getOriginalUrl(), mapping.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        LocalDate today = LocalDate.now();
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            byDay.put(today.minusDays(i).toString(), 0L);
        }
        clickEventRepository.countByDay(shortCode, today.minusDays(6).atStartOfDay())
                .forEach(row -> byDay.put(row.getClickDay(), row.getClicks()));

        return toStats(mapping, byDay);
    }

    @Transactional(readOnly = true)
    public List<UrlStatsResponse> listMine(String username) {
        return urlMappingRepository.findByCreatedByOrderByCreatedAtDesc(username)
                .stream()
                .map(m -> toStats(m, null))
                .toList();
    }

    @CacheEvict(value = "urlMappings", key = "#shortCode")
    @Transactional
    public void deactivate(String shortCode, String username) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (mapping.getCreatedBy() == null || !mapping.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You can only deactivate your own URLs");
        }
        mapping.setIsActive(false);
        urlMappingRepository.save(mapping);
        log.info("Deactivated short URL: {} by {}", shortCode, username);
    }

    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @CacheEvict(value = "urlMappings", allEntries = true)
    @Transactional
    public void cleanupExpiredUrls() {
        int count = urlMappingRepository.deactivateExpiredUrls(LocalDateTime.now());
        log.info("Deactivated {} expired URLs", count);
    }

    // ---------------------------------------------------------------- helpers

    private void validateTargetUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("Only http and https URLs are allowed");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("URL must contain a valid host");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL");
        }
    }

    private String resolveShortCode(ShortenRequest request) {
        String alias = request.getCustomAlias();
        if (alias != null && !alias.isBlank()) {
            if (RESERVED.contains(alias.toLowerCase())) {
                throw new InvalidUrlException("This alias is reserved, please choose another");
            }
            if (urlMappingRepository.existsByShortCode(alias)) {
                throw new ConflictException("Custom alias already taken: " + alias);
            }
            return alias;
        }
        return generateUniqueCode();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(shortCodeLength);
            for (int i = 0; i < shortCodeLength; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            if (!RESERVED.contains(code.toLowerCase()) && !urlMappingRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after 10 attempts");
    }

    private UrlStatsResponse toStats(UrlMapping mapping, Map<String, Long> clicksByDay) {
        return UrlStatsResponse.builder()
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .clickCount(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .isActive(mapping.getIsActive())
                .clicksByDay(clicksByDay)
                .build();
    }
}