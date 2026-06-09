package com.example.alieninvasion.item;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public class AlienChemArmorMaterial {
    public static final int BASE_DURABILITY = 20; // iron-level durability

    public static final Holder<ArmorMaterial> ALIEN_CHEM = Holder.direct(new ArmorMaterial(
            Map.of(
                    net.minecraft.world.item.ArmorItem.Type.HELMET,     2,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 6,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS,   5,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS,      2
            ),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ItemRegistry.NIBIRIUM_INGOT),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_chem")
            )),
            1.0F,
            0.0F
    ));
}
