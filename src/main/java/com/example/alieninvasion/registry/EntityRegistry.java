package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienBruteEntity;
import com.example.alieninvasion.entity.AlienChickenEntity;
import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.entity.AlienTrollEntity;
import com.example.alieninvasion.entity.HiveTyrantEntity;
import com.example.alieninvasion.entity.TelekineticAlienEntity;
import com.example.alieninvasion.entity.UfoEntity;
import com.example.alieninvasion.entity.DrillEntity;
import com.example.alieninvasion.entity.MeteorEntity;
import com.example.alieninvasion.entity.ParasiteEntity;
import com.example.alieninvasion.entity.AlienStalkerEntity;
import com.example.alieninvasion.entity.PlasmaCasterEntity;
import com.example.alieninvasion.entity.HiveShamanEntity;
import com.example.alieninvasion.entity.AlienBreacherEntity;
import com.example.alieninvasion.entity.GravityGrenadeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityRegistry {

    public static final EntityType<AlienGruntEntity> ALIEN_GRUNT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_grunt"),
            EntityType.Builder.of(AlienGruntEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).build("alien_grunt")
    );

    public static final EntityType<TelekineticAlienEntity> TELEKINETIC_ALIEN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "telekinetic_alien"),
            EntityType.Builder.of(TelekineticAlienEntity::new, MobCategory.MONSTER).sized(0.6f, 2.9f).build("telekinetic_alien")
    );

    public static final EntityType<UfoEntity> UFO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "ufo"),
            EntityType.Builder.of(UfoEntity::new, MobCategory.MONSTER).sized(2.0f, 2.0f).build("ufo")
    );

    public static final EntityType<AlienBruteEntity> ALIEN_BRUTE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_brute"),
            EntityType.Builder.of(AlienBruteEntity::new, MobCategory.MONSTER).sized(1.4f, 2.7f).build("alien_brute")
    );

    public static final EntityType<AlienChickenEntity> ALIEN_CHICKEN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_chicken"),
            EntityType.Builder.of(AlienChickenEntity::new, MobCategory.MONSTER).sized(0.4f, 0.7f)
                    .build("alien_chicken"));

    public static final EntityType<HiveTyrantEntity> HIVE_TYRANT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hive_tyrant"),
            EntityType.Builder.of(HiveTyrantEntity::new, MobCategory.MONSTER).sized(0.9f, 2.9f).fireImmune()
                    .build("hive_tyrant"));

    public static final EntityType<AlienTrollEntity> ALIEN_TROLL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_troll"),
            EntityType.Builder.of(AlienTrollEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f)
                    .build("alien_troll"));

    public static final EntityType<DrillEntity> DRILL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "drill"),
            EntityType.Builder.of(DrillEntity::new, MobCategory.MISC).sized(1.0f, 2.0f).build("drill")
    );

    public static final EntityType<MeteorEntity> METEOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "meteor"),
            EntityType.Builder.of(MeteorEntity::new, MobCategory.MISC).sized(1.5f, 1.5f).build("meteor")
    );

    public static final EntityType<com.example.alieninvasion.entity.InfestedWormEntity> INFESTED_WORM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_worm"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedWormEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.5f).build("infested_worm")
    );

    public static final EntityType<com.example.alieninvasion.entity.AlienRaptorEntity> ALIEN_RAPTOR = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_raptor"),
            EntityType.Builder.of(com.example.alieninvasion.entity.AlienRaptorEntity::new, MobCategory.MONSTER)
                    .sized(0.85f, 1.1f).build("alien_raptor")
    );

    public static final EntityType<ParasiteEntity> PARASITE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "parasite"),
            EntityType.Builder.of(ParasiteEntity::new, MobCategory.MONSTER).sized(0.4f, 0.3f).build("parasite")
    );


    // Rare aliens with unique abilities.
    public static final EntityType<AlienStalkerEntity> ALIEN_STALKER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_stalker"),
            EntityType.Builder.of(AlienStalkerEntity::new, MobCategory.MONSTER).sized(1.0f, 0.9f)
                    .build("alien_stalker")
    );

    public static final EntityType<AlienBreacherEntity> ALIEN_BREACHER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_breacher"),
            EntityType.Builder.of(AlienBreacherEntity::new, MobCategory.MONSTER).sized(1.4f, 0.9f)
                    .build("alien_breacher")
    );

    public static final EntityType<PlasmaCasterEntity> PLASMA_CASTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_caster"),
            EntityType.Builder.of(PlasmaCasterEntity::new, MobCategory.MONSTER).sized(0.6f, 1.99f)
                    .build("plasma_caster")
    );

    public static final EntityType<HiveShamanEntity> HIVE_SHAMAN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hive_shaman"),
            EntityType.Builder.of(HiveShamanEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f)
                    .build("hive_shaman")
    );

    // Thrown gravity-grenade projectile (not a Mob, so no attributes).
    public static final EntityType<GravityGrenadeEntity> GRAVITY_GRENADE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "gravity_grenade"),
            EntityType.Builder.<GravityGrenadeEntity>of(GravityGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("gravity_grenade")
    );

    public static final EntityType<com.example.alieninvasion.entity.EmpGrenadeEntity> EMP_GRENADE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "emp_grenade"),
            EntityType.Builder.<com.example.alieninvasion.entity.EmpGrenadeEntity>of(com.example.alieninvasion.entity.EmpGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("emp_grenade")
    );

    public static final EntityType<com.example.alieninvasion.entity.AstralResonanceGrenadeEntity> ASTRAL_RESONANCE_GRENADE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "astral_resonance_grenade"),
            EntityType.Builder.<com.example.alieninvasion.entity.AstralResonanceGrenadeEntity>of(com.example.alieninvasion.entity.AstralResonanceGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("astral_resonance_grenade")
    );

    public static final EntityType<com.example.alieninvasion.entity.InfestedPlayerCloneEntity> INFESTED_PLAYER_CLONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_player_clone"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedPlayerCloneEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).build("infested_player_clone")
    );

    public static final EntityType<com.example.alieninvasion.entity.InfestedCreeperEntity> INFESTED_CREEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_creeper"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.7f).build("infested_creeper")
    );

    public static final EntityType<com.example.alieninvasion.entity.InfestedSkeletonEntity> INFESTED_SKELETON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_skeleton"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f).build("infested_skeleton")
    );

    public static final EntityType<com.example.alieninvasion.entity.InfestedZombieEntity> INFESTED_ZOMBIE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infested_zombie"),
            EntityType.Builder.of(com.example.alieninvasion.entity.InfestedZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).build("infested_zombie")
    );

    public static final EntityType<com.example.alieninvasion.entity.PlasmaBoltEntity> PLASMA_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_bolt"),
            EntityType.Builder.<com.example.alieninvasion.entity.PlasmaBoltEntity>of(com.example.alieninvasion.entity.PlasmaBoltEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("plasma_bolt")
    );

    public static final EntityType<com.example.alieninvasion.entity.MarkBoltEntity> MARK_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "mark_bolt"),
            EntityType.Builder.<com.example.alieninvasion.entity.MarkBoltEntity>of(com.example.alieninvasion.entity.MarkBoltEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("mark_bolt")
    );

    public static final EntityType<com.example.alieninvasion.entity.SwarmMotherEntity> SWARM_MOTHER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "swarm_mother"),
            EntityType.Builder.of(com.example.alieninvasion.entity.SwarmMotherEntity::new, MobCategory.MONSTER)
                    .sized(2.2f, 3.4f).build("swarm_mother")
    );

    // CREATURE, не MONSTER: охотник не должен исчезать на мирной сложности.
    public static final EntityType<com.example.alieninvasion.entity.HunterEntity> HUNTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "hunter"),
            EntityType.Builder.of(com.example.alieninvasion.entity.HunterEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f).fireImmune().build("hunter")
    );

    public static final EntityType<com.example.alieninvasion.entity.SkyDroneEntity> SKY_DRONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "sky_drone"),
            EntityType.Builder.of(com.example.alieninvasion.entity.SkyDroneEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 0.5f).build("sky_drone")
    );

    public static final EntityType<com.example.alieninvasion.entity.CaveLurkerEntity> CAVE_LURKER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "cave_lurker"),
            EntityType.Builder.of(com.example.alieninvasion.entity.CaveLurkerEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 0.8f).build("cave_lurker")
    );

    public static final EntityType<com.example.alieninvasion.entity.AcidSpitterEntity> ACID_SPITTER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "acid_spitter"),
            EntityType.Builder.of(com.example.alieninvasion.entity.AcidSpitterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).build("acid_spitter")
    );

    public static final EntityType<com.example.alieninvasion.entity.AcidBoltEntity> ACID_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "acid_bolt"),
            EntityType.Builder.<com.example.alieninvasion.entity.AcidBoltEntity>of(com.example.alieninvasion.entity.AcidBoltEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("acid_bolt")
    );

    public static final EntityType<com.example.alieninvasion.entity.RadiationBoltEntity> RADIATION_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "radiation_bolt"),
            EntityType.Builder.<com.example.alieninvasion.entity.RadiationBoltEntity>of(com.example.alieninvasion.entity.RadiationBoltEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("radiation_bolt")
    );

    public static void registerEntities() {
        // Attributes registration
        FabricDefaultAttributeRegistry.register(ALIEN_GRUNT, AlienGruntEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TELEKINETIC_ALIEN, TelekineticAlienEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(UFO, UfoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_BRUTE, AlienBruteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_CHICKEN, AlienChickenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HIVE_TYRANT, HiveTyrantEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_TROLL, AlienTrollEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PARASITE, com.example.alieninvasion.entity.ParasiteEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_WORM, com.example.alieninvasion.entity.InfestedWormEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_RAPTOR, com.example.alieninvasion.entity.AlienRaptorEntity.createAttributes());

        FabricDefaultAttributeRegistry.register(ALIEN_STALKER, AlienStalkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALIEN_BREACHER, AlienBreacherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PLASMA_CASTER, PlasmaCasterEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HIVE_SHAMAN, HiveShamanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_PLAYER_CLONE, com.example.alieninvasion.entity.InfestedPlayerCloneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_CREEPER, net.minecraft.world.entity.monster.Creeper.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_SKELETON, net.minecraft.world.entity.monster.Skeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(INFESTED_ZOMBIE, net.minecraft.world.entity.monster.Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(SWARM_MOTHER, com.example.alieninvasion.entity.SwarmMotherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SKY_DRONE, com.example.alieninvasion.entity.SkyDroneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CAVE_LURKER, com.example.alieninvasion.entity.CaveLurkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ACID_SPITTER, com.example.alieninvasion.entity.AcidSpitterEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HUNTER, com.example.alieninvasion.entity.HunterEntity.createAttributes());
    }
}
