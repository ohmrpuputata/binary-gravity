package com.example.alieninvasion.block;

import com.example.alieninvasion.world.InvasionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Blueprint Table: the field manual. Right-click (empty hand) to read the high-tier
 * "blueprints" - the apex weapon recipes and what each fabrication machine does -
 * plus a live invasion-day readout. It closes the discovery gap for the gated
 * end-game crafts so players know what to build toward, without bloating the mod
 * with a whole research system.
 */
public class BlueprintTableBlock extends Block {
    public BlueprintTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide) {
            int day = 0;
            boolean won = false;
            if (level instanceof ServerLevel sl) {
                InvasionManager mgr = InvasionManager.get(sl);
                day = mgr.getInvasionDays();
                won = mgr.isVictoryAchieved();
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        18, 0.4D, 0.4D, 0.4D, 0.4D);
            }
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("§b===== ПОЛЕВОЙ МАНУАЛ ====="), false);
            player.displayClientMessage(Component.literal("§7День вторжения: §f" + day
                    + (won ? "  §a[РОЙ ПОВЕРЖЕН]" : "  §c[Мать Роя — День 8]")), false);
            player.displayClientMessage(Component.literal("§dApex-оружие:"), false);
            player.displayClientMessage(Component.literal(
                    "§f• Bio-Blade §7= 4 alien alloy + 1 cosmic block + 1 hive core + 1 dark matter shard"), false);
            player.displayClientMessage(Component.literal(
                    "§f• Cosmic Warhammer §7= 7 cosmic block + 1 dark matter shard"), false);
            player.displayClientMessage(Component.literal(
                    "§f• Star Cleaver §7= 4 cosmic ingot + 2 diamond"), false);
            player.displayClientMessage(Component.literal("§dФабрикация (ПКМ по блоку сырьём):"), false);
            player.displayClientMessage(Component.literal(
                    "§f• Переработчик §7= 6 scrap → 1 alloy    §f• Промывщик §7= raw → 2× ingot"), false);
            player.displayClientMessage(Component.literal(
                    "§f• Радиокузня §7= плавка без топлива    §f• Станция дезактивации §7= чистит радиацию"), false);
            player.displayClientMessage(Component.literal(
                    "§dApex-награда: §7победа над Матерью Роя роняет Bio-Blade + dark matter + hive cores."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
