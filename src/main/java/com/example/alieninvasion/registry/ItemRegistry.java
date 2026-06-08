package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.item.InfestedFleshItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tiers;
import com.example.alieninvasion.item.ModToolTiers;
import com.example.alieninvasion.item.AlienBlasterItem;
import com.example.alieninvasion.item.GravityBootsItem;
import com.example.alieninvasion.item.InvasionTrackerItem;

public class ItemRegistry {

    public static final Item ALIEN_GRUNT_SPAWN_EGG = registerItem("alien_grunt_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_GRUNT, 0x00FF00, 0x000000, new Item.Properties()));

    // Summons a peaceful "worker" grunt (scavenger): it mines/gathers and hauls loot
    // to a stash instead of attacking. Uses the post-spawn hook to set the flag + name.
    public static final Item ALIEN_WORKER_SPAWN_EGG = registerItem("alien_worker_spawn_egg",
            new com.example.alieninvasion.item.CustomEntitySpawnerItem(() -> EntityRegistry.ALIEN_GRUNT,
                    new Item.Properties().rarity(Rarity.UNCOMMON), entity -> {
                if (entity instanceof com.example.alieninvasion.entity.AlienGruntEntity grunt) {
                    grunt.setScavenger(true);
                    grunt.setCustomName(net.minecraft.network.chat.Component.literal("§aПришелец-рабочий"));
                    grunt.setCustomNameVisible(true);
                }
            }));

    public static final Item TELEKINETIC_ALIEN_SPAWN_EGG = registerItem("telekinetic_alien_spawn_egg",
            new SpawnEggItem(EntityRegistry.TELEKINETIC_ALIEN, 0x800080, 0x000000, new Item.Properties()));

    public static final Item UFO_SPAWN_EGG = registerItem("ufo_spawn_egg",
            new SpawnEggItem(EntityRegistry.UFO, 0x808080, 0xFF0000, new Item.Properties()));

    public static final Item ALIEN_BRUTE_SPAWN_EGG = registerItem("alien_brute_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_BRUTE, 0x555555, 0xFF0000, new Item.Properties()));

    public static final Item ALIEN_CHICKEN_SPAWN_EGG = registerItem("alien_chicken_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_CHICKEN, 0x3FA000, 0xCC2020, new Item.Properties()));

    public static final Item HIVE_TYRANT_SPAWN_EGG = registerItem("hive_tyrant_spawn_egg",
            new SpawnEggItem(EntityRegistry.HIVE_TYRANT, 0x2B3340, 0x21D8C8, new Item.Properties()));

    public static final Item ALIEN_TROLL_SPAWN_EGG = registerItem("alien_troll_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_TROLL, 0x4CAF20, 0x103000, new Item.Properties()));

    public static final Item INFESTED_PLAYER_CLONE_SPAWN_EGG = registerItem("infested_player_clone_spawn_egg",
            new SpawnEggItem(EntityRegistry.INFESTED_PLAYER_CLONE, 0x3FA000, 0x00FF00, new Item.Properties()));

    public static final Item INFESTED_CREEPER_SPAWN_EGG = registerItem("infested_creeper_spawn_egg",
            new SpawnEggItem(EntityRegistry.INFESTED_CREEPER, 0x0DA70D, 0x5D8A00, new Item.Properties()));

    public static final Item INFESTED_SKELETON_SPAWN_EGG = registerItem("infested_skeleton_spawn_egg",
            new SpawnEggItem(EntityRegistry.INFESTED_SKELETON, 0xC1C1C1, 0x5D8A00, new Item.Properties()));

    public static final Item INFESTED_ZOMBIE_SPAWN_EGG = registerItem("infested_zombie_spawn_egg",
            new SpawnEggItem(EntityRegistry.INFESTED_ZOMBIE, 0x00A0A0, 0x5D8A00, new Item.Properties()));

    public static final Item SWARM_MOTHER_SPAWN_EGG = registerItem("swarm_mother_spawn_egg",
            new SpawnEggItem(EntityRegistry.SWARM_MOTHER, 0x8A008A, 0x000000, new Item.Properties().rarity(Rarity.RARE)));

    // Interesting loot the player can scavenge from dead aliens / hives.
    public static final Item ALIEN_ALLOY = registerItem("alien_alloy",
            new Item(new Item.Properties()));

    public static final Item HIVE_CORE = registerItem("hive_core",
            new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final Item INFESTED_FLESH = registerItem("infested_flesh",
            new InfestedFleshItem(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.1F).alwaysEdible().build())));

    // Late-game weapon crafted from Alien Alloy: the strongest melee blade in the mod.
    // BioBladeItem: heavy infection + life-siphon on hit, armour-piercing vs aliens,
    // a right-click "Bio-Nova" AoE, always glowing. Fire-proof so lava can't eat it.
    public static final Item BIO_BLADE = registerItem("bio_blade",
            new com.example.alieninvasion.item.BioBladeItem(Tiers.NETHERITE, new Item.Properties()
                    .rarity(Rarity.EPIC).fireResistant()
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 11, -2.2F))));

    // Alien (bio) tool set. All share the sturdy ALIEN tool tier (high durability)
    // and each has a unique special ability (see the Bio*Item classes).
    public static final Item BIO_PICKAXE = registerItem("bio_pickaxe",
            new com.example.alieninvasion.item.BioPickaxeItem(ModToolTiers.ALIEN, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.ALIEN, 1.0F, -2.8F))));

    public static final Item BIO_AXE = registerItem("bio_axe",
            new com.example.alieninvasion.item.BioAxeItem(ModToolTiers.ALIEN, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(AxeItem.createAttributes(ModToolTiers.ALIEN, 5.0F, -3.0F))));

    public static final Item BIO_SHOVEL = registerItem("bio_shovel",
            new com.example.alieninvasion.item.BioShovelItem(ModToolTiers.ALIEN, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(ShovelItem.createAttributes(ModToolTiers.ALIEN, 1.5F, -3.0F))));

    public static final Item BIO_HOE = registerItem("bio_hoe",
            new com.example.alieninvasion.item.BioHoeItem(ModToolTiers.ALIEN, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(HoeItem.createAttributes(ModToolTiers.ALIEN, -3.0F, 0.0F))));

    public static final Item COSMIC_SHARD = registerItem("cosmic_shard",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Refined cosmic metal: the core crafting material for Cosmic Armor.
    public static final Item COSMIC_INGOT = registerItem("cosmic_ingot",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Injectable cure: purges Infection/Radiation/Psychic Pressure + Regeneration.
    public static final Item BIO_SERUM = registerItem("bio_serum",
            new com.example.alieninvasion.item.BioSerumItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    // Слабое противоядие: очищает 1 и 2 стадию заражения.
    public static final Item WEAK_ANTIDOTE = registerItem("weak_antidote",
            new com.example.alieninvasion.item.AntidoteItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    // Боевой стим: мгновенное лечение + щит/регенерация/скорость/сила. Облегчение боя.
    public static final Item COSMIC_STIMULANT = registerItem("cosmic_stimulant",
            new com.example.alieninvasion.item.CosmicStimulantItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    // Crafting component (powers the Gravity Gun recipe and Cosmic gear).
    public static final Item ALIEN_BATTERY = registerItem("alien_battery",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Throwable anti-gravity grenade.
    public static final Item GRAVITY_GRENADE = registerItem("gravity_grenade",
            new com.example.alieninvasion.item.GravityGrenadeItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    // Hazmat suit: custom material (own worn skin) + durability so it never stacks.
    public static final Item HAZMAT_HELMET = registerItem("hazmat_helmet",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.HazmatArmorMaterial.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(com.example.alieninvasion.item.HazmatArmorMaterial.BASE_DURABILITY))));

    public static final Item HAZMAT_CHESTPLATE = registerItem("hazmat_chestplate",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.HazmatArmorMaterial.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(com.example.alieninvasion.item.HazmatArmorMaterial.BASE_DURABILITY))));

    public static final Item HAZMAT_LEGGINGS = registerItem("hazmat_leggings",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.HazmatArmorMaterial.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(com.example.alieninvasion.item.HazmatArmorMaterial.BASE_DURABILITY))));

    public static final Item HAZMAT_BOOTS = registerItem("hazmat_boots",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.HazmatArmorMaterial.HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(com.example.alieninvasion.item.HazmatArmorMaterial.BASE_DURABILITY))));

    // Light hazmat (crafted from alien skin). Full set shields LIGHT radiation +
    // corrupted-ground infection. Upgrade with Nibirium into the full hazmat.
    public static final Item LIGHT_HAZMAT_HELMET = registerItem("light_hazmat_helmet",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.LightHazmatArmorMaterial.LIGHT_HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(com.example.alieninvasion.item.LightHazmatArmorMaterial.BASE_DURABILITY))));
    public static final Item LIGHT_HAZMAT_CHESTPLATE = registerItem("light_hazmat_chestplate",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.LightHazmatArmorMaterial.LIGHT_HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()
                    .durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(com.example.alieninvasion.item.LightHazmatArmorMaterial.BASE_DURABILITY))));
    public static final Item LIGHT_HAZMAT_LEGGINGS = registerItem("light_hazmat_leggings",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.LightHazmatArmorMaterial.LIGHT_HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()
                    .durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(com.example.alieninvasion.item.LightHazmatArmorMaterial.BASE_DURABILITY))));
    public static final Item LIGHT_HAZMAT_BOOTS = registerItem("light_hazmat_boots",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.LightHazmatArmorMaterial.LIGHT_HAZMAT,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()
                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(com.example.alieninvasion.item.LightHazmatArmorMaterial.BASE_DURABILITY))));

    // Chitin Armor: a mid-tier organic suit. Full set purges Infection + Poison
    // (cheaper anti-infection path than the Cosmic set). See ModEvents.
    public static final Item CHITIN_HELMET = registerItem("chitin_helmet",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.ChitinArmorMaterial.CHITIN,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(com.example.alieninvasion.item.ChitinArmorMaterial.BASE_DURABILITY))));

    public static final Item CHITIN_CHESTPLATE = registerItem("chitin_chestplate",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.ChitinArmorMaterial.CHITIN,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(com.example.alieninvasion.item.ChitinArmorMaterial.BASE_DURABILITY))));

    public static final Item CHITIN_LEGGINGS = registerItem("chitin_leggings",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.ChitinArmorMaterial.CHITIN,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(com.example.alieninvasion.item.ChitinArmorMaterial.BASE_DURABILITY))));

    public static final Item CHITIN_BOOTS = registerItem("chitin_boots",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.ChitinArmorMaterial.CHITIN,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(com.example.alieninvasion.item.ChitinArmorMaterial.BASE_DURABILITY))));

    // Cosmic Armor: a custom material. The FULL set lets you walk safely over alien
    // blocks (draining armor durability) and grants Infection immunity (see ModEvents).
    public static final Item COSMIC_HELMET = registerItem("cosmic_helmet",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.CosmicArmorMaterial.COSMIC,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(com.example.alieninvasion.item.CosmicArmorMaterial.BASE_DURABILITY))));

    public static final Item COSMIC_CHESTPLATE = registerItem("cosmic_chestplate",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.CosmicArmorMaterial.COSMIC,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(com.example.alieninvasion.item.CosmicArmorMaterial.BASE_DURABILITY))));

    public static final Item COSMIC_LEGGINGS = registerItem("cosmic_leggings",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.CosmicArmorMaterial.COSMIC,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(com.example.alieninvasion.item.CosmicArmorMaterial.BASE_DURABILITY))));

    public static final Item COSMIC_BOOTS = registerItem("cosmic_boots",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.CosmicArmorMaterial.COSMIC,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(com.example.alieninvasion.item.CosmicArmorMaterial.BASE_DURABILITY))));

    // Uses a self-refilling charge bar (see GravityGunItem) instead of durability.
    public static final Item GRAVITY_GUN = registerItem("gravity_gun",
            new com.example.alieninvasion.item.GravityGunItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    // Handheld cleansing wand. NOTE: registered as "purifier_wand" (NOT "purifier")
    // because the placeable PurifierBlock already owns the "alien-invasion:purifier"
    // id. Two registry entries sharing one id is what crashed the client on
    // multiplayer join (ItemModels.modelIds "Map contained two equal IDs").
    public static final Item PURIFIER_WAND = registerItem("purifier_wand",
            new com.example.alieninvasion.item.PurifierItem(new Item.Properties().rarity(Rarity.UNCOMMON).durability(250)));

    // Blaster now uses its durability bar as a 30-round magazine; reload with Plasma Cells.
    public static final Item ALIEN_BLASTER = registerItem("alien_blaster",
            new AlienBlasterItem(new Item.Properties().rarity(Rarity.RARE).durability(30)));

    // Render-only plasma round fired by the Alien Blaster (not in the creative tab).
    public static final Item PLASMA_BOLT_ITEM = registerItem("plasma_bolt",
            new Item(new Item.Properties()));

    // Craftable ammo: one cell fully reloads the blaster's 30-round magazine.
    public static final Item PLASMA_CELL = registerItem("plasma_cell",
            new Item(new Item.Properties()));

    public static final Item GRAVITY_BOOTS = registerItem("gravity_boots",
            new GravityBootsItem(com.example.alieninvasion.item.CosmicArmorMaterial.COSMIC,
                    new Item.Properties().rarity(Rarity.RARE)
                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(com.example.alieninvasion.item.CosmicArmorMaterial.BASE_DURABILITY))));

    public static final Item INVASION_TRACKER = registerItem("invasion_tracker",
            new InvasionTrackerItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final Item ALIEN_BREACHER_SPAWN_EGG = registerItem("alien_breacher_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_BREACHER, 0x473B25, 0xFF0000, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item DRILL_SPAWN_EGG = registerItem("drill_spawn_egg",
            new com.example.alieninvasion.item.CustomEntitySpawnerItem(() -> EntityRegistry.DRILL, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item METEOR_SPAWN_EGG = registerItem("meteor_spawn_egg",
            new com.example.alieninvasion.item.CustomEntitySpawnerItem(() -> EntityRegistry.METEOR, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item PARASITE_ITEM = registerItem("parasite_item",
            new com.example.alieninvasion.item.ParasiteItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item PARASITE_SPAWN_EGG = registerItem("parasite_spawn_egg",
            new SpawnEggItem(EntityRegistry.PARASITE, 0x5D8A00, 0x8A008A, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item ALIEN_STALKER_SPAWN_EGG = registerItem("alien_stalker_spawn_egg",
            new SpawnEggItem(EntityRegistry.ALIEN_STALKER, 0x1A2230, 0x55FF99, new Item.Properties().rarity(Rarity.RARE)));

    public static final Item PLASMA_CASTER_SPAWN_EGG = registerItem("plasma_caster_spawn_egg",
            new SpawnEggItem(EntityRegistry.PLASMA_CASTER, 0x223A2A, 0xFF6622, new Item.Properties().rarity(Rarity.RARE)));

    public static final Item HIVE_SHAMAN_SPAWN_EGG = registerItem("hive_shaman_spawn_egg",
            new SpawnEggItem(EntityRegistry.HIVE_SHAMAN, 0x3A2A4A, 0xBB55FF, new Item.Properties().rarity(Rarity.RARE)));

    public static final Item SKY_DRONE_SPAWN_EGG = registerItem("sky_drone_spawn_egg",
            new SpawnEggItem(EntityRegistry.SKY_DRONE, 0x224455, 0x66FFCC, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item CAVE_LURKER_SPAWN_EGG = registerItem("cave_lurker_spawn_egg",
            new SpawnEggItem(EntityRegistry.CAVE_LURKER, 0x1A2418, 0x88CC55, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item ACID_SPITTER_SPAWN_EGG = registerItem("acid_spitter_spawn_egg",
            new SpawnEggItem(EntityRegistry.ACID_SPITTER, 0x3A4A22, 0xCCFF44, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item BIO_GRAPPLING_HOOK = registerItem("bio_grappling_hook",
            new com.example.alieninvasion.item.BioGrapplingHookItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final Item COSMIC_WARHAMMER = registerItem("cosmic_warhammer",
            new com.example.alieninvasion.item.CosmicWarhammerItem(ModToolTiers.COSMIC, new Item.Properties().rarity(Rarity.RARE).stacksTo(1)
                    .attributes(SwordItem.createAttributes(ModToolTiers.COSMIC, 9, -3.2F))));

    // --- Super cosmic tools (top-tier endgame gear) ---
    public static final Item COSMIC_PICKAXE = registerItem("cosmic_pickaxe",
            new com.example.alieninvasion.item.CosmicPickaxeItem(ModToolTiers.COSMIC, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.COSMIC, 2.0F, -2.6F))));

    public static final Item STAR_CLEAVER = registerItem("star_cleaver",
            new com.example.alieninvasion.item.StarCleaverItem(ModToolTiers.COSMIC, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(AxeItem.createAttributes(ModToolTiers.COSMIC, 8.0F, -2.8F))));

    // Blink Core: short-range teleport / escape tool (relief + co-op mobility).
    public static final Item BLINK_CORE = registerItem("blink_core",
            new com.example.alieninvasion.item.BlinkCoreItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    // Comms Beacon: broadcasts your position to the whole server (multiplayer).
    public static final Item COMMS_BEACON = registerItem("comms_beacon",
            new com.example.alieninvasion.item.CommsBeaconItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    // Rally Banner: AoE team buff for co-op (Regen/Resistance/Speed/Absorption).
    public static final Item RALLY_BANNER = registerItem("rally_banner",
            new com.example.alieninvasion.item.RallyBannerItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    // New items from the big content patch (v1.13.0)
    public static final Item BORER = registerItem("borer",
            new com.example.alieninvasion.item.CustomEntitySpawnerItem(() -> EntityRegistry.BORER, new Item.Properties().rarity(Rarity.RARE)));

    public static final Item EMP_GRENADE = registerItem("emp_grenade",
            new com.example.alieninvasion.item.EmpGrenadeItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(16)));

    public static final Item HERBAL_SALVE = registerItem("herbal_salve",
            new com.example.alieninvasion.item.HerbalSalveItem(new Item.Properties().rarity(Rarity.COMMON).stacksTo(64)));

    // Alien Apocalypse v1.14: mining economy, toxic water and borer upgrades.
    public static final Item ALIEN_SCRAP = registerItem("alien_scrap",
            new Item(new Item.Properties().rarity(Rarity.COMMON)));

    public static final Item COSMIC_CREDIT = registerItem("cosmic_credit",
            new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final Item URANIUM_DUST = registerItem("uranium_dust",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item URANIUM_ROD = registerItem("uranium_rod",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item XENOCRYSTAL = registerItem("xenocrystal",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item BIO_FIBER = registerItem("bio_fiber",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final Item PLASMA_CORE = registerItem("plasma_core",
            new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final Item IRIDIUM_PLATE = registerItem("iridium_plate",
            new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final Item DARK_MATTER_SHARD = registerItem("dark_matter_shard",
            new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // Raw crystal of pure radiation. Dropped by the Pure Radiation Block. Carrying it
    // without a full hazmat suit irradiates you to death (see RadiationFieldManager).
    public static final Item RADIATION_CRYSTAL = registerItem("radiation_crystal",
            new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // --- Nibirium economy (day-2 ore infection -> platinum/palladium -> nibirium) ---
    // Chunks drop from the infected ore blocks; 9 -> a raw ore; raw smelts (blast
    // furnace only) to an ingot; 4 platinum + 4 palladium -> nibirium alloy.
    public static final Item PLATINUM_CHUNK = registerItem("platinum_chunk", new Item(new Item.Properties()));
    public static final Item PALLADIUM_CHUNK = registerItem("palladium_chunk", new Item(new Item.Properties()));
    public static final Item RAW_PLATINUM = registerItem("raw_platinum", new Item(new Item.Properties()));
    public static final Item RAW_PALLADIUM = registerItem("raw_palladium", new Item(new Item.Properties()));
    public static final Item PLATINUM_INGOT = registerItem("platinum_ingot", new Item(new Item.Properties()));
    public static final Item PALLADIUM_INGOT = registerItem("palladium_ingot", new Item(new Item.Properties()));
    public static final Item NIBIRIUM_INGOT = registerItem("nibirium_ingot",
            new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    // Alien hide: from the alien-flesh block (cut with shears/sword) and from slain
    // aliens. The early-game crafting base for a light hazmat suit.
    public static final Item ALIEN_SKIN = registerItem("alien_skin", new Item(new Item.Properties()));

    // Nibirium tools (netherite-grade). Pickaxe & shovel break a 3x3 (ModEvents);
    // the sword cleaves nearby foes (NibiriumSwordItem).
    public static final Item NIBIRIUM_SWORD = registerItem("nibirium_sword",
            new com.example.alieninvasion.item.NibiriumSwordItem(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM,
                    new Item.Properties().attributes(SwordItem.createAttributes(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM, 5, -2.4F))));
    public static final Item NIBIRIUM_PICKAXE = registerItem("nibirium_pickaxe",
            new net.minecraft.world.item.PickaxeItem(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM,
                    new Item.Properties().attributes(PickaxeItem.createAttributes(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM, 1.5F, -2.8F))));
    public static final Item NIBIRIUM_AXE = registerItem("nibirium_axe",
            new net.minecraft.world.item.AxeItem(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM,
                    new Item.Properties().attributes(AxeItem.createAttributes(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM, 5.5F, -3.0F))));
    public static final Item NIBIRIUM_SHOVEL = registerItem("nibirium_shovel",
            new net.minecraft.world.item.ShovelItem(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM,
                    new Item.Properties().attributes(ShovelItem.createAttributes(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM, 1.5F, -3.0F))));
    public static final Item NIBIRIUM_HOE = registerItem("nibirium_hoe",
            new net.minecraft.world.item.HoeItem(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM,
                    new Item.Properties().attributes(HoeItem.createAttributes(com.example.alieninvasion.item.ModToolTiers.NIBIRIUM, -3.0F, 0.0F))));

    public static final Item DRILL_FUEL_CELL = registerItem("drill_fuel_cell",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    public static final Item REINFORCED_DRILL_HEAD = registerItem("reinforced_drill_head",
            new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item LAVA_COOLING_MODULE = registerItem("lava_cooling_module",
            new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()));

    public static final Item TOXIC_SEAL_MODULE = registerItem("toxic_seal_module",
            new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item HEADLAMP_MODULE = registerItem("headlamp_module",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final Item TOXIC_WATER_BUCKET = registerItem("toxic_water_bucket",
            new BucketItem(com.example.alieninvasion.registry.ModFluids.TOXIC_WATER_STILL,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final Item INFECTED_WATER_BUCKET = registerItem("infected_water_bucket",
            new BucketItem(com.example.alieninvasion.registry.ModFluids.INFECTED_WATER_STILL,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final Item URANIUM_SWORD = registerItem("uranium_sword",
            new com.example.alieninvasion.item.UraniumSwordItem(ModToolTiers.URANIUM, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(SwordItem.createAttributes(ModToolTiers.URANIUM, 4, -2.4F))));

    public static final Item URANIUM_PICKAXE = registerItem("uranium_pickaxe",
            new PickaxeItem(ModToolTiers.URANIUM, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.URANIUM, 1.5F, -2.8F))));

    public static final Item URANIUM_AXE = registerItem("uranium_axe",
            new AxeItem(ModToolTiers.URANIUM, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(AxeItem.createAttributes(ModToolTiers.URANIUM, 5.0F, -3.0F))));

    public static final Item URANIUM_SHOVEL = registerItem("uranium_shovel",
            new ShovelItem(ModToolTiers.URANIUM, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(ShovelItem.createAttributes(ModToolTiers.URANIUM, 1.5F, -3.0F))));

    public static final Item URANIUM_HOE = registerItem("uranium_hoe",
            new HoeItem(ModToolTiers.URANIUM, new Item.Properties().rarity(Rarity.UNCOMMON)
                    .attributes(HoeItem.createAttributes(ModToolTiers.URANIUM, -2.0F, 0.0F))));

    public static final Item PLASMA_SWORD = registerItem("plasma_sword",
            new com.example.alieninvasion.item.PlasmaSwordItem(ModToolTiers.PLASMA, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(SwordItem.createAttributes(ModToolTiers.PLASMA, 5, -2.35F))));

    public static final Item PLASMA_PICKAXE = registerItem("plasma_pickaxe",
            new PickaxeItem(ModToolTiers.PLASMA, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.PLASMA, 2.0F, -2.65F))));

    public static final Item PLASMA_AXE = registerItem("plasma_axe",
            new AxeItem(ModToolTiers.PLASMA, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(AxeItem.createAttributes(ModToolTiers.PLASMA, 6.0F, -2.95F))));

    public static final Item PLASMA_SHOVEL = registerItem("plasma_shovel",
            new ShovelItem(ModToolTiers.PLASMA, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.PLASMA, 2.0F, -2.85F))));

    public static final Item PLASMA_HOE = registerItem("plasma_hoe",
            new HoeItem(ModToolTiers.PLASMA, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(HoeItem.createAttributes(ModToolTiers.PLASMA, -3.0F, 0.2F))));

    public static final Item IRIDIUM_SWORD = registerItem("iridium_sword",
            new com.example.alieninvasion.item.IridiumSwordItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(SwordItem.createAttributes(ModToolTiers.IRIDIUM, 6, -2.25F))));

    public static final Item IRIDIUM_PICKAXE = registerItem("iridium_pickaxe",
            new PickaxeItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.IRIDIUM, 2.5F, -2.55F))));

    public static final Item IRIDIUM_AXE = registerItem("iridium_axe",
            new AxeItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(AxeItem.createAttributes(ModToolTiers.IRIDIUM, 7.0F, -2.85F))));

    public static final Item IRIDIUM_SHOVEL = registerItem("iridium_shovel",
            new ShovelItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.IRIDIUM, 2.5F, -2.75F))));

    public static final Item IRIDIUM_HOE = registerItem("iridium_hoe",
            new HoeItem(ModToolTiers.IRIDIUM, new Item.Properties().rarity(Rarity.RARE).fireResistant()
                    .attributes(HoeItem.createAttributes(ModToolTiers.IRIDIUM, -4.0F, 0.5F))));

    public static final Item RADIATION_DRILL_HEAD = registerItem("radiation_drill_head",
            new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item PURIFIER_DRILL_HEAD = registerItem("purifier_drill_head",
            new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item TOXIC_WATER_PUMP = registerItem("toxic_water_pump",
            new com.example.alieninvasion.item.ToxicWaterPumpItem(new Item.Properties().rarity(Rarity.RARE).durability(256)));

    public static final Item GEIGER_COUNTER = registerItem("geiger_counter",
            new com.example.alieninvasion.item.GeigerCounterItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final Item PORTABLE_PURIFIER = registerItem("portable_purifier",
            new com.example.alieninvasion.item.PortablePurifierItem(new Item.Properties().rarity(Rarity.RARE).durability(160)));

    public static final Item RAD_PILLS = registerItem("rad_pills",
            new com.example.alieninvasion.item.RadPillItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    public static final Item BIO_FILTER_MASK = registerItem("bio_filter_mask",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final Item CONTAMINATED_FOOD = registerItem("contaminated_food",
            new com.example.alieninvasion.item.ContaminatedFoodItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.1F).alwaysEdible().build())));

    public static final Item PURIFIED_WATER_FLASK = registerItem("purified_water_flask",
            new Item(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name), item);
    }

    public static void registerItems() {
        // Just triggering class loading
    }
}
