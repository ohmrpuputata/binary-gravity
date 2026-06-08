package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;

// Bio-Pickaxe: an excavator that rips through alien/cosmic stone almost instantly.
public class BioPickaxeItem extends PickaxeItem {
    public BioPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(ModBlocks.INFESTED_STONE) || state.is(ModBlocks.ALIEN_RESIDUE)
                || state.is(ModBlocks.ALIEN_HIVE) || state.is(ModBlocks.COSMIC_ORE)
                || state.is(ModBlocks.ALIEN_STASH) || state.is(ModBlocks.ALIEN_BEACON)) {
            return 30.0F; // tears through the infestation
        }
        return super.getDestroySpeed(stack, state);
    }
}
