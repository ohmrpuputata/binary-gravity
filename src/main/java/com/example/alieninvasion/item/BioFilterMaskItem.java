package com.example.alieninvasion.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

public class BioFilterMaskItem extends ArmorItem {
    public BioFilterMaskItem(Item.Properties properties) {
        super(AlienHazmatArmorMaterial.ALIEN_HAZMAT, Type.HELMET, properties);
    }
}
