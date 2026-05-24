package com.sovereignstate.registry;

import com.sovereignstate.item.CourtWarrantItem;
import com.sovereignstate.item.DivisionScrollItem;
import com.sovereignstate.item.MilitaryBadgeItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.sovereignstate.item.SurveyorMapItem;
import com.sovereignstate.item.DivisionScrollItem;
import com.sovereignstate.item.MilitaryBadgeItem;
import com.sovereignstate.item.CourtWarrantItem;

public class ModItems {

    public static final Item SURVEYOR_MAP = new SurveyorMapItem(new FabricItemSettings().maxCount(1));
    public static final Item POLICE_BATON = new Item(new FabricItemSettings().maxCount(1));
    public static final Item CULTURE_TOME = new Item(new FabricItemSettings().maxCount(1));
    public static final Item RELIGION_TOME = new Item(new FabricItemSettings().maxCount(1));
    public static final Item SPY_KIT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item LAW_PARCHMENT = new Item(new FabricItemSettings().maxCount(64));
    public static final Item CONSTITUTION_PARCHMENT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item TRADE_CONTRACT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item MARRIAGE_CONTRACT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item WARRANT_PARCHMENT = new CourtWarrantItem(new FabricItemSettings().maxCount(1));
    public static final Item PARDON_PARCHMENT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item APPEAL_PARCHMENT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item HANDCUFF = new Item(new FabricItemSettings().maxCount(1));
    public static final Item ROYAL_DECREE_SCROLL = new DivisionScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item UNIFORM_DYE_KIT = new Item(new FabricItemSettings().maxCount(16));
    public static final Item COIN_ENGRAVER = new Item(new FabricItemSettings().maxCount(1));
    public static final Item GENDER_CHANGE_PETITION = new Item(new FabricItemSettings().maxCount(1));
    public static final Item LOYALTY_OATH_PAPER = new Item(new FabricItemSettings().maxCount(16));
    public static final Item CULTURAL_RELIC = new Item(new FabricItemSettings().maxCount(1));
    public static final Item CULTURAL_DRESS = new Item(new FabricItemSettings().maxCount(1));
    public static final Item CULTURAL_SUPPRESSION_EDICT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item ASSIMILATION_DECREE = new Item(new FabricItemSettings().maxCount(1));
    public static final Item SLAVE_REGISTRY_PAPER = new Item(new FabricItemSettings().maxCount(1));
    public static final Item DISCRIMINATION_COMPLAINT = new Item(new FabricItemSettings().maxCount(1));
    public static final Item MILITARY_RANK_BADGE = new MilitaryBadgeItem(new FabricItemSettings().maxCount(1));
    public static final Item RELIGIOUS_RELIC = new Item(new FabricItemSettings().maxCount(1));
    public static final Item PARTY_MEMBERSHIP_CARD = new Item(new FabricItemSettings().maxCount(1));
    public static final Item COPPER_COIN = new Item(new FabricItemSettings().maxCount(64));
    public static final Item SILVER_COIN = new Item(new FabricItemSettings().maxCount(64));
    public static final Item SOVEREIGN_COIN = new Item(new FabricItemSettings().maxCount(64));
    public static final Item PLATINUM_BOND = new Item(new FabricItemSettings().maxCount(1));
    public static final Item BLANK_EMBLEM = new Item(new FabricItemSettings().maxCount(1));
    public static final Item CULTURE_REGISTRY_SCROLL = new Item(new FabricItemSettings().maxCount(64));
    public static final Item LOYALTY_OATH_ITEM = new Item(new FabricItemSettings().maxCount(16));

    public static void register() {
        registerItem("surveyor_map", SURVEYOR_MAP);
        registerItem("police_baton", POLICE_BATON);
        registerItem("culture_tome", CULTURE_TOME);
        registerItem("religion_tome", RELIGION_TOME);
        registerItem("spy_kit", SPY_KIT);
        registerItem("law_parchment", LAW_PARCHMENT);
        registerItem("constitution_parchment", CONSTITUTION_PARCHMENT);
        registerItem("trade_contract", TRADE_CONTRACT);
        registerItem("marriage_contract", MARRIAGE_CONTRACT);
        registerItem("warrant_parchment", WARRANT_PARCHMENT);
        registerItem("pardon_parchment", PARDON_PARCHMENT);
        registerItem("appeal_parchment", APPEAL_PARCHMENT);
        registerItem("royal_decree_scroll", ROYAL_DECREE_SCROLL);
        registerItem("handcuff", HANDCUFF);
        registerItem("uniform_dye_kit", UNIFORM_DYE_KIT);
        registerItem("coin_engraver", COIN_ENGRAVER);
        registerItem("gender_change_petition", GENDER_CHANGE_PETITION);
        registerItem("loyalty_oath_paper", LOYALTY_OATH_PAPER);
        registerItem("cultural_relic", CULTURAL_RELIC);
        registerItem("cultural_dress", CULTURAL_DRESS);
        registerItem("cultural_suppression_edict", CULTURAL_SUPPRESSION_EDICT);
        registerItem("assimilation_decree", ASSIMILATION_DECREE);
        registerItem("slave_registry_paper", SLAVE_REGISTRY_PAPER);
        registerItem("discrimination_complaint", DISCRIMINATION_COMPLAINT);
        registerItem("military_rank_badge", MILITARY_RANK_BADGE);
        registerItem("religious_relic", RELIGIOUS_RELIC);
        registerItem("party_membership_card", PARTY_MEMBERSHIP_CARD);
        registerItem("copper_coin", COPPER_COIN);
        registerItem("silver_coin", SILVER_COIN);
        registerItem("sovereign_coin", SOVEREIGN_COIN);
        registerItem("platinum_bond", PLATINUM_BOND);
        registerItem("blank_emblem", BLANK_EMBLEM);
        registerItem("culture_registry_scroll", CULTURE_REGISTRY_SCROLL);
        registerItem("loyalty_oath_item", LOYALTY_OATH_ITEM);
    }

    private static void registerItem(String name, Item item) {
        Registry.register(Registries.ITEM, new Identifier("sovereignstate", name), item);
    }
}