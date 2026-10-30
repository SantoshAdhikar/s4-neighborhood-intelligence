package com.s4.neighborhood.service;

import com.s4.neighborhood.entity.BusinessProfile;
import com.s4.neighborhood.entity.NearbyPlace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Industry-aware ranking. Listing names and categories are leads, not verified service menus. */
public final class CompetitorSelectionService {
    public static final int TARGET_COUNT = 50;

    private CompetitorSelectionService() {}

    private record Specialty(String label, String[] ownerSignals, String[] listingSignals) {
        boolean appliesTo(String owner) { return hasAny(owner, ownerSignals); }
        boolean appearsIn(String listing) { return hasAny(listing, listingSignals); }
    }

    private record IndustryRule(String name, String[] listingSignals, String[] relatedSignals,
                                List<Specialty> specialties) {}

    private static Specialty specialty(String label, String[] owner, String... listing) {
        return new Specialty(label, owner, listing);
    }

    private static final List<IndustryRule> INDUSTRIES = List.of(
            new IndustryRule("beauty personal care",
                    new String[]{"beauty", "salon", "spa", "hairdresser", "barber", "cosmetics", "esthetic", "threading", "brow*", "lash*", "nail*", "wax*", "skin care", "massage"},
                    new String[]{"tattoo", "permanent makeup", "tanning"},
                    List.of(
                            specialty("eyebrow threading", new String[]{"thread*"}, "thread*", "eyebrow threading"),
                            specialty("brow services", new String[]{"brow*", "eyebrow", "lamination"}, "brow*", "eyebrow", "lamination"),
                            specialty("lash services", new String[]{"lash*", "eyelash"}, "lash*", "eyelash"),
                            specialty("waxing", new String[]{"wax*", "depilat"}, "wax*", "depilat"),
                            specialty("facials and skin care", new String[]{"facial", "skin care", "esthetic"}, "facial", "skin care", "skincare", "esthetic"),
                            specialty("nail services", new String[]{"nail*", "manicure", "pedicure"}, "nail*", "manicure", "pedicure"),
                            specialty("hair and barber services", new String[]{"hair*", "haircut", "barber", "coloring"}, "hair*", "hairdresser", "barber", "haircut"),
                            specialty("massage", new String[]{"massage"}, "massage")
                    )),
            new IndustryRule("restaurant food",
                    new String[]{"restaurant", "fast food", "cafe", "coffee shop", "bistro", "diner", "food court", "bakery", "pizzeria", "takeaway", "takeout"},
                    new String[]{"bar", "pub", "catering", "ice cream", "confectionery"},
                    List.of(
                            specialty("pizza", new String[]{"pizza", "pizzeria"}, "pizza", "pizzeria", "cuisine italian"),
                            specialty("Indian or Nepali cuisine", new String[]{"indian", "nepali", "nepalese"}, "indian", "nepali", "nepalese", "cuisine indian", "cuisine nepalese"),
                            specialty("Japanese or sushi cuisine", new String[]{"sushi", "japanese", "ramen"}, "sushi", "japanese", "ramen"),
                            specialty("Thai cuisine", new String[]{"thai"}, "thai"),
                            specialty("Chinese cuisine", new String[]{"chinese", "dim sum"}, "chinese", "dim sum"),
                            specialty("Mexican cuisine", new String[]{"mexican", "taco", "burrito"}, "mexican", "taco", "burrito"),
                            specialty("burgers", new String[]{"burger"}, "burger"),
                            specialty("cafe or coffee", new String[]{"coffee", "cafe", "espresso"}, "coffee", "cafe", "espresso"),
                            specialty("bakery", new String[]{"bakery", "baked", "pastry", "cake"}, "bakery", "baked", "pastry", "cake"),
                            specialty("vegetarian or vegan food", new String[]{"vegetarian", "vegan"}, "vegetarian", "vegan"),
                            specialty("seafood", new String[]{"seafood", "fish restaurant"}, "seafood", "fish restaurant")
                    )),
            new IndustryRule("retail",
                    new String[]{"supermarket", "grocery", "convenience", "wholesale", "department store", "general store", "greengrocer", "hardware", "electronics", "clothes", "clothing", "books", "shoes", "gift", "houseware", "retail", "deli", "butcher", "cosmetics", "beauty supply"},
                    new String[]{"market", "mall", "shopping centre", "spices", "beverages", "bakery"},
                    List.of(
                            specialty("grocery and food retail", new String[]{"grocery", "groceries", "food", "rice", "spices", "cash carry", "supermarket", "market"}, "grocery", "supermarket", "convenience", "cash carry", "food market", "greengrocer"),
                            specialty("clothing", new String[]{"clothing", "fashion", "apparel", "clothes"}, "clothes", "clothing", "fashion", "apparel"),
                            specialty("hardware", new String[]{"hardware", "tools", "building supply"}, "hardware", "tool", "building supply"),
                            specialty("electronics", new String[]{"electronics", "computer", "phone accessories"}, "electronics", "computer", "mobile phone"),
                            specialty("books", new String[]{"books", "bookstore"}, "books", "bookstore"),
                            specialty("beauty products", new String[]{"beauty supply", "cosmetics", "skin care products"}, "beauty supply", "cosmetics"),
                            specialty("shoes", new String[]{"shoe", "footwear"}, "shoe", "footwear")
                    )),
            new IndustryRule("automotive",
                    new String[]{"car repair", "auto repair", "mechanic", "car service", "car parts", "tyres", "tires", "car wash", "car dealer", "shop car", "vehicle repair", "auto body"},
                    new String[]{"fuel", "parking", "car rental", "vehicle inspection"},
                    List.of(
                            specialty("auto mechanics and repair", new String[]{"repair*", "mechanic", "diagnostic", "engine", "brake", "transmission", "maintenance"}, "car repair", "auto repair", "mechanic", "garage", "service center", "service centre"),
                            specialty("tires", new String[]{"tire", "tyre", "wheel"}, "tire", "tyre", "wheel"),
                            specialty("body and collision repair", new String[]{"body", "collision", "paint", "dent*"}, "auto body", "body shop", "collision", "dent*"),
                            specialty("car wash and detailing", new String[]{"wash", "detail"}, "car wash", "detailing", "detail"),
                            specialty("auto parts", new String[]{"part", "accessor"}, "car parts", "auto parts", "accessories")
                    )),
            new IndustryRule("health wellness",
                    new String[]{"clinic", "doctors", "dentist", "pharmacy", "physiotherapy", "fitness centre", "gym", "yoga", "wellness", "health care"},
                    new String[]{"hospital", "sports centre", "massage"},
                    List.of(
                            specialty("dental care", new String[]{"dent*", "orthodont"}, "dent*", "orthodont"),
                            specialty("medical clinic", new String[]{"doctor", "clinic", "medical", "primary care"}, "doctor", "clinic", "medical"),
                            specialty("pharmacy", new String[]{"pharmac*", "medication"}, "pharmac*", "drugstore"),
                            specialty("fitness", new String[]{"gym", "fitness", "training", "yoga", "pilates"}, "gym", "fitness", "training", "yoga", "pilates"),
                            specialty("physiotherapy", new String[]{"physical therapy", "physiotherapy", "rehab"}, "physical therapy", "physiotherapy", "rehab")
                    )),
            new IndustryRule("home services",
                    new String[]{"plumber", "electrician", "carpenter", "painter", "hvac", "roofer", "cleaning", "landscaper", "gardener", "contractor", "locksmith", "handyman"},
                    new String[]{"building supply", "hardware"},
                    List.of(
                            specialty("plumbing", new String[]{"plumb*"}, "plumb*"),
                            specialty("electrical", new String[]{"electric*"}, "electric*"),
                            specialty("HVAC", new String[]{"hvac", "heating", "air conditioning"}, "hvac", "heating", "air conditioning"),
                            specialty("cleaning", new String[]{"clean*", "janitor"}, "clean*", "janitor"),
                            specialty("landscaping", new String[]{"landscap*", "gardening", "lawn"}, "landscap*", "gardening", "lawn"),
                            specialty("roofing", new String[]{"roof*"}, "roof*"),
                            specialty("painting", new String[]{"paint"}, "paint")
                    )),
            new IndustryRule("professional services",
                    new String[]{"lawyer", "attorney", "accountant", "insurance", "estate agent", "real estate", "tax", "notary", "consulting", "legal"},
                    new String[]{"office", "financial"},
                    List.of(
                            specialty("legal services", new String[]{"legal", "attorney", "lawyer"}, "legal", "attorney", "lawyer"),
                            specialty("accounting and tax", new String[]{"account*", "bookkeep", "tax"}, "account*", "bookkeep", "tax"),
                            specialty("real estate", new String[]{"real estate", "realtor", "property"}, "real estate", "realtor", "estate agent"),
                            specialty("insurance", new String[]{"insur*"}, "insur*")
                    )),
            new IndustryRule("pet services",
                    new String[]{"pet", "groom*", "veterinary", "animal", "dog", "cat", "boarding"},
                    new String[]{"pet shop", "pet supplies"},
                    List.of(
                            specialty("pet grooming", new String[]{"groom*", "bath"}, "groom*"),
                            specialty("veterinary care", new String[]{"veterinary", "vet", "animal hospital"}, "veterinary", "vet", "animal hospital"),
                            specialty("pet boarding", new String[]{"board", "daycare", "kennel"}, "boarding", "daycare", "kennel")
                    )),
            new IndustryRule("education tutoring",
                    new String[]{"school", "tutor", "learning center", "learning centre", "music school", "language school", "driving school", "training center", "training centre"},
                    new String[]{"library", "college", "university"},
                    List.of(
                            specialty("tutoring", new String[]{"tutor*", "math", "reading"}, "tutor*", "learning center", "learning centre"),
                            specialty("music lessons", new String[]{"music", "piano", "guitar"}, "music", "piano", "guitar"),
                            specialty("driving lessons", new String[]{"driving"}, "driving school")
                    )),
            new IndustryRule("technology repair",
                    new String[]{"computer repair", "mobile phone", "phone repair", "electronics repair", "repair shop", "computer shop"},
                    new String[]{"electronics", "computer", "mobile phone"},
                    List.of(
                            specialty("computer repair", new String[]{"computer", "laptop", "pc repair"}, "computer repair", "laptop repair", "pc repair"),
                            specialty("phone repair", new String[]{"phone", "screen repair"}, "phone repair", "mobile phone", "screen repair")
                    )),
            new IndustryRule("hospitality lodging",
                    new String[]{"hotel", "motel", "hostel", "guest house", "guesthouse", "bed breakfast", "lodging"},
                    new String[]{"resort", "camp site"},
                    List.of(specialty("hotel accommodation", new String[]{"hotel", "motel", "room"}, "hotel", "motel"),
                            specialty("guesthouse", new String[]{"guesthouse", "bed breakfast"}, "guesthouse", "guest house", "bed breakfast"))),
            new IndustryRule("cleaning laundry",
                    new String[]{"laundry", "dry cleaning", "cleaners", "washateria", "laundromat"},
                    new String[]{"house cleaning", "janitorial"},
                    List.of(specialty("laundry", new String[]{"laundry", "wash", "laundromat"}, "laundry", "laundromat"),
                            specialty("dry cleaning", new String[]{"dry clean"}, "dry cleaning", "dry cleaners"))),
            new IndustryRule("events photography",
                    new String[]{"photograph", "photo studio", "event planner", "wedding planner", "event venue"},
                    new String[]{"party supply", "catering"},
                    List.of(specialty("photography", new String[]{"photograph*", "portrait", "photo"}, "photograph*", "photo studio"),
                            specialty("event planning", new String[]{"event", "wedding", "party"}, "event planner", "wedding planner"))),
            new IndustryRule("childcare",
                    new String[]{"childcare", "day care", "daycare", "kindergarten", "preschool", "nursery"},
                    new String[]{"school", "tutor"},
                    List.of(specialty("childcare", new String[]{"daycare", "childcare", "preschool"}, "daycare", "childcare", "preschool", "kindergarten")))
    );

    public static List<Candidate> select(BusinessProfile business, List<NearbyPlace> nearby) {
        return select(business, nearby, business.getServices());
    }

    public static List<Candidate> select(BusinessProfile business, List<NearbyPlace> nearby, String focusServices) {
        return candidates(business, nearby, focusServices).stream().limit(TARGET_COUNT).toList();
    }

    /** Used by the manual map so it can highlight relevant places without hiding all map records. */
    public static List<Candidate> selectAll(BusinessProfile business, List<NearbyPlace> nearby) {
        return candidates(business, nearby, business.getServices());
    }

    private static List<Candidate> candidates(BusinessProfile business, List<NearbyPlace> nearby, String focusServices) {
        List<Candidate> matches = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String ownerName = normalize(business.getName());
        String focus = focusServices == null || focusServices.isBlank()
                ? business.getServices() : focusServices;
        String owner = normalize(focus == null || focus.isBlank() ? business.getName() : focus);
        IndustryRule industry = findIndustry(business.getIndustry(), owner);
        for (NearbyPlace place : nearby) {
            String name = normalize(place.getName());
            if (name.isBlank() || name.equals(ownerName)) continue;
            // Avoid dropping different locations of a chain while collapsing duplicate map features.
            String key = name + "|" + Math.round(place.getLatitude() * 2000) + "|" + Math.round(place.getLongitude() * 2000);
            if (!visited.add(key)) continue;
            Candidate match = industry == null ? classifyUnknown(owner, place) : classify(industry, owner, place);
            if (match != null) matches.add(match);
        }
        matches.sort(Comparator.comparingInt((Candidate candidate) -> priority(candidate.matchLevel()))
                .thenComparingDouble(candidate -> candidate.place().getDistanceMiles())
                .thenComparing(candidate -> normalize(candidate.place().getName())));
        return matches;
    }

    private static IndustryRule findIndustry(String rawIndustry, String owner) {
        String industry = normalize(rawIndustry);
        for (IndustryRule rule : INDUSTRIES) if (rule.name().equals(industry)) return rule;
        if (!industry.equals("other") && !industry.equals("other local services")) return null;
        // Service-defined fallback. No arbitrary nearby shop becomes a confirmed competitor.
        return INDUSTRIES.stream().filter(rule -> rule.specialties().stream().anyMatch(s -> s.appliesTo(owner)))
                .findFirst().orElse(null);
    }

    private static Candidate classifyUnknown(String owner, NearbyPlace place) {
        // Unrecognized industries can surface tentative name/category overlaps without
        // pretending to have a fully configured taxonomy or verified service list.
        Set<String> ignore = Set.of("local", "business", "services", "service", "shop", "store", "center", "centre",
                "company", "studio", "the", "and", "for", "with", "near", "small", "best", "new");
        String listing = normalize(place.getName() + " " + place.getCategory());
        int shared = 0;
        for (String word : owner.split(" ")) {
            if (word.length() >= 5 && !ignore.contains(word) &&
                    (" " + listing + " ").contains(" " + word + " ")) shared++;
        }
        return shared > 0 ? new Candidate(place, "POTENTIAL_OVERLAP",
                "Possible keyword overlap for a custom business type; services and competitor status require verification.") : null;
    }

    private static Candidate classify(IndustryRule industry, String owner, NearbyPlace place) {
        if (industry == null) return null;
        String listing = normalize(place.getName() + " " + place.getCategory());
        boolean inIndustry = hasAny(listing, industry.listingSignals());
        boolean related = hasAny(listing, industry.relatedSignals());
        if (!inIndustry && !related) return null;
        List<String> ownerSpecialties = new ArrayList<>();
        List<String> overlapping = new ArrayList<>();
        for (Specialty specialty : industry.specialties()) {
            if (!specialty.appliesTo(owner)) continue;
            ownerSpecialties.add(specialty.label());
            if (specialty.appearsIn(listing)) overlapping.add(specialty.label());
        }
        if (!overlapping.isEmpty()) return new Candidate(place, "DIRECT_SPECIALTY",
                "Listing name/category suggests " + String.join(", ", overlapping)
                        + "; actual services must be verified on published pages.");
        if (inIndustry && ownerSpecialties.isEmpty()) return new Candidate(place,
                industry.name().equals("retail") ? "RELATED_INDUSTRY" : "SAME_INDUSTRY",
                "Broad industry match; owner and listing specialties have not been verified.");
        boolean distinctOtherSpecialty = industry.specialties().stream()
                .filter(s -> !s.appliesTo(owner))
                .anyMatch(s -> s.appearsIn(listing));
        if (inIndustry && distinctOtherSpecialty && !ownerSpecialties.isEmpty()) {
            if (industry.name().equals("retail")) return null;
            return new Candidate(place, "RELATED_INDUSTRY",
                    "Listing indicates another specialty; shared services have not been verified.");
        }
        if (inIndustry) return new Candidate(place, "POTENTIAL_OVERLAP",
                "Same broad industry; no confirmed overlap with selected services: "
                        + String.join(", ", ownerSpecialties) + ".");
        return new Candidate(place, "RELATED_INDUSTRY",
                "Related business category; not a verified direct competitor.");
    }

    private static int priority(String level) {
        return switch (level) {
            case "DIRECT_SPECIALTY" -> 0;
            case "SAME_INDUSTRY" -> 1;
            case "POTENTIAL_OVERLAP" -> 2;
            default -> 3;
        };
    }

    private static boolean hasAny(String text, String... signals) {
        for (String raw : signals) {
            String signal = normalize(raw);
            // Boundary-aware signals avoid, e.g., 'bar' matching 'barber'.
            if (!signal.isEmpty() && (" " + text + " ").contains(" " + signal + " ")) return true;
            // Handle common morphological stems explicitly when the signal ends with '*'.
            if (raw.endsWith("*") && java.util.Arrays.stream(text.split(" "))
                    .anyMatch(word -> word.startsWith(normalize(raw.substring(0, raw.length() - 1))))) return true;
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    public record Candidate(NearbyPlace place, String matchLevel, String reason) {}
}
