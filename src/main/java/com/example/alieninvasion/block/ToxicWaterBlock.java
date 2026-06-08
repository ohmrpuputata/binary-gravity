package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ItemRegistry;
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
import net.minecraft.world.entity.EquipmentSlot;
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
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.HAZMAT_HELMET)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.HAZMAT_CHESTPLATE)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.HAZMAT_LEGGINGS)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.HAZMAT_BOOTS);
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
                level.setBlockAndUpdate(target, ModBlocks.DEAD_INFESTED_CROP.defaultBlockState());
            } else if (random.nextInt(10) == 0 && ContaminationRules.canContaminate(level, target, targetState)) {
                BlockState replacement = ContaminationRules.contaminatedStateFor(targetState);
                if (replacement != null) {
                    level.setBlockAndUpdate(target, replacement);
                }
            }
        }
    }
}
