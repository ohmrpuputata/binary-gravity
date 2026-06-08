package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Tool material for the alien "bio" tools.
 * High durability so the late-game alien gear feels sturdy, mines anything a
 * netherite tool can, and is repaired on an anvil with Alien Alloy.
 */
public enum ModToolTiers implements Tier {
    ALIEN(2800, 9.0F, 4.0F, 16),
    URANIUM(1800, 8.0F, 3.0F, 12),
    PLASMA(2200, 9.5F, 4.5F, 18),
    IRIDIUM(3200, 10.5F, 5.5F, 22),
    COSMIC(2500, 8.0F, 5.0F, 18),
    NIBIRIUM(3000, 9.5F, 5.0F, 20);

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

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        if (this == COSMIC) {
            return Ingredient.of(ItemRegistry.COSMIC_INGOT);
        }
        if (this == URANIUM) {
            return Ingredient.of(ItemRegistry.URANIUM_ROD);
        }
        if (this == PLASMA) {
            return Ingredient.of(ItemRegistry.PLASMA_CORE);
        }
        if (this == IRIDIUM) {
            return Ingredient.of(ItemRegistry.IRIDIUM_PLATE);
        }
        if (this == NIBIRIUM) {
            return Ingredient.of(ItemRegistry.NIBIRIUM_INGOT);
        }
        return Ingredient.of(ItemRegistry.ALIEN_ALLOY);
    }
}
