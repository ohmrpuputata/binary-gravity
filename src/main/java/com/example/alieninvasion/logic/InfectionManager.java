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

public final class InfectionManager {
    private InfectionManager() {
    }

    public static final float MAX = 100.0F;
    private static final int   GRACE_SECONDS = 3;
    private static final float GAIN = 1.5F;

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

    public static void setMeterMultiplier(Player player, float mult) {
        if (mult == 1.0F) METER_MULT.remove(player.getUUID());
        else METER_MULT.put(player.getUUID(), mult);
    }

    public static void addMeter(Player player, float amount) {
        float mult = METER_MULT.getOrDefault(player.getUUID(), 1.0F);
        setMeter(player.getUUID(), getMeter(player) + amount * mult);
    }

    public static void capMeter(Player player, float max) {
        float cur = getMeter(player);
        if (cur > max) setMeter(player.getUUID(), max);
    }

    public static void clear(Player player) {
        UUID id = player.getUUID();
        METER.remove(id);
        STAND.remove(id);
        LAST_TIER.remove(id);
        if (player instanceof ServerPlayer sp) {
            sp.removeEffect(MobEffects.CONFUSION);
        }
    }

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
        }
        setMeter(id, meter);
        meter = getMeter(id);
        float meterDelta = meter - meterBefore;

        // Тошнота — только пока статус Заражения активно повышается (аналог помех у радиации)
        if (meterDelta > 0.0F) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        }

        if (meter >= MAX) {
            player.hurt(level.damageSources().magic(), 1000.0F);
            return;
        }

        // Тиры: 1=ЯдI, 2=ЯдI, 3=ЯдII+Заражение (тошнота убрана из тиров)
        var poisH = MobEffects.POISON;
        var infH  = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);

        int newTier  = meter >= 75.0F ? 3 : meter >= 50.0F ? 2 : meter >= 25.0F ? 1 : 0;
        int prevTier = LAST_TIER.getOrDefault(id, 0);

        if (newTier < prevTier) {
            if (newTier < 3) {
                player.removeEffect(infH);
                player.removeEffect(poisH);
            }
            if (newTier < 1) player.removeEffect(poisH);
            LAST_TIER.put(id, newTier);
        } else if (newTier > prevTier) {
            LAST_TIER.put(id, newTier);
        }

        if (newTier >= 1) {
            int amp = newTier >= 3 ? 1 : 0;
            player.addEffect(new MobEffectInstance(poisH, 100, amp, false, true));
        }
        if (newTier >= 3) player.addEffect(new MobEffectInstance(infH, 100, 0, false, true));
    }
}
