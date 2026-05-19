package com.sovereignstate.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GovernmentTypes {

    public record GovernmentType(
            String id,
            String category,
            String displayName,
            String description
    ) {}

    private static final Map<String, GovernmentType> REGISTRY = new LinkedHashMap<>();

    static {
        // --- MONARCHY ---
        register("absolute_monarchy",        "monarchy", "Absolute Monarchy",         "The sovereign holds total, unchecked authority.");
        register("constitutional_monarchy",  "monarchy", "Constitutional Monarchy",   "A monarch rules within the limits of a constitution.");
        register("semi_constitutional_monarchy", "monarchy", "Semi-Constitutional Monarchy", "Monarch retains most power but faces some parliamentary checks.");
        // register("feudal_monarchy",       "monarchy", "Feudal Monarchy",           "Decentralized rule through vassals and lords. Requires vassal system.");

        // --- DEMOCRACY ---
        register("representative_democracy", "democracy", "Representative Democracy", "Citizens elect officials to govern on their behalf.");
        register("direct_democracy",         "democracy", "Direct Democracy",         "Citizens vote directly on all laws and decisions.");

        // --- REPUBLIC ---
        register("republic",                 "republic", "Republic",                  "Elected officials govern under a constitutional framework.");
        register("federal_republic",         "republic", "Federal Republic",          "A republic with power divided between central and regional governments.");

        // --- AUTHORITARIAN ---
        register("oligarchy",                "authoritarian", "Oligarchy",             "A small group of powerful individuals holds control.");
        register("theocracy",                "authoritarian", "Theocracy",             "Religious authority governs the state.");
        // register("one_party_communist",   "authoritarian", "One-Party Communist",   "Single-party rule under communist ideology.");
        // register("military_junta",        "authoritarian", "Military Junta",        "Rule by a committee of military officers.");
    }

    public static void register(String id, String category, String displayName, String description) {
        REGISTRY.put(id, new GovernmentType(id, category, displayName, description));
    }

    public static boolean isValid(String id) {
        return REGISTRY.containsKey(id);
    }

    public static GovernmentType get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<GovernmentType> getAll() {
        return REGISTRY.values();
    }

    public static List<GovernmentType> getByCategory(String category) {
        return REGISTRY.values().stream()
                .filter(g -> g.category().equals(category))
                .collect(Collectors.toList());
    }

    public static List<String> getAllCategories() {
        return REGISTRY.values().stream()
                .map(GovernmentType::category)
                .distinct()
                .collect(Collectors.toList());
    }
}