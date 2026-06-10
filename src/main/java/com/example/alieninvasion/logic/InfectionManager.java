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
import net.minecraft.world.effect.MobEffects;
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
    private static final int GRACE_SECONDS = 3;
    private static final float GAIN = 1.5F;  // meter per second while standing (post-grace)

    private static final Map<UUID, Float>   METER      = new ConcurrentHashMap<>();
    private static final Map<UUID, Float>   METER_MULT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STAND      = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_TIER  = new ConcurrentHashMap<>();

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

    /** Set per-player infection intake multiplier (1.0 = normal, 0.5 = half speed). */
    public static void setMeterMultiplier(Player player, float mult) {
        if (mult == 1.0F) METER_MULT.remove(player.getUUID());
        else METER_MULT.put(player.getUUID(), mult);
    }

    public static void addMeter(Player player, float amount) {
        float mult = METER_MULT.getOrDefault(player.getUUID(), 1.0F);
        setMeter(player.getUUID(), getMeter(player) + amount * mult);
    }

    /** Cap the infection meter so it cannot exceed {@code max}. */
    public static void capMeter(Player player, float max) {
        float cur = getMeter(player);
        if (cur > max) setMeter(player.getUUID(), max);
    }

    public static void clear(Player player) {
        METER.remove(player.getUUID());
        STAND.remove(player.getUUID());
        LAST_TIER.remove(player.getUUID());
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && !sp.level().isClientSide) {
            sp.removeEffect(MobEffects.CONFUSION);
        }
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
        float meterBefore = meter;
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
        float meterDelta = meter - meterBefore;

        // Тошнота при заражении: пока метр растёт — накладываем CONFUSION (как помехи у радиации)
        if (meterDelta > 0.0F) {
            var existing = player.getEffect(MobEffects.CONFUSION);
            if (existing == null || existing.getDuration() < 60) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 260, 0, false, true));
            }
        }

        // Death at 100%
        if (meter >= MAX) {
            player.hurt(level.damageSources().magic(), 1000.0F);
            return;
        }

        // Cumulative tiers: 1=PoisonI, 3=PoisonII+Infection (тошнота теперь proximity-based)
        var poisH = MobEffects.POISON;
        var infH  = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);

        int newTier  = meter >= 75.0F ? 3 : meter >= 50.0F ? 2 : meter >= 25.0F ? 1 : 0;
        int prevTier = LAST_TIER.getOrDefault(id, 0);

        // Tier decreased — strip effects that no longer apply
        if (newTier < prevTier) {
            if (newTier < 3) {
                player.removeEffect(infH);
                player.removeEffect(poisH); // re-add at correct amp below
            }
            if (newTier < 1) player.removeEffect(poisH);
            LAST_TIER.put(id, newTier);
        } else if (newTier > prevTier) {
            LAST_TIER.put(id, newTier);
        }

        // Apply cumulative effects
        if (newTier >= 1) {
            int amp = newTier >= 3 ? 1 : 0; // Poison II at tier 3, Poison I otherwise
            player.addEffect(new MobEffectInstance(poisH, 100, amp, false, true));
        }
        if (newTier >= 3) player.addEffect(new MobEffectInstance(infH,  100, 0, false, true));

    }
}
