package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Bio-Blade: a living alien war-blade. The strongest melee weapon in the mod.
//  * Every strike infects the victim and siphons their life back to the wielder
//    (double siphon vs. the swarm, plus armour-piercing bonus damage vs. aliens).
//  * Always glows with an enchant sheen (living energy), even un-enchanted.
//  * Right-click unleashes "Bio-Nova" - an AoE pulse that infects, damages and
//    knocks back everything hostile around you and heals you per enemy struck.
public class BioBladeItem extends SwordItem {
    private static final int NOVA_COOLDOWN = 80; // 4s

    public BioBladeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && !target.level().isClientSide) {
            boolean targetIsAlien = AlienUtils.isAlliedTo(attacker, target);

            // Heavy infection on every hit (stronger / longer vs. the old version).
            target.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION), 200, 1, false, true));

            if (targetIsAlien) {
                // Armour-piercing bio-burn that bypasses alien chitin, and a big life siphon.
                target.hurt(target.damageSources().magic(), 6.0F);
                attacker.heal(8.0F);
            } else {
                // Vampiric drain from any other foe.
                attacker.heal(4.0F);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
            }

            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        8, 0.2, 0.3, 0.2, 0.02);
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.4F);

        if (!level.isClientSide) {
            ServerLevel sl = (ServerLevel) level;
            double radius = 4.5D;
            int hits = 0;
            AABB area = player.getBoundingBox().inflate(radius);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive())) {
                if (player.distanceToSqr(victim) > radius * radius) continue;
                if (victim instanceof Player) continue; // friendly fire off for players

                boolean isAlien = AlienUtils.isAlliedTo(player, victim);
                victim.hurt(level.damageSources().playerAttack(player), isAlien ? 14.0F : 8.0F);
                victim.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION), 200, 1, false, true));

                Vec3 push = victim.position().subtract(player.position());
                push = push.lengthSqr() > 0.01D ? push.normalize().scale(0.9D) : new Vec3(0, 0.3D, 0);
                victim.setDeltaMovement(push.x, 0.45D, push.z);
                victim.hurtMarked = true;
                hits++;
            }

            // Heal for each enemy caught in the nova (capped).
            if (hits > 0) {
                player.heal(Math.min(8.0F, hits * 2.0F));
            }
            sl.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, radius * 0.5, 0.5, radius * 0.5, 0.05);
            if (hits == 0) {
                player.displayClientMessage(Component.literal("§a[Био-Нова] §7Поблизости нет врагов."), true);
            }
        }

        player.getCooldowns().addCooldown(this, NOVA_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // Always shimmers with living alien energy.
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
