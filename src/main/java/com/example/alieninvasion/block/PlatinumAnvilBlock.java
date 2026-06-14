package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Platinum Anvil — a crafting station with a 3×3 grid, a catalyst slot
 * (amethyst shard / radiation shard / dark matter shard / cosmic shard),
 * and a result slot.  Degrades on use like vanilla anvil (3 damage stages).
 */
public class PlatinumAnvilBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 2);

    private static final Component CONTAINER_TITLE = Component.translatable("container.alien-invasion.platinum_anvil");

    /* ---------- Vanilla anvil collision shapes ---------- */
    private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

    private static final VoxelShape X_LEG  = Block.box(3.0, 4.0, 4.0, 13.0, 5.0, 12.0);
    private static final VoxelShape X_WAIST = Block.box(4.0, 5.0, 6.0, 12.0, 10.0, 10.0);
    private static final VoxelShape X_TOP  = Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape X_SHAPE = Shapes.or(BASE, X_LEG, X_WAIST, X_TOP);

    private static final VoxelShape Z_LEG  = Block.box(4.0, 4.0, 3.0, 12.0, 5.0, 13.0);
    private static final VoxelShape Z_WAIST = Block.box(6.0, 5.0, 4.0, 10.0, 10.0, 12.0);
    private static final VoxelShape Z_TOP  = Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    private static final VoxelShape Z_SHAPE = Shapes.or(BASE, Z_LEG, Z_WAIST, Z_TOP);

    public PlatinumAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(DAMAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DAMAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new PlatinumAnvilMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    CONTAINER_TITLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Randomly damage the anvil after a successful craft (12 % chance, like vanilla).
     * Called from {@link PlatinumAnvilMenu#consumeIngredients()}.
     */
    public static void damage(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PlatinumAnvilBlock)) return;
        int dmg = state.getValue(DAMAGE);
        if (dmg >= 2) {
            level.removeBlock(pos, false);
            level.levelEvent(1029, pos, 0); // anvil destroy particles + sound
        } else {
            level.setBlock(pos, state.setValue(DAMAGE, dmg + 1), 2);
            level.levelEvent(1030, pos, 0); // anvil use sound
        }
    }
}
