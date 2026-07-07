package xyz.qincai.manhunt.platform;

import xyz.qincai.manhunt.player.PlayerEffect;

import java.util.List;

public interface ConfigProvider {
    void loadConfigs();
    void reloadConfigs();
    String getMessage(String key, String... replacements);

    int getPreHuntCountdown();
    double getHunterCircleRadius();
    int getFormationSearchRadius();
    boolean isTrackingEnabled();
    int getTrackingUpdateTicks();
    boolean isTrackingShowDistance();
    int getRunnerLives();
    boolean isHunterInfiniteRespawns();
    int getHunterRespawnLimit();
    boolean isScoreboardEnabled();
    boolean isActionBarEnabled();
    boolean isHunterKeepInventory();
    boolean isHunterKeepArmor();
    List<PlayerEffect> getRunnerPotionEffects();
    List<PlayerEffect> getHunterPotionEffects();
}
