package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CosmicCrystalBlock extends Block {
    public CosmicCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            AABB area = new AABB(pos).inflate(5.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, entity)) {
                    entity.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION), 100, 0, false, true
                    ));
                }
            }
            level.scheduleTick(pos, this, 20);
        }
    }
}
