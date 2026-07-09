package xyz.qincai.manhunt.game;

public enum ManhuntGameMode {
    NORMAL("Normal"),
    INFECTION("Infection");

    private final String displayName;

    ManhuntGameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
