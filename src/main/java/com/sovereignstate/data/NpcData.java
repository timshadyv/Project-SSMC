package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

public class NpcData extends PersistentState {

    private NbtList npcs = new NbtList();

    public static NpcData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    NpcData state = new NpcData();
                    state.npcs = nbt.getList("npcs", 10);
                    return state;
                },
                NpcData::new,
                "sovereignstate_npcs"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("npcs", npcs);
        return nbt;
    }

    // --- Register ---

    public void registerNpc(String id, String name, String type, String gender,
                            String culture, String religion, String divisionID,
                            int birthChunkX, int birthChunkZ) {
        NbtCompound npc = new NbtCompound();
        npc.putString("id", id);
        npc.putString("name", name);
        npc.putString("type", type);
        npc.putString("gender", gender);
        npc.putString("culture", culture);
        npc.putString("religion", religion);
        npc.putString("socialClass", "peasant");
        npc.putString("divisionID", divisionID);
        npc.putInt("happiness", 50);
        npc.putBoolean("isWanted", false);
        npc.putBoolean("isIncarcerated", false);
        npc.putBoolean("isSuppressedCulture", false);
        npc.putInt("loyaltyToLeader", 50);
        npc.putInt("birthChunkX", birthChunkX);
        npc.putInt("birthChunkZ", birthChunkZ);
        npc.putInt("assignedWorkBlockX", 0);
        npc.putInt("assignedWorkBlockY", 64);
        npc.putInt("assignedWorkBlockZ", 0);
        npcs.add(npc);
        markDirty();
    }

    // --- Get ---

    public NbtCompound getNpcById(String id) {
        for (int i = 0; i < npcs.size(); i++) {
            NbtCompound npc = npcs.getCompound(i);
            if (npc.getString("id").equals(id)) return npc;
        }
        return null;
    }

    public List<NbtCompound> getAllNpcs() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < npcs.size(); i++) list.add(npcs.getCompound(i));
        return list;
    }

    public List<NbtCompound> getNpcsByType(String type) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < npcs.size(); i++) {
            NbtCompound npc = npcs.getCompound(i);
            if (npc.getString("type").equals(type)) list.add(npc);
        }
        return list;
    }

    public List<NbtCompound> getNpcsByDivision(String divisionID) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < npcs.size(); i++) {
            NbtCompound npc = npcs.getCompound(i);
            if (npc.getString("divisionID").equals(divisionID)) list.add(npc);
        }
        return list;
    }

    public List<NbtCompound> getNpcsByChunk(int chunkX, int chunkZ) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < npcs.size(); i++) {
            NbtCompound npc = npcs.getCompound(i);
            if (npc.getInt("birthChunkX") == chunkX &&
                    npc.getInt("birthChunkZ") == chunkZ) list.add(npc);
        }
        return list;
    }

    // --- Happiness ---

    public void adjustHappiness(String id, int amount) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        int current = npc.getInt("happiness");
        npc.putInt("happiness", Math.max(0, Math.min(100, current + amount)));
        markDirty();
    }

    public double getAverageHappinessByDivision(String divisionID) {
        List<NbtCompound> divNpcs = getNpcsByDivision(divisionID);
        if (divNpcs.isEmpty()) return 50.0;
        int total = 0;
        for (NbtCompound npc : divNpcs) total += npc.getInt("happiness");
        return (double) total / divNpcs.size();
    }

    // --- Type Conversion ---

    public void convertNpcType(String id, String newType) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putString("type", newType);
        markDirty();
    }

    // --- Status Setters ---

    public void setWanted(String id, boolean value) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putBoolean("isWanted", value);
        markDirty();
    }

    public void setIncarcerated(String id, boolean value) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putBoolean("isIncarcerated", value);
        markDirty();
    }

    public void setSuppressedCulture(String id, boolean value) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putBoolean("isSuppressedCulture", value);
        markDirty();
    }

    public void setSocialClass(String id, String socialClass) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putString("socialClass", socialClass);
        markDirty();
    }

    public void setLoyalty(String id, int loyalty) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putInt("loyaltyToLeader", Math.max(0, Math.min(100, loyalty)));
        markDirty();
    }

    public void setWallet(String id, String currencyID, int amount) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putInt("wallet_" + currencyID, amount);
        markDirty();
    }

    public int getWallet(String id, String currencyID) {
        NbtCompound npc = getNpcById(id);
        return npc != null ? npc.getInt("wallet_" + currencyID) : 0;
    }

    public void setAssignedWorkBlock(String id, int x, int y, int z) {
        NbtCompound npc = getNpcById(id);
        if (npc == null) return;
        npc.putInt("assignedWorkBlockX", x);
        npc.putInt("assignedWorkBlockY", y);
        npc.putInt("assignedWorkBlockZ", z);
        markDirty();
    }

    // --- Remove ---

    public void removeNpc(String id) {
        npcs.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }
}