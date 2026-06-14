package com.example.alieninvasion.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class InfectionVisuals {
    private InfectionVisuals() {
    }

    public static void spread(ServerLevel level, BlockPos from, BlockPos to) {
        double startX = from.getX() + 0.5D;
        double startY = from.getY() + 0.55D;
        double startZ = from.getZ() + 0.5D;
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        for (int step = 1; step <= 5; step++) {
            double progress = step / 6.0D;
            level.sendParticles(
                    step % 2 == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.WARPED_SPORE,
                    startX + dx * progress,
                    startY + dy * progress,
                    startZ + dz * progress,
                    1,
                    0.025D,
                    0.025D,
                    0.025D,
                    0.0D);
        }
    }

    public static void breakBurst(ServerLevel level, BlockPos pos, BlockState state) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                18,
                0.42D,
                0.42D,
                0.42D,
                0.08D);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 0.55D,
                pos.getZ() + 0.5D,
                8,
                0.35D,
                0.25D,
                0.35D,
                0.02D);
        level.sendParticles(
                ParticleTypes.SNEEZE,
                pos.getX() + 0.5D,
                pos.getY() + 0.45D,
                pos.getZ() + 0.5D,
                5,
                0.3D,
                0.2D,
                0.3D,
                0.025D);
    }
}
