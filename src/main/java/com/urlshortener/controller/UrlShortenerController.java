package com.urlshortener.controller;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.UrlStatsResponse;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    /** Public. If a valid JWT is sent, the URL is owned by that user; otherwise it is anonymous. */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @Valid @RequestBody ShortenRequest request,
            @AuthenticationPrincipal UserDetails user) {
        String username = user != null ? user.getUsername() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urlShortenerService.shorten(request, username));
    }

    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlShortenerService.getStats(shortCode));
    }

    /** "My URLs": requires login. */
    @GetMapping("/urls")
    public ResponseEntity<List<UrlStatsResponse>> myUrls(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(urlShortenerService.listMine(user.getUsername()));
    }

    /** Requires login AND ownership. */
    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<Void> deactivate(
            @PathVariable String shortCode,
            @AuthenticationPrincipal UserDetails user) {
        urlShortenerService.deactivate(shortCode, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}