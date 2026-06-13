package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;

public class PureRadiationBlock extends Block {
    public PureRadiationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (player.isCreative()) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            return super.getDestroyProgress(state, player, world, pos);
        }
        // Mining speed is extremely slow if not using Nibirium Pickaxe (about 30x slower, like mining obsidian with a fist)
        return super.getDestroyProgress(state, player, world, pos) * 0.03F;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @org.jetbrains.annotations.Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && tool.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            // Drop 1-2 radiation crystals, with fortune bonus
            int baseCount = 1 + level.getRandom().nextInt(2); // 1-2
            int fortuneLevel = 0;
            var fortuneHolder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(Enchantments.FORTUNE);
            if (fortuneHolder.isPresent()) {
                fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(fortuneHolder.get(), tool);
            }
            if (fortuneLevel > 0) {
                int bonus = level.getRandom().nextInt(fortuneLevel + 1);
                baseCount += bonus;
            }
            Block.popResource(level, pos, new ItemStack(ItemRegistry.RADIATION_CRYSTAL, baseCount));
        }
    }
}

