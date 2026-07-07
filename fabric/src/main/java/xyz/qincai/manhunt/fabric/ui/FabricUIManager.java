package xyz.qincai.manhunt.fabric.ui;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
        for (ServerPlayerEntity player : gameManager.getRunners()) {
            player.sendMessage(Text.literal("\u27a1 " + getCurrentPhase().getDisplay()).formatted(Formatting.GOLD), true);
        }
        for (ServerPlayerEntity player : gameManager.getHunters()) {
            int runnerCount = gameManager.getRunners().size();
            player.sendMessage(Text.literal("\u27a1 Runners alive: " + runnerCount).formatted(Formatting.RED), true);
        }
    }

    public GamePhase getCurrentPhase() {
        long time = System.currentTimeMillis();
        int idx = (int) ((time / 60000) % phases.length);
        return phases[Math.min(idx, phases.length - 1)];
    }

    public void showTitle(ServerPlayerEntity player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // Title display would use packet-based approach in Fabric
        player.sendMessage(Text.literal("\u2728 " + title + " - " + subtitle).formatted(Formatting.BOLD), false);
    }
}
