package com.example.alieninvasion.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

public class BioFilterMaskItem extends ArmorItem {
    public BioFilterMaskItem(Item.Properties properties) {
        super(BioFilterMaskArmorMaterial.MATERIAL, Type.HELMET, properties);
    }
}
