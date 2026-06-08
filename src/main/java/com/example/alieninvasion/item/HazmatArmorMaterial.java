package com.example.alieninvasion.item;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hazmat suit material - gives the worn suit its own yellow skin
 * (textures/models/armor/hazmat_layer_1.png + _2). Full set grants radiation
 * immunity (handled in ModEvents). Iron-tier protection.
 */
public final class HazmatArmorMaterial {

    public static final int BASE_DURABILITY = 16;

    public static final Holder<ArmorMaterial> HAZMAT = Holder.direct(new ArmorMaterial(
            createDefenseMap(2, 6, 5, 2),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hazmat"))),
            0.0F,
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

    private HazmatArmorMaterial() {
    }
}
