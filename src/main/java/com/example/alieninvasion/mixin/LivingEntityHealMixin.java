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

        // Infection effects (cumulative tiers)
        float meter = InfectionManager.getMeter(player);
        int infTier = meter >= 75.0F ? 3 : meter >= 50.0F ? 2 : meter >= 25.0F ? 1 : 0;
        if (infTier >= 1) {
            int amp = infTier >= 3 ? 1 : 0;
            self.addEffect(new MobEffectInstance(MobEffects.POISON, 100, amp, false, true));
        }
        // Тошнота убрана из тиров — теперь применяется только при активном росте заражения
        if (infTier >= 3) self.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION), 100, 0, false, true));

        // Radiation effects (cumulative tiers)
        RadiationManager.reapplyDoseEffects(player);
    }
}
