package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArmyData extends PersistentState {

    private NbtList armies = new NbtList();

    public static ArmyData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    ArmyData state = new ArmyData();
                    state.armies = nbt.getList("armies", 10);
                    return state;
                },
                ArmyData::new,
                "sovereignstate_armies"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("armies", armies);
        return nbt;
    }

    // ─── Create / Delete ──────────────────────────────────────────────────────

    public String createArmy(String divisionID, String name, String unitType,
                             int unitCount, String generalUUID, int chunkX, int chunkZ) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound army = new NbtCompound();
        army.putString("id", id);
        army.putString("divisionID", divisionID);
        army.putString("name", name);
        army.putString("unitType", unitType);
        army.putInt("unitCount", unitCount);
        army.putString("generalUUID", generalUUID);
        army.putInt("chunkX", chunkX);
        army.putInt("chunkZ", chunkZ);
        army.putBoolean("isDeployed", false);
        armies.add(army);
        markDirty();
        return id;
    }

    public void deleteArmy(String id) {
        armies.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }

    // ─── Get ──────────────────────────────────────────────────────────────────

    public NbtCompound getArmyById(String id) {
        for (int i = 0; i < armies.size(); i++) {
            NbtCompound a = armies.getCompound(i);
            if (a.getString("id").equals(id)) return a;
        }
        return null;
    }

    public List<NbtCompound> getArmiesByDivision(String divisionID) {
        List<NbtCompound> result = new ArrayList<>();
        for (int i = 0; i < armies.size(); i++) {
            NbtCompound a = armies.getCompound(i);
            if (a.getString("divisionID").equals(divisionID)) result.add(a);
        }
        return result;
    }

    public List<NbtCompound> getAllArmies() {
        List<NbtCompound> result = new ArrayList<>();
        for (int i = 0; i < armies.size(); i++) result.add(armies.getCompound(i));
        return result;
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    public void setUnitCount(String id, int count) {
        NbtCompound a = getArmyById(id);
        if (a == null) return;
        a.putInt("unitCount", Math.max(0, count));
        markDirty();
    }

    public void adjustUnitCount(String id, int delta) {
        NbtCompound a = getArmyById(id);
        if (a == null) return;
        a.putInt("unitCount", Math.max(0, a.getInt("unitCount") + delta));
        markDirty();
    }

    public void setDeployed(String id, boolean deployed) {
        NbtCompound a = getArmyById(id);
        if (a == null) return;
        a.putBoolean("isDeployed", deployed);
        markDirty();
    }

    public void setPosition(String id, int chunkX, int chunkZ) {
        NbtCompound a = getArmyById(id);
        if (a == null) return;
        a.putInt("chunkX", chunkX);
        a.putInt("chunkZ", chunkZ);
        markDirty();
    }

    public void setGeneral(String id, String generalUUID) {
        NbtCompound a = getArmyById(id);
        if (a == null) return;
        a.putString("generalUUID", generalUUID);
        markDirty();
    }
}