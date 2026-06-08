package com.example.alieninvasion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
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
import com.example.alieninvasion.registry.ItemRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Плазменная турель (Plasma Turret):
 * Автоматическое защитное устройство. Стреляет плазменными зарядами по пришельцам в радиусе 20 блоков.
 * Требует зарядки космическими батареями при помощи правого клика.
 */
public class PlasmaTurretBlock extends Block implements EntityBlock {

    public PlasmaTurretBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlasmaTurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof PlasmaTurretBlockEntity turret) {
                turret.tickServer();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlasmaTurretBlockEntity turret) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b[Плазменная турель] Энергия: " + turret.getCharge() + "%"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ItemRegistry.ALIEN_BATTERY)) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PlasmaTurretBlockEntity turret) {
                    if (turret.getCharge() < 100) {
                        turret.setCharge(Math.min(100, turret.getCharge() + 50));
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.5F);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a[Плазменная турель] Заряжена! Энергия: " + turret.getCharge() + "%"), true);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[Плазменная турель] Уже полностью заряжена!"), true);
                    }
                }
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
