package com.example.alieninvasion.network;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: игрок нажал ЛКМ удерживая бластер II или III — сервер стреляет лазером. */
public record LeftClickBlasterPayload() implements CustomPacketPayload {
    public static final Type<LeftClickBlasterPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "left_click_blaster"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeftClickBlasterPayload> CODEC =
            StreamCodec.unit(new LeftClickBlasterPayload());

    @Override
    public Type<LeftClickBlasterPayload> type() {
        return TYPE;
    }
}
