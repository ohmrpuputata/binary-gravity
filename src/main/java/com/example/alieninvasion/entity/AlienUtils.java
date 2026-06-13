package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AlienUtils {
    public static boolean isAlliedTo(LivingEntity self, net.minecraft.world.entity.Entity other) {
        if (other == self)
            return true;
        // CREATIVE/SPECTATOR INVISIBILITY: the swarm simply does not perceive
        // builders - no targeting, no infested-ground bites, no parasites.
        if (other instanceof net.minecraft.world.entity.player.Player player
                && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        if (other instanceof AlienGruntEntity || other instanceof AlienBruteEntity
                || other instanceof TelekineticAlienEntity || other instanceof UfoEntity
                || other instanceof AlienChickenEntity || other instanceof HiveTyrantEntity
                || other instanceof AlienTrollEntity || other instanceof DrillEntity
                || other instanceof MeteorEntity || other instanceof ParasiteEntity
                || other instanceof AlienStalkerEntity || other instanceof PlasmaCasterEntity
                || other instanceof HiveShamanEntity || other instanceof AlienBreacherEntity
                || other instanceof SkyDroneEntity || other instanceof CaveLurkerEntity
                || other instanceof AcidSpitterEntity || other instanceof SwarmMotherEntity
                || other instanceof InfestedWormEntity || other instanceof AlienRaptorEntity) {
            return true;
        }
        return false;
    }

    /** Полноэкранный титр всем игрокам сервера — для ключевых моментов финала. */
    public static void broadcastTitle(ServerLevel level, net.minecraft.network.chat.Component title,
            net.minecraft.network.chat.Component subtitle) {
        for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    public static void spawnGoreParticles(LivingEntity entity, float damageAmount) {
        if (entity.level() instanceof ServerLevel serverLevel && damageAmount > 0) {
            // Ограничиваем число частиц: /kill и пустота наносят урон Float.MAX_VALUE,
            // из-за чего (int)damageAmount давал ~2 млрд итераций и подвешивал сервер.
            int particleCount = Math.min(12, (int) Math.max(1, Math.min(damageAmount, 12.0F)));
            for (int i = 0; i < particleCount; i++) {
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BEEF)),
                        entity.getX(), entity.getY() + entity.getEyeHeight() * 0.5, entity.getZ(),
                        1, 0.2, 0.2, 0.2, 0.1);
                serverLevel.sendParticles(
                        new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        entity.getX(), entity.getY() + entity.getEyeHeight() * 0.5, entity.getZ(),
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }
}
