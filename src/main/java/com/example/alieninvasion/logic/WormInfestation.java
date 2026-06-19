package com.example.alieninvasion.logic;

import com.example.alieninvasion.block.BloodyBlocks;
import com.example.alieninvasion.entity.InfestedWormEntity;
import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModAttachments;
import com.example.alieninvasion.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Червь залезает в МИРНОГО моба и вынашивается. Носитель внешне НИКАК НЕ отличается
 * от обычного моба — ни имени, ни свечения, ни фиолетового ихора: заражение скрытно,
 * носитель неотличим, пока его не разорвёт изнутри. Чем дольше живёт — тем КРУПНЕЕ
 * вылезет червь. Когда носитель погибает (созрел или убит), он становится ТРУПОМ и
 * червь ВЫЛЕЗАЕТ ИЗ ТРУПА по ходу лежания (см. corpse-микшин), а не мгновенно.
 */
public final class WormInfestation {
    public static final String HOST_TAG = "WormHost";
    private static final int BURST_TICKS = 600;   // ~30 c — носитель «созревает» и гибнет
    private static final int STAGE_TIME = 300;    // дольше носил — крупнее червь

    private WormInfestation() {
    }

    /** Червь забрался в мирного моба: тихо метим тегом и заводим срок — БЕЗ видимых меток. */
    public static void infest(PathfinderMob host) {
        if (host.getTags().contains(HOST_TAG)) {
            return;
        }
        host.addTag(HOST_TAG);
        host.setAttached(ModAttachments.WORM_GESTATION, 0);
        // Носитель НИКАК не помечается: ни зелёного имени, ни свечения, ни фиолетового
        // ихора — внешне это обычный мирный моб. Заражение лишь слышно (червь вгрызается).
        if (host.level() instanceof ServerLevel sl) {
            sl.playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
    }

    /** Каждый тик у живого носителя: растёт срок вынашивания; созрев — носителя разрывает. */
    public static void tickHost(ServerLevel level, LivingEntity host) {
        if (!(host instanceof PathfinderMob) || !host.getTags().contains(HOST_TAG)) {
            return;
        }
        int g = host.getAttachedOrElse(ModAttachments.WORM_GESTATION, 0) + 1;
        host.setAttached(ModAttachments.WORM_GESTATION, g);
        // Никаких частиц/меток/свечения за время вынашивания — носитель остаётся
        // визуально неотличим от обычного моба, пока его не разорвёт изнутри.
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
