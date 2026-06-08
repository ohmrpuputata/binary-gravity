package com.example.alieninvasion.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void modifySkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Vec3> info) {
        ClientLevel level = (ClientLevel) (Object) this;
        long time = level.getDayTime();
        int day = (int) (time / 24000L);
        if (day > 0) {
            float progress = Math.min(day / 8.0F, 1.0F); // Max intensity at day 8
            Vec3 originalSky = info.getReturnValue();
            if (originalSky != null) {
                // Shift sky color to crimson red (R=0.7, G=0.04, B=0.04)
                double red = originalSky.x * (1.0F - progress) + 0.7D * progress;
                double green = originalSky.y * (1.0F - progress) + 0.04D * progress;
                double blue = originalSky.z * (1.0F - progress) + 0.04D * progress;
                info.setReturnValue(new Vec3(red, green, blue));
            }
        }
    }
}
