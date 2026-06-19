package com.example.alieninvasion.logic;

import com.example.alieninvasion.block.BloodyBlocks;
import com.example.alieninvasion.entity.InfestedWormEntity;
import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModAttachments;
import com.example.alieninvasion.registry.ModEffects;
import com.example.alieninvasion.registry.ModParticles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Червь залезает в МИРНОГО моба и вынашивается. Носитель ЯВНО заражён: зелёное имя,
 * свечение (видно сквозь стены), фиолетовые споры. Чем дольше живёт — тем КРУПНЕЕ
 * вылезет червь. Когда носитель погибает (созрел или убит), он становится ТРУПОМ и
 * червь ВЫЛЕЗАЕТ ИЗ ТРУПА по ходу лежания (см. corpse-микшин), а не мгновенно.
 */
public final class WormInfestation {
    public static final String HOST_TAG = "WormHost";
    private static final int BURST_TICKS = 600;   // ~30 c — носитель «созревает» и гибнет
    private static final int STAGE_TIME = 300;    // дольше носил — крупнее червь

    private WormInfestation() {
    }

    /** Червь забрался в мирного моба: метим, делаем ЯВНО заражённым, заводим срок. */
    public static void infest(PathfinderMob host) {
        if (host.getTags().contains(HOST_TAG)) {
            return;
        }
        host.addTag(HOST_TAG);
        host.setAttached(ModAttachments.WORM_GESTATION, 0);
        applyMarks(host);
        if (host.level() instanceof ServerLevel sl) {
            sl.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + host.getBbHeight() * 0.5D,
                    host.getZ(), 14, 0.25D, 0.25D, 0.25D, 0.04D);
            sl.playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
    }

    /** Зелёное имя + свечение + ихор в крови — чтобы заражённый моб было ВИДНО. */
    private static void applyMarks(PathfinderMob host) {
        host.setCustomName(Component.literal("§2Заражённый " + host.getType().getDescription().getString()));
        host.setCustomNameVisible(true);
        int dur = 120;
        host.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
        host.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0, false, false));
        host.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION),
                dur, 0, false, false));
    }

    /** Каждый тик у живого носителя: растёт срок, ПОДНОВЛЯЮТСЯ метки, споры; созрев — гибнет. */
    public static void tickHost(ServerLevel level, LivingEntity host) {
        if (!(host instanceof PathfinderMob mob) || !host.getTags().contains(HOST_TAG)) {
            return;
        }
        int g = host.getAttachedOrElse(ModAttachments.WORM_GESTATION, 0) + 1;
        host.setAttached(ModAttachments.WORM_GESTATION, g);
        if (g % 40 == 0) {
            applyMarks(mob); // подновляем — свечение/имя не должны пропадать
        }
        if (g % 8 == 0) {
            level.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + host.getBbHeight() * 0.6D,
                    host.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        }
        if (g >= BURST_TICKS) {
            // Созрел — носителя разрывает изнутри. САМ червь вылезет уже ИЗ ТРУПА.
            host.hurt(level.damageSources().magic(), host.getMaxHealth() * 2.0F + 10.0F);
        }
    }

    /** Червь ВЫЛЕЗАЕТ ИЗ ТРУПА — зовётся из corpse-микшина по ходу лежания, не мгновенно. */
    public static void emergeFromCorpse(ServerLevel level, LivingEntity host) {
        if (!host.getTags().contains(HOST_TAG)) {
            return;
        }
        host.removeTag(HOST_TAG);
        int gestation = host.getAttachedOrElse(ModAttachments.WORM_GESTATION, 0);
        int stage = Math.max(0, Math.min(2, gestation / STAGE_TIME));
        InfestedWormEntity worm = EntityRegistry.INFESTED_WORM.create(level);
        if (worm != null) {
            worm.moveTo(host.getX(), host.getY() + 0.1D, host.getZ(), host.getYRot(), 0.0F);
            worm.setStage(stage);
            level.addFreshEntity(worm);
        }
        level.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + 0.4D, host.getZ(),
                40, 0.35D, 0.4D, 0.35D, 0.12D);
        BloodyBlocks.splatter(level, host.blockPosition(), true);
        level.playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.1F, 0.45F);
    }
}
