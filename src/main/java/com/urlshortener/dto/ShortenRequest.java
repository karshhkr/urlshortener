package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL must not be blank")
    @Pattern(
            regexp = "^(https?://)([\\w\\-]+\\.)+[\\w]{2,}(/.*)?$",
            message = "Please provide a valid HTTP/HTTPS URL"
    )
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    @Size(max = 10, message = "Custom alias must not exceed 10 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]*$",
            message = "Custom alias can only contain letters, numbers, hyphens, underscores"
    )
    private String customAlias;

    private Integer expiryDays;
}