package com.sovereignstate.util;

import com.sovereignstate.data.WorldStateData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public class ChunkHelper {

    // --- Player Chunk Position ---

    public static int getChunkX(PlayerEntity player) {
        return (int) Math.floor(player.getX() / 16);
    }

    public static int getChunkZ(PlayerEntity player) {
        return (int) Math.floor(player.getZ() / 16);
    }

    // --- Chunk Ownership ---

    public static String getChunkOwner(ServerWorld world, int chunkX, int chunkZ) {
        WorldStateData data = WorldStateData.get(world);
        return data.getTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "owner"));
    }

    public static void setChunkOwner(ServerWorld world, int chunkX, int chunkZ, String ownerName) {
        WorldStateData data = WorldStateData.get(world);
        data.setTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "owner"), ownerName);
    }

    public static boolean isChunkOwned(ServerWorld world, int chunkX, int chunkZ) {
        String owner = getChunkOwner(world, chunkX, chunkZ);
        return owner != null && !owner.isEmpty();
    }

    // --- Chunk Division ---

    public static String getChunkDivisionID(ServerWorld world, int chunkX, int chunkZ) {
        WorldStateData data = WorldStateData.get(world);
        return data.getTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "divisionID"));
    }

    public static void setChunkDivisionID(ServerWorld world, int chunkX, int chunkZ, String divisionID) {
        WorldStateData data = WorldStateData.get(world);
        data.setTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "divisionID"), divisionID);
    }

    // --- Jail Zone ---

    public static boolean isChunkInJailZone(ServerWorld world, int chunkX, int chunkZ) {
        WorldStateData data = WorldStateData.get(world);
        return data.getBooleanTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isJailZone"));
    }

    public static void setChunkJailZone(ServerWorld world, int chunkX, int chunkZ, boolean isJail) {
        WorldStateData data = WorldStateData.get(world);
        data.setBooleanTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isJailZone"), isJail);
    }

    // --- Capital ---

    public static boolean isChunkCapital(ServerWorld world, int chunkX, int chunkZ) {
        WorldStateData data = WorldStateData.get(world);
        return data.getBooleanTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isCapital"));
    }

    public static void setChunkCapital(ServerWorld world, int chunkX, int chunkZ, boolean isCapital) {
        WorldStateData data = WorldStateData.get(world);
        data.setBooleanTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "isCapital"), isCapital);
    }

    // --- Get All Owned Chunks By Division ---

    public static List<int[]> getAllOwnedChunksByDivision(ServerWorld world, String divisionID) {
        WorldStateData data = WorldStateData.get(world);
        List<int[]> chunks = new ArrayList<>();
        // Search a reasonable range around origin
        for (int x = -500; x <= 500; x++) {
            for (int z = -500; z <= 500; z++) {
                String chunkDivID = data.getTag(NbtHelper.buildChunkKey(x, z, "divisionID"));
                if (divisionID.equals(chunkDivID)) {
                    chunks.add(new int[]{x, z});
                }
            }
        }
        return chunks;
    }

    // --- Chunk Radius ---

    public static List<int[]> getChunksInRadius(int centerX, int centerZ, int radius) {
        List<int[]> chunks = new ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                chunks.add(new int[]{x, z});
            }
        }
        return chunks;
    }

    // --- Culture ---

    public static String getChunkCulture(ServerWorld world, int chunkX, int chunkZ) {
        WorldStateData data = WorldStateData.get(world);
        return data.getTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "culture"));
    }

    public static void setChunkCulture(ServerWorld world, int chunkX, int chunkZ, String culture) {
        WorldStateData data = WorldStateData.get(world);
        data.setTag(NbtHelper.buildChunkKey(chunkX, chunkZ, "culture"), culture);
    }
}