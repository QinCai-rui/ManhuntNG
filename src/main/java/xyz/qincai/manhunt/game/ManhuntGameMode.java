package xyz.qincai.manhunt.game;

import xyz.qincai.manhunt.config.ConfigManager;

public enum ManhuntGameMode {
    NORMAL("Normal", "advanced.gamemode.normal"),
    INFECTION("Infection", "advanced.gamemode.infection");

    private final String fallbackName;
    private final String messageKey;

    ManhuntGameMode(String fallbackName, String messageKey) {
        this.fallbackName = fallbackName;
        this.messageKey = messageKey;
    }

    public String getDisplayName() {
        return fallbackName;
    }

    public String getDisplayName(ConfigManager configManager) {
        return configManager.getMessage(messageKey);
    }
}
