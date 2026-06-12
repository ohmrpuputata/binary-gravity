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
    // Укусы НЕ убивают сами по себе: тычки толпы загоняют шкалу максимум до 90%,
    // а смертельный взрыв на 100% даёт только среда (земля, вода, кристаллы).
    private static final float BITE_CAP = 90.0F;
    // Между «заразными» укусами — короткая неуязвимость шкалы (аналог ванильных
    // i-frames): восемь мобов, ударивших за секунду, = 1-2 укуса, а не шот до 100.
    private static final int BITE_COOLDOWN_TICKS = 10;

    private static final Map<UUID, Float>   METER      = new ConcurrentHashMap<>();
    private static final Map<UUID, Float>   METER_MULT = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STAND      = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_TIER  = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> WORM_TIMER   = new ConcurrentHashMap<>();
    private static final Map<UUID, Long>    LAST_BITE  = new ConcurrentHashMap<>();

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

    /** Заражение от укусов/тычек мобов — с откатом и потолком 90% (см. BITE_CAP). */
    public static void addMeterFromBite(Player player, float amount) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        long last = LAST_BITE.getOrDefault(id, -1000L);
        if (now - last < BITE_COOLDOWN_TICKS) {
            return;
        }
        float meter = getMeter(id);
        if (meter >= BITE_CAP) {
            return;
        }
        LAST_BITE.put(id, now);
        float mult = METER_MULT.getOrDefault(id, 1.0F);
        setMeter(id, Math.min(BITE_CAP, meter + amount * mult));
    }

    public static void capMeter(Player player, float max) {
        float cur = getMeter(player);
        if (cur > max) setMeter(player.getUUID(), max);
    }

    public static void clear(Player player) {
        UUID id = player.getUUID();
        METER.remove(id);
        METER_MULT.remove(id);
        STAND.remove(id);
        LAST_TIER.remove(id);
        WORM_TIMER.remove(id);
        LAST_BITE.remove(id);
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

        // Swimming IN infected water soaks the infection straight through the
        // skin - several times faster than standing on rotten ground. Get out.
        if (player.level().getBlockState(player.blockPosition())
                .is(com.example.alieninvasion.registry.ModBlocks.INFECTED_WATER)) {
            setMeter(id, getMeter(id) + 3.0F);
            meter = getMeter(id);
            player.displayClientMessage(Component.literal("§5☣ Заражённая вода проникает в кожу!"), true);
        }

        // NOTE: no continuous nausea here. Constant screen-warp made the game
        // unplayable; the only nausea left is a short burst on stage-up below.

        if (meter >= MAX) {
            player.hurt(level.damageSources().magic(), 1000.0F);
            return;
        }

        var poisH = MobEffects.POISON;
        var infH  = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);

        // STAGES: 1 (25%) malaise -> 2 (50%) fever + the mutation whisper ->
        // 3 (75%) the worms start crawling out -> 100% the host bursts.
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
            // One SHORT dizzy spell when the disease reaches a new stage - a
            // readable "it got worse" cue, never a permanent effect.
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            if (newTier == 2) {
                player.displayClientMessage(Component.literal(
                        "§5Лихорадка усиливается. Лечитесь, пока черви не проснулись."), false);
            } else if (newTier == 3) {
                player.displayClientMessage(Component.literal(
                        "§4Под кожей шевелятся черви..."), false);
            }
        }

        if (newTier >= 1) {
            int amp = newTier >= 3 ? 1 : 0;
            player.addEffect(new MobEffectInstance(poisH, 100, amp, false, true));
        }
        if (newTier >= 2) {
            // Fever: the body burns through food fighting the parasites. Tremors
            // are rare and brief - pressure, not punishment.
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0, false, false));
            if (level.random.nextInt(15) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false));
            }
        }
        if (newTier >= 3) {
            player.addEffect(new MobEffectInstance(infH, 100, 0, false, true));
            // Worms tear their way OUT of the host every ~12 seconds.
            // CAP: no more than 4 live worms around the host - without the cap a
            // player stuck on tier 3 bred an endless worm army (lag + unwinnable).
            int t = WORM_TIMER.merge(id, 1, Integer::sum);
            if (t >= 12) {
                WORM_TIMER.remove(id);
                int nearbyWorms = level.getEntitiesOfClass(
                        com.example.alieninvasion.entity.InfestedWormEntity.class,
                        player.getBoundingBox().inflate(24.0D),
                        w -> w.isAlive()).size();
                com.example.alieninvasion.entity.InfestedWormEntity worm = nearbyWorms < 4
                        ? com.example.alieninvasion.registry.EntityRegistry.INFESTED_WORM.create(level)
                        : null;
                if (worm != null) {
                    worm.moveTo(player.getX(), player.getY() + 0.3D, player.getZ(),
                            level.random.nextFloat() * 360.0F, 0.0F);
                    worm.setStage(0);
                    level.addFreshEntity(worm);
                    player.hurt(level.damageSources().magic(), 2.0F);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0D,
                            player.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.1D);
                    level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK,
                            SoundSource.PLAYERS, 0.9F, 0.5F);
                    player.displayClientMessage(Component.literal("§4Червь прогрыз себе выход!"), true);
                }
            }
        } else {
            WORM_TIMER.remove(id);
        }
    }
}
