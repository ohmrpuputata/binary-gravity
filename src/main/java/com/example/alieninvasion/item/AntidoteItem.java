package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class AntidoteItem extends Item {
    public AntidoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            user.addTag("CuredByAntidote");
            if (user instanceof Player p) {
                com.example.alieninvasion.logic.InfectionManager.clear(p);
            }
            user.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
        }
        if (user instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
