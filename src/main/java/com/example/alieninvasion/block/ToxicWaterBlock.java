package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class ToxicWaterBlock extends LiquidBlock {
    public ToxicWaterBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (hasFullHazmat(living)) {
                living.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
                super.entityInside(state, level, pos, entity);
                return;
            }
            boolean vulnerable = living instanceof Player || living instanceof Animal || living instanceof Villager;
            if (vulnerable) {
                living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION),
                        140, 0, false, true));
                living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80, 0, false, true));
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    private boolean hasFullHazmat(LivingEntity entity) {
        return com.example.alieninvasion.logic.ArmorProtection.hasSealedSuit(entity);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5D, pos.getY() + 0.8D,
                    pos.getZ() + 0.5D, 1, 0.3D, 0.1D, 0.3D, 0.0D);
        }
        for (Direction direction : Direction.values()) {
            BlockPos target = pos.relative(direction);
            BlockState targetState = level.getBlockState(target);
            if (targetState.is(BlockTags.CROPS) || targetState.is(BlockTags.SAPLINGS)) {
                level.setBlockAndUpdate(target, ModBlocks.ALIEN_TENDRILS.defaultBlockState());
            } else if (random.nextInt(24) == 0 && ContaminationRules.canContaminate(level, target, targetState)) {
                BlockState replacement = ContaminationRules.contaminatedStateFor(targetState);
                if (replacement != null) {
                    level.setBlockAndUpdate(target, replacement);
                }
            }
        }
        // DOWNWARD ROT: toxin sinks - the column under a toxic surface converts
        // with no depth limit, so pools end up toxic top to bottom.
        BlockState below = level.getBlockState(pos.below());
        if (below.is(net.minecraft.world.level.block.Blocks.WATER) && below.getFluidState().isSource()) {
            level.setBlockAndUpdate(pos.below(), ModBlocks.TOXIC_WATER.defaultBlockState());
        }
        // Creeping water corruption: the toxin slowly eats adjacent clean water,
        // but only along shallows (a bottom within 3 blocks), so it crawls along
        // shores and ponds instead of swallowing whole oceans.
        if (random.nextInt(8) == 0) {
            Direction dir = Direction.getRandom(random);
            if (dir == Direction.UP) dir = Direction.DOWN;
            BlockPos target = pos.relative(dir);
            BlockState targetState = level.getBlockState(target);
            if (targetState.is(net.minecraft.world.level.block.Blocks.WATER)
                    && targetState.getFluidState().isSource() && isShallow(level, target)) {
                level.setBlockAndUpdate(target, ModBlocks.TOXIC_WATER.defaultBlockState());
            }
        }
    }

    static boolean isShallow(ServerLevel level, BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            if (level.getBlockState(pos.below(i)).getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
