package com.example.alieninvasion.mixin;

import com.example.alieninvasion.logic.BleedManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Любое существо кровоточит из РАНЫ (см. {@link BleedManager}): пока рана открыта —
 * капли и лужи нужного цвета, и рана сворачивается сама. Низкое HP само по себе не
 * кровит. Дёшево: для существ без раны {@code tick} сразу возвращается.
 */
@Mixin(LivingEntity.class)
public class LivingEntityBleedMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void alien$bleed(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() instanceof ServerLevel level) {
            BleedManager.tick(level, self);
            com.example.alieninvasion.logic.WormInfestation.tickHost(level, self);
        }
    }
}
