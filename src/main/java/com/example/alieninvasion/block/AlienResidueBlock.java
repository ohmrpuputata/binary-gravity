package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class AlienResidueBlock extends Block {

    public AlienResidueBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (!AlienUtils.isAlliedTo(null, living)) { // Damage non-aliens
                living.hurt(level.damageSources().magic(), 1.0F);
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1)); // Slowness II for 5s
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Spread logic
        if (random.nextInt(10) == 0) { // 10% chance to spread
            for (int i = 0; i < 4; i++) {
                BlockPos targetPos = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.is(net.minecraft.world.level.block.Blocks.WATER)) {
                    level.setBlockAndUpdate(targetPos, ModBlocks.TOXIC_WATER.defaultBlockState());
                    continue;
                }
                BlockState contaminated = ContaminationRules.contaminatedStateFor(targetState);
                if (contaminated != null && ContaminationRules.canContaminate(level, targetPos, targetState)) {
                    level.setBlockAndUpdate(targetPos, contaminated);
                }
            }
        }
    }
}
