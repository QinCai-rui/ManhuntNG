package xyz.qincai.manhunt.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.player.PlayerRole;

public class FabricCommandRegistrar {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext context,
                                ManhuntNG mod) {
        dispatcher.register(Commands.literal("manhunt")
                .then(Commands.literal("join")
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            var gameManager = mod.getGameManager();
                            if (gameManager.isGameActive()) {
                                ctx.getSource().sendFailure(Component.literal("A game is already in progress!"));
                                return 0;
                            }
                            mod.getPlayerRegistry().setRole(player.getUUID(), PlayerRole.SPECTATOR);
                            gameManager.getMatch().addSpectator(player.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("You joined the manhunt lobby!"), false);
                            return 1;
                        }))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            if (mod.getGameManager().isGameActive()) {
                                ctx.getSource().sendFailure(Component.literal("Cannot leave during an active game!"));
                                return 0;
                            }
                            mod.getPlayerRegistry().removePlayerFromGame(player.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("You left the manhunt lobby."), false);
                            return 1;
                        }))
                .then(Commands.literal("start")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            mod.getGameManager().startGame(player != null ? player.getUUID() : null);
                            return 1;
                        }))
                .then(Commands.literal("stop")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            mod.getGameManager().stopGame();
                            ctx.getSource().sendSuccess(() -> Component.literal("Game stopped."), false);
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            mod.getConfigProvider().reloadConfigs();
                            ctx.getSource().sendSuccess(() -> Component.literal("Configuration reloaded!"), false);
                            return 1;
                        }))
                .then(Commands.literal("runner")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    var target = EntityArgument.getPlayer(ctx, "player");
                                    var gm = mod.getGameManager();
                                    if (gm.isGameActive()) return 0;
                                    mod.getPlayerRegistry().removePlayerFromGame(target.getUUID());
                                    mod.getPlayerRegistry().setRole(target.getUUID(), PlayerRole.RUNNER);
                                    gm.getMatch().setRunnerUuid(target.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is now the Runner!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("hunter")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    var target = EntityArgument.getPlayer(ctx, "player");
                                    var gm = mod.getGameManager();
                                    if (gm.isGameActive()) return 0;
                                    mod.getPlayerRegistry().removePlayerFromGame(target.getUUID());
                                    mod.getPlayerRegistry().setRole(target.getUUID(), PlayerRole.HUNTER);
                                    gm.getMatch().addHunter(target.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is now a Hunter!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("forcestart")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            mod.getGameManager().startGameForce(player != null ? player.getUUID() : null);
                            ctx.getSource().sendSuccess(() -> Component.literal("Game force started!"), false);
                            return 1;
                        }))
        );
    }
}
