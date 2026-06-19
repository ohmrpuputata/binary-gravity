package com.example.alieninvasion.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Глушит надоедливый «стук/треск двери» от зомби-подобных пришельцев. Ванильный
 * BreakDoorGoal шлёт levelEvent 1019/1020 (стук по деревянной/железной двери) и 1021
 * (дверь выломана) — это громкий повторяющийся БАМ-БАМ. Сами двери всё равно ломаются
 * (через destroyBlock), мы убираем только звук-событие. Заодно гасит 1019-бам у
 * PillarUpGoal. Остальные levelEvent проходят как обычно.
 */
@Mixin(ServerLevel.class)
public abstract class LevelEventMixin {
    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void alien$muteDoorBang(Player player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type == 1019 || type == 1020 || type == 1021) {
            ci.cancel();
        }
    }
}
