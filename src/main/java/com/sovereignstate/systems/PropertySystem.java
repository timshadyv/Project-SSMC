package com.sovereignstate.systems;

import com.sovereignstate.data.DivisionData;
import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.data.TradeData;
import com.sovereignstate.registry.ModItems;
import com.sovereignstate.util.ChunkHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;

public class PropertySystem {

    // -------------------------------------------------------------------------
    // LIST CHUNK FOR SALE (public)
    // -------------------------------------------------------------------------

    public static void listChunkForSale(ServerPlayerEntity seller, ServerWorld world,
                                        int price, String currencyID) {
        int chunkX = ChunkHelper.getChunkX(seller);
        int chunkZ = ChunkHelper.getChunkZ(seller);

        // Must own the chunk
        String ownerName = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        if (!ownerName.equals(seller.getName().getString())) {
            seller.sendMessage(Text.literal("§cYou don't own this chunk."));
            return;
        }

        // Cannot sell the capital chunk
        if (ChunkHelper.isChunkCapital(world, chunkX, chunkZ)) {
            seller.sendMessage(Text.literal("§cYou cannot sell your capital chunk."));
            return;
        }

        TradeData tradeData = TradeData.get(world);

        // Check not already listed
        String chunkKey = chunkX + "_" + chunkZ;
        List<NbtCompound> active = tradeData.getActiveListings();
        for (NbtCompound listing : active) {
            if (listing.getString("itemTag").equals("chunk:" + chunkKey)) {
                seller.sendMessage(Text.literal("§cThis chunk is already listed for sale."));
                return;
            }
        }

        String sellerUUID = seller.getUuid().toString();
        String listingID = tradeData.createListing(
                sellerUUID, "player",
                "chunk:" + chunkKey,   // itemTag stores chunk reference
                "", 0,
                price, currencyID);

        seller.sendMessage(Text.literal(
                "§aChunk §7[" + chunkX + ", " + chunkZ + "]§a listed for §f" +
                        price + " " + currencyID + " §7[ID: " + listingID + "]"));
    }

    // -------------------------------------------------------------------------
    // OFFER CHUNK TO SPECIFIC PLAYER (private)
    // -------------------------------------------------------------------------

    public static void offerChunkToPlayer(ServerPlayerEntity seller, ServerWorld world,
                                          ServerPlayerEntity buyer,
                                          int price, String currencyID) {
        int chunkX = ChunkHelper.getChunkX(seller);
        int chunkZ = ChunkHelper.getChunkZ(seller);

        String ownerName = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        if (!ownerName.equals(seller.getName().getString())) {
            seller.sendMessage(Text.literal("§cYou don't own this chunk."));
            return;
        }

        if (ChunkHelper.isChunkCapital(world, chunkX, chunkZ)) {
            seller.sendMessage(Text.literal("§cYou cannot sell your capital chunk."));
            return;
        }

        String chunkKey = chunkX + "_" + chunkZ;
        TradeData tradeData = TradeData.get(world);
        String offerID = tradeData.createOffer(
                seller.getUuid().toString(), "player",
                buyer.getUuid().toString(), "player",
                currencyID, price,
                "chunk:" + chunkKey);   // itemTag stores chunk reference

        seller.sendMessage(Text.literal(
                "§aChunk offer sent to §e" + buyer.getName().getString() +
                        "§a for §f" + price + " " + currencyID +
                        " §7[ID: " + offerID + "]"));
        buyer.sendMessage(Text.literal(
                "§6Land offer from §e" + seller.getName().getString() +
                        "§6: chunk §7[" + chunkX + ", " + chunkZ + "]§6 for §f" +
                        price + " " + currencyID + " §7[ID: " + offerID + "]"));
        buyer.sendMessage(Text.literal(
                "§eUse §f/ss property accept " + offerID +
                        " §eor §f/ss property reject " + offerID));
    }

    // -------------------------------------------------------------------------
    // BUY A PUBLIC LISTING
    // -------------------------------------------------------------------------

    public static void buyChunk(ServerPlayerEntity buyer, ServerWorld world,
                                String listingID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound listing = tradeData.getListingById(listingID);

        if (listing == null || !listing.getString("status").equals("active")) {
            buyer.sendMessage(Text.literal("§cListing not found or no longer available."));
            return;
        }

        String itemTag = listing.getString("itemTag");
        if (!itemTag.startsWith("chunk:")) {
            buyer.sendMessage(Text.literal("§cThis is not a property listing."));
            return;
        }

        String buyerUUID  = buyer.getUuid().toString();
        String sellerUUID = listing.getString("sellerUUID");

        if (sellerUUID.equals(buyerUUID)) {
            buyer.sendMessage(Text.literal("§cYou cannot buy your own listing."));
            return;
        }

        String priceCurrencyID = listing.getString("priceCurrencyID");
        int    priceAmount     = listing.getInt("priceAmount");

        // Parse chunk coords
        String chunkKey = itemTag.substring(6); // strip "chunk:"
        String[] parts  = chunkKey.split("_");
        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);

        // Check law: protectionism — only division members can buy land
        DivisionData divData    = DivisionData.get(world);
        PlayerStateData ps      = PlayerStateData.get(world);
        String chunkDivID       = ChunkHelper.getChunkDivisionID(world, chunkX, chunkZ);
        if (chunkDivID != null && !chunkDivID.isEmpty()) {
            if (divData.hasLaw(chunkDivID, "protectionism")) {
                String buyerDivID = ps.getDivisionID(buyerUUID);
                if (!chunkDivID.equals(buyerDivID)) {
                    buyer.sendMessage(Text.literal(
                            "§cThis division has §eprotectionism§c laws. " +
                                    "Only members can purchase land here."));
                    return;
                }
            }
        }

        // Check buyer balance
        int balance = ps.getWallet(buyerUUID, priceCurrencyID);
        if (balance < priceAmount) {
            buyer.sendMessage(Text.literal(
                    "§cYou need §f" + priceAmount + " " + priceCurrencyID +
                            "§c. You have: §f" + balance));
            return;
        }

        // Transfer payment
        ps.adjustWallet(buyerUUID,  priceCurrencyID, -priceAmount);
        ps.adjustWallet(sellerUUID, priceCurrencyID,  priceAmount);

        // Transfer chunk ownership
        transferChunkOwnership(world, chunkX, chunkZ, buyer);

        // Issue deed item
        issueDeed(buyer, chunkX, chunkZ);

        tradeData.setListingStatus(listingID, "sold");

        buyer.sendMessage(Text.literal(
                "§aPurchased chunk §7[" + chunkX + ", " + chunkZ + "]§a for §f" +
                        priceAmount + " " + priceCurrencyID + "§a. Deed issued!"));

        notifyPlayer(buyer, sellerUUID,
                "§aYour land listing §7[" + listingID + "]§a was sold to §e" +
                        buyer.getName().getString() + "§a!");
    }

    // -------------------------------------------------------------------------
    // ACCEPT PRIVATE LAND OFFER
    // -------------------------------------------------------------------------

    public static void acceptLandOffer(ServerPlayerEntity buyer, ServerWorld world,
                                       String offerID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound offer   = tradeData.getOfferById(offerID);

        if (offer == null || !offer.getString("status").equals("pending")) {
            buyer.sendMessage(Text.literal("§cOffer not found or no longer active."));
            return;
        }
        if (!offer.getString("receiverUUID").equals(buyer.getUuid().toString())) {
            buyer.sendMessage(Text.literal("§cThis offer was not sent to you."));
            return;
        }

        String itemTag = offer.getString("itemTag");
        if (!itemTag.startsWith("chunk:")) {
            buyer.sendMessage(Text.literal("§cThis is not a land offer."));
            return;
        }

        String sellerUUID      = offer.getString("senderUUID");
        String priceCurrencyID = offer.getString("currencyID");
        int    priceAmount     = offer.getInt("amount");
        String buyerUUID       = buyer.getUuid().toString();

        String chunkKey = itemTag.substring(6);
        String[] parts  = chunkKey.split("_");
        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);

        // Check law: protectionism
        DivisionData divData = DivisionData.get(world);
        PlayerStateData ps   = PlayerStateData.get(world);
        String chunkDivID    = ChunkHelper.getChunkDivisionID(world, chunkX, chunkZ);
        if (chunkDivID != null && !chunkDivID.isEmpty()) {
            if (divData.hasLaw(chunkDivID, "protectionism")) {
                String buyerDivID = ps.getDivisionID(buyerUUID);
                if (!chunkDivID.equals(buyerDivID)) {
                    buyer.sendMessage(Text.literal(
                            "§cThis division has §eprotectionism§c laws. " +
                                    "Only members can purchase land here."));
                    return;
                }
            }
        }

        // Check buyer balance
        int balance = ps.getWallet(buyerUUID, priceCurrencyID);
        if (balance < priceAmount) {
            buyer.sendMessage(Text.literal(
                    "§cYou need §f" + priceAmount + " " + priceCurrencyID +
                            "§c. You have: §f" + balance));
            return;
        }

        // Transfer payment
        ps.adjustWallet(buyerUUID,  priceCurrencyID, -priceAmount);
        ps.adjustWallet(sellerUUID, priceCurrencyID,  priceAmount);

        // Transfer chunk ownership
        transferChunkOwnership(world, chunkX, chunkZ, buyer);

        // Issue deed
        issueDeed(buyer, chunkX, chunkZ);

        tradeData.setOfferStatus(offerID, "accepted");

        buyer.sendMessage(Text.literal(
                "§aLand purchase complete! Chunk §7[" + chunkX + ", " + chunkZ +
                        "]§a is now yours. Deed issued!"));

        notifyPlayer(buyer, sellerUUID,
                "§aYour land offer §7[" + offerID + "]§a was accepted by §e" +
                        buyer.getName().getString() + "§a!");
    }

    // -------------------------------------------------------------------------
    // REJECT PRIVATE LAND OFFER
    // -------------------------------------------------------------------------

    public static void rejectLandOffer(ServerPlayerEntity buyer, ServerWorld world,
                                       String offerID) {
        TradeData tradeData = TradeData.get(world);
        NbtCompound offer   = tradeData.getOfferById(offerID);

        if (offer == null) {
            buyer.sendMessage(Text.literal("§cOffer not found."));
            return;
        }
        if (!offer.getString("receiverUUID").equals(buyer.getUuid().toString())) {
            buyer.sendMessage(Text.literal("§cThis offer was not sent to you."));
            return;
        }

        tradeData.setOfferStatus(offerID, "rejected");
        buyer.sendMessage(Text.literal("§cLand offer rejected: §7" + offerID));
        notifyPlayer(buyer, offer.getString("senderUUID"),
                "§cYour land offer §7[" + offerID + "]§c was rejected.");
    }

    // -------------------------------------------------------------------------
    // BROWSE PROPERTY LISTINGS
    // -------------------------------------------------------------------------

    public static void browseListings(ServerPlayerEntity player, ServerWorld world) {
        TradeData tradeData = TradeData.get(world);
        List<NbtCompound> listings = tradeData.getActiveListings();

        boolean found = false;
        player.sendMessage(Text.literal("§6--- Property Listings ---"));
        for (NbtCompound listing : listings) {
            if (!listing.getString("itemTag").startsWith("chunk:")) continue;
            found = true;
            String id              = listing.getString("id");
            String chunkKey        = listing.getString("itemTag").substring(6);
            int    priceAmount     = listing.getInt("priceAmount");
            String priceCurrencyID = listing.getString("priceCurrencyID");
            String sellerUUID      = listing.getString("sellerUUID");
            String sellerName      = getPlayerName(player, sellerUUID);
            String[] parts         = chunkKey.split("_");

            player.sendMessage(Text.literal(
                    "§e[" + id + "] §fChunk §7[" + parts[0] + ", " + parts[1] + "]" +
                            " §f| Price: §e" + priceAmount + " " + priceCurrencyID +
                            " §f| Seller: §e" + sellerName));
        }

        if (!found) {
            player.sendMessage(Text.literal("§eNo property listings available."));
            return;
        }
        player.sendMessage(Text.literal("§7Use §f/ss property buy <id> §7to purchase."));
    }

    // -------------------------------------------------------------------------
    // CHUNK INFO
    // -------------------------------------------------------------------------

    public static void showChunkInfo(ServerPlayerEntity player, ServerWorld world) {
        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        String owner    = ChunkHelper.getChunkOwner(world, chunkX, chunkZ);
        String divID    = ChunkHelper.getChunkDivisionID(world, chunkX, chunkZ);
        boolean capital = ChunkHelper.isChunkCapital(world, chunkX, chunkZ);

        player.sendMessage(Text.literal("§6--- Chunk Info ---"));
        player.sendMessage(Text.literal("§eCoords: §f[" + chunkX + ", " + chunkZ + "]"));
        player.sendMessage(Text.literal("§eOwner: §f" + (owner.isEmpty() ? "Unclaimed" : owner)));
        player.sendMessage(Text.literal("§eDivision: §f" + (divID.isEmpty() ? "None" : divID)));
        player.sendMessage(Text.literal("§eCapital: §f" + (capital ? "Yes" : "No")));
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static void transferChunkOwnership(ServerWorld world, int chunkX, int chunkZ,
                                               ServerPlayerEntity newOwner) {
        ChunkHelper.setChunkOwner(world, chunkX, chunkZ, newOwner.getName().getString());
        // Division ID on chunk updates to buyer's division
        PlayerStateData ps    = PlayerStateData.get(world);
        String newOwnerDivID  = ps.getDivisionID(newOwner.getUuid().toString());
        ChunkHelper.setChunkDivisionID(world, chunkX, chunkZ,
                newOwnerDivID != null ? newOwnerDivID : "");
    }

    private static void issueDeed(ServerPlayerEntity player, int chunkX, int chunkZ) {
        ItemStack deed = new ItemStack(ModItems.TRADE_CONTRACT);
        NbtCompound tag = deed.getOrCreateNbt();
        tag.putInt("chunkX", chunkX);
        tag.putInt("chunkZ", chunkZ);
        tag.putString("owner", player.getName().getString());
        deed.setCustomName(Text.literal("§6Deed: Chunk [" + chunkX + ", " + chunkZ + "]"));
        player.getInventory().offerOrDrop(deed);
    }

    private static void notifyPlayer(ServerPlayerEntity context,
                                     String targetUUID, String message) {
        if (context.getServer() == null) return;
        context.getServer().getPlayerManager().getPlayerList().forEach(p -> {
            if (p.getUuid().toString().equals(targetUUID)) {
                p.sendMessage(Text.literal(message));
            }
        });
    }

    private static String getPlayerName(ServerPlayerEntity context, String uuid) {
        if (context.getServer() == null) return uuid.substring(0, 8);
        return context.getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getUuid().toString().equals(uuid))
                .map(p -> p.getName().getString())
                .findFirst()
                .orElse(uuid.substring(0, 8));
    }
}