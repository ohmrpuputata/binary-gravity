package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.RadiationBoltEntity;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;

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
        return tag.contains(MAX_COOLDOWN_KEY) ? tag.getInt(MAX_COOLDOWN_KEY) : 100;
    }

    protected static void setMaxCooldown(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MAX_COOLDOWN_KEY, Math.max(1, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // New heat systems
    public static float getHeat(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("BlasterHeat") ? tag.getFloat("BlasterHeat") : 0.0F;
    }

    public static void setHeat(ItemStack stack, float value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putFloat("BlasterHeat", Math.max(0.0F, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isOverheated(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("Overheated") && tag.getBoolean("Overheated");
    }

    public static void setOverheated(ItemStack stack, boolean value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("Overheated", value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getAltAttackCooldown(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("AltAttackCooldown") ? tag.getInt("AltAttackCooldown") : 0;
    }

    public static void setAltAttackCooldown(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("AltAttackCooldown", Math.max(0, value));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long getLastShootTime(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("LastShootTime") ? tag.getLong("LastShootTime") : 0L;
    }

    public static void setLastShootTime(ItemStack stack, long time) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong("LastShootTime", time);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static float getModelHeat(ItemStack stack) {
        return Math.min(1.0F, getHeat(stack) / 100.0F);
    }

    public int getTier() {
        return 1;
    }

    public int getFireRate() {
        return 6; // Every 6 ticks
    }

    public float getSpread() {
        return 2.0F;
    }

    public float getHeatIncreasePerShot() {
        return 3.0F;
    }

    public float getCoolingRate() {
        return 2.0F; // Normal cooling rate per tick
    }

    public boolean hasAlternativeFire() {
        return false;
    }

    public void handleAlternativeFire(Player player, ItemStack stack) {
        // No-op by default
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            float heat = getHeat(stack);
            boolean overheated = isOverheated(stack);

            int altCd = getAltAttackCooldown(stack);
            if (altCd > 0) {
                setAltAttackCooldown(stack, altCd - 1);
            }

            if (heat > 0.0F) {
                long lastShootTime = getLastShootTime(stack);
                long currentTime = level.getGameTime();
                if (currentTime - lastShootTime >= 40L || currentTime < lastShootTime) {
                    float coolingRate = overheated ? 0.25F : getCoolingRate();
                    float newHeat = Math.max(0.0F, heat - coolingRate);
                    setHeat(stack, newHeat);
                    
                    // Map heat to cooldown for rendering the reload bar correctly
                    setCooldown(stack, Math.round(newHeat));
                    setMaxCooldown(stack, 100);

                    if (overheated && newHeat == 0.0F) {
                        setOverheated(stack, false);
                        if (entity instanceof Player player) {
                            player.displayClientMessage(Component.literal("§a[!] Бластер остыл и готов к работе."), true);
                        }
                    }
                } else {
                    setCooldown(stack, Math.round(heat));
                    setMaxCooldown(stack, 100);
                }
            } else {
                setCooldown(stack, 0);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isOverheated(stack)) {
            player.displayClientMessage(Component.literal("§c[!] Бластер перегрет!"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Ammo check for non-creative
        if (!player.getAbilities().instabuild && !hasAnyAmmo(player)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 2.0F);
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    protected boolean hasAnyAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ItemRegistry.PLATINUM_CHUNK) || stack.is(ItemRegistry.PALLADIUM_CHUNK)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasAmmo(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    protected ItemStack findAndConsumeAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ItemRegistry.PLATINUM_CHUNK) || stack.is(ItemRegistry.PALLADIUM_CHUNK)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                stack.shrink(1);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (isOverheated(stack)) {
            livingEntity.releaseUsingItem();
            return;
        }

        if (livingEntity instanceof Player player) {
            int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;

            if (ticksUsed % getFireRate() == 0) {
                // Ammo check/consumption
                ItemStack ammo = ItemStack.EMPTY;
                if (player.getAbilities().instabuild) {
                    if (hasAmmo(player, ItemRegistry.PLATINUM_CHUNK)) {
                        ammo = new ItemStack(ItemRegistry.PLATINUM_CHUNK);
                    } else if (hasAmmo(player, ItemRegistry.PALLADIUM_CHUNK)) {
                        ammo = new ItemStack(ItemRegistry.PALLADIUM_CHUNK);
                    } else {
                        ammo = new ItemStack(ItemRegistry.PLATINUM_CHUNK);
                    }
                } else {
                    ammo = findAndConsumeAmmo(player);
                }

                if (ammo.isEmpty()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 2.0F);
                    player.releaseUsingItem();
                    return;
                }

                int ammoType = ammo.is(ItemRegistry.PLATINUM_CHUNK) ? 0 : 1;

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.75F, 1.5F + level.random.nextFloat() * 0.3F);

                if (!level.isClientSide) {
                    RadiationBoltEntity bolt = new RadiationBoltEntity(level, player, false);
                    bolt.setAmmoType(ammoType);
                    bolt.setBlasterTier(getTier());
                    bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.8F, getSpread());
                    level.addFreshEntity(bolt);

                    setLastShootTime(stack, level.getGameTime());

                    float newHeat = getHeat(stack) + getHeatIncreasePerShot();
                    setHeat(stack, newHeat);

                    if (newHeat >= 100.0F) {
                        triggerOverheat(player, stack);
                        player.releaseUsingItem();
                        return;
                    }
                }
                
                // Overheat particles warning
                spawnOverheatEffects(level, player, Math.round(getHeat(stack)), getTier() == 3);
            }
        }
    }

    protected void triggerOverheat(Player player, ItemStack stack) {
        setOverheated(stack, true);
        setHeat(stack, 100.0F);

        Level level = player.level();
        if (!level.isClientSide) {
            level.explode(player, player.getX(), player.getY(), player.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
            player.hurt(player.damageSources().explosion(null), 4.0F); // 2 hearts damage
            player.displayClientMessage(Component.literal("§c[!] Бластер перегрелся и взорвался!"), true);
        }
    }

    protected void spawnOverheatEffects(
            Level level,
            LivingEntity livingEntity,
            int heat,
            boolean greenRay) {
        if (!(level instanceof ServerLevel serverLevel) || heat < 60 || level.random.nextInt(3) != 0) {
            return;
        }
        Vec3 muzzle = livingEntity.getEyePosition()
                .add(livingEntity.getViewVector(1.0F).scale(0.75D))
                .add(0.0D, -0.18D, 0.0D);
        serverLevel.sendParticles(
                heat >= 85 ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK,
                muzzle.x, muzzle.y, muzzle.z,
                greenRay ? 3 : 2,
                0.045D, 0.035D, 0.045D,
                0.01D);
        if (greenRay) {
            serverLevel.sendParticles(
                    ParticleTypes.SNEEZE,
                    muzzle.x, muzzle.y, muzzle.z,
                    1,
                    0.025D, 0.025D, 0.025D,
                    0.005D);
        }
        if (heat >= 95 && level.random.nextInt(5) == 0) {
            level.playSound(null, livingEntity.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.35F, greenRay ? 1.65F : 1.35F);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getHeat(stack) > 0.0F;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getHeat(stack) * 13.0F / 100.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isOverheated(stack) ? 0xFF0000 : 0xFF5555;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // Helper method to execute laser firing
    public static void fireLaser(ServerPlayer player, ItemStack stack, boolean passesThroughBlocks, float damage, float maxDistance, int colorType) {
        Level level = player.level();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos;

        if (passesThroughBlocks) {
            endPos = eyePos.add(lookVec.scale(maxDistance));
        } else {
            net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
                    eyePos,
                    eyePos.add(lookVec.scale(maxDistance)),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player
            );
            net.minecraft.world.phys.BlockHitResult blockHit = level.clip(context);
            endPos = blockHit.getLocation();
        }

        double actualDistance = eyePos.distanceTo(endPos);
        net.minecraft.world.phys.AABB boundingBox = new net.minecraft.world.phys.AABB(eyePos, endPos).inflate(1.0D);
        java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> {
            if (entity == player || !entity.isAlive()) return false;
            net.minecraft.world.phys.AABB aabb = entity.getBoundingBox().inflate(0.3D);
            return aabb.clip(eyePos, endPos).isPresent();
        });

        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            
            if (colorType == 2) { // Blaster III (Green Ray)
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
                target.addEffect(new MobEffectInstance(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                                com.example.alieninvasion.registry.ModEffects.IRRADIATION),
                        200, 0));
                if (target instanceof Player playerTarget) {
                    com.example.alieninvasion.logic.RadiationManager.addDose(playerTarget, 30.0F);
                }
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, colorType == 2 ? 0.6F : 1.2F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F, colorType == 2 ? 0.5F : 1.0F);

            double step = 0.25D;
            int numSteps = (int) (actualDistance / step);
            
            DustParticleOptions mainDust = colorType == 2
                    ? new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.9F, 0.0F), 2.5F) // Thick yellow
                    : new DustParticleOptions(new org.joml.Vector3f(0.8F, 0.0F, 0.8F), 1.2F); // Purple
            
            for (int i = 0; i <= numSteps; i++) {
                Vec3 point = eyePos.add(lookVec.scale(i * step));
                serverLevel.sendParticles(mainDust, point.x, point.y, point.z, colorType == 2 ? 3 : 1, 0.02, 0.02, 0.02, 0);
                
                if (serverLevel.random.nextFloat() < 0.15F) {
                    if (colorType == 1) {
                        if (serverLevel.random.nextBoolean()) {
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0);
                        } else {
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0);
                        }
                    } else {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
        }
    }
}
