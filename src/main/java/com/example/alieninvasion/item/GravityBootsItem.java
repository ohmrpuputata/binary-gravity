package com.example.alieninvasion.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;

public class GravityBootsItem extends ArmorItem {
    public GravityBootsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties);
    }
}
