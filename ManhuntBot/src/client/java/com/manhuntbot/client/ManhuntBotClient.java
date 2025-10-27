package com.manhuntbot.client;

import com.manhuntbot.client.render.HunterEntityRenderer;
import com.manhuntbot.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ManhuntBotClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HunterNetworkingClient.registerReceivers();
        HunterPathClient.init();
        EntityRendererRegistry.register(ModEntities.HUNTER, HunterEntityRenderer::new);
    }
}
