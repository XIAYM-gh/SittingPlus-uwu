package uwu.sittingplus.payload.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SitV1C2SPayload(String identifierPath) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "c2s/sit-v1");
    public static final CustomPayload.Id<SitV1C2SPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, SitV1C2SPayload> CODEC = PacketCodec.tuple(PacketCodecs.STRING, SitV1C2SPayload::identifierPath, SitV1C2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
