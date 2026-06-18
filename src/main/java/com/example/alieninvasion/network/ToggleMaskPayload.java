package com.example.alieninvasion.network;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: игрок нажал клавишу «маска» — сервер надевает/снимает (см. MaskSlot.toggle). */
public record ToggleMaskPayload() implements CustomPacketPayload {
    public static final Type<ToggleMaskPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "toggle_mask"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMaskPayload> CODEC =
            StreamCodec.unit(new ToggleMaskPayload());

    @Override
    public Type<ToggleMaskPayload> type() {
        return TYPE;
    }
}
