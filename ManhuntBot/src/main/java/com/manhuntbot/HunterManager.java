package com.manhuntbot;

import com.manhuntbot.entity.HunterEntity;
import com.manhuntbot.entity.ModEntities;
import com.manhuntbot.network.HunterNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class HunterManager {

    private static final int RESPAWN_DELAY_TICKS = 200;
    private static final Map<ServerWorld, HunterHolder> DATA = new WeakHashMap<>();

    private HunterManager() {}

    public static void tickWorld(ServerWorld world) {
        HunterHolder holder = DATA.computeIfAbsent(world, unused -> new HunterHolder());
        holder.removeMissingPlayers(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (holder.syncedPlayers.add(player.getUuid())) {
                HunterNetworking.sendDifficulty(player, ManhuntBotMod.getDifficulty());
            }
        }
        HunterEntity current = holder.get();
        if (current == null || !current.isAlive()) {
            if (world.getPlayers().isEmpty()) {
                return;
            }
            if (holder.respawnCooldown > 0) {
                holder.respawnCooldown--;
                return;
            }
            ServerPlayerEntity targetPlayer = world.getPlayers().get(0);
            spawnHunter(world, targetPlayer.getBlockPos());
            holder.respawnCooldown = RESPAWN_DELAY_TICKS;
        } else {
            holder.respawnCooldown = RESPAWN_DELAY_TICKS;
        }
    }

    public static void applyDifficulty(MinecraftServer server, HunterDifficulty difficulty) {
        for (ServerWorld world : server.getWorlds()) {
            HunterHolder holder = DATA.computeIfAbsent(world, unused -> new HunterHolder());
            HunterEntity hunter = holder.get();
            if (hunter != null) {
                hunter.setDifficulty(difficulty);
            }
            holder.syncedPlayers.clear();
            for (ServerPlayerEntity player : world.getPlayers()) {
                holder.syncedPlayers.add(player.getUuid());
            }
        }
    }

    private static void spawnHunter(ServerWorld world, BlockPos playerPos) {
        HunterHolder holder = DATA.computeIfAbsent(world, unused -> new HunterHolder());

        HunterEntity hunter = ModEntities.HUNTER.create(world);
        if (hunter == null) {
            return;
        }
        BlockPos spawnPos = findSpawnPos(world, playerPos);
        hunter.refreshPositionAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, world.random.nextFloat() * 360F, 0F);
        hunter.setDifficulty(ManhuntBotMod.getDifficulty());
        world.spawnEntity(hunter);

        holder.entity = new WeakReference<>(hunter);
        holder.respawnCooldown = RESPAWN_DELAY_TICKS;
        world.getPlayers().forEach(player -> {
            ManhuntBotMod.onHunterSpawned(player, hunter);
            holder.syncedPlayers.add(player.getUuid());
        });
    }

    private static BlockPos findSpawnPos(ServerWorld world, BlockPos origin) {
        BlockPos.Mutable mutable = origin.mutableCopy();
        for (int attempt = 0; attempt < 40; attempt++) {
            int radius = 12 + attempt / 2;
            int offsetX = MathHelper.nextInt(world.random, -radius, radius);
            int offsetZ = MathHelper.nextInt(world.random, -radius, radius);
            mutable.set(origin.getX() + offsetX, origin.getY(), origin.getZ() + offsetZ);

            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mutable.getX(), mutable.getZ());
            mutable.setY(topY);
            if (!world.getBlockState(mutable.down()).isAir()) {
                return mutable.toImmutable();
            }
        }
        return origin;
    }

    private static final class HunterHolder {
        private WeakReference<HunterEntity> entity = new WeakReference<>(null);
        private int respawnCooldown = 0;
        private final Set<UUID> syncedPlayers = new HashSet<>();

        HunterEntity get() {
            HunterEntity hunter = entity.get();
            return hunter != null && hunter.isAlive() ? hunter : null;
        }

        void removeMissingPlayers(ServerWorld world) {
            syncedPlayers.removeIf(uuid -> world.getPlayerByUuid(uuid) == null);
        }
    }
}
