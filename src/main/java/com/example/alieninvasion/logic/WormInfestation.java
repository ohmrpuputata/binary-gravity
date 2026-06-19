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
 * Червь залезает в МИРНОГО моба и вынашивается в нём. Пока носитель жив, срок растёт;
 * чем дольше — тем КРУПНЕЕ червь вылезет (до «великого»). По созреванию носителя
 * разрывает изнутри: брызги ихора, новый червь, труп. Убей заражённое животное
 * раньше — и большой червь не родится.
 */
public final class WormInfestation {
    public static final String HOST_TAG = "WormHost";
    private static final int BURST_TICKS = 600;   // ~30 c до созревания «великого»
    private static final int STAGE_TIME = 300;    // каждые ~15 c — на стадию крупнее

    private WormInfestation() {
    }

    /** Червь забрался в мирного моба: метим носителя, заводим срок, делаем больным. */
    public static void infest(PathfinderMob host) {
        if (host.getTags().contains(HOST_TAG)) {
            return;
        }
        host.addTag(HOST_TAG);
        host.setAttached(ModAttachments.WORM_GESTATION, 0);
        host.setCustomName(Component.literal("§2Заражённый " + host.getType().getDescription().getString()));
        host.setCustomNameVisible(true);
        int dur = BURST_TICKS + 200;
        host.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0, false, false));
        host.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
        host.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION),
                dur, 0, false, false)); // чтобы кровь носителя была фиолетовой
        if (host.level() instanceof ServerLevel sl) {
            sl.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + host.getBbHeight() * 0.5D,
                    host.getZ(), 12, 0.25D, 0.25D, 0.25D, 0.04D);
            sl.playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
    }

    /** Каждый тик у живого носителя: растим срок, тошнотворные споры, по созреванию — разрыв. */
    public static void tickHost(ServerLevel level, LivingEntity host) {
        if (!(host instanceof PathfinderMob) || !host.getTags().contains(HOST_TAG)) {
            return;
        }
        int g = host.getAttachedOrElse(ModAttachments.WORM_GESTATION, 0) + 1;
        host.setAttached(ModAttachments.WORM_GESTATION, g);
        if (g % 10 == 0) {
            level.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + host.getBbHeight() * 0.6D,
                    host.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        }
        if (g >= BURST_TICKS) {
            burst(level, host, g);
        }
    }

    private static void burst(ServerLevel level, LivingEntity host, int gestation) {
        int stage = Math.max(0, Math.min(2, gestation / STAGE_TIME)); // дольше носил — крупнее червь
        InfestedWormEntity worm = EntityRegistry.INFESTED_WORM.create(level);
        if (worm != null) {
            worm.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), 0.0F);
            worm.setStage(stage);
            level.addFreshEntity(worm);
        }
        level.sendParticles(ModParticles.BLOOD_PURPLE, host.getX(), host.getY() + 0.5D, host.getZ(),
                36, 0.35D, 0.4D, 0.35D, 0.12D);
        BloodyBlocks.splatter(level, host.blockPosition(), true);
        level.playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.1F, 0.5F);
        host.removeTag(HOST_TAG);
        // Носителя разрывает изнутри — труп ляжет и истечёт кровью (см. corpse-микшин).
        host.hurt(level.damageSources().magic(), host.getMaxHealth() * 2.0F + 10.0F);
    }
}
