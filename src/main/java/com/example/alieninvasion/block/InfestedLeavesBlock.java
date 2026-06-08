package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.ContaminationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class InfestedLeavesBlock extends Block {
    public InfestedLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !AlienUtils.isAlliedTo(null, living)) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 60, 0));
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Day 4+: the corrupted canopy dies and the dead leaves fall away, leaving
        // bare petrified trunks (the "trees die / leaves drop" stage of the patch).
        if (com.example.alieninvasion.logic.SurvivalManager.getDay(level) >= 4 && random.nextInt(8) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        if (random.nextInt(10) != 0) return;
        BlockPos target = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
        BlockState targetState = level.getBlockState(target);
        BlockState replacement = ContaminationRules.contaminatedStateFor(targetState);
        if (replacement != null && ContaminationRules.canContaminate(level, target, targetState)) {
            level.setBlockAndUpdate(target, replacement);
        }
    }
}
