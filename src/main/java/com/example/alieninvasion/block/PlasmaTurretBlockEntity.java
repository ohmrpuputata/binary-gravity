package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.events.ModEvents;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Плазменная турель (Block Entity):
 * Логика работы турели на сервере. Сканирует область, находит ближайшего пришельца,
 * расстреливает его плазменными лазерами и расходует заряд батареи.
 * Во время ЭМП-бури турель отключается.
 */
public class PlasmaTurretBlockEntity extends BlockEntity {
    private int charge = 0; // Заряд от 0 до 100%

    public PlasmaTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PLASMA_TURRET_BLOCK_ENTITY, pos, state);
    }

    public int getCharge() {
        return this.charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
        this.setChanged();
    }

    public void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        // Если ЭМП-буря активна или нет заряда, турель отключается
        if (ModEvents.empTicksActive > 0 || this.charge <= 0) {
            if (level.getGameTime() % 80 == 0 && this.charge > 0) {
                // Если буря активна, спавним искры неисправности
                ServerLevel sl = (ServerLevel) level;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D, worldPosition.getZ() + 0.5D, 3, 0.1, 0.1, 0.1, 0.01);
            }
            return;
        }

        // Турель стреляет каждые 15 тиков (0.75 сек)
        if (level.getGameTime() % 15 == 0) {
            LivingEntity target = findNearestAlien();
            if (target != null) {
                shoot(target);
            }
        }
    }

    private LivingEntity findNearestAlien() {
        double radius = 20.0D;
        AABB area = new AABB(worldPosition).inflate(radius);
        LivingEntity closest = null;
        double closestDistSq = radius * radius;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && AlienUtils.isAlliedTo(null, e))) {
            double distSq = entity.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
            if (distSq < closestDistSq) {
                closest = entity;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    private void shoot(LivingEntity target) {
        if (level == null || level.isClientSide) return;
        ServerLevel sl = (ServerLevel) level;

        Vec3 start = Vec3.atCenterOf(worldPosition).add(0.0D, 0.5D, 0.0D);
        Vec3 end = target.position().add(0.0D, target.getEyeHeight() * 0.75D, 0.0D);
        double distance = start.distanceTo(end);

        // Рисуем луч плазмы из мелких частиц огня
        Vec3 pathVec = end.subtract(start);
        for (int i = 0; i < (int)(distance * 2); i++) {
            Vec3 point = start.add(pathVec.scale(i / (distance * 2.0D)));
            sl.sendParticles(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }

        // Звук выстрела
        level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 1.5F);

        // Наносим урон пришельцу
        target.hurt(level.damageSources().magic(), 6.0F);

        // Расходуем заряд турели
        this.charge--;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Charge", this.charge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.charge = tag.getInt("Charge");
    }
}
