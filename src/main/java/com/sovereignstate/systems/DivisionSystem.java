package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.util.ChunkHelper;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class DivisionSystem {

    // --- Found a new division ---

    public static String foundDivision(ServerPlayerEntity player, ServerWorld world,
                                       String name, String governmentType,
                                       String ideology) {

        DivisionData divData = DivisionData.get(world);
        PlayerStateData playerState = PlayerStateData.get(world);
        String uuid = player.getUuid().toString();

        // Check name not taken
        List<NbtCompound> all = divData.getAllDivisions();
        for (NbtCompound div : all) {
            if (div.getString("name").equalsIgnoreCase(name)) {
                player.sendMessage(Text.literal(
                        "§cA division named '" + name + "' already exists."));
                return null;
            }
        }

        // Create division at tier 1 (village level)
        String divisionID = divData.createDivision(name, 1, governmentType, uuid);

        // Set additional properties
        divData.setLeader(divisionID, uuid, player.getName().getString());
        divData.setMotto(divisionID, "");

        // Assign player to this division
        playerState.setDivisionID(uuid, divisionID);

        // Claim current chunk for this division
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);
        ChunkHelper.setChunkOwner(world, chunkX, chunkZ, player.getName().getString());
        ChunkHelper.setChunkDivisionID(world, chunkX, chunkZ, divisionID);
        ChunkHelper.setChunkCapital(world, chunkX, chunkZ, true);

        // Broadcast to all players
        MinecraftServer server = player.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§6" + player.getName().getString() +
                            " has founded §e" + name +
                            "§6 as a §e" + governmentType + "§6!");
        }

        player.sendMessage(Text.literal("§a--- Division Founded ---"));
        player.sendMessage(Text.literal("§eName: §f" + name));
        player.sendMessage(Text.literal("§eGovernment: §f" + governmentType));
        player.sendMessage(Text.literal("§eYour Division ID: §f" + divisionID));
        player.sendMessage(Text.literal(
                "§aYour current chunk has been set as the capital."));

        return divisionID;
    }

    // --- Dissolve a division ---

    public static void dissolveDivision(ServerPlayerEntity player, ServerWorld world,
                                        String divisionID) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        String leaderUUID = div.getString("leaderUUID");
        if (!player.getUuid().toString().equals(leaderUUID)) {
            player.sendMessage(Text.literal(
                    "§cOnly the division leader can dissolve it."));
            return;
        }

        String name = div.getString("name");
        divData.deleteDivision(divisionID);

        MinecraftServer server = player.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§c" + name + " has been dissolved.");
        }
    }

    // --- Get division info ---

    public static void showDivisionInfo(ServerPlayerEntity player, ServerWorld world,
                                        String divisionID) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Division Info ---"));
        player.sendMessage(Text.literal("§eName: §f" + div.getString("name")));
        player.sendMessage(Text.literal("§eTier: §f" + div.getInt("tier")));
        player.sendMessage(Text.literal(
                "§eGovernment: §f" + div.getString("governmentType")));
        player.sendMessage(Text.literal(
                "§eLeader: §f" + div.getString("leaderName")));
        player.sendMessage(Text.literal(
                "§eApproval: §f" + div.getInt("approvalRating") + "%"));
        player.sendMessage(Text.literal(
                "§eState Religion: §f" + div.getString("stateReligion")));
        player.sendMessage(Text.literal(
                "§eCulture: §f" + div.getString("culture")));
        player.sendMessage(Text.literal(
                "§eMotto: §f" + div.getString("motto")));

        List<String> laws = divData.getLaws(divisionID);
        player.sendMessage(Text.literal("§eActive Laws: §f" + laws.size()));
    }

    // --- Set division leader ---

    public static void setLeader(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, ServerPlayerEntity newLeader) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        String leaderUUID = div.getString("leaderUUID");
        if (!player.getUuid().toString().equals(leaderUUID)) {
            player.sendMessage(Text.literal(
                    "§cOnly the current leader can transfer leadership."));
            return;
        }

        divData.setLeader(divisionID, newLeader.getUuid().toString(),
                newLeader.getName().getString());

        player.sendMessage(Text.literal(
                "§aLeadership transferred to " + newLeader.getName().getString()));
        newLeader.sendMessage(Text.literal(
                "§aYou are now the leader of " + div.getString("name")));
    }

    // --- Join a division ---

    public static void joinDivision(ServerPlayerEntity player, ServerWorld world,
                                    String divisionID) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        PlayerStateData playerState = PlayerStateData.get(world);
        playerState.setDivisionID(player.getUuid().toString(), divisionID);

        player.sendMessage(Text.literal(
                "§aYou have joined §e" + div.getString("name") + "§a."));
    }

    // --- Leave a division ---

    public static void leaveDivision(ServerPlayerEntity player, ServerWorld world) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);

        if (divisionID == null || divisionID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);
        String divName = div != null ? div.getString("name") : "Unknown";

        // Check if leader
        if (div != null && div.getString("leaderUUID").equals(uuid)) {
            player.sendMessage(Text.literal(
                    "§cYou are the leader. Transfer leadership before leaving."));
            return;
        }

        playerState.setDivisionID(uuid, "");
        player.sendMessage(Text.literal("§aYou have left §e" + divName + "§a."));
    }
}