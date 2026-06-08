package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {
    // Only the entities that keep CUSTOM models need their own layers.
    // Telekinetic (Enderman), Brute (IronGolem), Alien Chicken (Chicken) and
    // Hive Tyrant (Warden) reuse vanilla layers from net.minecraft ModelLayers.
    public static final ModelLayerLocation ALIEN_GRUNT = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_grunt"), "main");
    public static final ModelLayerLocation UFO = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "ufo"), "main");
    public static final ModelLayerLocation METEOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "meteor"), "main");
    public static final ModelLayerLocation DRILL = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "drill"), "main");
    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "swarm_mother"), "main");
    public static final ModelLayerLocation BORER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "borer"), "main");
}
