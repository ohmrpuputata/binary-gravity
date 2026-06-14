package com.example.alieninvasion.client;

import com.example.alieninvasion.registry.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class AcidRainEffects {
    private static final int EFFECT_RADIUS = 18;

    private AcidRainEffects() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AcidRainEffects::tick);
    }

    private static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || client.isPaused() || !level.isRaining()) {
            return;
        }

        RandomSource random = level.random;
        float rainStrength = level.getRainLevel(1.0F);
        int smokeCount = rainStrength > 0.65F ? 4 : 2;

        if ((level.getGameTime() & 1L) == 0L) {
            for (int i = 0; i < smokeCount; i++) {
                spawnSurfaceReaction(level, client.player.getX(), client.player.getY(), client.player.getZ(), random);
            }
        }

        if (level.canSeeSky(client.player.blockPosition())
                && level.getGameTime() % 90L == 0L
                && random.nextFloat() < 0.65F) {
            level.playLocalSound(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.WEATHER,
                    0.20F,
                    1.55F + random.nextFloat() * 0.25F,
                    false);
        }
    }

    private static void spawnSurfaceReaction(
            ClientLevel level,
            double playerX,
            double playerY,
            double playerZ,
            RandomSource random) {
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = 3.5D + Math.sqrt(random.nextDouble()) * (EFFECT_RADIUS - 3.5D);
            int x = Mth.floor(playerX + Math.cos(angle) * distance);
            int z = Mth.floor(playerZ + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos surfaceAir = new BlockPos(x, y, z);

            if (Math.abs(y - playerY) > 11.0D || !level.canSeeSky(surfaceAir)) {
                continue;
            }

            double px = x + 0.2D + random.nextDouble() * 0.6D;
            double py = y + 0.03D;
            double pz = z + 0.2D + random.nextDouble() * 0.6D;
            double driftX = (random.nextDouble() - 0.5D) * 0.018D;
            double driftZ = (random.nextDouble() - 0.5D) * 0.018D;
            double rise = 0.018D + random.nextDouble() * 0.022D;

            level.addParticle(ModParticles.ACID_SMOKE, px, py, pz, driftX, rise, driftZ);

            if (random.nextFloat() < 0.28F) {
                level.addParticle(
                        ParticleTypes.SNEEZE,
                        px,
                        py + 0.06D,
                        pz,
                        (random.nextDouble() - 0.5D) * 0.05D,
                        0.035D + random.nextDouble() * 0.035D,
                        (random.nextDouble() - 0.5D) * 0.05D);
            }
            return;
        }
    }
}
