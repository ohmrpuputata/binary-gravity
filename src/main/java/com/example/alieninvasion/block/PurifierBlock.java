package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.ContaminationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Late-game keystone: the Purifier. Placed by the player, it actively cleanses
// the alien infestation around it and damages/repels any alien that comes near,
// letting you carve out and HOLD safe territory in the endless invasion.
// Crafted from Hive Cores (boss/hive loot) + Alien Alloy.
public class PurifierBlock extends Block {
    private static final int RADIUS = 6;

    public PurifierBlock(BlockBehaviour.Properties properties) {
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
        cleanse(level, pos, random);
        repel(level, pos);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                4, 0.3, 0.3, 0.3, 0.01);
        level.scheduleTick(pos, this, 20); // keep ticking
    }

    private void cleanse(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 12; i++) {
            BlockPos p = pos.offset(random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    random.nextInt(RADIUS * 2 + 1) - RADIUS, random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState s = level.getBlockState(p);
            BlockState clean = ContaminationRules.cleanStateFor(s);
            if (clean != null) {
                level.setBlockAndUpdate(p, clean);
            }
        }
    }

    private void repel(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, e -> AlienUtils.isAlliedTo(null, e))) {
            Vec3 away = mob.position().subtract(center);
            if (away.lengthSqr() < 1.0e-3) {
                away = new Vec3(1.0, 0.0, 0.0);
            }
            away = away.normalize().scale(0.6);
            mob.push(away.x, 0.25, away.z);
            mob.hurtMarked = true;
            mob.hurt(level.damageSources().magic(), 2.0F);
        }
    }
}
