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

public class EmpGrenadeEntity extends ThrowableItemProjectile {
    public EmpGrenadeEntity(EntityType<? extends EmpGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public EmpGrenadeEntity(Level level, LivingEntity shooter) {
        super(EntityRegistry.EMP_GRENADE, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistry.EMP_GRENADE;
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
                if (target instanceof UfoEntity ufo) {
                    ufo.addTag("EmpActive");
                    ufo.setEmpTicks(160);
                } else if (target instanceof DrillEntity drill) {
                    drill.addTag("EmpActive");
                    drill.setEmpTicks(160);
                } else if (target instanceof Player player) {
                    player.addTag("EmpActive");
                    com.example.alieninvasion.events.ModEvents.PLAYER_EMP_TICKS.put(player.getUUID(), 160);
                }
            }
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 80, 1.5, 1.5, 1.5, 0.4);
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 1.5F);
        sl.playSound(null, this.blockPosition(), SoundEvents.GILDED_BLACKSTONE_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
    }
}
