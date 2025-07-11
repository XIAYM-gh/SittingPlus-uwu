package uwu.sittingplus.payload.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HelloV1S2CPayload(int protocolVersion) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "s2c/hello-v1");
    public static final CustomPayload.Id<HelloV1S2CPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, HelloV1S2CPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, HelloV1S2CPayload::protocolVersion, HelloV1S2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
