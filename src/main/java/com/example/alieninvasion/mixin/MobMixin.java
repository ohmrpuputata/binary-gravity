package com.example.alieninvasion.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void makeAliensPersistent(double distance, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        var loc = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
        if (loc != null && "alien-invasion".equals(loc.getNamespace())) {
            cir.setReturnValue(false);
        }
    }
}
