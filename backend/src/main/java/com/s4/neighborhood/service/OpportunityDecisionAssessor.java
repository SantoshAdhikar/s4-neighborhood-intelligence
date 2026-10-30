package com.s4.neighborhood.service;

import com.s4.neighborhood.dto.EvidenceReportResponse.*;
import java.util.*;
import java.util.stream.Collectors;

/** A transparent, deterministic sorting aid; does not estimate revenue or model confidence. */
public final class OpportunityDecisionAssessor {
    private OpportunityDecisionAssessor() {}

    public static List<ActionReadiness> assess(List<Opportunity> opportunities, List<Source> sources,
                                               List<EvidenceCoverage> coverages) {
        if (opportunities == null || sources == null || coverages == null) return List.of();
        Map<Integer, EvidenceCoverage> byRank = coverages.stream()
                .collect(Collectors.toMap(EvidenceCoverage::rank, entry -> entry, (first, ignored) -> first));
        Set<String> goalWords = sources.stream()
                .filter(s -> "profile-goal".equals(s.id()) || "research-focus".equals(s.id()))
                .flatMap(s -> significantWords(s.description()).stream()).collect(Collectors.toSet());
        List<Evaluated> evaluated = new ArrayList<>();
        for (Opportunity item : opportunities) {
            EvidenceCoverage coverage = byRank.get(item.rank());
            if (coverage == null) continue;
            boolean observed = coverage.observedSources() > 0;
            boolean complex = "high".equals(item.effort());
            String readiness = !observed ? "VERIFY_FIRST" : complex ? "PLAN_FIRST" : "READY_TO_TEST";
            long goalMatches = significantWords(item.title() + " " + item.rationale()).stream()
                    .filter(goalWords::contains).distinct().limit(3).count();
            int band = "READY_TO_TEST".equals(readiness) ? 0 : "PLAN_FIRST".equals(readiness) ? 1 : 2;
            int sourceStrength = Math.min(coverage.observedSources(), 3) * 3
                    + Math.min(coverage.contextualSources(), 2);
            int effort = "low".equals(item.effort()) ? 0 : "medium".equals(item.effort()) ? 1 : 2;
            String reason = switch (readiness) {
                case "READY_TO_TEST" -> "Cites observed source material and has manageable stated effort. Test the action and track its measurement; results are not guaranteed.";
                case "PLAN_FIRST" -> "Cites observed source material, but high stated effort calls for scope and resource review before implementation.";
                default -> "No observed-source citation is attached. Verify the underlying claim before using this as a business decision.";
            };
            evaluated.add(new Evaluated(item.rank(), readiness, reason, (int) goalMatches,
                    coverage.observedSources(), coverage.contextualSources(), coverage.ownerSources(), band, effort, sourceStrength));
        }
        evaluated.sort(Comparator.comparingInt(Evaluated::band)
                .thenComparing(Comparator.comparingInt(Evaluated::goalMatches).reversed())
                .thenComparing(Comparator.comparingInt(Evaluated::sourceStrength).reversed())
                .thenComparingInt(Evaluated::effort)
                .thenComparingInt(Evaluated::modelRank));
        List<ActionReadiness> answer = new ArrayList<>();
        for (Evaluated evaluatedItem : evaluated) {
            answer.add(new ActionReadiness(answer.size() + 1, evaluatedItem.modelRank(),
                    evaluatedItem.readiness(), evaluatedItem.reason(), evaluatedItem.goalMatches(),
                    evaluatedItem.observed(), evaluatedItem.contextual(), evaluatedItem.owner()));
        }
        return List.copyOf(answer);
    }

    private static final Set<String> STOPWORDS = Set.of("about", "after", "analysis", "business", "customer",
            "customers", "focus", "from", "goal", "have", "improve", "more", "owner", "reported",
            "selected", "service", "services", "that", "their", "this", "what", "when", "with", "your", "increase");

    private static Set<String> significantWords(String text) {
        Set<String> words = new HashSet<>();
        if (text == null) return words;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.length() >= 4 && !STOPWORDS.contains(word)) words.add(word);
        }
        return words;
    }

    private record Evaluated(int modelRank, String readiness, String reason, int goalMatches,
                             int observed, int contextual, int owner, int band, int effort, int sourceStrength) {}
}
