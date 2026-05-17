package com.sovereignstate.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block PARLIAMENT_BENCH = new Block(FabricBlockSettings.create().strength(2.5f).sounds(BlockSoundGroup.WOOD));
    public static final Block COURTHOUSE_GAVEL = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block IRON_JAIL_BARS = new Block(FabricBlockSettings.create().strength(5.0f, 10.0f).sounds(BlockSoundGroup.METAL));
    public static final Block THRONE = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block MATRIARCH_THRONE = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block PATRIARCH_THRONE = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block VOTING_URN = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.STONE));
    public static final Block BANK_VAULT = new Block(FabricBlockSettings.create().strength(5.0f, 10.0f).sounds(BlockSoundGroup.METAL));
    public static final Block BANK_TELLER = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block MARKET_STALL = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block TRADE_ROUTE_FLAG = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block CUSTOMS_POST = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.STONE));
    public static final Block CULTURE_MONUMENT = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.STONE));
    public static final Block CULTURE_CRAFTING = new Block(FabricBlockSettings.create().strength(2.5f).sounds(BlockSoundGroup.WOOD));
    public static final Block FLAG_CREATOR = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block HERALDRY_TABLE = new Block(FabricBlockSettings.create().strength(2.5f).sounds(BlockSoundGroup.WOOD));
    public static final Block ALTAR = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.STONE));
    public static final Block HIGH_ALTAR = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.STONE));
    public static final Block PROPAGANDA_BOARD = new Block(FabricBlockSettings.create().strength(1.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block MILITARY_WALL = new Block(FabricBlockSettings.create().strength(15.0f, 30.0f).sounds(BlockSoundGroup.STONE));
    public static final Block WATCHTOWER = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.STONE));
    public static final Block ARTILLERY = new Block(FabricBlockSettings.create().strength(5.0f).sounds(BlockSoundGroup.METAL));
    public static final Block COUNCIL_TABLE = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block COMMAND_PODIUM = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block MERIT_TERMINAL = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.METAL));
    public static final Block LOYALTY_OATH_BLOCK = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.STONE));
    public static final Block CONSTITUTION_DISPLAY = new Block(FabricBlockSettings.create().strength(2.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block STOCK_EXCHANGE = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.METAL));
    public static final Block PRESIDENTIAL_DESK = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block FEDERAL_SEAL = new Block(FabricBlockSettings.create().strength(5.0f).sounds(BlockSoundGroup.METAL));
    public static final Block GYNARCHY_SEAL = new Block(FabricBlockSettings.create().strength(5.0f).sounds(BlockSoundGroup.METAL));
    public static final Block ANDROCRACY_SEAL = new Block(FabricBlockSettings.create().strength(5.0f).sounds(BlockSoundGroup.METAL));
    public static final Block CURRENCY_MINT = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.METAL));
    public static final Block CURRENCY_EXCHANGE = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.METAL));
    public static final Block SERVER_ADMIN_PANEL = new Block(FabricBlockSettings.create().strength(4.0f).sounds(BlockSoundGroup.METAL));
    public static final Block SLAVE_REGISTRY_BLOCK = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.WOOD));
    public static final Block HERESY_TRIBUNAL = new Block(FabricBlockSettings.create().strength(3.0f).sounds(BlockSoundGroup.STONE));
    public static final Block FLAG_POLE = new Block(FabricBlockSettings.create().strength(2.0f).nonOpaque().sounds(BlockSoundGroup.WOOD));

    public static void register() {
        registerBlock("parliament_bench", PARLIAMENT_BENCH);
        registerBlock("courthouse_gavel", COURTHOUSE_GAVEL);
        registerBlock("iron_jail_bars", IRON_JAIL_BARS);
        registerBlock("throne", THRONE);
        registerBlock("matriarch_throne", MATRIARCH_THRONE);
        registerBlock("patriarch_throne", PATRIARCH_THRONE);
        registerBlock("voting_urn", VOTING_URN);
        registerBlock("bank_vault", BANK_VAULT);
        registerBlock("bank_teller", BANK_TELLER);
        registerBlock("market_stall", MARKET_STALL);
        registerBlock("trade_route_flag", TRADE_ROUTE_FLAG);
        registerBlock("customs_post", CUSTOMS_POST);
        registerBlock("culture_monument", CULTURE_MONUMENT);
        registerBlock("culture_crafting", CULTURE_CRAFTING);
        registerBlock("flag_creator", FLAG_CREATOR);
        registerBlock("heraldry_table", HERALDRY_TABLE);
        registerBlock("altar", ALTAR);
        registerBlock("high_altar", HIGH_ALTAR);
        registerBlock("propaganda_board", PROPAGANDA_BOARD);
        registerBlock("military_wall", MILITARY_WALL);
        registerBlock("watchtower", WATCHTOWER);
        registerBlock("artillery", ARTILLERY);
        registerBlock("council_table", COUNCIL_TABLE);
        registerBlock("command_podium", COMMAND_PODIUM);
        registerBlock("merit_terminal", MERIT_TERMINAL);
        registerBlock("loyalty_oath_block", LOYALTY_OATH_BLOCK);
        registerBlock("constitution_display", CONSTITUTION_DISPLAY);
        registerBlock("stock_exchange", STOCK_EXCHANGE);
        registerBlock("presidential_desk", PRESIDENTIAL_DESK);
        registerBlock("federal_seal", FEDERAL_SEAL);
        registerBlock("gynarchy_seal", GYNARCHY_SEAL);
        registerBlock("androcracy_seal", ANDROCRACY_SEAL);
        registerBlock("currency_mint", CURRENCY_MINT);
        registerBlock("currency_exchange", CURRENCY_EXCHANGE);
        registerBlock("server_admin_panel", SERVER_ADMIN_PANEL);
        registerBlock("slave_registry_block", SLAVE_REGISTRY_BLOCK);
        registerBlock("heresy_tribunal", HERESY_TRIBUNAL);
        registerBlock("flag_pole", FLAG_POLE);
    }

    private static void registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier("sovereignstate", name), block);
        Registry.register(Registries.ITEM, new Identifier("sovereignstate", name),
                new BlockItem(block, new FabricItemSettings()));
    }
}