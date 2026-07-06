package xyz.qincai.manhunt.ui;

public enum GamePhase {
    OVERWORLD_PREP("Phase I", "Overworld Prep"),
    NETHER_RUSH("Phase II", "Nether Rush"),
    FORTRESS_RUN("Phase III", "Fortress Run"),
    BASTION_ROUTE("Phase IV", "Bastion Route"),
    RETURN_EYES("Phase V", "Return & Eyes"),
    STRONGHOLD_DIVE("Phase VI", "Stronghold Dive"),
    END_RUSH("Phase VII", "End Rush"),
    FINALE("Phase VIII", "Finale");

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
