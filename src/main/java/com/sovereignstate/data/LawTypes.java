package com.sovereignstate.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LawTypes {

    public record LawType(
            String id,
            String category,
            String displayName,
            String description,
            Set<String> allowedGovernmentCategories  // empty = allowed under any government
    ) {
        public boolean isAllowedUnder(String governmentCategory) {
            return allowedGovernmentCategories.isEmpty() ||
                    allowedGovernmentCategories.contains(governmentCategory);
        }
    }

    private static final Map<String, LawType> REGISTRY = new LinkedHashMap<>();

    static {
        // --- GENDER ---
        register("patriarchal_rule",      "gender", "Patriarchal Rule",       "Only men may hold leadership positions.",         Set.of());
        register("matriarchal_rule",      "gender", "Matriarchal Rule",       "Only women may hold leadership positions.",       Set.of());
        register("female_suffrage",       "gender", "Female Suffrage",        "Women are granted equal voting rights.",          Set.of("democracy", "republic"));
        register("equal_inheritance",     "gender", "Equal Inheritance",      "All genders inherit titles equally.",             Set.of());
        register("male_only_leadership",  "gender", "Male-Only Leadership",   "Leadership titles pass only to men.",             Set.of());

        // --- SUCCESSION ---
        register("primogeniture",         "succession", "Primogeniture",      "The firstborn child inherits the title.",         Set.of("monarchy"));
        register("elective_succession",   "succession", "Elective Succession","The next ruler is chosen by a council or vote.",  Set.of("monarchy", "republic"));
        register("absolute_cognatic",     "succession", "Absolute Cognatic",  "The eldest child inherits regardless of gender.", Set.of("monarchy"));
        register("seniority_succession",  "succession", "Seniority",          "The oldest eligible member of the dynasty inherits.", Set.of("monarchy"));

        // --- RELIGION ---
        register("state_religion",        "religion", "State Religion",       "One religion is officially enforced by the state.", Set.of());
        register("religious_tolerance",   "religion", "Religious Tolerance",  "All religions are permitted freely.",             Set.of());
        register("religious_persecution", "religion", "Religious Persecution","Non-state religions are banned or punished.",     Set.of());
        register("secular_state",         "religion", "Secular State",        "Religion is separated from governance entirely.", Set.of("democracy", "republic"));

        // --- TAXATION ---
        register("flat_tax",              "taxation", "Flat Tax",             "All citizens taxed at the same rate.",            Set.of());
        register("progressive_tax",       "taxation", "Progressive Tax",      "Higher earners pay a higher tax rate.",           Set.of("democracy", "republic"));
        register("tax_exempt_clergy",     "taxation", "Tax-Exempt Clergy",    "Religious figures pay no taxes.",                 Set.of());
        register("wealth_tax",            "taxation", "Wealth Tax",           "A tax levied on total accumulated wealth.",       Set.of());

        // --- MILITARY ---
        register("conscription",          "military", "Conscription",         "All citizens may be drafted into the military.",  Set.of());
        register("standing_army",         "military", "Standing Army",        "A permanent professional army is maintained.",    Set.of());
        register("mercenary_allowed",     "military", "Mercenaries Allowed",  "The state may hire mercenary forces.",            Set.of());
        register("military_service",      "military", "Mandatory Service",    "All citizens must complete military service.",    Set.of());

        // --- TRADE ---
        register("free_trade",            "trade", "Free Trade",              "No tariffs or restrictions on trade.",            Set.of());
        register("protectionism",         "trade", "Protectionism",           "Heavy tariffs protect local industry.",           Set.of());
        register("state_monopoly",        "trade", "State Monopoly",          "The state controls all major trade routes.",      Set.of("authoritarian"));
        register("open_borders",          "trade", "Open Borders",            "Free movement of people and goods.",              Set.of());

        // --- RIGHTS ---
        register("serfdom",               "rights", "Serfdom",                "Lower classes are bound to land and lords.",      Set.of("monarchy", "authoritarian"));
        register("slavery_legal",         "rights", "Slavery",                "Slavery is legally permitted.",                   Set.of());
        register("trial_by_jury",         "rights", "Trial by Jury",          "Citizens are judged by a jury of peers.",         Set.of("democracy", "republic"));
        register("free_speech",           "rights", "Free Speech",            "Citizens may speak freely without persecution.",  Set.of("democracy", "republic"));
        register("press_censorship",      "rights", "Press Censorship",       "The state controls all public information.",      Set.of("authoritarian"));
    }

    public static void register(String id, String category, String displayName,
                                String description, Set<String> allowedGovernmentCategories) {
        REGISTRY.put(id, new LawType(id, category, displayName, description, allowedGovernmentCategories));
    }

    public static boolean isValid(String id) {
        return REGISTRY.containsKey(id);
    }

    public static LawType get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<LawType> getAll() {
        return REGISTRY.values();
    }

    public static List<LawType> getByCategory(String category) {
        return REGISTRY.values().stream()
                .filter(l -> l.category().equals(category))
                .collect(Collectors.toList());
    }

    public static List<LawType> getAllowedForGovernment(String governmentCategory) {
        return REGISTRY.values().stream()
                .filter(l -> l.isAllowedUnder(governmentCategory))
                .collect(Collectors.toList());
    }

    public static List<String> getAllCategories() {
        return REGISTRY.values().stream()
                .map(LawType::category)
                .distinct()
                .collect(Collectors.toList());
    }
}