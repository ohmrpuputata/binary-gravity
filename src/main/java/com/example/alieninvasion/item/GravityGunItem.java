package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GravityGunItem extends Item {
    /** Maximum number of shots the gun can hold. */
    public static final int MAX_CHARGE = 8;
    /** Ticks between automatic +1 charge regeneration (20 ticks = 1 second). */
    private static final int RECHARGE_TICKS = 30;
    private static final String CHARGE_KEY = "Charge";

    public GravityGunItem(Properties properties) {
        super(properties);
    }

    private static int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // A freshly crafted gun has no tag yet -> treat it as fully charged.
        return tag.contains(CHARGE_KEY) ? tag.getInt(CHARGE_KEY) : MAX_CHARGE;
    }

    private static void setCharge(ItemStack stack, int value) {
        int clamped = Math.max(0, Math.min(MAX_CHARGE, value));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(CHARGE_KEY, clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Charges refill themselves over time, server-side only.
        if (!level.isClientSide) {
            int charge = getCharge(stack);
            if (charge < MAX_CHARGE && level.getGameTime() % RECHARGE_TICKS == 0) {
                setCharge(stack, charge + 1);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.getTags().contains("EmpActive")) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 0.5F);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] ЭМП-буря отключила гравитационную пушку!"), true);
            return InteractionResultHolder.fail(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        int charge = getCharge(stack);

        // Out of charge: click and wait for it to recharge.
        if (charge <= 0 && !creative) {
            if (!level.isClientSide) {
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.3F);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            Vec3 eyePosition = player.getEyePosition(1.0F);
            Vec3 lookVector = player.getViewVector(1.0F);
            double maxRange = 30.0D;
            Vec3 targetVec = eyePosition.add(lookVector.scale(maxRange));

            Entity hitEntity = null;
            // Scan for living entities along the line of sight
            for (Entity entity : level.getEntities(player, player.getBoundingBox().expandTowards(lookVector.scale(maxRange)).inflate(1.0D))) {
                if (entity instanceof LivingEntity && entity != player) {
                    var boundingBox = entity.getBoundingBox().inflate(0.4D);
                    var optional = boundingBox.clip(eyePosition, targetVec);
                    if (optional.isPresent()) {
                        double dist = eyePosition.distanceToSqr(optional.get());
                        if (dist < maxRange * maxRange) {
                            hitEntity = entity;
                            maxRange = Math.sqrt(dist);
                        }
                    }
                }
            }

            // Spawn trail particles
            ServerLevel serverLevel = (ServerLevel) level;
            double steps = maxRange * 2.0D;
            for (int i = 0; i < steps; i++) {
                Vec3 p = eyePosition.add(lookVector.scale(i * 0.5D));
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }

            if (hitEntity instanceof LivingEntity target) {
                // Apply ANTI_GRAVITY effect (from ModEffects)
                target.addEffect(new MobEffectInstance(
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.ANTI_GRAVITY),
                    100, 0, false, false
                ));
                level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.5F, 1.2F);
                serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 2.0F);

            // Fire consumes one self-refilling charge (creative mode shoots for free).
            if (!creative) {
                setCharge(stack, charge - 1);
                player.getCooldowns().addCooldown(this, 8);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---- Charge shown as a self-refilling energy bar instead of durability ----
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getCharge(stack) * 13.0F / MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00E0E0; // cyan energy
    }
}
