package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final CreativeModeTab ALIEN_invasion_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_invasion_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ItemRegistry.ALIEN_GRUNT_SPAWN_EGG))
                    .title(Component.translatable("itemGroup.alien_invasion"))
                    .displayItems((context, entries) -> {
                        // EVERYTHING the mod registers, automatically - new blocks and
                        // items can never be forgotten out of the creative tab again.
                        for (Item item : BuiltInRegistries.ITEM) {
                            if (BuiltInRegistries.ITEM.getKey(item).getNamespace()
                                    .equals(AlienInvasionMod.MODID)) {
                                entries.accept(item);
                            }
                        }
                    })
                    .build());

    public static void registerItemGroups() {
        AlienInvasionMod.LOGGER.info("Registering Item Groups for " + AlienInvasionMod.MODID);
    }
}
