package com.s4.neighborhood.client;

import com.s4.neighborhood.dto.EvidenceReportResponse.Source;

import java.util.List;
import java.util.Map;

final class EvidenceCitationSchema {
    private EvidenceCitationSchema() {}

    static Map<String, Object> citationArray(List<Source> catalog, boolean requireAtLeastOne) {
        List<String> ids = catalog.stream().map(Source::id).distinct().toList();
        if (ids.isEmpty()) throw new IllegalStateException("No evidence sources available for a cited report");
        Map<String, Object> items = Map.of("type", "string", "enum", ids);
        return requireAtLeastOne
                ? Map.of("type", "array", "items", items, "minItems", 1)
                : Map.of("type", "array", "items", items);
    }
}
