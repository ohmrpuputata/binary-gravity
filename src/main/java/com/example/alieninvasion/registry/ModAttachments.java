package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
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

    /** Запас воздуха в герметичной маске (тики). Тратится в ядовитом газе, пополняется
     *  баллоном. Synced — рисуем шкалу в HUD. */
    public static final AttachmentType<Integer> MASK_AIR = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT)
            .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all())
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "mask_air"));

    /** Текущий радиационный ФОН вокруг игрока (0..99), посчитанный на СЕРВЕРЕ из реальной
     *  экспозиции (та же, что копит дозу) + буря. Synced — дозиметр в HUD рисует именно
     *  его, а не клиентскую переоценку: раньше дальние блоки завышали фон и буря не учитывалась. */
    public static final AttachmentType<Integer> RADIATION_FIELD = AttachmentRegistry.<Integer>builder()
            .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all())
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "radiation_field"));

    private ModAttachments() {
    }

    /** Тач-класс: ссылка форсит статическую инициализацию (регистрацию) на старте мода. */
    public static void init() {
    }
}
