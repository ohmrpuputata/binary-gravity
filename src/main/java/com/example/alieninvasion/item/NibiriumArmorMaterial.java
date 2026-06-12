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

/** Nibirium armor: the heaviest endgame plate - tanky and radiation-resistant. */
public class NibiriumArmorMaterial {
    public static final Holder<ArmorMaterial> INSTANCE = Holder.direct(new ArmorMaterial(
            Map.of(
                    net.minecraft.world.item.ArmorItem.Type.HELMET,     4,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 9,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS,   7,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS,      4
            ),
            12,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(ItemRegistry.NIBIRIUM_INGOT),
            List.of(new ArmorMaterial.Layer(
                    ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "nibirium")
            )),
            3.0F,
            0.1F
    ));
}
