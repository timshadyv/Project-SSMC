package com.sovereignstate.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeData extends PersistentState {

    private NbtList offers  = new NbtList();
    private NbtList listings = new NbtList();

    public static TradeData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                nbt -> {
                    TradeData state = new TradeData();
                    state.offers   = nbt.getList("offers",   10);
                    state.listings = nbt.getList("listings", 10);
                    return state;
                },
                TradeData::new,
                "sovereignstate_trade"
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.put("offers",   offers);
        nbt.put("listings", listings);
        return nbt;
    }

    // -------------------------------------------------------------------------
    // TRADE OFFERS
    // -------------------------------------------------------------------------

    /**
     * Create a currency offer: senderUUID offers amount of currencyID to receiverUUID.
     * senderType / receiverType = "player" or "division"
     */
    public String createOffer(String senderUUID, String senderType,
                              String receiverUUID, String receiverType,
                              String currencyID, int amount,
                              String itemTag) {          // empty string if currency-only
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound offer = new NbtCompound();
        offer.putString("id",           id);
        offer.putString("senderUUID",   senderUUID);
        offer.putString("senderType",   senderType);
        offer.putString("receiverUUID", receiverUUID);
        offer.putString("receiverType", receiverType);
        offer.putString("currencyID",   currencyID);
        offer.putInt   ("amount",       amount);
        offer.putString("itemTag",      itemTag);        // serialised ItemStack NBT or ""
        offer.putString("status",       "pending");
        offer.putLong  ("createdAt",    System.currentTimeMillis());
        offers.add(offer);
        markDirty();
        return id;
    }

    public NbtCompound getOfferById(String id) {
        for (int i = 0; i < offers.size(); i++) {
            NbtCompound o = offers.getCompound(i);
            if (o.getString("id").equals(id)) return o;
        }
        return null;
    }

    public List<NbtCompound> getOffersForReceiver(String receiverUUID) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            NbtCompound o = offers.getCompound(i);
            if (o.getString("receiverUUID").equals(receiverUUID) &&
                    o.getString("status").equals("pending")) list.add(o);
        }
        return list;
    }

    public List<NbtCompound> getOffersBySender(String senderUUID) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            NbtCompound o = offers.getCompound(i);
            if (o.getString("senderUUID").equals(senderUUID) &&
                    o.getString("status").equals("pending")) list.add(o);
        }
        return list;
    }

    public void setOfferStatus(String id, String status) {
        NbtCompound o = getOfferById(id);
        if (o == null) return;
        o.putString("status", status);
        markDirty();
    }

    public void removeOffer(String id) {
        offers.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }

    // -------------------------------------------------------------------------
    // MARKET LISTINGS
    // -------------------------------------------------------------------------

    /**
     * Post a market listing.
     * sellerUUID: player or division UUID
     * itemTag: serialised ItemStack NBT, or "" for currency listings
     * currencyID / amount: what is being sold (if currency listing)
     * priceAmount / priceCurrencyID: what the seller wants in return
     */
    public String createListing(String sellerUUID, String sellerType,
                                String itemTag,
                                String currencyID, int amount,
                                int priceAmount, String priceCurrencyID) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        NbtCompound listing = new NbtCompound();
        listing.putString("id",              id);
        listing.putString("sellerUUID",      sellerUUID);
        listing.putString("sellerType",      sellerType);
        listing.putString("itemTag",         itemTag);
        listing.putString("currencyID",      currencyID);
        listing.putInt   ("amount",          amount);
        listing.putInt   ("priceAmount",     priceAmount);
        listing.putString("priceCurrencyID", priceCurrencyID);
        listing.putString("status",          "active");
        listing.putLong  ("createdAt",       System.currentTimeMillis());
        listings.add(listing);
        markDirty();
        return id;
    }

    public NbtCompound getListingById(String id) {
        for (int i = 0; i < listings.size(); i++) {
            NbtCompound l = listings.getCompound(i);
            if (l.getString("id").equals(id)) return l;
        }
        return null;
    }

    public List<NbtCompound> getActiveListings() {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < listings.size(); i++) {
            NbtCompound l = listings.getCompound(i);
            if (l.getString("status").equals("active")) list.add(l);
        }
        return list;
    }

    public List<NbtCompound> getListingsBySeller(String sellerUUID) {
        List<NbtCompound> list = new ArrayList<>();
        for (int i = 0; i < listings.size(); i++) {
            NbtCompound l = listings.getCompound(i);
            if (l.getString("sellerUUID").equals(sellerUUID) &&
                    l.getString("status").equals("active")) list.add(l);
        }
        return list;
    }

    public void setListingStatus(String id, String status) {
        NbtCompound l = getListingById(id);
        if (l == null) return;
        l.putString("status", status);
        markDirty();
    }

    public void removeListing(String id) {
        listings.removeIf(tag -> ((NbtCompound) tag).getString("id").equals(id));
        markDirty();
    }
}