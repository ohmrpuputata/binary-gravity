package com.example.alieninvasion.logic;

import com.example.alieninvasion.block.BloodyBlocks;
import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.ModEffects;
import com.example.alieninvasion.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Реалистичное кровотечение через РАНУ, а не порог HP.
 *
 * Режущий/физический урон открывает рану с таймером и интенсивностью. Пока рана
 * открыта — существо капает кровью и оставляет лужи/след; рана сама сворачивается
 * (быстрее, если существо лечится). Низкое HP само по себе НЕ кровит. Чисто визуал
 * (HP не снимает). Цвет крови: красный, у заражённых/пришельцев — фиолетовый ихор.
 * Скелеты (костяная нежить) не кровят.
 *
 * Хранилище — WeakHashMap: записи живут только у кровоточащих и сами исчезают,
 * когда существо выгружено (GC) или рана свернулась.
 */
public final class BleedManager {
    // value = {ticksLeft, intensity(0..100), purple(0/1)}
    private static final Map<LivingEntity, int[]> WOUNDS = new WeakHashMap<>();

    private BleedManager() {
    }

    /** Открыть/усилить рану от урона (если это режущий урон и существо кровит). */
    public static void wound(LivingEntity e, DamageSource src, float amount) {
        if (!canBleed(e) || !isBleedingDamage(src) || amount <= 0.0F) {
            return;
        }
        int ticks = Math.min(400, 100 + (int) (amount * 22));      // ~5с базы, дольше от урона, до 20с
        int intensity = (int) Math.min(100.0F, 45.0F + amount * 14.0F);
        int purple = isInfectedBlood(e) ? 1 : 0;
        int[] w = WOUNDS.get(e);
        if (w == null) {
            WOUNDS.put(e, new int[]{ticks, intensity, purple});
        } else {
            // Повторные/сильные раны ПРОДЛЕВАЮТ кровотечение (накопление, с потолком).
            w[0] = Math.min(400, w[0] + ticks);
            w[1] = Math.max(w[1], intensity);
            w[2] = purple;
        }
    }

    public static boolean isBleeding(LivingEntity e) {
        int[] w = WOUNDS.get(e);
        return w != null && w[0] > 0;
    }

    /** Тик кровотечения: капли/лужи нужного цвета, спад таймера (быстрее при лечении). */
    public static void tick(ServerLevel level, LivingEntity e) {
        int[] w = WOUNDS.get(e);
        if (w == null) {
            return;
        }
        if (!e.isAlive() || w[0] <= 0) {
            WOUNDS.remove(e);
            clearBleedHud(e);
            return;
        }
        // Свёртывание: обычный спад; быстрее, если существо регенерирует (лечится).
        w[0] -= e.hasEffect(MobEffects.REGENERATION) ? 4 : 1;
        // Игроку синкаем интенсивность — клиент рисует красную виньетку (свою кровь
        // в виде-от-первого-лица иначе не видно).
        if (e instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.setAttached(com.example.alieninvasion.registry.ModAttachments.BLEEDING, w[1]);
        }

        boolean purple = w[2] == 1;
        boolean heavy = w[1] >= 60;
        SimpleParticleType particle = purple ? ModParticles.BLOOD_PURPLE : ModParticles.BLOOD;
        if (e.tickCount % (heavy ? 3 : 6) == 0) {
            level.sendParticles(particle, e.getX(), e.getY() + 0.2D, e.getZ(),
                    heavy ? 9 : 5, 0.16D, 0.06D, 0.16D, 0.03D);
        }
        boolean moving = e.getDeltaMovement().horizontalDistanceSqr() > 0.002D;
        if (e.tickCount % (heavy ? 8 : 16) == 0 && (moving || heavy)) {
            BloodyBlocks.splatter(level, e.blockPosition().below(), purple);
        }
        if (w[0] <= 0) {
            WOUNDS.remove(e);
            clearBleedHud(e);
        }
    }

    private static void clearBleedHud(LivingEntity e) {
        if (e instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.setAttached(com.example.alieninvasion.registry.ModAttachments.BLEEDING, 0);
        }
    }

    /** Костяная нежить (скелеты) и конструкты (железный/снежный голем) не кровоточат. */
    public static boolean canBleed(LivingEntity e) {
        return !(e instanceof AbstractSkeleton)
                && !(e instanceof net.minecraft.world.entity.animal.AbstractGolem);
    }

    /**
     * Фиолетовым ихором кровят только пришельцы (и заражённый ИГРОК). Заражённые
     * ванильные мобы внешне НЕ отличаются от обычных — кровь у них обычная, красная,
     * несмотря на эффект заражения.
     */
    public static boolean isInfectedBlood(LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.player.Player) {
            return e.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
        }
        return AlienUtils.isAlliedTo(null, e);
    }

    /** Кровь идёт только от режущего/физического урона, не от огня/яда/магии/утопления. */
    public static boolean isBleedingDamage(DamageSource s) {
        return !(s.is(DamageTypeTags.IS_FIRE)
                || s.is(DamageTypeTags.IS_FREEZING)
                || s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)
                || s.is(DamageTypes.WITHER) || s.is(DamageTypes.WITHER_SKULL)
                || s.is(DamageTypes.DROWN) || s.is(DamageTypes.IN_WALL)
                || s.is(DamageTypes.STARVE) || s.is(DamageTypes.FLY_INTO_WALL)
                || s.is(DamageTypes.DRAGON_BREATH) || s.is(DamageTypes.SONIC_BOOM)
                || s.is(DamageTypes.FELL_OUT_OF_WORLD) || s.is(DamageTypes.OUTSIDE_BORDER));
    }
}
