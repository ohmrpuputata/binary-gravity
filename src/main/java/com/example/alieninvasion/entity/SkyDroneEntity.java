package com.example.alieninvasion.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

// Дрон Роя: лёгкий летающий перехватчик. Пикирует на игроков (в т.ч. в воздухе) -
// в небе больше не спрятаться. На базе Фантома.
public class SkyDroneEntity extends Phantom {
    public SkyDroneEntity(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected boolean isSunBurnTick() {
        return false; // alien drones don't burn by day
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && amount > 0) {
            AlienUtils.spawnGoreParticles(this, amount);
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
