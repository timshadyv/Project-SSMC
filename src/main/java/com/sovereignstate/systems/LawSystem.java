package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.GovernmentTypes;
import com.sovereignstate.data.LawTypes;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class LawSystem {

    // --- Submit a law ---

    public static void submitLaw(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, String lawName) {
        DivisionData divData = DivisionData.get(world);
        NbtCompound div = divData.getDivisionById(divisionID);

        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return;
        }

        // Validate law exists in registry
        if (!LawTypes.isValid(lawName)) {
            player.sendMessage(Text.literal("§cUnknown law: " + lawName));
            player.sendMessage(Text.literal("§eUse /ss laws to see all valid laws."));
            return;
        }

        // Check if already active
        if (divData.hasLaw(divisionID, lawName)) {
            player.sendMessage(Text.literal("§cThis law is already active."));
            return;
        }

        // Check if law is allowed under this government type
        String govType = div.getString("governmentType");
        GovernmentTypes.GovernmentType gov = GovernmentTypes.get(govType);
        String govCategory = gov != null ? gov.category() : "";

        LawTypes.LawType lawType = LawTypes.get(lawName);
        if (!lawType.isAllowedUnder(govCategory)) {
            player.sendMessage(Text.literal(
                    "§cThe law '§f" + lawType.displayName() +
                            "§c' is not available under a §f" + govType + "§c government."));
            return;
        }

        // Check constitution for conflicts
        List<String> constitution = divData.getConstitution(divisionID);
        for (String clause : constitution) {
            if (clause.contains("no_" + lawName) || clause.contains("prohibit_" + lawName)) {
                player.sendMessage(Text.literal(
                        "§cThis law conflicts with your constitution: §e" + clause));
                return;
            }
        }

        String leaderUUID = div.getString("leaderUUID");
        String uuid = player.getUuid().toString();

        // Authoritarian and monarchy governments: only leader can pass laws directly
        if (govCategory.equals("monarchy") || govCategory.equals("authoritarian")) {
            if (!uuid.equals(leaderUUID)) {
                player.sendMessage(Text.literal(
                        "§cOnly the leader can pass laws in a " + govType + "."));
                return;
            }
            enactLaw(player, world, divisionID, lawName, lawType);
            return;
        }

        // All other governments: submit for vote
        submitForVote(player, world, divisionID, lawName, lawType);
    }

    // --- Enact law directly ---

    private static void enactLaw(ServerPlayerEntity player, ServerWorld world,
                                 String divisionID, String lawName,
                                 LawTypes.LawType lawType) {
        DivisionData divData = DivisionData.get(world);
        divData.addLaw(divisionID, lawName);

        NbtCompound div = divData.getDivisionById(divisionID);
        String divName = div != null ? div.getString("name") : "Unknown";

        MinecraftServer server = player.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§a§e" + divName + "§a has enacted: §f" + lawType.displayName() +
                            " §7[" + lawType.category() + "]");
        }

        player.sendMessage(Text.literal("§aLaw enacted: §f" + lawType.displayName()));
    }

    // --- Submit for vote ---

    private static void submitForVote(ServerPlayerEntity player, ServerWorld world,
                                      String divisionID, String lawName,
                                      LawTypes.LawType lawType) {
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
                    "§6Vote started in §e" + divName + "§6: §f" + lawType.displayName() +
                            " §7[" + lawType.category() + "]");
            TextHelper.broadcastToAll(server,
                    "§eUse §f/ss vote yes §e<lawname> or §f/ss vote no §e<lawname> to vote.");
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

        String displayName = LawTypes.isValid(lawName)
                ? LawTypes.get(lawName).displayName()
                : lawName;

        MinecraftServer server = player.getServer();
        if (server != null) {
            TextHelper.broadcastToAll(server,
                    "§c§e" + div.getString("name") +
                            "§c has repealed: §f" + displayName);
        }

        player.sendMessage(Text.literal("§aLaw repealed: §f" + displayName));
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
            if (LawTypes.isValid(law)) {
                LawTypes.LawType lawType = LawTypes.get(law);
                player.sendMessage(Text.literal(
                        "§a✓ §f" + lawType.displayName() + " §7[" + lawType.category() + "]"));
            } else {
                player.sendMessage(Text.literal("§e? §f" + law + " §7[unknown]"));
            }
        }
    }

    // --- Tick: process votes ---

    public static void tick(MinecraftServer server) {
        server.getWorlds().forEach(world -> {
            WorldStateData worldState = WorldStateData.get(world);
            DivisionData divData = DivisionData.get(world);

            for (NbtCompound div : divData.getAllDivisions()) {
                String divisionID = div.getString("id");

                for (LawTypes.LawType lawType : LawTypes.getAll()) {
                    String lawName = lawType.id();
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
                                "§aVote passed in §e" + divName + "§a: §f" +
                                        lawType.displayName() +
                                        " §7(Yes: " + yes + " No: " + no + ")");
                    } else {
                        TextHelper.broadcastToAll(server,
                                "§cVote failed in §e" + divName + "§c: §f" +
                                        lawType.displayName() +
                                        " §7(Yes: " + yes + " No: " + no + ")");
                    }
                }
            }
        });
    }
}