package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum ModToolTiers implements Tier {
    PLATINUM(1561, 6.0F, 2.0F, 14),
    PALLADIUM(500,  8.0F, 3.0F, 10),
    NIBIRIUM(1561,  9.5F, 5.0F, 20);

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;

    ModToolTiers(int uses, float speed, float attackDamageBonus, int enchantmentValue) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return attackDamageBonus; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
    @Override public int getEnchantmentValue() { return enchantmentValue; }

    @Override
    public Ingredient getRepairIngredient() {
        return switch (this) {
            case PLATINUM  -> Ingredient.of(ItemRegistry.PLATINUM_INGOT);
            case PALLADIUM -> Ingredient.of(ItemRegistry.PALLADIUM_INGOT);
            case NIBIRIUM  -> Ingredient.of(ItemRegistry.NIBIRIUM_INGOT);
        };
    }
}
