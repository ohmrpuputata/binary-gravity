package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.RadiationBoltEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GreenRayBlasterItem extends AlienBlasterItem {
    public GreenRayBlasterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getCooldown(stack) > 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            setCooldown(stack, 40); // reload cooldown slightly shorter as it is upgraded
            setMaxCooldown(stack, 40);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.8F);
            if (!level.isClientSide) {
                RadiationBoltEntity bolt = new RadiationBoltEntity(level, player, true);
                bolt.setGreenRay(true);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.2F, 0.2F);
                level.addFreshEntity(bolt);
            }
            player.swing(hand, true);
            setCooldown(stack, 50); // upgraded cooldown 2.5 seconds (50 ticks) instead of 3.0s (60 ticks)
            setMaxCooldown(stack, 50);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (ticksUsed < 20 && ticksUsed % 2 == 0) {
            level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.7F + level.random.nextFloat() * 0.3F);
            if (!level.isClientSide) {
                RadiationBoltEntity bolt = new RadiationBoltEntity(level, livingEntity, false);
                bolt.setGreenRay(true);
                bolt.shootFromRotation(livingEntity, livingEntity.getXRot(), livingEntity.getYRot(), 0.0F, 3.0F, 1.5F);
                level.addFreshEntity(bolt);
            }
        }
    }
}
