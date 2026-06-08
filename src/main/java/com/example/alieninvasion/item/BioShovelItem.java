package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// Bio-Shovel: a terraformer. Right-click to cleanse a small area of infestation
// (infested stone -> stone, alien residue -> grass), at the cost of durability.
public class BioShovelItem extends ShovelItem {
    public BioShovelItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide) {
            BlockPos center = ctx.getClickedPos();
            int converted = 0;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.INFESTED_STONE)) {
                    level.setBlockAndUpdate(p.immutable(), Blocks.STONE.defaultBlockState());
                    converted++;
                } else if (s.is(ModBlocks.ALIEN_RESIDUE)) {
                    level.setBlockAndUpdate(p.immutable(), Blocks.GRASS_BLOCK.defaultBlockState());
                    converted++;
                }
            }
            if (converted > 0) {
                ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        converted * 2, 1.2, 1.0, 1.2, 0.0);
                level.playSound(null, center, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.2F);
                Player player = ctx.getPlayer();
                if (player != null && !player.getAbilities().instabuild) {
                    ctx.getItemInHand().hurtAndBreak(converted, player, LivingEntity.getSlotForHand(ctx.getHand()));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(ctx);
    }
}
