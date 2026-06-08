package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Космический Молот (Cosmic Warhammer):
 * Тяжелое двуручное оружие из Космических Блоков.
 * Правый клик бьет по земле, создавая ударную волну (с фиолетовыми частицами взрыва),
 * которая раскидывает всех врагов в радиусе 5 блоков и подбрасывает их вверх.
 */
public class CosmicWarhammerItem extends SwordItem {
    public CosmicWarhammerItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Бьем по земле ударной волной
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
        
        if (!level.isClientSide) {
            ServerLevel sl = (ServerLevel) level;
            double radius = 6.0D;

            // Фиолетовый взрыв: используем Dragon Breath и Explosion частицы
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 0.1D, player.getZ(), 60, 2.0D, 0.1D, 2.0D, 0.1D);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.1D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

            AABB area = player.getBoundingBox().inflate(radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                if (player.distanceToSqr(entity) <= radius * radius) {
                    // Раскидываем и подбрасываем
                    Vec3 pushVec = entity.position().subtract(player.position());
                    if (pushVec.lengthSqr() > 0.01D) {
                        pushVec = pushVec.normalize().scale(1.8D);
                    } else {
                        pushVec = new Vec3(0, 0, 0);
                    }
                    entity.setDeltaMovement(pushVec.x, 1.1D, pushVec.z);
                    entity.hurtMarked = true;
                    // Наносим мощный урон ударной волной
                    entity.hurt(level.damageSources().playerAttack(player), 12.0F);
                }
            }

            // Отдача дает носителю короткую защиту - "якорь" в гуще боя.
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true));
        }

        player.getCooldowns().addCooldown(this, 40); // 2 секунды перезарядки
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // Космический молот всегда мерцает звездной энергией.
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
