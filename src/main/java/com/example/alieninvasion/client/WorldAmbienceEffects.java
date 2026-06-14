package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienTrollEntity;
import com.example.alieninvasion.entity.HiveTyrantEntity;
import com.example.alieninvasion.entity.SwarmMotherEntity;
import com.example.alieninvasion.entity.UfoEntity;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class WorldAmbienceEffects {
    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 8;
    private static int cameraShakeTicks;
    private static float cameraShakeStrength;

    private WorldAmbienceEffects() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WorldAmbienceEffects::tick);
    }

    private static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || client.isPaused()) {
            return;
        }

        RandomSource random = level.random;
        BlockPos origin = client.player.blockPosition();
        int samples = (level.getGameTime() & 1L) == 0L ? 7 : 4;
        if (cameraShakeTicks > 0) {
            cameraShakeTicks--;
        }

        for (int i = 0; i < samples; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS,
                    random.nextInt(VERTICAL_RADIUS * 2 + 1) - VERTICAL_RADIUS,
                    random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (isBloody(id, state)) {
                animateBlood(level, pos, random);
            }
            if (isInfested(id, state)) {
                animateInfestation(level, pos, random);
            }
            if (isAlienOre(state)) {
                animateOre(level, pos, state, random);
            }
            animateMachine(level, pos, state, random);
        }

        if (level.getGameTime() % 4L == 0L) {
            animateFootprints(level, client, random);
        }
        if (level.getGameTime() % 5L == 0L) {
            animateLargeAliens(level, client, random);
        }
        if (level.getGameTime() % 6L == 0L) {
            animateInvasionSky(level, client, random);
        }
        if (level.getGameTime() % 160L == 0L) {
            playAtmosphere(level, client, random);
        }
    }

    private static boolean isBloody(ResourceLocation id, BlockState state) {
        return state.is(ModBlocks.BLOOD_POOL)
                || state.is(ModBlocks.BLOOD_LAYER)
                || id.getNamespace().equals(AlienInvasionMod.MODID)
                && id.getPath().startsWith("bloody_");
    }

    private static boolean isInfested(ResourceLocation id, BlockState state) {
        if (!id.getNamespace().equals(AlienInvasionMod.MODID)) {
            return false;
        }
        String path = id.getPath();
        return path.startsWith("infested_")
                || path.startsWith("bloody_infested_")
                || state.is(ModBlocks.ALIEN_RESIDUE)
                || state.is(ModBlocks.ALIEN_FLESH)
                || state.is(ModBlocks.ALIEN_HIVE)
                || state.is(ModBlocks.ALIEN_TENDRILS);
    }

    private static boolean isAlienOre(BlockState state) {
        return state.is(ModBlocks.COSMIC_CRYSTAL_ORE)
                || state.is(ModBlocks.PLATINUM_ORE)
                || state.is(ModBlocks.PALLADIUM_ORE)
                || state.is(ModBlocks.DARK_MATTER_ORE)
                || state.is(ModBlocks.INFESTED_DIAMOND_ORE)
                || state.is(ModBlocks.INFESTED_REDSTONE_ORE)
                || state.is(ModBlocks.PURE_RADIATION_BLOCK);
    }

    private static void animateInfestation(ClientLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0 || !hasExposedFace(level, pos)) {
            return;
        }

        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 0.15D + random.nextDouble() * 0.9D;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(
                random.nextInt(5) == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.WARPED_SPORE,
                x, y, z,
                (random.nextDouble() - 0.5D) * 0.012D,
                0.006D + random.nextDouble() * 0.012D,
                (random.nextDouble() - 0.5D) * 0.012D);

        if (random.nextInt(18) == 0) {
            level.addParticle(
                    ParticleTypes.MYCELIUM,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.52D,
                    pos.getZ() + 0.5D,
                    0.0D, 0.012D, 0.0D);
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (id.getPath().contains("grass") && random.nextInt(4) == 0) {
            level.addParticle(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.04D,
                    pos.getZ() + random.nextDouble(),
                    0.0D,
                    0.008D,
                    0.0D);
        }

        if (random.nextInt(24) == 0) {
            for (int i = 0; i < 5; i++) {
                double angle = i * (Math.PI * 2.0D / 5.0D) + random.nextDouble() * 0.25D;
                level.addParticle(
                        ParticleTypes.REVERSE_PORTAL,
                        pos.getX() + 0.5D + Math.cos(angle) * 0.38D,
                        pos.getY() + 0.45D + random.nextDouble() * 0.25D,
                        pos.getZ() + 0.5D + Math.sin(angle) * 0.38D,
                        -Math.cos(angle) * 0.012D,
                        0.004D,
                        -Math.sin(angle) * 0.012D);
            }
        }
    }

    private static void animateOre(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if (random.nextInt(5) != 0 || !hasExposedFace(level, pos)) {
            return;
        }
        if (level.getMaxLocalRawBrightness(pos) > 7 && random.nextInt(4) != 0) {
            return;
        }

        Direction face = randomExposedFace(level, pos, random);
        if (face == null) {
            return;
        }
        double x = pos.getX() + 0.5D + face.getStepX() * 0.52D
                + (random.nextDouble() - 0.5D) * 0.55D;
        double y = pos.getY() + 0.5D + face.getStepY() * 0.52D
                + (random.nextDouble() - 0.5D) * 0.55D;
        double z = pos.getZ() + 0.5D + face.getStepZ() * 0.52D
                + (random.nextDouble() - 0.5D) * 0.55D;

        if (state.is(ModBlocks.PLATINUM_ORE)) {
            level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0.0D, 0.008D, 0.0D);
        } else if (state.is(ModBlocks.PALLADIUM_ORE)) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.006D, 0.0D);
        } else if (state.is(ModBlocks.COSMIC_CRYSTAL_ORE)) {
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0D, 0.012D, 0.0D);
        } else if (state.is(ModBlocks.DARK_MATTER_ORE)) {
            level.addParticle(ParticleTypes.SCULK_SOUL, x, y, z, 0.0D, 0.008D, 0.0D);
        } else {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.008D, 0.0D);
        }

        if ((state.is(ModBlocks.DARK_MATTER_ORE) || state.is(ModBlocks.PURE_RADIATION_BLOCK))
                && random.nextInt(5) == 0) {
            level.addParticle(
                    random.nextBoolean() ? ModParticles.ACID_SMOKE : ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.02D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    private static void animateMachine(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if (isPoweredMachine(state)) {
            if (random.nextInt(5) == 0) {
                level.addParticle(
                        ParticleTypes.GLOW,
                        pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                        pos.getY() + 0.45D + random.nextDouble() * 0.45D,
                        pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                        0.0D, 0.003D, 0.0D);
            }
            if (random.nextInt(28) == 0) {
                animateElectricArc(level, pos, random);
            }
        }

        if (state.is(ModBlocks.CRACKED_ALIEN_PIPE) || state.is(ModBlocks.TOXIC_BARREL)) {
            if (random.nextInt(4) == 0 && hasExposedFace(level, pos)) {
                double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
                double y = pos.getY() + 0.65D + random.nextDouble() * 0.35D;
                double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
                level.addParticle(ModParticles.ACID_SMOKE, x, y, z,
                        (random.nextDouble() - 0.5D) * 0.015D,
                        0.018D + random.nextDouble() * 0.015D,
                        (random.nextDouble() - 0.5D) * 0.015D);
            }
            if (random.nextInt(90) == 0) {
                level.playLocalSound(pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                        0.14F, 1.65F + random.nextFloat() * 0.2F, false);
            }
            return;
        }

        if (state.is(ModBlocks.WARNING_LAMP) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.GLOW,
                    pos.getX() + 0.3D + random.nextDouble() * 0.4D,
                    pos.getY() + 0.45D + random.nextDouble() * 0.4D,
                    pos.getZ() + 0.3D + random.nextDouble() * 0.4D,
                    0.0D, 0.004D, 0.0D);
            return;
        }

        if ((state.is(ModBlocks.RADIO_TRANSMITTER)
                || state.is(ModBlocks.BLACK_MARKET_TERMINAL)
                || state.is(ModBlocks.PLASMA_TURRET))
                && random.nextInt(7) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 0.55D + random.nextDouble() * 0.55D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    (random.nextDouble() - 0.5D) * 0.035D,
                    0.015D,
                    (random.nextDouble() - 0.5D) * 0.035D);
        }
    }

    private static boolean isPoweredMachine(BlockState state) {
        return state.is(ModBlocks.RADIO_TRANSMITTER)
                || state.is(ModBlocks.BLACK_MARKET_TERMINAL)
                || state.is(ModBlocks.PLASMA_TURRET)
                || state.is(ModBlocks.WARNING_LAMP)
                || state.is(ModBlocks.ALIEN_BEACON)
                || state.is(ModBlocks.PLANET_REACTOR)
                || state.is(ModBlocks.PURIFIER)
                || state.is(ModBlocks.PURIFIER_STATION);
    }

    private static void animateElectricArc(ClientLevel level, BlockPos origin, RandomSource random) {
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= 4; distance++) {
                BlockPos target = origin.relative(direction, distance);
                if (!isPoweredMachine(level.getBlockState(target))) {
                    continue;
                }
                Vec3 start = Vec3.atCenterOf(origin);
                Vec3 delta = Vec3.atCenterOf(target).subtract(start);
                for (int step = 1; step < 7; step++) {
                    double progress = step / 7.0D;
                    Vec3 point = start.add(delta.scale(progress));
                    level.addParticle(
                            ParticleTypes.ELECTRIC_SPARK,
                            point.x + (random.nextDouble() - 0.5D) * 0.12D,
                            point.y + (random.nextDouble() - 0.5D) * 0.12D,
                            point.z + (random.nextDouble() - 0.5D) * 0.12D,
                            0.0D, 0.0D, 0.0D);
                }
                return;
            }
        }
    }

    private static void animateBlood(ClientLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(level.isRaining() ? 2 : 5) != 0 || !hasExposedFace(level, pos)) {
            return;
        }

        if (level.isRaining() && level.canSeeSky(pos.above()) && random.nextBoolean()) {
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.03D,
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 0.035D,
                    0.045D + random.nextDouble() * 0.03D,
                    (random.nextDouble() - 0.5D) * 0.035D);
        } else if (level.getBlockState(pos.below()).isAir()) {
            level.addParticle(
                    ParticleTypes.DRIPPING_LAVA,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() - 0.02D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    0.0D, 0.0D, 0.0D);
        } else {
            level.addParticle(
                    ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.01D,
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.006D, 0.0D);
        }
    }

    private static void animateFootprints(ClientLevel level, Minecraft client, RandomSource random) {
        if (!client.player.onGround()
                || client.player.getDeltaMovement().horizontalDistanceSqr() < 0.0004D) {
            return;
        }
        BlockPos feet = client.player.blockPosition();
        BlockState floor = level.getBlockState(feet.below());
        BlockState decal = level.getBlockState(feet);
        ResourceLocation floorId = BuiltInRegistries.BLOCK.getKey(floor.getBlock());
        if (!isBloody(floorId, floor) && !decal.is(ModBlocks.BLOOD_LAYER)) {
            return;
        }

        level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                client.player.getX() - client.player.getDeltaMovement().x * 1.4D,
                client.player.getY() + 0.035D,
                client.player.getZ() - client.player.getDeltaMovement().z * 1.4D,
                (random.nextDouble() - 0.5D) * 0.012D,
                0.004D,
                (random.nextDouble() - 0.5D) * 0.012D);
    }

    private static void animateInvasionSky(ClientLevel level, Minecraft client, RandomSource random) {
        if (level.getDayTime() / 24000L < 1L || !level.canSeeSky(client.player.blockPosition().above())) {
            return;
        }

        for (UfoEntity ufo : level.getEntitiesOfClass(
                UfoEntity.class,
                client.player.getBoundingBox().inflate(64.0D, 36.0D, 64.0D))) {
            for (int i = 0; i < 7; i++) {
                level.addParticle(
                        i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW,
                        ufo.getX() + (random.nextDouble() - 0.5D) * 1.5D,
                        ufo.getY() - 2.0D - random.nextDouble() * 16.0D,
                        ufo.getZ() + (random.nextDouble() - 0.5D) * 1.5D,
                        0.0D, -0.012D, 0.0D);
            }
            break;
        }

        double px = client.player.getX() + random.nextInt(45) - 22;
        double py = client.player.getY() + 18.0D + random.nextDouble() * 18.0D;
        double pz = client.player.getZ() + random.nextInt(45) - 22;
        level.addParticle(
                random.nextInt(4) == 0 ? ParticleTypes.END_ROD : ParticleTypes.ASH,
                px, py, pz,
                (random.nextDouble() - 0.5D) * 0.018D,
                -0.035D - random.nextDouble() * 0.025D,
                (random.nextDouble() - 0.5D) * 0.018D);

        if (random.nextInt(120) == 0) {
            double beamX = client.player.getX() + (random.nextBoolean() ? 1 : -1) * (32.0D + random.nextDouble() * 28.0D);
            double beamZ = client.player.getZ() + (random.nextBoolean() ? 1 : -1) * (32.0D + random.nextDouble() * 28.0D);
            for (int i = 0; i < 18; i++) {
                level.addParticle(
                        i % 3 == 0 ? ParticleTypes.GLOW : ParticleTypes.END_ROD,
                        beamX + (random.nextDouble() - 0.5D) * 1.1D,
                        client.player.getY() + 8.0D + i * 1.4D,
                        beamZ + (random.nextDouble() - 0.5D) * 1.1D,
                        0.0D, -0.012D, 0.0D);
            }
        }

        if (random.nextInt(300) == 0) {
            level.addParticle(
                    ParticleTypes.FLASH,
                    client.player.getX() + (random.nextBoolean() ? 45.0D : -45.0D),
                    client.player.getY() + 8.0D,
                    client.player.getZ() + (random.nextDouble() - 0.5D) * 70.0D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static void animateLargeAliens(ClientLevel level, Minecraft client, RandomSource random) {
        for (Entity entity : level.getEntities(
                client.player,
                client.player.getBoundingBox().inflate(14.0D),
                WorldAmbienceEffects::isLargeAlien)) {
            if (!entity.onGround() || random.nextInt(5) != 0) {
                continue;
            }
            double distance = Math.max(2.0D, entity.distanceTo(client.player));
            cameraShakeTicks = 5;
            cameraShakeStrength = (float) Math.min(1.25D, 8.0D / distance);
            level.addParticle(
                    ParticleTypes.POOF,
                    entity.getX(),
                    entity.getY() + 0.1D,
                    entity.getZ(),
                    0.0D, 0.02D, 0.0D);
            return;
        }
    }

    private static boolean isLargeAlien(Entity entity) {
        return entity instanceof SwarmMotherEntity
                || entity instanceof HiveTyrantEntity
                || entity instanceof AlienTrollEntity;
    }

    public static float cameraYawOffset(float partialTick) {
        if (cameraShakeTicks <= 0) {
            return 0.0F;
        }
        return (float) Math.sin((cameraShakeTicks - partialTick) * 4.7D) * cameraShakeStrength;
    }

    public static float cameraPitchOffset(float partialTick) {
        if (cameraShakeTicks <= 0) {
            return 0.0F;
        }
        return (float) Math.cos((cameraShakeTicks - partialTick) * 5.3D) * cameraShakeStrength * 0.65F;
    }

    private static void playAtmosphere(ClientLevel level, Minecraft client, RandomSource random) {
        BlockPos origin = client.player.blockPosition();
        BlockPos heart = null;
        BlockPos radio = null;
        BlockPos infested = null;

        for (int i = 0; i < 96; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(41) - 20,
                    random.nextInt(17) - 8,
                    random.nextInt(41) - 20);
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.ALIEN_HEART)) {
                heart = pos;
                break;
            }
            if (state.is(ModBlocks.RADIO_TRANSMITTER) || state.is(ModBlocks.BLACK_MARKET_TERMINAL)) {
                radio = pos;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (isInfested(id, state)) {
                infested = pos;
            }
        }

        if (heart != null) {
            level.playLocalSound(heart, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT,
                    0.48F, 0.62F + random.nextFloat() * 0.12F, false);
            level.playLocalSound(heart, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                    0.18F, 0.55F, false);
        } else if (radio != null) {
            level.playLocalSound(radio, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.AMBIENT,
                    0.2F, 0.45F + random.nextFloat() * 0.3F, false);
            level.playLocalSound(radio, SoundEvents.SCULK_CLICKING, SoundSource.AMBIENT,
                    0.13F, 1.75F, false);
        } else if (infested != null) {
            level.playLocalSound(infested, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.AMBIENT,
                    0.16F, 0.55F + random.nextFloat() * 0.18F, false);
        } else if (level.getDayTime() / 24000L >= 2L && random.nextBoolean()) {
            double x = client.player.getX() + (random.nextBoolean() ? 28.0D : -28.0D);
            double z = client.player.getZ() + (random.nextDouble() - 0.5D) * 45.0D;
            level.playLocalSound(x, client.player.getY() + 4.0D, z,
                    random.nextBoolean() ? SoundEvents.PHANTOM_AMBIENT : SoundEvents.WARDEN_AMBIENT,
                    SoundSource.HOSTILE, 0.22F, 0.62F + random.nextFloat() * 0.16F, false);
        }
    }

    private static boolean hasExposedFace(ClientLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isSolidRender(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static Direction randomExposedFace(ClientLevel level, BlockPos pos, RandomSource random) {
        Direction start = Direction.from3DDataValue(random.nextInt(Direction.values().length));
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.from3DDataValue((start.get3DDataValue() + i) % Direction.values().length);
            BlockPos neighbor = pos.relative(direction);
            if (!level.getBlockState(neighbor).isSolidRender(level, neighbor)) {
                return direction;
            }
        }
        return null;
    }
}
