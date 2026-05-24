package com.sovereignstate.registry;

import com.sovereignstate.entity.GuardEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<GuardEntity> GUARD = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("sovereignstate", "guard"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build()
    );

    public static void register() {
        // Calling this method triggers the static initialiser above
    }
}