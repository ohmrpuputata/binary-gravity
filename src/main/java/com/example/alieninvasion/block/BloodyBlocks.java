package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        if (state.is(BlockTags.WOODEN_STAIRS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_STAIRS.defaultBlockState());
        }
        if (state.is(BlockTags.STAIRS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_STONE_STAIRS.defaultBlockState());
        }
        if (state.is(BlockTags.WOODEN_SLABS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_SLAB.defaultBlockState());
        }
        if (state.is(BlockTags.SLABS)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_STONE_SLAB.defaultBlockState());
        }
        if (state.is(BlockTags.WOODEN_FENCES)) {
            return ContaminationRules.copyProperties(state, ModBlocks.BLOODY_PLANK_FENCE.defaultBlockState());
        }
        // Unsupported partial shapes and mechanisms keep their original state and
        // receive BLOOD_LAYER in splatter(). Replacing a lever, rail or redstone
        // component with a full bloody material cube would break the mechanism.
        if (state.getDestroySpeed(level, pos) < 0.0F
                || !state.isCollisionShapeFullBlock(level, pos)
                || !state.isSolidRender(level, pos)) {
            return null;
        }
        BlockState contaminated = ContaminationRules.contaminatedStateFor(state);
        if (contaminated != null) {
            Block categoryVariant = ModBlocks.cleanBloodyVariantForInfested(contaminated.getBlock());
            if (categoryVariant != null) {
                return ContaminationRules.copyProperties(state, categoryVariant.defaultBlockState());
            }
        }
        // Полнокубические НЕПРОЗРАЧНЫЕ блоки — кровавый куб по типу звука. НЕ трогаем:
        // тайл-энтити (сундуки/машины/реактор), небьющиеся, частичные И прозрачные
        // (листва/стекло/лёд) — иначе заражённая листва превращалась в земляной куб.
        if (state.hasBlockEntity()) {
            return null;
        }
        return cubeFor(state).defaultBlockState();
    }

    /** Подбор кровавого куба под исходный блок по типу звука/категории. */
    private static net.minecraft.world.level.block.Block cubeFor(BlockState state) {
        // Заражённые блоки Роя — отдельная кровавая текстура (заражёнка + кровь),
        // чтобы не выглядело как чужеродный обычный камень.
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key.getNamespace().equals("alien-invasion") && key.getPath().startsWith("infested")) {
            SoundType s = state.getSoundType();
            boolean ground = s == SoundType.GRAVEL || s == SoundType.SAND || s == SoundType.SNOW
                    || s == SoundType.ROOTED_DIRT || s == SoundType.MUD || s == SoundType.GRASS
                    || s == SoundType.WET_GRASS || s == SoundType.CROP || s == SoundType.MOSS;
            return ground ? ModBlocks.BLOODY_INFESTED_DIRT : ModBlocks.BLOODY_INFESTED;
        }
        if (state.is(BlockTags.PLANKS)) return ModBlocks.BLOODY_PLANKS;
        if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS) || state.is(ModBlocks.INFESTED_STONE_BRICKS)) {
            return ModBlocks.BLOODY_STONE_BRICKS;
        }
        SoundType snd = state.getSoundType();
        if (snd == SoundType.WOOD || snd == SoundType.NETHER_WOOD || snd == SoundType.BAMBOO
                || snd == SoundType.BAMBOO_WOOD || snd == SoundType.CHERRY_WOOD || snd == SoundType.WOOL) {
            return ModBlocks.BLOODY_PLANKS;
        }
        if (snd == SoundType.GRAVEL || snd == SoundType.SAND || snd == SoundType.SNOW
                || snd == SoundType.ROOTED_DIRT || snd == SoundType.MUD || snd == SoundType.CROP
                || snd == SoundType.GRASS || snd == SoundType.MOSS || snd == SoundType.SOUL_SAND
                || snd == SoundType.SOUL_SOIL || snd == SoundType.NYLIUM) {
            return ModBlocks.BLOODY_DIRT;
        }
        return ModBlocks.BLOODY_STONE;
    }

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
        BlockPos top = under.above();
        BlockState above = level.getBlockState(top);
        if ((above.isAir() || above.canBeReplaced())
                && above.getFluidState().isEmpty()
                && !above.is(ModBlocks.BLOOD_LAYER)) {
            level.setBlock(top, ModBlocks.BLOOD_LAYER.defaultBlockState(), 2 | 16);
        }
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
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.3F);
            return restoreState(level, pos, state, fallbackClean);
        }
        return null;
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
    }
}
