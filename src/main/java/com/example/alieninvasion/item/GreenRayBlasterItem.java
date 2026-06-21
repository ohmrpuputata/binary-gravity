package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffectInstance;

public class GreenRayBlasterItem extends BlasterIIItem {
    public GreenRayBlasterItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getTier() {
        return 3;
    }

    @Override
    public float getHeatIncreasePerShot() {
        return 0.3F; // Heats up very, very slowly (0.3F instead of 1.5F/3.0F)
    }

    @Override
    public void handleAlternativeFire(Player player, ItemStack stack) {
        Level level = player.level();
        if (level.isClientSide) return;

        if (isOverheated(stack)) {
            player.displayClientMessage(Component.literal("§c[!] Бластер перегрет!"), true);
            return;
        }

        if (getAltAttackCooldown(stack) > 0) {
            return;
        }

        // Ammo check: Pure Radiation Shard
        boolean creative = player.getAbilities().instabuild;
        boolean hasAmmo = creative;
        if (!creative) {
            // Find and consume 1 Radiation Crystal
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.is(ItemRegistry.RADIATION_CRYSTAL)) {
                    invStack.shrink(1);
                    hasAmmo = true;
                    break;
                }
            }
        }

        if (!hasAmmo) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 2.0F);
            return;
        }

        // Fire laser: 25 damage, max distance 40, passes through blocks, colorType 2 (yellow/green)
        if (player instanceof ServerPlayer serverPlayer) {
            fireLaser(serverPlayer, stack, true, 25.0F, 40.0F, 2);
            setLastShootTime(stack, level.getGameTime());
        }

        // Cooldown and heat increase
        setAltAttackCooldown(stack, 30); // 1.5 seconds cooldown
        float newHeat = getHeat(stack) + 40.0F;
        setHeat(stack, newHeat);

        if (newHeat >= 100.0F) {
            triggerOverheat(player, stack);
        }
    }

    @Override
    protected void triggerOverheat(Player player, ItemStack stack) {
        setOverheated(stack, true);
        setHeat(stack, 100.0F);

        Level level = player.level();
        if (!level.isClientSide) {
            BlockPos pos = player.blockPosition();

            // Instantly kill player
            player.hurt(level.damageSources().explosion(null), 10000.0F);

            // Massive block-destroying explosion (radius 8.0)
            level.explode(player, player.getX(), player.getY(), player.getZ(), 8.0F, Level.ExplosionInteraction.TNT);

            // Irradiate all entities in 12 block radius
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(pos).inflate(12.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                entity.addEffect(new MobEffectInstance(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION),
                        400, 1)); // 20 seconds of Irradiation II
                if (entity instanceof Player p) {
                    com.example.alieninvasion.logic.RadiationManager.addDose(p, 80.0F);
                }
            }

            // Leave increased radiation background by placing Pure Radiation blocks
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        if (x * x + y * y + z * z <= 5) {
                            BlockPos blockPos = pos.offset(x, y, z);
                            if (level.getBlockState(blockPos).isAir() || 
                                level.getBlockState(blockPos).is(Blocks.DIRT) || 
                                level.getBlockState(blockPos).is(Blocks.STONE) ||
                                level.getBlockState(blockPos).is(Blocks.DEEPSLATE)) {
                                level.setBlockAndUpdate(blockPos, ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
    }
}
