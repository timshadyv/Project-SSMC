package com.sovereignstate.entity;

import com.sovereignstate.data.PlayerStateData;
import com.sovereignstate.util.ChunkHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import com.sovereignstate.registry.ModItems;

public class GuardEntity extends PathAwareEntity {

    private String divisionID = "";

    public GuardEntity(EntityType<? extends GuardEntity> type, World world) {
        super(type, world);
        // Hold a handcuff item visually
        this.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,
                new ItemStack(ModItems.HANDCUFF));
    }

    // ─── Attributes ───────────────────────────────────────────────────────────

    public static DefaultAttributeContainer.Builder createGuardAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0);
    }

    // ─── Goals ────────────────────────────────────────────────────────────────

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ArrestGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    // ─── Division ID ──────────────────────────────────────────────────────────

    public String getDivisionID() { return divisionID; }
    public void setDivisionID(String id) { this.divisionID = id; }

    // ─── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("divisionID", divisionID);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        divisionID = nbt.getString("divisionID");
    }

    // ─── Arrest Goal ──────────────────────────────────────────────────────────

    private static class ArrestGoal extends Goal {

        private final GuardEntity guard;
        private ServerPlayerEntity target;
        private int arrestCooldown = 0;

        public ArrestGoal(GuardEntity guard) {
            this.guard = guard;
        }

        @Override
        public boolean canStart() {
            if (guard.getWorld().isClient) return false;
            if (guard.divisionID.isEmpty()) return false;

            ServerWorld world = (ServerWorld) guard.getWorld();
            PlayerStateData playerData = PlayerStateData.get(world);

            // Look for a nearby wanted player in the same division
            for (PlayerEntity nearby : world.getPlayers()) {
                String uuid = nearby.getUuid().toString();
                if (!playerData.isWanted(uuid)) continue;

                String theirDiv = playerData.getDivisionID(uuid);
                if (!guard.divisionID.equals(theirDiv)) continue;

                double dist = guard.squaredDistanceTo(nearby);
                if (dist > 400) continue; // 20 blocks

                target = (ServerPlayerEntity) nearby;
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            if (target == null || !target.isAlive()) return false;
            if (guard.getWorld().isClient) return false;
            ServerWorld world = (ServerWorld) guard.getWorld();
            if (!PlayerStateData.get(world).isWanted(target.getUuid().toString())) return false;
            if (guard.squaredDistanceTo(target) > 625) return false; // lost at 25 blocks
            return true;
        }

        @Override
        public void stop() {
            target = null;
            arrestCooldown = 0;
            guard.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) return;
            guard.getLookControl().lookAt(target, 30.0f, 30.0f);

            double dist = guard.squaredDistanceTo(target);

            if (dist > 9) {
                // Chase the target
                guard.getNavigation().startMovingTo(target, 1.2);
            } else {
                // Close enough — arrest
                guard.getNavigation().stop();
                arrestCooldown--;
                if (arrestCooldown <= 0) {
                    arrestCooldown = 40; // try every 2 seconds
                    performArrest((ServerWorld) guard.getWorld());
                }
            }
        }

        private void performArrest(ServerWorld world) {
            if (target == null) return;
            String uuid = target.getUuid().toString();
            PlayerStateData playerData = PlayerStateData.get(world);

            if (!playerData.isWanted(uuid)) return;

            // Clear wanted, set incarcerated
            playerData.setWanted(uuid, false);
            playerData.setIncarcerated(uuid, true);

            // Apply restraint effects (60 seconds each)
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,      1200, 3, false, true));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 1200, 2, false, true));

            // Teleport to jail spawn if one is set
            int jailX = playerData.getJailSpawnX(uuid);
            int jailY = playerData.getJailSpawnY(uuid);
            int jailZ = playerData.getJailSpawnZ(uuid);

            if (jailY > 0) {
                target.teleport(jailX + 0.5, jailY, jailZ + 0.5);
                target.sendMessage(Text.literal("§c⛓ You have been arrested and taken to jail."));
            } else {
                target.sendMessage(Text.literal("§c⛓ You have been arrested. §7(No jail spawn set — ask your division leader to set one.)"));
            }

            // Notify online division members
            for (ServerPlayerEntity online : world.getServer().getPlayerManager().getPlayerList()) {
                String theirDiv = playerData.getDivisionID(online.getUuid().toString());
                if (guard.divisionID.equals(theirDiv)) {
                    online.sendMessage(Text.literal(
                            "§6[Guard] §e" + target.getName().getString() + " §6has been arrested."));
                }
            }

            target = null;
        }
    }
}