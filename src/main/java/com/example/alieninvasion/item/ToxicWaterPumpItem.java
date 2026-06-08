package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ToxicWaterPumpItem extends Item {
    public ToxicWaterPumpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (!state.is(ModBlocks.TOXIC_WATER) || player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            ItemStack bucket = new ItemStack(ItemRegistry.TOXIC_WATER_BUCKET);
            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 0.65F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
