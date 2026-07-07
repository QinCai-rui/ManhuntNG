package xyz.qincai.manhunt.platform;

import java.util.UUID;
import xyz.qincai.manhunt.game.Match;

public interface WorldProvider {
    void createGameWorlds(Match match);
    void teleportToMainWorld(Match match);
    void teleportToFormation(Match match);
    void freezeAllPlayers(Match match);
    void unfreezeAllPlayers(Match match);
    void unfreezeHorizontalAllPlayers(Match match);
    void setAllPlayersSurvival(Match match);
    void healAllPlayers(Match match);
    void clearPlayerState(Match match);
    void setAllPlayersInvulnerable(Match match, boolean invulnerable);
    void setInvulnerable(UUID uuid, boolean invulnerable);
    void setGameRule(Match match, String rule, boolean value);
    void clearMobTargets(Match match);
    void clearEffects(Match match);
    void sendTitle(UUID playerUuid, String title, String subtitle);
}
