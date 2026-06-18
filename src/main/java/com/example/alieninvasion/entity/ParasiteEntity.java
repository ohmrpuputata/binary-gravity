package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;

// Паразит-Мозгоед: стремительно мчится к игроку и ПРЫГАЕТ ему на голову.
// Заняв слот шлема, мучает носителя (см. ModEvents). Очень быстрый и прыгучий.
public class ParasiteEntity extends Silverfish implements IAlienUnit {
    // ALIEN VOICE: quiet, pitched vanilla sounds remixed into something wrong -
    // rarer and softer than the originals so the swarm unnerves instead of annoys.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return 1.45F + this.random.nextFloat() * 0.1F;
    }

    public ParasiteEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.PARASITE; }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)        // glass cannon: 1 HP, dies instantly
                .add(Attributes.MOVEMENT_SPEED, 0.6D)    // extremely fast scuttler
                .add(Attributes.ATTACK_DAMAGE, 0.0D)     // deals NO damage - it only seizes control
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Pounce toward its target from range - "leaps onto the player/mob".
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienLeapGoal(this, 0.55F));
        // Also hunt normal (un-infected, non-alien) mobs to burrow into them.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> e != this && !(e instanceof Player) && !AlienUtils.isAlliedTo(this, e)
                        && !e.getTags().contains("ParasiteHost")));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // The bite deals NO damage - it ATTACHES / INFESTS. So we must NOT gate this
        // on super.doHurtTarget()'s return (it's false at 0 attack damage, which is
        // why the worm "stopped climbing onto the player"). Do it directly.
        if (!this.level().isClientSide) {
            if (target instanceof Player player) {
                ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                // Only a Cosmic helmet or the FULL hazmat set keeps it out.
                boolean protectedHead = helmet.is(ItemRegistry.COSMIC_HELMET)
                        || helmet.is(ItemRegistry.BIO_FILTER_MASK)
                        || hasFullHazmat(player);
                if (!protectedHead && !helmet.is(ItemRegistry.PARASITE_ITEM)) {
                    if (!helmet.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(helmet.copy());
                    }
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.PARASITE_ITEM));
                    com.example.alieninvasion.events.ModEvents.PARASITE_ATTACH.put(player.getUUID(), this.level().getGameTime());
                    this.level().playSound(null, player.blockPosition(), SoundEvents.SPIDER_DEATH, SoundSource.HOSTILE, 1.5F, 0.7F);
                    this.level().playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
                    player.displayClientMessage(Component.literal("§c[!] Паразит захватил ваш разум! 5 секунд его не снять!"), false);
                    this.discard();
                    return true;
                }
                return false; // protected head - bounces off, keeps trying
            }
            if (target instanceof net.minecraft.world.entity.PathfinderMob host
                    && !AlienUtils.isAlliedTo(this, host) && !host.getTags().contains("ParasiteHost")) {
                // Burrow into a normal mob -> deranged, hyper-aggressive host.
                infectMob(host);
                this.discard();
                return true;
            }
        }
        return super.doHurtTarget(target);
    }

    private void infectMob(net.minecraft.world.entity.PathfinderMob host) {
        host.addTag("ParasiteHost");
        host.setCustomName(net.minecraft.network.chat.Component.literal(
                "§2Заражённый " + host.getType().getDescription().getString()));
        host.setCustomNameVisible(true);
        // Permanent frenzy buffs.
        host.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 999999, 1, false, false));
        host.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 999999, 1, false, false));
        host.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.GLOWING, 999999, 0, false, false));
        // Attack EVERYTHING alive (players, animals, mobs, even aliens) and chase it.
        // CRITICAL: only mobs that actually have the attack-damage attribute can melee.
        // Giving a MeleeAttackGoal to a passive mob (e.g. an armadillo / cow / chicken,
        // which have no generic.attack_damage) crashes the server the instant the goal
        // tries to perform an attack (IllegalArgumentException: Can't find attribute).
        if (host.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            net.minecraft.world.entity.ai.goal.GoalSelector goals = ((com.example.alieninvasion.mixin.MobAccessor) host).getGoalSelector();
            net.minecraft.world.entity.ai.goal.GoalSelector targets = ((com.example.alieninvasion.mixin.MobAccessor) host).getTargetSelector();
            goals.addGoal(0, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(host, 1.6D, false));
            targets.addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    host, net.minecraft.world.entity.LivingEntity.class, 5, true, false,
                    e -> e != host && e.isAlive() && !(e instanceof ParasiteEntity)));
        }
        this.level().playSound(null, host.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.2F, 0.5F);
    }

    private boolean hasFullHazmat(Player p) {
        return com.example.alieninvasion.logic.ArmorProtection.hasSealedSuit(p);
    }
    
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
