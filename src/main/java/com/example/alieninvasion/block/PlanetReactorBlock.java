package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.HomeworldManager;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Реактор охотника: «НАСТОЯЩАЯ пушка» Макса — бомба для столицы Роя. Ставится
 * только в родном мире Роя, в ГОРОДЕ (у центрального шпиля). После установки —
 * 1:40 до детонации всей планеты; рой бросает всё, чтобы его сгрызть, бомбу
 * нужно отбивать.
 */
public class PlanetReactorBlock extends Block implements EntityBlock {

    public PlanetReactorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlanetReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof PlanetReactorBlockEntity reactor) {
                reactor.tickServer();
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        boolean inHomeworld = level.dimension().equals(HomeworldManager.HOMEWORLD);
        double dx = pos.getX() - HomeworldManager.CITY_CENTER.getX();
        double dz = pos.getZ() - HomeworldManager.CITY_CENTER.getZ();
        boolean inCity = dx * dx + dz * dz <= HomeworldManager.BOMB_PLACEMENT_RADIUS * HomeworldManager.BOMB_PLACEMENT_RADIUS;
        if (!inHomeworld || !inCity) {
            if (placer instanceof Player p) {
                p.displayClientMessage(Component.literal(
                        "§c[Бомба] Заряд настроен на столицу Роя. Тащи его в ГОРОД и ставь у центрального шпиля."), false);
            }
            level.destroyBlock(pos, true); // возвращаем предмет
            return;
        }
        if (level.getBlockEntity(pos) instanceof PlanetReactorBlockEntity reactor) {
            reactor.arm();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PlanetReactorBlockEntity reactor) {
            player.displayClientMessage(Component.literal(reactor.statusLine()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PlanetReactorBlockEntity reactor) {
            reactor.onBroken();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
