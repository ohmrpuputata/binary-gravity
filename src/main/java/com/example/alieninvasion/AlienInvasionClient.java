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
                EntityRendererRegistry.register(EntityRegistry.INFESTED_PLAYER_CLONE,
                                ctx -> new net.minecraft.client.renderer.entity.ZombieRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.INFESTED_CREEPER,
                                ctx -> new net.minecraft.client.renderer.entity.CreeperRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.INFESTED_SKELETON,
                                ctx -> new net.minecraft.client.renderer.entity.SkeletonRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.INFESTED_ZOMBIE,
                                ctx -> new net.minecraft.client.renderer.entity.ZombieRenderer(ctx));
                EntityRendererRegistry.register(EntityRegistry.SWARM_MOTHER,
                                com.example.alieninvasion.client.SwarmMotherRenderer::new);
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
                EntityRendererRegistry.register(EntityRegistry.SKY_DRONE,
                                com.example.alieninvasion.client.SkyDroneRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.CAVE_LURKER,
                                com.example.alieninvasion.client.CaveLurkerRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.ACID_SPITTER,
                                com.example.alieninvasion.client.AcidSpitterRenderer::new);
                EntityRendererRegistry.register(EntityRegistry.BORER,
                                com.example.alieninvasion.client.BorerRenderer::new);

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
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.BORER,
                                com.example.alieninvasion.client.model.BorerModel::createBodyLayer);

                // HUD Overlay
                com.example.alieninvasion.client.InvasionHUDOverlay.register();

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
                                ModBlocks.INFESTED_LEAVES, ModBlocks.DEAD_INFESTED_CROP, ModBlocks.BLOOD_POOL);
        }
}
