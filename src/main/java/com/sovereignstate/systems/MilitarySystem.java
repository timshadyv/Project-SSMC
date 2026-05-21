package com.sovereignstate.systems;

import com.sovereignstate.data.ArmyData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.util.ChunkHelper;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class MilitarySystem {

    // ─── Ranks (in ascending order) ───────────────────────────────────────────

    public static final String[] RANKS = {
            "CIVILIAN", "RECRUIT", "SOLDIER", "SERGEANT",
            "LIEUTENANT", "CAPTAIN", "GENERAL"
    };

    public static int getRankLevel(String rank) {
        for (int i = 0; i < RANKS.length; i++) {
            if (RANKS[i].equalsIgnoreCase(rank)) return i;
        }
        return 0;
    }

    public static boolean outranks(String rankA, String rankB) {
        return getRankLevel(rankA) > getRankLevel(rankB);
    }

    // ─── Unit Types ───────────────────────────────────────────────────────────

    public static final String[] UNIT_TYPES = { "INFANTRY", "ARCHER", "CAVALRY" };

    public static boolean isValidUnitType(String type) {
        for (String t : UNIT_TYPES) if (t.equalsIgnoreCase(type)) return true;
        return false;
    }

    // ─── Raise Army ───────────────────────────────────────────────────────────

    /**
     * Leader raises a new army. Costs population from the division.
     * unitCount cannot exceed current division population.
     */
    public static void raiseArmy(ServerPlayerEntity player, ServerWorld world,
                                 String name, String unitType, int unitCount) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can raise armies."));
            return;
        }
        if (!isValidUnitType(unitType)) {
            player.sendMessage(Text.literal("§cInvalid unit type. Choose: INFANTRY, ARCHER, CAVALRY."));
            return;
        }
        if (unitCount < 1) {
            player.sendMessage(Text.literal("§cUnit count must be at least 1."));
            return;
        }

        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);
        if (divID == null || divID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divID);
        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        int population = div.getInt("population");
        if (unitCount > population) {
            player.sendMessage(Text.literal(
                    "§cNot enough population. You have §e" + population +
                            "§c available, requested §e" + unitCount + "§c."));
            return;
        }

        // Deduct population
        div.putInt("population", population - unitCount);
        divData.markDirty();

        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        String armyID = ArmyData.get(world).createArmy(divID, name, unitType.toUpperCase(),
                unitCount, uuid, chunkX, chunkZ);

        player.sendMessage(Text.literal("§a--- Army Raised ---"));
        player.sendMessage(Text.literal("§eName: §f" + name));
        player.sendMessage(Text.literal("§eType: §f" + unitType.toUpperCase()));
        player.sendMessage(Text.literal("§eUnits: §f" + unitCount));
        player.sendMessage(Text.literal("§eArmy ID: §f" + armyID));
        player.sendMessage(Text.literal("§eStationed at chunk: §f" + chunkX + ", " + chunkZ));
    }

    // ─── Disband Army ─────────────────────────────────────────────────────────

    /**
     * Disbands an army and returns its units to the division population.
     */
    public static void disbandArmy(ServerPlayerEntity player, ServerWorld world, String armyID) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can disband armies."));
            return;
        }

        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);
        ArmyData armyData = ArmyData.get(world);
        NbtCompound army = armyData.getArmyById(armyID);

        if (army == null) {
            player.sendMessage(Text.literal("§cArmy not found."));
            return;
        }
        if (!army.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThat army does not belong to your division."));
            return;
        }

        // Return units to population
        int returning = army.getInt("unitCount");
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divID);
        if (div != null) {
            div.putInt("population", div.getInt("population") + returning);
            divData.markDirty();
        }

        String armyName = army.getString("name");
        armyData.deleteArmy(armyID);

        player.sendMessage(Text.literal("§aArmy §e" + armyName +
                "§a disbanded. §e" + returning + "§a units returned to population."));
    }

    // ─── Deploy Army ──────────────────────────────────────────────────────────

    /**
     * Moves an army to the player's current chunk position.
     * Requires CAPTAIN rank or higher, or being the leader.
     */
    public static void deployArmy(ServerPlayerEntity player, ServerWorld world, String armyID) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);
        if (divID == null || divID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        ArmyData armyData = ArmyData.get(world);
        NbtCompound army = armyData.getArmyById(armyID);
        if (army == null) {
            player.sendMessage(Text.literal("§cArmy not found."));
            return;
        }
        if (!army.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThat army does not belong to your division."));
            return;
        }

        String rank = PlayerStateData.get(world).getMilitaryRank(uuid);
        boolean isLeader = DivisionSystem.isLeader(world, player);
        boolean isGeneral = army.getString("generalUUID").equals(uuid);
        boolean hasRank = getRankLevel(rank) >= getRankLevel("CAPTAIN");

        if (!isLeader && !isGeneral && !hasRank) {
            player.sendMessage(Text.literal("§cYou need CAPTAIN rank or higher to deploy armies."));
            return;
        }

        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);
        armyData.setPosition(armyID, chunkX, chunkZ);
        armyData.setDeployed(armyID, true);

        player.sendMessage(Text.literal("§a⚔ Army §e" + army.getString("name") +
                "§a deployed to chunk §e" + chunkX + ", " + chunkZ + "§a."));

        // Notify all online players in the same division
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
                String theirDiv = PlayerStateData.get(world).getDivisionID(online.getUuid().toString());
                if (divID.equals(theirDiv) && !online.equals(player)) {
                    online.sendMessage(Text.literal("§6[Military] §e" + army.getString("name") +
                            "§6 has been deployed to §e" + chunkX + ", " + chunkZ + "§6."));
                }
            }
        }
    }

    // ─── Assign General ───────────────────────────────────────────────────────

    /**
     * Leader assigns a player as general of an army and promotes them to GENERAL rank.
     */
    public static void assignGeneral(ServerPlayerEntity player, ServerWorld world,
                                     String armyID, ServerPlayerEntity target) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can assign generals."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        ArmyData armyData = ArmyData.get(world);
        NbtCompound army = armyData.getArmyById(armyID);

        if (army == null) {
            player.sendMessage(Text.literal("§cArmy not found."));
            return;
        }
        if (!army.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThat army does not belong to your division."));
            return;
        }

        String targetUUID = target.getUuid().toString();
        String targetDivID = PlayerStateData.get(world).getDivisionID(targetUUID);
        if (!divID.equals(targetDivID)) {
            player.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is not in your division."));
            return;
        }

        armyData.setGeneral(armyID, targetUUID);
        PlayerStateData.get(world).setMilitaryRank(targetUUID, "GENERAL");

        player.sendMessage(Text.literal("§a" + target.getName().getString() +
                " is now the General of §e" + army.getString("name") + "§a."));
        target.sendMessage(Text.literal("§6You have been appointed General of §e" +
                army.getString("name") + "§6!"));
    }

    // ─── Conscript Player ─────────────────────────────────────────────────────

    /**
     * Leader or general conscripts a division member into military service (sets rank to RECRUIT).
     */
    public static void conscriptPlayer(ServerPlayerEntity player, ServerWorld world,
                                       ServerPlayerEntity target) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);
        if (divID == null || divID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        boolean isLeader = DivisionSystem.isLeader(world, player);
        String rank = PlayerStateData.get(world).getMilitaryRank(uuid);
        if (!isLeader && getRankLevel(rank) < getRankLevel("GENERAL")) {
            player.sendMessage(Text.literal("§cOnly the leader or a General can conscript players."));
            return;
        }

        String targetUUID = target.getUuid().toString();
        String targetDivID = PlayerStateData.get(world).getDivisionID(targetUUID);
        if (!divID.equals(targetDivID)) {
            player.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is not in your division."));
            return;
        }

        PlayerStateData.get(world).setMilitaryRank(targetUUID, "RECRUIT");
        player.sendMessage(Text.literal("§a" + target.getName().getString() +
                " has been conscripted as a §eRECRUIT§a."));
        target.sendMessage(Text.literal("§cYou have been conscripted into the military as a RECRUIT."));
    }

    // ─── Promote / Demote ─────────────────────────────────────────────────────

    public static void setRank(ServerPlayerEntity player, ServerWorld world,
                               ServerPlayerEntity target, String newRank) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can set military ranks."));
            return;
        }
        if (getRankLevel(newRank) < 0 || getRankLevel(newRank) == 0 && !newRank.equalsIgnoreCase("CIVILIAN")) {
            player.sendMessage(Text.literal("§cInvalid rank. Valid ranks: CIVILIAN, RECRUIT, SOLDIER, SERGEANT, LIEUTENANT, CAPTAIN, GENERAL"));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        String targetDivID = PlayerStateData.get(world).getDivisionID(target.getUuid().toString());
        if (!divID.equals(targetDivID)) {
            player.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is not in your division."));
            return;
        }

        PlayerStateData.get(world).setMilitaryRank(target.getUuid().toString(), newRank.toUpperCase());
        player.sendMessage(Text.literal("§a" + target.getName().getString() +
                "'s rank set to §e" + newRank.toUpperCase() + "§a."));
        target.sendMessage(Text.literal("§aYour military rank has been set to §e" +
                newRank.toUpperCase() + "§a."));
    }

    // ─── Reinforce Army ───────────────────────────────────────────────────────

    /**
     * Adds more units to an existing army, drawing from division population.
     */
    public static void reinforceArmy(ServerPlayerEntity player, ServerWorld world,
                                     String armyID, int count) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can reinforce armies."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        ArmyData armyData = ArmyData.get(world);
        NbtCompound army = armyData.getArmyById(armyID);

        if (army == null) {
            player.sendMessage(Text.literal("§cArmy not found."));
            return;
        }
        if (!army.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThat army does not belong to your division."));
            return;
        }

        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divID);
        if (div == null) return;

        int pop = div.getInt("population");
        if (count > pop) {
            player.sendMessage(Text.literal("§cNot enough population. Available: §e" + pop));
            return;
        }

        div.putInt("population", pop - count);
        divData.markDirty();
        armyData.adjustUnitCount(armyID, count);

        player.sendMessage(Text.literal("§a+§e" + count + "§a units added to §e" +
                army.getString("name") + "§a. New total: §e" +
                armyData.getArmyById(armyID).getInt("unitCount")));
    }

    // ─── Info ─────────────────────────────────────────────────────────────────

    public static void listArmies(ServerPlayerEntity player, ServerWorld world) {
        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (divID == null || divID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        List<NbtCompound> armies = ArmyData.get(world).getArmiesByDivision(divID);
        if (armies.isEmpty()) {
            player.sendMessage(Text.literal("§eYour division has no armies."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Your Armies ---"));
        for (NbtCompound a : armies) {
            String deployed = a.getBoolean("isDeployed") ? "§aDeployed" : "§7Garrisoned";
            player.sendMessage(Text.literal(
                    "§e" + a.getString("name") +
                            " §7[" + a.getString("unitType") + "]" +
                            " §fUnits: " + a.getInt("unitCount") +
                            " | " + deployed +
                            " §7ID: " + a.getString("id")));
        }
    }

    public static void showArmyInfo(ServerPlayerEntity player, ServerWorld world, String armyID) {
        NbtCompound army = ArmyData.get(world).getArmyById(armyID);
        if (army == null) {
            player.sendMessage(Text.literal("§cArmy not found."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (!army.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThat army does not belong to your division."));
            return;
        }

        String generalUUID = army.getString("generalUUID");
        String generalName = "None";
        ServerPlayerEntity general = player.getServer() != null
                ? player.getServer().getPlayerManager().getPlayer(
                net.minecraft.util.math.MathHelper.ceilDiv(0, 1) == 0
                ? generalUUID : generalUUID)
                : null;
        // Look up general name from online players or fallback to UUID
        if (player.getServer() != null) {
            for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
                if (p.getUuid().toString().equals(generalUUID)) {
                    generalName = p.getName().getString();
                    break;
                }
            }
        }

        player.sendMessage(Text.literal("§6--- Army Info ---"));
        player.sendMessage(Text.literal("§eName: §f" + army.getString("name")));
        player.sendMessage(Text.literal("§eID: §f" + army.getString("id")));
        player.sendMessage(Text.literal("§eUnit Type: §f" + army.getString("unitType")));
        player.sendMessage(Text.literal("§eUnit Count: §f" + army.getInt("unitCount")));
        player.sendMessage(Text.literal("§eGeneral: §f" + generalName));
        player.sendMessage(Text.literal("§eDeployed: §f" + (army.getBoolean("isDeployed") ? "Yes" : "No")));
        player.sendMessage(Text.literal("§ePosition: §fChunk " +
                army.getInt("chunkX") + ", " + army.getInt("chunkZ")));
    }

    public static void showMyRank(ServerPlayerEntity player, ServerWorld world) {
        String rank = PlayerStateData.get(world).getMilitaryRank(player.getUuid().toString());
        if (rank == null || rank.isEmpty()) rank = "CIVILIAN";
        player.sendMessage(Text.literal("§eYour military rank: §f" + rank));
    }
}