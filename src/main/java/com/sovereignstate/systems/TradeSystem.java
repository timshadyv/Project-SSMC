package com.sovereignstate.systems;

import com.sovereignstate.data.CurrencyData;
import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.TradeData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class TradeSystem {

    // -------------------------------------------------------------------------
    // TRADE OFFERS — Currency
    // -------------------------------------------------------------------------

    public static void offerCurrency(ServerPlayerEntity sender, ServerWorld world,
                                     ServerPlayerEntity receiver,
                                     String currencyID, int amount) {
        TradeData tradeData = TradeData.get(world);
        PlayerStateData playerState = PlayerStateData.get(world);

        // Validate currency exists
        CurrencyData currencyData = CurrencyData.get(world);
        if (currencyData.getCurrencyById(currencyID) == null) {
            sender.sendMessage(Text.literal("§cCurrency not found: " + currencyID));
            return;
        }

        // Check sender has enough
        String senderUUID = sender.getUuid().toString();
        int balance = playerState.getWallet(senderUUID, currencyID);
        if (balance < amount) {
            sender.sendMessage(Text.literal(
                    "§cYou don't have enough §e" + currencyID +
                            "§c. Balance: §f" + balance));
            return;
        }

        String receiverUUID = receiver.getUuid().toString();
        String offerID = tradeData.createOffer(
                senderUUID, "player",
                receiverUUID, "player",
                currencyID, amount, "");

        sender.sendMessage(Text.literal(
                "§aTrade offer sent to §e" + receiver.getName().getString() +
                        "§a: §f" + amount + " " + currencyID +
                        " §7[ID: " + offerID + "]"));
        receiver.sendMessage(Text.literal(
                "§6Trade offer from §e" + sender.getName().getString() +
                        "§6: §f" + amount + " " + currencyID +
                        " §7[ID: " + offerID + "]"));
        receiver.sendMessage(Text.literal(
                "§eUse §f/ss trade accept " + offerID +
                        " §eor §f/ss trade reject " + offerID));
    }

    // -------------------------------------------------------------------------
    // TRADE OFFERS — Items
    // -------------------------------------------------------------------------

    public static void offerItem(ServerPlayerEntity sender, ServerWorld world,
                                 ServerPlayerEntity receiver,
                                 int priceAmount, String priceCurrencyID) {
        ItemStack held = sender.getMainHandStack();
        if (held.isEmpty()) {
            sender.sendMessage(Text.literal("§cYou must hold the item you want to trade."));
            return;
        }

        // Validate currency
        CurrencyData currencyData = CurrencyData.get(world);
        if (currencyData.getCurrencyById(priceCurrencyID) == null) {
            sender.sendMessage(Text.literal("§cCurrency not found: " + priceCurrencyID));
            return;
        }

        // Serialise item to NBT string
        NbtCompound itemNbt = new NbtCompound();
        held.writeNbt(itemNbt);
        String itemTag = itemNbt.toString();

        TradeData tradeData = TradeData.get(world);
        String senderUUID = sender.getUuid().toString();
        String receiverUUID = receiver.getUuid().toString();

        String offerID = tradeData.createOffer(
                senderUUID, "player",
                receiverUUID, "player",
                priceCurrencyID, priceAmount, itemTag);

        String itemName = held.getName().getString();

        sender.sendMessage(Text.literal(
                "§aItem offer sent to §e" + receiver.getName().getString() +
                        "§a: §f" + itemName + " §afor §f" + priceAmount + " " + priceCurrencyID +
                        " §7[ID: " + offerID + "]"));
        receiver.sendMessage(Text.literal(
                "§6Item offer from §e" + sender.getName().getString() +
                        "§6: §f" + itemName + " §6for §f" + priceAmount + " " + priceCurrencyID +
                        " §7[ID: " + offerID + "]"));
        receiver.sendMessage(Text.literal(
                "§eUse §f/ss trade accept " + offerID +
                        " §eor §f/ss trade reject " + offerID));
    }

    // -------------------------------------------------------------------------
    // ACCEPT OFFER
    // -------------------------------------------------------------------------

    public static void acceptOffer(ServerPlayerEntity player, ServerWorld world,
                                   String offerID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound offer = tradeData.getOfferById(offerID);

        if (offer == null) {
            player.sendMessage(Text.literal("§cOffer not found: " + offerID));
            return;
        }
        if (!offer.getString("status").equals("pending")) {
            player.sendMessage(Text.literal("§cThis offer is no longer active."));
            return;
        }
        if (!offer.getString("receiverUUID").equals(player.getUuid().toString())) {
            player.sendMessage(Text.literal("§cThis offer was not sent to you."));
            return;
        }

        String senderUUID   = offer.getString("senderUUID");
        String currencyID   = offer.getString("currencyID");
        int    amount       = offer.getInt("amount");
        String itemTag      = offer.getString("itemTag");

        PlayerStateData playerState = PlayerStateData.get(world);

        // Item trade
        if (!itemTag.isEmpty()) {
            // Check receiver has enough currency
            int receiverBalance = playerState.getWallet(player.getUuid().toString(), currencyID);
            if (receiverBalance < amount) {
                player.sendMessage(Text.literal(
                        "§cYou don't have enough §e" + currencyID +
                                "§c to accept this. Need: §f" + amount +
                                " §cHave: §f" + receiverBalance));
                return;
            }

            // Transfer currency receiver → sender
            playerState.adjustWallet(player.getUuid().toString(), currencyID, -amount);
            playerState.adjustWallet(senderUUID, currencyID, amount);

            // Give item to receiver
            try {
                NbtCompound itemNbt = NbtHelper.fromNbtProviderString(itemTag);
                ItemStack stack = ItemStack.fromNbt(itemNbt);
                if (!stack.isEmpty()) {
                    player.getInventory().offerOrDrop(stack);
                }
            } catch (Exception e) {
                player.sendMessage(Text.literal("§cFailed to restore item from offer."));
                return;
            }

            player.sendMessage(Text.literal(
                    "§aTrade accepted! You paid §f" + amount + " " + currencyID +
                            "§a and received the item."));

            // Notify sender if online
            notifySender(player, senderUUID, "§aYour item trade offer §7[" + offerID + "]§a was accepted!");

        } else {
            // Currency trade — check sender still has enough
            int senderBalance = playerState.getWallet(senderUUID, currencyID);
            if (senderBalance < amount) {
                player.sendMessage(Text.literal(
                        "§cThe sender no longer has enough §e" + currencyID +
                                "§c to complete this trade."));
                tradeData.setOfferStatus(offerID, "cancelled");
                return;
            }

            // Transfer currency sender → receiver
            playerState.adjustWallet(senderUUID, currencyID, -amount);
            playerState.adjustWallet(player.getUuid().toString(), currencyID, amount);

            player.sendMessage(Text.literal(
                    "§aTrade accepted! You received §f" + amount + " " + currencyID + "§a."));

            notifySender(player, senderUUID, "§aYour currency trade offer §7[" + offerID + "]§a was accepted!");
        }

        tradeData.setOfferStatus(offerID, "accepted");
    }

    // -------------------------------------------------------------------------
    // REJECT OFFER
    // -------------------------------------------------------------------------

    public static void rejectOffer(ServerPlayerEntity player, ServerWorld world,
                                   String offerID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound offer = tradeData.getOfferById(offerID);

        if (offer == null) {
            player.sendMessage(Text.literal("§cOffer not found: " + offerID));
            return;
        }
        if (!offer.getString("receiverUUID").equals(player.getUuid().toString())) {
            player.sendMessage(Text.literal("§cThis offer was not sent to you."));
            return;
        }

        tradeData.setOfferStatus(offerID, "rejected");
        player.sendMessage(Text.literal("§cOffer rejected: §7" + offerID));
        notifySender(player, offer.getString("senderUUID"),
                "§cYour trade offer §7[" + offerID + "]§c was rejected.");
    }

    // -------------------------------------------------------------------------
    // INBOX
    // -------------------------------------------------------------------------

    public static void showInbox(ServerPlayerEntity player, ServerWorld world) {
        TradeData tradeData = TradeData.get(world);
        List<NbtCompound> incoming = tradeData.getOffersForReceiver(
                player.getUuid().toString());

        if (incoming.isEmpty()) {
            player.sendMessage(Text.literal("§eNo pending trade offers."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Trade Inbox ---"));
        for (NbtCompound offer : incoming) {
            String id         = offer.getString("id");
            String senderUUID = offer.getString("senderUUID");
            String currencyID = offer.getString("currencyID");
            int    amount     = offer.getInt("amount");
            String itemTag    = offer.getString("itemTag");

            String senderName = getSenderName(player, senderUUID);

            if (!itemTag.isEmpty()) {
                player.sendMessage(Text.literal(
                        "§e[" + id + "] §fItem offer from §e" + senderName +
                                " §ffor §e" + amount + " " + currencyID));
            } else {
                player.sendMessage(Text.literal(
                        "§e[" + id + "] §fCurrency from §e" + senderName +
                                "§f: §e" + amount + " " + currencyID));
            }
        }
        player.sendMessage(Text.literal(
                "§7Use §f/ss trade accept <id> §7or §f/ss trade reject <id>"));
    }

    // -------------------------------------------------------------------------
    // MARKET — Post currency listing
    // -------------------------------------------------------------------------

    public static void postCurrencyListing(ServerPlayerEntity seller, ServerWorld world,
                                           String currencyID, int amount,
                                           int priceAmount, String priceCurrencyID) {
        PlayerStateData playerState = PlayerStateData.get(world);
        CurrencyData currencyData = CurrencyData.get(world);

        if (currencyData.getCurrencyById(currencyID) == null) {
            seller.sendMessage(Text.literal("§cCurrency not found: " + currencyID));
            return;
        }
        if (currencyData.getCurrencyById(priceCurrencyID) == null) {
            seller.sendMessage(Text.literal("§cPrice currency not found: " + priceCurrencyID));
            return;
        }

        String sellerUUID = seller.getUuid().toString();
        int balance = playerState.getWallet(sellerUUID, currencyID);
        if (balance < amount) {
            seller.sendMessage(Text.literal(
                    "§cYou don't have enough §e" + currencyID +
                            "§c. Balance: §f" + balance));
            return;
        }

        // Escrow: hold the currency until sold or cancelled
        playerState.adjustWallet(sellerUUID, currencyID, -amount);

        TradeData tradeData = TradeData.get(world);
        String listingID = tradeData.createListing(
                sellerUUID, "player", "",
                currencyID, amount,
                priceAmount, priceCurrencyID);

        seller.sendMessage(Text.literal(
                "§aListing posted: §f" + amount + " " + currencyID +
                        " §afor §f" + priceAmount + " " + priceCurrencyID +
                        " §7[ID: " + listingID + "]"));
    }

    // -------------------------------------------------------------------------
    // MARKET — Post item listing
    // -------------------------------------------------------------------------

    public static void postItemListing(ServerPlayerEntity seller, ServerWorld world,
                                       int priceAmount, String priceCurrencyID) {
        ItemStack held = seller.getMainHandStack();
        if (held.isEmpty()) {
            seller.sendMessage(Text.literal("§cHold the item you want to list."));
            return;
        }

        CurrencyData currencyData = CurrencyData.get(world);
        if (currencyData.getCurrencyById(priceCurrencyID) == null) {
            seller.sendMessage(Text.literal("§cCurrency not found: " + priceCurrencyID));
            return;
        }

        NbtCompound itemNbt = new NbtCompound();
        held.writeNbt(itemNbt);
        String itemTag  = itemNbt.toString();
        String itemName = held.getName().getString();

        // Take item from seller's hand
        seller.getMainHandStack().decrement(held.getCount());

        TradeData tradeData = TradeData.get(world);
        String sellerUUID = seller.getUuid().toString();
        String listingID = tradeData.createListing(
                sellerUUID, "player", itemTag,
                "", 0,
                priceAmount, priceCurrencyID);

        seller.sendMessage(Text.literal(
                "§aItem listed: §f" + itemName +
                        " §afor §f" + priceAmount + " " + priceCurrencyID +
                        " §7[ID: " + listingID + "]"));
    }

    // -------------------------------------------------------------------------
    // MARKET — Browse
    // -------------------------------------------------------------------------

    public static void browseMarket(ServerPlayerEntity player, ServerWorld world) {
        TradeData tradeData = TradeData.get(world);
        List<NbtCompound> listings = tradeData.getActiveListings();

        if (listings.isEmpty()) {
            player.sendMessage(Text.literal("§eThe market is empty."));
            return;
        }

        player.sendMessage(Text.literal("§6--- Market Listings ---"));
        for (NbtCompound listing : listings) {
            String id              = listing.getString("id");
            String itemTag         = listing.getString("itemTag");
            String currencyID      = listing.getString("currencyID");
            int    amount          = listing.getInt("amount");
            int    priceAmount     = listing.getInt("priceAmount");
            String priceCurrencyID = listing.getString("priceCurrencyID");
            String sellerUUID      = listing.getString("sellerUUID");
            String sellerName      = getSenderName(player, sellerUUID);

            if (!itemTag.isEmpty()) {
                // Item listing — try to get name
                String itemName = "Item";
                try {
                    NbtCompound itemNbt = NbtHelper.fromNbtProviderString(itemTag);
                    ItemStack stack = ItemStack.fromNbt(itemNbt);
                    itemName = stack.getName().getString();
                } catch (Exception ignored) {}

                player.sendMessage(Text.literal(
                        "§e[" + id + "] §f" + itemName +
                                " §7| Price: §f" + priceAmount + " " + priceCurrencyID +
                                " §7| Seller: §f" + sellerName));
            } else {
                player.sendMessage(Text.literal(
                        "§e[" + id + "] §f" + amount + " " + currencyID +
                                " §7| Price: §f" + priceAmount + " " + priceCurrencyID +
                                " §7| Seller: §f" + sellerName));
            }
        }
        player.sendMessage(Text.literal("§7Use §f/ss market buy <id> §7to purchase."));
    }

    // -------------------------------------------------------------------------
    // MARKET — Buy
    // -------------------------------------------------------------------------

    public static void buyListing(ServerPlayerEntity buyer, ServerWorld world,
                                  String listingID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound listing = tradeData.getListingById(listingID);

        if (listing == null) {
            buyer.sendMessage(Text.literal("§cListing not found: " + listingID));
            return;
        }
        if (!listing.getString("status").equals("active")) {
            buyer.sendMessage(Text.literal("§cThis listing is no longer available."));
            return;
        }

        String buyerUUID       = buyer.getUuid().toString();
        String sellerUUID      = listing.getString("sellerUUID");

        if (sellerUUID.equals(buyerUUID)) {
            buyer.sendMessage(Text.literal("§cYou cannot buy your own listing."));
            return;
        }

        String priceCurrencyID = listing.getString("priceCurrencyID");
        int    priceAmount     = listing.getInt("priceAmount");
        String currencyID      = listing.getString("currencyID");
        int    amount          = listing.getInt("amount");
        String itemTag         = listing.getString("itemTag");

        PlayerStateData playerState = PlayerStateData.get(world);

        // Check buyer has enough
        int buyerBalance = playerState.getWallet(buyerUUID, priceCurrencyID);
        if (buyerBalance < priceAmount) {
            buyer.sendMessage(Text.literal(
                    "§cYou need §f" + priceAmount + " " + priceCurrencyID +
                            "§c. You have: §f" + buyerBalance));
            return;
        }

        // Apply division sales tax if buyer is in a division with sales_tax law
        DivisionData divData = DivisionData.get(world);
        PlayerStateData ps = PlayerStateData.get(world);
        String buyerDivID = ps.getDivisionID(buyerUUID);
        int taxAmount = 0;
        if (buyerDivID != null && !buyerDivID.isEmpty()) {
            if (divData.hasLaw(buyerDivID, "sales_tax")) {
                int taxRate = divData.getTaxRate(buyerDivID);
                taxAmount = (int) Math.ceil(priceAmount * (taxRate / 100.0));
                if (buyerBalance < priceAmount + taxAmount) {
                    buyer.sendMessage(Text.literal(
                            "§cYou need §f" + (priceAmount + taxAmount) + " " + priceCurrencyID +
                                    "§c (includes §f" + taxAmount + "§c sales tax)."));
                    return;
                }
                // Tax goes to division treasury
                divData.adjustTreasury(buyerDivID, priceCurrencyID, taxAmount);
                playerState.adjustWallet(buyerUUID, priceCurrencyID, -taxAmount);
            }
        }

        // Transfer payment buyer → seller
        playerState.adjustWallet(buyerUUID,   priceCurrencyID, -priceAmount);
        playerState.adjustWallet(sellerUUID,  priceCurrencyID,  priceAmount);

        // Give goods to buyer
        if (!itemTag.isEmpty()) {
            try {
                NbtCompound itemNbt = NbtHelper.fromNbtProviderString(itemTag);
                ItemStack stack = ItemStack.fromNbt(itemNbt);
                if (!stack.isEmpty()) buyer.getInventory().offerOrDrop(stack);
            } catch (Exception e) {
                buyer.sendMessage(Text.literal("§cFailed to restore item."));
                return;
            }
            buyer.sendMessage(Text.literal(
                    "§aPurchased item for §f" + priceAmount + " " + priceCurrencyID +
                            (taxAmount > 0 ? " §7(+" + taxAmount + " tax)" : "")));
        } else {
            // Currency listing — give currency to buyer
            playerState.adjustWallet(buyerUUID, currencyID, amount);
            buyer.sendMessage(Text.literal(
                    "§aPurchased §f" + amount + " " + currencyID +
                            " §afor §f" + priceAmount + " " + priceCurrencyID +
                            (taxAmount > 0 ? " §7(+" + taxAmount + " tax)" : "")));
        }

        tradeData.setListingStatus(listingID, "sold");
        notifySender(buyer, sellerUUID,
                "§aYour market listing §7[" + listingID + "]§a was purchased!");
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static void notifySender(ServerPlayerEntity context,
                                     String targetUUID, String message) {
        if (context.getServer() == null) return;
        context.getServer().getPlayerManager().getPlayerList().forEach(p -> {
            if (p.getUuid().toString().equals(targetUUID)) {
                p.sendMessage(Text.literal(message));
            }
        });
    }

    private static String getSenderName(ServerPlayerEntity context, String uuid) {
        if (context.getServer() == null) return uuid.substring(0, 8);
        return context.getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getUuid().toString().equals(uuid))
                .map(p -> p.getName().getString())
                .findFirst()
                .orElse(uuid.substring(0, 8));
    }
}