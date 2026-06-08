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
    private static final float INCUBATE = 25.0F;  // INFECTION amplifier 0
    private static final float SICK = 60.0F;      // amplifier 1
    private static final float SEVERE = 90.0F;    // amplifier 2
    private static final int GRACE_SECONDS = 2;   // must stand this long before it climbs
    private static final float GAIN = 8.0F;       // meter per second while standing (post-grace)
    private static final float DECAY = 0.6F;      // slow natural recovery per second off corrupted ground

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
            meter -= DECAY;
        }
        setMeter(id, meter);
        meter = getMeter(id);

        // Apply-only escalation (never hard-remove, so direct infections from the
        // Bio-Blade / parasites / infected mobs aren't wiped a tick later).
        var holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);
        if (meter >= SEVERE) {
            player.addEffect(new MobEffectInstance(holder, 120, 2, false, true));
        } else if (meter >= SICK) {
            player.addEffect(new MobEffectInstance(holder, 120, 1, false, true));
        } else if (meter >= INCUBATE) {
            player.addEffect(new MobEffectInstance(holder, 120, 0, false, true));
        }

        if (meter >= INCUBATE) {
            String c = meter >= SEVERE ? "§4" : meter >= SICK ? "§c" : "§e";
            player.displayClientMessage(Component.literal(c + "☣ Заражение: " + (int) meter + "%"), true);
        }
    }
}
