package uwu.sittingplus.payload.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record StopSitV1S2CPayload(UUID targetPlayer) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "s2c/stop-v1");
    public static final CustomPayload.Id<StopSitV1S2CPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, StopSitV1S2CPayload> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC, StopSitV1S2CPayload::targetPlayer, StopSitV1S2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
