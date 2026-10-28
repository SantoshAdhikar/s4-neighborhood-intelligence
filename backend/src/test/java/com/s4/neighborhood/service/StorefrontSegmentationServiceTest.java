package com.s4.neighborhood.service;

import com.s4.neighborhood.client.MetaSegmentationClient;
import com.s4.neighborhood.entity.StorefrontSegmentation;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorefrontSegmentationServiceTest {
    private final StorefrontMediaService media = mock(StorefrontMediaService.class);
    private final StorefrontSegmentationRepository repository = mock(StorefrontSegmentationRepository.class);
    private final MetaSegmentationClient meta = mock(MetaSegmentationClient.class);
    private final StorefrontSegmentationService service = new StorefrontSegmentationService(
            media, repository, meta, 8_388_608, 2);

    @Test
    void rejectsInstructionLikePromptBeforeAnyApiCall() {
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.segment(UUID.randomUUID(), UUID.randomUUID(), "Find windows; then scan customers"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(media, repository, meta);
    }

    @Test
    void videoTrackingIsNotSilentlyAttemptedAsImage() {
        UUID businessId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        when(media.file(businessId, mediaId))
                .thenReturn(new StorefrontMediaService.MediaFile(Path.of("clip.mp4"), "video/mp4"));
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.segment(businessId, mediaId, "storefront sign"));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
        verifyNoInteractions(repository, meta);
    }

    @Test
    void completedRequestUsesDatabaseWithoutChargingMetaAgain() throws Exception {
        UUID businessId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        Path image = java.nio.file.Files.createTempFile("s4-sam-test", ".png");
        try {
            var stored = new StorefrontSegmentation();
            stored.setMediaId(mediaId);
            stored.setConcept("storefront sign");
            stored.setStatus("COMPLETED");
            stored.setRawOutput("");
            stored.setModelName("sam-3.1");
            when(media.file(businessId, mediaId))
                    .thenReturn(new StorefrontMediaService.MediaFile(image, "image/png"));
            when(repository.findByMediaIdAndConcept(mediaId, "storefront sign"))
                    .thenReturn(Optional.of(stored));

            var result = service.segment(businessId, mediaId, "  STOREFRONT   SIGN  ");

            assertEquals("COMPLETED", result.status());
            verify(repository, never()).claim(any(), anyInt());
            verifyNoInteractions(meta);
        } finally {
            java.nio.file.Files.deleteIfExists(image);
        }
    }
}
