package xyz.qincai.manhunt.game;

import xyz.qincai.manhunt.config.ConfigManager;

public enum StartMode {
    DREAMSTART("Dreamstart", "advanced.startmode.dreamstart"),
    HEADSTART("Headstart", "advanced.startmode.headstart");

    private final String fallbackName;
    private final String messageKey;

    StartMode(String fallbackName, String messageKey) {
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
