package xyz.qincai.manhunt.fabric.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.*;

public class FabricPotionEffectManager {

    private static final Map<String, MobEffect> EFFECT_MAP = new HashMap<>();

    static {
        EFFECT_MAP.put("speed", MobEffects.MOVEMENT_SPEED);
        EFFECT_MAP.put("slowness", MobEffects.MOVEMENT_SLOWNESS);
        EFFECT_MAP.put("haste", MobEffects.DIG_SPEED);
        EFFECT_MAP.put("mining_fatigue", MobEffects.DIG_SLOWNESS);
        EFFECT_MAP.put("strength", MobEffects.DAMAGE_BOOST);
        EFFECT_MAP.put("jump_boost", MobEffects.JUMP);
        EFFECT_MAP.put("regeneration", MobEffects.REGENERATION);
        EFFECT_MAP.put("resistance", MobEffects.DAMAGE_RESISTANCE);
        EFFECT_MAP.put("fire_resistance", MobEffects.FIRE_RESISTANCE);
        EFFECT_MAP.put("water_breathing", MobEffects.WATER_BREATHING);
        EFFECT_MAP.put("invisibility", MobEffects.INVISIBILITY);
        EFFECT_MAP.put("night_vision", MobEffects.NIGHT_VISION);
        EFFECT_MAP.put("weakness", MobEffects.WEAKNESS);
        EFFECT_MAP.put("poison", MobEffects.POISON);
    }

    public void applyEffects(ServerPlayer player, PlayerRole role, List<String> effects) {
        for (String raw : effects) {
            String[] parts = raw.split(":");
            if (parts.length < 2) continue;

            MobEffect effect = EFFECT_MAP.get(parts[0]);
            if (effect == null) continue;

            int amplifier = Mth.clamp(Integer.parseInt(parts[1]) - 1, 0, 255);
            int duration = parts.length > 2 && parts[2].equals("infinite") ? -1
                    : Integer.parseInt(parts[2]) * 20;

            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
        }
    }

    public void clearEffects(ServerPlayer player) {
        player.removeAllEffects();
    }
}
