package com.example.alieninvasion;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModFluids;
import com.example.alieninvasion.client.AlienBruteRenderer;
import com.example.alieninvasion.client.AlienChickenRenderer;
import com.example.alieninvasion.client.AlienGruntRenderer;
import com.example.alieninvasion.client.AlienTrollRenderer;
import com.example.alieninvasion.client.HiveTyrantRenderer;
import com.example.alieninvasion.client.ModModelLayers;
import com.example.alieninvasion.client.TelekineticAlienRenderer;
import com.example.alieninvasion.client.UfoRenderer;
import com.example.alieninvasion.client.DrillRenderer;
import com.example.alieninvasion.client.MeteorRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class AlienInvasionClient implements ClientModInitializer {
        private static void registerHumanoid(net.minecraft.client.model.geom.ModelLayerLocation layer,
                        com.example.alieninvasion.client.model.AlienHumanoidModel.Variant variant) {
                EntityModelLayerRegistry.registerModelLayer(layer,
                                () -> com.example.alieninvasion.client.model.AlienHumanoidModel.createBodyLayer(variant));
        }

        @Override
        public void onInitializeClient() {
                // Renderers
                EntityRendererRegistry.register(EntityRegistry.ALIEN_GRUNT, AlienGruntRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ALIEN_BRUTE, AlienBruteRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.TELEKINETIC_ALIEN, TelekineticAlienRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.UFO, UfoRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ALIEN_CHICKEN, AlienChickenRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.HIVE_TYRANT, HiveTyrantRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ALIEN_TROLL, AlienTrollRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.DRILL, DrillRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.METEOR, MeteorRenderer::new);

                // Rare aliens (reuse vanilla models) + thrown grenade.
                EntityRendererRegistry.register(EntityRegistry.ALIEN_STALKER,
                                com.example.alieninvasion.client.AlienStalkerRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ALIEN_BREACHER,
                                com.example.alieninvasion.client.AlienBreacherRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.PLASMA_CASTER,
                                com.example.alieninvasion.client.PlasmaCasterRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.HIVE_SHAMAN,
                                com.example.alieninvasion.client.HiveShamanRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.PARASITE,
                                com.example.alieninvasion.client.ParasiteRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.INFESTED_WORM,
                                com.example.alieninvasion.client.InfestedWormRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ALIEN_RAPTOR,
                                com.example.alieninvasion.client.AlienRaptorRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.INFESTED_PLAYER_CLONE,
                                com.example.alieninvasion.client.InfestedCloneRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.INFESTED_CREEPER,
                                ctx -> new net.minecraft.client.renderer.entity.CreeperRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.INFESTED_SKELETON,
                                ctx -> new net.minecraft.client.renderer.entity.SkeletonRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.INFESTED_ZOMBIE,
                                ctx -> new net.minecraft.client.renderer.entity.ZombieRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.SWARM_MOTHER,
                                com.example.alieninvasion.client.SwarmMotherRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.HUNTER,
                                com.example.alieninvasion.client.HunterRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.GRAVITY_GRENADE,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.EMP_GRENADE,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.PLASMA_BOLT,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.MARK_BOLT,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.ACID_BOLT,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.RADIATION_BOLT,
                                ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx));
                EntityRendererRegistry.register(EntityRegistry.SKY_DRONE,
                                com.example.alieninvasion.client.SkyDroneRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.CAVE_LURKER,
                                com.example.alieninvasion.client.CaveLurkerRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ACID_SPITTER,
                                com.example.alieninvasion.client.AcidSpitterRenderer::new);
                // Only the custom-model entities register their own layers.
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ALIEN_GRUNT,
                                com.example.alieninvasion.client.model.AlienGruntModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.UFO,
                                com.example.alieninvasion.client.model.UfoModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.DRILL,
                                com.example.alieninvasion.client.model.DrillModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.METEOR,
                                com.example.alieninvasion.client.model.MeteorModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SWARM_MOTHER,
                                com.example.alieninvasion.client.model.SwarmMotherModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLASMA_TURRET,
                                com.example.alieninvasion.client.model.PlasmaTurretModel::createBodyLayer);
                net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                                com.example.alieninvasion.registry.ModBlocks.PLASMA_TURRET_BLOCK_ENTITY,
                                com.example.alieninvasion.client.PlasmaTurretRenderer::new);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ALIEN_CHICKEN,
                                com.example.alieninvasion.client.model.AlienChickenModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.SKY_DRONE,
                                com.example.alieninvasion.client.model.SkyDroneModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.INFESTED_WORM,
                                com.example.alieninvasion.client.model.InfestedWormModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PARASITE,
                                com.example.alieninvasion.client.model.InfestedWormModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ALIEN_RAPTOR,
                                com.example.alieninvasion.client.model.AlienRaptorModel::createBodyLayer);
                // Shared swarm-humanoid skeleton: one parametric model, eight builds.
                registerHumanoid(ModModelLayers.ALIEN_BRUTE,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.BRUTE);
                registerHumanoid(ModModelLayers.ALIEN_STALKER,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.STALKER);
                registerHumanoid(ModModelLayers.PLASMA_CASTER,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.CASTER);
                registerHumanoid(ModModelLayers.HIVE_SHAMAN,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.SHAMAN);
                registerHumanoid(ModModelLayers.TELEKINETIC_ALIEN,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.TELEKINETIC);
                registerHumanoid(ModModelLayers.ALIEN_TROLL,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.TROLL);
                registerHumanoid(ModModelLayers.HIVE_TYRANT,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.TYRANT);
                registerHumanoid(ModModelLayers.ALIEN_BREACHER,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.BREACHER);
                registerHumanoid(ModModelLayers.ACID_SPITTER,
                                com.example.alieninvasion.client.model.AlienHumanoidModel.Variant.SPITTER);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PALLADIUM_ARMOR,
                                com.example.alieninvasion.client.model.PalladiumArmorModel::createBodyLayer);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.COSMIC_ARMOR,
                                () -> com.example.alieninvasion.client.model.AdvancedArmorModel.createBodyLayer(
                                                com.example.alieninvasion.client.model.AdvancedArmorModel.Variant.COSMIC));
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.HAZMAT_ARMOR,
                                () -> com.example.alieninvasion.client.model.AdvancedArmorModel.createBodyLayer(
                                                com.example.alieninvasion.client.model.AdvancedArmorModel.Variant.HAZMAT));
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.CHEM_ARMOR,
                                () -> com.example.alieninvasion.client.model.AdvancedArmorModel.createBodyLayer(
                                                com.example.alieninvasion.client.model.AdvancedArmorModel.Variant.CHEM));
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLATINUM_ARMOR,
                                () -> com.example.alieninvasion.client.model.AdvancedArmorModel.createBodyLayer(
                                                com.example.alieninvasion.client.model.AdvancedArmorModel.Variant.PLATINUM));
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                                new com.example.alieninvasion.client.PalladiumArmorRenderer(),
                                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_HELMET,
                                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_CHESTPLATE,
                                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_LEGGINGS,
                                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_BOOTS);
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                                new com.example.alieninvasion.client.AdvancedArmorRenderer(
                                                ModModelLayers.COSMIC_ARMOR, "cosmic"),
                                com.example.alieninvasion.registry.ItemRegistry.COSMIC_HELMET,
                                com.example.alieninvasion.registry.ItemRegistry.COSMIC_CHESTPLATE,
                                com.example.alieninvasion.registry.ItemRegistry.COSMIC_LEGGINGS,
                                com.example.alieninvasion.registry.ItemRegistry.COSMIC_BOOTS);
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                                new com.example.alieninvasion.client.AdvancedArmorRenderer(
                                                ModModelLayers.HAZMAT_ARMOR, "alien_hazmat"),
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_HELMET,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_CHESTPLATE,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_LEGGINGS,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_BOOTS);
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                                new com.example.alieninvasion.client.AdvancedArmorRenderer(
                                                ModModelLayers.CHEM_ARMOR, "alien_chem"),
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_HELMET,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_CHESTPLATE,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_LEGGINGS,
                                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_BOOTS);
                net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
                                new com.example.alieninvasion.client.AdvancedArmorRenderer(
                                                ModModelLayers.PLATINUM_ARMOR, "platinum"),
                                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_HELMET,
                                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_CHESTPLATE,
                                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_LEGGINGS,
                                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_BOOTS);
                // HUD Overlay
                com.example.alieninvasion.client.InvasionHUDOverlay.register();

                // Победа: сервер шлёт пакет — клиент прячет HUD вторжения.
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                                com.example.alieninvasion.network.VictoryPayload.TYPE,
                                (payload, context) -> com.example.alieninvasion.client.ClientInvasionState.victoryShown = payload.won());
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                                (handler, client) -> com.example.alieninvasion.client.ClientInvasionState.victoryShown = false);

                FluidRenderHandlerRegistry.INSTANCE.register(
                                ModFluids.TOXIC_WATER_STILL,
                                ModFluids.TOXIC_WATER_FLOWING,
                                new SimpleFluidRenderHandler(
                                                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "block/toxic_water_still"),
                                                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "block/toxic_water_flow"),
                                                0x99a6ff40));
                FluidRenderHandlerRegistry.INSTANCE.setBlockTransparency(ModBlocks.TOXIC_WATER, true);
                BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(),
                                ModFluids.TOXIC_WATER_STILL, ModFluids.TOXIC_WATER_FLOWING);

                // Infected water reuses the animated toxic-water textures with a teal
                // tint so it reads as "corrupted water" while behaving like real water.
                FluidRenderHandlerRegistry.INSTANCE.register(
                                ModFluids.INFECTED_WATER_STILL,
                                ModFluids.INFECTED_WATER_FLOWING,
                                new SimpleFluidRenderHandler(
                                                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "block/toxic_water_still"),
                                                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "block/toxic_water_flow"),
                                                0x995FD0B0));
                FluidRenderHandlerRegistry.INSTANCE.setBlockTransparency(ModBlocks.INFECTED_WATER, true);
                BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(),
                                ModFluids.INFECTED_WATER_STILL, ModFluids.INFECTED_WATER_FLOWING);
                BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(),
                                ModBlocks.INFESTED_LEAVES, ModBlocks.DEAD_INFESTED_CROP, ModBlocks.BLOOD_POOL,
                                ModBlocks.BLOOD_LAYER,
                                ModBlocks.ALIEN_TENDRILS, ModBlocks.INFESTED_DOOR, ModBlocks.INFESTED_TRAPDOOR,
                                ModBlocks.INFESTED_GLASS);
                BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(), ModBlocks.INFESTED_ICE,
                                ModBlocks.ALIEN_PORTAL);

                net.minecraft.client.gui.screens.MenuScreens.register(
                                com.example.alieninvasion.registry.ModBlocks.PLATINUM_ANVIL_MENU,
                                com.example.alieninvasion.client.PlatinumAnvilScreen::new);
        }
}
