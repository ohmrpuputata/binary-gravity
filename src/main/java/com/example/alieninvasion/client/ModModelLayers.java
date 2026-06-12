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
    public static final ModelLayerLocation INFESTED_WORM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_worm"), "main");
    public static final ModelLayerLocation ALIEN_RAPTOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_raptor"), "main");
    public static final ModelLayerLocation PARASITE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "parasite"), "main");
    public static final ModelLayerLocation PLASMA_TURRET = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_turret"), "main");
    public static final ModelLayerLocation ALIEN_CHICKEN = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_chicken"), "main");
    public static final ModelLayerLocation ALIEN_BREACHER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_breacher"), "main");
    public static final ModelLayerLocation SKY_DRONE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "sky_drone"), "main");
    public static final ModelLayerLocation SWARM_MOTHER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "swarm_mother"), "main");

    public static final ModelLayerLocation ALIEN_BRUTE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_brute"), "main");
    public static final ModelLayerLocation ALIEN_STALKER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_stalker"), "main");
    public static final ModelLayerLocation PLASMA_CASTER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_caster"), "main");
    public static final ModelLayerLocation HIVE_SHAMAN = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hive_shaman"), "main");
    public static final ModelLayerLocation TELEKINETIC_ALIEN = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "telekinetic_alien"), "main");
    public static final ModelLayerLocation ALIEN_TROLL = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_troll"), "main");
    public static final ModelLayerLocation HIVE_TYRANT = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hive_tyrant"), "main");
    public static final ModelLayerLocation ACID_SPITTER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "acid_spitter"), "main");
    public static final ModelLayerLocation PALLADIUM_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "palladium_armor"), "main");
    public static final ModelLayerLocation COSMIC_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "cosmic_armor"), "main");
    public static final ModelLayerLocation HAZMAT_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hazmat_armor"), "main");
    public static final ModelLayerLocation CHEM_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "chem_armor"), "main");
    public static final ModelLayerLocation PLATINUM_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_armor"), "main");
}
