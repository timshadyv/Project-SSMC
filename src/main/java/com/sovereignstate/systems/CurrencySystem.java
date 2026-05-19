package com.sovereignstate.systems;

import com.sovereignstate.data.CurrencyData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class CurrencySystem {

    // --- Create a currency ---

    public static String createCurrency(ServerPlayerEntity player, ServerWorld world,
                                        String name, String namePlural, String symbol,
                                        int baseValue, int mintRate) {
        PlayerStateData playerState = PlayerStateData.get(world);
        DivisionData divData = DivisionData.get(world);
        CurrencyData currencyData = CurrencyData.get(world);

        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);

        if (divisionID == null || divisionID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou must be in a division to create a currency."));
            return null;
        }

        // Check if division already has a currency
        NbtCompound existing = currencyData.getCurrencyByDivision(divisionID);
        if (existing != null) {
            player.sendMessage(Text.literal("§cYour division already has a currency: §e" +
                    existing.getString("name")));
            return null;
        }

        // Check leader permission
        NbtCompound div = divData.getDivisionById(divisionID);
        if (div == null) {
            player.sendMessage(Text.literal("§cDivision not found."));
            return null;
        }

        if (!div.getString("leaderUUID").equals(uuid)) {
            player.sendMessage(Text.literal("§cOnly the division leader can create a currency."));
            return null;
        }

        String currencyID = currencyData.createCurrency(name, namePlural, symbol,
                divisionID, baseValue, mintRate);

        // Set as official currency
        divData.setOfficialCurrency(divisionID, currencyID);

        player.sendMessage(Text.literal("§a--- Currency Created ---"));
        player.sendMessage(Text.literal("§eName: §f" + name + " (" + namePlural + ")"));
        player.sendMessage(Text.literal("§eSymbol: §f" + symbol));
        player.sendMessage(Text.literal("§eBase Value: §f" + baseValue));
        player.sendMessage(Text.literal("§eMint Rate: §f" + mintRate + " coins per ingot"));
        player.sendMessage(Text.literal("§eCurrency ID: §f" + currencyID));

        return currencyID;
    }

    // --- Mint coins ---

    public static void mintCoins(ServerPlayerEntity player, ServerWorld world,
                                 String currencyID, int amount) {
        CurrencyData currencyData = CurrencyData.get(world);
        PlayerStateData playerState = PlayerStateData.get(world);
        DivisionData divData = DivisionData.get(world);

        NbtCompound currency = currencyData.getCurrencyById(currencyID);
        if (currency == null) {
            player.sendMessage(Text.literal("§cCurrency not found: " + currencyID));
            return;
        }

        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);
        String issuingDivisionID = currency.getString("issuingDivisionID");

        // Only issuing division leader can mint
        if (!divisionID.equals(issuingDivisionID)) {
            player.sendMessage(Text.literal("§cOnly the issuing division can mint this currency."));
            return;
        }

        NbtCompound div = divData.getDivisionById(divisionID);
        if (div == null || !div.getString("leaderUUID").equals(uuid)) {
            player.sendMessage(Text.literal("§cOnly the division leader can mint coins."));
            return;
        }

        currencyData.mintCoins(currencyID, amount);
        divData.adjustTreasury(divisionID, currencyID, amount);
        currencyData.applyInflation(currencyID);

        float inflationRate = currencyData.getInflationRate(currencyID);
        player.sendMessage(Text.literal("§a+" + amount + " §e" + currency.getString("name") +
                " §aminted into division treasury."));
        player.sendMessage(Text.literal("§eCurrent inflation rate: §f" +
                String.format("%.1f", inflationRate * 100) + "%"));
    }

    // --- Transfer coins between players ---

    public static void transferCoins(ServerPlayerEntity sender, ServerWorld world,
                                     ServerPlayerEntity receiver, String currencyID,
                                     int amount) {
        PlayerStateData playerState = PlayerStateData.get(world);
        String senderUUID = sender.getUuid().toString();
        String receiverUUID = receiver.getUuid().toString();

        int senderWallet = playerState.getWallet(senderUUID, currencyID);
        if (senderWallet < amount) {
            sender.sendMessage(Text.literal("§cInsufficient funds. You have §e" +
                    senderWallet + " §c" + currencyID + "."));
            return;
        }

        playerState.adjustWallet(senderUUID, currencyID, -amount);
        playerState.adjustWallet(receiverUUID, currencyID, amount);

        sender.sendMessage(Text.literal("§aSent §e" + amount + " §a" + currencyID +
                " §ato §e" + receiver.getName().getString()));
        receiver.sendMessage(Text.literal("§aReceived §e" + amount + " §a" + currencyID +
                " §afrom §e" + sender.getName().getString()));
    }

    // --- Exchange currencies ---

    public static void exchangeCurrency(ServerPlayerEntity player, ServerWorld world,
                                        String fromCurrencyID, String toCurrencyID,
                                        int amount) {
        CurrencyData currencyData = CurrencyData.get(world);
        PlayerStateData playerState = PlayerStateData.get(world);
        String uuid = player.getUuid().toString();

        NbtCompound fromCurrency = currencyData.getCurrencyById(fromCurrencyID);
        NbtCompound toCurrency = currencyData.getCurrencyById(toCurrencyID);

        if (fromCurrency == null || toCurrency == null) {
            player.sendMessage(Text.literal("§cOne or both currencies not found."));
            return;
        }

        int wallet = playerState.getWallet(uuid, fromCurrencyID);
        if (wallet < amount) {
            player.sendMessage(Text.literal("§cInsufficient funds."));
            return;
        }

        float rate = currencyData.getExchangeRate(fromCurrencyID, toCurrencyID);
        int received = (int)(amount * rate);

        playerState.adjustWallet(uuid, fromCurrencyID, -amount);
        playerState.adjustWallet(uuid, toCurrencyID, received);

        player.sendMessage(Text.literal("§aExchanged §e" + amount + " " +
                fromCurrency.getString("name") + " §a→ §e" + received + " " +
                toCurrency.getString("name")));
    }

    // --- Withdraw from treasury ---

    public static void withdrawFromTreasury(ServerPlayerEntity player, ServerWorld world,
                                            String currencyID, int amount) {
        PlayerStateData playerState = PlayerStateData.get(world);
        DivisionData divData = DivisionData.get(world);
        String uuid = player.getUuid().toString();
        String divisionID = playerState.getDivisionID(uuid);

        if (divisionID == null || divisionID.isEmpty()) {
            player.sendMessage(Text.literal("§cYou are not in a division."));
            return;
        }

        NbtCompound div = divData.getDivisionById(divisionID);
        if (div == null || !div.getString("leaderUUID").equals(uuid)) {
            player.sendMessage(Text.literal("§cOnly the division leader can withdraw from treasury."));
            return;
        }

        int treasury = divData.getTreasury(divisionID, currencyID);
        if (treasury < amount) {
            player.sendMessage(Text.literal("§cInsufficient treasury funds. Treasury: §e" + treasury));
            return;
        }

        divData.adjustTreasury(divisionID, currencyID, -amount);
        playerState.adjustWallet(uuid, currencyID, amount);

        player.sendMessage(Text.literal("§aWithdrew §e" + amount + " §a" + currencyID +
                " §afrom treasury."));
    }

    // --- Show currency info ---

    public static void showCurrencyInfo(ServerPlayerEntity player, ServerWorld world,
                                        String currencyID) {
        CurrencyData currencyData = CurrencyData.get(world);
        NbtCompound currency = currencyData.getCurrencyById(currencyID);

        if (currency == null) {
            player.sendMessage(Text.literal("§cCurrency not found: " + currencyID));
            return;
        }

        player.sendMessage(Text.literal("§6--- Currency Info ---"));
        player.sendMessage(Text.literal("§eName: §f" + currency.getString("name")));
        player.sendMessage(Text.literal("§eSymbol: §f" + currency.getString("symbol")));
        player.sendMessage(Text.literal("§eBase Value: §f" + currency.getInt("baseValue")));
        player.sendMessage(Text.literal("§eIn Circulation: §f" +
                currency.getInt("totalInCirculation")));
        player.sendMessage(Text.literal("§eInflation Rate: §f" +
                String.format("%.1f", currency.getFloat("inflationRate") * 100) + "%"));
        player.sendMessage(Text.literal("§eLegal Tender: §f" +
                currency.getBoolean("isLegalTender")));
        player.sendMessage(Text.literal("§eMint Rate: §f" +
                currency.getInt("mintRate") + " coins per ingot"));
    }

    // --- Tick: apply inflation ---

    public static void tick(MinecraftServer server) {
        server.getWorlds().forEach(world -> {
            CurrencyData currencyData = CurrencyData.get(world);
            List<NbtCompound> currencies = currencyData.getAllCurrencies();
            for (NbtCompound currency : currencies) {
                currencyData.applyInflation(currency.getString("id"));
            }
        });
    }
}