package xyz.qincai.manhunt.player;

public class PlayerEffect {
    private final String type;
    private final int amplifier;
    private final int duration;
    private final boolean ambient;

    public PlayerEffect(String type, int amplifier, int duration, boolean ambient) {
        this.type = type;
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
    }

    public String getType() { return type; }
    public int getAmplifier() { return amplifier; }
    public int getDuration() { return duration; }
    public boolean isAmbient() { return ambient; }
}
