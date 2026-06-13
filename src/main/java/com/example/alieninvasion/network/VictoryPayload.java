package com.example.alieninvasion.network;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: сообщает клиенту, что вторжение завершено победой — чтобы клиентский HUD
 * вторжения полностью скрылся. Scoreboard-теги на клиента не синхронизируются,
 * поэтому состояние победы доставляем явным пакетом.
 */
public record VictoryPayload(boolean won) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VictoryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "victory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VictoryPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, VictoryPayload::won, VictoryPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
