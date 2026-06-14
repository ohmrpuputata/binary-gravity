package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.logic.InfectionVisuals;
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

// Corruption block: aggressively converts surrounding natural terrain (stone,
// deepslate, dirt, sand...) into more of itself, so the world is slowly eaten
// by the infestation. Hurts and slows non-aliens that walk on it.
public class InfestedBlock extends Block {

    public InfestedBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !AlienUtils.isAlliedTo(null, living)) {
            boolean protectedSuit = living instanceof net.minecraft.world.entity.player.Player p
                    && com.example.alieninvasion.logic.ArmorProtection.hasSealedSuit(p);
            if (!protectedSuit) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
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
        if (random.nextInt(6) != 0) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            BlockState ts = level.getBlockState(target);
            if (ts.is(net.minecraft.world.level.block.Blocks.WATER)) {
                level.setBlockAndUpdate(target, ModBlocks.TOXIC_WATER.defaultBlockState());
                InfectionVisuals.spread(level, pos, target);
                continue;
            }
            // Day-gated ore corruption (coal->platinum, iron->flesh, gold->radiation...).
            BlockState ore = ContaminationRules.oreConversionFor(ts,
                    com.example.alieninvasion.logic.SurvivalManager.getDay(level));
            if (ore != null && level.getBlockEntity(target) == null) {
                level.setBlockAndUpdate(target, ore);
                InfectionVisuals.spread(level, pos, target);
                continue;
            }
            BlockState contaminated = ContaminationRules.contaminatedStateFor(ts);
            if (contaminated != null && ContaminationRules.canContaminate(level, target, ts)) {
                level.setBlockAndUpdate(target, contaminated);
                InfectionVisuals.spread(level, pos, target);
            }
        }
    }
}
