package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Доп. слот экипировки «маска» (поверх брони) — без сторонних библиотек.
 * Стек маски хранится на игроке как attachment: persistent (переживает перезаход)
 * и synced (виден на лице у других игроков). Пусто = маски нет (атрибут снят).
 */
public final class ModAttachments {
    public static final AttachmentType<ItemStack> MASK = AttachmentRegistry.<ItemStack>builder()
            .persistent(ItemStack.CODEC)
            .syncWith(ItemStack.STREAM_CODEC, AttachmentSyncPredicate.all())
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "mask"));

    private ModAttachments() {
    }

    /** Тач-класс: ссылка форсит статическую инициализацию (регистрацию) на старте мода. */
    public static void init() {
    }
}
