package xyz.qincai.manhunt.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.qincai.manhunt.game.GameState;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.ManhuntMod;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

import java.util.HashSet;

public class ManhuntCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, ManhuntMod mod) {
        var root = CommandManager.literal("manhunt")
                .requires(src -> src.hasPermissionLevel(2));

        root.then(CommandManager.literal("start")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    mod.getGameManager().startGame(player);
                    return 1;
                })
        );

        root.then(CommandManager.literal("stop")
                .executes(ctx -> {
                    mod.getGameManager().endGame();
                    ctx.getSource().sendFeedback(() -> Text.literal("Game stopped.").formatted(Formatting.RED), true);
                    return 1;
                })
        );

        root.then(CommandManager.literal("pause")
                .executes(ctx -> {
                    mod.getGameManager().pauseGame();
                    return 1;
                })
        );

        root.then(CommandManager.literal("resume")
                .executes(ctx -> {
                    mod.getGameManager().resumeGame();
                    return 1;
                })
        );

        root.then(CommandManager.literal("setrunner")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            var roles = mod.getGameManager().getPlayerRoles();
                            roles.values().removeIf(r -> r == PlayerRole.RUNNER);
                            roles.put(target.getUuid(), PlayerRole.RUNNER);
                            mod.getGameManager().getRunners().add(target);
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal(target.getName().getString() + " is now runner.").formatted(Formatting.AQUA), true);
                            return 1;
                        })
                )
        );

        root.then(CommandManager.literal("sethunter")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> {
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                            var roles = mod.getGameManager().getPlayerRoles();
                            roles.put(target.getUuid(), PlayerRole.HUNTER);
                            mod.getGameManager().getHunters().add(target);
                            ctx.getSource().sendFeedback(() ->
                                    Text.literal(target.getName().getString() + " is now hunter.").formatted(Formatting.RED), true);
                            return 1;
                        })
                )
        );

        root.then(CommandManager.literal("status")
                .executes(ctx -> {
                    var gm = mod.getGameManager();
                    ctx.getSource().sendFeedback(() ->
                            Text.literal("Game state: " + gm.getState()).formatted(Formatting.YELLOW), false);
                    ctx.getSource().sendFeedback(() ->
                            Text.literal("Runners: " + gm.getRunners().size() + ", Hunters: " + gm.getHunters().size()).formatted(Formatting.GRAY), false);
                    return 1;
                })
        );

        dispatcher.register(root);
    }
}
