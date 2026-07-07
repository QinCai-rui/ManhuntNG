package xyz.qincai.manhunt.fabric.listener;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.qincai.manhunt.player.PlayerRole;
import xyz.qincai.manhunt.fabric.game.FabricGameManager;

public class FabricGameListener {

    public FabricGameListener(FabricGameManager gameManager) {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                gameManager.onPlayerDeath(player);
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity victim && source.getAttacker() instanceof ServerPlayerEntity attacker) {
                PlayerRole victimRole = gameManager.getPlayerRoles().get(victim.getUuid());
                PlayerRole attackerRole = gameManager.getPlayerRoles().get(attacker.getUuid());
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
