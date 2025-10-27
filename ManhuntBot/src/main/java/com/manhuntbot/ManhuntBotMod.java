package com.manhuntbot;

import com.manhuntbot.command.HunterCommand;
import com.manhuntbot.entity.HunterEntity;
import com.manhuntbot.entity.ModEntities;
import com.manhuntbot.network.HunterNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManhuntBotMod implements ModInitializer {

    public static final String MOD_ID = "manhuntbot";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile HunterDifficulty currentDifficulty = HunterDifficulty.MEDIUM;

    @Override
    public void onInitialize() {
        ModEntities.init();
        HunterNetworking.registerServerReceivers();
        HunterCommand.register();

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.isClient()) {
                HunterManager.tickWorld(world);
            }
        });

        LOGGER.info("Manhunt Bot initialized. Current difficulty: {}", currentDifficulty.displayName());
    }

    public static HunterDifficulty getDifficulty() {
        return currentDifficulty;
    }

    public static void setDifficulty(HunterDifficulty difficulty, MinecraftServer server) {
        if (difficulty == currentDifficulty) {
            return;
        }
        currentDifficulty = difficulty;
        LOGGER.info("Hunter difficulty changed to {}", difficulty.displayName());
        HunterNetworking.broadcastDifficulty(server, difficulty);
        HunterManager.applyDifficulty(server, difficulty);
    }

    public static void setClientDifficulty(HunterDifficulty difficulty) {
        currentDifficulty = difficulty;
    }

    static void onHunterSpawned(ServerPlayerEntity player, HunterEntity hunter) {
        hunter.setDifficulty(currentDifficulty);
        HunterNetworking.sendDifficulty(player, currentDifficulty);
    }
}
