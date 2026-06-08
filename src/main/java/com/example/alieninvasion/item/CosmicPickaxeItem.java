package com.example.alieninvasion.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;

// Cosmic Pickaxe: a super excavation tool - near-instant mining of anything it can
// correctly harvest, with high durability and an ever-present cosmic sheen.
public class CosmicPickaxeItem extends PickaxeItem {
    public CosmicPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        // If this is a block the pick can actually harvest, rip through it.
        return base > 1.0F ? Math.max(base, 30.0F) : base;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
