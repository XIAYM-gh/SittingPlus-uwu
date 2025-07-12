package uwu.sittingplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uwu.sittingplus.payload.c2s.HelloV1C2SPayload;
import uwu.sittingplus.payload.c2s.SitV1C2SPayload;
import uwu.sittingplus.payload.c2s.StopSitV1C2SPayload;
import uwu.sittingplus.payload.s2c.HelloV1S2CPayload;
import uwu.sittingplus.payload.s2c.PoseSyncV1S2CPayload;
import uwu.sittingplus.payload.s2c.StopSitV1S2CPayload;

import java.util.*;

public class SittingPlusInitializer implements ModInitializer {
    private static final HashMap<UUID, String> PLAYER_SIT_POSES = new HashMap<>();
    private static final ArrayList<UUID> PENDING = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SittingPlusInitializer.class);

    private static void reset() {
        PLAYER_SIT_POSES.clear();
        PENDING.clear();
    }

    @SuppressWarnings("resource")
    @Override
    public void onInitialize() {
        // Register Payloads
        PayloadTypeRegistry<RegistryByteBuf> playC2S = PayloadTypeRegistry.playC2S();
        playC2S.register(HelloV1C2SPayload.ID, HelloV1C2SPayload.CODEC);
        playC2S.register(SitV1C2SPayload.ID, SitV1C2SPayload.CODEC);
        playC2S.register(StopSitV1C2SPayload.ID, StopSitV1C2SPayload.CODEC);

        PayloadTypeRegistry<RegistryByteBuf> playS2C = PayloadTypeRegistry.playS2C();
        playS2C.register(HelloV1S2CPayload.ID, HelloV1S2CPayload.CODEC);
        playS2C.register(PoseSyncV1S2CPayload.ID, PoseSyncV1S2CPayload.CODEC);
        playS2C.register(StopSitV1S2CPayload.ID, StopSitV1S2CPayload.CODEC);

        // Payload Handling
        ServerPlayNetworking.registerGlobalReceiver(HelloV1C2SPayload.ID, (payload, context) -> {
            context.responseSender().sendPacket(new HelloV1S2CPayload(Constants.PROTOCOL_VERSION));

            if (payload.protocolVersion() != Constants.PROTOCOL_VERSION) {
                LOGGER.warn("Player {} joined with a mismatch SittingPlus-uwu client!", context.player().getName());
                return;
            }

            UUID uuid = context.player().getUuid();
            if (PENDING.contains(uuid)) {
                PENDING.remove(uuid);
                for (Map.Entry<UUID, String> entry : PLAYER_SIT_POSES.entrySet()) {
                    context.responseSender().sendPacket(new PoseSyncV1S2CPayload(entry.getKey(), entry.getValue()));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SitV1C2SPayload.ID, (payload, context) -> {
            UUID uuid = context.player().getUuid();
            String pose = payload.identifierPath();
            PLAYER_SIT_POSES.put(uuid, pose);

            // Sync to all players
            context.server().getPlayerManager()
                    .sendToAll(ServerPlayNetworking.createS2CPacket(new PoseSyncV1S2CPayload(uuid, pose)));
        });

        ServerPlayNetworking.registerGlobalReceiver(StopSitV1C2SPayload.ID, (payload, context) -> handleStopSit(context.server(), context.player()
                .getUuid()));

        // Events
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> reset());

        // Sync to new players
        ServerPlayConnectionEvents.JOIN.register((handler, sender, _server) -> {
            UUID uuid = handler.player.getUuid();
            if (!PENDING.contains(uuid)) {
                PENDING.add(uuid);
            }
        });

        // Auto reset player poses
        HashMap<UUID, Vec3d> lastPos = new HashMap<>();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<ServerPlayerEntity> list = server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity player : list) {
                UUID uuid = player.getUuid();
                Vec3d pos = player.getPos();
                Vec3d prevPos = lastPos.put(uuid, pos);

                if ((prevPos != null && prevPos.squaredDistanceTo(pos) > 0.05) || player.isInSneakingPose() || player.isSwimming()) {
                    handleStopSit(server, uuid);
                }
            }
        });
    }

    private void handleStopSit(MinecraftServer server, UUID uuid) {
        if (PLAYER_SIT_POSES.remove(uuid) == null) {
            return;
        }

        // Sync state to all players
        server.getPlayerManager().sendToAll(ServerPlayNetworking.createS2CPacket(new StopSitV1S2CPayload(uuid)));
    }
}
