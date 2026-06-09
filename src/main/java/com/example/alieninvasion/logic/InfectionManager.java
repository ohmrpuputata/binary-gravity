package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dose-style infection mechanic (mirrors {@link RadiationManager}).
 *
 * You do NOT get infected the instant you touch corrupted ground. Standing on it
 * is tracked: only after a ~2 second grace does the infection meter start to
 * climb, building a visible scale. Step off and it slowly recovers; let it climb
 * and it escalates the INFECTION effect through its tiers. Cures (antidote, serum)
 * and the full Chitin/Cosmic sets flush the meter.
 *
 * Like the radiation dose, the meter is a non-persisted per-player session value
 * (cleared on death) kept in a static map so the HUD can read it in single-player.
 */
public final class InfectionManager {
    private InfectionManager() {
    }

    public static final float MAX = 100.0F;
    private static final int GRACE_SECONDS = 2;
    private static final float GAIN = 8.0F;  // meter per second while standing (post-grace)

    private static final Map<UUID, Float> METER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STAND = new ConcurrentHashMap<>();

    public static float getMeter(UUID id) {
        return METER.getOrDefault(id, 0.0F);
    }

    public static float getMeter(Player player) {
        return getMeter(player.getUUID());
    }

    private static void setMeter(UUID id, float v) {
        v = Math.max(0.0F, Math.min(MAX, v));
        if (v <= 0.01F) {
            METER.remove(id);
        } else {
            METER.put(id, v);
        }
    }

    public static void reduceMeter(Player player, float amount) {
        setMeter(player.getUUID(), getMeter(player) - amount);
    }

    public static void addMeter(Player player, float amount) {
        setMeter(player.getUUID(), getMeter(player) + amount);
    }

    public static void clear(Player player) {
        METER.remove(player.getUUID());
        STAND.remove(player.getUUID());
    }

    /**
     * Per-second player update.
     *
     * @param onAlienGround whether the player is standing on/in corrupted ground
     * @param immune        full Chitin/Cosmic set (or similar) — no intake, meter flushed
     */
    public static void tickPlayer(ServerLevel level, ServerPlayer player, boolean onAlienGround, boolean immune) {
        UUID id = player.getUUID();
        if (immune || player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            clear(player);
            return;
        }

        float meter = getMeter(id);
        if (onAlienGround) {
            int stood = STAND.merge(id, 1, Integer::sum);
            if (stood <= GRACE_SECONDS) {
                // Grace window: warn the player to step off before it takes hold.
                player.displayClientMessage(Component.literal(
                        "§e⚠ Заражённая земля! Сойдите (" + (GRACE_SECONDS - stood + 1) + "с)"), true);
            } else {
                meter += GAIN;
                level.sendParticles(ParticleTypes.WARPED_SPORE, player.getX(), player.getY() + 0.2D, player.getZ(),
                        6, player.getBbWidth() * 0.5D, 0.1D, player.getBbWidth() * 0.5D, 0.0D);
                if (level.random.nextInt(3) == 0) {
                    level.playSound(null, player.blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD,
                            SoundSource.PLAYERS, 0.4F, 0.7F + level.random.nextFloat() * 0.3F);
                }
            }
        } else {
            STAND.remove(id);
            // No natural decay — infection only clears through medicine
        }
        setMeter(id, meter);
        meter = getMeter(id);

        // Death at 100%
        if (meter >= MAX) {
            player.hurt(level.damageSources().magic(), 1000.0F);
            return;
        }

        var infHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);
        if (meter >= 75.0F) {
            player.addEffect(new MobEffectInstance(infHolder, 120, 1, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 120, 0, false, true));
        } else if (meter >= 50.0F) {
            player.addEffect(new MobEffectInstance(infHolder, 120, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 120, 0, false, true));
        } else if (meter >= 25.0F) {
            player.addEffect(new MobEffectInstance(infHolder, 120, 0, false, true));
        }

        if (meter >= 25.0F) {
            String c = meter >= 75.0F ? "§4" : meter >= 50.0F ? "§c" : "§e";
            player.displayClientMessage(Component.literal(c + "☣ Заражение: " + (int) meter + "%"), true);
        }
    }
}
