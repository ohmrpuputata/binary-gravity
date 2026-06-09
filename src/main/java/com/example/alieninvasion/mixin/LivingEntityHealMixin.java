package com.example.alieninvasion.mixin;

import com.example.alieninvasion.logic.InfectionManager;
import com.example.alieninvasion.logic.RadiationManager;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHealMixin {

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void alien_blockHealIfIrradiated(float healAmount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION))) {
            ci.cancel();
        }
    }

    /** Milk removes all effects — immediately restore infection- and radiation-driven ones. */
    @Inject(method = "removeAllEffects", at = @At("RETURN"))
    private void alien_reapplyBarEffects(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player) || self.level().isClientSide || self.isDeadOrDying()) {
            return;
        }

        // Infection effects
        float meter = InfectionManager.getMeter(player);
        if (meter >= 25.0F) {
            var poisH = MobEffects.POISON;
            var nausH = MobEffects.CONFUSION;
            var infH  = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION);
            if (meter >= 75.0F) {
                self.addEffect(new MobEffectInstance(poisH, 100, 1, false, true));
                self.addEffect(new MobEffectInstance(infH,  100, 0, false, true));
            } else if (meter >= 50.0F) {
                self.addEffect(new MobEffectInstance(poisH, 100, 0, false, true));
                self.addEffect(new MobEffectInstance(nausH, 100, 0, false, true));
            } else {
                self.addEffect(new MobEffectInstance(poisH, 100, 0, false, true));
            }
        }

        // Radiation effects
        RadiationManager.reapplyDoseEffects(player);
    }
}
