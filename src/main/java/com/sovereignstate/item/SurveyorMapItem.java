package com.sovereignstate.item;

import com.sovereignstate.systems.ChunkClaimingSystem;
import com.sovereignstate.util.ChunkHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SurveyorMapItem extends Item {

    public SurveyorMapItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.success(stack);

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ServerWorld serverWorld = (ServerWorld) world;

        int chunkX = ChunkHelper.getChunkX(player);
        int chunkZ = ChunkHelper.getChunkZ(player);

        if (user.isSneaking()) {
            // Shift+right click: show chunk info
            String owner = ChunkHelper.getChunkOwner(serverWorld, chunkX, chunkZ);
            String divisionID = ChunkHelper.getChunkDivisionID(serverWorld, chunkX, chunkZ);

            player.sendMessage(Text.literal("§6--- Chunk Info ---"));
            player.sendMessage(Text.literal("§ePosition: §f(" + chunkX + ", " + chunkZ + ")"));

            if (owner == null || owner.isEmpty()) {
                player.sendMessage(Text.literal("§eOwner: §fUnclaimed"));
            } else {
                player.sendMessage(Text.literal("§eOwner: §f" + owner));
                player.sendMessage(Text.literal("§eDivision: §f" +
                        (divisionID.isEmpty() ? "None" : divisionID)));
                player.sendMessage(Text.literal("§eJail Zone: §f" +
                        ChunkHelper.isChunkInJailZone(serverWorld, chunkX, chunkZ)));
                player.sendMessage(Text.literal("§eCapital: §f" +
                        ChunkHelper.isChunkCapital(serverWorld, chunkX, chunkZ)));
            }
        } else {
            // Right click: claim chunk
            ChunkClaimingSystem.claimChunk(player, serverWorld);
        }

        return TypedActionResult.success(stack);
    }
}