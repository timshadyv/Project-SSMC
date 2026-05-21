package com.sovereignstate.systems;

import com.sovereignstate.data.ContractData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.GovernmentTypes;
import com.sovereignstate.data.PlayerStateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class ContractSystem {

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    public static void createTradeAgreement(ServerPlayerEntity creator, ServerWorld world,
                                            ServerPlayerEntity other,
                                            String currencyID, int amount, int intervalDays) {
        ContractData contractData = ContractData.get(world);
        String creatorUUID = creator.getUuid().toString();
        String otherUUID   = other.getUuid().toString();

        String id = contractData.createTradeAgreement(
                creatorUUID, "player",
                otherUUID,   "player",
                currencyID, amount, intervalDays);

        // Creator auto-signs as Party A
        contractData.signAsA(id);

        creator.sendMessage(Text.literal(
                "§aTrade agreement created §7[ID: " + id + "]"));
        creator.sendMessage(Text.literal(
                "§e" + other.getName().getString() +
                        " §emust sign with §f/ss contract sign " + id));

        other.sendMessage(Text.literal(
                "§6Contract offer from §e" + creator.getName().getString() +
                        "§6: Trade Agreement"));
        other.sendMessage(Text.literal(
                "§fPay §e" + amount + " " + currencyID +
                        " §fevery §e" + intervalDays + " days"));
        other.sendMessage(Text.literal(
                "§eSign with §f/ss contract sign " + id +
                        " §eor add clauses first with §f/ss contract addclause " + id + " <text>"));
    }

    public static void createNAP(ServerPlayerEntity creator, ServerWorld world,
                                 ServerPlayerEntity other, int durationDays) {
        ContractData contractData = ContractData.get(world);
        String creatorUUID = creator.getUuid().toString();
        String otherUUID   = other.getUuid().toString();

        // Check if NAP already exists
        if (contractData.hasActiveNAP(creatorUUID, otherUUID)) {
            creator.sendMessage(Text.literal(
                    "§cAn active NAP already exists with " + other.getName().getString()));
            return;
        }

        String id = contractData.createNAP(
                creatorUUID, "player",
                otherUUID,   "player",
                durationDays);

        contractData.signAsA(id);

        creator.sendMessage(Text.literal(
                "§aNon-Aggression Pact created §7[ID: " + id + "]"));
        creator.sendMessage(Text.literal(
                "§e" + other.getName().getString() +
                        " §emust sign with §f/ss contract sign " + id));

        other.sendMessage(Text.literal(
                "§6NAP offer from §e" + creator.getName().getString() +
                        "§6: Non-Aggression Pact for §e" + durationDays + " days"));
        other.sendMessage(Text.literal(
                "§eSign with §f/ss contract sign " + id));
    }

    public static void createLoan(ServerPlayerEntity lender, ServerWorld world,
                                  ServerPlayerEntity borrower,
                                  String currencyID, int amount,
                                  int interestPercent, int repayDays) {
        PlayerStateData ps = PlayerStateData.get(world);

        // Check lender has funds
        int lenderBalance = ps.getWallet(lender.getUuid().toString(), currencyID);
        if (lenderBalance < amount) {
            lender.sendMessage(Text.literal(
                    "§cYou don't have §f" + amount + " " + currencyID +
                            "§c to offer. Balance: §f" + lenderBalance));
            return;
        }

        ContractData contractData = ContractData.get(world);
        int totalOwed = amount + (int) Math.ceil(amount * (interestPercent / 100.0));

        String id = contractData.createLoan(
                lender.getUuid().toString(),   "player",
                borrower.getUuid().toString(), "player",
                currencyID, amount, interestPercent, repayDays);

        contractData.signAsA(id);

        lender.sendMessage(Text.literal(
                "§aLoan offer created §7[ID: " + id + "]"));
        lender.sendMessage(Text.literal(
                "§fAmount: §e" + amount + " " + currencyID +
                        " §f| Interest: §e" + interestPercent + "%" +
                        " §f| Total owed: §e" + totalOwed +
                        " §f| Repay in: §e" + repayDays + " days"));

        borrower.sendMessage(Text.literal(
                "§6Loan offer from §e" + lender.getName().getString()));
        borrower.sendMessage(Text.literal(
                "§fReceive: §e" + amount + " " + currencyID +
                        " §f| Must repay: §e" + totalOwed + " " + currencyID +
                        " §fwithin §e" + repayDays + " days"));
        borrower.sendMessage(Text.literal(
                "§eSign with §f/ss contract sign " + id));
    }

    // -------------------------------------------------------------------------
    // SIGN
    // -------------------------------------------------------------------------

    public static void signContract(ServerPlayerEntity player, ServerWorld world,
                                    String contractID) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            player.sendMessage(Text.literal("§cContract not found: " + contractID));
            return;
        }
        if (!c.getString("status").equals("pending")) {
            player.sendMessage(Text.literal("§cThis contract is not awaiting signatures."));
            return;
        }

        String uuid = player.getUuid().toString();
        String partyA = c.getString("partyAUUID");
        String partyB = c.getString("partyBUUID");

        if (uuid.equals(partyA) && !c.getBoolean("signedA")) {
            contractData.signAsA(contractID);
        } else if (uuid.equals(partyB) && !c.getBoolean("signedB")) {
            contractData.signAsB(contractID);

            // If both now signed, activate
            NbtCompound updated = contractData.getById(contractID);
            if (updated.getString("status").equals("active")) {
                onContractActivated(player, world, updated);
            }
        } else {
            player.sendMessage(Text.literal(
                    "§cYou are not a party to this contract or have already signed."));
            return;
        }

        player.sendMessage(Text.literal("§aSigned contract §7[" + contractID + "]"));
    }

    private static void onContractActivated(ServerPlayerEntity player, ServerWorld world,
                                            NbtCompound c) {
        String type = c.getString("type");
        String id   = c.getString("id");
        ContractData contractData = ContractData.get(world);

        // For loans: disburse funds immediately on activation
        if (type.equals("loan") && !c.getBoolean("disbursed")) {
            PlayerStateData ps     = PlayerStateData.get(world);
            String lenderUUID      = c.getString("partyAUUID");
            String borrowerUUID    = c.getString("partyBUUID");
            String currencyID      = c.getString("currencyID");
            int    amount          = c.getInt("amount");

            int lenderBalance = ps.getWallet(lenderUUID, currencyID);
            if (lenderBalance >= amount) {
                ps.adjustWallet(lenderUUID,   currencyID, -amount);
                ps.adjustWallet(borrowerUUID, currencyID,  amount);
                contractData.setDisbursed(id, true);

                // Notify both parties
                notifyParty(player, lenderUUID,
                        "§aLoan disbursed: §f" + amount + " " + currencyID +
                                " §asent to borrower.");
                notifyParty(player, borrowerUUID,
                        "§aLoan received: §f" + amount + " " + currencyID +
                                "§a. Repay by contract deadline.");
            } else {
                // Lender can no longer afford it — cancel
                contractData.setStatus(id, "cancelled");
                notifyParty(player, borrowerUUID,
                        "§cLoan contract §7[" + id + "]§c cancelled — lender has insufficient funds.");
                return;
            }
        }

        MinecraftServer server = player.getServer();
        if (server != null) {
            String typeLabel = switch (type) {
                case "trade_agreement" -> "Trade Agreement";
                case "nap"             -> "Non-Aggression Pact";
                case "loan"            -> "Loan";
                default                -> type;
            };
            server.getPlayerManager().getPlayerList().forEach(p -> {
                if (p.getUuid().toString().equals(c.getString("partyAUUID")) ||
                        p.getUuid().toString().equals(c.getString("partyBUUID"))) {
                    p.sendMessage(Text.literal(
                            "§a§lContract Active: §r§e" + typeLabel +
                                    " §7[" + id + "]"));
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // ADD / REMOVE CLAUSES
    // -------------------------------------------------------------------------

    public static void addClause(ServerPlayerEntity player, ServerWorld world,
                                 String contractID, String clause) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            player.sendMessage(Text.literal("§cContract not found."));
            return;
        }
        if (c.getString("status").equals("active")) {
            player.sendMessage(Text.literal("§cClauses are locked once both parties sign."));
            return;
        }
        if (!c.getString("partyAUUID").equals(player.getUuid().toString()) &&
                !c.getString("partyBUUID").equals(player.getUuid().toString())) {
            player.sendMessage(Text.literal("§cYou are not a party to this contract."));
            return;
        }

        contractData.addClause(contractID, clause);
        player.sendMessage(Text.literal("§aClause added: §f" + clause));
    }

    public static void removeClause(ServerPlayerEntity player, ServerWorld world,
                                    String contractID, int index) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            player.sendMessage(Text.literal("§cContract not found."));
            return;
        }
        if (c.getString("status").equals("active")) {
            player.sendMessage(Text.literal("§cClauses are locked once both parties sign."));
            return;
        }

        contractData.removeClause(contractID, index);
        player.sendMessage(Text.literal("§aClause §f#" + index + "§a removed."));
    }

    // -------------------------------------------------------------------------
    // LIST / INFO
    // -------------------------------------------------------------------------

    public static void listContracts(ServerPlayerEntity player, ServerWorld world) {
        ContractData contractData = ContractData.get(world);
        List<NbtCompound> all = contractData.getContractsForParty(
                player.getUuid().toString());

        if (all.isEmpty()) {
            player.sendMessage(Text.literal("§eYou have no contracts."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Your Contracts ---"));
        for (NbtCompound c : all) {
            String status = c.getString("status");
            String color  = switch (status) {
                case "active"    -> "§a";
                case "breached"  -> "§c";
                case "cancelled" -> "§7";
                case "resolved"  -> "§e";
                default          -> "§f";
            };
            player.sendMessage(Text.literal(
                    color + "[" + c.getString("id") + "] §f" +
                            c.getString("type") + " " + color + "[" + status + "]"));
        }
        player.sendMessage(Text.literal("§7Use §f/ss contract info <id> §7for details."));
    }

    public static void showContractInfo(ServerPlayerEntity player, ServerWorld world,
                                        String contractID) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            player.sendMessage(Text.literal("§cContract not found."));
            return;
        }

        String type   = c.getString("type");
        String status = c.getString("status");

        player.sendMessage(Text.literal("§6--- Contract Info ---"));
        player.sendMessage(Text.literal("§eID: §f"     + c.getString("id")));
        player.sendMessage(Text.literal("§eType: §f"   + type));
        player.sendMessage(Text.literal("§eStatus: §f" + status));
        player.sendMessage(Text.literal("§eSigned A: §f" + c.getBoolean("signedA")));
        player.sendMessage(Text.literal("§eSigned B: §f" + c.getBoolean("signedB")));

        switch (type) {
            case "trade_agreement" -> {
                player.sendMessage(Text.literal("§eAmount: §f" +
                        c.getInt("amount") + " " + c.getString("currencyID")));
                player.sendMessage(Text.literal("§eEvery: §f" +
                        c.getInt("intervalDays") + " days"));
                player.sendMessage(Text.literal("§eDays since payment: §f" +
                        c.getInt("daysSinceLastPayment")));
            }
            case "nap" -> {
                player.sendMessage(Text.literal("§eDuration: §f" +
                        c.getInt("durationDays") + " days"));
                player.sendMessage(Text.literal("§eDays active: §f" +
                        c.getInt("daysActive")));
            }
            case "loan" -> {
                player.sendMessage(Text.literal("§eAmount: §f" +
                        c.getInt("amount") + " " + c.getString("currencyID")));
                player.sendMessage(Text.literal("§eTotal owed: §f" +
                        c.getInt("totalOwed") + " " + c.getString("currencyID")));
                player.sendMessage(Text.literal("§eRepay within: §f" +
                        c.getInt("repayDays") + " days"));
                player.sendMessage(Text.literal("§eDays active: §f" +
                        c.getInt("daysActive")));
                player.sendMessage(Text.literal("§eDisbursed: §f" +
                        c.getBoolean("disbursed")));
            }
        }

        if (!c.getString("breachReason").isEmpty()) {
            player.sendMessage(Text.literal("§cBreach reason: §f" +
                    c.getString("breachReason")));
        }
        if (!c.getString("breachDecision").isEmpty()) {
            player.sendMessage(Text.literal("§eDecision: §f" +
                    c.getString("breachDecision")));
        }

        // Clauses
        List<String> clauses = contractData.getClauses(contractID);
        if (!clauses.isEmpty()) {
            player.sendMessage(Text.literal("§e--- Clauses ---"));
            for (int i = 0; i < clauses.size(); i++) {
                player.sendMessage(Text.literal("§f#" + i + ": " + clauses.get(i)));
            }
        }
    }

    // -------------------------------------------------------------------------
    // CANCEL
    // -------------------------------------------------------------------------

    public static void cancelContract(ServerPlayerEntity player, ServerWorld world,
                                      String contractID) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            player.sendMessage(Text.literal("§cContract not found."));
            return;
        }

        String uuid = player.getUuid().toString();
        if (!c.getString("partyAUUID").equals(uuid) &&
                !c.getString("partyBUUID").equals(uuid)) {
            player.sendMessage(Text.literal("§cYou are not a party to this contract."));
            return;
        }

        // Cancelling an active contract = breach
        if (c.getString("status").equals("active")) {
            contractData.markBreached(contractID,
                    "Cancelled by " + player.getName().getString());
            notifyOtherParty(player, world, c,
                    "§c" + player.getName().getString() +
                            " has cancelled contract §7[" + contractID + "]§c — this counts as a breach.");
            player.sendMessage(Text.literal(
                    "§cContract cancelled. This is recorded as a breach."));
            triggerBreachHandling(player, world, c);
        } else {
            contractData.setStatus(contractID, "cancelled");
            player.sendMessage(Text.literal("§aContract §7[" + contractID + "]§a cancelled."));
        }
    }

    // -------------------------------------------------------------------------
    // BREACH DECISION (autocratic governments)
    // -------------------------------------------------------------------------

    public static void handleBreachDecision(ServerPlayerEntity leader, ServerWorld world,
                                            String contractID, String decision) {
        ContractData contractData = ContractData.get(world);
        NbtCompound c = contractData.getById(contractID);

        if (c == null) {
            leader.sendMessage(Text.literal("§cContract not found."));
            return;
        }
        if (!c.getString("status").equals("breached")) {
            leader.sendMessage(Text.literal("§cThis contract has not been breached."));
            return;
        }

        // Verify leader is from an involved division or is a party
        PlayerStateData ps       = PlayerStateData.get(world);
        DivisionData divData     = DivisionData.get(world);
        String leaderDivID       = ps.getDivisionID(leader.getUuid().toString());
        String partyAUUID        = c.getString("partyAUUID");
        String partyBUUID        = c.getString("partyBUUID");

        boolean isParty = leader.getUuid().toString().equals(partyAUUID) ||
                leader.getUuid().toString().equals(partyBUUID);
        boolean isDivLeader = leaderDivID != null && (
                leaderDivID.equals(partyAUUID) || leaderDivID.equals(partyBUUID));

        if (!isParty && !isDivLeader) {
            leader.sendMessage(Text.literal("§cYou are not authorised to decide this breach."));
            return;
        }

        // Check government type allows unilateral decision
        if (leaderDivID != null) {
            NbtCompound div = divData.getDivisionById(leaderDivID);
            if (div != null) {
                String govType = div.getString("governmentType");
                GovernmentTypes.GovernmentType gov = GovernmentTypes.get(govType);
                if (gov != null && !gov.category().equals("authoritarian") &&
                        !gov.category().equals("monarchy")) {
                    leader.sendMessage(Text.literal(
                            "§cYour government type requires a court decision, " +
                                    "not a unilateral ruling."));
                    return;
                }
            }
        }

        // Apply decision
        switch (decision.toLowerCase()) {
            case "fine" -> {
                applyFine(leader, world, c);
                contractData.setBreachDecision(contractID, "fine");
            }
            case "jail" -> {
                applyJail(leader, world, c);
                contractData.setBreachDecision(contractID, "jail");
            }
            case "war" -> {
                applyWarFlag(leader, world, c);
                contractData.setBreachDecision(contractID, "war");
            }
            case "sanctions" -> {
                applySanctions(leader, world, c);
                contractData.setBreachDecision(contractID, "sanctions");
            }
            case "pardon" -> {
                contractData.setBreachDecision(contractID, "pardoned");
                leader.sendMessage(Text.literal("§aBreach pardoned. No penalty applied."));
                notifyOtherParty(leader, world, c,
                        "§aThe breach of contract §7[" + contractID + "]§a has been pardoned.");
            }
            default -> {
                leader.sendMessage(Text.literal(
                        "§cUnknown decision. Use: fine, jail, war, sanctions, pardon"));
                return;
            }
        }

        leader.sendMessage(Text.literal(
                "§aDecision §f'" + decision + "'§a applied to contract §7[" + contractID + "]"));
    }

    // -------------------------------------------------------------------------
    // BREACH ENFORCEMENT
    // -------------------------------------------------------------------------

    private static void triggerBreachHandling(ServerPlayerEntity context,
                                              ServerWorld world, NbtCompound c) {
        // Determine breaching party's division government type
        // If authoritarian/monarchy → notify leader to decide
        // If democracy/republic → auto-file with court (placeholder for CourtSystem)
        DivisionData divData = DivisionData.get(world);
        PlayerStateData ps   = PlayerStateData.get(world);

        String breachingUUID = c.getString("partyBUUID"); // B is usually the obligated party
        String divID         = ps.getDivisionID(breachingUUID);

        if (divID != null && !divID.isEmpty()) {
            NbtCompound div = divData.getDivisionById(divID);
            if (div != null) {
                String govType = div.getString("governmentType");
                GovernmentTypes.GovernmentType gov = GovernmentTypes.get(govType);
                String leaderUUID = div.getString("leaderUUID");

                if (gov != null && (gov.category().equals("authoritarian") ||
                        gov.category().equals("monarchy"))) {
                    // Notify leader to make decision
                    notifyParty(context, leaderUUID,
                            "§c⚠ Contract breach detected §7[" + c.getString("id") + "]§c. " +
                                    "Use §f/ss contract decide " + c.getString("id") +
                                    " <fine/jail/war/sanctions/pardon>");
                    return;
                }
            }
        }

        // Default: flag publicly, no auto-enforcement yet (CourtSystem handles this later)
        if (context.getServer() != null) {
            context.getServer().getPlayerManager().getPlayerList().forEach(p ->
                    p.sendMessage(Text.literal(
                            "§c⚠ Contract §7[" + c.getString("id") + "]§c has been breached. " +
                                    "Reason: §f" + c.getString("breachReason"))));
        }
    }

    private static void applyFine(ServerPlayerEntity context, ServerWorld world,
                                  NbtCompound c) {
        PlayerStateData ps  = PlayerStateData.get(world);
        String currencyID   = c.getString("currencyID");
        int    fineAmount   = c.getInt("amount");
        String breachingUUID = c.getString("partyBUUID");

        if (currencyID.isEmpty()) currencyID = "gold";

        int balance = ps.getWallet(breachingUUID, currencyID);
        if (balance >= fineAmount) {
            ps.adjustWallet(breachingUUID, currencyID, -fineAmount);
            ps.adjustWallet(c.getString("partyAUUID"), currencyID, fineAmount);
            notifyParty(context, breachingUUID,
                    "§cFine applied: §f" + fineAmount + " " + currencyID +
                            " §cdeducted for contract breach.");
        } else {
            // Can't pay fine — escalate to jail flag
            flagForArrest(context, world, breachingUUID,
                    "Unpaid contract fine: " + fineAmount + " " + currencyID);
        }
    }

    private static void applyJail(ServerPlayerEntity context, ServerWorld world,
                                  NbtCompound c) {
        flagForArrest(context, world, c.getString("partyBUUID"),
                "Contract breach: " + c.getString("breachReason"));
    }

    private static void applyWarFlag(ServerPlayerEntity context, ServerWorld world,
                                     NbtCompound c) {
        DivisionData divData = DivisionData.get(world);
        PlayerStateData ps   = PlayerStateData.get(world);
        String divID = ps.getDivisionID(c.getString("partyBUUID"));
        if (divID != null) {
            // Mark as valid war target (casus belli) — DiplomacySystem will read this
            NbtCompound div = divData.getDivisionById(divID);
            if (div != null) {
                div.putBoolean("hasCasusBelli", true);
                div.putString("casusBelliReason", "Contract breach: " + c.getString("id"));
            }
            notifyParty(context, c.getString("partyAUUID"),
                    "§cContract breach recorded. §e" +
                            (div != null ? div.getString("name") : "The other party") +
                            " §cis now a valid war target (casus belli).");
        }
    }

    private static void applySanctions(ServerPlayerEntity context, ServerWorld world,
                                       NbtCompound c) {
        // Flags the breaching division as sanctioned — TradeSystem will check this
        DivisionData divData = DivisionData.get(world);
        PlayerStateData ps   = PlayerStateData.get(world);
        String divID = ps.getDivisionID(c.getString("partyBUUID"));
        if (divID != null) {
            NbtCompound div = divData.getDivisionById(divID);
            if (div != null) {
                div.putBoolean("isSanctioned", true);
                notifyParty(context, c.getString("partyAUUID"),
                        "§eTrade sanctions applied against the breaching party.");
                notifyParty(context, c.getString("partyBUUID"),
                        "§cYour division has been sanctioned due to contract breach §7[" +
                                c.getString("id") + "]");
            }
        }
    }

    private static void flagForArrest(ServerPlayerEntity context, ServerWorld world,
                                      String targetUUID, String reason) {
        PlayerStateData ps = PlayerStateData.get(world);
        ps.setWanted(targetUUID, true);
        ps.setWantedReason(targetUUID, reason);
        notifyParty(context, targetUUID,
                "§c⚠ You have been flagged for arrest. Reason: §f" + reason);
        if (context.getServer() != null) {
            context.getServer().getPlayerManager().getPlayerList().forEach(p ->
                    p.sendMessage(Text.literal(
                            "§c⚠ §e" + targetUUID.substring(0, 8) +
                                    " §cis wanted for: §f" + reason)));
        }
    }

    // -------------------------------------------------------------------------
    // TICK — called from SovereignState main tick loop
    // -------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        server.getWorlds().forEach(world -> {
            ContractData contractData = ContractData.get(world);

            for (NbtCompound c : contractData.getActiveContracts()) {
                String type = c.getString("type");
                String id   = c.getString("id");

                switch (type) {
                    case "trade_agreement" -> tickTradeAgreement(server, world, contractData, c, id);
                    case "nap"             -> tickNAP(server, world, contractData, c, id);
                    case "loan"            -> tickLoan(server, world, contractData, c, id);
                }
            }
        });
    }

    private static void tickTradeAgreement(MinecraftServer server, ServerWorld world,
                                           ContractData contractData,
                                           NbtCompound c, String id) {
        int days     = c.getInt("daysSinceLastPayment");
        int interval = c.getInt("intervalDays");

        contractData.incrementDays(id);

        if (days + 1 >= interval) {
            // Payment due — attempt auto-deduct
            PlayerStateData ps   = PlayerStateData.get(world);
            String payerUUID     = c.getString("partyBUUID");
            String receiverUUID  = c.getString("partyAUUID");
            String currencyID    = c.getString("currencyID");
            int    amount        = c.getInt("amount");

            int balance = ps.getWallet(payerUUID, currencyID);
            if (balance >= amount) {
                ps.adjustWallet(payerUUID,    currencyID, -amount);
                ps.adjustWallet(receiverUUID, currencyID,  amount);
                contractData.resetPaymentTimer(id);
                notifyBothParties(server, c,
                        "§aTrade agreement payment of §f" + amount + " " + currencyID +
                                "§a processed §7[" + id + "]");
            } else {
                // Breach — can't pay
                contractData.markBreached(id, "Missed payment of " + amount + " " + currencyID);
                notifyBothParties(server, c,
                        "§c⚠ Trade agreement §7[" + id + "]§c breached — missed payment!");
            }
        }
    }

    private static void tickNAP(MinecraftServer server, ServerWorld world,
                                ContractData contractData,
                                NbtCompound c, String id) {
        int daysActive   = c.getInt("daysActive");
        int durationDays = c.getInt("durationDays");

        contractData.incrementDays(id);

        if (daysActive + 1 >= durationDays) {
            contractData.setStatus(id, "completed");
            notifyBothParties(server, c,
                    "§eNon-Aggression Pact §7[" + id + "]§e has expired.");
        }
    }

    private static void tickLoan(MinecraftServer server, ServerWorld world,
                                 ContractData contractData,
                                 NbtCompound c, String id) {
        if (!c.getBoolean("disbursed")) return;

        int daysActive = c.getInt("daysActive");
        int repayDays  = c.getInt("repayDays");

        contractData.incrementDays(id);

        if (daysActive + 1 >= repayDays) {
            // Repayment due
            PlayerStateData ps    = PlayerStateData.get(world);
            String borrowerUUID   = c.getString("partyBUUID");
            String lenderUUID     = c.getString("partyAUUID");
            String currencyID     = c.getString("currencyID");
            int    totalOwed      = c.getInt("totalOwed");

            int balance = ps.getWallet(borrowerUUID, currencyID);
            if (balance >= totalOwed) {
                ps.adjustWallet(borrowerUUID, currencyID, -totalOwed);
                ps.adjustWallet(lenderUUID,   currencyID,  totalOwed);
                contractData.setStatus(id, "completed");
                notifyBothParties(server, c,
                        "§aLoan §7[" + id + "]§a repaid in full. §f" +
                                totalOwed + " " + currencyID + "§a transferred.");
            } else {
                contractData.markBreached(id,
                        "Loan repayment defaulted. Owed: " + totalOwed + " " + currencyID);
                notifyBothParties(server, c,
                        "§c⚠ Loan §7[" + id + "]§c defaulted — borrower could not repay!");
            }
        }
    }

    // -------------------------------------------------------------------------
    // NOTIFY HELPERS
    // -------------------------------------------------------------------------

    private static void notifyParty(ServerPlayerEntity context,
                                    String targetUUID, String message) {
        if (context.getServer() == null) return;
        context.getServer().getPlayerManager().getPlayerList().forEach(p -> {
            if (p.getUuid().toString().equals(targetUUID))
                p.sendMessage(Text.literal(message));
        });
    }

    private static void notifyOtherParty(ServerPlayerEntity player, ServerWorld world,
                                         NbtCompound c, String message) {
        String uuid  = player.getUuid().toString();
        String other = c.getString("partyAUUID").equals(uuid)
                ? c.getString("partyBUUID")
                : c.getString("partyAUUID");
        notifyParty(player, other, message);
    }

    private static void notifyBothParties(MinecraftServer server, NbtCompound c,
                                          String message) {
        String a = c.getString("partyAUUID");
        String b = c.getString("partyBUUID");
        server.getPlayerManager().getPlayerList().forEach(p -> {
            String uuid = p.getUuid().toString();
            if (uuid.equals(a) || uuid.equals(b))
                p.sendMessage(Text.literal(message));
        });
    }
}