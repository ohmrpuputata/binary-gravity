package com.example.alieninvasion.mixin.client;

import com.example.alieninvasion.item.AlienBlasterItem;
import com.example.alieninvasion.network.LeftClickBlasterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void alienInvasion$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.player != null) {
            ItemStack stack = client.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!stack.isEmpty() && (stack.getItem() instanceof AlienBlasterItem blaster)) {
                if (blaster.hasAlternativeFire()) {
                    ClientPlayNetworking.send(new LeftClickBlasterPayload());
                    client.player.swing(InteractionHand.MAIN_HAND);
                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }
}
