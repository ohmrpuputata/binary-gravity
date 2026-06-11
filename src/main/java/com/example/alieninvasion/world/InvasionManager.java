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
    private int lastSiegeDay = 0;

    // Survive this many invasion days to beat the game.
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

        // SIEGE: every second invasion day (from day 4), at night, the swarm
        // deliberately storms RECLAIMED (purifier-protected) chunks - holding
        // territory means defending it.
        if (this.invasionDays >= 4 && this.invasionDays % 2 == 0 && this.invasionDays != this.lastSiegeDay
                && !level.isDay() && level.getGameTime() % 200L == 0L) {
            if (runSiege(level)) {
                this.lastSiegeDay = this.invasionDays;
                this.setDirty();
            }
        }

        // Orbital bombardment from day 3: meteors hammer surface players while
        // burrowing drills hunt anyone hiding deep underground.
        if (this.invasionDays >= 3 && level.getGameTime() % 600L == 0L) {
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
                double ox = player.getX() + (level.random.nextDouble() - 0.5) * 18.0;
                double oz = player.getZ() + (level.random.nextDouble() - 0.5) * 18.0;
                double oy = surfaceY + 45;
                com.example.alieninvasion.entity.MeteorEntity meteor = com.example.alieninvasion.registry.EntityRegistry.METEOR
                        .create(level);
                if (meteor != null) {
                    meteor.setPos(ox, oy, oz);
                    meteor.setDifficulty(this.invasionDays);
                    meteor.setDeltaMovement((player.getX() - ox) * 0.02, -1.2, (player.getZ() - oz) * 0.02);
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
                    int count = 4 + level.random.nextInt(3) + this.invasionDays / 3;
                    for (int i = 0; i < count; i++) {
                        int bx = cp.getMinBlockX() + level.random.nextInt(16);
                        int bz = cp.getMinBlockZ() + level.random.nextInt(16);
                        int by = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx, bz);
                        net.minecraft.world.entity.Mob raider = (i % 4 == 3
                                ? com.example.alieninvasion.registry.EntityRegistry.ALIEN_BREACHER
                                : com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT).create(level);
                        if (raider != null) {
                            raider.moveTo(bx + 0.5D, by, bz + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                            raider.setTarget(player);
                            level.addFreshEntity(raider);
                        }
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

    // Beat the game: NO vanilla credits. The swarm breaks and evacuates instead.
    public void triggerVictory(ServerLevel level) {
        if (this.victoryAchieved) return;
        this.victoryAchieved = true;
        this.retreatComplete = false;
        this.retreatZeroTicks = 0;
        this.setDirty();
        level.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§a§lМать Роя пала! §rРой в панике бежит — корабли-носители забирают остатки в небо..."),
                false);
    }

    // Evacuation: aliens flee from players, carrier UFOs lift them into the sky, and
    // once the area is clear we announce the world is free. All spawning is gated off.
    private void tickRetreat(ServerLevel level) {
        if (this.retreatComplete) return;
        if (level.getGameTime() % 20L != 0L) return;

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
