package com.example.alieninvasion.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

// Пещерный охотник: стремительный паукообразный засадник подземелий. Лазает по
// стенам, очень быстрый - под землёй тоже не спрятаться. На базе Паука.
public class CaveLurkerEntity extends Spider implements IAlienUnit {
    public CaveLurkerEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.LURKER; }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)   // much faster than a spider
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            AlienUtils.spawnGoreParticles(this, amount);
            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                    && !AlienUtils.isAlliedTo(this, attacker)) {
                this.setTarget(attacker);
            }
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
