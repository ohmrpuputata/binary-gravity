package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    // While held, the device ticks like a real Geiger counter - click rate scales
    // with the local field and the player's accumulated dose.
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        if (level.isClientSide || !selected || !(entity instanceof Player p)) {
            return;
        }
        if (level.getGameTime() % 6 != 0) {
            return; // sample on a steady beat, not every tick
        }
        int field = com.example.alieninvasion.logic.RadiationManager.scanField(level, p.blockPosition());
        float dose = com.example.alieninvasion.logic.RadiationManager.getDose(p);
        float intensity = Math.max(field, dose * 0.4F);
        if (intensity > 0.0F && level.random.nextFloat() < Math.min(0.9F, 0.15F + intensity * 0.03F)) {
            level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.6F + level.random.nextFloat() * 0.8F);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int danger = com.example.alieninvasion.logic.RadiationManager.scanField(level, player.blockPosition());
            int dose = (int) com.example.alieninvasion.logic.RadiationManager.getDose(player);
            String fieldColor = danger >= 18 ? "§c" : danger >= 8 ? "§e" : "§a";
            String doseColor = dose >= 80 ? "§4" : dose >= 45 ? "§c" : dose >= 15 ? "§e" : "§a";
            player.displayClientMessage(Component.literal(
                    fieldColor + "Фон: " + danger + " мкЗв/ч  " + doseColor + "| Доза: " + dose + " рад"), true);
            if (com.example.alieninvasion.logic.RadiationManager.isStormActive()) {
                player.displayClientMessage(Component.literal("§4☢ Радиоактивная буря!"), false);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
