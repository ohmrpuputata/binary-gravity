package com.example.alieninvasion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Тонкая лужа-декаль на полу — остаётся от дождя и СО ВРЕМЕНЕМ ВЫСЫХАЕТ.
 *
 * ACID — вид лужи: false = обычная вода (синяя), true = кислота (зелёная, от
 * кислотного дождя). AMOUNT (0..3) — таймер высыхания: каждый случайный тик лужа
 * подсыхает на ступень и в итоге исчезает; свежий дождь подливает новые.
 *
 * Форма ПУСТАЯ (как у крови) — курсор и шаги проходят сквозь лужу, она не мешает
 * ставить/ломать блоки и не цепляет ноги. Снимается, если убрать блок под ней.
 */
public class RainPuddleBlock extends Block {
    public static final BooleanProperty ACID = BooleanProperty.create("acid");
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 0, 3);
    public static final int MAX_AMOUNT = 3;

    public RainPuddleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACID, false).setValue(AMOUNT, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACID, AMOUNT);
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
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Высыхание: лужа испаряется ступенями и пропадает. Под дождём в небо она почти
        // не сохнет (свежая влага), в укрытии/после дождя — сохнет.
        boolean watered = level.isRaining() && level.canSeeSky(pos);
        if (watered && random.nextInt(3) != 0) {
            return;
        }
        int amount = state.getValue(AMOUNT);
        if (amount > 0) {
            level.setBlock(pos, state.setValue(AMOUNT, amount - 1), 2);
        } else {
            level.removeBlock(pos, false);
        }
    }
}
