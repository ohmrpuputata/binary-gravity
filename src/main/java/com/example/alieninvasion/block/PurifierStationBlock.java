package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.RadiationManager;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Decontamination Station: a placed anchor that continuously scrubs radiation
 * DOSE (and radiation/poison effects) off any player standing near it - turning a
 * sealed room into a safe haven from the dose-based radiation system. Distinct
 * from the Purifier block, which cleanses infested terrain and repels aliens.
 */
public class PurifierStationBlock extends Block {
    private static final double RADIUS = 8.0D;

    public PurifierStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB box = new AABB(pos).inflate(RADIUS);
        boolean cleansedSomeone = false;
        for (Player player : level.getEntitiesOfClass(Player.class, box, Player::isAlive)) {
            RadiationManager.reduceDose(player, 4.0F);
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            player.removeEffect(MobEffects.POISON);
            cleansedSomeone = true;
        }
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.01);
        if (cleansedSomeone && random.nextInt(4) == 0) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
        level.scheduleTick(pos, this, 20);
    }
}
