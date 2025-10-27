package com.manhuntbot.command;

import com.manhuntbot.HunterDifficulty;
import com.manhuntbot.ManhuntBotMod;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class HunterCommand {

    private HunterCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(buildRoot("hunter"));
            dispatcher.register(buildRoot("manhunt"));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildRoot(String literal) {
        return CommandManager.literal(literal)
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.literal("difficulty")
                        .then(CommandManager.literal("calm").executes(ctx -> setDifficulty(ctx.getSource(), HunterDifficulty.CALM)))
                        .then(CommandManager.literal("easy").executes(ctx -> setDifficulty(ctx.getSource(), HunterDifficulty.EASY)))
                        .then(CommandManager.literal("medium").executes(ctx -> setDifficulty(ctx.getSource(), HunterDifficulty.MEDIUM)))
                        .then(CommandManager.literal("hard").executes(ctx -> setDifficulty(ctx.getSource(), HunterDifficulty.HARD))))
                .then(CommandManager.literal("info").executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    source.sendFeedback(Text.literal("Hunter difficulty: " + ManhuntBotMod.getDifficulty().displayName()), false);
                    return 1;
                }));
    }

    private static int setDifficulty(ServerCommandSource source, HunterDifficulty difficulty) {
        if (source.getServer() == null) {
            return 0;
        }
        ManhuntBotMod.setDifficulty(difficulty, source.getServer());
        source.sendFeedback(Text.literal("Set hunter difficulty to " + difficulty.displayName()), true);
        return 1;
    }
}
