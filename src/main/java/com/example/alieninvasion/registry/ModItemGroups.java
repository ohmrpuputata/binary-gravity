package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final CreativeModeTab ALIEN_invasion_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_invasion_group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ItemRegistry.ALIEN_GRUNT_SPAWN_EGG))
                    .title(Component.translatable("itemGroup.alien_invasion"))
                    .displayItems((context, entries) -> {
                        entries.accept(ItemRegistry.ALIEN_GRUNT_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_WORKER_SPAWN_EGG);
                        entries.accept(ItemRegistry.TELEKINETIC_ALIEN_SPAWN_EGG);
                        entries.accept(ItemRegistry.UFO_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_BRUTE_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_CHICKEN_SPAWN_EGG);
                        entries.accept(ItemRegistry.HIVE_TYRANT_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_TROLL_SPAWN_EGG);
                        entries.accept(ItemRegistry.DRILL_SPAWN_EGG);
                        entries.accept(ItemRegistry.METEOR_SPAWN_EGG);
                        entries.accept(ItemRegistry.PARASITE_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_STALKER_SPAWN_EGG);
                        entries.accept(ItemRegistry.PLASMA_CASTER_SPAWN_EGG);
                        entries.accept(ItemRegistry.HIVE_SHAMAN_SPAWN_EGG);
                        entries.accept(ItemRegistry.SKY_DRONE_SPAWN_EGG);
                        entries.accept(ItemRegistry.CAVE_LURKER_SPAWN_EGG);
                        entries.accept(ItemRegistry.ACID_SPITTER_SPAWN_EGG);
                        entries.accept(ItemRegistry.ALIEN_BREACHER_SPAWN_EGG);
                        entries.accept(ItemRegistry.INFESTED_PLAYER_CLONE_SPAWN_EGG);
                        entries.accept(ItemRegistry.INFESTED_CREEPER_SPAWN_EGG);
                        entries.accept(ItemRegistry.INFESTED_SKELETON_SPAWN_EGG);
                        entries.accept(ItemRegistry.INFESTED_ZOMBIE_SPAWN_EGG);
                        entries.accept(ItemRegistry.SWARM_MOTHER_SPAWN_EGG);
                        entries.accept(ItemRegistry.PARASITE_ITEM);
                        entries.accept(ItemRegistry.HIVE_CORE);
                        entries.accept(ItemRegistry.INFESTED_FLESH);
                        entries.accept(ItemRegistry.COSMIC_SHARD);
                        entries.accept(ItemRegistry.COSMIC_INGOT);
                        entries.accept(ItemRegistry.ALIEN_BATTERY);
                        entries.accept(ItemRegistry.BIO_SERUM);
                        entries.accept(ItemRegistry.WEAK_ANTIDOTE);
                        entries.accept(ItemRegistry.COSMIC_STIMULANT);
                        entries.accept(ItemRegistry.BLINK_CORE);
                        entries.accept(ItemRegistry.COMMS_BEACON);
                        entries.accept(ItemRegistry.GRAVITY_GUN);
                        entries.accept(ItemRegistry.ALIEN_BLASTER);
                        entries.accept(ItemRegistry.PLASMA_CELL);
                        entries.accept(ItemRegistry.GRAVITY_GRENADE);
                        entries.accept(ItemRegistry.EMP_GRENADE);
                        entries.accept(ItemRegistry.HERBAL_SALVE);
                        entries.accept(ItemRegistry.DARK_MATTER_SHARD);
                        entries.accept(ItemRegistry.RADIATION_CRYSTAL);
                        entries.accept(ItemRegistry.PLATINUM_CHUNK);
                        entries.accept(ItemRegistry.PALLADIUM_CHUNK);
                        entries.accept(ItemRegistry.RAW_PLATINUM);
                        entries.accept(ItemRegistry.RAW_PALLADIUM);
                        entries.accept(ItemRegistry.PLATINUM_INGOT);
                        entries.accept(ItemRegistry.PALLADIUM_INGOT);
                        entries.accept(ItemRegistry.NIBIRIUM_INGOT);
                        entries.accept(ItemRegistry.ALIEN_SKIN);
                        entries.accept(ModBlocks.COSMIC_CRYSTAL_ORE.asItem());
                        entries.accept(ModBlocks.PURE_RADIATION_CRYSTAL_ORE.asItem());
                        entries.accept(ModBlocks.PLATINUM_ORE.asItem());
                        entries.accept(ModBlocks.PALLADIUM_ORE.asItem());
                        entries.accept(ModBlocks.ALIEN_FLESH.asItem());
                        entries.accept(ModBlocks.INFESTED_DIAMOND_ORE.asItem());
                        entries.accept(ModBlocks.INFESTED_REDSTONE_ORE.asItem());
                        entries.accept(ItemRegistry.PLATINUM_SWORD);
                        entries.accept(ItemRegistry.PLATINUM_PICKAXE);
                        entries.accept(ItemRegistry.PLATINUM_AXE);
                        entries.accept(ItemRegistry.PLATINUM_SHOVEL);
                        entries.accept(ItemRegistry.PLATINUM_HOE);
                        entries.accept(ItemRegistry.PALLADIUM_SWORD);
                        entries.accept(ItemRegistry.PALLADIUM_PICKAXE);
                        entries.accept(ItemRegistry.PALLADIUM_AXE);
                        entries.accept(ItemRegistry.PALLADIUM_SHOVEL);
                        entries.accept(ItemRegistry.PALLADIUM_HOE);
                        entries.accept(ItemRegistry.NIBIRIUM_SMITHING_TEMPLATE);
                        entries.accept(ItemRegistry.NIBIRIUM_SWORD);
                        entries.accept(ItemRegistry.NIBIRIUM_PICKAXE);
                        entries.accept(ItemRegistry.NIBIRIUM_AXE);
                        entries.accept(ItemRegistry.NIBIRIUM_SHOVEL);
                        entries.accept(ItemRegistry.NIBIRIUM_HOE);
                        entries.accept(ItemRegistry.ALIEN_HAZMAT_HELMET);
                        entries.accept(ItemRegistry.ALIEN_HAZMAT_CHESTPLATE);
                        entries.accept(ItemRegistry.ALIEN_HAZMAT_LEGGINGS);
                        entries.accept(ItemRegistry.ALIEN_HAZMAT_BOOTS);
                        entries.accept(ItemRegistry.ALIEN_CHEM_HELMET);
                        entries.accept(ItemRegistry.ALIEN_CHEM_CHESTPLATE);
                        entries.accept(ItemRegistry.ALIEN_CHEM_LEGGINGS);
                        entries.accept(ItemRegistry.ALIEN_CHEM_BOOTS);
                        entries.accept(ItemRegistry.INFECTED_WATER_BUCKET);
                        entries.accept(ItemRegistry.TOXIC_WATER_BUCKET);
                        entries.accept(ItemRegistry.GEIGER_COUNTER);
                        entries.accept(ItemRegistry.RAD_PILLS);
                        entries.accept(ItemRegistry.BIO_FILTER_MASK);
                        entries.accept(ItemRegistry.CONTAMINATED_FOOD);
                        entries.accept(ItemRegistry.PURIFIER_WAND);
                        entries.accept(ItemRegistry.INVASION_TRACKER);
                        entries.accept(ItemRegistry.COSMIC_HELMET);
                        entries.accept(ItemRegistry.COSMIC_CHESTPLATE);
                        entries.accept(ItemRegistry.COSMIC_LEGGINGS);
                        entries.accept(ItemRegistry.COSMIC_BOOTS);
                        entries.accept(ItemRegistry.GRAVITY_BOOTS);
                        entries.accept(ModBlocks.PURIFIER.asItem());
                        entries.accept(ModBlocks.PLASMA_TURRET.asItem());
                        entries.accept(ModBlocks.SWARM_BEACON.asItem());
                        entries.accept(ModBlocks.INFESTED_STONE.asItem());
                        entries.accept(ModBlocks.INFESTED_DEEPSLATE.asItem());
                        entries.accept(ModBlocks.INFESTED_DIRT.asItem());
                        entries.accept(ModBlocks.INFESTED_SAND.asItem());
                        entries.accept(ModBlocks.INFESTED_GRAVEL.asItem());
                        entries.accept(ModBlocks.INFESTED_CLAY.asItem());
                        entries.accept(ModBlocks.INFESTED_NETHERRACK.asItem());
                        entries.accept(ModBlocks.INFESTED_GRASS.asItem());
                        entries.accept(ModBlocks.INFESTED_LOG.asItem());
                        entries.accept(ModBlocks.INFESTED_PLANKS.asItem());
                        entries.accept(ModBlocks.INFESTED_LEAVES.asItem());
                        entries.accept(ModBlocks.DEAD_INFESTED_CROP.asItem());
                        entries.accept(ModBlocks.COSMIC_CRYSTAL.asItem());
                        entries.accept(ModBlocks.PURE_RADIATION_BLOCK.asItem());
                        entries.accept(ModBlocks.ALIEN_HIVE.asItem());
                        entries.accept(ModBlocks.ALIEN_RESIDUE.asItem());
                        entries.accept(ModBlocks.BLACK_MARKET_TERMINAL.asItem());
                        entries.accept(ModBlocks.PURIFIER_STATION.asItem());
                        entries.accept(ModBlocks.ORE_WASHER.asItem());
                        entries.accept(ModBlocks.BLUEPRINT_TABLE.asItem());
                        entries.accept(ModBlocks.WARNING_LAMP.asItem());
                        entries.accept(ModBlocks.CRACKED_ALIEN_PIPE.asItem());
                        entries.accept(ModBlocks.TOXIC_BARREL.asItem());
                        entries.accept(ModBlocks.BROKEN_LAB_CRATE.asItem());
                        entries.accept(ModBlocks.RADIATION_CRYSTAL_CLUSTER.asItem());
                        entries.accept(ModBlocks.CONTAMINATED_BONES.asItem());
                        entries.accept(ModBlocks.ALIEN_BEACON.asItem());
                        entries.accept(ModBlocks.ALIEN_STASH.asItem());
                    })
                    .build());

    public static void registerItemGroups() {
        AlienInvasionMod.LOGGER.info("Registering Item Groups for " + AlienInvasionMod.MODID);
    }
}
