package uwu.sittingplus.payload.c2s;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class StopSitV1C2SPayload implements CustomPayload {
    public static final StopSitV1C2SPayload INSTANCE = new StopSitV1C2SPayload();
    public static final Identifier IDENTIFIER = Identifier.of("sitting-plus-uwu", "c2s/stop-v1");
    public static final CustomPayload.Id<StopSitV1C2SPayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, StopSitV1C2SPayload> CODEC = PacketCodec.unit(INSTANCE);

    private StopSitV1C2SPayload() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
