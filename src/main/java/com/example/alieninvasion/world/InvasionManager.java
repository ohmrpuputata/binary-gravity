package com.example.alieninvasion.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class InvasionManager extends SavedData {
    private static final String DATA_NAME = "alien_invasion_manager";
    private int invasionDays = 0;
    /**
     * Minecraft days per invasion stage. MUST stay 1: the HUD, the world
     * contamination and every day-gated event read the world-time day, so a
     * slower stage clock here desyncs the announced day from what the player
     * actually sees happening to the world.
     */
    private static final int MC_DAYS_PER_STAGE = 1;
    private long lastDayTime = 0;
    private boolean victoryAchieved = false;
    private boolean retreatComplete = false;
    private int retreatZeroTicks = 0;
    private int retreatSeconds = 0; // не сохраняется: после рестарта отсчёт честно начинается заново
    private int lastSiegeDay = 0;

    // --- Финальный акт: портал -> охотник -> реактор -> исцеление мира ---
    private net.minecraft.core.BlockPos overworldPortalPos = null;
    private boolean hunterArrived = false;
    private int hunterTimer = 0;
    private boolean planetDestroyed = false;
    private int planetTicks = 0;
    private boolean portalCollapsed = false;
    private boolean healingDone = false;
    private int healingCleanStreak = 0; // секунды подряд с чистотой >= 99%
    private final java.util.Set<java.util.UUID> creditsShown = new java.util.HashSet<>();
    // Зелёный босс-бар «Исцеление Земли: NN%» — виден, пока мир превращается обратно.
    private final net.minecraft.server.level.ServerBossEvent healingBar = new net.minecraft.server.level.ServerBossEvent(
            net.minecraft.network.chat.Component.literal("Исцеление Земли"),
            net.minecraft.world.BossEvent.BossBarColor.GREEN,
            net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    // День, с которого Маяк Роя позволяет призвать Мать Роя — победа достигается
    // ТОЛЬКО её убийством (см. SwarmMotherEntity.die -> triggerVictory), не выживанием.
    public static final int VICTORY_DAY = 8;

    // Swarm Adaptation counters
    private int bowHits = 0;
    private int fireDamage = 0;
    private int heavyArmorTicks = 0;

    public static InvasionManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(InvasionManager::create, InvasionManager::load, null),
                DATA_NAME);
    }

    public static InvasionManager create() {
        return new InvasionManager();
    }

    public static InvasionManager load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        InvasionManager data = new InvasionManager();
        data.invasionDays = tag.getInt("InvasionDays");
        data.lastDayTime = tag.getLong("LastDayTime");
        data.bowHits = tag.getInt("BowHits");
        data.fireDamage = tag.getInt("FireDamage");
        data.heavyArmorTicks = tag.getInt("HeavyArmorTicks");
        data.victoryAchieved = tag.getBoolean("VictoryAchieved");
        data.retreatComplete = tag.getBoolean("RetreatComplete");
        data.lastSiegeDay = tag.getInt("LastSiegeDay");
        data.hunterArrived = tag.getBoolean("HunterArrived");
        data.planetDestroyed = tag.getBoolean("PlanetDestroyed");
        data.planetTicks = tag.getInt("PlanetTicks");
        data.portalCollapsed = tag.getBoolean("PortalCollapsed");
        data.healingDone = tag.getBoolean("HealingDone");
        if (tag.contains("PortalX")) {
            data.overworldPortalPos = new net.minecraft.core.BlockPos(
                    tag.getInt("PortalX"), tag.getInt("PortalY"), tag.getInt("PortalZ"));
        }
        for (net.minecraft.nbt.Tag t : tag.getList("CreditsShown", net.minecraft.nbt.Tag.TAG_STRING)) {
            try {
                data.creditsShown.add(java.util.UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("InvasionDays", invasionDays);
        tag.putLong("LastDayTime", lastDayTime);
        tag.putInt("BowHits", bowHits);
        tag.putInt("FireDamage", fireDamage);
        tag.putInt("HeavyArmorTicks", heavyArmorTicks);
        tag.putBoolean("VictoryAchieved", victoryAchieved);
        tag.putBoolean("RetreatComplete", retreatComplete);
        tag.putInt("LastSiegeDay", lastSiegeDay);
        tag.putBoolean("HunterArrived", hunterArrived);
        tag.putBoolean("PlanetDestroyed", planetDestroyed);
        tag.putInt("PlanetTicks", planetTicks);
        tag.putBoolean("PortalCollapsed", portalCollapsed);
        tag.putBoolean("HealingDone", healingDone);
        if (overworldPortalPos != null) {
            tag.putInt("PortalX", overworldPortalPos.getX());
            tag.putInt("PortalY", overworldPortalPos.getY());
            tag.putInt("PortalZ", overworldPortalPos.getZ());
        }
        net.minecraft.nbt.ListTag credits = new net.minecraft.nbt.ListTag();
        for (java.util.UUID id : creditsShown) {
            credits.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put("CreditsShown", credits);
        return tag;
    }

    public void tick(ServerLevel level) {
        long time = level.getDayTime();
        this.lastDayTime = time;

        // FINALE: once the Swarm Mother is dead, the invasion is over - run the
        // evacuation (aliens flee, carriers lift them away), stop ALL spawning and
        // freeze the day counter so the world stops escalating after the victory.
        if (this.victoryAchieved) {
            tickRetreat(level);
            tickPostVictory(level);
            return;
        }

        // Comparing the target stage against invasionDays (not against elapsed time)
        // means the counter only ever moves UP - so existing saves keep the progress
        // they already reached and never regress, and "/invasion set" stays sticky.
        int targetStage = stageForDays((int) (time / 24000L));
        if (targetStage > invasionDays) {
            invasionDays = targetStage;
            level.getServer().getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component
                            .literal("§c[Вторжение] Начинается день " + invasionDays + "... Рой становится сильнее."),
                    false);
            this.setDirty();
        }

        // VICTORY: Now triggered by defeating the Mother of the Swarm boss on Day 8+.
        // if (!this.victoryAchieved && this.invasionDays >= VICTORY_DAY) {
        //     triggerVictory(level);
        // }

        // Nightly Spawning Logic called here
        if (!level.isDay()) {
            AlienSpawner.spawnerTick(level, invasionDays);
        }

        // Creeping infestation: the world is slowly converted to alien matter
        // around every player, faster as the invasion drags on.
        if (level.getGameTime() % 100L == 0L) {
            infestNearPlayers(level);
        }

        // SIEGE: every EVEN invasion day starting from day 6 (6, 8, 10...), at night,
        // the swarm deliberately storms RECLAIMED (purifier-protected) chunks -
        // holding territory means defending it.
        if (this.invasionDays >= 5 && this.invasionDays % 2 == 0 && this.invasionDays != this.lastSiegeDay
                && !level.isDay() && level.getGameTime() % 200L == 0L) {
            if (runSiege(level)) {
                this.lastSiegeDay = this.invasionDays;
                this.setDirty();
            }
        }

        // Orbital bombardment from day 5: meteors hammer surface players while
        // burrowing drills hunt anyone hiding deep underground. Day 4 already
        // introduces brutes and plasma casters - stacking the bombardment onto the
        // same day made the difficulty spike unfair, so it starts one day later.
        if (this.invasionDays >= 5 && level.getGameTime() % 600L == 0L) {
            orbitalStrikes(level);
        }
    }

    private void orbitalStrikes(ServerLevel level) {
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (level.random.nextFloat() > 0.33f) {
                continue;
            }
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.getBlockX(), player.getBlockZ());
            boolean deep = player.getBlockY() < surfaceY - 8;
            if (deep) {
                com.example.alieninvasion.entity.DrillEntity drill = com.example.alieninvasion.registry.EntityRegistry.DRILL
                        .create(level);
                if (drill != null) {
                    drill.setPos(player.getX(), surfaceY + 3, player.getZ());
                    drill.setDifficulty(this.invasionDays);
                    drill.setTargetY(player.getBlockY());
                    level.addFreshEntity(drill);
                }
            } else {
                // Land NEAR the player, not on their head - a wide scatter and a
                // near-vertical fall with only a slight drift, so it's a hazard to
                // dodge, not a guided missile.
                double ox = player.getX() + (level.random.nextDouble() - 0.5) * 60.0;
                double oz = player.getZ() + (level.random.nextDouble() - 0.5) * 60.0;
                double oy = surfaceY + 50;
                com.example.alieninvasion.entity.MeteorEntity meteor = com.example.alieninvasion.registry.EntityRegistry.METEOR
                        .create(level);
                if (meteor != null) {
                    meteor.setPos(ox, oy, oz);
                    meteor.setDifficulty(this.invasionDays);
                    meteor.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.3, -1.2,
                            (level.random.nextDouble() - 0.5) * 0.3);
                    level.addFreshEntity(meteor);
                }
            }
        }
    }

    private void infestNearPlayers(net.minecraft.server.level.ServerLevel level) {
        if (this.invasionDays < 1) {
            return;
        }
        int attempts = Math.min(2 + this.invasionDays, 12);
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            for (int i = 0; i < attempts; i++) {
                net.minecraft.core.BlockPos p = player.blockPosition().offset(
                        level.random.nextInt(33) - 16, level.random.nextInt(17) - 8, level.random.nextInt(33) - 16);
                if (!level.isLoaded(p)) {
                    continue;
                }
                net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
                // Same conversion table as the global contamination system, so the
                // creeping infection near players matches the world-wide apocalypse.
                if (!com.example.alieninvasion.logic.ContaminationRules.canContaminate(level, p, s)) {
                    continue;
                }
                net.minecraft.world.level.block.state.BlockState newState =
                        com.example.alieninvasion.logic.ContaminationRules.contaminatedStateFor(s);
                if (newState != null) {
                    if (newState.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) && s.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
                        newState = newState.setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, s.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS));
                    }
                    level.setBlockAndUpdate(p, newState);
                    // Make the creeping corruption visible: a soul puff per converted block,
                    // with an occasional low sculk-spread groan so the infestation reads.
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                            p.getX() + 0.5D, p.getY() + 1.0D, p.getZ() + 0.5D, 4, 0.3D, 0.3D, 0.3D, 0.01D);
                    if (level.random.nextInt(6) == 0) {
                        level.playSound(null, p, net.minecraft.sounds.SoundEvents.SCULK_BLOCK_SPREAD,
                                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.6F);
                    }
                }
            }
        }
    }

    /**
     * The swarm storms reclaimed chunks: a wave of grunts plus a breacher (it chews
     * through walls) spawns INSIDE one purifier-protected chunk near each player.
     * One siege per player per siege-day. Tower-defense pressure on held land.
     */
    private boolean runSiege(ServerLevel level) {
        com.example.alieninvasion.logic.ChunkContaminationData data =
                com.example.alieninvasion.logic.ChunkContaminationData.get(level);
        boolean any = false;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            net.minecraft.world.level.ChunkPos center = player.chunkPosition();
            outer:
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    net.minecraft.world.level.ChunkPos cp =
                            new net.minecraft.world.level.ChunkPos(center.x + dx, center.z + dz);
                    if (!data.isPurified(cp)) continue;
                    // The siege arrives BY DROPSHIP: a carrier descends over the
                    // reclaimed chunk and unloads the assault squad on a light beam.
                    int count = 4 + level.random.nextInt(3) + this.invasionDays / 3;
                    int breacherCount = Math.max(1, count / 4);
                    int gruntCount = count - breacherCount;
                    int bx = cp.getMinBlockX() + 8;
                    int bz = cp.getMinBlockZ() + 8;
                    int by = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    com.example.alieninvasion.entity.UfoEntity carrier =
                            com.example.alieninvasion.registry.EntityRegistry.UFO.create(level);
                    if (carrier != null) {
                        carrier.moveTo(bx + 0.5D, by + 32, bz + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                        carrier.setVariant(com.example.alieninvasion.entity.UfoEntity.CARRIER);
                        carrier.addTag("Dropship");
                        carrier.addTag("grunts:" + gruntCount);
                        carrier.addTag("breachers:" + breacherCount);
                        carrier.addTag("diff:" + this.invasionDays);
                        carrier.setPersistenceRequired();
                        level.addFreshEntity(carrier);
                    }
                    level.playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                            net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§c§l[Рой] ОСАДА! §rРой штурмует вашу отвоёванную территорию!"));
                    any = true;
                    break outer; // one besieged chunk per player per siege-day
                }
            }
        }
        return any;
    }

    // Beat the game: the swarm breaks and evacuates; a rift to their homeworld opens.
    public void triggerVictory(ServerLevel level) {
        triggerVictory(level, null);
    }

    public void triggerVictory(ServerLevel level, net.minecraft.core.BlockPos portalNear) {
        if (this.victoryAchieved) return;
        this.victoryAchieved = true;
        this.retreatComplete = false;
        this.retreatZeroTicks = 0;
        this.setDirty();
        buildVictoryPortal(level, portalNear);
        level.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§a§lМать Роя пала! §rРой в панике бежит — корабли-носители забирают остатки в небо..."),
                false);
        // Победная сцена: полноэкранный титр, фанфары и салют у каждого игрока.
        com.example.alieninvasion.entity.AlienUtils.broadcastTitle(level,
                net.minecraft.network.chat.Component.literal("§a§lМАТЬ РОЯ ПАЛА"),
                net.minecraft.network.chat.Component.literal("§eРой отступает. Земля выстояла."));
        for (net.minecraft.server.level.ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,
                    new com.example.alieninvasion.network.VictoryPayload(true));
            level.playSound(null, p.blockPosition(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.MASTER, 1.0F, 1.0F);
            if (p.level() instanceof ServerLevel psl) {
                for (int i = 0; i < 6; i++) {
                    psl.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                            p.getX() + (psl.random.nextDouble() - 0.5) * 12.0D,
                            p.getY() + 6.0D + psl.random.nextDouble() * 6.0D,
                            p.getZ() + (psl.random.nextDouble() - 0.5) * 12.0D,
                            30, 0.4D, 0.4D, 0.4D, 0.1D);
                }
                psl.playSound(null, p.blockPosition(),
                        net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                        net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.0F);
            }
        }
    }

    /** Смерть королевы рвёт пространство: рядом открывается портал в родной мир Роя. */
    private void buildVictoryPortal(ServerLevel level, net.minecraft.core.BlockPos near) {
        if (this.overworldPortalPos != null) {
            return;
        }
        if (near == null) {
            if (level.players().isEmpty()) {
                return;
            }
            near = level.players().get(0).blockPosition();
        }
        int x = near.getX() + 6;
        int z = near.getZ();
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        net.minecraft.core.BlockPos base = new net.minecraft.core.BlockPos(x, y, z);
        com.example.alieninvasion.logic.HomeworldManager.buildPortalFrame(level, base);
        this.overworldPortalPos = base;
        this.setDirty();
        level.playSound(null, base, net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
                net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.6F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§5[!] Смерть королевы разорвала пространство — открылся ПОРТАЛ в родной мир Роя!"),
                false);
    }

    // Evacuation: aliens flee from players, carrier UFOs lift them into the sky, and
    // once the area is clear we announce the world is free. All spawning is gated off.
    private void tickRetreat(ServerLevel level) {
        if (this.retreatComplete) return;
        if (level.getGameTime() % 20L != 0L) return;
        this.retreatSeconds++;

        // ЛУЧ-ЭВАКУАЦИЯ (страховка): медленные черви и застрявшие в ямах бойцы
        // физически не могут выйти из 80-блочного радиуса — без этого «Земля
        // свободна» (и приход Макса) могли не наступить НИКОГДА. Через 45 секунд
        // отступления носители просто забирают оставшихся лучом с земли.
        if (this.retreatSeconds > 45) {
            if (this.retreatSeconds == 46) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§7Носители опускают лучи — рой забирают прямо с земли..."),
                        false);
            }
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                for (net.minecraft.world.entity.Mob a : level.getEntitiesOfClass(
                        net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(96.0D),
                        e -> e.isAlive() && com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, e)
                                && !(e instanceof com.example.alieninvasion.entity.UfoEntity))) {
                    for (int i = 0; i < 10; i++) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                a.getX(), a.getY() + i * 0.8D, a.getZ(), 2, 0.15D, 0.1D, 0.15D, 0.0D);
                    }
                    level.playSound(null, a.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                            net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.4F);
                    a.addTag("EvacBeam"); // разрешает discard даже боссам (см. HiveTyrantEntity.remove)
                    a.discard();
                }
            }
        }

        int remaining = 0;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            java.util.List<net.minecraft.world.entity.Mob> aliens = level.getEntitiesOfClass(
                    net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(80.0D),
                    e -> com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, e));
            for (net.minecraft.world.entity.Mob a : aliens) {
                if (a instanceof com.example.alieninvasion.entity.UfoEntity) {
                    continue; // ships handle themselves
                }
                a.addTag("Retreating");
                a.setTarget(null);
                a.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
                net.minecraft.world.phys.Vec3 away = a.position().subtract(player.position());
                if (away.lengthSqr() > 0.01D) {
                    away = away.normalize().scale(0.5D);
                    a.setDeltaMovement(away.x, a.getDeltaMovement().y, away.z);
                    a.hurtMarked = true;
                }
                remaining++;
            }
            // Dispatch a carrier to beam a cluster away (capped so the sky isn't full of ships).
            long ships = level.getEntitiesOfClass(com.example.alieninvasion.entity.UfoEntity.class,
                    player.getBoundingBox().inflate(96.0D), e -> e.getTags().contains("EvacShip")).size();
            if (!aliens.isEmpty() && ships < 3 && level.random.nextFloat() < 0.5F) {
                net.minecraft.world.entity.Mob pick = aliens.get(level.random.nextInt(aliens.size()));
                if (!(pick instanceof com.example.alieninvasion.entity.UfoEntity)) {
                    com.example.alieninvasion.entity.UfoEntity evac =
                            com.example.alieninvasion.registry.EntityRegistry.UFO.create(level);
                    if (evac != null) {
                        evac.moveTo(pick.getX(), pick.getY() + 12, pick.getZ(), 0, 0);
                        evac.setVariant(com.example.alieninvasion.entity.UfoEntity.CARRIER);
                        evac.addTag("EvacShip");
                        level.addFreshEntity(evac);
                        level.playSound(null, pick.blockPosition(),
                                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                                net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.6F);
                    }
                }
            }
        }

        if (remaining == 0 && !level.players().isEmpty()) {
            this.retreatZeroTicks++;
            if (this.retreatZeroTicks > 6) {
                this.retreatComplete = true;
                this.setDirty();
                level.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§a§lЗемля свободна. Вторжение окончено — рой бежал к звёздам."),
                        false);
            }
        } else {
            this.retreatZeroTicks = 0;
        }
    }

    public boolean isVictoryAchieved() {
        return victoryAchieved;
    }

    public net.minecraft.core.BlockPos getOverworldPortalPos() {
        return overworldPortalPos;
    }

    public boolean isPlanetDestroyed() {
        return planetDestroyed;
    }

    /**
     * ЭПИЛОГ. После отступления роя на сцену выходит охотник Макс Максбетов,
     * а после уничтожения планеты Роя — мир Земли плавно исцеляется и игрокам
     * показываются титры.
     */
    private void tickPostVictory(ServerLevel level) {
        // 1) Охотник приходит через ~30 секунд после того, как рой бежал.
        if (this.retreatComplete && !this.hunterArrived && !level.players().isEmpty()) {
            this.hunterTimer++;
            if (this.hunterTimer == 400) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§7Сквозь дым кто-то идёт. Неторопливо. Как хозяин."), false);
            }
            if (this.hunterTimer >= 600) {
                this.hunterArrived = true;
                this.setDirty();
                spawnHunter(level);
            }
        }

        // 2) Земля исцеляется после взрыва планеты Роя.
        if (this.planetDestroyed) {
            this.planetTicks++;
            // ВСПЫШКА: первые секунды небо над Землёй полыхает — чужая звезда гаснет.
            if (this.planetTicks <= 80 && this.planetTicks % 10 == 0) {
                for (net.minecraft.server.level.ServerPlayer p : level.players()) {
                    for (int i = 0; i < 6; i++) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                                p.getX() + (level.random.nextDouble() - 0.5D) * 140.0D,
                                p.getY() + 45.0D + level.random.nextDouble() * 35.0D,
                                p.getZ() + (level.random.nextDouble() - 0.5D) * 140.0D,
                                1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                    if (this.planetTicks == 10) {
                        level.playSound(null, p.blockPosition(),
                                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                                net.minecraft.sounds.SoundSource.MASTER, 6.0F, 0.35F);
                    } else if (this.planetTicks == 50) {
                        level.playSound(null, p.blockPosition(),
                                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                                net.minecraft.sounds.SoundSource.MASTER, 4.0F, 0.5F);
                    }
                }
            }
            // Разрыв схлопывается через 5 секунд после взрыва: мира Роя больше нет —
            // и пути туда больше нет.
            if (!this.portalCollapsed && this.planetTicks >= 100) {
                collapsePortal(level);
            }

            healWorldAroundPlayers(level);
            if (this.planetTicks % 20 == 0) {
                dissolveAliens(level);
            }
            if (!this.healingDone && this.planetTicks % 20 == 0) {
                updateHealingBar(level);
            }
            // Титры — спустя ~15 секунд после взрыва, каждому игроку один раз.
            if (this.planetTicks > 300) {
                for (net.minecraft.server.level.ServerPlayer p : level.players()) {
                    if (this.creditsShown.add(p.getUUID())) {
                        this.setDirty();
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                                net.minecraft.network.protocol.game.ClientboundGameEventPacket.WIN_GAME, 1.0F));
                        grantAdvancement(p, "planet_destroyed", "destroyed");
                    }
                }
            }
        }
    }

    private void spawnHunter(ServerLevel level) {
        java.util.List<net.minecraft.server.level.ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        net.minecraft.server.level.ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        double a = level.random.nextDouble() * Math.PI * 2.0D;
        int x = anchor.getBlockX() + (int) (Math.cos(a) * 14.0D);
        int z = anchor.getBlockZ() + (int) (Math.sin(a) * 14.0D);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        com.example.alieninvasion.entity.HunterEntity hunter =
                com.example.alieninvasion.registry.EntityRegistry.HUNTER.create(level);
        if (hunter == null) {
            return;
        }
        hunter.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(hunter);
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.entity.LightningBolt bolt =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(x + (i == 0 ? 3 : -3), y, z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
        level.playSound(null, hunter.blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 1.6F);
        com.example.alieninvasion.entity.AlienUtils.broadcastTitle(level,
                net.minecraft.network.chat.Component.literal("§6МАКС МАКСБЕТОВ"),
                net.minecraft.network.chat.Component.literal("§eЛегендарный охотник на пришельцев. НЕ ТРОГАТЬ."));
    }

    /** Вызывается реактором из мира Роя в момент детонации. */
    public void onPlanetDestroyed(ServerLevel overworld) {
        if (this.planetDestroyed) {
            return;
        }
        this.planetDestroyed = true;
        this.planetTicks = 0;
        this.setDirty();
        overworld.setWeatherParameters(12000000, 0, false, false); // небо очищается
        // Рассвет нового мира: взрыв чужой звезды встречают первые лучи солнца.
        // (Безопасно: после победы getDay() читает замороженный invasionDays.)
        overworld.setDayTime(23200L);
        com.example.alieninvasion.entity.AlienUtils.broadcastTitle(overworld,
                net.minecraft.network.chat.Component.literal("§a§lПЛАНЕТА РОЯ УНИЧТОЖЕНА"),
                net.minecraft.network.chat.Component.literal("§eНебо очищается. Земля оживает на глазах..."));
        overworld.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Макс Максбетов] §fВот это, бомжи, называется ФЕЙЕРВЕРК. Учитесь, пока я жив."), false);
        for (net.minecraft.server.level.ServerPlayer p : overworld.players()) {
            overworld.playSound(null, p.blockPosition(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.MASTER, 1.0F, 1.2F);
        }
    }

    /** Быстрое, видимое глазу исцеление: заражённые блоки рядом с игроками возвращаются к природе. */
    private void healWorldAroundPlayers(ServerLevel level) {
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            boolean healedAny = false;
            for (int i = 0; i < 384; i++) {
                net.minecraft.core.BlockPos p = player.blockPosition().offset(
                        level.random.nextInt(65) - 32,
                        level.random.nextInt(41) - 20,
                        level.random.nextInt(65) - 32);
                if (!level.isLoaded(p)) {
                    continue;
                }
                // Нашли заражение — лечим весь вертикальный «стержень» (±5 блоков):
                // случайная выборка превращается в видимую волну чистоты.
                boolean hit = false;
                for (int dy = -5; dy <= 5; dy++) {
                    net.minecraft.core.BlockPos cp = p.above(dy);
                    net.minecraft.world.level.block.state.BlockState s = level.getBlockState(cp);
                    net.minecraft.world.level.block.state.BlockState healed = healedStateFor(s);
                    if (healed != null) {
                        level.setBlockAndUpdate(cp, healed);
                        hit = true;
                        healedAny = true;
                    }
                }
                if (hit && level.random.nextInt(4) == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            p.getX() + 0.5D, p.getY() + 1.0D, p.getZ() + 0.5D, 3, 0.3D, 0.3D, 0.3D, 0.0D);
                }
            }
            if (healedAny && level.random.nextInt(30) == 0) {
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AZALEA_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
    }

    /** Схлопывание разрыва: после гибели мира Роя портал на Земле исчезает навсегда. */
    private void collapsePortal(ServerLevel level) {
        this.portalCollapsed = true;
        this.setDirty();
        net.minecraft.core.BlockPos base = this.overworldPortalPos;
        if (base == null) {
            return;
        }
        for (int dx = -1; dx <= 4; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                net.minecraft.core.BlockPos p = base.offset(dx, dy, 0);
                if (!level.isLoaded(p)) {
                    continue;
                }
                var s = level.getBlockState(p);
                if (s.is(com.example.alieninvasion.registry.ModBlocks.ALIEN_PORTAL)
                        || s.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                        || s.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)) {
                    level.removeBlock(p, false);
                }
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                base.getX() + 2.0D, base.getY() + 2.5D, base.getZ() + 0.5D, 120, 1.5D, 2.0D, 1.0D, 0.15D);
        level.playSound(null, base, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.BLOCKS, 3.0F, 0.4F);
        level.playSound(null, base, net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.6F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§5Разрыв пространства схлопнулся — мира Роя больше не существует."),
                false);
    }

    /**
     * Замер исцеления: каждую секунду сэмплируем поверхность вокруг игроков и
     * показываем зелёный босс-бар «Исцеление Земли: NN%». Три секунды подряд
     * чистоты >= 99% — бар уходит с финальным сообщением.
     */
    private void updateHealingBar(ServerLevel level) {
        int samples = 0;
        int contaminated = 0;
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            for (int i = 0; i < 64; i++) {
                int x = player.getBlockX() + level.random.nextInt(65) - 32;
                int z = player.getBlockZ() + level.random.nextInt(65) - 32;
                if (!level.isLoaded(new net.minecraft.core.BlockPos(x, player.getBlockY(), z))) {
                    continue;
                }
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                var s = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                samples++;
                if (com.example.alieninvasion.logic.ContaminationRules.isContaminated(s)) {
                    contaminated++;
                }
            }
        }
        if (samples == 0) {
            return;
        }
        float clean = 1.0F - contaminated / (float) samples;
        this.healingBar.setProgress(clean);
        this.healingBar.setName(net.minecraft.network.chat.Component.literal(
                "§a🌱 Исцеление Земли: " + Math.round(clean * 100.0F) + "%"));
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            this.healingBar.addPlayer(p);
        }

        if (clean >= 0.99F) {
            this.healingCleanStreak++;
            if (this.healingCleanStreak >= 3) {
                this.healingDone = true;
                this.setDirty();
                this.healingBar.removeAllPlayers();
                level.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§a§lЗемля чиста. §rТрава зеленеет, небо ясное — всё закончилось по-настоящему."),
                        false);
            }
        } else {
            this.healingCleanStreak = 0;
        }
    }

    private static net.minecraft.world.level.block.state.BlockState healedStateFor(
            net.minecraft.world.level.block.state.BlockState s) {
        // Трава возвращается травой, а не голой землёй — мир оживает красиво.
        if (s.is(com.example.alieninvasion.registry.ModBlocks.INFESTED_GRASS)) {
            return net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (s.is(com.example.alieninvasion.registry.ModBlocks.ALIEN_HIVE)
                || s.is(com.example.alieninvasion.registry.ModBlocks.ALIEN_HEART)
                || s.is(com.example.alieninvasion.registry.ModBlocks.ALIEN_TENDRILS)
                || s.is(com.example.alieninvasion.registry.ModBlocks.BLOOD_POOL)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        net.minecraft.world.level.block.state.BlockState clean =
                com.example.alieninvasion.logic.ContaminationRules.cleanStateFor(s);
        if (clean != null) {
            return com.example.alieninvasion.logic.ContaminationRules.copyProperties(s, clean);
        }
        return null;
    }

    /** Пришельцы растворяются в дымке — по несколько за раз, на глазах. */
    private void dissolveAliens(ServerLevel level) {
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            java.util.List<net.minecraft.world.entity.Mob> aliens = level.getEntitiesOfClass(
                    net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(64.0D),
                    m -> m.isAlive() && com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, m));
            int dissolved = 0;
            for (net.minecraft.world.entity.Mob alien : aliens) {
                if (dissolved >= 6) {
                    break;
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        alien.getX(), alien.getY() + 1.0D, alien.getZ(), 16, 0.3D, 0.6D, 0.3D, 0.05D);
                level.playSound(null, alien.blockPosition(), net.minecraft.sounds.SoundEvents.SCULK_BLOCK_BREAK,
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.4F);
                alien.addTag("EvacBeam"); // разрешает discard даже боссам (см. HiveTyrantEntity.remove)
                alien.discard();
                dissolved++;
            }
        }
    }

    private static void grantAdvancement(net.minecraft.server.level.ServerPlayer player, String name, String criterion) {
        var holder = player.getServer().getAdvancements().get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alien-invasion", name));
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    // Maps elapsed Minecraft days -> invasion stage. Day 1 = stage 1 (prompt start),
    // then one new stage every MC_DAYS_PER_STAGE days.
    private static int stageForDays(int mcDays) {
        if (mcDays <= 0) {
            return 0;
        }
        return 1 + (mcDays - 1) / MC_DAYS_PER_STAGE;
    }

    public int getInvasionDays() {
        return invasionDays;
    }

    public void setInvasionDays(int days) {
        this.invasionDays = days;
        this.setDirty();
    }

    public int getBowHits() {
        return bowHits;
    }

    public void incrementBowHits() {
        this.bowHits++;
        this.setDirty();
    }

    public int getFireDamage() {
        return fireDamage;
    }

    public void incrementFireDamage() {
        this.fireDamage++;
        this.setDirty();
    }

    public int getHeavyArmorTicks() {
        return heavyArmorTicks;
    }

    public void incrementHeavyArmorTicks(int amount) {
        this.heavyArmorTicks += amount;
        this.setDirty();
    }
}
