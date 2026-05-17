package com.sovereignstate.util;

import com.sovereignstate.data.CurrencyData;
import com.sovereignstate.data.PlayerStateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class TextHelper {

    // --- Political Titles ---

    public static String getPoliticalTitle(String governmentType, String gender, String office) {
        boolean female = gender.equals("female");

        return switch (office) {
            case "head_of_state" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "Matriarch";
                case "patriarchal_state", "androcracy" -> "Patriarch";
                case "monarchy" -> female ? "Queen" : "King";
                case "republic", "democracy" -> "President";
                case "oligarchy" -> "Chancellor";
                case "theocracy" -> female ? "High Priestess" : "High Priest";
                case "tribal" -> female ? "Chieftainess" : "Chieftain";
                case "federation" -> "Prime Minister";
                default -> "Leader";
            };
            case "province_governor" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "Lady-Governor";
                case "patriarchal_state", "androcracy" -> "Lord-Governor";
                default -> "Governor";
            };
            case "military_commander" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "War-Mother";
                case "patriarchal_state", "androcracy" -> "Warlord";
                default -> "General";
            };
            case "chief_judge" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "Lady Justice";
                case "patriarchal_state", "androcracy" -> "Lord Justice";
                default -> "Judge";
            };
            case "religious_leader" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "High Priestess";
                case "patriarchal_state", "androcracy" -> "High Priest";
                default -> female ? "High Priestess" : "High Priest";
            };
            case "party_leader" -> switch (governmentType) {
                case "matriarchal_state", "gynarchy" -> "Chair-Mother";
                case "patriarchal_state", "androcracy" -> "Chair-Father";
                default -> "Party Leader";
            };
            default -> "Official";
        };
    }

    // --- Format Currency ---

    public static String formatCurrency(int amount, String currencyID, ServerWorld world) {
        CurrencyData data = CurrencyData.get(world);
        NbtCompound currency = data.getCurrencyById(currencyID);
        if (currency == null) return amount + " coins";
        String name = amount == 1 ? currency.getString("name") : currency.getString("namePlural");
        String symbol = currency.getString("symbol");
        return symbol + amount + " " + name;
    }

    // --- Dark Law Category ---

    public static String getDarkLawCategory(String lawName) {
        if (lawName.startsWith("forced_labour") ||
                lawName.equals("corvee") ||
                lawName.equals("slave_class") ||
                lawName.equals("prison_labour") ||
                lawName.equals("debt_bondage")) {
            return "forced_labour";
        }
        if (lawName.startsWith("cultural_proscription") ||
                lawName.equals("cultural_dress_ban") ||
                lawName.equals("cultural_suppression") ||
                lawName.equals("forced_assimilation")) {
            return "cultural_suppression";
        }
        if (lawName.equals("party_ban") ||
                lawName.equals("political_prisoner_designation") ||
                lawName.equals("press_censorship") ||
                lawName.equals("sedition_law")) {
            return "political_suppression";
        }
        if (lawName.equals("caste_tax") ||
                lawName.equals("gender_tax_differential") ||
                lawName.equals("trade_embargo") ||
                lawName.equals("property_confiscation")) {
            return "economic_coercion";
        }
        return null;
    }

    // --- Broadcast To Social Class ---

    public static void broadcastToSocialClass(MinecraftServer server, String socialClass, String message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            PlayerStateData stateData = PlayerStateData.get(world);
            String playerClass = stateData.getSocialClass(player.getUuid().toString());
            if (socialClass.equals(playerClass)) {
                player.sendMessage(Text.literal("[" + socialClass + "] " + message));
            }
        }
    }

    // --- Broadcast To All ---

    public static void broadcastToAll(MinecraftServer server, String message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal("[Sovereign State] " + message));
        }
    }

    // --- Send Message To Player ---

    public static void sendMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(message));
    }
}