package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CurrencyData extends PersistentState {

    private NbtList currencies = new NbtList();

    public static CurrencyData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    CurrencyData state = new CurrencyData();
                    state.currencies = nbt.getList("currencies", 10);
                    return state;
                },
                CurrencyData::new,
                "sovereignstate_currencies"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("currencies", currencies);
        return nbt;
    }

    // --- Create ---

    public String createCurrency(String name, String namePlural, String symbol, String issuingDivisionID, int baseValue, int mintRate) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound currency = new NbtCompound();
        currency.putString("id", id);
        currency.putString("name", name);
        currency.putString("namePlural", namePlural);
        currency.putString("symbol", symbol);
        currency.putString("issuingDivisionID", issuingDivisionID);
        currency.putInt("baseValue", baseValue);
        currency.putString("forgeryKey", UUID.randomUUID().toString());
        currency.putFloat("inflationRate", 0f);
        currency.putBoolean("isLegalTender", true);
        currency.putInt("mintRate", mintRate);
        currency.putInt("totalInCirculation", 0);

        // Default tiers
        currency.putString("tier_1_name", "Copper");
        currency.putInt("tier_1_multiplier", 1);
        currency.putBoolean("tier_1_isStackable", true);

        currency.putString("tier_2_name", "Silver");
        currency.putInt("tier_2_multiplier", 10);
        currency.putBoolean("tier_2_isStackable", true);

        currency.putString("tier_3_name", "Gold");
        currency.putInt("tier_3_multiplier", 100);
        currency.putBoolean("tier_3_isStackable", true);

        currency.putString("tier_4_name", "Platinum");
        currency.putInt("tier_4_multiplier", 1000);
        currency.putBoolean("tier_4_isStackable", false);

        currency.putString("tier_5_name", "Bond");
        currency.putInt("tier_5_multiplier", 10000);
        currency.putBoolean("tier_5_isStackable", false);

        currencies.add(currency);
        markDirty();
        return id;
    }

    // --- Get ---

    public NbtCompound getCurrencyById(String id) {
        for (int i = 0; i < currencies.size(); i++) {
            NbtCompound currency = currencies.getCompound(i);
            if (currency.getString("id").equals(id)) return currency;
        }
        return null;
    }

    public NbtCompound getCurrencyByDivision(String divisionID) {
        for (int i = 0; i < currencies.size(); i++) {
            NbtCompound currency = currencies.getCompound(i);
            if (currency.getString("issuingDivisionID").equals(divisionID)) return currency;
        }
        return null;
    }

    public List<NbtCompound> getAllCurrencies() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < currencies.size(); i++) list.add(currencies.getCompound(i));
        return list;
    }

    // --- Delete ---

    public void deleteCurrency(String id) {
        currencies.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }

    // --- Exchange Rate ---

    public float getExchangeRate(String currencyID1, String currencyID2) {
        NbtCompound c1 = getCurrencyById(currencyID1);
        NbtCompound c2 = getCurrencyById(currencyID2);
        if (c1 == null || c2 == null) return 1f;
        int base1 = c1.getInt("baseValue");
        int base2 = c2.getInt("baseValue");
        if (base2 == 0) return 1f;
        return (float) base1 / base2;
    }

    // --- Mint ---

    public void mintCoins(String currencyID, int amount) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return;
        int current = currency.getInt("totalInCirculation");
        currency.putInt("totalInCirculation", current + amount);
        markDirty();
    }

    // --- Inflation ---

    public void applyInflation(String currencyID) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return;
        int inCirculation = currency.getInt("totalInCirculation");
        int baseValue = currency.getInt("baseValue");
        float inflationRate = currency.getFloat("inflationRate");

        if (inCirculation > baseValue * 1000) {
            inflationRate = Math.min(inflationRate + 0.01f, 1.0f);
        } else if (inflationRate > 0) {
            inflationRate = Math.max(inflationRate - 0.005f, 0f);
        }

        currency.putFloat("inflationRate", inflationRate);
        markDirty();
    }

    public float getInflationRate(String currencyID) {
        NbtCompound currency = getCurrencyById(currencyID);
        return currency != null ? currency.getFloat("inflationRate") : 0f;
    }

    // --- Texture ---

    public void updateTexture(String currencyID, boolean isObverse, byte[] textureData) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return;
        if (isObverse) {
            currency.putByteArray("textureObverse", textureData);
        } else {
            currency.putByteArray("textureReverse", textureData);
        }
        markDirty();
    }

    public byte[] getTexture(String currencyID, boolean isObverse) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return new byte[256];
        return isObverse ? currency.getByteArray("textureObverse") : currency.getByteArray("textureReverse");
    }

    // --- Legal Tender ---

    public void setLegalTender(String currencyID, boolean isLegal) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return;
        currency.putBoolean("isLegalTender", isLegal);
        markDirty();
    }

    // --- Destroy Coins (deflation) ---

    public void destroyCoins(String currencyID, int amount) {
        NbtCompound currency = getCurrencyById(currencyID);
        if (currency == null) return;
        int current = currency.getInt("totalInCirculation");
        currency.putInt("totalInCirculation", Math.max(0, current - amount));
        markDirty();
    }
}