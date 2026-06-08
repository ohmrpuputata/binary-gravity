package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

// Bio-Hoe: a cultivator that channels a healing pulse (right-click in air),
// mending the wielder and nearby allies. Has a cooldown and costs durability.
public class BioHoeItem extends HoeItem {
    public BioHoeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            player.heal(4.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            for (Player ally : level.getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(5.0D), p -> p != player)) {
                ally.heal(2.0F);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            }
            ((ServerLevel) level).sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.6, 0.6, 0.6, 0.0);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 1.5F);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(5, player, LivingEntity.getSlotForHand(hand));
            }
            player.getCooldowns().addCooldown(this, 200); // 10s
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
