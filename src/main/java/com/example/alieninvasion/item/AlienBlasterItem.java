package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.PlasmaBoltEntity;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Alien Blaster: a plasma sidearm, unaffected by EMP. Durability bar = a 30-round
 * magazine; the remaining rounds are shown right in the item name.
 *   - Right-click: single plasma shot. When empty, right-click reloads (1 Plasma Cell).
 *   - Sneak + right-click: CHARGED BLAST - a 5-bolt scattershot for 3 rounds.
 */
public class AlienBlasterItem extends Item {
    public AlienBlasterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        boolean creative = player.getAbilities().instabuild;
        int rounds = creative ? 999 : stack.getMaxDamage() - stack.getDamageValue();

        // RELOAD when empty.
        if (!creative && rounds <= 0) {
            if (consumeCell(player)) {
                stack.setDamageValue(0);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.displayClientMessage(Component.literal("§a[Бластер] Перезаряжен (30/30)."), true);
                player.getCooldowns().addCooldown(this, 25);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 0.6F);
                player.displayClientMessage(Component.literal("§c[Бластер] Нет плазменных обойм!"), true);
                player.getCooldowns().addCooldown(this, 10);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // CHARGED BLAST (sneak): 5-bolt scattershot, costs 3 rounds.
        if (player.isShiftKeyDown()) {
            if (!creative && rounds < 3) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 0.8F);
                player.displayClientMessage(Component.literal("§c[Бластер] Нужно ≥3 заряда для мощного выстрела."), true);
                player.getCooldowns().addCooldown(this, 8);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 1.4F);
            if (!level.isClientSide) {
                for (int i = 0; i < 5; i++) {
                    PlasmaBoltEntity bolt = new PlasmaBoltEntity(level, player);
                    bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 7.0F);
                    level.addFreshEntity(bolt);
                }
            }
            if (!creative) stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 3));
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(this, 30);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // NORMAL single shot.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5F,
                1.2F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            PlasmaBoltEntity bolt = new PlasmaBoltEntity(level, player);
            bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.5F);
            level.addFreshEntity(bolt);
        }
        if (!creative) {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                player.displayClientMessage(Component.literal("§e[Бластер] Обойма пуста — ПКМ для перезарядки."), true);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean consumeCell(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ItemRegistry.PLASMA_CELL)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    // Show remaining ammo right in the item name, e.g. "Alien Blaster [24/30]".
    @Override
    public Component getName(ItemStack stack) {
        int rounds = stack.getMaxDamage() - stack.getDamageValue();
        return Component.translatable(this.getDescriptionId(stack))
                .append(Component.literal(" §7[" + rounds + "/" + stack.getMaxDamage() + "]"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
