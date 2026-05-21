package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContractData extends PersistentState {

    private NbtList contracts = new NbtList();

    public static ContractData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    ContractData state = new ContractData();
                    state.contracts = nbt.getList("contracts", 10);
                    return state;
                },
                ContractData::new,
                "sovereignstate_contracts"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("contracts", contracts);
        return nbt;
    }

    // -------------------------------------------------------------------------
    // CREATE CONTRACTS
    // -------------------------------------------------------------------------

    /**
     * Trade Agreement — Party A pays Party B amount of currencyID every intervalDays.
     * partyType: "player" or "division"
     */
    public String createTradeAgreement(String partyAUUID, String partyAType,
                                       String partyBUUID, String partyBType,
                                       String currencyID, int amount, int intervalDays) {
        String id = generateID();
        NbtCompound c = new NbtCompound();
        c.putString("id",           id);
        c.putString("type",         "trade_agreement");
        c.putString("status",       "pending");         // pending → active → breached → completed → cancelled
        c.putString("partyAUUID",   partyAUUID);
        c.putString("partyAType",   partyAType);
        c.putString("partyBUUID",   partyBUUID);
        c.putString("partyBType",   partyBType);
        c.putString("currencyID",   currencyID);
        c.putInt   ("amount",       amount);
        c.putInt   ("intervalDays", intervalDays);
        c.putInt   ("daysSinceLastPayment", 0);
        c.putBoolean("signedA",     false);
        c.putBoolean("signedB",     false);
        c.putString("breachReason", "");
        c.putString("breachDecision", "");             // fine/jail/war/pardon/sanctions
        c.put      ("clauses",      new NbtList());
        contracts.add(c);
        markDirty();
        return id;
    }

    /**
     * Non-Aggression Pact — neither party may declare war for durationDays.
     */
    public String createNAP(String partyAUUID, String partyAType,
                            String partyBUUID, String partyBType,
                            int durationDays) {
        String id = generateID();
        NbtCompound c = new NbtCompound();
        c.putString("id",           id);
        c.putString("type",         "nap");
        c.putString("status",       "pending");
        c.putString("partyAUUID",   partyAUUID);
        c.putString("partyAType",   partyAType);
        c.putString("partyBUUID",   partyBUUID);
        c.putString("partyBType",   partyBType);
        c.putInt   ("durationDays", durationDays);
        c.putInt   ("daysActive",   0);
        c.putBoolean("signedA",     false);
        c.putBoolean("signedB",     false);
        c.putString("breachReason", "");
        c.putString("breachDecision", "");
        c.put      ("clauses",      new NbtList());
        contracts.add(c);
        markDirty();
        return id;
    }

    /**
     * Loan — Party B receives amount now, repays amount + interest within repayDays.
     */
    public String createLoan(String lenderUUID, String lenderType,
                             String borrowerUUID, String borrowerType,
                             String currencyID, int amount,
                             int interestPercent, int repayDays) {
        String id = generateID();
        int totalOwed = amount + (int) Math.ceil(amount * (interestPercent / 100.0));
        NbtCompound c = new NbtCompound();
        c.putString("id",              id);
        c.putString("type",            "loan");
        c.putString("status",          "pending");
        c.putString("partyAUUID",      lenderUUID);     // A = lender
        c.putString("partyAType",      lenderType);
        c.putString("partyBUUID",      borrowerUUID);   // B = borrower
        c.putString("partyBType",      borrowerType);
        c.putString("currencyID",      currencyID);
        c.putInt   ("amount",          amount);
        c.putInt   ("interestPercent", interestPercent);
        c.putInt   ("totalOwed",       totalOwed);
        c.putInt   ("repayDays",       repayDays);
        c.putInt   ("daysActive",      0);
        c.putBoolean("signedA",        false);
        c.putBoolean("signedB",        false);
        c.putBoolean("disbursed",      false);          // has loan been paid out yet
        c.putString("breachReason",    "");
        c.putString("breachDecision",  "");
        c.put      ("clauses",         new NbtList());
        contracts.add(c);
        markDirty();
        return id;
    }

    // -------------------------------------------------------------------------
    // SIGNING
    // -------------------------------------------------------------------------

    public void signAsA(String contractID) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putBoolean("signedA", true);
        activateIfBothSigned(c);
        markDirty();
    }

    public void signAsB(String contractID) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putBoolean("signedB", true);
        activateIfBothSigned(c);
        markDirty();
    }

    private void activateIfBothSigned(NbtCompound c) {
        if (c.getBoolean("signedA") && c.getBoolean("signedB")) {
            c.putString("status", "active");
        }
    }

    // -------------------------------------------------------------------------
    // CLAUSES
    // -------------------------------------------------------------------------

    public void addClause(String contractID, String clause) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        // Clauses locked once both parties sign
        if (c.getString("status").equals("active")) return;
        NbtList clauses = c.getList("clauses", 8);
        clauses.add(NbtString.of(clause));
        c.put("clauses", clauses);
        markDirty();
    }

    public void removeClause(String contractID, int index) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        if (c.getString("status").equals("active")) return;
        NbtList clauses = c.getList("clauses", 8);
        if (index >= 0 && index < clauses.size()) {
            clauses.remove(index);
            c.put("clauses", clauses);
            markDirty();
        }
    }

    public List<String> getClauses(String contractID) {
        NbtCompound c = getById(contractID);
        List<String> list = new ArrayList<>();
        if (c == null) return list;
        NbtList clauses = c.getList("clauses", 8);
        for (int i = 0; i < clauses.size(); i++) list.add(clauses.getString(i));
        return list;
    }

    // -------------------------------------------------------------------------
    // BREACH
    // -------------------------------------------------------------------------

    public void markBreached(String contractID, String reason) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putString("status",       "breached");
        c.putString("breachReason", reason);
        markDirty();
    }

    public void setBreachDecision(String contractID, String decision) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putString("breachDecision", decision);
        c.putString("status", "resolved");
        markDirty();
    }

    // -------------------------------------------------------------------------
    // STATUS SETTERS
    // -------------------------------------------------------------------------

    public void setStatus(String contractID, String status) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putString("status", status);
        markDirty();
    }

    public void incrementDays(String contractID) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        String type = c.getString("type");
        if (type.equals("trade_agreement")) {
            c.putInt("daysSinceLastPayment", c.getInt("daysSinceLastPayment") + 1);
        } else {
            c.putInt("daysActive", c.getInt("daysActive") + 1);
        }
        markDirty();
    }

    public void resetPaymentTimer(String contractID) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putInt("daysSinceLastPayment", 0);
        markDirty();
    }

    public void setDisbursed(String contractID, boolean disbursed) {
        NbtCompound c = getById(contractID);
        if (c == null) return;
        c.putBoolean("disbursed", disbursed);
        markDirty();
    }

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    public NbtCompound getById(String id) {
        for (int i = 0; i < contracts.size(); i++) {
            NbtCompound c = contracts.getCompound(i);
            if (c.getString("id").equals(id)) return c;
        }
        return null;
    }

    public List<NbtCompound> getAll() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < contracts.size(); i++) list.add(contracts.getCompound(i));
        return list;
    }

    public List<NbtCompound> getActiveContracts() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < contracts.size(); i++) {
            NbtCompound c = contracts.getCompound(i);
            if (c.getString("status").equals("active")) list.add(c);
        }
        return list;
    }

    public List<NbtCompound> getContractsForParty(String uuid) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < contracts.size(); i++) {
            NbtCompound c = contracts.getCompound(i);
            if (c.getString("partyAUUID").equals(uuid) ||
                    c.getString("partyBUUID").equals(uuid)) list.add(c);
        }
        return list;
    }

    public List<NbtCompound> getBreachedContracts() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < contracts.size(); i++) {
            NbtCompound c = contracts.getCompound(i);
            if (c.getString("status").equals("breached")) list.add(c);
        }
        return list;
    }

    public boolean hasActiveNAP(String uuidA, String uuidB) {
        for (int i = 0; i < contracts.size(); i++) {
            NbtCompound c = contracts.getCompound(i);
            if (!c.getString("type").equals("nap")) continue;
            if (!c.getString("status").equals("active")) continue;
            String a = c.getString("partyAUUID");
            String b = c.getString("partyBUUID");
            if ((a.equals(uuidA) && b.equals(uuidB)) ||
                    (a.equals(uuidB) && b.equals(uuidA))) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    private String generateID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}