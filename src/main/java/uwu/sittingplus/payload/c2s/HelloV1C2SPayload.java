package uwu.sittingplus.payload.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HelloV1C2SPayload(int protocolVersion) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "c2s/hello-v1");
    public static final CustomPayload.Id<HelloV1C2SPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, HelloV1C2SPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, HelloV1C2SPayload::protocolVersion, HelloV1C2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
