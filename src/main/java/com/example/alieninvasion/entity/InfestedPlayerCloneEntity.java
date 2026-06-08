package com.example.alieninvasion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

/**
 * Зараженный Клон Игрока (Infested Player Clone):
 * Опасный зомби-пришелец, который спавнится на месте смерти игрока от ассимиляции.
 * Он носит броню и оружие игрока на момент смерти и использует их против него.
 */
public class InfestedPlayerCloneEntity extends Zombie implements IAlienUnit {

    public InfestedPlayerCloneEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.INFECTED; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected boolean isSunBurnTick() {
        return false; // Пришельцы не горят на солнце
    }

    public void copyFromPlayer(Player player) {
        // Устанавливаем имя клона как имя игрока
        this.setCustomName(net.minecraft.network.chat.Component.literal("Зараженный клон " + player.getName().getString()));
        this.setCustomNameVisible(true);

        // Копируем всю экипировку
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.setItemSlot(slot, stack.copy());
                // Гарантируем 100% шанс выпадения предметов при смерти
                this.setDropChance(slot, 2.0F);
            }
        }
    }
}
