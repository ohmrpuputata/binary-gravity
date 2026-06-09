package com.example.alieninvasion.mixin;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityHealMixin {
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void alien_blockHealIfIrradiated(float healAmount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION))) {
            ci.cancel();
        }
    }
}
