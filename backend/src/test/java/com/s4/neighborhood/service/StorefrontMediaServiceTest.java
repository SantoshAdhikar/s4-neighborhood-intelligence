package com.s4.neighborhood.service;

import com.s4.neighborhood.client.MetaVisionClient;
import com.s4.neighborhood.dto.VisualFindings;
import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.repository.BusinessProfileRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StorefrontMediaServiceTest {
    @TempDir Path mediaRoot;

    private final BusinessProfileRepository businesses = mock(BusinessProfileRepository.class);
    private final StorefrontMediaRepository media = mock(StorefrontMediaRepository.class);
    private final MetaVisionClient meta = mock(MetaVisionClient.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private StorefrontMediaService service;
    private UUID businessId;
    private BusinessProfile business;

    @BeforeEach
    void setUp() {
        service = new StorefrontMediaService(businesses, media, meta, json,
                mediaRoot.toString(), 5_000_000, 2);
        businessId = UUID.randomUUID();
        business = new BusinessProfile();
        business.setName("Example Store");
        business.setIndustry("Retail");
        business.setServices("Retail goods");
        business.setPrimaryGoal("Improve visibility");
    }

    @Test
    void rejectsFilesWithoutSupportedMagicHeader() {
        when(businesses.findById(businessId)).thenReturn(Optional.of(business));
        MockMultipartFile file = new MockMultipartFile("file", "fake.png",
                "image/png", "not really an image".getBytes());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.upload(businessId, file));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
        verifyNoInteractions(media);
    }

    @Test
    void uploadsAndHashesValidPng() throws Exception {
        when(businesses.findById(businessId)).thenReturn(Optional.of(business));
        when(media.saveAndFlush(any(StorefrontMedia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        byte[] bytes = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "window.png", "image/png", bytes);

        var response = service.upload(businessId, file);

        assertEquals("UPLOADED", response.status());
        assertEquals("image/png", response.mimeType());
        assertEquals(64, response.sha256().length());
        try (var saved = Files.list(mediaRoot)) {
            assertEquals(1, saved.count());
        }
        verifyNoInteractions(meta);
    }

    @Test
    void completedAnalysisIsNotChargedTwice() {
        UUID mediaId = UUID.randomUUID();
        StorefrontMedia saved = new StorefrontMedia();
        saved.setStatus("ANALYZED");
        saved.setBusinessId(businessId);
        saved.setAnalysisJson("{\"observations\":[],\"limitations\":[]}");
        when(businesses.findById(businessId)).thenReturn(Optional.of(business));
        when(media.findByIdAndBusinessId(mediaId, businessId)).thenReturn(Optional.of(saved));

        assertEquals("ANALYZED", service.analyze(businessId, mediaId).status());
        verify(media, never()).claimAnalysis(any(), any(), anyInt());
        verifyNoInteractions(meta);
    }

    @Test
    void successfulMetaCallPersistsRealStructuredFindings() throws Exception {
        UUID mediaId = UUID.randomUUID();
        StorefrontMedia saved = new StorefrontMedia();
        saved.setBusinessId(businessId);
        saved.setStorageKey("media.png");
        saved.setMimeType("image/png");
        Files.write(mediaRoot.resolve("media.png"), new byte[]{1, 2, 3});

        when(businesses.findById(businessId)).thenReturn(Optional.of(business));
        when(media.findByIdAndBusinessId(mediaId, businessId)).thenReturn(Optional.of(saved));
        when(media.claimAnalysis(mediaId, businessId, 2)).thenReturn(1);
        when(media.saveAndFlush(saved)).thenReturn(saved);
        var findings = new VisualFindings(List.of(new VisualFindings.Observation(
                "sign", "A sign is visible", "image", "medium")), List.of());
        String responseJson = "{\"observations\":[{\"aspect\":\"sign\"," +
                "\"visibleEvidence\":\"A sign is visible\",\"videoTimestamp\":\"image\"," +
                "\"certainty\":\"medium\"}],\"limitations\":[]}";
        when(meta.analyze(any(Path.class), eq("image/png"), eq(business)))
                .thenReturn(new MetaVisionClient.Analysis("muse-spark-1.3", "resp-1", responseJson, findings));

        var response = service.analyze(businessId, mediaId);

        assertEquals("ANALYZED", response.status());
        assertEquals("resp-1", response.modelResponseId());
        assertEquals("A sign is visible", response.findings().observations().getFirst().visibleEvidence());
        verify(meta, times(1)).analyze(any(Path.class), eq("image/png"), eq(business));
    }
}
