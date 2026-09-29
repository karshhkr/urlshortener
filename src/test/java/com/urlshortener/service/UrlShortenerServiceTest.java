package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlRepo;

    @Mock
    private ClickEventRepository eventRepo;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(urlRepo, eventRepo);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "shortCodeLength", 6);
        ReflectionTestUtils.setField(service, "defaultExpiryDays", 365);
    }

    private ShortenRequest request(String url, String alias) {
        ShortenRequest r = new ShortenRequest();
        r.setUrl(url);
        r.setCustomAlias(alias);
        return r;
    }

    @Test
    void shorten_rejectsNonHttpScheme() {
        assertThrows(InvalidUrlException.class,
                () -> service.shorten(request("javascript:alert(1)", null), null));
        verify(urlRepo, never()).save(any());
    }

    @Test
    void shorten_rejectsReservedAlias() {
        assertThrows(InvalidUrlException.class,
                () -> service.shorten(request("https://example.com", "admin"), null));
        verify(urlRepo, never()).save(any());
    }

    @Test
    void shorten_rejectsAliasAlreadyTaken() {
        when(urlRepo.existsByShortCode("mylink")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.shorten(request("https://example.com", "mylink"), null));
        verify(urlRepo, never()).save(any());
    }

    @Test
    void shorten_storesOwnerAndGeneratesCodeOfConfiguredLength() {
        when(urlRepo.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepo.save(any(UrlMapping.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse response = service.shorten(request("https://example.com/page", null), "john");

        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(urlRepo).save(captor.capture());
        assertEquals("john", captor.getValue().getCreatedBy());
        assertEquals(6, response.getShortCode().length());
        assertEquals("http://localhost:8080/" + response.getShortCode(), response.getShortUrl());
    }

    @Test
    void deactivate_byNonOwner_isForbidden() {
        UrlMapping mapping = UrlMapping.builder().shortCode("abcd12").createdBy("alice").build();
        when(urlRepo.findByShortCode("abcd12")).thenReturn(Optional.of(mapping));

        assertThrows(AccessDeniedException.class, () -> service.deactivate("abcd12", "bob"));
        assertTrue(mapping.getIsActive());
        verify(urlRepo, never()).save(any());
    }

    @Test
    void deactivate_ofAnonymousUrl_isForbidden() {
        UrlMapping mapping = UrlMapping.builder().shortCode("abcd12").createdBy(null).build();
        when(urlRepo.findByShortCode("abcd12")).thenReturn(Optional.of(mapping));

        assertThrows(AccessDeniedException.class, () -> service.deactivate("abcd12", "bob"));
    }

    @Test
    void deactivate_byOwner_deactivates() {
        UrlMapping mapping = UrlMapping.builder().shortCode("abcd12").createdBy("alice").build();
        when(urlRepo.findByShortCode("abcd12")).thenReturn(Optional.of(mapping));

        service.deactivate("abcd12", "alice");

        assertFalse(mapping.getIsActive());
        verify(urlRepo).save(mapping);
    }

    @Test
    void cachedTarget_reportsExpiry() {
        assertTrue(new UrlShortenerService.CachedTarget("https://a.com", LocalDateTime.now().minusMinutes(1)).isExpired());
        assertFalse(new UrlShortenerService.CachedTarget("https://a.com", LocalDateTime.now().plusDays(1)).isExpired());
        assertFalse(new UrlShortenerService.CachedTarget("https://a.com", null).isExpired());
    }
}