package com.urlshortener.service;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClickTrackingService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    /**
     * Runs on the "clickExecutor" thread pool, so the redirect response never waits for
     * these two DB writes. Called from another bean (RedirectController), so the @Async
     * proxy is applied.
     */
    @Async("clickExecutor")
    @Transactional
    public void record(String shortCode, String userAgent, String referrer) {
        urlMappingRepository.incrementClickCount(shortCode);
        clickEventRepository.save(ClickEvent.builder()
                .shortCode(shortCode)
                .userAgent(truncate(userAgent, 255))
                .referrer(truncate(referrer, 512))
                .build());
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}