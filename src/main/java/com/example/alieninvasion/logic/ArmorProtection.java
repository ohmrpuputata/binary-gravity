package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArmorProtection {
    private ArmorProtection() {}

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * How many equipped armor pieces are "alien-grade" mod armor (anything that is
     * not a vanilla material). The swarm's armor-piercing adaptation is shrugged off
     * by these, so this drives how much ordinary (vanilla) armor still gets pierced.
     */
    public static int alienGradeArmorPieces(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor && isAlienGrade(armor)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isAlienGrade(ArmorItem armor) {
        var m = armor.getMaterial();
        boolean vanilla = m == ArmorMaterials.LEATHER || m == ArmorMaterials.CHAIN
                || m == ArmorMaterials.IRON || m == ArmorMaterials.GOLD
                || m == ArmorMaterials.DIAMOND || m == ArmorMaterials.NETHERITE
                || m == ArmorMaterials.TURTLE || m == ArmorMaterials.ARMADILLO;
        return !vanilla;
    }

    public static boolean hasSealedSuit(LivingEntity entity) {
        return hasSet(entity,
                ItemRegistry.ALIEN_HAZMAT_HELMET, ItemRegistry.ALIEN_HAZMAT_CHESTPLATE,
                ItemRegistry.ALIEN_HAZMAT_LEGGINGS, ItemRegistry.ALIEN_HAZMAT_BOOTS)
                || hasSet(entity,
                ItemRegistry.ALIEN_CHEM_HELMET, ItemRegistry.ALIEN_CHEM_CHESTPLATE,
                ItemRegistry.ALIEN_CHEM_LEGGINGS, ItemRegistry.ALIEN_CHEM_BOOTS)
                || hasSet(entity,
                ItemRegistry.COSMIC_HELMET, ItemRegistry.COSMIC_CHESTPLATE,
                ItemRegistry.COSMIC_LEGGINGS, ItemRegistry.COSMIC_BOOTS);
    }

    public static boolean isRadiationImmune(LivingEntity entity) {
        return entity.getTags().contains("RadiationImmune")
                || entity.getTags().contains("BunkerTrader")
                || hasSealedSuit(entity);
    }

    private static boolean hasSet(LivingEntity entity, Item helmet, Item chestplate, Item leggings, Item boots) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(helmet)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(chestplate)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(leggings)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(boots);
    }
}
