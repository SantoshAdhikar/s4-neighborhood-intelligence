package com.s4.neighborhood.client;

import com.s4.neighborhood.dto.EvidenceReportResponse.Source;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceCitationSchemaTest {
    @Test
    void schemaOnlyPermitsIdsFromTheCurrentCatalog() {
        List<Source> catalog = List.of(
                new Source("profile-goal", "OWNER_PROFILE", "Goal", "/profile", null),
                new Source("page-42", "WEBSITE_PAGE", "Booking page", "https://example.com/book", null),
                new Source("page-42", "WEBSITE_PAGE", "Duplicate", "https://example.com/book", null));
        Map<String, Object> field = EvidenceCitationSchema.citationArray(catalog, true);
        assertEquals(1, field.get("minItems"));
        Map<?, ?> item = (Map<?, ?>) field.get("items");
        assertEquals(List.of("profile-goal", "page-42"), item.get("enum"));
        assertFalse(((List<?>) item.get("enum")).contains("invented-source"));
    }

    @Test
    void metaSchemaOmitsUnsupportedMinimumAndStillRestrictsIds() {
        Map<String, Object> field = EvidenceCitationSchema.citationArray(List.of(
                new Source("visual-1", "STORE_FRONT_IMAGE", "Sign", "/storefront", null)), false);
        assertFalse(field.containsKey("minItems"));
        assertEquals(List.of("visual-1"), ((Map<?, ?>) field.get("items")).get("enum"));
    }

    @Test
    void emptyCatalogCannotGenerateCitedRecommendations() {
        assertThrows(IllegalStateException.class,
                () -> EvidenceCitationSchema.citationArray(List.of(), true));
    }
}
