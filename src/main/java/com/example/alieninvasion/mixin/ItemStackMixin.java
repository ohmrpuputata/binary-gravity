package com.example.alieninvasion.mixin;

import com.example.alieninvasion.item.ModToolTiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void speedUpPalladiumToolsOnInfested(BlockState state, CallbackInfoReturnable<Float> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof TieredItem tiered) {
            if (tiered.getTier() == ModToolTiers.PALLADIUM) {
                if (isInfested(state)) {
                    cir.setReturnValue(cir.getReturnValue() * 2.0F);
                }
            }
        }
    }

    private boolean isInfested(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!key.getNamespace().equals("alien-invasion")) {
            return false;
        }
        String path = key.getPath();
        return path.contains("infested") || path.contains("infected") 
            || path.contains("bloody") || path.equals("blood_pool") 
            || path.startsWith("alien_");
    }
}
