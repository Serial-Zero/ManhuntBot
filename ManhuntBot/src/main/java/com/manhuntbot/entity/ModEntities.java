package com.manhuntbot.entity;

import com.manhuntbot.ManhuntBotMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

    public static final EntityType<HunterEntity> HUNTER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ManhuntBotMod.MOD_ID, "hunter"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, HunterEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                    .trackRangeBlocks(96)
                    .forceTrackedVelocityUpdates(true)
                    .build()
    );

    private ModEntities() {}

    public static void init() {
        FabricDefaultAttributeRegistry.register(HUNTER, HunterEntity.createAttributes());
    }
}
