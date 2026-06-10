package com.example.alieninvasion.mixin.client;

import com.example.alieninvasion.logic.RadiationManager;
import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Unique
    private boolean alien_canDoubleJump = false;
    @Unique
    private boolean alien_wasJumping = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void handleDoubleJump(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (player.onGround() || player.isInWater() || player.onClimbable() || player.getAbilities().flying) {
            alien_canDoubleJump = true;
            alien_wasJumping = player.input.jumping;
            return;
        }

        boolean isJumping = player.input.jumping;
        if (isJumping && !alien_wasJumping && alien_canDoubleJump) {
            ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
            if (feet.is(ItemRegistry.GRAVITY_BOOTS) && !player.getTags().contains("EmpActive")) {
                alien_canDoubleJump = false;
                player.setDeltaMovement(player.getDeltaMovement().x, 0.62D, player.getDeltaMovement().z);
                player.hasImpulse = true;
                player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.7F, 1.4F);
            }
        }
        alien_wasJumping = isJumping;
    }

    /**
     * Перехватываем hurtTo() чтобы при дрейфе максимального здоровья от облучения
     * не показывалась анимация урона (красная вспышка, покачивание камеры).
     * Флаг выставляется сервером в RadiationManager.applyHealthDrain() перед
     * изменением атрибута MAX_HEALTH.
     */
    @Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
    private void suppressRadiationHealthDrainAnim(float health, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (RadiationManager.SUPPRESS_HURT_ANIM.remove(self.getUUID())) {
            // Просто обновить здоровье без установки hurtTime / invulnerableTime
            if (!self.isDeadOrDying()) {
                self.setHealth(health);
            }
            ci.cancel();
        }
    }
}
