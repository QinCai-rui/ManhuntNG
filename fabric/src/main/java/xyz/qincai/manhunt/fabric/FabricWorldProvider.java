package xyz.qincai.manhunt.fabric;

import xyz.qincai.manhunt.game.Match;
import xyz.qincai.manhunt.platform.ManhuntPlatform;
import xyz.qincai.manhunt.platform.WorldProvider;

import java.util.UUID;

public class FabricWorldProvider implements WorldProvider {
    private final ManhuntPlatform platform;

    public FabricWorldProvider(ManhuntPlatform platform) {
        this.platform = platform;
    }

    @Override public void createGameWorlds(Match match) {}
    @Override public void teleportToMainWorld(Match match) {}
    @Override public void teleportToFormation(Match match) {}
    @Override public void freezeAllPlayers(Match match) {}
    @Override public void unfreezeAllPlayers(Match match) {}
    @Override public void unfreezeHorizontalAllPlayers(Match match) {}
    @Override public void setAllPlayersSurvival(Match match) {}
    @Override public void healAllPlayers(Match match) {}
    @Override public void clearPlayerState(Match match) {}
    @Override public void setAllPlayersInvulnerable(Match match, boolean invulnerable) {}
    @Override public void setInvulnerable(UUID uuid, boolean invulnerable) {}
    @Override public void setGameRule(Match match, String rule, boolean value) {}
    @Override public void clearMobTargets(Match match) {}
    @Override public void clearEffects(Match match) {}

    @Override
    public void sendTitle(UUID playerUuid, String title, String subtitle) {}
}
