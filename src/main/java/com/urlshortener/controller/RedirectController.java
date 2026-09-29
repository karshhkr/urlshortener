package com.urlshortener.controller;

import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.service.ClickTrackingService;
import com.urlshortener.service.UrlShortenerService;
import com.urlshortener.service.UrlShortenerService.CachedTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ClickTrackingService clickTrackingService;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{4,10}}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = HttpHeaders.REFERER, required = false) String referrer) {

        if (UrlShortenerService.RESERVED.contains(shortCode.toLowerCase())) {
            return ResponseEntity.notFound().build();
        }

        CachedTarget target = urlShortenerService.getTarget(shortCode);

        // Must be checked here: on a cache hit getTarget() does not run its own expiry check.
        if (target.isExpired()) {
            throw new UrlNotFoundException("Short URL has expired: " + shortCode);
        }

        // Fire-and-forget: does not block the redirect.
        clickTrackingService.record(shortCode, userAgent, referrer);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, target.originalUrl());
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}