package com.enchantmenu;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ApplyPayload(String data) implements CustomPayload {
    public static final CustomPayload.Id<ApplyPayload> ID =
        new CustomPayload.Id<>(Identifier.of("enchantmenu", "apply"));

    public static final PacketCodec<RegistryByteBuf, ApplyPayload> CODEC =
        PacketCodec.tuple(PacketCodecs.STRING, ApplyPayload::data, ApplyPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
