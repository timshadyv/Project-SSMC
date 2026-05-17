package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DivisionData extends PersistentState {

    private NbtList divisions = new NbtList();

    public static DivisionData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    DivisionData state = new DivisionData();
                    state.divisions = nbt.getList("divisions", 10);
                    return state;
                },
                DivisionData::new,
                "sovereignstate_divisions"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("divisions", divisions);
        return nbt;
    }

    // --- Create / Delete ---

    public String createDivision(String name, int tier, String governmentType, String leaderUUID) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound div = new NbtCompound();
        div.putString("id", id);
        div.putString("name", name);
        div.putInt("tier", tier);
        div.putString("governmentType", governmentType);
        div.putString("leaderUUID", leaderUUID);
        div.putString("leaderName", "");
        div.putString("culture", "");
        div.putString("stateReligion", "");
        div.putString("genderPolicy", "egalitarian");
        div.putString("officialCurrencyID", "");
        div.putString("parentDivisionID", "");
        div.putString("motto", "");
        div.putInt("approvalRating", 50);
        div.putInt("population", 0);
        div.putBoolean("isAtWar", false);
        div.put("activeLaws", new NbtList());
        div.put("constitution", new NbtList());
        divisions.add(div);
        markDirty();
        return id;
    }

    public void deleteDivision(String id) {
        divisions.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }

    // --- Get ---

    public NbtCompound getDivisionById(String id) {
        for (int i = 0; i < divisions.size(); i++) {
            NbtCompound div = divisions.getCompound(i);
            if (div.getString("id").equals(id)) return div;
        }
        return null;
    }

    public List<NbtCompound> getAllDivisions() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < divisions.size(); i++) {
            list.add(divisions.getCompound(i));
        }
        return list;
    }

    public List<NbtCompound> getDivisionsByTier(int tier) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < divisions.size(); i++) {
            NbtCompound div = divisions.getCompound(i);
            if (div.getInt("tier") == tier) list.add(div);
        }
        return list;
    }

    public List<NbtCompound> getChildDivisions(String parentID) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < divisions.size(); i++) {
            NbtCompound div = divisions.getCompound(i);
            if (div.getString("parentDivisionID").equals(parentID)) list.add(div);
        }
        return list;
    }

    // --- Laws ---

    public void addLaw(String divisionID, String law) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        NbtList laws = div.getList("activeLaws", 8);
        laws.add(NbtString.of(law));
        div.put("activeLaws", laws);
        markDirty();
    }

    public void removeLaw(String divisionID, String law) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        NbtList laws = div.getList("activeLaws", 8);
        laws.removeIf(tag -> tag.asString().equals(law));
        div.put("activeLaws", laws);
        markDirty();
    }

    public boolean hasLaw(String divisionID, String law) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return false;
        NbtList laws = div.getList("activeLaws", 8);
        for (int i = 0; i < laws.size(); i++) {
            if (laws.getString(i).equals(law)) return true;
        }
        return false;
    }

    public List<String> getLaws(String divisionID) {
        NbtCompound div = getDivisionById(divisionID);
        List<String> list = new ArrayList<>();
        if (div == null) return list;
        NbtList laws = div.getList("activeLaws", 8);
        for (int i = 0; i < laws.size(); i++) list.add(laws.getString(i));
        return list;
    }

    // --- Constitution ---

    public void addConstitutionClause(String divisionID, String clause) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        NbtList constitution = div.getList("constitution", 8);
        constitution.add(NbtString.of(clause));
        div.put("constitution", constitution);
        markDirty();
    }

    public List<String> getConstitution(String divisionID) {
        NbtCompound div = getDivisionById(divisionID);
        List<String> list = new ArrayList<>();
        if (div == null) return list;
        NbtList constitution = div.getList("constitution", 8);
        for (int i = 0; i < constitution.size(); i++) list.add(constitution.getString(i));
        return list;
    }

    // --- Leader ---

    public void setLeader(String divisionID, String leaderUUID, String leaderName) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("leaderUUID", leaderUUID);
        div.putString("leaderName", leaderName);
        markDirty();
    }

    public String getLeaderUUID(String divisionID) {
        NbtCompound div = getDivisionById(divisionID);
        return div != null ? div.getString("leaderUUID") : "";
    }

    // --- Approval ---

    public void adjustApproval(String divisionID, int amount) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        int current = div.getInt("approvalRating");
        div.putInt("approvalRating", Math.max(0, Math.min(100, current + amount)));
        markDirty();
    }

    public int getApproval(String divisionID) {
        NbtCompound div = getDivisionById(divisionID);
        return div != null ? div.getInt("approvalRating") : 0;
    }

    // --- Treasury ---

    public void adjustTreasury(String divisionID, String currencyID, int amount) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        String key = "treasury_" + currencyID;
        div.putInt(key, div.getInt(key) + amount);
        markDirty();
    }

    public int getTreasury(String divisionID, String currencyID) {
        NbtCompound div = getDivisionById(divisionID);
        return div != null ? div.getInt("treasury_" + currencyID) : 0;
    }

    // --- Setters ---

    public void setGenderPolicy(String divisionID, String policy) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("genderPolicy", policy);
        markDirty();
    }

    public void setCulture(String divisionID, String culture) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("culture", culture);
        markDirty();
    }

    public void setStateReligion(String divisionID, String religion) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("stateReligion", religion);
        markDirty();
    }

    public void setOfficialCurrency(String divisionID, String currencyID) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("officialCurrencyID", currencyID);
        markDirty();
    }

    public void setParentDivision(String divisionID, String parentID) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("parentDivisionID", parentID);
        markDirty();
    }

    public void setAtWar(String divisionID, boolean atWar) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putBoolean("isAtWar", atWar);
        markDirty();
    }

    public void setMotto(String divisionID, String motto) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("motto", motto);
        markDirty();
    }

    public void setGovernmentType(String divisionID, String type) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putString("governmentType", type);
        markDirty();
    }

    public void setFlagData(String divisionID, byte[] flagData) {
        NbtCompound div = getDivisionById(divisionID);
        if (div == null) return;
        div.putByteArray("flagData", flagData);
        markDirty();
    }

    public byte[] getFlagData(String divisionID) {
        NbtCompound div = getDivisionById(divisionID);
        return div != null ? div.getByteArray("flagData") : new byte[512];
    }
}