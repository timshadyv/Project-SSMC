package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.util.DivisionHelper;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LawSystem {

    // All valid law names and their categories
    public static final Map<String, String> LAW_CATEGORIES = new HashMap<>();

    static {
        // Economic laws
        LAW_CATEGORIES.put("income_tax_low", "economic");
        LAW_CATEGORIES.put("income_tax_medium", "economic");
        LAW_CATEGORIES.put("income_tax_high", "economic");
        LAW_CATEGORIES.put("property_tax", "economic");
        LAW_CATEGORIES.put("sales_tax", "economic");
        LAW_CATEGORIES.put("free_trade", "economic");
        LAW_CATEGORIES.put("trade_embargo", "economic");
        LAW_CATEGORIES.put("currency_control", "economic");

        // Social laws
        LAW_CATEGORIES.put("male_suffrage", "social");
        LAW_CATEGORIES.put("female_suffrage", "social");
        LAW_CATEGORIES.put("universal_suffrage", "social");
        LAW_CATEGORIES.put("male_property_rights", "social");
        LAW_CATEGORIES.put("female_property_rights", "social");
        LAW_CATEGORIES.put("universal_property_rights", "social");
        LAW_CATEGORIES.put("freedom_of_religion", "social");
        LAW_CATEGORIES.put("state_religion_enforcement", "social");
        LAW_CATEGORIES.put("gender_self_declaration", "social");
        LAW_CATEGORIES.put("marriage_law_heterosexual", "social");
        LAW_CATEGORIES.put("marriage_law_open", "social");

        // Military laws
        LAW_CATEGORIES.put("conscription", "military");
        LAW_CATEGORIES.put("professional_military", "military");
        LAW_CATEGORIES.put("martial_law", "military");
        LAW_CATEGORIES.put("border_closure", "military");
        LAW_CATEGORIES.put("open_borders", "military");

        // Political laws
        LAW_CATEGORIES.put("free_press", "political");
        LAW_CATEGORIES.put("press_censorship", "political");
        LAW_CATEGORIES.put("party_ban", "political");
        LAW_CATEGORIES.put("multiparty_system", "political");
        LAW_CATEGORIES.put("sedition_law", "political");
        LAW_CATEGORIES.put("political_prisoner_designation", "political");
        LAW_CATEGORIES.put("loyalty_oath_requirement", "political");
        LAW_CATEGORIES.put("curfew", "political");

        // Dark laws
        LAW_CATEGORIES.put("slave_class", "dark");
        LAW_CATEGORIES.put("debt_bondage", "dark");
        LAW_CATEGORIES.put("corvee", "dark");
        LAW_CATEGORIES.put("prison_labour", "dark");
        LAW_CATEGORIES.put("forced_assimilation", "dark");
        LAW_CATEGORIES.put("cultural_suppression", "dark");
        LAW_CATEGORIES.put("cultural_dress_ban", "dark");
        LAW_CATEGORIES.put("caste_tax", "dark");
        LAW_CATEGORIES.put("gender_tax_differential", "dark");
        LAW_CATEGORIES.put("property_confiscation", "dark");
    }

    // --- Submit a law ---

    public static void submitLaw(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, String lawName) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        // Check law is valid
        if (!LAW_CATEGORIES.containsKey(lawName)) {
            player.sendMessage(Text.literal("§cUnknown law: " + lawName));
            player.sendMessage(Text.literal("§eUse /ss laws to see all valid laws."));
            return;
        }

        // Check if already active
        if (divData.hasLaw(divisionID, lawName)) {
            player.sendMessage(Text.literal("§cThis law is already active."));
            return;
        }

        String category = LAW_CATEGORIES.get(lawName);
        String darkCategory = TextHelper.getDarkLawCategory(lawName);

        // Check constitution for conflicts
        List<String> constitution = divData.getConstitution(divisionID);
        for (String clause : constitution) {
            if (clause.contains("no_" + lawName) || clause.contains("prohibit_" + lawName)) {
                player.sendMessage(Text.literal(
                        "§cThis law conflicts with your constitution: §e" + clause));
                return;
            }
        }

        // Get government type for permission check
        String govType = div.getString("governmentType");
        String leaderUUID = div.getString("leaderUUID");
        String uuid = player.getUuid().toString();

        // In monarchy/autocracy: only leader can pass laws
        if (govType.equals("monarchy") || govType.equals("autocracy") ||
                govType.equals("tribal")) {
            if (!uuid.equals(leaderUUID)) {
                player.sendMessage(Text.literal(
                        "§cOnly the leader can pass laws in a " + govType + "."));
                return;
            }
            enactLaw(player, world, divisionID, lawName, category, darkCategory);
            return;
        }

        // In other governments: submit for vote
        submitForVote(player, world, divisionID, lawName, category);
    }

    // --- Enact law directly ---

    private static void enactLaw(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, String lawName,
                                 String category, String darkCategory) {
        DivisionData divData = DivisionData.get(world);
        divData.addLaw(divisionID, lawName);

        NbtCompound div = divData.getDivisionById(divisionID);
        String divName = div != null ? div.getString("name") : "Unknown";

        MinecraftServer server = player.getServer();
        if (server != null) {
            String prefix = darkCategory != null ? "§c⚠ DARK LAW: " : "§a";
            TextHelper.broadcastToAll(server,
                    prefix + "§e" + divName + "§a has enacted: §f" + lawName +
                            " §7[" + category + "]");
        }

        player.sendMessage(Text.literal("§aLaw enacted: §f" + lawName));
    }

    // --- Submit for vote ---

    private static void submitForVote(ServerPlayerEntity player, ServerWorld world,
                                      String divisionID, String lawName,
                                      String category) {
        WorldStateData worldState = WorldStateData.get(world);

        String voteKey = "vote_" + divisionID + "_" + lawName;
        worldState.setTag(voteKey + "_status", "pending");
        worldState.setIntTag(voteKey + "_yes", 0);
        worldState.setIntTag(voteKey + "_no", 0);
        worldState.setIntTag(voteKey + "_timer", 6000); // 5 minutes

        MinecraftServer server = player.getServer();
        if (server != null) {
            DivisionData divData = DivisionData.get(world);
            NbtCompound div = divData.getDivisionById(divisionID);
            String divName = div != null ? div.getString("name") : "Unknown";
            TextHelper.broadcastToAll(server,
                    "§6Vote started in §e" + divName + "§6: §f" + lawName +
                            " §7[" + category + "]");
            TextHelper.broadcastToAll(server,
                    "§eUse §f/ss vote yes§e or §f/ss vote no§e to vote.");
        }
    }

    // --- Cast vote ---

    public static void castVote(ServerPlayerEntity player, ServerWorld world,
                                String divisionID, String lawName, boolean voteYes) {
        WorldStateData worldState = WorldStateData.get(world);
        String voteKey = "vote_" + divisionID + "_" + lawName;

        if (!worldState.getTag(voteKey + "_status").equals("pending")) {
            player.sendMessage(Text.literal("§cNo active vote on this law."));
            return;
        }

        // Check player hasn't voted
        String voterKey = voteKey + "_voter_" + player.getUuid();
        if (worldState.getBooleanTag(voterKey)) {
            player.sendMessage(Text.literal("§cYou have already voted."));
            return;
        }

        // Check player is in division
        PlayerStateData playerState = PlayerStateData.get(world);
        String playerDivID = playerState.getDivisionID(player.getUuid().toString());
        if (!divisionID.equals(playerDivID)) {
            player.sendMessage(Text.literal(
                    "§cYou must be a member of this division to vote."));
            return;
        }

        worldState.setBooleanTag(voterKey, true);
        if (voteYes) {
            worldState.setIntTag(voteKey + "_yes",
                    worldState.getIntTag(voteKey + "_yes") + 1);
        } else {
            worldState.setIntTag(voteKey + "_no",
                    worldState.getIntTag(voteKey + "_no") + 1);
        }

        player.sendMessage(Text.literal(
                "§aVote cast: §f" + (voteYes ? "YES" : "NO")));
    }

    // --- Repeal a law ---

    public static void repealLaw(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, String lawName) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        if (!divData.hasLaw(divisionID, lawName)) {
            player.sendMessage(Text.literal("§cThis law is not active."));
            return;
        }

        String leaderUUID = div.getString("leaderUUID");
        if (!player.getUuid().toString().equals(leaderUUID)) {
            player.sendMessage(Text.literal(
                    "§cOnly the division leader can repeal laws."));
            return;
        }

        divData.removeLaw(divisionID, lawName);

        MinecraftServer server = player.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§c§e" + div.getString("name") +
                            "§c has repealed: §f" + lawName);
        }

        player.sendMessage(Text.literal("§aLaw repealed: §f" + lawName));
    }

    // --- List laws ---

    public static void listLaws(ServerPlayerEntity player, ServerWorld world,
                                String divisionID) {
        DivisionData divData = DivisionData.get(world);
        List<String> laws = divData.getLaws(divisionID);

        if (laws.isEmpty()) {
            player.sendMessage(Text.literal("§eNo active laws in this division."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Active Laws ---"));
        for (String law : laws) {
            String category = LAW_CATEGORIES.getOrDefault(law, "unknown");
            String darkCategory = TextHelper.getDarkLawCategory(law);
            String prefix = darkCategory != null ? "§c⚠ " : "§a✓ ";
            player.sendMessage(Text.literal(
                    prefix + "§f" + law + " §7[" + category + "]"));
        }
    }

    // --- Tick: process votes ---

    public static void tick(MinecraftServer server) {
        server.getWorlds().forEach(world -> {
            WorldStateData worldState = WorldStateData.get(world);
            DivisionData divData = DivisionData.get(world);

            for (NbtCompound div : divData.getAllDivisions()) {
                String divisionID = div.getString("id");

                for (String lawName : LAW_CATEGORIES.keySet()) {
                    String voteKey = "vote_" + divisionID + "_" + lawName;
                    if (!worldState.getTag(voteKey + "_status").equals("pending")) continue;

                    int timer = worldState.getIntTag(voteKey + "_timer") - 200;
                    if (timer > 0) {
                        worldState.setIntTag(voteKey + "_timer", timer);
                        continue;
                    }

                    // Vote ended
                    int yes = worldState.getIntTag(voteKey + "_yes");
                    int no = worldState.getIntTag(voteKey + "_no");
                    worldState.setTag(voteKey + "_status", "closed");

                    String divName = div.getString("name");
                    if (yes > no) {
                        divData.addLaw(divisionID, lawName);
                        TextHelper.broadcastToAll(server,
                                "§aVote passed in §e" + divName + "§a: §f" + lawName +
                                        " §7(Yes: " + yes + " No: " + no + ")");
                    } else {
                        TextHelper.broadcastToAll(server,
                                "§cVote failed in §e" + divName + "§c: §f" + lawName +
                                        " §7(Yes: " + yes + " No: " + no + ")");
                    }
                }
            }
        });
    }
}