package com.s4.neighborhood.security;

import com.s4.neighborhood.controller.AnalysisRunController;
import com.s4.neighborhood.controller.BusinessProfileController;
import com.s4.neighborhood.controller.BusinessClaimController;
import com.s4.neighborhood.controller.EvidenceReportController;
import com.s4.neighborhood.controller.NeighborhoodController;
import com.s4.neighborhood.controller.StorefrontMediaController;
import com.s4.neighborhood.controller.StorefrontSegmentationController;
import com.s4.neighborhood.service.AccountService;
import com.s4.neighborhood.service.AnalysisRunService;
import com.s4.neighborhood.service.BusinessProfileService;
import com.s4.neighborhood.service.BusinessClaimService;
import com.s4.neighborhood.service.EvidenceReportService;
import com.s4.neighborhood.service.NeighborhoodResearchService;
import com.s4.neighborhood.service.StorefrontMediaService;
import com.s4.neighborhood.service.StorefrontSegmentationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProtectedBusinessEndpointsTest {
    @Mock BusinessAccessGuard access;
    @Mock AccountService accounts;
    @Mock BusinessProfileService businesses;
    @Mock BusinessClaimService claims;
    @Mock NeighborhoodResearchService neighborhood;
    @Mock StorefrontMediaService media;
    @Mock StorefrontSegmentationService segmentation;
    @Mock EvidenceReportService reports;
    @Mock AnalysisRunService runs;

    @Test
    void rejectsCrossAccountBusinessAndNeighborhoodRequestsBeforeServicesRun() {
        UUID id = deniedBusiness();
        BusinessProfileController profiles = new BusinessProfileController(businesses, accounts, access);
        NeighborhoodController explorer = new NeighborhoodController(neighborhood, access);

        assertMissing(() -> profiles.get(id));
        assertMissing(() -> profiles.update(id, null));
        assertMissing(() -> explorer.research(id));
        verifyNoInteractions(businesses, neighborhood);
    }

    @Test
    void rejectsCrossAccountMediaAndSegmentationRequestsBeforeServicesRun() {
        UUID id = deniedBusiness();
        UUID mediaId = UUID.randomUUID();
        StorefrontMediaController storefront = new StorefrontMediaController(media, access);
        StorefrontSegmentationController masks = new StorefrontSegmentationController(segmentation, access);

        assertMissing(() -> storefront.upload(id, null));
        assertMissing(() -> storefront.list(id));
        assertMissing(() -> storefront.media(id, mediaId));
        assertMissing(() -> storefront.analyze(id, mediaId));
        assertMissing(() -> masks.list(id, mediaId));
        assertMissing(() -> masks.segment(id, mediaId, null));
        verifyNoInteractions(media, segmentation);
    }

    @Test
    void rejectsCrossAccountReportAndAnalysisRequestsBeforeServicesRun() {
        UUID id = deniedBusiness();
        UUID reportId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        EvidenceReportController evidence = new EvidenceReportController(access, reports);
        AnalysisRunController analysis = new AnalysisRunController(access, runs);

        assertMissing(() -> evidence.latest(id));
        assertMissing(() -> evidence.read(id, reportId));
        assertMissing(() -> evidence.generate(id));
        assertMissing(() -> analysis.start(id, null));
        assertMissing(() -> analysis.latest(id));
        assertMissing(() -> analysis.get(id, runId));
        verifyNoInteractions(reports, runs);
    }

    @Test
    void rejectsCrossAccountOwnershipClaimsBeforeServicesRun() {
        UUID id = deniedBusiness();
        BusinessClaimController controller = new BusinessClaimController(claims, access, accounts);
        assertMissing(() -> controller.status(id));
        assertMissing(() -> controller.request(id));
        verifyNoInteractions(claims);
    }

    private UUID deniedBusiness() {
        UUID businessId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"))
                .when(access).requireOwner(businessId);
        return businessId;
    }

    private static void assertMissing(ThrowingAction action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::call);
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @FunctionalInterface
    private interface ThrowingAction { void call() throws Exception; }
}
