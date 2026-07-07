package xyz.qincai.manhunt.platform;

import xyz.qincai.manhunt.ui.GamePhase;

public interface UIFacade {
    void setCurrentPhase(GamePhase phase);
    GamePhase getCurrentPhase();
    void updatePhase();
    void startUIUpdates();
    void stopUIUpdates();
    void showPauseTitle();
    void hidePauseTitle();
    void sendTitle(String title, String subtitle);
    void sendToAll(String message);
    void broadcastMessage(String message);
}
