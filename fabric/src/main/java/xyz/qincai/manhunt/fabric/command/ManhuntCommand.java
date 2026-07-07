package xyz.qincai.manhunt.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.ManhuntMod;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

public class ManhuntCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ManhuntMod mod) {
        var root = Commands.literal("manhunt")
                .requires(src -> src.hasPermission(2));

        root.then(Commands.literal("start")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    mod.getGameManager().startGame(player);
                    return 1;
                })
        );

        root.then(Commands.literal("stop")
                .executes(ctx -> {
                    mod.getGameManager().endGame();
                    ctx.getSource().sendSuccess(() -> Component.literal("Game stopped.").withStyle(ChatFormatting.RED), true);
                    return 1;
                })
        );

        root.then(Commands.literal("pause")
                .executes(ctx -> {
                    mod.getGameManager().pauseGame();
                    return 1;
                })
        );

        root.then(Commands.literal("resume")
                .executes(ctx -> {
                    mod.getGameManager().resumeGame();
                    return 1;
                })
        );

        root.then(Commands.literal("setrunner")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            var roles = mod.getGameManager().getPlayerRoles();
                            roles.values().removeIf(r -> r == PlayerRole.RUNNER);
                            roles.put(target.getUUID(), PlayerRole.RUNNER);
                            mod.getGameManager().getRunners().add(target);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal(target.getName().getString() + " is now runner.").withStyle(ChatFormatting.AQUA), true);
                            return 1;
                        })
                )
        );

        root.then(Commands.literal("sethunter")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            var roles = mod.getGameManager().getPlayerRoles();
                            roles.put(target.getUUID(), PlayerRole.HUNTER);
                            mod.getGameManager().getHunters().add(target);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal(target.getName().getString() + " is now hunter.").withStyle(ChatFormatting.RED), true);
                            return 1;
                        })
                )
        );

        root.then(Commands.literal("status")
                .executes(ctx -> {
                    var gm = mod.getGameManager();
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("Game state: " + gm.getState()).withStyle(ChatFormatting.YELLOW), false);
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("Runners: " + gm.getRunners().size() + ", Hunters: " + gm.getHunters().size()).withStyle(ChatFormatting.GRAY), false);
                    return 1;
                })
        );

        dispatcher.register(root);
    }
}
