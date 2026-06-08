package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.GravityGrenadeEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Gravity Grenade: a throwable that detonates into an anti-gravity burst,
// launching everything nearby into the air. Great for breaking up a swarm.
public class GravityGrenadeItem extends Item {
    public GravityGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.6F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            GravityGrenadeEntity grenade = new GravityGrenadeEntity(level, player);
            grenade.setItem(stack);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.4F, 1.0F);
            level.addFreshEntity(grenade);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
