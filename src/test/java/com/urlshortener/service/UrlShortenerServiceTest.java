package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private Base62Service base62Service;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @Test
    void shorten_withValidUrl_returnsShortCode() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(urlShortenerService, "shortCodeLength", 6);
        ReflectionTestUtils.setField(urlShortenerService, "defaultExpiryDays", 365);

        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://www.example.com/very/long/url");

        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
        when(base62Service.encode(anyLong())).thenReturn("abc123");
        when(urlMappingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ShortenResponse response = urlShortenerService.shorten(request);

        assertThat(response.getShortUrl()).startsWith("http://localhost:8080/");
        assertThat(response.getOriginalUrl()).isEqualTo("https://www.example.com/very/long/url");
        verify(urlMappingRepository).save(any(UrlMapping.class));
    }

    @Test
    void getOriginalUrl_withInvalidCode_throwsNotFoundException() {
        when(urlMappingRepository.findByShortCodeAndIsActiveTrue("invalid"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlShortenerService.getOriginalUrl("invalid"))
                .isInstanceOf(UrlNotFoundException.class);
    }
}