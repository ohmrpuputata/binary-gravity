package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Зараженный крипер (Infested Creeper):
 * При взрыве разбрызгивает кислоту, покрывая землю блоками ALIEN_RESIDUE и отравляя игрока.
 */
public class InfestedCreeperEntity extends Creeper implements IAlienUnit {
    private int alienFuse = 0;
    private final int alienMaxFuse = 30;

    public InfestedCreeperEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.INFECTED; }

    @Override
    public void tick() {
        if (this.isAlive() && !this.level().isClientSide) {
            int swellDir = this.getSwellDir();
            if (swellDir > 0) {
                this.alienFuse += swellDir;
                if (this.alienFuse >= this.alienMaxFuse) {
                    this.explodeCreeperCustom();
                    return;
                }
            } else {
                this.alienFuse--;
                if (this.alienFuse < 0) {
                    this.alienFuse = 0;
                }
            }
        }
        super.tick();
    }

    private void explodeCreeperCustom() {
        if (!this.level().isClientSide) {
            float f = this.isPowered() ? 2.0F : 1.0F;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.0F * f, Level.ExplosionInteraction.MOB);
            
            // Распространяем кислотные блоки в радиусе 3 блоков
            BlockPos center = this.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
                if (pos.closerThan(center, 3.0D)) {
                    BlockState state = this.level().getBlockState(pos);
                    if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.STONE)) && this.level().getBlockState(pos.above()).isAir()) {
                        this.level().setBlockAndUpdate(pos, ModBlocks.ALIEN_RESIDUE.defaultBlockState());
                    }
                }
            }

            // Создаем облако ядовитых/заражающих спор
            AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            cloud.setRadius(4.0F);
            cloud.setRadiusOnUse(-0.5F);
            cloud.setWaitTime(10);
            cloud.setDuration(300);
            cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            cloud.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION), 600, 0));
            this.level().addFreshEntity(cloud);
            
            this.discard();
        }
    }
    
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
