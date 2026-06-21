package com.example.alieninvasion.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BlasterIIItem extends AlienBlasterItem {
    public BlasterIIItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public float getHeatIncreasePerShot() {
        return 1.5F; // Heats up significantly slower than Blaster I (3.0F)
    }

    @Override
    public boolean hasAlternativeFire() {
        return true;
    }

    @Override
    public void handleAlternativeFire(Player player, ItemStack stack) {
        Level level = player.level();
        if (level.isClientSide) return;

        if (isOverheated(stack)) {
            player.displayClientMessage(Component.literal("§c[!] Бластер перегрет!"), true);
            return;
        }

        if (getAltAttackCooldown(stack) > 0) {
            return;
        }

        // Ammo check: Amethyst Shard
        boolean creative = player.getAbilities().instabuild;
        boolean hasAmmo = creative;
        if (!creative) {
            // Find and consume 1 Amethyst Shard
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(Items.AMETHYST_SHARD)) {
                    invStack.shrink(1);
                    hasAmmo = true;
                    break;
                }
            }
        }

        if (!hasAmmo) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 2.0F);
            return;
        }

        // Fire laser: 10 damage, max distance 30, colorType 1 (purple)
        if (player instanceof ServerPlayer serverPlayer) {
            fireLaser(serverPlayer, stack, false, 10.0F, 30.0F, 1);
            setLastShootTime(stack, level.getGameTime());
        }

        // Cooldown and heat increase
        setAltAttackCooldown(stack, 15); // 0.75 seconds cooldown
        float newHeat = getHeat(stack) + 25.0F;
        setHeat(stack, newHeat);

        if (newHeat >= 100.0F) {
            triggerOverheat(player, stack);
        }
    }
}
