package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AlienHiveBlock extends Block {

    public AlienHiveBlock(Properties properties) {
        super(properties);
    }

    // Looting a hive rewards the player: always some Alien Alloy, rarely a Hive
    // Core. Gives a reason to clear infestations.
    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide) {
            RandomSource rng = level.getRandom();
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.ALIEN_ALLOY, 1 + rng.nextInt(2)));
            if (rng.nextFloat() < 0.15F) {
                Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(
                        com.example.alieninvasion.registry.ItemRegistry.HIVE_CORE));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    private static long lastSpawnTime = 0;

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        long gameTime = level.getGameTime();
        if (gameTime - lastSpawnTime < 2400L) {
            return;
        }

        // Spawn Grunt logic (Reproduction)
        if (random.nextInt(50) == 0) { // Rare chance
            if (level.getEntitiesOfClass(AlienGruntEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(32))
                    .size() < 10) {
                com.example.alieninvasion.entity.AlienGruntEntity grunt = EntityRegistry.ALIEN_GRUNT.create(level);
                if (grunt != null) {
                    grunt.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
                    grunt.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
                    level.addFreshEntity(grunt);
                    lastSpawnTime = gameTime;
                }
            }
        }
    }
}
