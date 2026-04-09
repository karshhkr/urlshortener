package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.UrlStatsResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final Base62Service base62Service;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length:6}")
    private int shortCodeLength;

    @Value("${app.default-expiry-days:365}")
    private int defaultExpiryDays;

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String shortCode = resolveShortCode(request);

        LocalDateTime expiresAt = request.getExpiryDays() != null
                ? LocalDateTime.now().plusDays(request.getExpiryDays())
                : LocalDateTime.now().plusDays(defaultExpiryDays);

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        urlMappingRepository.save(mapping);
        log.info("Created short URL: {} -> {}", shortCode, request.getUrl());

        return ShortenResponse.builder()
                .shortCode(shortCode)
                .shortUrl(baseUrl + "/" + shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(expiresAt)
                .createdAt(mapping.getCreatedAt())
                .build();
    }

    @Cacheable(value = "urlMappings", key = "#shortCode")
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (mapping.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired: " + shortCode);
        }

        return mapping.getOriginalUrl();
    }

    @Transactional
    public void incrementClickCount(String shortCode) {
        urlMappingRepository.incrementClickCount(shortCode);
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        return UrlStatsResponse.builder()
                .shortCode(mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .clickCount(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .isActive(mapping.getIsActive())
                .build();
    }

    @CacheEvict(value = "urlMappings", key = "#shortCode")
    @Transactional
    public void deactivate(String shortCode) {
        UrlMapping mapping = urlMappingRepository
                .findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));
        mapping.setIsActive(false);
        urlMappingRepository.save(mapping);
        log.info("Deactivated short URL: {}", shortCode);
    }

    @Scheduled(cron = "0 0 2 * * *") // runs at 2 AM daily
    @Transactional
    public void cleanupExpiredUrls() {
        int count = urlMappingRepository.deactivateExpiredUrls(LocalDateTime.now());
        log.info("Deactivated {} expired URLs", count);
    }

    private String resolveShortCode(ShortenRequest request) {
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (urlMappingRepository.existsByShortCode(request.getCustomAlias())) {
                throw new InvalidUrlException("Custom alias already taken: " + request.getCustomAlias());
            }
            return request.getCustomAlias();
        }
        return generateUniqueCode();
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new RuntimeException("Failed to generate unique short code after 10 attempts");
            }
            long randomValue = Math.abs(new Random().nextLong() % (long) Math.pow(62, shortCodeLength));
            code = base62Service.encode(randomValue);
            // Pad to desired length
            while (code.length() < shortCodeLength) code = "a" + code;
            if (code.length() > shortCodeLength) code = code.substring(0, shortCodeLength);
        } while (urlMappingRepository.existsByShortCode(code));
        return code;
    }
}