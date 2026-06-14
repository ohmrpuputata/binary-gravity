package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
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

public final class WorldAmbienceEffects {
    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 8;

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
    }

    private static void animateOre(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if (random.nextInt(5) != 0 || !hasExposedFace(level, pos)) {
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
    }

    private static void animateMachine(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
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
