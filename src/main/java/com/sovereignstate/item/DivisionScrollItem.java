package com.sovereignstate.item;

import com.sovereignstate.network.ServerPacketSender;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class DivisionScrollItem extends Item {

    public DivisionScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.success(stack);

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ServerWorld serverWorld = (ServerWorld) world;

        ServerPacketSender.sendOpenDivisionScreen(player, serverWorld);

        return TypedActionResult.success(stack);
    }
}