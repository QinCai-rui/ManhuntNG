package xyz.qincai.manhunt.fabric.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xyz.qincai.manhunt.ui.GamePhase;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

public class FabricUIManager {
    private final FabricGameManager gameManager;
    private int phaseIndex = 0;
    private final GamePhase[] phases = GamePhase.values();

    public FabricUIManager(FabricGameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void update() {
        for (ServerPlayer player : gameManager.getRunners()) {
            player.displayClientMessage(Component.literal("\u27a1 " + getCurrentPhase().getDisplay()).withStyle(ChatFormatting.GOLD), true);
        }
        for (ServerPlayer player : gameManager.getHunters()) {
            int runnerCount = gameManager.getRunners().size();
            player.displayClientMessage(Component.literal("\u27a1 Runners alive: " + runnerCount).withStyle(ChatFormatting.RED), true);
        }
    }

    public GamePhase getCurrentPhase() {
        long time = System.currentTimeMillis();
        int idx = (int) ((time / 60000) % phases.length);
        return phases[Math.min(idx, phases.length - 1)];
    }

    public void showTitle(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.displayClientMessage(Component.literal("\u2728 " + title + " - " + subtitle).withStyle(ChatFormatting.BOLD), false);
    }
}
