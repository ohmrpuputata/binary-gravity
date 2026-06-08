package com.example.alieninvasion.logic;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Block-distance radiation field around a Pure Radiation Block (design-spec model).
 *
 *   <= 50 (here scanned to ~32 for perf): light radiation while in range.
 *   <= 16: strong radiation.
 *   <=  8: lethal irradiation - sticks until death; a full hazmat suit downgrades
 *          it to a survivable chip, but only at this range.
 *
 * Aliens are immune (their hide adapted to their irradiated homeworld). Carrying a
 * raw radiation crystal without a hazmat suit is also lethal. Damage rates live in
 * the effect classes; this manager just applies the right tier each second.
 *
 * Light radius is scanned to 32 rather than the spec's 50 so the per-second block
 * scan stays cheap; bump PLAYER_SCAN if you want the full 50.
 */
public final class RadiationFieldManager {
    private RadiationFieldManager() {
    }

    private static final int LETHAL = 8;
    private static final int STRONG = 16;
    private static final int LIGHT = 32;
    private static final int STEP = 2;          // sparse sampling for the scan
    private static final int PLAYER_SCAN = 32;
    private static final int CREATURE_SCAN = 16; // creatures only feel the closer zones

    private static Holder<MobEffect> h(MobEffect e) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e);
    }

    /** Nearest Pure Radiation Block (sparse scan); returns Double.MAX_VALUE if none. */
    private static double nearestDistSq(ServerLevel level, BlockPos c, int max) {
        double best = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -max; dx <= max; dx += STEP) {
            for (int dy = -max; dy <= max; dy += STEP) {
                for (int dz = -max; dz <= max; dz += STEP) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (level.getBlockState(m).is(ModBlocks.PURE_RADIATION_BLOCK)) {
                        double d = dx * dx + dy * dy + dz * dz;
                        if (d < best) best = d;
                    }
                }
            }
        }
        return best;
    }

    public static void tickPlayer(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            return;
        }
        apply(level, player, RadiationManager.hasFullHazmat(player), RadiationManager.hasFullLightHazmat(player),
                holdsCrystal(player), PLAYER_SCAN);
    }

    /** Animals/villagers near the player also take the field (your livestock dies). */
    public static void radiateCreatures(ServerLevel level, ServerPlayer anchor) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, anchor.getBoundingBox().inflate(20.0D),
                x -> x.isAlive() && (x instanceof Animal || x instanceof AbstractVillager))) {
            apply(level, e, false, false, false, CREATURE_SCAN);
        }
    }

    private static void apply(ServerLevel level, LivingEntity e, boolean fullHazmat, boolean lightHazmat,
                              boolean holdsCrystal, int scan) {
        if (AlienUtils.isAlliedTo(null, e)) {
            return; // aliens are radiation-immune
        }
        // Carrying raw crystal unshielded is a death sentence.
        if (holdsCrystal && !fullHazmat) {
            e.addEffect(new MobEffectInstance(h(ModEffects.IRRADIATION), 1_000_000, 0, false, true));
            return;
        }
        double dsq = nearestDistSq(level, e.blockPosition(), scan);
        if (dsq == Double.MAX_VALUE) {
            return;
        }
        double d = Math.sqrt(dsq);
        if (d <= LETHAL) {
            if (fullHazmat) {
                e.addEffect(new MobEffectInstance(h(ModEffects.RADIATION), 60, 0, false, true)); // chips through the suit
            } else {
                e.addEffect(new MobEffectInstance(h(ModEffects.IRRADIATION), 1_000_000, 0, false, true));
            }
        } else if (d <= STRONG) {
            if (!fullHazmat) {
                e.addEffect(new MobEffectInstance(h(ModEffects.STRONG_RADIATION), 60, 0, false, true));
            }
        } else if (d <= LIGHT) {
            // Light radiation: both the light suit and the full hazmat shield it.
            if (!fullHazmat && !lightHazmat) {
                e.addEffect(new MobEffectInstance(h(ModEffects.RADIATION), 60, 0, false, true));
            }
        }
    }

    private static boolean holdsCrystal(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ItemRegistry.RADIATION_CRYSTAL)) {
                return true;
            }
        }
        return false;
    }
}
