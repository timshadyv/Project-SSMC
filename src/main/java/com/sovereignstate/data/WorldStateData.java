package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public class WorldStateData extends PersistentState {

    private NbtCompound data = new NbtCompound();

    // --- Core ---

    public static WorldStateData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    WorldStateData state = new WorldStateData();
                    state.data = nbt.copy();
                    return state;
                },
                WorldStateData::new,
                "sovereignstate_world"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.copyFrom(data);
        return nbt;
    }

    // --- String tags ---

    public String getTag(String key) {
        return data.contains(key) ? data.getString(key) : "";
    }

    public void setTag(String key, String value) {
        data.putString(key, value);
        markDirty();
    }

    // --- Boolean tags ---

    public boolean getBooleanTag(String key) {
        return data.contains(key) && data.getBoolean(key);
    }

    public void setBooleanTag(String key, boolean value) {
        data.putBoolean(key, value);
        markDirty();
    }

    // --- Int tags ---

    public int getIntTag(String key) {
        return data.contains(key) ? data.getInt(key) : 0;
    }

    public void setIntTag(String key, int value) {
        data.putInt(key, value);
        markDirty();
    }

    // --- Float tags ---

    public float getFloatTag(String key) {
        return data.contains(key) ? data.getFloat(key) : 0f;
    }

    public void setFloatTag(String key, float value) {
        data.putFloat(key, value);
        markDirty();
    }

    // --- List tags ---

    public NbtList getListTag(String key) {
        return data.contains(key) ? data.getList(key, 8) : new NbtList();
    }

    public void appendToList(String key, String value) {
        NbtList list = getListTag(key);
        list.add(NbtString.of(value));
        data.put(key, list);
        markDirty();
    }

    public void removeFromList(String key, String value) {
        NbtList list = getListTag(key);
        list.removeIf(tag -> tag.asString().equals(value));
        data.put(key, list);
        markDirty();
    }

    public boolean listContains(String key, String value) {
        NbtList list = getListTag(key);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return true;
        }
        return false;
    }

    // --- Remove tag ---

    public void removeTag(String key) {
        data.remove(key);
        markDirty();
    }

    public boolean hasTag(String key) {
        return data.contains(key);
    }
}