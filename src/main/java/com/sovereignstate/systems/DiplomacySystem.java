package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.WorldStateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiplomacySystem {

    // ─── Relation Types ───────────────────────────────────────────────────────

    public enum Relation { NEUTRAL, ALLIED, WAR }

    public enum DeclareWarResult { SUCCESS, NO_CASUS_BELLI, ALREADY_AT_WAR, DIVISION_NOT_FOUND }

    // ─── Key Helpers ──────────────────────────────────────────────────────────

    /** Alphabetically sorted so relKey(A,B) == relKey(B,A) */
    private static String relKey(String a, String b) {
        String[] s = {a, b};
        Arrays.sort(s);
        return "rel_" + s[0] + "_" + s[1];
    }

    // ─── Relation Get / Set ───────────────────────────────────────────────────

    public static Relation getRelation(ServerWorld world, String divA, String divB) {
        String val = WorldStateData.get(world).getTag(relKey(divA, divB));
        if (val.isEmpty()) return Relation.NEUTRAL;
        try { return Relation.valueOf(val); } catch (Exception e) { return Relation.NEUTRAL; }
    }

    private static void setRelation(ServerWorld world, String divA, String divB, Relation rel) {
        WorldStateData data = WorldStateData.get(world);
        if (rel == Relation.NEUTRAL) {
            data.removeTag(relKey(divA, divB));
        } else {
            data.setTag(relKey(divA, divB), rel.name());
        }
        // Keep DivisionData isAtWar flag in sync
        if (rel == Relation.WAR) {
            DivisionData.get(world).setAtWar(divA, true);
            DivisionData.get(world).setAtWar(divB, true);
        } else {
            DivisionData.get(world).setAtWar(divA, syncWarStatus(world, divA));
            DivisionData.get(world).setAtWar(divB, syncWarStatus(world, divB));
        }
    }

    /** Returns true if divId is still at war with anyone after a relation change */
    private static boolean syncWarStatus(ServerWorld world, String divId) {
        DivisionData divData = DivisionData.get(world);
        for (NbtCompound other : divData.getAllDivisions()) {
            String otherId = other.getString("id");
            if (!otherId.equals(divId) && getRelation(world, divId, otherId) == Relation.WAR) {
                return true;
            }
        }
        return false;
    }

    // ─── Casus Belli ─────────────────────────────────────────────────────────

    public static void grantCasusBelli(ServerWorld world, String division, String against) {
        WorldStateData.get(world).setBooleanTag("cb_" + division + "_" + against, true);
    }

    public static boolean hasCasusBelli(ServerWorld world, String division, String against) {
        return WorldStateData.get(world).getBooleanTag("cb_" + division + "_" + against);
    }

    private static void consumeCasusBelli(ServerWorld world, String division, String against) {
        WorldStateData.get(world).removeTag("cb_" + division + "_" + against);
    }

    // ─── Pending Request Helpers (stored as "from§to" strings) ───────────────

    private static boolean hasRequest(ServerWorld world, String listKey, String from, String to) {
        return WorldStateData.get(world).listContains(listKey, from + "§" + to);
    }

    private static void addRequest(ServerWorld world, String listKey, String from, String to) {
        WorldStateData data = WorldStateData.get(world);
        String entry = from + "§" + to;
        if (!data.listContains(listKey, entry)) data.appendToList(listKey, entry);
    }

    private static boolean removeRequest(ServerWorld world, String listKey, String from, String to) {
        WorldStateData data = WorldStateData.get(world);
        String entry = from + "§" + to;
        if (data.listContains(listKey, entry)) {
            data.removeFromList(listKey, entry);
            return true;
        }
        return false;
    }

    private static List<String> getIncomingRequests(ServerWorld world, String listKey, String to) {
        List<String> result = new ArrayList<>();
        NbtList list = WorldStateData.get(world).getListTag(listKey);
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getString(i);
            String[] parts = entry.split("§", 2);
            if (parts.length == 2 && parts[1].equals(to)) result.add(parts[0]);
        }
        return result;
    }

    // ─── Alliance ─────────────────────────────────────────────────────────────

    public static boolean proposeAlliance(ServerWorld world, String from, String to) {
        if (getRelation(world, from, to) != Relation.NEUTRAL) return false;
        if (hasRequest(world, "alliance_proposals", from, to)) return false;
        addRequest(world, "alliance_proposals", from, to);
        return true;
    }

    public static boolean acceptAlliance(ServerWorld world, String acceptor, String proposer) {
        if (!hasRequest(world, "alliance_proposals", proposer, acceptor)) return false;
        removeRequest(world, "alliance_proposals", proposer, acceptor);
        setRelation(world, proposer, acceptor, Relation.ALLIED);
        return true;
    }

    public static boolean rejectAlliance(ServerWorld world, String rejecter, String proposer) {
        return removeRequest(world, "alliance_proposals", proposer, rejecter);
    }

    public static boolean leaveAlliance(ServerWorld world, String from, String ally) {
        if (getRelation(world, from, ally) != Relation.ALLIED) return false;
        setRelation(world, from, ally, Relation.NEUTRAL);
        return true;
    }

    public static List<String> getIncomingAllianceProposals(ServerWorld world, String div) {
        return getIncomingRequests(world, "alliance_proposals", div);
    }

    // ─── War ─────────────────────────────────────────────────────────────────

    public static DeclareWarResult declareWar(ServerWorld world, String attacker, String defender, boolean force) {
        if (getRelation(world, attacker, defender) == Relation.WAR) {
            return DeclareWarResult.ALREADY_AT_WAR;
        }
        if (DivisionData.get(world).getDivisionById(attacker) == null) {
            return DeclareWarResult.DIVISION_NOT_FOUND;
        }
        if (!force && !hasCasusBelli(world, attacker, defender)) {
            return DeclareWarResult.NO_CASUS_BELLI;
        }

        consumeCasusBelli(world, attacker, defender);
        // Clean up any pending peace proposals
        removeRequest(world, "peace_proposals", attacker, defender);
        removeRequest(world, "peace_proposals", defender, attacker);
        setRelation(world, attacker, defender, Relation.WAR);
        return DeclareWarResult.SUCCESS;
    }

    // ─── Peace ────────────────────────────────────────────────────────────────

    public static boolean proposePeace(ServerWorld world, String from, String to) {
        if (getRelation(world, from, to) != Relation.WAR) return false;
        if (hasRequest(world, "peace_proposals", from, to)) return false;
        addRequest(world, "peace_proposals", from, to);
        return true;
    }

    public static boolean acceptPeace(ServerWorld world, String acceptor, String proposer) {
        if (!hasRequest(world, "peace_proposals", proposer, acceptor)) return false;
        removeRequest(world, "peace_proposals", proposer, acceptor);
        setRelation(world, proposer, acceptor, Relation.NEUTRAL);
        return true;
    }

    public static boolean rejectPeace(ServerWorld world, String rejecter, String proposer) {
        return removeRequest(world, "peace_proposals", proposer, rejecter);
    }

    public static List<String> getIncomingPeaceProposals(ServerWorld world, String div) {
        return getIncomingRequests(world, "peace_proposals", div);
    }

    // ─── Vassal ───────────────────────────────────────────────────────────────

    public static String getOverlord(ServerWorld world, String subject) {
        String val = WorldStateData.get(world).getTag("vassal_" + subject);
        return val.isEmpty() ? null : val;
    }

    public static boolean isVassal(ServerWorld world, String division) {
        return getOverlord(world, division) != null;
    }

    public static List<String> getVassals(ServerWorld world, String overlord) {
        List<String> result = new ArrayList<>();
        WorldStateData data = WorldStateData.get(world);
        for (NbtCompound div : DivisionData.get(world).getAllDivisions()) {
            String id = div.getString("id");
            if (overlord.equals(data.getTag("vassal_" + id))) result.add(id);
        }
        return result;
    }

    public static boolean proposeVassal(ServerWorld world, String overlord, String subject) {
        if (isVassal(world, subject)) return false;
        if (getRelation(world, overlord, subject) == Relation.WAR) return false;
        addRequest(world, "vassal_proposals", overlord, subject);
        return true;
    }

    public static boolean acceptVassal(ServerWorld world, String subject, String overlord) {
        if (!hasRequest(world, "vassal_proposals", overlord, subject)) return false;
        removeRequest(world, "vassal_proposals", overlord, subject);
        WorldStateData.get(world).setTag("vassal_" + subject, overlord);
        setRelation(world, subject, overlord, Relation.ALLIED);
        return true;
    }

    public static boolean rejectVassal(ServerWorld world, String subject, String overlord) {
        return removeRequest(world, "vassal_proposals", overlord, subject);
    }

    public static boolean declareIndependence(ServerWorld world, String subject) {
        String overlord = getOverlord(world, subject);
        if (overlord == null) return false;
        WorldStateData.get(world).removeTag("vassal_" + subject);
        grantCasusBelli(world, overlord, subject);
        setRelation(world, subject, overlord, Relation.WAR);
        return true;
    }

    public static List<String> getIncomingVassalProposals(ServerWorld world, String subject) {
        return getIncomingRequests(world, "vassal_proposals", subject);
    }

    // ─── Info Queries ─────────────────────────────────────────────────────────

    public static List<String> getEnemies(ServerWorld world, String div) {
        List<String> enemies = new ArrayList<>();
        for (NbtCompound other : DivisionData.get(world).getAllDivisions()) {
            String otherId = other.getString("id");
            if (!otherId.equals(div) && getRelation(world, div, otherId) == Relation.WAR) {
                enemies.add(otherId);
            }
        }
        return enemies;
    }

    public static List<String> getAllies(ServerWorld world, String div) {
        List<String> allies = new ArrayList<>();
        for (NbtCompound other : DivisionData.get(world).getAllDivisions()) {
            String otherId = other.getString("id");
            if (!otherId.equals(div) && getRelation(world, div, otherId) == Relation.ALLIED) {
                allies.add(otherId);
            }
        }
        return allies;
    }

    public static boolean isAtWar(ServerWorld world, String a, String b) {
        return getRelation(world, a, b) == Relation.WAR;
    }

    public static boolean isAllied(ServerWorld world, String a, String b) {
        return getRelation(world, a, b) == Relation.ALLIED;
    }
}