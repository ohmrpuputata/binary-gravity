package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;

/** Registers the mod's procedural structure features and wires their placement. */
public class ModFeatures {
    public static final Feature<NoneFeatureConfiguration> CAVE_DUNGEON =
            new CaveDungeonFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> ALIEN_OUTPOST =
            new AlienOutpostFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> CRASHED_UFO =
            new CrashedUfoFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> INFESTED_MINE =
            new InfestedMineFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> COSMIC_VAULT =
            new CosmicVaultFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> ABANDONED_LAB =
            new AbandonedLabFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> HIVE_NEST =
            new HiveNestFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> BURIED_MOTHERSHIP =
            new BuriedMothershipFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> ALIEN_MONOLITH =
            new AlienMonolithFeature(NoneFeatureConfiguration.CODEC);
    public static final Feature<NoneFeatureConfiguration> SURVIVOR_BUNKER =
            new SurvivorBunkerFeature(NoneFeatureConfiguration.CODEC);

    // Loot tables (data/alien-invasion/loot_table/chests/*.json)
    public static final ResourceKey<LootTable> CAVE_DUNGEON_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/cave_dungeon"));
    public static final ResourceKey<LootTable> ALIEN_CITY_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/alien_city"));
    public static final ResourceKey<LootTable> MINE_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/infested_mine"));
    public static final ResourceKey<LootTable> COSMIC_VAULT_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/cosmic_vault"));
    public static final ResourceKey<LootTable> ABANDONED_LAB_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/abandoned_lab"));
    public static final ResourceKey<LootTable> HIVE_NEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/hive_nest"));
    public static final ResourceKey<LootTable> MOTHERSHIP_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, rl("chests/mothership"));

    // Placed feature keys (data/alien-invasion/worldgen/placed_feature/*.json)
    public static final ResourceKey<PlacedFeature> CAVE_DUNGEON_PLACED = pf("cave_dungeon");
    public static final ResourceKey<PlacedFeature> ALIEN_OUTPOST_PLACED = pf("alien_outpost");
    public static final ResourceKey<PlacedFeature> CRASHED_UFO_PLACED = pf("crashed_ufo");
    public static final ResourceKey<PlacedFeature> INFESTED_MINE_PLACED = pf("infested_mine");
    public static final ResourceKey<PlacedFeature> COSMIC_VAULT_PLACED = pf("cosmic_vault");
    public static final ResourceKey<PlacedFeature> ABANDONED_LAB_PLACED = pf("abandoned_lab");
    public static final ResourceKey<PlacedFeature> HIVE_NEST_PLACED = pf("hive_nest");
    public static final ResourceKey<PlacedFeature> BURIED_MOTHERSHIP_PLACED = pf("buried_mothership");
    public static final ResourceKey<PlacedFeature> ALIEN_MONOLITH_PLACED = pf("alien_monolith");
    public static final ResourceKey<PlacedFeature> SURVIVOR_BUNKER_PLACED = pf("survivor_bunker");

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE, rl("cave_dungeon"), CAVE_DUNGEON);
        Registry.register(BuiltInRegistries.FEATURE, rl("alien_outpost"), ALIEN_OUTPOST);
        Registry.register(BuiltInRegistries.FEATURE, rl("crashed_ufo"), CRASHED_UFO);
        Registry.register(BuiltInRegistries.FEATURE, rl("infested_mine"), INFESTED_MINE);
        Registry.register(BuiltInRegistries.FEATURE, rl("cosmic_vault"), COSMIC_VAULT);
        Registry.register(BuiltInRegistries.FEATURE, rl("abandoned_lab"), ABANDONED_LAB);
        Registry.register(BuiltInRegistries.FEATURE, rl("hive_nest"), HIVE_NEST);
        Registry.register(BuiltInRegistries.FEATURE, rl("buried_mothership"), BURIED_MOTHERSHIP);
        Registry.register(BuiltInRegistries.FEATURE, rl("alien_monolith"), ALIEN_MONOLITH);
        Registry.register(BuiltInRegistries.FEATURE, rl("survivor_bunker"), SURVIVOR_BUNKER);

        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, CAVE_DUNGEON_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, INFESTED_MINE_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, COSMIC_VAULT_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, ABANDONED_LAB_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_DECORATION, HIVE_NEST_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, ALIEN_OUTPOST_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, CRASHED_UFO_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, BURIED_MOTHERSHIP_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, ALIEN_MONOLITH_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, SURVIVOR_BUNKER_PLACED);

        AlienInvasionMod.LOGGER.info("Registered worldgen structures: cave_dungeon, infested_mine, cosmic_vault, abandoned_lab, hive_nest, alien_outpost, crashed_ufo, buried_mothership, alien_monolith");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, path);
    }

    private static ResourceKey<PlacedFeature> pf(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, rl(path));
    }
}
