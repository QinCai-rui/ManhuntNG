package xyz.qincai.manhunt.ui;

public enum GamePhase {
    OVERWORLD_PREP("Phase I", "Overworld Prep"),
    NETHER_RUSH("Phase II", "Nether Rush"),
    FORTRESS_RUN("Phase III", "Fortress Run"),
    BLAZE_ROD_RUN("Phase IV", "Blaze Rod Run"),
    BASTION_ROUTE("Phase V", "Bastion Route"),
    RETURN_EYES("Phase VI", "Return & Eyes"),
    STRONGHOLD_DIVE("Phase VII", "Stronghold Dive"),
    END_RUSH("Phase VIII", "End Rush"),
    FINALE("Phase IX", "Finale");

    private final String number;
    private final String name;

    GamePhase(String number, String name) {
        this.number = number;
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return number + " \u2014 " + name;
    }
}
