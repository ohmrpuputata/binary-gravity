package com.example.alieninvasion.mixin.client;

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

        // On the ground / in water / climbing / flying: recharge the air-jump and
        // sync the held-jump state so it can't auto-trigger off a single hold.
        if (player.onGround() || player.isInWater() || player.onClimbable() || player.getAbilities().flying) {
            alien_canDoubleJump = true;
            alien_wasJumping = player.input.jumping;
            return;
        }

        boolean isJumping = player.input.jumping;
        // Rising edge in mid-air = the SECOND tap of space -> air jump.
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
}
