package xyz.qincai.manhunt.fabric.listener;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

public class FabricGameListener {

    public FabricGameListener(FabricGameManager gameManager) {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                gameManager.onPlayerDeath(player);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer attacker) {
                PlayerRole victimRole = gameManager.getPlayerRoles().get(victim.getUUID());
                PlayerRole attackerRole = gameManager.getPlayerRoles().get(attacker.getUUID());
                if (victimRole == PlayerRole.RUNNER && attackerRole == PlayerRole.HUNTER) {
                    return true;
                }
                if (attackerRole == PlayerRole.RUNNER) {
                    return false;
                }
            }
            return true;
        });
    }
}
