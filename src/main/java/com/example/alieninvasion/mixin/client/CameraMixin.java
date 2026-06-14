package com.example.alieninvasion.mixin.client;

import com.example.alieninvasion.client.WorldAmbienceEffects;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void alienInvasion$shakeNearLargeAliens(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo ci) {
        setRotation(
                getYRot() + WorldAmbienceEffects.cameraYawOffset(partialTick),
                getXRot() + WorldAmbienceEffects.cameraPitchOffset(partialTick));
    }
}
