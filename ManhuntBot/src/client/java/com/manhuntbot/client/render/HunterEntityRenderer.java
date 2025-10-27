package com.manhuntbot.client.render;

import com.manhuntbot.entity.HunterEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class HunterEntityRenderer extends BipedEntityRenderer<HunterEntity, PlayerEntityModel<HunterEntity>> {

    public HunterEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public Identifier getTexture(HunterEntity entity) {
        return new Identifier("minecraft", "textures/entity/steve.png");
    }
}
