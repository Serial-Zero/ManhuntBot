package com.manhuntbot.client;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.movement.CalculationContext;
import baritone.utils.pathing.Favoring;
import com.manhuntbot.HunterDifficulty;
import com.manhuntbot.ManhuntBotMod;
import com.manhuntbot.entity.HunterEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HunterPathClient {

    private static final int MAX_NODES = 64;
    private static int tickCounter;

    private HunterPathClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) {
                return;
            }
            tick(client);
        });
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        HunterDifficulty difficulty = ManhuntBotMod.getDifficulty();
        if (difficulty == HunterDifficulty.CALM) {
            return;
        }
        tickCounter++;
        if (tickCounter % difficulty.getPathIntervalTicks() != 0) {
            return;
        }

        ClientWorld world = client.world;
        HunterEntity hunter = findHunter(client, world);
        if (hunter == null) {
            return;
        }

        List<BlockPos> path = computePath(client, hunter);
        if (!path.isEmpty()) {
            HunterNetworkingClient.sendPathToServer(hunter.getId(), path);
        }
    }

    private static HunterEntity findHunter(MinecraftClient client, ClientWorld world) {
        if (client.player != null) {
            Box range = client.player.getBoundingBox().expand(160.0D);
            List<HunterEntity> entities = world.getEntitiesByClass(HunterEntity.class, range, HunterEntity::isAlive);
            if (!entities.isEmpty()) {
                return entities.get(0);
            }
        }
        return null;
    }

    private static List<BlockPos> computePath(MinecraftClient client, HunterEntity hunter) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone == null) {
            return List.of();
        }
        try {
            CalculationContext context = new CalculationContext(baritone);
            BetterBlockPos start = new BetterBlockPos(MathHelper.floor(hunter.getX()), MathHelper.floor(hunter.getY()), MathHelper.floor(hunter.getZ()));
            BetterBlockPos target = new BetterBlockPos(MathHelper.floor(client.player.getX()), MathHelper.floor(client.player.getY()), MathHelper.floor(client.player.getZ()));
            Goal goal = new GoalNear(target, 1);
            Favoring favoring = new Favoring(null, context);
            AStarPathFinder finder = new AStarPathFinder(start, start.x, start.y, start.z, goal, favoring, context);
            PathCalculationResult result = finder.calculate(120, 200);
            Optional<IPath> optional = result.getPath();
            if (optional.isEmpty()) {
                return List.of();
            }
            IPath baritonePath = optional.get();
            List<BlockPos> nodes = new ArrayList<>(baritonePath.positions().size());
            baritonePath.positions().forEach(pos -> nodes.add(new BlockPos(pos.x, pos.y, pos.z)));
            if (!nodes.isEmpty()) {
                BlockPos hunterBlock = hunter.getBlockPos();
                nodes.removeIf(pos -> pos.equals(hunterBlock));
            }
            if (nodes.size() > MAX_NODES) {
                return new ArrayList<>(nodes.subList(0, MAX_NODES));
            }
            return nodes;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
