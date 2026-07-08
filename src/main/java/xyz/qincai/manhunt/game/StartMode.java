package xyz.qincai.manhunt.game;

public enum StartMode {
    DREAMSTART("Dreamstart"),
    HEADSTART("Headstart");

    private final String displayName;

    StartMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
