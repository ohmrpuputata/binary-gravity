package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.InfectionManager;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public class DarkMatterOreBlock extends Block {
    public DarkMatterOreBlock(BlockBehaviour.Properties properties) {
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
        // Mining speed is extremely slow if not using Nibirium Pickaxe
        return super.getDestroyProgress(state, player, world, pos) * 0.03F;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @org.jetbrains.annotations.Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && tool.is(ItemRegistry.NIBIRIUM_PICKAXE)) {
            // Drop 1-2 dark matter shards, with fortune bonus
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
            Block.popResource(level, pos, new ItemStack(ItemRegistry.DARK_MATTER_SHARD, baseCount));
        }
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
        AABB wide = new AABB(pos).inflate(8.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, wide)) {
            if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) continue;
            double dist = Math.sqrt(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            float amount;
            if (dist <= 2.0)      amount = 20.0F;
            else if (dist <= 4.0) amount = 10.0F;
            else                  amount = 5.0F;
            InfectionManager.addMeter(player, amount);
        }
        level.scheduleTick(pos, this, 20);
    }
}

