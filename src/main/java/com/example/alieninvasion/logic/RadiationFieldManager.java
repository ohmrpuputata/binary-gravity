package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Proximity-based dose fill from Pure Radiation Block.
 * No longer applies effects directly — feeds RadiationManager.DOSE instead.
 *   <= 2  blocks : +20 dose/sec
 *   <= 4  blocks : +10 dose/sec
 *   <= 8  blocks : +5  dose/sec
 */
public final class RadiationFieldManager {
    private RadiationFieldManager() {
    }

    private static final int SCAN = 8;
    private static final int STEP = 1;

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

    /** Last measured field strength per player — drives the geiger counter clicks. */
    private static final java.util.Map<java.util.UUID, Float> LAST_FIELD =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static float getFieldLevel(ServerPlayer player) {
        return LAST_FIELD.getOrDefault(player.getUUID(), 0.0F);
    }

    public static float getFieldLevel(java.util.UUID id) {
        return LAST_FIELD.getOrDefault(id, 0.0F);
    }

    public static void tickPlayer(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) {
            LAST_FIELD.remove(player.getUUID());
            return;
        }
        double dsq = nearestDistSq(level, player.blockPosition());
        if (dsq == Double.MAX_VALUE) {
            LAST_FIELD.put(player.getUUID(), 0.0F);
            return;
        }
        double d = Math.sqrt(dsq);
        float dose;
        if (d <= 2.0)      dose = 20.0F;
        else if (d <= 4.0) dose = 10.0F;
        else               dose = 5.0F;
        LAST_FIELD.put(player.getUUID(), dose);
        RadiationManager.addDose(player, dose);
    }
}
