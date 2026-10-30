package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse.EvidenceCoverage;
import com.s4.neighborhood.dto.EvidenceReportResponse.Opportunity;
import com.s4.neighborhood.dto.EvidenceReportResponse.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Labels the kinds of evidence cited. Coverage is not a probability that advice is correct. */
public final class EvidenceCoverageAssessor {
    private EvidenceCoverageAssessor() {}

    public static List<EvidenceCoverage> assess(List<Opportunity> opportunities, List<Source> sources) {
        Map<String, Source> catalog = new HashMap<>();
        for (Source source : sources) catalog.put(source.id(), source);
        return opportunities.stream().map(item -> assess(item, catalog)).toList();
    }

    private static EvidenceCoverage assess(Opportunity opportunity, Map<String, Source> catalog) {
        int observed = 0;
        int contextual = 0;
        int owner = 0;
        Set<String> uniqueIds = new HashSet<>(opportunity.evidenceIds());
        for (String id : uniqueIds) {
            Source source = catalog.get(id);
            if (source == null) continue;
            switch (source.kind()) {
                case "STORE_FRONT_IMAGE", "WEBSITE_PAGE", "OWNER_WEBSITE", "NEARBY_COMPETITOR" -> observed++;
                case "OSM_PLACE", "SAM_SEGMENTATION" -> contextual++;
                case "OWNER_PROFILE", "OWNER_FEEDBACK" -> owner++;
                default -> contextual++; // Unknown source categories aren't independently verified.
            }
        }
        if (observed > 0) {
            return new EvidenceCoverage(opportunity.rank(), "OBSERVED", "Observed sources included",
                    "Includes sampled website content or saved visual-model observations. These sources still do not verify the recommendation's outcome.",
                    observed, contextual, owner);
        }
        if (contextual > 0) {
            return new EvidenceCoverage(opportunity.rank(), "CONTEXT", "Contextual evidence only",
                    "Nearby listings and candidate segmentation masks provide context, not proof of specific competitor services or business results.",
                    observed, contextual, owner);
        }
        return new EvidenceCoverage(opportunity.rank(), "OWNER_ONLY", "Owner input only",
                "Based on owner-provided details or feedback, without independent observational evidence. Verify before making changes.",
                observed, contextual, owner);
    }
}
