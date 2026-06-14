package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.RadiationBoltEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Alien Blaster: fires concentrated radiation bolts.
 *   - Right-click:        single heavy beam — high damage (20), 3-second cooldown.
 *   - Shift+Right-click:  rapid machine-gun burst of 10 small bolts, 1.5-second cooldown.
 */
public class AlienBlasterItem extends Item {
    private static final String COOLDOWN_KEY = "BlasterCooldown";
    private static final String MAX_COOLDOWN_KEY = "BlasterMaxCooldown";

    public AlienBlasterItem(Properties properties) {
        super(properties);
    }

    protected static int getCooldown(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(COOLDOWN_KEY) ? tag.getInt(COOLDOWN_KEY) : 0;
    }

    protected static void setCooldown(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(COOLDOWN_KEY, Math.max(0, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    protected static int getMaxCooldown(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(MAX_COOLDOWN_KEY) ? tag.getInt(MAX_COOLDOWN_KEY) : 1;
    }

    protected static void setMaxCooldown(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MAX_COOLDOWN_KEY, Math.max(1, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            int cd = getCooldown(stack);
            if (cd > 0) {
                setCooldown(stack, cd - 1);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Cooldown check on the stack
        if (getCooldown(stack) > 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            // Start rapid machine-gun burst: 20 ticks (1s) of use, plus 30 ticks (1.5s) of reload cooldown
            player.startUsingItem(hand);
            setCooldown(stack, 50); // 20 ticks burst + 30 ticks reload
            setMaxCooldown(stack, 50);
        } else {
            // Normal: one heavy beam, high damage
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, 0.6F);
            if (!level.isClientSide) {
                RadiationBoltEntity bolt = new RadiationBoltEntity(level, player, true);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.3F);
                level.addFreshEntity(bolt);
            }
            player.swing(hand, true);
            setCooldown(stack, 60); // 3-second cooldown
            setMaxCooldown(stack, 60);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        
        // Shoot 10 times: every 2 ticks (at ticks 0, 2, 4, 6, 8, 10, 12, 14, 16, 18)
        if (ticksUsed < 20 && ticksUsed % 2 == 0) {
            level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.5F + level.random.nextFloat() * 0.3F);
            if (!level.isClientSide) {
                RadiationBoltEntity bolt = new RadiationBoltEntity(level, livingEntity, false);
                // Tight spread of 2.0F for a rapid machine gun feel
                bolt.shootFromRotation(livingEntity, livingEntity.getXRot(), livingEntity.getYRot(), 0.0F, 2.8F, 2.0F);
                level.addFreshEntity(bolt);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 20; // 1 second burst
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Aiming stance during burst
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCooldown(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int cd = getCooldown(stack);
        int max = getMaxCooldown(stack);
        return Math.round((float) cd * 13.0F / (float) max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF5555; // Red reload bar
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
