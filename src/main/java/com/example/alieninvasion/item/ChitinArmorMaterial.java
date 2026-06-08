package com.example.alieninvasion.item;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Chitin Armor: a mid-tier organic suit grown from Alien Alloy and Infested
 * Flesh. Cheaper than Cosmic; its full-set perk (purge Infection + Poison) lives
 * in ModEvents. Worn texture -> textures/models/armor/chitin_layer_1.png (+ _2).
 */
public final class ChitinArmorMaterial {

    public static final int BASE_DURABILITY = 24;

    public static final Holder<ArmorMaterial> CHITIN = Holder.direct(new ArmorMaterial(
            createDefenseMap(2, 6, 5, 2),
            10,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ItemRegistry.ALIEN_ALLOY),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "chitin"))),
            1.5F,
            0.0F
    ));

    private static Map<ArmorItem.Type, Integer> createDefenseMap(int helmet, int chest, int legs, int boots) {
        EnumMap<ArmorItem.Type, Integer> m = new EnumMap<>(ArmorItem.Type.class);
        m.put(ArmorItem.Type.HELMET, helmet);
        m.put(ArmorItem.Type.CHESTPLATE, chest);
        m.put(ArmorItem.Type.LEGGINGS, legs);
        m.put(ArmorItem.Type.BOOTS, boots);
        return m;
    }

    private ChitinArmorMaterial() {
    }
}
