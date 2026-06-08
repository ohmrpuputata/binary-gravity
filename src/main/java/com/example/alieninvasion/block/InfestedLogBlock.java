package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.ContaminationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class InfestedLogBlock extends RotatedPillarBlock {
    public InfestedLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !AlienUtils.isAlliedTo(null, living)) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) return;
        for (int i = 0; i < 3; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            BlockState replacement = ContaminationRules.contaminatedStateFor(level.getBlockState(target));
            if (replacement != null && ContaminationRules.canContaminate(level, target, level.getBlockState(target))) {
                level.setBlockAndUpdate(target, replacement);
            }
        }
    }
}
