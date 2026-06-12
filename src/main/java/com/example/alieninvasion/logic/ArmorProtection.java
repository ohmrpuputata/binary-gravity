package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public final class ArmorProtection {
    private ArmorProtection() {}

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
