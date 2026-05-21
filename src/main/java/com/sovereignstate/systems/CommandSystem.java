package com.sovereignstate.systems;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.systems.ContractSystem;
import com.sovereignstate.systems.PropertySystem;
import com.sovereignstate.systems.TradeSystem;
import com.sovereignstate.data.ArmyData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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

            // --- CURRENCY ---
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

            ss.then(currency);

            // --- TRADE ---
            var trade = CommandManager.literal("trade");

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

            trade.then(CommandManager.literal("inbox")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        TradeSystem.showInbox(player, world);
                        return 1;
                    }));

            ss.then(trade);

            // --- MARKET ---
            var market = CommandManager.literal("market");

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

            market.then(CommandManager.literal("browse")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        TradeSystem.browseMarket(player, world);
                        return 1;
                    }));

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

            ss.then(market);

            // --- PROPERTY ---
            var property = CommandManager.literal("property");

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

            property.then(CommandManager.literal("listings")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PropertySystem.browseListings(player, world);
                        return 1;
                    }));

            property.then(CommandManager.literal("info")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        PropertySystem.showChunkInfo(player, world);
                        return 1;
                    }));

            ss.then(property);

            // --- CONTRACT ---
            var contract = CommandManager.literal("contract");
            var contractCreate = CommandManager.literal("create");

            contractCreate.then(CommandManager.literal("trade")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .then(CommandManager.argument("intervalDays", IntegerArgumentType.integer(1))
                                                    .executes(context -> {
                                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                                        if (player == null) return 0;
                                                        ServerWorld world = context.getSource().getWorld();
                                                        String targetName = StringArgumentType.getString(context, "player");
                                                        String currencyID = StringArgumentType.getString(context, "currencyID");
                                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                                        int intervalDays = IntegerArgumentType.getInteger(context, "intervalDays");
                                                        ServerPlayerEntity target = context.getSource().getServer()
                                                                .getPlayerManager().getPlayer(targetName);
                                                        if (target == null) {
                                                            player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                            return 0;
                                                        }
                                                        ContractSystem.createTradeAgreement(player, world, target, currencyID, amount, intervalDays);
                                                        return 1;
                                                    }))))));

            contractCreate.then(CommandManager.literal("nap")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("durationDays", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String targetName = StringArgumentType.getString(context, "player");
                                        int durationDays = IntegerArgumentType.getInteger(context, "durationDays");
                                        ServerPlayerEntity target = context.getSource().getServer()
                                                .getPlayerManager().getPlayer(targetName);
                                        if (target == null) {
                                            player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                            return 0;
                                        }
                                        ContractSystem.createNAP(player, world, target, durationDays);
                                        return 1;
                                    }))));

            contractCreate.then(CommandManager.literal("loan")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("currencyID", StringArgumentType.word())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .then(CommandManager.argument("interestPercent", IntegerArgumentType.integer(0))
                                                    .then(CommandManager.argument("repayDays", IntegerArgumentType.integer(1))
                                                            .executes(context -> {
                                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                                if (player == null) return 0;
                                                                ServerWorld world = context.getSource().getWorld();
                                                                String targetName = StringArgumentType.getString(context, "player");
                                                                String currencyID = StringArgumentType.getString(context, "currencyID");
                                                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                                                int interest = IntegerArgumentType.getInteger(context, "interestPercent");
                                                                int repayDays = IntegerArgumentType.getInteger(context, "repayDays");
                                                                ServerPlayerEntity target = context.getSource().getServer()
                                                                        .getPlayerManager().getPlayer(targetName);
                                                                if (target == null) {
                                                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                                                    return 0;
                                                                }
                                                                ContractSystem.createLoan(player, world, target, currencyID, amount, interest, repayDays);
                                                                return 1;
                                                            })))))));

            contract.then(contractCreate);

            contract.then(CommandManager.literal("sign")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String contractID = StringArgumentType.getString(context, "contractID");
                                ContractSystem.signContract(player, world, contractID);
                                return 1;
                            })));

            contract.then(CommandManager.literal("addclause")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String contractID = StringArgumentType.getString(context, "contractID");
                                        String text = StringArgumentType.getString(context, "text");
                                        ContractSystem.addClause(player, world, contractID, text);
                                        return 1;
                                    }))));

            contract.then(CommandManager.literal("removeclause")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String contractID = StringArgumentType.getString(context, "contractID");
                                        int index = IntegerArgumentType.getInteger(context, "index");
                                        ContractSystem.removeClause(player, world, contractID, index);
                                        return 1;
                                    }))));

            contract.then(CommandManager.literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        ContractSystem.listContracts(player, world);
                        return 1;
                    }));

            contract.then(CommandManager.literal("info")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String contractID = StringArgumentType.getString(context, "contractID");
                                ContractSystem.showContractInfo(player, world, contractID);
                                return 1;
                            })));

            contract.then(CommandManager.literal("cancel")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String contractID = StringArgumentType.getString(context, "contractID");
                                ContractSystem.cancelContract(player, world, contractID);
                                return 1;
                            })));

            contract.then(CommandManager.literal("decide")
                    .then(CommandManager.argument("contractID", StringArgumentType.word())
                            .then(CommandManager.argument("decision", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String contractID = StringArgumentType.getString(context, "contractID");
                                        String decision = StringArgumentType.getString(context, "decision");
                                        ContractSystem.handleBreachDecision(player, world, contractID, decision);
                                        return 1;
                                    }))));

            ss.then(contract);
// --- DIPLOMACY ---
            var diplomacy = CommandManager.literal("diplomacy");
            var alliance = CommandManager.literal("alliance");
            var war = CommandManager.literal("war");
            var peace = CommandManager.literal("peace");
            var vassal = CommandManager.literal("vassal");

            // /ss diplomacy alliance propose <divID>
            alliance.then(CommandManager.literal("propose")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                String targetDiv = StringArgumentType.getString(context, "divID");
                                if (DivisionSystem.getDivisionName(world, targetDiv) == null) {
                                    player.sendMessage(Text.literal("§cDivision not found.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can propose alliances.")); return 0;
                                }
                                boolean ok = DiplomacySystem.proposeAlliance(world, myDiv, targetDiv);
                                if (ok) player.sendMessage(Text.literal("§aAlliance proposal sent to §e" + DivisionSystem.getDivisionName(world, targetDiv) + "§a."));
                                else player.sendMessage(Text.literal("§cCould not send proposal. Already allied, at war, or proposal already pending."));
                                return 1;
                            })));

            // /ss diplomacy alliance accept <divID>
            alliance.then(CommandManager.literal("accept")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can accept alliances.")); return 0;
                                }
                                String proposerDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.acceptAlliance(world, myDiv, proposerDiv);
                                if (ok) player.sendMessage(Text.literal("§aAlliance formed with §e" + DivisionSystem.getDivisionName(world, proposerDiv) + "§a!"));
                                else player.sendMessage(Text.literal("§cNo alliance proposal found from that division."));
                                return 1;
                            })));

            // /ss diplomacy alliance reject <divID>
            alliance.then(CommandManager.literal("reject")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                String proposerDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.rejectAlliance(world, myDiv, proposerDiv);
                                if (ok) player.sendMessage(Text.literal("§eAlliance proposal rejected."));
                                else player.sendMessage(Text.literal("§cNo proposal found from that division."));
                                return 1;
                            })));

            // /ss diplomacy alliance leave <divID>
            alliance.then(CommandManager.literal("leave")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can leave an alliance.")); return 0;
                                }
                                String allyDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.leaveAlliance(world, myDiv, allyDiv);
                                if (ok) player.sendMessage(Text.literal("§eYou have left the alliance with §c" + DivisionSystem.getDivisionName(world, allyDiv) + "§e."));
                                else player.sendMessage(Text.literal("§cYou are not allied with that division."));
                                return 1;
                            })));

            diplomacy.then(alliance);

            // /ss diplomacy war declare <divID>
            war.then(CommandManager.literal("declare")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can declare war.")); return 0;
                                }
                                String targetDiv = StringArgumentType.getString(context, "divID");
                                DiplomacySystem.DeclareWarResult result = DiplomacySystem.declareWar(world, myDiv, targetDiv, false);
                                switch (result) {
                                    case SUCCESS -> player.sendMessage(Text.literal("§c⚔ War declared on §e" + DivisionSystem.getDivisionName(world, targetDiv) + "§c!"));
                                    case NO_CASUS_BELLI -> player.sendMessage(Text.literal("§cYou have no casus belli against that division."));
                                    case ALREADY_AT_WAR -> player.sendMessage(Text.literal("§cYou are already at war with that division."));
                                    case DIVISION_NOT_FOUND -> player.sendMessage(Text.literal("§cDivision not found."));
                                }
                                return 1;
                            })));

            // /ss diplomacy war declareforce <divID>  (op-level override)
            war.then(CommandManager.literal("declareforce")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                String targetDiv = StringArgumentType.getString(context, "divID");
                                DiplomacySystem.DeclareWarResult result = DiplomacySystem.declareWar(world, myDiv, targetDiv, true);
                                if (result == DiplomacySystem.DeclareWarResult.SUCCESS)
                                    player.sendMessage(Text.literal("§c⚔ War force-declared on §e" + DivisionSystem.getDivisionName(world, targetDiv) + "§c!"));
                                else player.sendMessage(Text.literal("§cFailed: " + result.name()));
                                return 1;
                            })));

            diplomacy.then(war);

            // /ss diplomacy peace propose <divID>
            peace.then(CommandManager.literal("propose")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can propose peace.")); return 0;
                                }
                                String targetDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.proposePeace(world, myDiv, targetDiv);
                                if (ok) player.sendMessage(Text.literal("§aPeace proposal sent to §e" + DivisionSystem.getDivisionName(world, targetDiv) + "§a."));
                                else player.sendMessage(Text.literal("§cYou are not at war with that division, or proposal already sent."));
                                return 1;
                            })));

            // /ss diplomacy peace accept <divID>
            peace.then(CommandManager.literal("accept")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can accept peace.")); return 0;
                                }
                                String proposerDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.acceptPeace(world, myDiv, proposerDiv);
                                if (ok) player.sendMessage(Text.literal("§aPeace agreed with §e" + DivisionSystem.getDivisionName(world, proposerDiv) + "§a."));
                                else player.sendMessage(Text.literal("§cNo peace proposal found from that division."));
                                return 1;
                            })));

            // /ss diplomacy peace reject <divID>
            peace.then(CommandManager.literal("reject")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                String proposerDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.rejectPeace(world, myDiv, proposerDiv);
                                if (ok) player.sendMessage(Text.literal("§ePeace proposal rejected."));
                                else player.sendMessage(Text.literal("§cNo peace proposal found from that division."));
                                return 1;
                            })));

            diplomacy.then(peace);

            // /ss diplomacy vassal propose <divID>  (propose to make them YOUR vassal)
            vassal.then(CommandManager.literal("propose")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can propose vassalage.")); return 0;
                                }
                                String subjectDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.proposeVassal(world, myDiv, subjectDiv);
                                if (ok) player.sendMessage(Text.literal("§aVassalage proposal sent to §e" + DivisionSystem.getDivisionName(world, subjectDiv) + "§a."));
                                else player.sendMessage(Text.literal("§cCould not send proposal. They may already have an overlord or you are at war."));
                                return 1;
                            })));

            // /ss diplomacy vassal accept <divID>  (accept that divID becomes your overlord)
            vassal.then(CommandManager.literal("accept")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                if (!DivisionSystem.isLeader(world, player)) {
                                    player.sendMessage(Text.literal("§cOnly the division leader can accept vassalage.")); return 0;
                                }
                                String overlordDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.acceptVassal(world, myDiv, overlordDiv);
                                if (ok) player.sendMessage(Text.literal("§aYou are now a vassal of §e" + DivisionSystem.getDivisionName(world, overlordDiv) + "§a."));
                                else player.sendMessage(Text.literal("§cNo vassalage proposal found from that division."));
                                return 1;
                            })));

            // /ss diplomacy vassal reject <divID>
            vassal.then(CommandManager.literal("reject")
                    .then(CommandManager.argument("divID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                                if (myDiv == null || myDiv.isEmpty()) {
                                    player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                                }
                                String overlordDiv = StringArgumentType.getString(context, "divID");
                                boolean ok = DiplomacySystem.rejectVassal(world, myDiv, overlordDiv);
                                if (ok) player.sendMessage(Text.literal("§eVassalage proposal rejected."));
                                else player.sendMessage(Text.literal("§cNo vassalage proposal from that division."));
                                return 1;
                            })));

            // /ss diplomacy vassal independence
            vassal.then(CommandManager.literal("independence")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                        if (myDiv == null || myDiv.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                        }
                        if (!DivisionSystem.isLeader(world, player)) {
                            player.sendMessage(Text.literal("§cOnly the division leader can declare independence.")); return 0;
                        }
                        boolean ok = DiplomacySystem.declareIndependence(world, myDiv);
                        if (ok) player.sendMessage(Text.literal("§6You have declared independence! Your overlord now has casus belli against you."));
                        else player.sendMessage(Text.literal("§cYou are not a vassal of any division."));
                        return 1;
                    }));

            diplomacy.then(vassal);

            // /ss diplomacy status
            diplomacy.then(CommandManager.literal("status")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                        if (myDiv == null || myDiv.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                        }
                        player.sendMessage(Text.literal("§6--- Diplomacy Status ---"));
                        List<String> allies = DiplomacySystem.getAllies(world, myDiv);
                        List<String> enemies = DiplomacySystem.getEnemies(world, myDiv);
                        List<String> vassals = DiplomacySystem.getVassals(world, myDiv);
                        String overlord = DiplomacySystem.getOverlord(world, myDiv);
                        if (overlord != null)
                            player.sendMessage(Text.literal("§eOverlord: §f" + DivisionSystem.getDivisionName(world, overlord) + " §7(" + overlord + ")"));
                        if (!vassals.isEmpty()) {
                            StringBuilder vb = new StringBuilder("§eVassals: §f");
                            for (String v : vassals) vb.append(DivisionSystem.getDivisionName(world, v)).append(" §7(").append(v).append(")§f ");
                            player.sendMessage(Text.literal(vb.toString()));
                        }
                        if (!allies.isEmpty()) {
                            StringBuilder ab = new StringBuilder("§aAllies: §f");
                            for (String a : allies) ab.append(DivisionSystem.getDivisionName(world, a)).append(" §7(").append(a).append(")§f ");
                            player.sendMessage(Text.literal(ab.toString()));
                        } else player.sendMessage(Text.literal("§aAllies: §7none"));
                        if (!enemies.isEmpty()) {
                            StringBuilder eb = new StringBuilder("§cAt war with: §f");
                            for (String e : enemies) eb.append(DivisionSystem.getDivisionName(world, e)).append(" §7(").append(e).append(")§f ");
                            player.sendMessage(Text.literal(eb.toString()));
                        } else player.sendMessage(Text.literal("§cAt war with: §7none"));
                        return 1;
                    }));

            // /ss diplomacy inbox
            diplomacy.then(CommandManager.literal("inbox")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        String myDiv = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
                        if (myDiv == null || myDiv.isEmpty()) {
                            player.sendMessage(Text.literal("§cYou are not in a division.")); return 0;
                        }
                        player.sendMessage(Text.literal("§6--- Diplomacy Inbox ---"));
                        List<String> ap = DiplomacySystem.getIncomingAllianceProposals(world, myDiv);
                        List<String> pp = DiplomacySystem.getIncomingPeaceProposals(world, myDiv);
                        List<String> vp = DiplomacySystem.getIncomingVassalProposals(world, myDiv);
                        if (ap.isEmpty() && pp.isEmpty() && vp.isEmpty()) {
                            player.sendMessage(Text.literal("§7No pending proposals.")); return 1;
                        }
                        for (String d : ap) player.sendMessage(Text.literal("§a[Alliance] from §e" + DivisionSystem.getDivisionName(world, d) + " §7(" + d + ")"));
                        for (String d : pp) player.sendMessage(Text.literal("§b[Peace] from §e" + DivisionSystem.getDivisionName(world, d) + " §7(" + d + ")"));
                        for (String d : vp) player.sendMessage(Text.literal("§6[Vassal] §e" + DivisionSystem.getDivisionName(world, d) + " §6wants you as their vassal §7(" + d + ")"));
                        return 1;
                    }));

            ss.then(diplomacy);
            // --- MILITARY ---
            var military = CommandManager.literal("military");

            // /ss military raise <name> <unitType> <count>
            military.then(CommandManager.literal("raise")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .then(CommandManager.argument("unitType", StringArgumentType.word())
                                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = context.getSource().getWorld();
                                                String name = StringArgumentType.getString(context, "name");
                                                String unitType = StringArgumentType.getString(context, "unitType");
                                                int count = IntegerArgumentType.getInteger(context, "count");
                                                MilitarySystem.raiseArmy(player, world, name, unitType, count);
                                                return 1;
                                            })))));

            // /ss military disband <armyID>
            military.then(CommandManager.literal("disband")
                    .then(CommandManager.argument("armyID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String armyID = StringArgumentType.getString(context, "armyID");
                                MilitarySystem.disbandArmy(player, world, armyID);
                                return 1;
                            })));

            // /ss military deploy <armyID>
            military.then(CommandManager.literal("deploy")
                    .then(CommandManager.argument("armyID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String armyID = StringArgumentType.getString(context, "armyID");
                                MilitarySystem.deployArmy(player, world, armyID);
                                return 1;
                            })));

            // /ss military reinforce <armyID> <count>
            military.then(CommandManager.literal("reinforce")
                    .then(CommandManager.argument("armyID", StringArgumentType.word())
                            .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String armyID = StringArgumentType.getString(context, "armyID");
                                        int count = IntegerArgumentType.getInteger(context, "count");
                                        MilitarySystem.reinforceArmy(player, world, armyID, count);
                                        return 1;
                                    }))));

            // /ss military assigngeneral <armyID> <player>
            military.then(CommandManager.literal("assigngeneral")
                    .then(CommandManager.argument("armyID", StringArgumentType.word())
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String armyID = StringArgumentType.getString(context, "armyID");
                                        String targetName = StringArgumentType.getString(context, "player");
                                        ServerPlayerEntity target = context.getSource().getServer()
                                                .getPlayerManager().getPlayer(targetName);
                                        if (target == null) {
                                            player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                            return 0;
                                        }
                                        MilitarySystem.assignGeneral(player, world, armyID, target);
                                        return 1;
                                    }))));

            // /ss military conscript <player>
            military.then(CommandManager.literal("conscript")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String targetName = StringArgumentType.getString(context, "player");
                                ServerPlayerEntity target = context.getSource().getServer()
                                        .getPlayerManager().getPlayer(targetName);
                                if (target == null) {
                                    player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                    return 0;
                                }
                                MilitarySystem.conscriptPlayer(player, world, target);
                                return 1;
                            })));

            // /ss military rank <player> <rank>
            military.then(CommandManager.literal("rank")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("rank", StringArgumentType.word())
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        ServerWorld world = context.getSource().getWorld();
                                        String targetName = StringArgumentType.getString(context, "player");
                                        String rank = StringArgumentType.getString(context, "rank");
                                        ServerPlayerEntity target = context.getSource().getServer()
                                                .getPlayerManager().getPlayer(targetName);
                                        if (target == null) {
                                            player.sendMessage(Text.literal("§cPlayer not found: " + targetName));
                                            return 0;
                                        }
                                        MilitarySystem.setRank(player, world, target, rank);
                                        return 1;
                                    }))));

            // /ss military list
            military.then(CommandManager.literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        MilitarySystem.listArmies(player, world);
                        return 1;
                    }));

            // /ss military info <armyID>
            military.then(CommandManager.literal("info")
                    .then(CommandManager.argument("armyID", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ServerWorld world = context.getSource().getWorld();
                                String armyID = StringArgumentType.getString(context, "armyID");
                                MilitarySystem.showArmyInfo(player, world, armyID);
                                return 1;
                            })));

            // /ss military myrank
            military.then(CommandManager.literal("myrank")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = context.getSource().getWorld();
                        MilitarySystem.showMyRank(player, world);
                        return 1;
                    }));

            ss.then(military);
            dispatcher.register(ss);
        });
    }
}