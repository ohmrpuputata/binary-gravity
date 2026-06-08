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
 * Light hazmat suit, stitched from alien skin. Leather-grade protection, but the
 * full set shields the wearer from LIGHT radiation and from the infection/slow of
 * stepping on corrupted ground (wired in RadiationFieldManager / InfectionManager /
 * InfestedBlock). Upgrade it with Nibirium to get the full hazmat (химдоспех).
 */
public final class LightHazmatArmorMaterial {

    public static final int BASE_DURABILITY = 8;

    public static final Holder<ArmorMaterial> LIGHT_HAZMAT = Holder.direct(new ArmorMaterial(
            createDefenseMap(1, 3, 2, 1),
            12,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ItemRegistry.ALIEN_SKIN),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "light_hazmat"))),
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

    private LightHazmatArmorMaterial() {
    }
}
