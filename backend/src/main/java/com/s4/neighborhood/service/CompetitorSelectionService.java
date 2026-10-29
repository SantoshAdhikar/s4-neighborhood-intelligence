package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ranks OSM category matches; it cannot establish commercial competition or service equivalence. */
public final class CompetitorSelectionService {
    public static final int TARGET_COUNT = 10;

    private CompetitorSelectionService() {}

    public static List<Candidate> select(BusinessProfile business, List<NearbyPlace> nearby) {
        List<Candidate> candidates = new ArrayList<>();
        Set<String> distinct = new HashSet<>();
        String ownerName = normalize(business.getName());

        for (NearbyPlace place : nearby) {
            String name = normalize(place.getName());
            if (name.isEmpty() || name.equals(ownerName) || !distinct.add(name)) continue;
            String matchLevel = matchLevel(business.getIndustry(), place);
            if (matchLevel != null) candidates.add(new Candidate(place, matchLevel));
        }

        candidates.sort(Comparator
                .comparingInt((Candidate item) -> "SAME_INDUSTRY".equals(item.matchLevel()) ? 0 : 1)
                .thenComparingDouble(item -> item.place().getDistanceMiles())
                .thenComparing(item -> normalize(item.place().getName())));
        return candidates.stream().limit(TARGET_COUNT).toList();
    }

    private static String matchLevel(String industry, NearbyPlace place) {
        String kind = normalize(industry);
        String category = normalize(place.getCategory());
        String name = normalize(place.getName());

        if (kind.equals("beauty personal care")) {
            if (containsAny(name, "thread", "brow", "lash", "wax", "facial", "skin care", "skincare", "esthetic", "beauty", "cosmetic"))
                return "SAME_INDUSTRY";
            if (containsAny(name, "nail", "hair", "barber", "tattoo", "massage") ||
                    containsAny(category, "hairdresser", "massage", "spa"))
                return "RELATED_INDUSTRY";
            if (containsAny(category, "shop beauty", "shop cosmetics")) return "SAME_INDUSTRY";
            return null;
        }
        if (kind.equals("restaurant food")) {
            return containsAny(category, "amenity restaurant", "amenity cafe", "amenity fast food", "shop bakery")
                    ? "SAME_INDUSTRY" : null;
        }
        if (kind.equals("health wellness")) {
            return containsAny(category, "amenity clinic", "amenity doctors", "amenity dentist", "amenity pharmacy", "leisure fitness centre")
                    ? "SAME_INDUSTRY" : null;
        }
        if (kind.equals("automotive")) {
            return containsAny(category, "shop car", "shop car repair", "shop car parts", "shop tyres")
                    ? "SAME_INDUSTRY" : null;
        }
        if (kind.equals("home services")) {
            return containsAny(category, "craft plumber", "craft electrician", "craft carpenter", "craft painter", "craft hvac")
                    ? "SAME_INDUSTRY" : null;
        }
        if (kind.equals("professional services")) {
            return containsAny(category, "office lawyer", "office accountant", "office insurance", "office estate agent")
                    ? "SAME_INDUSTRY" : null;
        }
        // Unknown industries cannot be reliably matched by OSM's broad shop tags.
        return null;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) if (text.contains(keyword)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    public record Candidate(NearbyPlace place, String matchLevel) {}
}
