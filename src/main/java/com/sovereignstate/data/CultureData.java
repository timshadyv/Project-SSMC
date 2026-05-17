package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CultureData extends PersistentState {

    private NbtList cultures = new NbtList();
    private boolean presetsLoaded = false;

    public static CultureData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    CultureData state = new CultureData();
                    state.cultures = nbt.getList("cultures", 10);
                    state.presetsLoaded = nbt.getBoolean("presetsLoaded");
                    return state;
                },
                CultureData::new,
                "sovereignstate_cultures"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("cultures", cultures);
        nbt.putBoolean("presetsLoaded", presetsLoaded);
        return nbt;
    }

    // --- Presets ---

    public void loadPresetsIfNeeded() {
        if (presetsLoaded) return;
        createPreset("tribal", "Tribal", "A primitive society bound by blood and tradition.", "egalitarian", "primitive", 80);
        createPreset("feudal", "Feudal", "A hierarchical system of lords and vassals.", "patriarchal", "medieval", 60);
        createPreset("imperial", "Imperial", "A vast empire ruled by an emperor.", "patriarchal", "imperial", 70);
        createPreset("nomadic", "Nomadic", "A wandering people following ancient routes.", "egalitarian", "nomadic", 75);
        createPreset("merchant_republic", "Merchant Republic", "Trade and coin rule above all else.", "egalitarian", "renaissance", 50);
        createPreset("theocratic", "Theocratic", "The divine word governs all law.", "patriarchal", "religious", 65);
        createPreset("matriarchal_clan", "Matriarchal Clan", "Women lead, men serve.", "matriarchal", "ancient", 85);
        createPreset("warrior_society", "Warrior Society", "Strength and honour above all.", "patriarchal", "spartan", 70);
        createPreset("scholastic", "Scholastic", "Knowledge is the highest virtue.", "egalitarian", "academic", 45);
        createPreset("maritime", "Maritime", "The sea is home and highway.", "egalitarian", "coastal", 55);
        createPreset("ancient_empire", "Ancient Empire", "A civilisation of great antiquity.", "patriarchal", "ancient", 90);
        createPreset("steppe_horde", "Steppe Horde", "A fierce horde from the eastern plains.", "patriarchal", "nomadic", 80);
        presetsLoaded = true;
        markDirty();
    }

    private void createPreset(String id, String name, String description, String genderHierarchy, String architecturalStyle, int cohesion) {
        NbtCompound culture = new NbtCompound();
        culture.putString("id", id);
        culture.putString("name", name);
        culture.putString("description", description);
        culture.putBoolean("isCustom", false);
        culture.putString("defaultGenderHierarchy", genderHierarchy);
        culture.putString("architecturalStyle", architecturalStyle);
        culture.putInt("cohesionValue", cohesion);
        culture.putString("holyItem", "");
        culture.put("tabooItems", new NbtList());
        culture.putInt("colorPrimary", 0xFFFFFF);
        culture.putInt("colorSecondary", 0x888888);
        culture.putInt("colorTertiary", 0x333333);
        culture.put("socialStructure", new NbtList());
        cultures.add(culture);
    }

    // --- Create Custom Culture ---

    public String createCulture(String name, String description, String genderHierarchy, String architecturalStyle, int cohesion) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound culture = new NbtCompound();
        culture.putString("id", id);
        culture.putString("name", name);
        culture.putString("description", description);
        culture.putBoolean("isCustom", true);
        culture.putString("defaultGenderHierarchy", genderHierarchy);
        culture.putString("architecturalStyle", architecturalStyle);
        culture.putInt("cohesionValue", cohesion);
        culture.putString("holyItem", "");
        culture.put("tabooItems", new NbtList());
        culture.putInt("colorPrimary", 0xFFFFFF);
        culture.putInt("colorSecondary", 0x888888);
        culture.putInt("colorTertiary", 0x333333);
        culture.put("socialStructure", new NbtList());
        cultures.add(culture);
        markDirty();
        return id;
    }

    // --- Get ---

    public NbtCompound getCultureById(String id) {
        for (int i = 0; i < cultures.size(); i++) {
            NbtCompound culture = cultures.getCompound(i);
            if (culture.getString("id").equals(id)) return culture;
        }
        return null;
    }

    public List<NbtCompound> getAllCultures() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < cultures.size(); i++) list.add(cultures.getCompound(i));
        return list;
    }

    public List<NbtCompound> getPresetCultures() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < cultures.size(); i++) {
            NbtCompound culture = cultures.getCompound(i);
            if (!culture.getBoolean("isCustom")) list.add(culture);
        }
        return list;
    }

    public List<NbtCompound> getCustomCultures() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < cultures.size(); i++) {
            NbtCompound culture = cultures.getCompound(i);
            if (culture.getBoolean("isCustom")) list.add(culture);
        }
        return list;
    }

    // --- Social Tiers ---

    public void addSocialTier(String cultureID, String tierName, String genderRestriction, int wealthRequirement) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return;
        NbtList structure = culture.getList("socialStructure", 10);
        NbtCompound tier = new NbtCompound();
        tier.putString("tierName", tierName);
        tier.putString("genderRestriction", genderRestriction);
        tier.putInt("wealthRequirement", wealthRequirement);
        tier.put("privilegeList", new NbtList());
        structure.add(tier);
        culture.put("socialStructure", structure);
        markDirty();
    }

    public List<NbtCompound> getSocialTiers(String cultureID) {
        NbtCompound culture = getCultureById(cultureID);
        List<NbtCompound> list = new ArrayList<>();
        if (culture == null) return list;
        NbtList structure = culture.getList("socialStructure", 10);
        for (int i = 0; i < structure.size(); i++) list.add(structure.getCompound(i));
        return list;
    }

    // --- Taboo Items ---

    public void addTabooItem(String cultureID, String itemID) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return;
        NbtList taboos = culture.getList("tabooItems", 8);
        taboos.add(NbtString.of(itemID));
        culture.put("tabooItems", taboos);
        markDirty();
    }

    public boolean isItemTaboo(String cultureID, String itemID) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return false;
        NbtList taboos = culture.getList("tabooItems", 8);
        for (int i = 0; i < taboos.size(); i++) {
            if (taboos.getString(i).equals(itemID)) return true;
        }
        return false;
    }

    // --- Colors ---

    public void setColors(String cultureID, int primary, int secondary, int tertiary) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return;
        culture.putInt("colorPrimary", primary);
        culture.putInt("colorSecondary", secondary);
        culture.putInt("colorTertiary", tertiary);
        markDirty();
    }

    // --- Holy Item ---

    public void setHolyItem(String cultureID, String itemID) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return;
        culture.putString("holyItem", itemID);
        markDirty();
    }

    // --- Flag ---

    public void setFlagData(String cultureID, byte[] flagData) {
        NbtCompound culture = getCultureById(cultureID);
        if (culture == null) return;
        culture.putByteArray("flagData", flagData);
        markDirty();
    }

    public byte[] getFlagData(String cultureID) {
        NbtCompound culture = getCultureById(cultureID);
        return culture != null ? culture.getByteArray("flagData") : new byte[512];
    }
}