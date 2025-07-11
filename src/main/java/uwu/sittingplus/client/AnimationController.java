package uwu.sittingplus.client;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.UUID;

public class AnimationController {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final HashMap<UUID, KeyframeAnimationPlayer> ANIMATION_PLAYERS = new HashMap<>();
    private static final HashMap<UUID, Identifier> PENDING = new HashMap<>();

    public static void play(UUID target, Identifier animId) {
        if (CLIENT.world == null || !(PlayerAnimationRegistry.getAnimation(animId) instanceof KeyframeAnimation anim)) {
            return;
        }

        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) CLIENT.world.getPlayerByUuid(target);
        if (player == null) {
            if (!PENDING.containsKey(target)) {
                PENDING.put(target, animId);
            }

            return;
        }

        AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(anim);
        stack.addAnimLayer(0, animationPlayer);

        ANIMATION_PLAYERS.put(target, animationPlayer);
        PENDING.remove(target);
    }

    public static void stop(UUID target) {
        if (CLIENT.world == null) {
            return;
        }

        PENDING.remove(target);

        KeyframeAnimationPlayer animationPlayer = ANIMATION_PLAYERS.getOrDefault(target, null);
        if (animationPlayer == null) {
            return;
        }

        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) CLIENT.world.getPlayerByUuid(target);
        if (player == null) {
            return;
        }

        AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        stack.removeLayer(0);
        ANIMATION_PLAYERS.remove(target);
    }

    public static void tryPlayPending(UUID target) {
        if (!PENDING.containsKey(target)) {
            return;
        }

        Identifier animId = PENDING.remove(target);
        play(target, animId);
    }

    public static boolean clientNotSitting() {
        return CLIENT.player == null || !ANIMATION_PLAYERS.containsKey(CLIENT.player.getUuid());
    }
}
