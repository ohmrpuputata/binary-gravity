package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ItemRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Behaviour for the mod's tools:
 *   - Bio-Pickaxe / Bio-Shovel : 3x3 area mining (sneak to mine one block).
 *   - Cosmic Pickaxe           : 5x5 area mining (even better - sneak for one).
 *   - Bio-Axe / Star Cleaver   : fell the entire tree from one cut.
 * Only blocks the tool can actually harvest are broken (chests, hives and
 * unbreakable blocks are spared), and each extra block costs durability.
 */
public class CosmicToolEvents {
    private static boolean breaking = false;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (breaking || !(world instanceof ServerLevel level) || player.isSpectator()) return;
            ItemStack tool = player.getMainHandItem();
            Item item = tool.getItem();

            // --- Axes fell whole trees ---
            if (item == ItemRegistry.BIO_AXE || item == ItemRegistry.STAR_CLEAVER) {
                if (state.is(BlockTags.LOGS)) {
                    breaking = true;
                    try { fellTree(level, pos, player, tool); } finally { breaking = false; }
                }
                return;
            }

            // --- Pickaxes / shovel area-mine ---
            int radius;
            if (item == ItemRegistry.COSMIC_PICKAXE) radius = 2;        // 5x5
            else if (item == ItemRegistry.BIO_PICKAXE) radius = 1;      // 3x3
            else if (item == ItemRegistry.BIO_SHOVEL) radius = 1;       // 3x3
            else return;

            if (player.isShiftKeyDown()) return;                       // sneak = single block
            if (!tool.isCorrectToolForDrops(state)) return;            // centre must be a valid target

            Vec3 look = player.getViewVector(1.0F);
            Direction facing = Direction.getNearest(look.x, look.y, look.z);

            breaking = true;
            try {
                for (BlockPos p : plane(pos, facing, radius)) {
                    if (p.equals(pos)) continue;
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (level.getBlockEntity(p) != null) continue;     // never eat chests/containers
                    if (s.getDestroySpeed(level, p) < 0) continue;     // skip unbreakable
                    if (!tool.isCorrectToolForDrops(s)) continue;      // only tool-appropriate blocks
                    level.destroyBlock(p, true, player);
                    if (!player.getAbilities().instabuild) {
                        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        if (tool.isEmpty()) break;
                    }
                }
            } finally {
                breaking = false;
            }
        });
    }

    /** All blocks of an NxN plane perpendicular to the dig direction (N = 2*radius+1). */
    private static List<BlockPos> plane(BlockPos center, Direction facing, int radius) {
        List<BlockPos> out = new ArrayList<>();
        Direction.Axis axis = facing.getAxis();
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                switch (axis) {
                    case Y -> out.add(center.offset(a, 0, b));
                    case X -> out.add(center.offset(0, a, b));
                    case Z -> out.add(center.offset(a, b, 0));
                }
            }
        }
        return out;
    }

    /** Flood-fill connected logs upward/around the cut and break them all. */
    private static void fellTree(ServerLevel level, BlockPos start, net.minecraft.world.entity.player.Player player, ItemStack tool) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logs = new ArrayList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && logs.size() < 256) {
            BlockPos c = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {          // bias upward (trees grow up)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos np = c.offset(dx, dy, dz);
                        if (visited.contains(np)) continue;
                        if (level.getBlockState(np).is(BlockTags.LOGS)) {
                            visited.add(np);
                            queue.add(np);
                            logs.add(np);
                        }
                    }
                }
            }
        }
        for (BlockPos p : logs) {
            level.destroyBlock(p, true, player);
            if (!player.getAbilities().instabuild) {
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (tool.isEmpty()) break;
            }
        }
    }
}
