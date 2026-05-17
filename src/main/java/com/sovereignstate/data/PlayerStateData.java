package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public class PlayerStateData extends PersistentState {

    private NbtCompound data = new NbtCompound();

    public static PlayerStateData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    PlayerStateData state = new PlayerStateData();
                    state.data = nbt.copy();
                    return state;
                },
                PlayerStateData::new,
                "sovereignstate_players"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.copyFrom(data);
        return nbt;
    }

    private String key(String uuid, String property) {
        return "player_" + uuid + "_" + property;
    }

    // --- Gender ---
    public String getGender(String uuid) { return data.getString(key(uuid, "gender")); }
    public void setGender(String uuid, String gender) { data.putString(key(uuid, "gender"), gender); markDirty(); }

    // --- Gender Registered ---
    public boolean isGenderRegistered(String uuid) { return data.getBoolean(key(uuid, "genderRegistered")); }
    public void setGenderRegistered(String uuid, boolean value) { data.putBoolean(key(uuid, "genderRegistered"), value); markDirty(); }

    // --- Social Class ---
    public String getSocialClass(String uuid) { return data.getString(key(uuid, "socialClass")); }
    public void setSocialClass(String uuid, String socialClass) { data.putString(key(uuid, "socialClass"), socialClass); markDirty(); }

    // --- Culture ---
    public String getCulture(String uuid) { return data.getString(key(uuid, "culture")); }
    public void setCulture(String uuid, String culture) { data.putString(key(uuid, "culture"), culture); markDirty(); }

    // --- Religion ---
    public String getReligion(String uuid) { return data.getString(key(uuid, "religion")); }
    public void setReligion(String uuid, String religion) { data.putString(key(uuid, "religion"), religion); markDirty(); }

    // --- Party ---
    public String getPartyID(String uuid) { return data.getString(key(uuid, "partyID")); }
    public void setPartyID(String uuid, String partyID) { data.putString(key(uuid, "partyID"), partyID); markDirty(); }

    // --- Division ---
    public String getDivisionID(String uuid) { return data.getString(key(uuid, "divisionID")); }
    public void setDivisionID(String uuid, String divisionID) { data.putString(key(uuid, "divisionID"), divisionID); markDirty(); }

    // --- Wanted ---
    public boolean isWanted(String uuid) { return data.getBoolean(key(uuid, "isWanted")); }
    public void setWanted(String uuid, boolean value) { data.putBoolean(key(uuid, "isWanted"), value); markDirty(); }

    // --- Crime Log ---
    public NbtList getCrimeLog(String uuid) {
        String k = key(uuid, "crimeLog");
        return data.contains(k) ? data.getList(k, 8) : new NbtList();
    }
    public void appendCrime(String uuid, String crime) {
        String k = key(uuid, "crimeLog");
        NbtList list = data.contains(k) ? data.getList(k, 8) : new NbtList();
        list.add(NbtString.of(crime));
        data.put(k, list);
        markDirty();
    }
    public void clearCrimeLog(String uuid) { data.remove(key(uuid, "crimeLog")); markDirty(); }

    // --- Bounty ---
    public int getBounty(String uuid) { return data.getInt(key(uuid, "bounty")); }
    public void setBounty(String uuid, int bounty) { data.putInt(key(uuid, "bounty"), bounty); markDirty(); }

    // --- Incarcerated ---
    public boolean isIncarcerated(String uuid) { return data.getBoolean(key(uuid, "isIncarcerated")); }
    public void setIncarcerated(String uuid, boolean value) { data.putBoolean(key(uuid, "isIncarcerated"), value); markDirty(); }

    // --- Sentence Timer ---
    public int getSentenceTimer(String uuid) { return data.getInt(key(uuid, "sentenceTimer")); }
    public void setSentenceTimer(String uuid, int ticks) { data.putInt(key(uuid, "sentenceTimer"), ticks); markDirty(); }

    // --- Jail Spawn ---
    public int getJailSpawnX(String uuid) { return data.getInt(key(uuid, "jailSpawnX")); }
    public int getJailSpawnY(String uuid) { return data.getInt(key(uuid, "jailSpawnY")); }
    public int getJailSpawnZ(String uuid) { return data.getInt(key(uuid, "jailSpawnZ")); }
    public void setJailSpawn(String uuid, int x, int y, int z) {
        data.putInt(key(uuid, "jailSpawnX"), x);
        data.putInt(key(uuid, "jailSpawnY"), y);
        data.putInt(key(uuid, "jailSpawnZ"), z);
        markDirty();
    }

    // --- Home Spawn ---
    public int getHomeSpawnX(String uuid) { return data.getInt(key(uuid, "homeSpawnX")); }
    public int getHomeSpawnY(String uuid) { return data.getInt(key(uuid, "homeSpawnY")); }
    public int getHomeSpawnZ(String uuid) { return data.getInt(key(uuid, "homeSpawnZ")); }
    public void setHomeSpawn(String uuid, int x, int y, int z) {
        data.putInt(key(uuid, "homeSpawnX"), x);
        data.putInt(key(uuid, "homeSpawnY"), y);
        data.putInt(key(uuid, "homeSpawnZ"), z);
        markDirty();
    }

    // --- Loyalty ---
    public boolean isDisloyal(String uuid) { return data.getBoolean(key(uuid, "isDisloyal")); }
    public void setDisloyal(String uuid, boolean value) { data.putBoolean(key(uuid, "isDisloyal"), value); markDirty(); }

    public int getLoyaltyOathTimer(String uuid) { return data.getInt(key(uuid, "loyaltyOathTimer")); }
    public void setLoyaltyOathTimer(String uuid, int ticks) { data.putInt(key(uuid, "loyaltyOathTimer"), ticks); markDirty(); }

    // --- Spy ---
    public boolean isSpy(String uuid) { return data.getBoolean(key(uuid, "isSpy")); }
    public void setSpy(String uuid, boolean value) { data.putBoolean(key(uuid, "isSpy"), value); markDirty(); }
    public boolean isCaught(String uuid) { return data.getBoolean(key(uuid, "isCaught")); }
    public void setCaught(String uuid, boolean value) { data.putBoolean(key(uuid, "isCaught"), value); markDirty(); }

    // --- Military Rank ---
    public String getMilitaryRank(String uuid) { return data.getString(key(uuid, "militaryRank")); }
    public void setMilitaryRank(String uuid, String rank) { data.putString(key(uuid, "militaryRank"), rank); markDirty(); }

    // --- Wallet ---
    public int getWallet(String uuid, String currencyID) { return data.getInt(key(uuid, "wallet_" + currencyID)); }
    public void setWallet(String uuid, String currencyID, int amount) { data.putInt(key(uuid, "wallet_" + currencyID), amount); markDirty(); }
    public void adjustWallet(String uuid, String currencyID, int amount) { setWallet(uuid, currencyID, getWallet(uuid, currencyID) + amount); }

    // --- Marriage ---
    public String getMarriedToUUID(String uuid) { return data.getString(key(uuid, "marriedToUUID")); }
    public void setMarriedToUUID(String uuid, String partnerUUID) { data.putString(key(uuid, "marriedToUUID"), partnerUUID); markDirty(); }

    // --- Prison Labour ---
    public boolean isPrisonLabourActive(String uuid) { return data.getBoolean(key(uuid, "isPrisonLabourActive")); }
    public void setPrisonLabourActive(String uuid, boolean value) { data.putBoolean(key(uuid, "isPrisonLabourActive"), value); markDirty(); }
}