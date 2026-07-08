package xyz.qincai.manhunt.game;

public enum GameMode {
    NORMAL("Normal"),
    INFECTION("Infection");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}