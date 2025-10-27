package com.manhuntbot.network;

import com.manhuntbot.HunterDifficulty;
import com.manhuntbot.ManhuntBotMod;
import com.manhuntbot.entity.HunterEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class HunterNetworking {

    public static final Identifier PATH_UPDATE = new Identifier(ManhuntBotMod.MOD_ID, "path_update");
    public static final Identifier DIFFICULTY_UPDATE = new Identifier(ManhuntBotMod.MOD_ID, "difficulty_update");

    private HunterNetworking() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PATH_UPDATE, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt();
            int size = buf.readVarInt();
            List<BlockPos> nodes = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                nodes.add(new BlockPos(x, y, z));
            }
            server.execute(() -> {
                Entity entity = player.getWorld().getEntityById(entityId);
                if (entity instanceof HunterEntity hunter) {
                    hunter.applyBaritonePath(nodes);
                }
            });
        });
    }

    public static void broadcastDifficulty(MinecraftServer server, HunterDifficulty difficulty) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendDifficulty(player, difficulty);
        }
    }

    public static void sendDifficulty(ServerPlayerEntity player, HunterDifficulty difficulty) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(difficulty.ordinal());
        ServerPlayNetworking.send(player, DIFFICULTY_UPDATE, buf);
    }
}
