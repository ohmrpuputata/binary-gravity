package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Proximity-based dose fill from Pure Radiation Block.
 * No longer applies effects directly — feeds RadiationManager.DOSE instead.
 *   <= 8  blocks : +20 dose/sec
 *   <= 16 blocks : +10 dose/sec
 *   <= 32 blocks : +5  dose/sec
 */
public final class RadiationFieldManager {
    private RadiationFieldManager() {
    }

    private static final int SCAN = 32;
    private static final int STEP = 2;

    private static double nearestDistSq(ServerLevel level, BlockPos c) {
        double best = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN; dx <= SCAN; dx += STEP) {
            for (int dy = -SCAN; dy <= SCAN; dy += STEP) {
                for (int dz = -SCAN; dz <= SCAN; dz += STEP) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (level.getBlockState(m).is(ModBlocks.PURE_RADIATION_BLOCK)) {
                        double d = dx * dx + dy * dy + dz * dz;
                        if (d < best) best = d;
                    }
                }
            }
        }
        return best;
    }

    public static void tickPlayer(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            return;
        }
        double dsq = nearestDistSq(level, player.blockPosition());
        if (dsq == Double.MAX_VALUE) return;
        double d = Math.sqrt(dsq);
        float dose;
        if (d <= 8.0)       dose = 20.0F;
        else if (d <= 16.0) dose = 10.0F;
        else                dose = 5.0F;
        RadiationManager.addDose(player, dose);
    }
}
