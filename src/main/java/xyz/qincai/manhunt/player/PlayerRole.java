package xyz.qincai.manhunt.player;

public enum PlayerRole {
    RUNNER("Runner"),
    HUNTER("Hunter"),
    SPECTATOR("Spectator");

    private final String displayName;

    PlayerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
