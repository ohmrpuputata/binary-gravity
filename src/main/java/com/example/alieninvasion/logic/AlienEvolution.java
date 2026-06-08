package com.example.alieninvasion.logic;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Central "evolution" curve: the longer the invasion lasts, the tougher every
// alien that spawns becomes. Applied once at spawn time by the spawn paths.
public final class AlienEvolution {
    private AlienEvolution() {
    }

    public static void evolve(Mob mob, int day) {
        if (mob == null || day <= 0) {
            return;
        }
        // Gentle, capped curve. The base stats are already strong, so the day
        // multiplier only nudges them up instead of doubling everything (which
        // previously made late-game aliens one-shot players).
        double tier = Math.min(day, 20);
        bump(mob, Attributes.MAX_HEALTH, 1.0 + tier * 0.03);                       // up to ~1.6x HP
        bump(mob, Attributes.ATTACK_DAMAGE, 1.0 + tier * 0.02);                    // up to ~1.4x damage
        bump(mob, Attributes.MOVEMENT_SPEED, 1.0 + Math.min(tier * 0.005, 0.10));  // up to +10% speed
        mob.setHealth(mob.getMaxHealth());
    }

    private static void bump(Mob mob, Holder<Attribute> attr, double multiplier) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(inst.getBaseValue() * multiplier);
        }
    }
}
