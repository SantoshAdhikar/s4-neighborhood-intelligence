package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.AnalysisRun;
import com.s4.neighborhood.entity.EvidenceReport;
import com.s4.neighborhood.entity.StorefrontMedia;
import com.s4.neighborhood.entity.StorefrontSegmentation;
import com.s4.neighborhood.repository.EvidenceReportRepository;
import com.s4.neighborhood.repository.StorefrontMediaRepository;
import com.s4.neighborhood.repository.StorefrontSegmentationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisModelEvidenceServiceTest {
    @Mock StorefrontMediaRepository media;
    @Mock StorefrontSegmentationRepository segmentations;
    @Mock EvidenceReportRepository reports;
    @InjectMocks AnalysisModelEvidenceService service;

    @Test
    void localModeNeverClaimsMetaVisualOrSegmentationRequests() {
        AnalysisRun run = run(false);
        var records = service.forRun(run);
        assertEquals("NOT_REQUESTED", records.get(0).status());
        assertEquals("NOT_REQUESTED", records.get(1).status());
        assertEquals("NOT_RECORDED", records.get(2).status());
        verifyNoInteractions(media, segmentations, reports);
    }

    @Test
    void distinguishesSavedVisualReuseFromNewMetaResult() {
        AnalysisRun run = run(true);
        UUID mediaId = UUID.randomUUID();
        run.setMediaId(mediaId);
        StorefrontMedia photo = photo(run, Instant.now().minusSeconds(7200));
        when(media.findByIdAndBusinessId(mediaId, run.getBusinessId())).thenReturn(Optional.of(photo));

        var previous = service.forRun(run);
        assertEquals("REUSED", previous.get(0).status());
        assertEquals("resp-visual", previous.get(0).responseId());
        assertEquals("NOT_REQUESTED", previous.get(1).status());

        photo.setAnalyzedAt(run.getCreatedAt().plusSeconds(1));
        var current = service.forRun(run);
        assertEquals("COMPLETED", current.get(0).status());
    }

    @Test
    void doesNotAttributeAnImageProcessedAfterTheRunFinished() {
        AnalysisRun run = run(true);
        run.setCompletedAt(run.getCreatedAt().plusSeconds(15));
        UUID mediaId = UUID.randomUUID();
        run.setMediaId(mediaId);
        StorefrontMedia photo = photo(run, run.getCreatedAt().plusSeconds(30));
        when(media.findByIdAndBusinessId(mediaId, run.getBusinessId())).thenReturn(Optional.of(photo));

        var recorded = service.forRun(run).get(0);
        assertEquals("UNVERIFIED", recorded.status());
        assertNull(recorded.responseId());
    }

    @Test
    void metaReportRequiresSavedProviderModelAndResponseId() {
        AnalysisRun run = run(true);
        UUID reportId = UUID.randomUUID();
        run.setReportId(reportId);
        EvidenceReport record = new EvidenceReport();
        record.setBusinessId(run.getBusinessId());
        record.setStatus("COMPLETED");
        record.setModelName("muse-spark-1.3");
        record.setCompletedAt(run.getCreatedAt().plusSeconds(3));
        when(reports.findById(reportId)).thenReturn(Optional.of(record));

        assertEquals("UNVERIFIED", service.forRun(run).get(2).status());

        record.setModelResponseId("resp-report");
        assertEquals("COMPLETED", service.forRun(run).get(2).status());
    }

    @Test
    void samEvidenceRequiresTheSelectedImageAndSavedSignResult() {
        AnalysisRun run = run(true);
        UUID mediaId = UUID.randomUUID();
        run.setMediaId(mediaId);
        run.setSegmentSign(true);
        StorefrontMedia photo = photo(run, run.getCreatedAt().plusSeconds(1));
        when(media.findByIdAndBusinessId(mediaId, run.getBusinessId())).thenReturn(Optional.of(photo));
        StorefrontSegmentation segmentation = new StorefrontSegmentation();
        segmentation.setStatus("COMPLETED");
        segmentation.setModelName("sam-3.1");
        segmentation.setModelResponseId("resp-sam");
        segmentation.setCompletedAt(run.getCreatedAt().plusSeconds(2));
        when(segmentations.findByMediaIdAndConcept(mediaId, "storefront sign"))
                .thenReturn(Optional.of(segmentation));

        assertEquals("COMPLETED", service.forRun(run).get(1).status());
        verify(segmentations).findByMediaIdAndConcept(mediaId, "storefront sign");
    }

    private static AnalysisRun run(boolean useMeta) {
        AnalysisRun run = new AnalysisRun();
        run.setBusinessId(UUID.randomUUID());
        run.setGoal("Improve local discovery");
        run.setStatus("COMPLETED");
        run.setUseMeta(useMeta);
        return run;
    }

    private static StorefrontMedia photo(AnalysisRun run, Instant time) {
        StorefrontMedia photo = new StorefrontMedia();
        photo.setBusinessId(run.getBusinessId());
        photo.setMimeType("image/png");
        photo.setSizeBytes(100000);
        photo.setStatus("ANALYZED");
        photo.setModelName("muse-spark-1.3");
        photo.setModelResponseId("resp-visual");
        photo.setAnalyzedAt(time);
        return photo;
    }
}
