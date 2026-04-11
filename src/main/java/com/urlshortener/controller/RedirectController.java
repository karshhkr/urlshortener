package com.urlshortener.controller;

import com.urlshortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    private static final Set<String> RESERVED = Set.of(
            "index.html", "favicon.ico", "actuator",
            "api", "static", "error", "webjars"
    );

    @GetMapping("/{shortCode:[a-zA-Z0-9]{4,10}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {

        if (RESERVED.contains(shortCode.toLowerCase())) {
            return ResponseEntity.notFound().build();
        }

        String originalUrl = urlShortenerService.getOriginalUrl(shortCode);
        urlShortenerService.incrementClickCount(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}