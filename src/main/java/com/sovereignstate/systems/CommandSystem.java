package com.sovereignstate.systems;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import com.sovereignstate.systems.CurrencySystem;
import com.sovereignstate.systems.PropertySystem;

import java.util.List;

public class CommandSystem {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            var ss = CommandManager.literal("ss");

            // /ss found <name> <government>
            ss.then(CommandManager.literal("found")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .then(CommandManager.argument("government", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = source.getWorld();
                                        String name = StringArgumentType.getString(context, "name");
                                        String gov = StringArgumentType.getString(context, "government");
                                        DivisionSystem.foundDivision(player, world, name, gov, "neutral");
                                        return 1;
                                    }))));

            // /ss info
            ss.then(CommandManager.literal("info")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PlayerStateData playerState = PlayerStateData.get(world);
                        String divisionID = playerState.getDivisionID(player.getUuid().toString());
                        if (divisionID == null || divisionID.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division."));
                        } else {
                            DivisionSystem.showDivisionInfo(player, world, divisionID);
                        }
                        return 1;
                    }));

            // /ss join <divisionID>
            ss.then(CommandManager.literal("join")
                    .then(CommandManager.argument("divisionID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String divisionID = StringArgumentType.getString(context, "divisionID");
                                DivisionSystem.joinDivision(player, world, divisionID);
                                return 1;
                            })));

            // /ss leave
            ss.then(CommandManager.literal("leave")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        DivisionSystem.leaveDivision(player, world);
                        return 1;
                    }));

            // /ss list
            ss.then(CommandManager.literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        DivisionData divData = DivisionData.get(world);
                        List<NbtCompound> all = divData.getAllDivisions();
                        if (all.isEmpty()) {
                            player.sendMessage(Text.literal("§eNo divisions exist yet."));
                        } else {
                            player.sendMessage(Text.literal("§6--- All Divisions ---"));
                            for (NbtCompound div : all) {
                                player.sendMessage(Text.literal(
                                        "§e" + div.getString("name") +
                                                " §7[" + div.getString("governmentType") + "]" +
                                                " §fID: " + div.getString("id")));
                            }
                        }
                        return 1;
                    }));

            // /ss gender <male/female>
            ss.then(CommandManager.literal("gender")
                    .then(CommandManager.argument("gender", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String gender = StringArgumentType.getString(context, "gender");
                                if (!gender.equals("male") && !gender.equals("female")) {
                                    player.sendMessage(Text.literal("§cGender must be 'male' or 'female'."));
                                    return 0;
                                }
                                PlayerStateData playerState = PlayerStateData.get(world);
                                playerState.setGender(player.getUuid().toString(), gender);
                                playerState.setGenderRegistered(player.getUuid().toString(), true);
                                player.sendMessage(Text.literal("§aGender set to §e" + gender + "§a."));
                                return 1;
                            })));

            // /ss dissolve
            ss.then(CommandManager.literal("dissolve")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PlayerStateData playerState = PlayerStateData.get(world);
                        String divisionID = playerState.getDivisionID(player.getUuid().toString());
                        if (divisionID == null || divisionID.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division."));
                        } else {
                            DivisionSystem.dissolveDivision(player, world, divisionID);
                        }
                        return 1;
                    }));

            // /ss abandon
            ss.then(CommandManager.literal("abandon")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        ChunkClaimingSystem.abandonChunk(player, world);
                        return 1;
                    }));

            // /ss setcapital
            ss.then(CommandManager.literal("setcapital")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        ChunkClaimingSystem.setCapital(player, world);
                        return 1;
                    }));

            // /ss setjail
            ss.then(CommandManager.literal("setjail")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        ChunkClaimingSystem.setJailZone(player, world);
                        return 1;
                    }));

            // /ss law <lawname>
            ss.then(CommandManager.literal("law")
                    .then(CommandManager.argument("lawname", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                PlayerStateData playerState = PlayerStateData.get(world);
                                String divisionID = playerState.getDivisionID(player.getUuid().toString());
                                if (divisionID == null || divisionID.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division."));
                                    return 0;
                                }
                                String lawName = StringArgumentType.getString(context, "lawname");
                                LawSystem.submitLaw(player, world, divisionID, lawName);
                                return 1;
                            })));

            // /ss repeal <lawname>
            ss.then(CommandManager.literal("repeal")
                    .then(CommandManager.argument("lawname", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                PlayerStateData playerState = PlayerStateData.get(world);
                                String divisionID = playerState.getDivisionID(player.getUuid().toString());
                                if (divisionID == null || divisionID.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division."));
                                    return 0;
                                }
                                String lawName = StringArgumentType.getString(context, "lawname");
                                LawSystem.repealLaw(player, world, divisionID, lawName);
                                return 1;
                            })));

            // /ss laws
            ss.then(CommandManager.literal("laws")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PlayerStateData playerState = PlayerStateData.get(world);
                        String divisionID = playerState.getDivisionID(player.getUuid().toString());
                        if (divisionID == null || divisionID.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division."));
                            return 0;
                        }
                        LawSystem.listLaws(player, world, divisionID);
                        return 1;
                    }));

            // /ss vote <yes/no> <lawname>
            ss.then(CommandManager.literal("vote")
                    .then(CommandManager.argument("choice", StringArgumentType.word())
                            .then(CommandManager.argument("lawname", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        PlayerStateData playerState = PlayerStateData.get(world);
                                        String divisionID = playerState.getDivisionID(player.getUuid().toString());
                                        if (divisionID == null || divisionID.isEmpty()) {
                                            player.sendMessage(Text.literal("§cYou are not in a division."));
                                            return 0;
                                        }
                                        String choice = StringArgumentType.getString(context, "choice");
                                        String lawName = StringArgumentType.getString(context, "lawname");
                                        boolean voteYes = choice.equalsIgnoreCase("yes");
                                        LawSystem.castVote(player, world, divisionID, lawName, voteYes);
                                        return 1;
                                    }))));

            // /ss balance
            ss.then(CommandManager.literal("balance")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        TaxSystem.checkBalance(player, world);
                        return 1;
                    }));

            // /ss give <currencyID> <amount>
            ss.then(CommandManager.literal("give")
                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        TaxSystem.giveCoins(player, world, currencyID, amount);
                                        return 1;
                                    }))));

            // /ss currency create <name> <namePlural> <symbol> <baseValue> <mintRate>
            var currency = CommandManager.literal("currency");

            currency.then(CommandManager.literal("create")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .then(CommandManager.argument("namePlural", StringArgumentType.word())
                                    .then(CommandManager.argument("symbol", StringArgumentType.word())
                                            .then(CommandManager.argument("baseValue", IntegerArgumentType.integer(1))
                                                    .then(CommandManager.argument("mintRate", IntegerArgumentType.integer(1))
                                                            .executes(context -> {
                                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                                if (player == null) return 0;
                                                                ServerWorld world = context.getSource().getWorld();
                                                                String name = StringArgumentType.getString(context, "name");
                                                                String namePlural = StringArgumentType.getString(context, "namePlural");
                                                                String symbol = StringArgumentType.getString(context, "symbol");
                                                                int baseValue = IntegerArgumentType.getInteger(context, "baseValue");
                                                                int mintRate = IntegerArgumentType.getInteger(context, "mintRate");
                                                                CurrencySystem.createCurrency(player, world, name, namePlural, symbol, baseValue, mintRate);
                                                                return 1;
                                                            })))))));

            currency.then(CommandManager.literal("mint")
                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        CurrencySystem.mintCoins(player, world, currencyID, amount);
                                        return 1;
                                    }))));

            currency.then(CommandManager.literal("info")
                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                CurrencySystem.showCurrencyInfo(player, world, currencyID);
                                return 1;
                            })));

            currency.then(CommandManager.literal("withdraw")
                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        CurrencySystem.withdrawFromTreasury(player, world, currencyID, amount);
                                        return 1;
                                    }))));

            currency.then(CommandManager.literal("pay")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = context.getSource().getWorld();
                                                String targetName = StringArgumentType.getString(context, "player");
                                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                                ServerPlayerEntity target = context.getSource().getServer()
                                                        .getPlayerManager().getPlayer(targetName);
                                                if (target == null) {
                                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                    return 0;
                                                }
                                                CurrencySystem.transferCoins(player, world, target, currencyID, amount);
                                                return 1;
                                            })))));
// --- TRADE COMMANDS ---
            var trade = CommandManager.literal("trade");

// /ss trade offer <player> <currencyID> <amount>
            trade.then(CommandManager.literal("offer")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = context.getSource().getWorld();
                                                String targetName = StringArgumentType.getString(context, "player");
                                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                                ServerPlayerEntity target = context.getSource().getServer()
                                                        .getPlayerManager().getPlayer(targetName);
                                                if (target == null) {
                                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                    return 0;
                                                }
                                                TradeSystem.offerCurrency(player, world, target, currencyID, amount);
                                                return 1;
                                            })))));

// /ss trade offeritem <player> <price> <currencyID>
            trade.then(CommandManager.literal("offeritem")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = context.getSource().getWorld();
                                                String targetName = StringArgumentType.getString(context, "player");
                                                int price = IntegerArgumentType.getInteger(context, "price");
                                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                                ServerPlayerEntity target = context.getSource().getServer()
                                                        .getPlayerManager().getPlayer(targetName);
                                                if (target == null) {
                                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                    return 0;
                                                }
                                                TradeSystem.offerItem(player, world, target, price, currencyID);
                                                return 1;
                                            })))));

// /ss trade accept <offerID>
            trade.then(CommandManager.literal("accept")
                    .then(CommandManager.argument("offerID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String offerID = StringArgumentType.getString(context, "offerID");
                                TradeSystem.acceptOffer(player, world, offerID);
                                return 1;
                            })));

// /ss trade reject <offerID>
            trade.then(CommandManager.literal("reject")
                    .then(CommandManager.argument("offerID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String offerID = StringArgumentType.getString(context, "offerID");
                                TradeSystem.rejectOffer(player, world, offerID);
                                return 1;
                            })));

// /ss trade inbox
            trade.then(CommandManager.literal("inbox")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        TradeSystem.showInbox(player, world);
                        return 1;
                    }));

            ss.then(trade);

// --- MARKET COMMANDS ---
            var market = CommandManager.literal("market");

// /ss market post <currencyID> <amount> <price> <priceCurrencyID>
            market.then(CommandManager.literal("post")
                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                            .then(CommandManager.argument("priceCurrencyID", StringArgumentType.word())
                                                    .executes(context -> {
                                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                                        if (player == null) return 0;
                                                        ServerWorld world = context.getSource().getWorld();
                                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                                        int price = IntegerArgumentType.getInteger(context, "price");
                                                        String priceCurrencyID = StringArgumentType.getString(context, "priceCurrencyID");
                                                        TradeSystem.postCurrencyListing(player, world, currencyID, amount, price, priceCurrencyID);
                                                        return 1;
                                                    }))))));

// /ss market postitem <price> <priceCurrencyID>
            market.then(CommandManager.literal("postitem")
                    .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                            .then(CommandManager.argument("priceCurrencyID", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        int price = IntegerArgumentType.getInteger(context, "price");
                                        String priceCurrencyID = StringArgumentType.getString(context, "priceCurrencyID");
                                        TradeSystem.postItemListing(player, world, price, priceCurrencyID);
                                        return 1;
                                    }))));

// /ss market browse
            market.then(CommandManager.literal("browse")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        TradeSystem.browseMarket(player, world);
                        return 1;
                    }));

// /ss market buy <listingID>
            market.then(CommandManager.literal("buy")
                    .then(CommandManager.argument("listingID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String listingID = StringArgumentType.getString(context, "listingID");
                                TradeSystem.buyListing(player, world, listingID);
                                return 1;
                            })));
// --- PROPERTY COMMANDS ---
            var property = CommandManager.literal("property");

// /ss property sell <price> <currencyID>
            property.then(CommandManager.literal("sell")
                    .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                            .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        int price = IntegerArgumentType.getInteger(context, "price");
                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                        PropertySystem.listChunkForSale(player, world, price, currencyID);
                                        return 1;
                                    }))));

// /ss property offer <player> <price> <currencyID>
            property.then(CommandManager.literal("offer")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = context.getSource().getWorld();
                                                String targetName = StringArgumentType.getString(context, "player");
                                                int price = IntegerArgumentType.getInteger(context, "price");
                                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                                ServerPlayerEntity target = context.getSource().getServer()
                                                        .getPlayerManager().getPlayer(targetName);
                                                if (target == null) {
                                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                    return 0;
                                                }
                                                PropertySystem.offerChunkToPlayer(player, world, target, price, currencyID);
                                                return 1;
                                            })))));

// /ss property buy <listingID>
            property.then(CommandManager.literal("buy")
                    .then(CommandManager.argument("listingID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String listingID = StringArgumentType.getString(context, "listingID");
                                PropertySystem.buyChunk(player, world, listingID);
                                return 1;
                            })));

// /ss property accept <offerID>
            property.then(CommandManager.literal("accept")
                    .then(CommandManager.argument("offerID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String offerID = StringArgumentType.getString(context, "offerID");
                                PropertySystem.acceptLandOffer(player, world, offerID);
                                return 1;
                            })));

// /ss property reject <offerID>
            property.then(CommandManager.literal("reject")
                    .then(CommandManager.argument("offerID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String offerID = StringArgumentType.getString(context, "offerID");
                                PropertySystem.rejectLandOffer(player, world, offerID);
                                return 1;
                            })));

// /ss property listings
            property.then(CommandManager.literal("listings")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PropertySystem.browseListings(player, world);
                        return 1;
                    }));

// /ss property info
            property.then(CommandManager.literal("info")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PropertySystem.showChunkInfo(player, world);
                        return 1;
                    }));

            ss.then(property);
            ss.then(market);
            ss.then(currency);

            dispatcher.register(ss);
        });
    }
}