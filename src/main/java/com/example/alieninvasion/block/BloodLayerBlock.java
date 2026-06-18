package com.example.alieninvasion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Кровавая декаль поверх ЛЮБОЙ грани блока (пол / стены / потолок).
 *
 * FACING — направление к опорной поверхности (DOWN = лежит на полу, NORTH/… = на
 * стене, UP = на потолке). AMOUNT (0..3) — стадия накопления: капли → пятно →
 * лужа → большая лужа; брызги в одном месте растят её.
 *
 * Форма выделения ПУСТАЯ: курсор проходит сквозь декаль к блоку под ней, поэтому
 * она НЕ мешает ставить/ломать блоки. Снимается водой или когда ломают/заменяют
 * блок-носитель. Чисто визуальный слой (нет коллизии, replaceable).
 */
public class BloodLayerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 0, 3);
    /** Заражённая/чужая кровь — фиолетовый ихор вместо красного. */
    public static final BooleanProperty INFECTED = BooleanProperty.create("infected");
    public static final int MAX_AMOUNT = 3;

    public BloodLayerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.DOWN).setValue(AMOUNT, 0).setValue(INFECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AMOUNT, INFECTED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing);
        // Грань опоры, обращённая к декали — это противоположная направлению к опоре.
        return level.getBlockState(support).isFaceSturdy(level, support, facing.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (neighbor.getFluidState().is(FluidTags.WATER) || state.getFluidState().is(FluidTags.WATER)) {
            level.scheduleTick(pos, this, 20);
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean touchingWater = state.getFluidState().is(FluidTags.WATER);
        for (Direction direction : Direction.values()) {
            touchingWater |= level.getFluidState(pos.relative(direction)).is(FluidTags.WATER);
        }
        if (!touchingWater) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D,
                3, 0.28D, 0.04D, 0.28D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.35F, 1.45F);
        } else {
            level.scheduleTick(pos, this, 20);
        }
    }
}
