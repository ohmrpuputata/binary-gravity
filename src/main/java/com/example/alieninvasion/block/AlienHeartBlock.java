package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ChunkContaminationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * INFECTION HEART: a pulsing organ hidden in heavily infested chunks. Destroy it
 * and the chunk goes INERT — the rot that's already there stays, but the global
 * contamination never grows it again (a searcher's alternative to the Purifier,
 * which scrubs AND protects but must keep standing). Drops a Hive Core.
 */
public class AlienHeartBlock extends Block {
    public AlienHeartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Heartbeat: blood mist + a low thump that helps players HEAR it nearby.
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                8, 0.5D, 0.4D, 0.5D, 0.01D);
        level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS,
                1.4F, 0.65F + random.nextFloat() * 0.1F);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && !state.is(newState.getBlock())) {
            ChunkPos cp = new ChunkPos(pos);
            ChunkContaminationData.get(serverLevel).setInert(cp, true);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, 6, 0.6D, 0.6D, 0.6D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, 40, 1.2D, 1.0D, 1.2D, 0.05D);
            serverLevel.playSound(null, pos, SoundEvents.WARDEN_DEATH, SoundSource.BLOCKS, 1.2F, 1.4F);
            for (ServerPlayer p : serverLevel.players()) {
                if (p.blockPosition().distSqr(pos) < 64 * 64) {
                    p.displayClientMessage(Component.literal(
                            "§a§lСердце заражения уничтожено! §rЗараза в этом чанке больше не растёт."), false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
