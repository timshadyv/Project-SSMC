package com.sovereignstate.systems;

import com.sovereignstate.data.CurrencyData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.WorldStateData;
import com.sovereignstate.util.TextHelper;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class TaxSystem {

    private static final int DAY_TICKS = 24000;

    // --- Main tick ---

    public static void tick(MinecraftServer server) {
        server.getWorlds().forEach(world -> {
            WorldStateData worldState = WorldStateData.get(world);
            DivisionData divData = DivisionData.get(world);
            PlayerStateData playerState = PlayerStateData.get(world);

            int dayCounter = worldState.getIntTag("dayTickCounter") + 200;
            worldState.setIntTag("dayTickCounter", dayCounter);
            boolean isNewDay = dayCounter >= DAY_TICKS;
            if (isNewDay) worldState.setIntTag("dayTickCounter", 0);

            List<NbtCompound> divisions = divData.getAllDivisions();

            for (NbtCompound div : divisions) {
                String divisionID = div.getString("id");
                String currencyID = div.getString("officialCurrencyID");

                if (currencyID.isEmpty()) continue;

                int incomeTaxRate = getIncomeTaxRate(divData, divisionID);
                int propertyTaxRate = getPropertyTaxRate(divData, divisionID);

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    String uuid = player.getUuid().toString();
                    String playerDivID = playerState.getDivisionID(uuid);

                    if (!divisionID.equals(playerDivID)) continue;

                    ServerWorld playerWorld = player.getServerWorld();

                    // Income tax with class multiplier
                    if (incomeTaxRate > 0) {
                        String socialClass = playerState.getSocialClass(uuid);
                        if (socialClass == null || socialClass.isEmpty()) socialClass = "peasant";
                        float classMultiplier = SocialClassSystem.getTaxMultiplier(socialClass);

                        int wallet = playerState.getWallet(uuid, currencyID);
                        int tax = (int) ((wallet * incomeTaxRate) / 100.0f * classMultiplier);
                        if (tax > 0) {
                            playerState.adjustWallet(uuid, currencyID, -tax);
                            divData.adjustTreasury(divisionID, currencyID, tax);
                            player.sendMessage(Text.literal(
                                    "§eTax collected: §c-" + tax + " §e" + currencyID +
                                            " §7(income " + incomeTaxRate + "% × " + socialClass + " ×" + classMultiplier + ")"));
                        }
                    }

                    // Caste tax law
                    if (divData.hasLaw(divisionID, "caste_tax")) {
                        applyCasteTax(player, playerState, divData, divisionID, currencyID);
                    }

                    // Gender tax differential
                    if (divData.hasLaw(divisionID, "gender_tax_differential")) {
                        applyGenderTax(player, playerState, divData, divisionID, currencyID, uuid);
                    }
                }

                if (isNewDay && propertyTaxRate > 0) {
                    collectPropertyTax(server, world, divisionID, currencyID,
                            propertyTaxRate, playerState, divData, worldState);
                }

                if (isNewDay) {
                    transferTaxUpHierarchy(world, divisionID, currencyID, divData);
                }
            }
        });
    }

    // --- Tax rates from laws ---

    private static int getIncomeTaxRate(DivisionData divData, String divisionID) {
        if (divData.hasLaw(divisionID, "income_tax_high")) return 30;
        if (divData.hasLaw(divisionID, "income_tax_medium")) return 15;
        if (divData.hasLaw(divisionID, "income_tax_low")) return 5;
        return 0;
    }

    private static int getPropertyTaxRate(DivisionData divData, String divisionID) {
        if (divData.hasLaw(divisionID, "property_tax")) return 10;
        return 0;
    }

    // --- Caste tax ---

    private static void applyCasteTax(ServerPlayerEntity player,
                                      PlayerStateData playerState,
                                      DivisionData divData,
                                      String divisionID, String currencyID) {
        String uuid = player.getUuid().toString();
        String socialClass = playerState.getSocialClass(uuid);
        if (socialClass == null || socialClass.isEmpty()) socialClass = "peasant";

        float multiplier = SocialClassSystem.getTaxMultiplier(socialClass);
        if (multiplier <= 0) return; // royalty exempt

        int wallet = playerState.getWallet(uuid, currencyID);
        int extraTax = (int) (wallet * 0.05f * multiplier);
        if (extraTax > 0) {
            playerState.adjustWallet(uuid, currencyID, -extraTax);
            divData.adjustTreasury(divisionID, currencyID, extraTax);
            player.sendMessage(Text.literal(
                    "§eCaste tax: §c-" + extraTax + " §7(" + socialClass + ")"));
        }
    }

    // --- Gender tax ---

    private static void applyGenderTax(ServerPlayerEntity player,
                                       PlayerStateData playerState,
                                       DivisionData divData,
                                       String divisionID, String currencyID,
                                       String uuid) {
        String gender = playerState.getGender(uuid);
        if (gender.equals("female")) {
            int wallet = playerState.getWallet(uuid, currencyID);
            int extraTax = (wallet * 10) / 100;
            if (extraTax > 0) {
                playerState.adjustWallet(uuid, currencyID, -extraTax);
                divData.adjustTreasury(divisionID, currencyID, extraTax);
                player.sendMessage(Text.literal(
                        "§eGender tax: §c-" + extraTax + " §7(differential)"));
            }
        }
    }

    // --- Property tax ---

    private static void collectPropertyTax(MinecraftServer server,
                                           ServerWorld world,
                                           String divisionID, String currencyID,
                                           int rate, PlayerStateData playerState,
                                           DivisionData divData,
                                           WorldStateData worldState) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String uuid = player.getUuid().toString();
            if (!divisionID.equals(playerState.getDivisionID(uuid))) continue;

            int chunksOwned = 0;
            for (int x = -500; x <= 500; x++) {
                for (int z = -500; z <= 500; z++) {
                    String owner = worldState.getTag("chunk_" + x + "_" + z + "_owner");
                    if (player.getName().getString().equals(owner)) chunksOwned++;
                }
            }

            if (chunksOwned > 0) {
                int tax = chunksOwned * rate;
                playerState.adjustWallet(uuid, currencyID, -tax);
                divData.adjustTreasury(divisionID, currencyID, tax);
                player.sendMessage(Text.literal(
                        "§eProperty tax: §c-" + tax +
                                " §7(" + chunksOwned + " chunks × " + rate + ")"));
            }
        }
    }

    // --- Transfer taxes up hierarchy ---

    private static void transferTaxUpHierarchy(ServerWorld world,
                                               String divisionID,
                                               String currencyID,
                                               DivisionData divData) {
        NbtCompound div = divData.getDivisionById(divisionID);
        if (div == null) return;

        String parentID = div.getString("parentDivisionID");
        if (parentID.isEmpty()) return;

        int treasury = divData.getTreasury(divisionID, currencyID);
        int transfer = (treasury * 10) / 100;

        if (transfer > 0) {
            divData.adjustTreasury(divisionID, currencyID, -transfer);
            divData.adjustTreasury(parentID, currencyID, transfer);
        }
    }

    // --- Give coins ---

    public static void giveCoins(ServerPlayerEntity player, ServerWorld world,
                                 String currencyID, int amount) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String uuid = player.getUuid().toString();
        playerState.adjustWallet(uuid, currencyID, amount);
        player.sendMessage(Text.literal(
                "§aReceived §e" + amount + " §a" + currencyID + " coins."));
    }

    // --- Check balance ---

    public static void checkBalance(ServerPlayerEntity player, ServerWorld world) {
        PlayerStateData playerState = PlayerStateData.get(world);
        DivisionData divData = DivisionData.get(world);
        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);

        player.sendMessage(Text.literal("§6--- Your Balance ---"));

        if (divisionID != null && !divisionID.isEmpty()) {
            NbtCompound div = divData.getDivisionById(divisionID);
            if (div != null) {
                String currencyID = div.getString("officialCurrencyID");
                if (!currencyID.isEmpty()) {
                    int wallet = playerState.getWallet(uuid, currencyID);
                    player.sendMessage(Text.literal("§eWallet: §f" + wallet + " " + currencyID));
                    int treasury = divData.getTreasury(divisionID, currencyID);
                    player.sendMessage(Text.literal("§eDivision Treasury: §f" + treasury + " " + currencyID));
                } else {
                    player.sendMessage(Text.literal("§cYour division has no official currency set."));
                }
            }
        } else {
            player.sendMessage(Text.literal("§cYou are not in a division."));
        }
    }
}