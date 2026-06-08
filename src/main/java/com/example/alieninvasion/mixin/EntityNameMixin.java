package com.example.alieninvasion.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Скрывает ник (подпись) над головой для ВСЕХ сущностей мода alien-invasion —
 * и чистых пришельцев, и заражённых ванильных мобов — независимо от
 * родительского класса. Проверка идёт по namespace регистра типов сущностей,
 * поэтому покрывает любой новый тип без дополнительного кода.
 */
@Mixin(Entity.class)
public class EntityNameMixin {

    @Inject(method = "isCustomNameVisible", at = @At("HEAD"), cancellable = true)
    private void suppressAlienNametag(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
        if (key != null && "alien-invasion".equals(key.getNamespace())) {
            cir.setReturnValue(false);
        }
    }
}
