package com.example.alieninvasion.logic;

import com.example.alieninvasion.ai.BlockBreakGoal;
import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.mixin.MobAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

// Make vanilla mobs smarter:
// Graft a dig-to-reach goal onto vanilla mobs (hostile and allied) so they
// outplay a player by going THROUGH soft cover instead of standing there.
public final class SmartMobs {
    private SmartMobs() {
    }

    // Only soft, cheap cover - dirt/sand/wood/leaves/glass/wool. A vanilla mob can
    // breach a flimsy hide-out but not chew through a real stone or obsidian base;
    // it wins by being clever, not by raw block-breaking.
    private static final Predicate<BlockState> SOFT_COVER = state ->
            state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                    || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL) || state.is(BlockTags.PLANKS)
                    || state.is(BlockTags.LOGS) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                    || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.HAY_BLOCK)
                    || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.is(Blocks.NETHERRACK);
    // NOTE: doors/trapdoors deliberately excluded - the constant door-banging was
    // maddening, and vanilla zombies already break doors on hard difficulty.

    public static void register() {
        injectSmartGoals();
    }

    // --- smarter vanilla mobs --------------------------------------------
    private static void injectSmartGoals() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof PathfinderMob mob)) {
                return;
            }
            if (AlienUtils.isAlliedTo(null, entity)) {
                return; // our own aliens already have these goals
            }
            // Spiders climb walls already - giving them block-breaking / pillaring
            // makes no sense, so they're skipped entirely (they parkour by nature).
            if (entity instanceof net.minecraft.world.entity.monster.Spider) {
                return;
            }
            // Hostiles (the threat) plus iron golems. Wolves are NOT included - a pet
            // chewing through the front door was absurd.
            boolean candidate = (entity instanceof Monster && !(entity instanceof EnderMan))
                    || entity instanceof IronGolem;
            if (!candidate) {
                return;
            }
            GoalSelector goals = ((MobAccessor) mob).getGoalSelector();
            // Priority 1 so these take over movement only when genuinely stuck or the
            // target is out of reach (their canUse stays false the rest of the time):
            //  - tower up with DIRT to a player perched out of reach;
            //  - dig through soft cover toward a player walled in with weak blocks;
            //  - PARKOUR: pounce-leap across gaps / up ledges toward the target.
            goals.addGoal(1, new com.example.alieninvasion.ai.PillarUpGoal(mob, Blocks.DIRT.defaultBlockState()));
            goals.addGoal(2, new BlockBreakGoal(mob, SOFT_COVER, 55));
            goals.addGoal(3, new com.example.alieninvasion.ai.AlienLeapGoal(mob, 0.5F));
        });
    }
}
