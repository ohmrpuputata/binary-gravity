package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public abstract class AlienEntity extends Monster {
    protected AlienEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    // prevent sunlight burning (Zombie gene removed)
    // prevent water damage (Enderman gene removed)

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        if (entity == this) {
            return true;
        }
        // Aliens are friends with other Aliens
        if (entity instanceof AlienEntity) {
            return true;
        }
        // Also friends with UFOs
        if (entity instanceof UfoEntity) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    // Custom Gore System
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && amount > 0) {
            spawnGoreParticles(amount);
        }
        return result;
    }

    private void spawnGoreParticles(float damageAmount) {
        if (this.level() instanceof ServerLevel serverLevel) {
            int particleCount = (int) Math.max(1, damageAmount); // More damage = more chunks

            // Spawn "Meat Chunks" (Rotten Flesh or Beef particles)
            for (int i = 0; i < particleCount; i++) {
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BEEF)),
                        this.getX(), this.getY() + this.getEyeHeight() * 0.5, this.getZ(),
                        1, // count per packet
                        0.2, 0.2, 0.2, // spread
                        0.1 // speed
                );

                // Add some "Blood" (Redstone Block particles) for variety
                serverLevel.sendParticles(
                        new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        this.getX(), this.getY() + this.getEyeHeight() * 0.5, this.getZ(),
                        2,
                        0.1, 0.1, 0.1,
                        0.05);
            }
        }
    }
}
