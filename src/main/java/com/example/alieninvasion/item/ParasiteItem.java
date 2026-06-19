package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Brain parasite. Worn on the head it hijacks YOU (handled in ModEvents). But it
 * now has a real use in hand: right-click a creature to drive the worm into it,
 * turning it into a mind-controlled thrall that hunts the swarm / hostiles for you
 * (re-targeting + ally protection live in ModEvents). Consumes the parasite.
 */
public class ParasiteItem extends Item {
    public ParasiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(target instanceof Mob mob) || target instanceof Player || !target.isAlive()
                || mob.getTags().contains("PlayerParasiteAlly")) {
            return InteractionResult.PASS;
        }

        mob.addTag("PlayerParasiteAlly");
        mob.setPersistenceRequired();
        mob.setTarget(null);
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0, false, false));
        // Подчинённый моб внешне НЕ отличается от обычного — без имени и бирки над
        // головой. Какого моба вы обратили, подскажет разовое сообщение ниже.

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WARPED_SPORE, mob.getX(), mob.getEyeY(), mob.getZ(),
                    24, 0.3D, 0.4D, 0.3D, 0.02D);
            sl.playSound(null, mob.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        player.displayClientMessage(Component.literal(
                "§a[!] Паразит подчинил «" + mob.getName().getString() + "» — теперь он сражается за вас."), true);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
