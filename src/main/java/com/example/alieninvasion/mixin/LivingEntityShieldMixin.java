package com.example.alieninvasion.mixin;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityShieldMixin {

    @Inject(method = "blockUsingShield", at = @At("HEAD"))
    private void alien_healOnShieldBlock(LivingEntity attacker, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack useItem = self.getUseItem();
        if (useItem.is(ItemRegistry.EMERADIUM_SHIELD)) {
            self.heal(2.0F);
        }
    }
}
