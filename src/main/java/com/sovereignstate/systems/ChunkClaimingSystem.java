package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.util.ChunkHelper;
import com.sovereignstate.util.NbtHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class ChunkClaimingSystem {

    public static void claimChunk(ServerPlayerEntity player, ServerWorld world) {
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        WorldStateData worldState = WorldStateData.get(world);
        PlayerStateData playerState = PlayerStateData.get(world);

        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);
        String playerName = player.getName().getString();

        // Check if already claimed
        String existingOwner = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        if (existingOwner != null && !existingOwner.isEmpty()) {
            player.sendMessage(Text.literal(
                    "§cThis chunk is already claimed by " + existingOwner + "."));
            return;
        }

        // Claim it
        ChunkHelper.setChunkOwner(world, chunkX, chunkZ, playerName);
        ChunkHelper.setChunkDivisionID(world, chunkX, chunkZ, divisionID);
        ChunkHelper.setChunkCulture(world, chunkX, chunkZ,
                playerState.getCulture(uuid));

        // Get division name for message
        String divisionName = "your territory";
        if (divisionID != null && !divisionID.isEmpty()) {
            DivisionData divData = DivisionData.get(world);
            NbtCompound div = divData.getDivisionById(divisionID);
            if (div != null) divisionName = div.getString("name");
        }

        player.sendMessage(Text.literal(
                "§aChunk (" + chunkX + ", " + chunkZ + ") claimed for " + divisionName + "."));
        worldState.markDirty();
    }

    public static void abandonChunk(ServerPlayerEntity player, ServerWorld world) {
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        String existingOwner = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        String playerName = player.getName().getString();

        if (existingOwner == null || existingOwner.isEmpty()) {
            player.sendMessage(Text.literal("§cThis chunk is not claimed."));
            return;
        }

        // Check if player is owner or division leader
        PlayerStateData playerState = PlayerStateData.get(world);
        String uuid = player.getUuid().toString();
        String divisionID = ChunkHelper.getChunkDivisionID(world, chunkX, chunkZ);
        String playerDivisionID = playerState.getDivisionID(uuid);

        boolean isOwner = existingOwner.equals(playerName);
        boolean isLeader = false;

        if (divisionID != null && !divisionID.isEmpty()) {
            DivisionData divData = DivisionData.get(world);
            String leaderUUID = divData.getLeaderUUID(divisionID);
            isLeader = uuid.equals(leaderUUID);
        }

        if (!isOwner && !isLeader) {
            player.sendMessage(Text.literal(
                    "§cYou do not have permission to abandon this chunk."));
            return;
        }

        // Clear all chunk tags
        WorldStateData worldState = WorldStateData.get(world);
        worldState.removeTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "owner"));
        worldState.removeTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "divisionID"));
        worldState.removeTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "culture"));
        worldState.removeTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isJailZone"));
        worldState.removeTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isCapital"));

        player.sendMessage(Text.literal(
                "§aChunk (" + chunkX + ", " + chunkZ + ") has been abandoned."));
    }

    public static void setCapital(ServerPlayerEntity player, ServerWorld world) {
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        String owner = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        String playerName = player.getName().getString();

        if (!playerName.equals(owner)) {
            player.sendMessage(Text.literal("§cYou do not own this chunk."));
            return;
        }

        ChunkHelper.setChunkCapital(world, chunkX, chunkZ, true);
        player.sendMessage(Text.literal(
                "§aChunk (" + chunkX + ", " + chunkZ + ") set as capital."));
    }

    public static void setJailZone(ServerPlayerEntity player, ServerWorld world) {
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        String owner = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        String playerName = player.getName().getString();

        if (!playerName.equals(owner)) {
            player.sendMessage(Text.literal("§cYou do not own this chunk."));
            return;
        }

        ChunkHelper.setChunkJailZone(world, chunkX, chunkZ, true);
        player.sendMessage(Text.literal(
                "§aChunk (" + chunkX + ", " + chunkZ + ") designated as jail zone."));
    }

    public static int getClaimedChunkCount(ServerWorld world, String divisionID) {
        List<int[]> chunks = ChunkHelper.getAllOwnedChunksByDivision(world, divisionID);
        return chunks.size();
    }
}