package xyz.qincai.manhunt.ui;

import xyz.qincai.manhunt.config.ConfigManager;

public enum GamePhase {
    OVERWORLD_PREP("Phase I", "Overworld Prep", "advanced.phase.overworld-prep"),
    NETHER_RUSH("Phase II", "Nether Rush", "advanced.phase.nether-rush"),
    FORTRESS_RUN("Phase III", "Fortress Run", "advanced.phase.fortress-run"),
    BLAZE_ROD_RUN("Phase IV", "Blaze Rod Run", "advanced.phase.blaze-rod-run"),
    BASTION_ROUTE("Phase V", "Bastion Route", "advanced.phase.bastion-route"),
    RETURN_EYES("Phase VI", "Return & Eyes", "advanced.phase.return-eyes"),
    STRONGHOLD_DIVE("Phase VII", "Stronghold Dive", "advanced.phase.stronghold-dive"),
    END_RUSH("Phase VIII", "End Rush", "advanced.phase.end-rush"),
    FINALE("Phase IX", "Finale", "advanced.phase.finale");

    private final String number;
    private final String fallbackName;
    private final String messageKey;

    GamePhase(String number, String fallbackName, String messageKey) {
        this.number = number;
        this.fallbackName = fallbackName;
        this.messageKey = messageKey;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return fallbackName;
    }

    public String getName(ConfigManager configManager) {
        return configManager.getMessage(messageKey);
    }

    public String getDisplay() {
        return number + " \u2014 " + fallbackName;
    }

    public String getDisplay(ConfigManager configManager) {
        return number + " \u2014 " + getName(configManager);
    }
}
