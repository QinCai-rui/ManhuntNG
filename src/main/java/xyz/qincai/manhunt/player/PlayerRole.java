package xyz.qincai.manhunt.player;

import xyz.qincai.manhunt.config.ConfigManager;

public enum PlayerRole {
    RUNNER("Runner", "advanced.role.runner"),
    HUNTER("Hunter", "advanced.role.hunter"),
    SPECTATOR("Spectator", "advanced.role.spectator");

    private final String fallbackName;
    private final String messageKey;

    PlayerRole(String fallbackName, String messageKey) {
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
