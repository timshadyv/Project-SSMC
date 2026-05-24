package com.sovereignstate.systems;

import com.sovereignstate.data.CaseData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.GovernmentTypes;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class CourtSystem {

    // ─── Democratic Check ─────────────────────────────────────────────────────

    public static boolean isDemocratic(ServerWorld world, String divisionID) {
        NbtCompound div = DivisionData.get(world).getDivisionById(divisionID);
        if (div == null) return false;
        String govType = div.getString("governmentType");
        GovernmentTypes.GovernmentType gov = GovernmentTypes.get(govType);
        if (gov == null) return false;
        return gov.category().equals("democracy") || gov.category().equals("republic");
    }

    // ─── Judge Role ───────────────────────────────────────────────────────────

    /**
     * Leader appoints a player as judge of their division.
     * Stored as "judgeUUID" field on the division.
     */
    public static void appointJudge(ServerPlayerEntity player, ServerWorld world,
                                    ServerPlayerEntity target) {
        if (!DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the division leader can appoint a judge."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (!isDemocratic(world, divID)) {
            player.sendMessage(Text.literal("§cOnly democratic/republic governments have judges."));
            return;
        }

        String targetDivID = PlayerStateData.get(world).getDivisionID(target.getUuid().toString());
        if (!divID.equals(targetDivID)) {
            player.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " is not in your division."));
            return;
        }

        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divID);
        if (div == null) return;

        div.putString("judgeUUID", target.getUuid().toString());
        divData.markDirty();

        player.sendMessage(Text.literal("§a" + target.getName().getString() +
                " has been appointed as Judge."));
        target.sendMessage(Text.literal("§6You have been appointed as Judge of §e" +
                div.getString("name") + "§6."));
    }

    public static boolean isJudge(ServerWorld world, ServerPlayerEntity player) {
        String uuid = player.getUuid().toString();
        String divID = PlayerStateData.get(world).getDivisionID(uuid);
        if (divID == null || divID.isEmpty()) return false;
        NbtCompound div = DivisionData.get(world).getDivisionById(divID);
        return div != null && div.getString("judgeUUID").equals(uuid);
    }

    // ─── File a Case ──────────────────────────────────────────────────────────

    /**
     * Any division member can file a case against another member.
     * Division must be democratic/republic.
     */
    public static void fileCase(ServerPlayerEntity plaintiff, ServerWorld world,
                                ServerPlayerEntity defendant, String charges) {
        String plaintiffUUID = plaintiff.getUuid().toString();
        String defendantUUID = defendant.getUuid().toString();

        String divID = PlayerStateData.get(world).getDivisionID(plaintiffUUID);
        if (divID == null || divID.isEmpty()) {
            plaintiff.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }
        if (!isDemocratic(world, divID)) {
            plaintiff.sendMessage(Text.literal(
                    "§cCourt cases can only be filed in democratic or republic governments."));
            return;
        }

        String defDivID = PlayerStateData.get(world).getDivisionID(defendantUUID);
        if (!divID.equals(defDivID)) {
            plaintiff.sendMessage(Text.literal("§cThe defendant must be in your division."));
            return;
        }
        if (plaintiffUUID.equals(defendantUUID)) {
            plaintiff.sendMessage(Text.literal("§cYou cannot file a case against yourself."));
            return;
        }

        String caseID = CaseData.get(world).createCase(divID, defendantUUID,
                plaintiffUUID, charges);

        plaintiff.sendMessage(Text.literal("§aCase filed. Case ID: §e" + caseID));
        plaintiff.sendMessage(Text.literal("§7The judge must now accept and rule on the case."));

        // Notify defendant
        defendant.sendMessage(Text.literal("§c[Court] A case has been filed against you by §e" +
                plaintiff.getName().getString() + "§c."));
        defendant.sendMessage(Text.literal("§cCharges: §f" + charges +
                " §7| Case ID: " + caseID));

        // Notify judge if online
        notifyJudge(plaintiff.getServer(), world, divID,
                "§6[Court] New case §e" + caseID + "§6 filed against §e" +
                        defendant.getName().getString() + "§6.");
    }

    /**
     * Called by ContractSystem when a NAP or contract is breached in a democratic division.
     */
    public static String fileContractBreachCase(ServerWorld world, String divisionID,
                                                String defendantUUID, String contractID,
                                                String chargesSummary) {
        if (!isDemocratic(world, divisionID)) return null;

        CaseData caseData = CaseData.get(world);
        String caseID = caseData.createCase(divisionID, defendantUUID,
                "DIVISION", chargesSummary);
        caseData.setContractID(caseID, contractID);

        notifyJudge(null, world, divisionID,
                "§6[Court] Contract breach case §e" + caseID + "§6 filed automatically.");
        return caseID;
    }

    // ─── Accept Case (Judge) ──────────────────────────────────────────────────

    public static void acceptCase(ServerPlayerEntity judge, ServerWorld world, String caseID) {
        if (!isJudge(world, judge)) {
            judge.sendMessage(Text.literal("§cOnly the appointed judge can accept cases."));
            return;
        }

        CaseData caseData = CaseData.get(world);
        NbtCompound c = caseData.getCaseById(caseID);
        if (c == null) {
            judge.sendMessage(Text.literal("§cCase not found."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(judge.getUuid().toString());
        if (!c.getString("divisionID").equals(divID)) {
            judge.sendMessage(Text.literal("§cThis case does not belong to your division."));
            return;
        }
        if (!c.getString("status").equals("OPEN")) {
            judge.sendMessage(Text.literal("§cThis case is not open for acceptance."));
            return;
        }

        caseData.setJudge(caseID, judge.getUuid().toString());
        caseData.setStatus(caseID, "IN_TRIAL");

        judge.sendMessage(Text.literal("§aCase §e" + caseID + "§a accepted. You are now presiding."));
        judge.sendMessage(Text.literal("§7Use §f/ss court addEvidence§7, then §f/ss court rule §7to deliver a verdict."));
    }

    // ─── Add Evidence ─────────────────────────────────────────────────────────

    public static void addEvidence(ServerPlayerEntity player, ServerWorld world,
                                   String caseID, String evidence) {
        CaseData caseData = CaseData.get(world);
        NbtCompound c = caseData.getCaseById(caseID);
        if (c == null) {
            player.sendMessage(Text.literal("§cCase not found."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (!c.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThis case does not belong to your division."));
            return;
        }

        // Plaintiff, defendant, or judge can submit evidence
        String uuid = player.getUuid().toString();
        boolean isParty = uuid.equals(c.getString("plaintiffUUID"))
                || uuid.equals(c.getString("defendantUUID"))
                || uuid.equals(c.getString("judgeUUID"));
        if (!isParty && !DivisionSystem.isLeader(world, player)) {
            player.sendMessage(Text.literal("§cOnly the plaintiff, defendant, or judge can submit evidence."));
            return;
        }
        if (c.getString("status").equals("CLOSED")) {
            player.sendMessage(Text.literal("§cThis case is already closed."));
            return;
        }

        caseData.addEvidence(caseID, player.getName().getString() + ": " + evidence);
        player.sendMessage(Text.literal("§aEvidence added to case §e" + caseID + "§a."));
    }

    // ─── Rule (Verdict) ───────────────────────────────────────────────────────

    /**
     * Judge delivers verdict.
     * sentence: ACQUIT | FINE | JAIL
     */
    public static void rule(ServerPlayerEntity judge, ServerWorld world, String caseID,
                            boolean guilty, String sentence,
                            int fineAmount, String fineCurrencyID, int jailDays) {
        if (!isJudge(world, judge)) {
            judge.sendMessage(Text.literal("§cOnly the appointed judge can deliver verdicts."));
            return;
        }

        CaseData caseData = CaseData.get(world);
        NbtCompound c = caseData.getCaseById(caseID);
        if (c == null) {
            judge.sendMessage(Text.literal("§cCase not found."));
            return;
        }
        if (!c.getString("judgeUUID").equals(judge.getUuid().toString())) {
            judge.sendMessage(Text.literal("§cYou are not the presiding judge for this case."));
            return;
        }
        if (!c.getString("status").equals("IN_TRIAL")) {
            judge.sendMessage(Text.literal("§cThis case is not in trial."));
            return;
        }

        String verdictStr = guilty ? "GUILTY" : "NOT_GUILTY";
        String sentenceStr = guilty ? sentence.toUpperCase() : "ACQUIT";

        caseData.setVerdict(caseID, verdictStr, sentenceStr, fineAmount, fineCurrencyID, jailDays);

        String defendantUUID = c.getString("defendantUUID");
        String divID = c.getString("divisionID");

        // Announce verdict
        MinecraftServer server = judge.getServer();
        NbtCompound div = DivisionData.get(world).getDivisionById(divID);
        String divName = div != null ? div.getString("name") : "Unknown";

        String verdictMsg = guilty
                ? "§c[Court - " + divName + "] §eCase " + caseID +
                  "§c: Defendant found §4GUILTY§c. Sentence: §f" + sentenceStr
                : "§a[Court - " + divName + "] §eCase " + caseID +
                  "§a: Defendant found §2NOT GUILTY§a. Case dismissed.";

        if (server != null) TextHelper.broadcastToAll(server, verdictMsg);

        // Enforce sentence
        if (guilty) {
            enforceSentence(judge, world, defendantUUID, sentenceStr,
                    fineAmount, fineCurrencyID, jailDays);
        }
    }

    private static void enforceSentence(ServerPlayerEntity judge, ServerWorld world,
                                        String defendantUUID, String sentence,
                                        int fineAmount, String fineCurrencyID, int jailDays) {
        switch (sentence) {
            case "FINE" -> {
                // Deduct from defendant's balance
                if (fineAmount > 0 && !fineCurrencyID.isEmpty()) {
                    PlayerStateData pDataFine = PlayerStateData.get(world);
                    int balance = pDataFine.getWallet(defendantUUID, fineCurrencyID);
                    int deduct = Math.min(balance, fineAmount);
                    pDataFine.adjustWallet(defendantUUID, fineCurrencyID, -deduct);
                    ServerPlayerEntity defendant = judge.getServer() != null
                            ? judge.getServer().getPlayerManager()
                            .getPlayer(java.util.UUID.fromString(defendantUUID))
                            : null;
                    if (defendant != null) {
                        defendant.sendMessage(Text.literal("§c[Court] You have been fined §e" +
                                fineAmount + " " + fineCurrencyID + "§c."));
                    }
                }
            }
            case "JAIL" -> {
                // Set wanted flag — guards arrest with HANDCUFF item
                PlayerStateData pData = PlayerStateData.get(world);
                pData.setWanted(defendantUUID, true);
                pData.setWantedReason(defendantUUID, "Court sentence: " + jailDays + " day(s)");
                ServerPlayerEntity defendant = judge.getServer() != null
                        ? judge.getServer().getPlayerManager()
                        .getPlayer(java.util.UUID.fromString(defendantUUID))
                        : null;
                if (defendant != null) {
                    defendant.sendMessage(Text.literal(
                            "§c[Court] You have been sentenced to §e" + jailDays +
                                    " day(s)§c in jail. Guards have been notified."));
                }
            }
            case "ACQUIT" -> { /* nothing to enforce */ }
        }
    }

    // ─── Issue Warrant (Judge or Autocrat Leader) ─────────────────────────────

    /**
     * Issues a wanted warrant for a player.
     * Democratic govts: judge only.
     * Authoritarian/monarchy: leader only (handled in CommandSystem).
     */
    public static void issueWarrant(ServerPlayerEntity issuer, ServerWorld world,
                                    ServerPlayerEntity target, String reason) {
        String divID = PlayerStateData.get(world).getDivisionID(issuer.getUuid().toString());
        if (divID == null || divID.isEmpty()) {
            issuer.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        boolean canIssue = false;
        NbtCompound div = DivisionData.get(world).getDivisionById(divID);
        if (div == null) return;

        String govType = div.getString("governmentType");
        GovernmentTypes.GovernmentType gov = GovernmentTypes.get(govType);
        String category = gov != null ? gov.category() : "";

        if (category.equals("democracy") || category.equals("republic")) {
            canIssue = isJudge(world, issuer);
        } else if (category.equals("monarchy") || category.equals("authoritarian")) {
            canIssue = DivisionSystem.isLeader(world, issuer);
        }

        if (!canIssue) {
            issuer.sendMessage(Text.literal("§cYou do not have authority to issue warrants."));
            return;
        }

        String targetDivID = PlayerStateData.get(world).getDivisionID(target.getUuid().toString());
        if (!divID.equals(targetDivID)) {
            issuer.sendMessage(Text.literal("§cWarrants can only be issued for members of your division."));
            return;
        }

        PlayerStateData pData = PlayerStateData.get(world);
        pData.setWanted(target.getUuid().toString(), true);
        pData.setWantedReason(target.getUuid().toString(), reason);

        issuer.sendMessage(Text.literal("§aWarrant issued for §e" + target.getName().getString() +
                "§a. Reason: §f" + reason));
        target.sendMessage(Text.literal("§c[Law] A warrant has been issued for your arrest. Reason: §f" + reason));

        MinecraftServer server = issuer.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§c[Law - " + divID + "] Warrant issued for §e" + target.getName().getString() + "§c.");
        }
    }

    // ─── Clear Warrant ────────────────────────────────────────────────────────

    public static void clearWarrant(ServerPlayerEntity issuer, ServerWorld world,
                                    ServerPlayerEntity target) {
        String divID = PlayerStateData.get(world).getDivisionID(issuer.getUuid().toString());
        if (divID == null || divID.isEmpty()) {
            issuer.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        boolean canClear = isJudge(world, issuer) || DivisionSystem.isLeader(world, issuer);
        if (!canClear) {
            issuer.sendMessage(Text.literal("§cOnly the judge or leader can clear warrants."));
            return;
        }

        PlayerStateData pData = PlayerStateData.get(world);
        String targetUUID = target.getUuid().toString();
        if (!pData.isWanted(targetUUID)) {
            issuer.sendMessage(Text.literal("§c" + target.getName().getString() +
                    " has no active warrant."));
            return;
        }

        pData.clearWanted(targetUUID);
        issuer.sendMessage(Text.literal("§aWarrant cleared for §e" +
                target.getName().getString() + "§a."));
        target.sendMessage(Text.literal("§aYour warrant has been cleared."));
    }

    // ─── Info / List ──────────────────────────────────────────────────────────

    public static void listCases(ServerPlayerEntity player, ServerWorld world) {
        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (divID == null || divID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        List<NbtCompound> openCases = CaseData.get(world).getOpenCasesByDivision(divID);
        if (openCases.isEmpty()) {
            player.sendMessage(Text.literal("§eNo open cases in your division."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Open Cases ---"));
        for (NbtCompound c : openCases) {
            player.sendMessage(Text.literal(
                    "§eID: §f" + c.getString("id") +
                            " §7| Status: " + c.getString("status") +
                            " §7| Charges: " + c.getString("chargesSummary")));
        }
    }

    public static void showCaseInfo(ServerPlayerEntity player, ServerWorld world, String caseID) {
        NbtCompound c = CaseData.get(world).getCaseById(caseID);
        if (c == null) {
            player.sendMessage(Text.literal("§cCase not found."));
            return;
        }

        String divID = PlayerStateData.get(world).getDivisionID(player.getUuid().toString());
        if (!c.getString("divisionID").equals(divID)) {
            player.sendMessage(Text.literal("§cThis case is not in your division."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Case " + caseID + " ---"));
        player.sendMessage(Text.literal("§eStatus: §f" + c.getString("status")));
        player.sendMessage(Text.literal("§eCharges: §f" + c.getString("chargesSummary")));
        player.sendMessage(Text.literal("§eDefendant UUID: §f" + c.getString("defendantUUID")));
        player.sendMessage(Text.literal("§ePlaintiff: §f" + c.getString("plaintiffUUID")));
        player.sendMessage(Text.literal("§eJudge: §f" +
                (c.getString("judgeUUID").isEmpty() ? "Not assigned" : c.getString("judgeUUID"))));
        player.sendMessage(Text.literal("§eVerdict: §f" + c.getString("verdict")));

        if (!c.getString("verdict").equals("PENDING")) {
            player.sendMessage(Text.literal("§eSentence: §f" + c.getString("sentence")));
            if (c.getInt("fineAmount") > 0)
                player.sendMessage(Text.literal("§eFine: §f" + c.getInt("fineAmount") +
                        " " + c.getString("fineCurrencyID")));
            if (c.getInt("jailDays") > 0)
                player.sendMessage(Text.literal("§eJail Days: §f" + c.getInt("jailDays")));
        }

        List<String> evidence = CaseData.get(world).getEvidence(caseID);
        if (!evidence.isEmpty()) {
            player.sendMessage(Text.literal("§eEvidence:"));
            for (String e : evidence) player.sendMessage(Text.literal("§7 - " + e));
        }
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────

    private static void notifyJudge(MinecraftServer server, ServerWorld world,
                                    String divID, String message) {
        NbtCompound div = DivisionData.get(world).getDivisionById(divID);
        if (div == null || server == null) return;
        String judgeUUID = div.getString("judgeUUID");
        if (judgeUUID.isEmpty()) return;
        try {
            ServerPlayerEntity judge = server.getPlayerManager()
                    .getPlayer(java.util.UUID.fromString(judgeUUID));
            if (judge != null) judge.sendMessage(Text.literal(message));
        } catch (Exception ignored) {}
    }
}