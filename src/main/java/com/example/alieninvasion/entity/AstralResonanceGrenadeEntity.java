package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class AstralResonanceGrenadeEntity extends ThrowableItemProjectile {
    public AstralResonanceGrenadeEntity(EntityType<? extends AstralResonanceGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public AstralResonanceGrenadeEntity(Level level, LivingEntity shooter) {
        super(EntityRegistry.ASTRAL_RESONANCE_GRENADE, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistry.ASTRAL_RESONANCE_GRENADE;
    }

    @Override
    protected double getDefaultGravity() {
        // Flat/straight trajectory: 0.012D instead of default 0.03D
        return 0.012D;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            detonate();
            this.discard();
        }
    }

    private void detonate() {
        ServerLevel sl = (ServerLevel) this.level();
        double radius = 8.0D;
        
        java.util.List<Entity> affected = sl.getEntities((Entity) null, this.getBoundingBox().inflate(radius));
        for (Entity target : affected) {
            if (target.distanceToSqr(this) <= radius * radius) {
                if (target instanceof UfoEntity || target instanceof DrillEntity) {
                    // Instantly destroy UFO or Drill
                    target.discard();
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 0.5D, target.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                } else if (target instanceof Player player) {
                    // EMP effect on player
                    player.addTag("EmpActive");
                    com.example.alieninvasion.events.ModEvents.PLAYER_EMP_TICKS.put(player.getUUID(), 160);
                }
            }
        }

        // Visuals and sounds
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 100, 1.5, 1.5, 1.5, 0.5);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 10, 1.0, 1.0, 1.0, 0.2);
        
        // Explode like a TNT
        sl.explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, false, Level.ExplosionInteraction.TNT);

        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 1.0F);
        sl.playSound(null, this.blockPosition(), SoundEvents.GILDED_BLACKSTONE_BREAK, SoundSource.PLAYERS, 2.0F, 0.4F);
    }
}
