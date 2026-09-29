package com.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenRequest {

    // Scheme/host checks happen in UrlShortenerService (java.net.URI is more reliable than a regex).
    @NotBlank(message = "URL must not be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    // Must match the redirect route: 4-10 letters/digits. Empty means "generate one for me".
    @Pattern(
            regexp = "^$|^[a-zA-Z0-9]{4,10}$",
            message = "Custom alias must be 4-10 letters or digits"
    )
    private String customAlias;

    @Min(value = 1, message = "Expiry must be at least 1 day")
    @Max(value = 3650, message = "Expiry must not exceed 3650 days")
    private Integer expiryDays;
}