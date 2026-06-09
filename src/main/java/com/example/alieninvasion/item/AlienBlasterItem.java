package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.RadiationBoltEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Alien Blaster: fires concentrated radiation bolts.
 *   - Right-click:        single heavy beam — high damage (20), 3-second cooldown.
 *   - Shift+Right-click:  burst of 10 small bolts, wide spread, 1.25-second cooldown.
 */
public class AlienBlasterItem extends Item {
    private static final int NORMAL_COOLDOWN = 60; // 3 seconds
    private static final int BURST_COOLDOWN  = 25; // 1.25 seconds
    private static final int BURST_COUNT     = 10;
    private static final float BURST_SPREAD  = 9.0F;

    public AlienBlasterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            // Burst: 10 small bolts in a spread cone
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
            if (!level.isClientSide) {
                for (int i = 0; i < BURST_COUNT; i++) {
                    RadiationBoltEntity bolt = new RadiationBoltEntity(level, player, false);
                    bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.8F, BURST_SPREAD);
                    level.addFreshEntity(bolt);
                }
            }
            player.getCooldowns().addCooldown(this, BURST_COOLDOWN);
        } else {
            // Normal: one heavy beam, high damage
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.6F);
            if (!level.isClientSide) {
                RadiationBoltEntity bolt = new RadiationBoltEntity(level, player, true);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.3F);
                level.addFreshEntity(bolt);
            }
            player.getCooldowns().addCooldown(this, NORMAL_COOLDOWN);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
