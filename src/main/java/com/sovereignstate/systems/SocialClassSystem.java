package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class SocialClassSystem {

    // Class hierarchy lowest to highest
    public static final String[] CLASSES = {
            "peasant", "merchant", "noble", "royalty"
    };

    public static int getClassRank(String className) {
        for (int i = 0; i < CLASSES.length; i++) {
            if (CLASSES[i].equalsIgnoreCase(className)) return i;
        }
        return -1;
    }

    public static boolean isValidClass(String className) {
        return getClassRank(className) >= 0;
    }

    // --- Assign class ---

    public static void setClass(ServerPlayerEntity actor, ServerWorld world,
                                ServerPlayerEntity target, String className) {
        if (!isValidClass(className)) {
            actor.sendMessage(Text.literal("§cInvalid class. Valid: peasant, merchant, noble, royalty"));
            return;
        }

        PlayerStateData playerState = PlayerStateData.get(world);
        DivisionData divData = DivisionData.get(world);

        String actorUUID = actor.getUuid().toString();
        String targetUUID = target.getUuid().toString();

        String actorDivID = playerState.getDivisionID(actorUUID);
        String targetDivID = playerState.getDivisionID(targetUUID);

        if (actorDivID == null || actorDivID.isEmpty()) {
            actor.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        if (!actorDivID.equals(targetDivID)) {
            actor.sendMessage(Text.literal("§cThat player is not in your division."));
            return;
        }

        NbtCompound div = divData.getDivisionById(actorDivID);
        if (div == null) {
            actor.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        // Only leader can assign classes
        if (!div.getString("leaderUUID").equals(actorUUID)) {
            actor.sendMessage(Text.literal("§cOnly the division leader can assign classes."));
            return;
        }

        playerState.setSocialClass(targetUUID, className);
        actor.sendMessage(Text.literal("§aSet §e" + target.getName().getString() +
                "§a's class to §e" + className + "§a."));
        target.sendMessage(Text.literal("§aYour social class has been set to §e" + className + "§a."));
    }

    // --- Promote ---

    public static void promote(ServerPlayerEntity actor, ServerWorld world,
                               ServerPlayerEntity target) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String targetUUID = target.getUuid().toString();
        String currentClass = playerState.getSocialClass(targetUUID);
        if (currentClass == null || currentClass.isEmpty()) currentClass = "peasant";

        int currentRank = getClassRank(currentClass);
        if (currentRank >= CLASSES.length - 1) {
            actor.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is already at the highest class."));
            return;
        }

        String newClass = CLASSES[currentRank + 1];
        setClass(actor, world, target, newClass);
    }

    // --- Demote ---

    public static void demote(ServerPlayerEntity actor, ServerWorld world,
                              ServerPlayerEntity target) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String targetUUID = target.getUuid().toString();
        String currentClass = playerState.getSocialClass(targetUUID);
        if (currentClass == null || currentClass.isEmpty()) currentClass = "peasant";

        int currentRank = getClassRank(currentClass);
        if (currentRank <= 0) {
            actor.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is already at the lowest class."));
            return;
        }

        String newClass = CLASSES[currentRank - 1];
        setClass(actor, world, target, newClass);
    }

    // --- View class ---

    public static void showClass(ServerPlayerEntity player, ServerWorld world,
                                 ServerPlayerEntity target) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String className = playerState.getSocialClass(target.getUuid().toString());
        if (className == null || className.isEmpty()) className = "peasant";
        player.sendMessage(Text.literal("§e" + target.getName().getString() +
                "§f's social class: §a" + className));
    }

    // --- Check if player can claim chunks ---

    public static boolean canClaimChunks(ServerWorld world, ServerPlayerEntity player) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String className = playerState.getSocialClass(player.getUuid().toString());
        if (className == null || className.isEmpty()) className = "peasant";
        int rank = getClassRank(className);
        return rank >= 2; // noble or above
    }

    // --- Check if player can found a nation ---

    public static boolean canFoundNation(ServerWorld world, ServerPlayerEntity player) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String className = playerState.getSocialClass(player.getUuid().toString());
        if (className == null || className.isEmpty()) className = "peasant";
        int rank = getClassRank(className);
        return rank >= 3; // royalty only
    }

    // --- Get tax multiplier by class ---

    public static float getTaxMultiplier(String className) {
        if (className == null || className.isEmpty()) return 1.0f;
        switch (className.toLowerCase()) {
            case "peasant":  return 1.5f;
            case "merchant": return 1.0f;
            case "noble":    return 0.5f;
            case "royalty":  return 0.0f;
            default:         return 1.0f;
        }
    }
}