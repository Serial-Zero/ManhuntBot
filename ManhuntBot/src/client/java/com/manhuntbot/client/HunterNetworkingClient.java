package com.manhuntbot.client;

import com.manhuntbot.HunterDifficulty;
import com.manhuntbot.ManhuntBotMod;
import com.manhuntbot.network.HunterNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class HunterNetworkingClient {

    private HunterNetworkingClient() {}

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(HunterNetworking.DIFFICULTY_UPDATE, (client, handler, buf, responseSender) -> {
            int ordinal = buf.readVarInt();
            HunterDifficulty[] difficulties = HunterDifficulty.values();
            if (ordinal < 0 || ordinal >= difficulties.length) {
                return;
            }
            HunterDifficulty difficulty = difficulties[ordinal];
            client.execute(() -> ManhuntBotMod.setClientDifficulty(difficulty));
        });
    }

    public static void sendPathToServer(int entityId, List<BlockPos> path) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeVarInt(path.size());
        for (BlockPos pos : path) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
        ClientPlayNetworking.send(HunterNetworking.PATH_UPDATE, buf);
    }
}
