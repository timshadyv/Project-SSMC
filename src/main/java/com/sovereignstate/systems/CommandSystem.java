package com.sovereignstate.systems;

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

import java.util.List;

public class CommandSystem {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            var ss = CommandManager.literal("ss");

            // /ss found <name> <government> <genderpolicy>
            ss.then(CommandManager.literal("found")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .then(CommandManager.argument("government", StringArgumentType.word())
                                    .then(CommandManager.argument("genderpolicy", StringArgumentType.word())
                                            .executes(context -> {
                                                ServerCommandSource source = context.getSource();
                                                ServerPlayerEntity player = source.getPlayer();
                                                if (player == null) return 0;
                                                ServerWorld world = source.getWorld();
                                                String name = StringArgumentType.getString(context, "name");
                                                String gov = StringArgumentType.getString(context, "government");
                                                String gender = StringArgumentType.getString(context, "genderpolicy");
                                                DivisionSystem.foundDivision(player, world, name, gov, gender, "neutral");
                                                return 1;
                                            })))));

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

            dispatcher.register(ss);
        });
    }
}