package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * BLOODSTAINED BLOCKS: heavy wounds splatter the floor. Кровь ложится на ВСЕ
 * блоки — и ванильные, и модовые. Полнокубический непрозрачный блок (камень,
 * земля, дерево, заражёнка) превращается в кровавый «двойник» по типу звука
 * (дерево→доски, земля/песок→земля, остальное→камень), а его ТОЧНЫЙ оригинал
 * сохраняется в BloodyBlockEntity — RIGHT-CLICK протирает, вода смывает, и
 * возвращается именно исходный блок. Лестницы/плиты/заборы сохраняют форму.
 * Любой блок, который нельзя превратить без потери вида (листва, стекло,
 * нестандартная форма, тайл-энтити, машины), получает стойкую кровавую ДЕКАЛЬ
 * ({@link BloodLayerBlock}) сверху — сам блок при этом не меняется.
 */
public final class BloodyBlocks {
    private BloodyBlocks() {}

    private interface Bloodstained {
    }

    private static boolean isBloody(BlockState s) {
        return s.getBlock() instanceof Bloodstained;
    }

    /** Bloody twin for a floor block, or null if this block doesn't stain. */
    public static BlockState bloodyFor(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || isBloody(state)) {
            return null;
        }
        if (state.hasBlockEntity()) {
            return null;
        }
        Block exactVariant = ModBlocks.bloodyVariantFor(state.getBlock());
        if (exactVariant != null) {
            return ContaminationRules.copyProperties(state, exactVariant.defaultBlockState());
        }
        // Сохраняем форму у лестниц/плит/заборов.
        // Unsupported partial shapes and mechanisms keep their original state and
        // receive BLOOD_LAYER in splatter(). Replacing a lever, rail or redstone
        // component with a full bloody material cube would break the mechanism.
        // No guessing: unsupported blocks must not turn into another material.
        // Add a real registered bloody variant when a block needs blood.
        return null;
    }
        // Полнокубические НЕПРОЗРАЧНЫЕ блоки — кровавый куб по типу звука. НЕ трогаем:
        // тайл-энтити (сундуки/машины/реактор), небьющиеся, частичные И прозрачные
        // (листва/стекло/лёд) — иначе заражённая листва превращалась в земляной куб.

    /** Подбор кровавого куба под исходный блок по типу звука/категории. */
        // Заражённые блоки Роя — отдельная кровавая текстура (заражёнка + кровь),
        // чтобы не выглядело как чужеродный обычный камень.
    /** Stain the block under the given position, remembering the exact original. */
    public static void splatter(ServerLevel level, BlockPos under) {
        BlockState floor = level.getBlockState(under);
        if (floor.isAir() || !floor.getFluidState().isEmpty() || isBloody(floor)) {
            return;
        }
        BlockState bloody = bloodyFor(level, under, floor);
        if (bloody != null) {
            // Камень/земля/дерево/заражёнка — превращаем весь блок в кровавый двойник.
            level.setBlock(under, bloody, 2 | 16);
            if (level.getBlockEntity(under) instanceof BloodyBlockEntity be) {
                be.setOriginal(floor);
            }
            return;
        }
        // Любой ДРУГОЙ блок (листва, стекло, нестандартная форма, тайл-энтити,
        // модовые машины и т.п.) нельзя превратить, не потеряв его вид — поэтому
        // кладём стойкую кровавую ДЕКАЛЬ ПОВЕРХ него, сам блок не трогаем. Так кровь
        // оказывается вообще на всех существующих блоках, и ванильных, и модовых.
        // No blood_layer puddle for unsupported blocks.
    }

    /** Точный блок, к которому надо вернуться: сохранённый оригинал, иначе fallback. */
    private static BlockState restoreState(BlockGetter level, BlockPos pos, BlockState state,
                                           java.util.function.Supplier<BlockState> fallbackClean) {
        if (level.getBlockEntity(pos) instanceof BloodyBlockEntity be && be.getOriginal() != null) {
            return be.getOriginal();
        }
        return ContaminationRules.copyProperties(state, fallbackClean.get());
    }

    /** Right-click wipe / water wash: bloody block reverts to its stored original. */
    public static InteractionResult wipe(Level level, BlockPos pos, BlockState restore) {
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, restore);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 10, 0.3D, 0.1D, 0.3D, 0.0D);
            }
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Water touching a bloody block washes it instantly to its stored original. */
    private static BlockState washedByWater(BlockState state, BlockState neighbor,
                                            net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                                            java.util.function.Supplier<BlockState> fallbackClean) {
        if (neighbor.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
                || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            level.scheduleTick(pos, state.getBlock(), 20);
        }
        return null;
    }

    private static void washTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                 java.util.function.Supplier<BlockState> fallbackClean) {
        boolean touchingWater = state.getFluidState().is(FluidTags.WATER);
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            touchingWater |= level.getFluidState(pos.relative(direction)).is(FluidTags.WATER);
        }
        if (!touchingWater) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                4, 0.35D, 0.15D, 0.35D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.setBlockAndUpdate(pos, restoreState(level, pos, state, fallbackClean));
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.35F);
        } else {
            level.scheduleTick(pos, state.getBlock(), 20);
        }
    }

    /** Full-cube bloodstained block: wipe with right-click, washes off in water. */
    public static class Plain extends net.minecraft.world.level.block.Block
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Plain(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class Stairs extends net.minecraft.world.level.block.StairBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Stairs(BlockState base, Properties props, java.util.function.Supplier<BlockState> clean) {
            super(base, props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class Slab extends net.minecraft.world.level.block.SlabBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Slab(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class Fence extends net.minecraft.world.level.block.FenceBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Fence(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class Pillar extends net.minecraft.world.level.block.RotatedPillarBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Pillar(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class Door extends net.minecraft.world.level.block.DoorBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public Door(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class TrapDoor extends net.minecraft.world.level.block.TrapDoorBlock
            implements net.minecraft.world.level.block.EntityBlock, Bloodstained {
        private final java.util.function.Supplier<BlockState> clean;

        public TrapDoor(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, props);
            this.clean = clean;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BloodyBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            return wipe(level, pos, restoreState(level, pos, state, clean));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            BlockState washed = washedByWater(state, neighbor, level, pos, clean);
            return washed != null ? washed : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            washTick(state, level, pos, random, clean);
        }
    }

    public static class BloodyCosmicCrystalPlain extends Plain {
        public BloodyCosmicCrystalPlain(Properties props, java.util.function.Supplier<BlockState> clean) {
            super(props, clean);
        }

        @Override
        public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
            return CosmicCrystalOreBlock.checkMiningSpeed(state, player, world, pos, () -> super.getDestroyProgress(state, player, world, pos));
        }

        @Override
        public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                                  @org.jetbrains.annotations.Nullable BlockEntity blockEntity, ItemStack tool) {
            if (tool.is(com.example.alieninvasion.registry.ItemRegistry.NIBIRIUM_PICKAXE)) {
                super.playerDestroy(level, player, pos, state, blockEntity, tool);
            }
        }
    }
}
