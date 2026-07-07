package xyz.qincai.manhunt.fabric.player;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import xyz.qincai.manhunt.player.PlayerRole;

import java.util.*;

public class FabricPotionEffectManager {

    private static final Map<String, StatusEffect> EFFECT_MAP = new HashMap<>();

    static {
        EFFECT_MAP.put("speed", StatusEffects.SPEED);
        EFFECT_MAP.put("slowness", StatusEffects.SLOWNESS);
        EFFECT_MAP.put("haste", StatusEffects.HASTE);
        EFFECT_MAP.put("mining_fatigue", StatusEffects.MINING_FATIGUE);
        EFFECT_MAP.put("strength", StatusEffects.STRENGTH);
        EFFECT_MAP.put("jump_boost", StatusEffects.JUMP_BOOST);
        EFFECT_MAP.put("regeneration", StatusEffects.REGENERATION);
        EFFECT_MAP.put("resistance", StatusEffects.RESISTANCE);
        EFFECT_MAP.put("fire_resistance", StatusEffects.FIRE_RESISTANCE);
        EFFECT_MAP.put("water_breathing", StatusEffects.WATER_BREATHING);
        EFFECT_MAP.put("invisibility", StatusEffects.INVISIBILITY);
        EFFECT_MAP.put("night_vision", StatusEffects.NIGHT_VISION);
        EFFECT_MAP.put("weakness", StatusEffects.WEAKNESS);
        EFFECT_MAP.put("poison", StatusEffects.POISON);
    }

    public void applyEffects(ServerPlayerEntity player, PlayerRole role, List<String> effects) {
        for (String raw : effects) {
            String[] parts = raw.split(":");
            if (parts.length < 2) continue;

            StatusEffect effect = EFFECT_MAP.get(parts[0]);
            if (effect == null) continue;

            int amplifier = MathHelper.clamp(Integer.parseInt(parts[1]) - 1, 0, 255);
            int duration = parts.length > 2 && parts[2].equals("infinite") ? -1
                    : Integer.parseInt(parts[2]) * 20;

            player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false));
        }
    }

    public void clearEffects(ServerPlayerEntity player) {
        player.clearStatusEffects();
    }
}
