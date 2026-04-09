package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {
    private String shortUrl;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}