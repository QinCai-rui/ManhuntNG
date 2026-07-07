package xyz.qincai.manhunt.fabric;

import xyz.qincai.manhunt.ManhuntNG;
import xyz.qincai.manhunt.platform.UIFacade;
import xyz.qincai.manhunt.ui.GamePhase;

public class FabricUIFacade implements UIFacade {
    private GamePhase currentPhase = GamePhase.OVERWORLD_PREP;

    @Override
    public void setCurrentPhase(GamePhase phase) { this.currentPhase = phase; }

    @Override
    public GamePhase getCurrentPhase() { return currentPhase; }

    @Override
    public void updatePhase() {}

    @Override
    public void startUIUpdates() {}

    @Override
    public void stopUIUpdates() {}

    @Override
    public void showPauseTitle() {}

    @Override
    public void hidePauseTitle() {}

    @Override
    public void sendTitle(String title, String subtitle) {}

    @Override
    public void sendToAll(String message) {}

    @Override
    public void broadcastMessage(String message) {
        ManhuntNG.getInstance().getLogger().info(message);
    }
}
