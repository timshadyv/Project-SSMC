package com.sovereignstate.util;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.WorldStateData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public class DivisionHelper {

    // --- Get Division At Tier ---

    public static String getDivisionAtTier(ServerWorld world, String childDivisionID, int targetTier) {
        DivisionData data = DivisionData.get(world);
        String currentID = childDivisionID;

        for (int i = 0; i < 10; i++) {
            NbtCompound div = data.getDivisionById(currentID);
            if (div == null) return "";
            int tier = div.getInt("tier");
            if (tier == targetTier) return currentID;
            String parentID = div.getString("parentDivisionID");
            if (parentID.isEmpty()) return "";
            currentID = parentID;
        }
        return "";
    }

    // --- Inherited Laws ---

    public static List<String> getLawsInheritedByDivision(ServerWorld world, String divisionID) {
        DivisionData data = DivisionData.get(world);
        List<String> mergedLaws = new ArrayList<>();
        String currentID = divisionID;

        for (int i = 0; i < 10; i++) {
            NbtCompound div = data.getDivisionById(currentID);
            if (div == null) break;
            List<String> laws = data.getLaws(currentID);
            for (String law : laws) {
                if (!mergedLaws.contains(law)) mergedLaws.add(law);
            }
            String parentID = div.getString("parentDivisionID");
            if (parentID.isEmpty()) break;
            currentID = parentID;
        }
        return mergedLaws;
    }

    // --- Is Law Active ---

    public static boolean isLawActive(ServerWorld world, String divisionID, String lawName) {
        List<String> laws = getLawsInheritedByDivision(world, divisionID);
        return laws.contains(lawName);
    }

    // --- Leader UUID ---

    public static String getDivisionLeaderUUID(ServerWorld world, String divisionID) {
        DivisionData data = DivisionData.get(world);
        NbtCompound div = data.getDivisionById(divisionID);
        return div != null ? div.getString("leaderUUID") : "";
    }

    // --- Is Player In Division ---

    public static boolean isPlayerInDivision(ServerWorld world, PlayerEntity player, String divisionID) {
        String uuid = player.getUuidAsString();
        WorldStateData wData = WorldStateData.get(world);
        String playerDivID = wData.getTag(NbtHelper.buildPlayerKey(uuid, "divisionID"));
        return divisionID.equals(playerDivID);
    }

    // --- Get Division Governing Chunk ---

    public static String getDivisionGoverningChunk(ServerWorld world, int chunkX, int chunkZ) {
        return ChunkHelper.getChunkDivisionID(world, chunkX, chunkZ);
    }

    // --- Government Type ---

    public static String getDivisionGovernmentType(ServerWorld world, String divisionID) {
        DivisionData data = DivisionData.get(world);
        NbtCompound div = data.getDivisionById(divisionID);
        return div != null ? div.getString("governmentType") : "";
    }

    // --- Gender Government Checks ---

    public static boolean isMaleOnlyGovernment(String governmentType) {
        return governmentType.equals("patriarchal_state") ||
                governmentType.equals("androcracy");
    }

    public static boolean isFemaleOnlyGovernment(String governmentType) {
        return governmentType.equals("matriarchal_state") ||
                governmentType.equals("gynarchy");
    }

    // --- Is Player Leader ---

    public static boolean isPlayerLeader(ServerWorld world, PlayerEntity player, String divisionID) {
        String leaderUUID = getDivisionLeaderUUID(world, divisionID);
        return player.getUuidAsString().equals(leaderUUID);
    }

    // --- Get Division Name ---

    public static String getDivisionName(ServerWorld world, String divisionID) {
        DivisionData data = DivisionData.get(world);
        NbtCompound div = data.getDivisionById(divisionID);
        return div != null ? div.getString("name") : "Unknown";
    }

    // --- Get All Division IDs In Chunk Range ---

    public static List<String> getDivisionIDsInChunkRange(ServerWorld world, int centerX, int centerZ, int radius) {
        List<String> divisionIDs = new ArrayList<>();
        List<int[]> chunks = ChunkHelper.getChunksInRadius(centerX, centerZ, radius);
        for (int[] chunk : chunks) {
            String divID = ChunkHelper.getChunkDivisionID(world, chunk[0], chunk[1]);
            if (!divID.isEmpty() && !divisionIDs.contains(divID)) {
                divisionIDs.add(divID);
            }
        }
        return divisionIDs;
    }

    // --- War Check ---

    public static boolean areAtWar(ServerWorld world, String divisionID1, String divisionID2) {
        WorldStateData data = WorldStateData.get(world);
        return data.getBooleanTag(NbtHelper.buildNationRelationKey(divisionID1, "atWarWith", divisionID2)) ||
                data.getBooleanTag(NbtHelper.buildNationRelationKey(divisionID2, "atWarWith", divisionID1));
    }

    // --- Alliance Check ---

    public static boolean areAllied(ServerWorld world, String divisionID1, String divisionID2) {
        WorldStateData data = WorldStateData.get(world);
        return data.getBooleanTag(NbtHelper.buildNationRelationKey(divisionID1, "alliedWith", divisionID2)) ||
                data.getBooleanTag(NbtHelper.buildNationRelationKey(divisionID2, "alliedWith", divisionID1));
    }
}