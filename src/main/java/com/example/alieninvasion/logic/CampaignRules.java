package com.example.alieninvasion.logic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

// Campaign rules: this is an Overworld-only survival run. The Nether and the End
// are disabled - any player who ends up outside the Overworld is immediately
// pulled back. The win condition (survive 8 days) lives in InvasionManager.
public final class CampaignRules {
    private CampaignRules() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() == Level.OVERWORLD) {
                return; // the only allowed dimension
            }
            if (level.players().isEmpty()) {
                return;
            }
            ServerLevel overworld = level.getServer().overworld();
            // Copy first: teleporting removes the player from this level's list.
            for (ServerPlayer player : List.copyOf(level.players())) {
                pullToOverworld(player, overworld);
            }
        });
    }

    private static void pullToOverworld(ServerPlayer player, ServerLevel overworld) {
        BlockPos target;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension() == Level.OVERWORLD) {
            target = respawn;
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
            target = new BlockPos(spawn.getX(), y, spawn.getZ());
        }
        player.teleportTo(overworld, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal(
                "§cИзмерения отключены. Цель — выжить 8 дней в Верхнем мире."));
    }
}
