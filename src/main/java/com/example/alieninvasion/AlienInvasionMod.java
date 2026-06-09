package com.example.alieninvasion;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModEffects;
import com.example.alieninvasion.registry.ModFluids;
import com.example.alieninvasion.registry.ModItemGroups;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlienInvasionMod implements ModInitializer {
	public static final String MODID = "alien-invasion"; // Must match fabric.mod.json
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> INFESTED_STONE_VEIN_KEY =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "infested_stone_vein"));

	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> ALIEN_RESIDUE_VEIN_KEY =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "alien_residue_vein"));

	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> COSMIC_CRYSTAL_VEIN_KEY =
			net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "cosmic_crystal_vein"));

	@Override
	public void onInitialize() {
		LOGGER.info("Alien Invasion Mod Initialized (Fabric)");

		ModEffects.registerEffects();
		ModFluids.registerFluids();
		com.example.alieninvasion.registry.ModBlocks.registerBlocks(); // Register Blocks
		EntityRegistry.registerEntities();
		ItemRegistry.registerItems();
		ModItemGroups.registerItemGroups();

		// EventHandler.registerEvents(); // Old
		com.example.alieninvasion.events.ModEvents.registerEvents();

		// Smart vanilla mobs + player-like neutral villagers.
		com.example.alieninvasion.logic.SmartMobs.register();

		// Overworld-only campaign (dimensions disabled; win = survive 8 days).
		com.example.alieninvasion.logic.CampaignRules.register();

		// Procedural structures: cave dungeons, alien cities, crashed UFOs.
		com.example.alieninvasion.worldgen.ModFeatures.register();

		// Register World Gen features
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
				net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,
				INFESTED_STONE_VEIN_KEY
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
				net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,
				ALIEN_RESIDUE_VEIN_KEY
		);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
				net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES,
				COSMIC_CRYSTAL_VEIN_KEY
		);

		net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			com.example.alieninvasion.command.InvasionCommand.register(dispatcher);
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(level -> {
			if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
				com.example.alieninvasion.world.InvasionManager.get(level).tick(level);
			}
		});
	}
}
