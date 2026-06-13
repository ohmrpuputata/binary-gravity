package com.example.alieninvasion.events;

import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.entity.UfoEntity;
import com.example.alieninvasion.logic.SurvivalManager;
import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.logic.InfectionManager;
import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModEffects;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.item.ModToolTiers;
import com.example.alieninvasion.world.InvasionManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import java.util.List;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player; 
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class ModEvents {
    public static int empTicksActive = 0;
    public static int meteorShowerTicks = 0;

    // Tracks when a brain-parasite latched onto each player (game-time tick), so it
    // can't be removed for the first 5 seconds (100 ticks). Set by ParasiteEntity.
    public static final java.util.Map<java.util.UUID, Long> PARASITE_ATTACH =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static final java.util.Map<java.util.UUID, Integer> PLAYER_EMP_TICKS =
            new java.util.concurrent.ConcurrentHashMap<>();
    
    public static class GravityAnomaly {
        public BlockPos pos;
        public int radius;
        public int ticksRemaining;
        
        public GravityAnomaly(BlockPos pos, int radius, int ticksRemaining) {
            this.pos = pos;
            this.radius = radius;
            this.ticksRemaining = ticksRemaining;
        }
    }
    
    public static final java.util.List<GravityAnomaly> ACTIVE_ANOMALIES = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Drifting acid-gas cloud: run from it or get under a roof. */
    public static class AcidCloud {
        public double x, z;
        public double vx, vz;
        public int radius;
        public int ticksRemaining;

        public AcidCloud(double x, double z, double vx, double vz, int radius, int ticks) {
            this.x = x; this.z = z; this.vx = vx; this.vz = vz;
            this.radius = radius; this.ticksRemaining = ticks;
        }
    }

    public static final java.util.List<AcidCloud> ACTIVE_CLOUDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    private static boolean isApplyingAPDamage = false;

    private static final ThreadLocal<Boolean> NIB_BREAKING = ThreadLocal.withInitial(() -> false);

    public static void registerEvents() {
        // Pre-contaminate chunks on load so unexplored areas match the current day's
        // infection level. The handler only enqueues; blocks are written from the
        // world tick (writing during chunk load can cascade-load neighbour chunks).
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD) {
                com.example.alieninvasion.logic.WorldContaminationManager.onChunkLoad(sl, chunk);
            }
        });

        // Drop queued contamination work when the world goes away so a freshly
        // opened world never inherits chunk queues from the previous one.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.dimension() == Level.OVERWORLD) {
                com.example.alieninvasion.logic.WorldContaminationManager.onWorldUnload();
            }
        });

        // Evict per-player radiation/infection session state on disconnect so the
        // static maps don't accumulate stale entries across a long-running server.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            net.minecraft.server.level.ServerPlayer p = handler.player;
            if (p != null) {
                com.example.alieninvasion.logic.RadiationManager.onDisconnect(p);
                com.example.alieninvasion.logic.InfectionManager.clear(p);
            }
        });

        // При входе шлём игроку актуальное состояние победы, чтобы HUD сразу был
        // в правильном виде (скрыт, если вторжение уже выиграно в этом мире).
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerLevel ow = server.getLevel(Level.OVERWORLD);
            boolean won = ow != null && InvasionManager.get(ow).isVictoryAchieved();
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(handler.player,
                    new com.example.alieninvasion.network.VictoryPayload(won));
        });

        // Palladium and Platinum sword effects on hit
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getEntity() instanceof Player player) {
                ItemStack weapon = player.getMainHandItem();
                if (!weapon.isEmpty()) {
                    if (weapon.is(ItemRegistry.PALLADIUM_SWORD)) {
                        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.POISON, 100, 0));
                    } else if (weapon.is(ItemRegistry.PLATINUM_SWORD)) {
                        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0));
                    } else if (weapon.is(ItemRegistry.NIBIRIUM_SWORD)) {
                        // Irradiation — blocks healing for 6 seconds
                        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION),
                                120, 0));
                    }
                }
            }
            return true;
        });

        // Platinum tools: repair when mining infested blocks
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world.isClientSide) {
                return;
            }
            net.minecraft.world.item.ItemStack tool = player.getMainHandItem();
            if (tool.isEmpty()) {
                return;
            }
            if (tool.getItem() instanceof net.minecraft.world.item.TieredItem tiered) {
                if (tiered.getTier() == ModToolTiers.PLATINUM) {
                    if (isInfested(state)) {
                        int currentDamage = tool.getDamageValue();
                        if (currentDamage > 0) {
                            // Net repair by reducing damage value
                            tool.setDamageValue(Math.max(0, currentDamage - 3));
                        }
                    }
                }
            }
        });

        // Nibirium pickaxe/shovel: shatter a 3x3 plane facing the player.
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world.isClientSide || player.isShiftKeyDown() || NIB_BREAKING.get()) {
                return;
            }
            net.minecraft.world.item.ItemStack tool = player.getMainHandItem();
            boolean pick = tool.is(ItemRegistry.NIBIRIUM_PICKAXE);
            boolean shovel = tool.is(ItemRegistry.NIBIRIUM_SHOVEL);
            if (!pick && !shovel) {
                return;
            }
            net.minecraft.core.Direction.Axis axis = net.minecraft.core.Direction.orderedByNearest(player)[0].getAxis();
            NIB_BREAKING.set(true);
            try {
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if (a == 0 && b == 0) {
                            continue;
                        }
                        BlockPos p = switch (axis) {
                            case Y -> pos.offset(a, 0, b);
                            case X -> pos.offset(0, a, b);
                            default -> pos.offset(a, b, 0);
                        };
                        BlockState s = world.getBlockState(p);
                        if (s.isAir() || world.getBlockEntity(p) != null) {
                            continue;
                        }
                        float hard = s.getDestroySpeed(world, p);
                        if (hard < 0.0F || hard > 50.0F) {
                            continue; // skip unbreakable / obsidian-grade
                        }
                        boolean correct = pick ? s.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                                : s.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
                        if (!correct || tool.isEmpty()) {
                            continue;
                        }
                        world.destroyBlock(p, true, player);
                        tool.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                    }
                }
            } finally {
                NIB_BREAKING.set(false);
            }
        });

        // Mimic right-click callback
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && entity instanceof LivingEntity living && living.getTags().contains("IsMimic")) {
                triggerMimicMorph((ServerLevel) world, living, player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // Sleep Prevention
        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) -> {
            if (SurvivalManager.isAlienInvasionActive(player.level())) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal(
                            "§cНет времени для сна! Они в небе... (День " + SurvivalManager.getDay(player.level()) + ")"));
                }
                return Player.BedSleepingProblem.OTHER_PROBLEM; // Deny sleep with generic reason (or appropriate enum)
            }
            return null; // Return null to allow sleep (Mojang mapping specific)
        });

        // Death Events (Infection/Assimilation)
        // CO-OP REVIVE: a dying player is yanked back from death if a teammate within
        // 12 blocks is carrying a Hive Core (consumed). Clutch save for online play.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer dying) || entity.level().isClientSide) {
                return true;
            }
            for (Player ally : entity.level().getEntitiesOfClass(Player.class,
                    entity.getBoundingBox().inflate(12.0D), p -> p != entity && p.isAlive())) {
                for (int i = 0; i < ally.getInventory().getContainerSize(); i++) {
                    ItemStack s = ally.getInventory().getItem(i);
                    if (s.is(ItemRegistry.HIVE_CORE)) {
                        s.shrink(1);
                        dying.setHealth(8.0F);
                        dying.removeAllEffects();
                        dying.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1, false, true));
                        dying.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1, false, true));
                        dying.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 200, 0, false, true));
                        if (entity.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, dying.getX(), dying.getY() + 1.0D, dying.getZ(), 80, 0.6, 1.0, 0.6, 0.3);
                            sl.playSound(null, dying.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
                        }
                        dying.sendSystemMessage(Component.literal("§a[!] " + ally.getName().getString() + " воскресил вас Ядром Улья!"));
                        ally.sendSystemMessage(Component.literal("§a[!] Вы воскресили " + dying.getName().getString() + "!"));
                        return false; // cancel the death
                    }
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // Clear the parasite removal-lock so a dead host doesn't get re-parasitized
            // on respawn.
            if (entity instanceof Player deadPlayer) {
                PARASITE_ATTACH.remove(deadPlayer.getUUID());
                com.example.alieninvasion.logic.RadiationManager.clearDose(deadPlayer);
                com.example.alieninvasion.logic.InfectionManager.clear(deadPlayer);

                // CORPSE-RUNNER: from day 4 the swarm grows a clone out of every
                // player corpse. It swallows the ENTIRE death drop, wears your best
                // gear and hunts YOU first. Killing it returns everything. If your
                // previous clone is still nearby, it claims the new drop instead.
                if (entity.level() instanceof ServerLevel cloneLevel
                        && cloneLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && SurvivalManager.getDay(cloneLevel) >= 4) {
                    com.example.alieninvasion.entity.InfestedPlayerCloneEntity existing = null;
                    for (com.example.alieninvasion.entity.InfestedPlayerCloneEntity c :
                            cloneLevel.getEntitiesOfClass(com.example.alieninvasion.entity.InfestedPlayerCloneEntity.class,
                                    deadPlayer.getBoundingBox().inflate(20.0D),
                                    c -> c.isAlive() && c.isOwner(deadPlayer.getUUID()))) {
                        existing = c;
                        break;
                    }
                    if (existing != null) {
                        existing.absorbDeathLoot(cloneLevel, deadPlayer.position());
                    } else {
                        com.example.alieninvasion.entity.InfestedPlayerCloneEntity clone =
                                EntityRegistry.INFESTED_PLAYER_CLONE.create(cloneLevel);
                        if (clone != null) {
                            clone.moveTo(deadPlayer.getX(), deadPlayer.getY(), deadPlayer.getZ(),
                                    deadPlayer.getYRot(), 0.0F);
                            clone.bindOwner(deadPlayer);
                            cloneLevel.addFreshEntity(clone);
                            clone.absorbDeathLoot(cloneLevel, deadPlayer.position());
                            cloneLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                                    deadPlayer.getX(), deadPlayer.getY() + 1.0D, deadPlayer.getZ(),
                                    40, 0.4D, 0.9D, 0.4D, 0.04D);
                            cloneLevel.playSound(null, deadPlayer.blockPosition(),
                                    SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.4F);
                            if (deadPlayer instanceof ServerPlayer sp) {
                                sp.sendSystemMessage(Component.literal(
                                        "§4Рой вырастил клона из вашего тела — он забрал ваши вещи. Убейте его, чтобы вернуть их."));
                            }
                        }
                    }
                }
            }

            // Infection logic: a host that dies while infected hatches a grunt.
            // Skip aliens themselves - otherwise the Bio-Blade (which now infects the
            // swarm) would breed an endless wave of grunts every time you kill one.
            boolean entityIsAlien = AlienUtils.isAlliedTo(null, entity);
            if (!entityIsAlien && entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION))) {
                spawnGrunt(entity.level(), entity.getX(), entity.getY(), entity.getZ());
            }

            // Assimilation logic
            if (source.getEntity() instanceof AlienGruntEntity) {
                if (entity instanceof Animal) {
                    spawnGrunt(entity.level(), entity.getX(), entity.getY(), entity.getZ());
                }
            }

            // Scavengeable loot from slain aliens (motivation to fight back).
            dropAlienLoot(entity);

            // THE LIFE CYCLE: infected hosts burst into worm broodlings on death
            // (1-2 tiny worms; late-game one may emerge already grown), and sometimes
            // a brain-parasite skitters out too.
            // После победы цикл прерывается: из убитых заражённых черви больше не
            // лезут — иначе добивание остатков вечно держало бы «пришельцев рядом»
            // и блокировало отступление роя (и приход Макса).
            if ((entity instanceof com.example.alieninvasion.entity.InfestedZombieEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedCreeperEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedSkeletonEntity
                    || entity instanceof com.example.alieninvasion.entity.InfestedPlayerCloneEntity)
                    && entity.level() instanceof ServerLevel pl
                    && !InvasionManager.get(pl).isVictoryAchieved()) {
                int wormDay = SurvivalManager.getDay(pl);
                int worms = 1 + pl.random.nextInt(2);
                for (int i = 0; i < worms; i++) {
                    com.example.alieninvasion.entity.InfestedWormEntity worm = EntityRegistry.INFESTED_WORM.create(pl);
                    if (worm != null) {
                        worm.moveTo(entity.getX() + (pl.random.nextDouble() - 0.5) * 0.6,
                                entity.getY() + 0.2, entity.getZ() + (pl.random.nextDouble() - 0.5) * 0.6,
                                pl.random.nextFloat() * 360F, 0F);
                        worm.setStage(wormDay >= 5 && pl.random.nextFloat() < 0.35F ? 1 : 0);
                        pl.addFreshEntity(worm);
                    }
                }
                if (pl.random.nextFloat() < 0.25F) {
                    com.example.alieninvasion.entity.ParasiteEntity p = EntityRegistry.PARASITE.create(pl);
                    if (p != null) {
                        p.moveTo(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
                        pl.addFreshEntity(p);
                    }
                }
            }
        });

        // Hurt Events (Blood Particles + Swarm Adaptation + Phantom Creepers)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Level level = entity.level();
            if (level.isClientSide) return true;

            // Mind-controlled parasite thralls never hurt players - the worm serves you.
            if (entity instanceof Player && source.getEntity() instanceof Mob src
                    && src.getTags().contains("PlayerParasiteAlly")) {
                return false;
            }

            // Gravity Boots fall damage protection
            if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                if (entity.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.GRAVITY_BOOTS)) {
                    return false;
                }
            }

            // 1. Phantom Creeper logic: vanish on touch or hit
            if (entity.getTags().contains("IsPhantom")) {
                ServerLevel sl = (ServerLevel) level;
                sl.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, entity.blockPosition(), SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.HOSTILE, 1.0F, 1.2F);
                entity.discard();
                return false; // Cancel damage
            }

            // 2. Swarm Adaptation logic (deflection, fire immunity, armor piercing)
            boolean isAlien = AlienUtils.isAlliedTo(null, entity);
            if (isAlien && level instanceof ServerLevel sl) {
                InvasionManager manager = InvasionManager.get(sl);

                // Projectile Shield - but NEVER deflect the player's plasma bolts, so
                // the Alien Blaster reliably damages the swarm.
                if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile
                        && !(source.getDirectEntity() instanceof com.example.alieninvasion.entity.PlasmaBoltEntity)) {
                    manager.incrementBowHits();
                    if (manager.getBowHits() > 30 && entity.getRandom().nextFloat() < 0.75F) {
                        level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.0F, 1.5F);
                        sl.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 8, 0.1, 0.1, 0.1, 0.1);
                        return false;
                    }
                }

                // Fire Immunity
                if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) || 
                    source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) || 
                    source.is(net.minecraft.world.damagesource.DamageTypes.LAVA) || 
                    source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
                    manager.incrementFireDamage();
                    if (manager.getFireDamage() > 30) {
                        entity.clearFire();
                        return false;
                    }
                }

                // Heavy Armor tracking
                if (source.getEntity() instanceof Player player) {
                    int heavyArmorCount = 0;
                    for (ItemStack armor : player.getArmorSlots()) {
                        if (armor.getItem() instanceof ArmorItem ai && 
                            (ai.getMaterial() == net.minecraft.world.item.ArmorMaterials.DIAMOND || 
                             ai.getMaterial() == net.minecraft.world.item.ArmorMaterials.NETHERITE)) {
                            heavyArmorCount++;
                        }
                    }
                    if (heavyArmorCount >= 3) {
                        manager.incrementHeavyArmorTicks(1);
                    }
                }
            }

            // 3. Alien Melee AP damage against heavy armor players + infection fill
            if (source.getEntity() instanceof Mob alien && AlienUtils.isAlliedTo(null, alien) && entity instanceof Player player) {
                // Every alien hit increases infection meter (bite path: capped at 90%
                // with a short cooldown, so a dogpile can't burst the meter to lethal 100)
                InfectionManager.addMeterFromBite(player, 1.0F);

                // Apply marked effect on hit and alert allies
                boolean isMarked = player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.MARKED));
                if (!isMarked) {
                    player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.MARKED), 600, 0, false, true));
                    if (level instanceof ServerLevel sl) {
                        sl.playSound(null, alien.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5F, 1.3F);
                        for (Mob ally : sl.getEntitiesOfClass(Mob.class, alien.getBoundingBox().inflate(64.0D),
                                e -> e != alien && AlienUtils.isAlliedTo(alien, e))) {
                            ally.setTarget(player);
                        }
                    }
                }

                InvasionManager manager = InvasionManager.get((ServerLevel) level);
                if (manager.getHeavyArmorTicks() > 100 && !isApplyingAPDamage) {
                    isApplyingAPDamage = true;
                    player.hurt(level.damageSources().magic(), amount);
                    isApplyingAPDamage = false;
                    return false;
                }
            }

            // Spawn blood particles
            if (amount > 0) {
                ((ServerLevel) level).sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        entity.getX(), entity.getY() + entity.getEyeHeight() * 0.7, entity.getZ(),
                        15, 0.2, 0.2, 0.2, 0.1);
            }
            // Heavy hits STAIN the floor: the block under the victim converts to
            // its bloody twin (stairs/fences keep shape). Wipe with right-click
            // or wash it off with water.
            if (amount >= 4.0F && level.random.nextFloat() < 0.5F && level instanceof ServerLevel splatLevel) {
                com.example.alieninvasion.block.BloodyBlocks.splatter(splatLevel, entity.blockPosition().below());
            }
            return true; // Allow damage
        });

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.isClientSide) return;

            // Radioactive-storm weather event (fallout under open sky; see RadiationManager).
            // Guard to the Overworld: END_WORLD_TICK fires per loaded dimension, but the
            // storm timer is a single global field - without this it would decrement
            // once per dimension and the storm would end ~3x early.
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                com.example.alieninvasion.logic.RadiationManager.tickStorm(level);

                // Global world contamination: detects day changes, re-queues visible
                // chunks and drains the work queues under a per-tick write budget.
                // После уничтожения планеты Роя мир ИСЦЕЛЯЕТСЯ (см. InvasionManager) —
                // глобальное заражение больше не должно перезаписывать вылеченные блоки.
                if (!com.example.alieninvasion.world.InvasionManager.get(level).isPlanetDestroyed()) {
                    com.example.alieninvasion.logic.WorldContaminationManager.tick(level);
                }
            }

            // Update Active Gravity Anomalies
            for (GravityAnomaly anomaly : ACTIVE_ANOMALIES) {
                anomaly.ticksRemaining--;
                if (anomaly.ticksRemaining <= 0) {
                    ACTIVE_ANOMALIES.remove(anomaly);
                    continue;
                }
                
                // Spawn particle indicators in the area
                if (level.random.nextInt(3) == 0) {
                    double rx = anomaly.pos.getX() + (level.random.nextDouble() - 0.5D) * anomaly.radius * 2;
                    double ry = anomaly.pos.getY() + (level.random.nextDouble() - 0.5D) * anomaly.radius * 2;
                    double rz = anomaly.pos.getZ() + (level.random.nextDouble() - 0.5D) * anomaly.radius * 2;
                    level.sendParticles(ParticleTypes.PORTAL, rx, ry, rz, 1, 0.0D, 0.1D, 0.0D, 0.0D);
                }
                
                // Lift entities
                net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(anomaly.pos).inflate(anomaly.radius);
                for (net.minecraft.world.entity.Entity entity : level.getEntities((net.minecraft.world.entity.Entity) null, area)) {
                    if (entity.distanceToSqr(anomaly.pos.getX(), anomaly.pos.getY(), anomaly.pos.getZ()) <= anomaly.radius * anomaly.radius) {
                        if (entity instanceof Player p && p.getAbilities().instabuild) {
                            continue;
                        }
                        // Apply slow hover force
                        net.minecraft.world.phys.Vec3 motion = entity.getDeltaMovement();
                        entity.setDeltaMovement(motion.x, Math.min(0.12D, motion.y + 0.05D), motion.z);
                        entity.hurtMarked = true;
                    }
                }
            }

            // ACID CLOUDS: slow drifting gas banks roam the wasteland from day 4.
            // Standing in one under open sky melts you - sprint away or get under
            // a roof. They sizzle audibly so you hear them coming.
            for (AcidCloud cloud : ACTIVE_CLOUDS) {
                cloud.ticksRemaining--;
                if (cloud.ticksRemaining <= 0) {
                    ACTIVE_CLOUDS.remove(cloud);
                    continue;
                }
                cloud.x += cloud.vx;
                cloud.z += cloud.vz;
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) cloud.x, (int) cloud.z);
                if (level.getGameTime() % 2L == 0L) {
                    for (int i = 0; i < 6; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0;
                        double r = Math.sqrt(level.random.nextDouble()) * cloud.radius;
                        level.sendParticles(ParticleTypes.SNEEZE,
                                cloud.x + Math.cos(a) * r, gy + 0.5 + level.random.nextDouble() * 2.5,
                                cloud.z + Math.sin(a) * r, 1, 0.2, 0.1, 0.2, 0.0);
                    }
                }
                if (level.getGameTime() % 60L == 0L) {
                    level.playSound(null, BlockPos.containing(cloud.x, gy, cloud.z), SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.AMBIENT, 1.2F, 0.5F);
                }
                if (level.getGameTime() % 20L == 0L) {
                    for (ServerPlayer p : level.players()) {
                        double ddx = p.getX() - cloud.x, ddz = p.getZ() - cloud.z;
                        if (ddx * ddx + ddz * ddz <= (double) cloud.radius * cloud.radius
                                && level.canSeeSky(p.blockPosition()) && !p.getAbilities().invulnerable) {
                            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80, 1, false, true));
                            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 120, 0, false, false));
                            p.displayClientMessage(Component.literal("§2☠ Кислотное облако! Бегите или под крышу!"), true);
                        }
                    }
                }
            }
            if (level.getGameTime() % 1200 == 0 && level.random.nextFloat() < 0.12F
                    && SurvivalManager.getDay(level) >= 5 && ACTIVE_CLOUDS.size() < 6) {
                for (ServerPlayer player : level.players()) {
                    double cx = player.getX() + (level.random.nextDouble() - 0.5) * 120.0;
                    double cz = player.getZ() + (level.random.nextDouble() - 0.5) * 120.0;
                    double ang = level.random.nextDouble() * Math.PI * 2.0;
                    ACTIVE_CLOUDS.add(new AcidCloud(cx, cz,
                            Math.cos(ang) * 0.045, Math.sin(ang) * 0.045,
                            6 + level.random.nextInt(5), 1600 + level.random.nextInt(1200)));
                    player.displayClientMessage(Component.literal(
                            "§2[!] С пустошей ползёт кислотное облако..."), false);
                    break;
                }
            }

            // Anomaly Gravity random trigger (Day 3+)
            if (level.getGameTime() % 400 == 0 && level.random.nextFloat() < 0.15F) {
                for (ServerPlayer player : level.players()) {
                    int day = SurvivalManager.getDay(level);
                    if (day >= 4) {
                        BlockPos spawnPos = player.blockPosition().offset(level.random.nextInt(31) - 15, level.random.nextInt(9) - 4, level.random.nextInt(31) - 15);
                        ACTIVE_ANOMALIES.add(new GravityAnomaly(spawnPos, 12, 600)); // 30 seconds
                        level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 2.0F, 0.5F);
                        player.displayClientMessage(Component.literal("§d[!] Возникла гравитационная аномалия поблизости!"), false);
                        break; // only spawn one per check
                    }
                }
            }

            // Tick EMP Pulse
            if (empTicksActive > 0) {
                empTicksActive--;
                if (empTicksActive <= 0) {
                    for (ServerPlayer player : level.players()) {
                        player.removeTag("EmpActive");
                    }
                    if (!level.players().isEmpty()) {
                        level.playSound(null, level.players().get(0).blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.0F, 1.0F);
                    }
                    // Broadcast message
                    for (ServerPlayer player : level.players()) {
                        player.displayClientMessage(Component.literal("§a[!] ЭМП-буря утихла. Приборы снова функционируют."), false);
                    }
                } else {
                    for (ServerPlayer player : level.players()) {
                        if (!player.getTags().contains("EmpActive")) {
                            player.addTag("EmpActive");
                        }
                    }
                    if (level.getGameTime() % 100 == 0) {
                        // Play spark sound
                        for (ServerPlayer player : level.players()) {
                            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 0.2F, 1.8F);
                        }
                    }
                }
            }

            // EMP random trigger (Day 4+, at night)
            if (level.isNight() && level.getGameTime() % 24000 == 13000) { // Shortly after night starts
                int day = SurvivalManager.getDay(level);
                if (day >= 5 && level.random.nextFloat() < 0.35F && empTicksActive <= 0) {
                    empTicksActive = 2400; // 2 minutes (2400 ticks)
                    for (ServerPlayer player : level.players()) {
                        player.addTag("EmpActive");
                        player.displayClientMessage(Component.literal("§c[!] Электромагнитный импульс! Высокотехнологичные приборы отключены!"), false);
                    }
                    // Play dramatic sound
                    for (ServerPlayer player : level.players()) {
                        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 1.0F, 0.5F);
                    }
                }
            }

            // METEOR SHOWER (rare world event): a ~30s storm of impacts near players.
            if (meteorShowerTicks <= 0 && level.isNight() && level.getGameTime() % 1200 == 0
                    && level.random.nextFloat() < 0.04F && !level.players().isEmpty()
                    && !InvasionManager.get(level).isVictoryAchieved()) {
                meteorShowerTicks = 600;
                for (ServerPlayer p : level.players()) {
                    p.displayClientMessage(Component.literal("§c[!] МЕТЕОРИТНЫЙ ДОЖДЬ! Найдите укрытие!"), false);
                    level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 1.0F, 0.4F);
                }
            }
            if (meteorShowerTicks > 0) {
                meteorShowerTicks--;
                if (level.getGameTime() % 25 == 0) {
                    for (ServerPlayer p : level.players()) {
                        double mx = p.getX() + (level.random.nextDouble() - 0.5) * 50.0;
                        double mz = p.getZ() + (level.random.nextDouble() - 0.5) * 50.0;
                        double my = p.getY() + 40 + level.random.nextInt(15);
                        com.example.alieninvasion.entity.MeteorEntity m = EntityRegistry.METEOR.create(level);
                        if (m != null) {
                            m.setPos(mx, my, mz);
                            m.setDeltaMovement(0.0, -1.0, 0.0);
                            level.addFreshEntity(m);
                        }
                    }
                }
            }

            // --- Cave gas pocket: deep, dimly-lit players occasionally rupture a
            // pocket of toxic cave gas (poison + nausea + brief darkness). A "get
            // out of here" hazard rather than a fight. ---
            if (level.getGameTime() % 200 == 0 && level.random.nextFloat() < 0.06F) {
                for (ServerPlayer p : level.players()) {
                    if (p.getAbilities().invulnerable) continue;
                    BlockPos pp = p.blockPosition();
                    if (p.getY() < 40 && !level.canSeeSky(pp) && level.getMaxLocalRawBrightness(pp) < 6) {
                        p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 140, 0, false, true));
                        p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 200, 0, false, false));
                        p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 120, 0, false, false));
                        level.sendParticles(ParticleTypes.SNEEZE, p.getX(), p.getY() + 0.7D, p.getZ(), 30, 1.0D, 1.0D, 1.0D, 0.02D);
                        level.playSound(null, pp, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 0.8F, 0.6F);
                        p.displayClientMessage(Component.literal("§2[!] Карман пещерного газа! Уходите отсюда."), true);
                    }
                }
            }

            // --- Underground ambush: deep in the dark the swarm springs a trap,
            // erupting from the surrounding blocks already locked onto the player. ---
            if (level.getGameTime() % 600 == 0 && level.random.nextFloat() < 0.12F
                    && SurvivalManager.getDay(level) >= 2
                    && !InvasionManager.get(level).isVictoryAchieved()) {
                for (ServerPlayer p : level.players()) {
                    BlockPos pp = p.blockPosition();
                    if (p.getY() >= 45 || level.canSeeSky(pp) || level.getMaxLocalRawBrightness(pp) >= 7) continue;
                    int want = 2 + level.random.nextInt(3);
                    int spawned = 0;
                    for (int i = 0; i < want * 4 && spawned < want; i++) {
                        BlockPos sp = pp.offset(level.random.nextInt(9) - 4, level.random.nextInt(3) - 1,
                                level.random.nextInt(9) - 4);
                        if (level.getBlockState(sp).isAir() && level.getBlockState(sp.above()).isAir()
                                && level.getBlockState(sp.below()).isSolidRender(level, sp.below())) {
                            Mob a = switch (level.random.nextInt(3)) {
                                case 0 -> EntityRegistry.ALIEN_GRUNT.create(level);
                                case 1 -> EntityRegistry.CAVE_LURKER.create(level);
                                default -> EntityRegistry.ALIEN_RAPTOR.create(level);
                            };
                            if (a != null) {
                                a.moveTo(sp.getX() + 0.5D, sp.getY(), sp.getZ() + 0.5D, level.random.nextFloat() * 360F, 0F);
                                a.setTarget(p);
                                level.addFreshEntity(a);
                                spawned++;
                            }
                        }
                    }
                    if (spawned > 0) {
                        p.displayClientMessage(Component.literal("§c[!] Засада! Рой выходит из тьмы."), true);
                        level.playSound(null, pp, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 0.8F);
                    }
                }
            }

            // --- Hive assault: mobs may emerge from already contaminated ground.
            // This event never writes contamination around the player; world spread
            // is handled globally and deterministically by WorldContaminationManager. ---
            if (level.isNight() && level.getGameTime() % 900 == 0 && level.random.nextFloat() < 0.10F
                    && SurvivalManager.getDay(level) >= 3
                    && !InvasionManager.get(level).isVictoryAchieved()) {
                for (ServerPlayer p : level.players()) {
                    BlockPos pp = p.blockPosition();
                    int infected = 0;
                    for (int i = 0; i < 24; i++) {
                        int rx = pp.getX() + level.random.nextInt(25) - 12;
                        int rz = pp.getZ() + level.random.nextInt(25) - 12;
                        int ry = level.getHeight(Heightmap.Types.MOTION_BLOCKING, rx, rz) - 1;
                        BlockPos gp = new BlockPos(rx, ry, rz);
                        if (ContaminationRules.isContaminated(level.getBlockState(gp))) infected++;
                    }
                    if (infected > 0) {
                        for (int i = 0; i < 3; i++) {
                            double x = p.getX() + (level.random.nextDouble() - 0.5D) * 18.0D;
                            double z = p.getZ() + (level.random.nextDouble() - 0.5D) * 18.0D;
                            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
                            BlockPos sp = new BlockPos((int) x, y, (int) z);
                            if (level.getBlockState(sp).isAir()) {
                                AlienGruntEntity g = EntityRegistry.ALIEN_GRUNT.create(level);
                                if (g != null) {
                                    g.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
                                    g.setTarget(p);
                                    level.addFreshEntity(g);
                                    // It CLAWS OUT of the rotten ground, not out of thin air.
                                    level.levelEvent(2001, sp.below(), net.minecraft.world.level.block.Block.getId(
                                            level.getBlockState(sp.below())));
                                }
                            }
                        }
                        for (int wi = 0; wi < 2; wi++) {
                            double wx = p.getX() + (level.random.nextDouble() - 0.5D) * 14.0D;
                            double wz = p.getZ() + (level.random.nextDouble() - 0.5D) * 14.0D;
                            int wy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) wx, (int) wz);
                            com.example.alieninvasion.entity.InfestedWormEntity wormSpawn =
                                    EntityRegistry.INFESTED_WORM.create(level);
                            if (wormSpawn != null) {
                                wormSpawn.moveTo(wx, wy, wz, level.random.nextFloat() * 360F, 0F);
                                wormSpawn.setStage(SurvivalManager.getDay(level) >= 6 ? 1 : 0);
                                wormSpawn.setTarget(p);
                                level.addFreshEntity(wormSpawn);
                            }
                        }
                        p.displayClientMessage(Component.literal("§5[!] Волна заражения роя! Земля гниёт вокруг."), false);
                        level.playSound(null, pp, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.7F);
                    }
                }
            }

            // Acid Rain (magic damage + grass spread)
            if (level.isRaining() && level.getGameTime() % 40 == 0) {
                for (ServerPlayer player : level.players()) {
                    if (level.canSeeSky(player.blockPosition())) {
                        player.hurt(level.damageSources().magic(), 1.0F);
                        player.displayClientMessage(Component.literal("§c[!] Кислотный дождь обжигает вас! Найдите укрытие."), true);
                    }
                }

                // Speed up grass block conversion to INFESTED_GRASS
                for (ServerPlayer player : level.players()) {
                    for (int i = 0; i < 5; i++) { // infect 5 random spots per player
                        int rx = player.blockPosition().getX() + level.random.nextInt(33) - 16;
                        int rz = player.blockPosition().getZ() + level.random.nextInt(33) - 16;
                        int ry = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, rx, rz) - 1;
                        BlockPos pos = new BlockPos(rx, ry, rz);
                        if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                            level.setBlockAndUpdate(pos, ModBlocks.INFESTED_GRASS.defaultBlockState());
                        }
                    }
                }
            }

            // Player ticks (Bleeding, Radiation, Hallucinations)
            for (ServerPlayer player : level.players()) {
                 // Tick local player EMP grenade effect
                 if (player.getTags().contains("EmpActive") && empTicksActive <= 0) {
                     int localEmp = PLAYER_EMP_TICKS.getOrDefault(player.getUUID(), 0);
                     if (localEmp > 0) {
                         localEmp--;
                         if (localEmp <= 0) {
                             player.removeTag("EmpActive");
                             PLAYER_EMP_TICKS.remove(player.getUUID());
                             player.displayClientMessage(Component.literal("§a[!] Ваши приборы снова работают."), true);
                         } else {
                             PLAYER_EMP_TICKS.put(player.getUUID(), localEmp);
                         }
                     }
                 }

                 // Parasite helmet effect: FORCED CONTROL. While the brain-worm rides
                 // your skull you behave like the swarm - sped up, slapping down random
                 // junk blocks, lunging at everything alive, and thrashing/self-harming
                 // when there's nothing to hit. Can't be pried off for the first 5s.
                 if (player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.PARASITE_ITEM)) {
                     long now = level.getGameTime();
                     PARASITE_ATTACH.putIfAbsent(player.getUUID(), now);

                     // The parasite supercharges its host.
                     player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1, true, false));

                     // Disorient only - the parasite deals NO direct damage, it just
                     // hijacks control.
                     if (player.tickCount % 25 == 0) {
                         player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 120, 0, false, false));
                         if (level.random.nextFloat() < 0.5F) {
                             player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 50, 0, false, false));
                         }
                     }

                     // FORCED ATTACK: lunge at the nearest living thing - players, mobs,
                     // animals, villagers - anything that isn't the host.
                     if (player.tickCount % 15 == 0) {
                         LivingEntity victim = null;
                         double best = Double.MAX_VALUE;
                         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0D),
                                 e -> e != player && e.isAlive())) {
                             double d = player.distanceToSqr(e);
                             if (d < best) { best = d; victim = e; }
                         }
                         if (victim != null) {
                             net.minecraft.world.phys.Vec3 to = victim.position().subtract(player.position());
                             if (to.lengthSqr() > 0.01D) {
                                 to = to.normalize().scale(0.6D);
                                 player.setDeltaMovement(to.x, player.getDeltaMovement().y, to.z);
                                 player.hurtMarked = true;
                             }
                             if (best < 9.0D) {
                                 victim.hurt(level.damageSources().playerAttack(player), 4.0F);
                                 player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                             }
                         } else {
                             // NO TARGETS: thrash and leap around (still no direct damage).
                             player.setDeltaMovement((level.random.nextDouble() - 0.5D) * 1.2D, 0.42D,
                                     (level.random.nextDouble() - 0.5D) * 1.2D);
                             player.hurtMarked = true;
                         }
                     }

                     // BUILD COMPULSIVELY: slap down a random block FROM YOUR OWN
                     // inventory nearby (consuming it), like a swarm worker.
                     if (player.tickCount % 30 == 0 && level.random.nextFloat() < 0.5F) {
                         net.minecraft.world.entity.player.Inventory inv = player.getInventory();
                         java.util.List<Integer> blockSlots = new java.util.ArrayList<>();
                         for (int i = 0; i < inv.getContainerSize(); i++) {
                             ItemStack s = inv.getItem(i);
                             if (!s.isEmpty() && s.getItem() instanceof net.minecraft.world.item.BlockItem) {
                                 blockSlots.add(i);
                             }
                         }
                         if (!blockSlots.isEmpty()) {
                             ItemStack s = inv.getItem(blockSlots.get(level.random.nextInt(blockSlots.size())));
                             BlockPos bp = player.blockPosition().offset(level.random.nextInt(3) - 1,
                                     level.random.nextInt(2), level.random.nextInt(3) - 1);
                             if (level.getBlockState(bp).isAir()
                                     && level.getBlockState(bp.below()).isSolidRender(level, bp.below())) {
                                 net.minecraft.world.level.block.Block blk =
                                         ((net.minecraft.world.item.BlockItem) s.getItem()).getBlock();
                                 level.setBlockAndUpdate(bp, blk.defaultBlockState());
                                 s.shrink(1);
                             }
                         }
                     }

                     // Drop your gear in the chaos.
                     if (player.tickCount % 100 == 0 && level.random.nextFloat() < 0.35F) {
                         ItemStack held = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                         if (!held.isEmpty()) {
                             player.drop(held.copy(), true, false);
                             player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                         }
                     }

                     if (player.tickCount % 20 == 0) {
                         long held = now - PARASITE_ATTACH.getOrDefault(player.getUUID(), now);
                         if (held < 100) {
                             player.displayClientMessage(Component.literal("§c[!] Паразит впился в мозг! Не снять ещё ~" + ((100 - held) / 20 + 1) + "с"), true);
                         } else {
                             player.displayClientMessage(Component.literal("§e[!] Хватка слабеет - снимите паразита со слота шлема!"), true);
                         }
                     }
                 } else {
                     // 5-SECOND REMOVAL LOCK: pried off too early -> it clamps back on.
                     Long attach = PARASITE_ATTACH.get(player.getUUID());
                     if (attach != null) {
                         if (level.getGameTime() - attach < 100) {
                             ItemStack swapped = player.getItemBySlot(EquipmentSlot.HEAD).copy();
                             player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PARASITE_ITEM));
                             if (!swapped.isEmpty()) {
                                 player.getInventory().placeItemBackInInventory(swapped);
                             }
                             player.displayClientMessage(Component.literal("§c[!] Паразит вцепился обратно! Ещё рано..."), true);
                         } else {
                             PARASITE_ATTACH.remove(player.getUUID());
                         }
                     }
                 }

                 // Gravity Boots: low-gravity float + high jumps while worn (server-side,
                 // so they actually work in survival). Fall immunity is in ALLOW_DAMAGE.
                 if (player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.GRAVITY_BOOTS)
                         && !player.getTags().contains("EmpActive")) {
                     player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 30, 0, true, false));
                     player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, 30, 1, true, false));
                 }

                 // Proximity Mimic trigger
                 List<LivingEntity> mimics = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.0D),
                     e -> e.getTags().contains("IsMimic") && e.isAlive());
                 if (!mimics.isEmpty()) {
                     triggerMimicMorph((ServerLevel) level, mimics.get(0), player);
                 }

                // --- Bleeding: a low-HP player bleeds, dripping a fading blood trail,
                // and the scent draws monsters AND aliens to them from afar. ---
                if (player.getHealth() < player.getMaxHealth() * 0.5f && !player.getAbilities().invulnerable) {
                    boolean heavy = player.getHealth() < player.getMaxHealth() * 0.25f;

                    if (player.tickCount % 5 == 0) {
                        level.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                player.getX(), player.getY() + 0.1D, player.getZ(),
                                heavy ? 6 : 3, 0.12, 0.02, 0.12, 0.0);
                    }

                    // Drop a fading blood pool at the feet (trail when moving, a puddle
                    // when standing in heavy bleed). Throttled so it never spams blocks.
                    boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.002D;
                    int cadence = heavy ? 8 : 18;
                    if (player.tickCount % cadence == 0 && (moving || heavy)) {
                        com.example.alieninvasion.block.BloodyBlocks.splatter(level, player.blockPosition().below());
                    }

                    // BLOOD SCENT: nearby hostiles + aliens with no target lock onto the
                    // bleeding player (they smell it, even through walls). Wider when heavy.
                    if (player.tickCount % 20 == 0) {
                        double scent = heavy ? 32.0D : 20.0D;
                        for (Mob hunter : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(scent),
                                e -> e.getTarget() == null
                                        && !e.getTags().contains("PlayerParasiteAlly")
                                        && (e instanceof net.minecraft.world.entity.monster.Enemy
                                            || AlienUtils.isAlliedTo(null, e))
                                        && !AlienUtils.isAlliedTo(e, player))) {
                            hunter.setTarget(player);
                        }
                        if (heavy) {
                            player.displayClientMessage(Component.literal(
                                    "§4🩸 Вы истекаете кровью — рой идёт на запах!"), true);
                        }
                    }
                }

                // Mind-controlled parasite thralls: retarget them at the nearest enemy
                // (hostiles + aliens), never at players, so they actually fight FOR you.
                if (player.tickCount % 20 == 0) {
                    for (Mob ally : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(40.0D),
                            e -> e.getTags().contains("PlayerParasiteAlly") && e.isAlive())) {
                        LivingEntity cur = ally.getTarget();
                        if (cur == null || cur instanceof Player || !cur.isAlive()
                                || cur.getTags().contains("PlayerParasiteAlly")) {
                            LivingEntity foeBest = null;
                            double best = Double.MAX_VALUE;
                            for (Mob foe : level.getEntitiesOfClass(Mob.class, ally.getBoundingBox().inflate(20.0D),
                                    e -> e != ally && e.isAlive() && !e.getTags().contains("PlayerParasiteAlly")
                                            && (e instanceof net.minecraft.world.entity.monster.Enemy
                                                || AlienUtils.isAlliedTo(null, e)))) {
                                double d = ally.distanceToSqr(foe);
                                if (d < best) { best = d; foeBest = foe; }
                            }
                            ally.setTarget(foeBest);
                        }
                        if (level.random.nextInt(3) == 0) {
                            level.sendParticles(ParticleTypes.WARPED_SPORE, ally.getX(), ally.getEyeY(), ally.getZ(),
                                    2, 0.2D, 0.3D, 0.2D, 0.0D);
                        }
                    }
                }

                // GEIGER COUNTER: just having it in the inventory makes it tick.
                // Click bursts scale with the measured field - the closer you are to
                // a source, the more frantic the crackle. A lone random click now and
                // then is natural background. This sound (not a HUD bar) is how you
                // "see" radiation without the counter equipped.
                if (player.tickCount % 2 == 0 && player.getInventory().contains(
                        new ItemStack(ItemRegistry.GEIGER_COUNTER))) {
                    float fieldLevel = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(player);
                    // ALWAYS-ON BACKGROUND: a real counter never goes fully silent -
                    // lone random ticks at rest rising to a frantic crackle near a
                    // source. Sharp dry clicks, loud enough to actually hear.
                    float chance = 0.012F + fieldLevel * 0.045F;
                    if (level.random.nextFloat() < chance) {
                        int burst = 1 + (fieldLevel >= 18.0F ? level.random.nextInt(3)
                                : fieldLevel >= 9.0F ? level.random.nextInt(2) : 0);
                        for (int c = 0; c < burst; c++) {
                            level.playSound(null, player.blockPosition(),
                                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS,
                                    0.55F, 1.8F + level.random.nextFloat() * 0.35F);
                        }
                    }

                }

                // SELF-AWARENESS: your character KNOWS something is wrong long
                // before any meter maxes - quiet first-person thoughts, only yours.
                {
                    if (player.tickCount % 160 == 0 && !player.isCreative()) {
                        float selfDose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(player);
                        float selfInf = com.example.alieninvasion.logic.InfectionManager.getMeter(player);
                        String thought = null;
                        if (selfDose >= 75.0F) thought = "Во рту привкус металла... дёсны кровоточат. Лечение, СРОЧНО.";
                        else if (selfInf >= 75.0F) thought = "Под кожей что-то ШЕВЕЛИТСЯ. Я чувствую, как оно растёт.";
                        else if (selfDose >= 40.0F) thought = "Накатывает слабость, кожу покалывает... это облучение.";
                        else if (selfInf >= 50.0F) thought = "Меня лихорадит. Голод не отпускает, руки дрожат.";
                        else if (player.getHealth() < player.getMaxHealth() * 0.25F) thought = "Перед глазами плывёт... слишком много крови потеряно.";
                        else if (selfDose >= 15.0F) thought = "Лёгкая тошнота. Где-то рядом что-то фонит.";
                        else if (selfInf >= 25.0F) thought = "Горло саднит, знобит. Кажется, я что-то подхватил.";
                        if (thought != null) {
                            player.displayClientMessage(Component.literal("§7§o" + thought), true);
                        }
                    }
                }

                // Radiation check (dose-based survival mechanic - see RadiationManager)
                if (player.tickCount % 20 == 0) {
                    com.example.alieninvasion.logic.RadiationManager.tickPlayer(level, player);
                    // Block-distance radiation from Pure Radiation Blocks (spec model).
                    com.example.alieninvasion.logic.RadiationFieldManager.tickPlayer(level, player);
                    radiatePeacefulEntities(level, player);

                    // Psychic Pressure check (Tyrant, Telekinetic, Hive)
                    boolean nearTyrant = !level.getEntitiesOfClass(com.example.alieninvasion.entity.HiveTyrantEntity.class, player.getBoundingBox().inflate(32.0D)).isEmpty();
                    boolean nearTele = !level.getEntitiesOfClass(com.example.alieninvasion.entity.TelekineticAlienEntity.class, player.getBoundingBox().inflate(32.0D)).isEmpty();
                    boolean nearHive = false;
                    BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
                    BlockPos ppos = player.blockPosition();
                    for (int x = -16; x <= 16; x += 2) {
                        for (int y = -8; y <= 8; y += 2) {
                            for (int z = -16; z <= 16; z += 2) {
                                mut.set(ppos.getX() + x, ppos.getY() + y, ppos.getZ() + z);
                                if (level.getBlockState(mut).is(ModBlocks.ALIEN_HIVE)) {
                                    nearHive = true;
                                    break;
                                }
                            }
                            if (nearHive) break;
                        }
                        if (nearHive) break;
                    }

                    if (nearTyrant || nearTele || nearHive) {
                        player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PSYCHIC_PRESSURE), 100, 0, false, true));
                    }
                }

                // Armor set bonuses: radiation/infection fill rate multipliers.
                boolean fullPlatinum = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.PLATINUM_HELMET)
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.PLATINUM_CHESTPLATE)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.PLATINUM_LEGGINGS)
                        && player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.PLATINUM_BOOTS);
                boolean fullPalladium = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.PALLADIUM_HELMET)
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.PALLADIUM_CHESTPLATE)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.PALLADIUM_LEGGINGS)
                        && player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.PALLADIUM_BOOTS);
                boolean fullHazmat = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.ALIEN_HAZMAT_HELMET)
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.ALIEN_HAZMAT_CHESTPLATE)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.ALIEN_HAZMAT_LEGGINGS)
                        && player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.ALIEN_HAZMAT_BOOTS);
                boolean fullChem = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.ALIEN_CHEM_HELMET)
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.ALIEN_CHEM_CHESTPLATE)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.ALIEN_CHEM_LEGGINGS)
                        && player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.ALIEN_CHEM_BOOTS);

                // Dose multiplier: chem(×5 slower) > hazmat(×3 slower) > platinum(×2 slower) > default
                // ANY armor shields a little - even a shirt stops some fallout.
                // Per piece: helmet 3%, chest 5%, legs 4%, boots 3% => a full set of
                // ANY armor (even leather) blocks 15%. Dedicated suits still rule.
                float anyArmorShield = 0.0F;
                if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty())  anyArmorShield += 0.03F;
                if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) anyArmorShield += 0.05F;
                if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty())  anyArmorShield += 0.04F;
                if (!player.getItemBySlot(EquipmentSlot.FEET).isEmpty())  anyArmorShield += 0.03F;
                float doseMult = fullChem ? 0.2F : fullHazmat ? (1.0F / 3.0F) : fullPlatinum ? 0.5F
                        : (1.0F - anyArmorShield);
                // Meter multiplier: chem(×5 slower) > hazmat(×3 slower) > palladium(×2 slower) > default
                float meterMult = fullChem ? 0.2F : fullHazmat ? (1.0F / 3.0F) : fullPalladium ? 0.5F : 1.0F;
                com.example.alieninvasion.logic.RadiationManager.setDoseMultiplier(player, doseMult);
                com.example.alieninvasion.logic.InfectionManager.setMeterMultiplier(player, meterMult);
                if (fullPlatinum) {
                    com.example.alieninvasion.logic.RadiationManager.capDose(player, 70.0F);
                }
                if (fullPalladium) {
                    com.example.alieninvasion.logic.InfectionManager.capMeter(player, 70.0F);
                }

                // Cosmic Armor set bonus + alien-block hazard.
                boolean fullCosmic = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.COSMIC_HELMET)
                        && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemRegistry.COSMIC_CHESTPLATE)
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ItemRegistry.COSMIC_LEGGINGS)
                        && player.getItemBySlot(EquipmentSlot.FEET).is(ItemRegistry.COSMIC_BOOTS);
                boolean onAlienGround = isAlienGround(level.getBlockState(player.blockPosition().below()))
                        || isAlienGround(level.getBlockState(player.blockPosition()));
                if (fullCosmic) {
                    // Full immunity to infection and radiation
                    player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
                    com.example.alieninvasion.logic.RadiationManager.clearDose(player);
                    com.example.alieninvasion.logic.RadiationManager.removeAllDoseEffects(player);
                    com.example.alieninvasion.logic.RadiationManager.SCREEN_GLITCH.remove(player.getUUID());
                    // SET BONUS: the full Cosmic suit is the late-game power reward - it
                    // shrugs off blows, mends the wearer and lets them see in the dark, so
                    // facing the strengthened swarm finally feels winnable.
                    if (player.tickCount % 20 == 0) {
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false));
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 60, 0, true, false));
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 300, 0, true, false));
                    }
                    if (onAlienGround && player.tickCount % 20 == 0) {
                        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
                        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                                EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                            ItemStack piece = player.getItemBySlot(slot);
                            if (piece.getItem() instanceof ArmorItem) {
                                piece.hurtAndBreak(1, player, slot); // safe passage costs durability
                            }
                        }
                    }
                }

                // Infection is now a DOSE-style meter (see InfectionManager): you only
                // start accruing it after standing on contaminated ground for ~2s, and
                // it builds a visible scale instead of infecting the instant you touch it.
                if (player.tickCount % 20 == 0) {
                    boolean infImmune = fullCosmic;
                    com.example.alieninvasion.logic.InfectionManager.tickPlayer(
                            level, player, onAlienGround, infImmune);
                }

                // BUNKER SANCTUARY: while the survivor-trader lives, his chunk stays
                // inert - the one place the corruption cannot take.
                if (player.tickCount % 100 == 0) {
                    for (net.minecraft.world.entity.npc.Villager trader : level.getEntitiesOfClass(
                            net.minecraft.world.entity.npc.Villager.class, player.getBoundingBox().inflate(48.0D),
                            v -> v.isAlive() && v.getTags().contains("BunkerTrader"))) {
                        var cdata = com.example.alieninvasion.logic.ChunkContaminationData.get(level);
                        var vcp = new net.minecraft.world.level.ChunkPos(trader.blockPosition());
                        if (!cdata.isInert(vcp)) cdata.setInert(vcp, true);
                    }
                }

                // Day 3+: VILLAGE ASSIMILATION. Villagers whose homes the corruption
                // has reached don't just die - they turn. A villager standing on
                // infested ground twists into an infested zombie on the spot, so a
                // rotten village becomes a swarm outpost. Stops after the victory —
                // the swarm is fleeing, it no longer takes hosts.
                if (player.tickCount % 100 == 0
                        && com.example.alieninvasion.logic.SurvivalManager.getDay(level) >= 3
                        && !InvasionManager.get(level).isVictoryAchieved()) {
                    for (net.minecraft.world.entity.npc.Villager villager : level.getEntitiesOfClass(
                            net.minecraft.world.entity.npc.Villager.class, player.getBoundingBox().inflate(28.0D),
                            v -> v.isAlive() && !v.hasCustomName())) {
                        if (!isAlienGround(level.getBlockState(villager.blockPosition().below()))
                                && !isAlienGround(level.getBlockState(villager.blockPosition()))) {
                            continue;
                        }
                        if (level.random.nextFloat() > 0.30F) continue; // gradual, not instant
                        com.example.alieninvasion.entity.InfestedZombieEntity turned =
                                EntityRegistry.INFESTED_ZOMBIE.create(level);
                        if (turned != null) {
                            turned.moveTo(villager.getX(), villager.getY(), villager.getZ(),
                                    villager.getYRot(), 0.0F);
                            turned.setPersistenceRequired();
                            level.sendParticles(ParticleTypes.SCULK_SOUL,
                                    villager.getX(), villager.getY() + 1.0D, villager.getZ(),
                                    25, 0.4D, 0.8D, 0.4D, 0.02D);
                            level.playSound(null, villager.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                                    SoundSource.HOSTILE, 1.0F, 0.7F);
                            villager.discard();
                            level.addFreshEntity(turned);
                        }
                    }
                }

                // Day 4+: corrupted ground rejects peaceful life. Wild animals that
                // stray onto infested terrain wither away (named/leashed pets spared,
                // and farms on clean ground are untouched).
                if (player.tickCount % 100 == 0
                        && com.example.alieninvasion.logic.SurvivalManager.getDay(level) >= 5) {
                    for (net.minecraft.world.entity.animal.Animal animal : level.getEntitiesOfClass(
                            net.minecraft.world.entity.animal.Animal.class, player.getBoundingBox().inflate(24.0D),
                            a -> a.isAlive() && !a.hasCustomName() && !a.isLeashed())) {
                        if (isAlienGround(level.getBlockState(animal.blockPosition().below()))) {
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                    animal.getX(), animal.getY() + 0.5D, animal.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.01D);
                            animal.discard();
                        }
                    }
                }

                // Psychic Hallucinations: Spawn phantom creepers + play ambient illusions
                if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PSYCHIC_PRESSURE))) {
                    if (level.random.nextInt(300) == 0) { // ~every 15 seconds
                        BlockPos spawnPos = player.blockPosition().offset(level.random.nextInt(17) - 8, level.random.nextInt(5) - 2, level.random.nextInt(17) - 8);
                        if (level.getBlockState(spawnPos).isAir() && level.getBlockState(spawnPos.below()).isCollisionShapeFullBlock(level, spawnPos.below())) {
                            Creeper creeper = EntityType.CREEPER.create(level);
                            if (creeper != null) {
                                creeper.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                                creeper.addTag("IsPhantom");
                                creeper.setSilent(true);
                                creeper.setCustomName(Component.literal("Фантомный крипер"));
                                creeper.setTarget(player);
                                level.addFreshEntity(creeper);
                            }
                        }
                        // Auditory hallucination
                        level.playSound(null, player.blockPosition(), level.random.nextBoolean() ? SoundEvents.CREEPER_PRIMED : SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.8F, 0.8F);
                    }
                }

                // Clean up Phantom Creepers that got close to this player
                for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(8.0D), c -> c.getTags().contains("IsPhantom"))) {
                    if (creeper.distanceToSqr(player) <= 4.0D) {
                        level.sendParticles(ParticleTypes.POOF, creeper.getX(), creeper.getY() + 1.0D, creeper.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
                        level.playSound(null, creeper.blockPosition(), SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.HOSTILE, 1.0F, 1.2F);
                        creeper.discard();
                    }
                }
            }

            if (level.getGameTime() % 400 != 0) return; // Every 20 seconds
            // Invasion over (boss dead): stop all worker/mimic/parasite/UFO spawning.
            if (InvasionManager.get(level).isVictoryAchieved()) return;

            // Day Worker Spawning: by day the swarm sends out "workers" that mine
            // resources, haul them to a stash and raise infested huts (see the
            // scavenger goals on the grunt). Capped so they don't pile up idle.
            if (level.isDay()) {
                if (level.random.nextFloat() < 0.10f) { // 10% chance every 20 seconds
                    for (ServerPlayer player : level.players()) {
                        int nearbyWorkers = level.getEntitiesOfClass(AlienGruntEntity.class,
                                player.getBoundingBox().inflate(48.0D), AlienGruntEntity::isScavenger).size();
                        if (nearbyWorkers >= 5) {
                            continue;
                        }
                        int squadSize = 1 + level.random.nextInt(2); // 1-2 workers
                        for (int i = 0; i < squadSize; i++) {
                            double x = player.getX() + (level.random.nextDouble() - 0.5D) * 40.0D;
                            double z = player.getZ() + (level.random.nextDouble() - 0.5D) * 40.0D;
                            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
                            BlockPos pos = new BlockPos((int) x, y, (int) z);

                            if (level.isLoaded(pos) && level.getBlockState(pos).isAir()) {
                                AlienGruntEntity grunt = EntityRegistry.ALIEN_GRUNT.create(level);
                                if (grunt != null) {
                                    grunt.moveTo(x, y + 1.0D, z, level.random.nextFloat() * 360.0F, 0.0F);
                                    grunt.setScavenger(true);
                                    grunt.setCustomName(Component.literal("§aПришелец-рабочий"));
                                    grunt.setCustomNameVisible(true);
                                    level.addFreshEntity(grunt);
                                }
                            }
                        }
                    }
                }
            }

            // Day Mimic Spawning - only once the swarm starts infiltrating (day 3+).
            if (level.isDay() && level.getGameTime() % 600 == 0 && level.random.nextFloat() < 0.15F
                    && SurvivalManager.getDay(level) >= 3) {
                for (ServerPlayer player : level.players()) {
                    double mx = player.getX() + (level.random.nextDouble() - 0.5D) * 40.0D;
                    double mz = player.getZ() + (level.random.nextDouble() - 0.5D) * 40.0D;
                    int my = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) mx, (int) mz);
                    BlockPos mpos = new BlockPos((int) mx, my, (int) mz);

                    if (level.isLoaded(mpos) && level.getBlockState(mpos).isAir()) {
                        LivingEntity mimic;
                        float r = level.random.nextFloat();
                        if (r < 0.35F) {
                            mimic = EntityType.COW.create(level);
                        } else if (r < 0.7F) {
                            mimic = EntityType.SHEEP.create(level);
                        } else {
                            mimic = EntityType.VILLAGER.create(level);
                        }

                        if (mimic != null) {
                            mimic.moveTo(mx, my + 1.0D, mz, level.random.nextFloat() * 360.0F, 0.0F);
                            mimic.addTag("IsMimic");
                            level.addFreshEntity(mimic);
                        }
                    }
                }
            }
            
            // Night Parasite Spawning: fast brain-leeches scuttle out of the dark
            // and sprint/leap straight at a player to latch onto their head.
            if (level.isNight() && level.getGameTime() % 300 == 0 && level.random.nextFloat() < 0.30F
                    && SurvivalManager.getDay(level) >= 2) {
                for (ServerPlayer player : level.players()) {
                    double px = player.getX() + (level.random.nextDouble() - 0.5D) * 28.0D;
                    double pz = player.getZ() + (level.random.nextDouble() - 0.5D) * 28.0D;
                    int py = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) px, (int) pz);
                    BlockPos ppos = new BlockPos((int) px, py, (int) pz);
                    if (level.isLoaded(ppos) && level.getBlockState(ppos).isAir()) {
                        com.example.alieninvasion.entity.ParasiteEntity parasite = EntityRegistry.PARASITE.create(level);
                        if (parasite != null) {
                            parasite.moveTo(px, py, pz, level.random.nextFloat() * 360F, 0F);
                            parasite.setTarget(player);
                            level.addFreshEntity(parasite);
                            // Wriggles up out of the soil.
                            level.levelEvent(2001, ppos.below(), net.minecraft.world.level.block.Block.getId(
                                    level.getBlockState(ppos.below())));
                        }
                    }
                }
            }

            if (SurvivalManager.isAlienInvasionActive(level) && SurvivalManager.getDay(level) >= 2) {
                int day = SurvivalManager.getDay(level);
                for (ServerPlayer player : level.players()) {
                    // Кап только по пришельцам: общий getEntities() считал дропы,
                    // стрелы и животных — ферма предметов глушила спавн UFO.
                    int nearbyAliens = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            player.getBoundingBox().inflate(100),
                            e -> com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, e)).size();

                    if (level.random.nextFloat() < (0.02f + (day * 0.01f)) && nearbyAliens < 30) {
                        double x = player.getX() + (level.random.nextDouble() - 0.5) * 60;
                        double z = player.getZ() + (level.random.nextDouble() - 0.5) * 60;
                        double y = player.getY() + 35 + level.random.nextInt(15);
                        if (y > 315) y = 315;
                        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
                        if (level.isLoaded(pos) && level.getBlockState(pos).isAir()) {
                            UfoEntity ufo = EntityRegistry.UFO.create(level);
                            if (ufo != null) {
                                ufo.moveTo(x, y, z, 0, 0);
                                level.addFreshEntity(ufo);
                            }
                        }
                    }
                }
            }
        });
    }

    private static boolean isAlienGround(net.minecraft.world.level.block.state.BlockState state) {
        return ContaminationRules.isContaminated(state)
                || state.is(ModBlocks.ALIEN_RESIDUE)
                || state.is(ModBlocks.ALIEN_HIVE)
                || state.is(ModBlocks.ALIEN_STASH);
    }

    private static boolean hasFullHazmat(LivingEntity entity) {
        return com.example.alieninvasion.logic.ArmorProtection.hasSealedSuit(entity);
    }

    private static boolean isRadiationSource(BlockState state) {
        return state.is(ModBlocks.PURE_RADIATION_BLOCK)
                || state.is(ModBlocks.TOXIC_BARREL)
                || state.is(ModBlocks.TOXIC_WATER);
    }

    private static boolean isNearRadiationSource(ServerLevel level, BlockPos pos, int horizontal, int vertical) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    mut.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (isRadiationSource(level.getBlockState(mut))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void radiatePeacefulEntities(ServerLevel level, ServerPlayer anchor) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, anchor.getBoundingBox().inflate(10.0D),
                e -> e != anchor && e.isAlive() && (e instanceof Animal || e instanceof AbstractVillager))) {
            if (com.example.alieninvasion.logic.ArmorProtection.isRadiationImmune(living)) {
                living.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
                living.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
                continue;
            }
            if (isNearRadiationSource(level, living.blockPosition(), 5, 3)) {
                living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION), 100, 0, false, true));
                living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80, 0, false, true));
            }
        }
    }

    private static void dropAlienLoot(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        var rng = entity.level().random;
        boolean isAlien = entity instanceof AlienGruntEntity
                || entity instanceof com.example.alieninvasion.entity.AlienBruteEntity
                || entity instanceof com.example.alieninvasion.entity.AlienChickenEntity
                || entity instanceof com.example.alieninvasion.entity.TelekineticAlienEntity
                || entity instanceof com.example.alieninvasion.entity.AlienTrollEntity
                || entity instanceof com.example.alieninvasion.entity.HiveTyrantEntity
                || entity instanceof com.example.alieninvasion.entity.AlienStalkerEntity
                || entity instanceof com.example.alieninvasion.entity.PlasmaCasterEntity
                || entity instanceof com.example.alieninvasion.entity.HiveShamanEntity
                || entity instanceof com.example.alieninvasion.entity.AlienBreacherEntity
                || entity instanceof com.example.alieninvasion.entity.CaveLurkerEntity
                || entity instanceof com.example.alieninvasion.entity.SwarmMotherEntity
                || entity instanceof UfoEntity;
        if (!isAlien) {
            return;
        }
        entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 4 + rng.nextInt(2)));
        if (rng.nextFloat() < 0.15f) {
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.ALIEN_BATTERY));
        }
        // Tyrant boss jackpot
        if (entity instanceof com.example.alieninvasion.entity.HiveTyrantEntity) {
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.HIVE_CORE, 1 + rng.nextInt(2)));
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 2 + rng.nextInt(3)));
        }
        // Swarm Mother: final boss reward
        if (entity instanceof com.example.alieninvasion.entity.SwarmMotherEntity) {
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.HIVE_CORE, 3 + rng.nextInt(3)));
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.DARK_MATTER_SHARD, 2 + rng.nextInt(3)));
            entity.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 4 + rng.nextInt(5)));
        }
    }

    private static void spawnGrunt(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel) {
            AlienGruntEntity grunt = EntityRegistry.ALIEN_GRUNT.create(level);
            if (grunt != null) {
                grunt.moveTo(x, y, z, 0, 0);
                level.addFreshEntity(grunt);
            }
        }
    }

    public static void triggerMimicMorph(ServerLevel level, LivingEntity mimic, Player player) {
        if (!mimic.isAlive()) return;
        BlockPos pos = mimic.blockPosition();

        level.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5F, 1.3F);
        level.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.5F, 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mimic.getX(), mimic.getY() + 0.5, mimic.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

        // A mimic is an infiltrator, NOT a heavy. It only ever hides a weak
        // ambusher: a grunt, a raptor, or (late game) a wall-breacher. Never a
        // telekinetic or a brute - those don't sneak around as livestock.
        int mimicDay = SurvivalManager.getDay(level);
        net.minecraft.world.entity.Mob alien;
        float r = level.random.nextFloat();
        if (mimicDay >= 5 && r < 0.25F) {
            alien = EntityRegistry.ALIEN_BREACHER.create(level);
        } else if (r < 0.55F) {
            alien = EntityRegistry.ALIEN_RAPTOR.create(level);
        } else {
            alien = EntityRegistry.ALIEN_GRUNT.create(level);
        }

        if (alien != null) {
            alien.moveTo(mimic.getX(), mimic.getY(), mimic.getZ(), mimic.getYRot(), mimic.getXRot());
            alien.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            alien.setTarget(player);
            level.addFreshEntity(alien);
        }

        mimic.discard();
        player.displayClientMessage(Component.literal("§c[!] Существо оказалось мимиком роя!"), true);
    }

    private static boolean isInfested(net.minecraft.world.level.block.state.BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!key.getNamespace().equals("alien-invasion")) {
            return false;
        }
        String path = key.getPath();
        return path.contains("infested") || path.contains("infected") 
            || path.contains("bloody") || path.equals("blood_pool") 
            || path.startsWith("alien_");
    }
}
