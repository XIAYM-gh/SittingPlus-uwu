package uwu.sittingplus.payload.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record PoseSyncV1S2CPayload(UUID targetPlayer, String pose) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "s2c/sync-v1");
    public static final CustomPayload.Id<PoseSyncV1S2CPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, PoseSyncV1S2CPayload> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC, PoseSyncV1S2CPayload::targetPlayer, PacketCodecs.STRING, PoseSyncV1S2CPayload::pose, PoseSyncV1S2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
