package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseData extends PersistentState {

    private NbtList cases = new NbtList();

    public static CaseData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    CaseData state = new CaseData();
                    state.cases = nbt.getList("cases", 10);
                    return state;
                },
                CaseData::new,
                "sovereignstate_cases"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("cases", cases);
        return nbt;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public String createCase(String divisionID, String defendantUUID,
                             String plaintiffUUID, String chargesSummary) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound c = new NbtCompound();
        c.putString("id", id);
        c.putString("divisionID", divisionID);
        c.putString("defendantUUID", defendantUUID);
        c.putString("plaintiffUUID", plaintiffUUID);   // player UUID or "DIVISION"
        c.putString("judgeUUID", "");
        c.putString("status", "OPEN");                 // OPEN, IN_TRIAL, CLOSED
        c.putString("verdict", "PENDING");             // PENDING, GUILTY, NOT_GUILTY
        c.putString("sentence", "");                   // FINE, JAIL, ACQUIT
        c.putInt("fineAmount", 0);
        c.putString("fineCurrencyID", "");
        c.putInt("jailDays", 0);
        c.putString("chargesSummary", chargesSummary);
        c.putString("contractID", "");                 // set if from a contract breach
        c.put("evidence", new NbtList());
        cases.add(c);
        markDirty();
        return id;
    }

    public void deleteCase(String id) {
        cases.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }

    // ─── Get ──────────────────────────────────────────────────────────────────

    public NbtCompound getCaseById(String id) {
        for (int i = 0; i < cases.size(); i++) {
            NbtCompound c = cases.getCompound(i);
            if (c.getString("id").equals(id)) return c;
        }
        return null;
    }

    public List<NbtCompound> getCasesByDivision(String divisionID) {
        List<NbtCompound> result = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            NbtCompound c = cases.getCompound(i);
            if (c.getString("divisionID").equals(divisionID)) result.add(c);
        }
        return result;
    }

    public List<NbtCompound> getOpenCasesByDivision(String divisionID) {
        List<NbtCompound> result = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            NbtCompound c = cases.getCompound(i);
            if (c.getString("divisionID").equals(divisionID)
                    && !c.getString("status").equals("CLOSED")) result.add(c);
        }
        return result;
    }

    public List<NbtCompound> getCasesByDefendant(String defendantUUID) {
        List<NbtCompound> result = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            NbtCompound c = cases.getCompound(i);
            if (c.getString("defendantUUID").equals(defendantUUID)) result.add(c);
        }
        return result;
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    public void setJudge(String id, String judgeUUID) {
        NbtCompound c = getCaseById(id);
        if (c == null) return;
        c.putString("judgeUUID", judgeUUID);
        markDirty();
    }

    public void setStatus(String id, String status) {
        NbtCompound c = getCaseById(id);
        if (c == null) return;
        c.putString("status", status);
        markDirty();
    }

    public void setVerdict(String id, String verdict, String sentence,
                           int fineAmount, String fineCurrencyID, int jailDays) {
        NbtCompound c = getCaseById(id);
        if (c == null) return;
        c.putString("verdict", verdict);
        c.putString("sentence", sentence);
        c.putInt("fineAmount", fineAmount);
        c.putString("fineCurrencyID", fineCurrencyID);
        c.putInt("jailDays", jailDays);
        c.putString("status", "CLOSED");
        markDirty();
    }

    public void setContractID(String id, String contractID) {
        NbtCompound c = getCaseById(id);
        if (c == null) return;
        c.putString("contractID", contractID);
        markDirty();
    }

    public void addEvidence(String id, String evidence) {
        NbtCompound c = getCaseById(id);
        if (c == null) return;
        NbtList ev = c.getList("evidence", 8);
        ev.add(NbtString.of(evidence));
        c.put("evidence", ev);
        markDirty();
    }

    public List<String> getEvidence(String id) {
        NbtCompound c = getCaseById(id);
        List<String> result = new ArrayList<>();
        if (c == null) return result;
        NbtList ev = c.getList("evidence", 8);
        for (int i = 0; i < ev.size(); i++) result.add(ev.getString(i));
        return result;
    }
}