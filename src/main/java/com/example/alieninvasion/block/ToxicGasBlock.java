package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Ядовитое облако: внутри лениво дрейфуют зелёные споры/дымка — чтобы зона
 *  «дышала» и читалась как опасная атмосфера, а не просто цветной куб. */
public class ToxicGasBlock extends Block {
    public ToxicGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ModParticles.ACID_SMOKE,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.01D + random.nextDouble() * 0.02D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }
}
