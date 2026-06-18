package com.example.alieninvasion.item;

import net.minecraft.world.item.Item;

/**
 * Био-фильтр — маска, надеваемая в ОТДЕЛЬНЫЙ слот маски (поверх брони), а НЕ в слот
 * шлема. Поэтому это обычный предмет (не ArmorItem) — ванильный слот головы его не
 * примет. Экипировка/рендер — через слот маски (см. MaskSlot / MaskFeatureRenderer).
 */
public class BioFilterMaskItem extends Item {
    public BioFilterMaskItem(Item.Properties properties) {
        super(properties);
    }
}
