package com.example.alieninvasion.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Comms Beacon: a multiplayer coordination tool. Right-click to broadcast your
 * name and coordinates to every player on the server, fire a bright signal beam
 * over your head and a ping sound - so co-op teams can rally, call for help or
 * mark a hive without typing.
 */
public class CommsBeaconItem extends Item {
    public CommsBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockPos pos = player.blockPosition();
            Component msg = Component.literal("§b[Маяк связи] §f" + player.getName().getString()
                    + " §7→ §aX:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ());
            MinecraftServer server = sp.getServer();
            if (server != null) {
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    other.sendSystemMessage(msg);
                    other.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
                }
            }
            // Signal beam column over the sender.
            if (level instanceof ServerLevel sl) {
                for (int i = 0; i < 24; i++) {
                    sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + i * 0.6D, player.getZ(),
                            2, 0.05, 0.1, 0.05, 0.0);
                }
            }
        }

        player.getCooldowns().addCooldown(this, 100); // 5s anti-spam
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
