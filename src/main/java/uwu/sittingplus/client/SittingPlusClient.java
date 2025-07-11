package uwu.sittingplus.client;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Colors;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import uwu.sittingplus.Constants;
import uwu.sittingplus.payload.c2s.HelloV1C2SPayload;
import uwu.sittingplus.payload.c2s.SitV1C2SPayload;
import uwu.sittingplus.payload.c2s.StopSitV1C2SPayload;
import uwu.sittingplus.payload.s2c.HelloV1S2CPayload;
import uwu.sittingplus.payload.s2c.PoseSyncV1S2CPayload;
import uwu.sittingplus.payload.s2c.StopSitV1S2CPayload;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class SittingPlusClient implements ClientModInitializer {
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Identifier[] ANIM_STAIRS = new Identifier[]{ Identifier.of("sittingplus", "chairsitting"), Identifier.of("sittingplus", "chairsitting2"), Identifier.of("sittingplus", "chairsitting3"), Identifier.of("sittingplus", "chairsitting4") };
    private static final Identifier[] ANIM_GROUND = new Identifier[]{ Identifier.of("sittingplus", "kneesitting"), Identifier.of("sittingplus", "buttsit"), Identifier.of("sittingplus", "buttsit2"), Identifier.of("sittingplus", "kneeleaning") };
    private static final Identifier[] ANIM_FENCES = new Identifier[]{ Identifier.of("sittingplus", "fencesitting"), Identifier.of("sittingplus", "fencesitting2") };
    private static final Identifier[] ANIM_BEDS = new Identifier[]{ Identifier.of("sittingplus", "bedlyingdown"), Identifier.of("sittingplus", "bedlyingdown2"), Identifier.of("sittingplus", "bedlyingdown3") };
    private static final Identifier[] ANIM_SWORDS = new Identifier[]{ Identifier.of("sittingplus", "swordsit"), Identifier.of("sittingplus", "swordsit2") };
    private static final Identifier[] ANIM_AXE = new Identifier[]{ Identifier.of("sittingplus", "sittingaxe") };
    private static final Identifier[] ANIM_SHOVEL = new Identifier[]{ Identifier.of("sittingplus", "sittingshovel") };
    private static final Identifier[] ANIM_FISHING_ROD = new Identifier[]{ Identifier.of("sittingplus", "fishing") };
    private static final HashMap<String, Identifier> POSES = new HashMap<>();
    private static boolean serverCompatible = false;
    private static KeyBinding sitKey;
    private static Perspective previousPerspective = null;
    private int animationIdx = 0;

    public void onInitializeClient() {
        sitKey = new KeyBinding("key.sittingplus.sit", Type.KEYSYM, GLFW.GLFW_KEY_X, "category.sittingplus");
        KeyBindingHelper.registerKeyBinding(sitKey);
        UseBlockCallback.EVENT.register(this::onRightClickBlock);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Prepare valid poses
        Arrays.stream(new Identifier[][]{ ANIM_STAIRS, ANIM_GROUND, ANIM_FENCES, ANIM_BEDS, ANIM_SWORDS, ANIM_AXE, ANIM_SHOVEL, ANIM_FISHING_ROD, new Identifier[]{ Identifier.of("sittingplus", "campfiresit"), Identifier.of("sittingplus", "furnacesit") } })
                .flatMap(Arrays::stream).forEach(i -> POSES.put(i.getPath(), i));

        ClientPlayConnectionEvents.JOIN.register((_handler, sender, _client) -> {
            // Reset variables
            serverCompatible = false;

            // Begin handshaking
            sender.sendPacket(new HelloV1C2SPayload(Constants.PROTOCOL_VERSION));
        });

        ClientPlayNetworking.registerGlobalReceiver(HelloV1S2CPayload.ID, (payload, context) -> {
            assert CLIENT.player != null;
            if (payload.protocolVersion() != Constants.PROTOCOL_VERSION) {
                CLIENT.player.sendMessage(Text.literal("The server incompatible with your version of SittingPlus-uwu, disabling synchronization. (Server: %s, Expected: %s).".formatted(payload.protocolVersion(), Constants.PROTOCOL_VERSION))
                        .withColor(Colors.LIGHT_GRAY), false);
                return;
            }

            serverCompatible = true;
        });

        ClientPlayNetworking.registerGlobalReceiver(PoseSyncV1S2CPayload.ID, (payload, context) -> {
            assert CLIENT.player != null;

            // Do nothing if the target is the client player.
            UUID target = payload.targetPlayer();
            if (target.equals(CLIENT.player.getUuid())) {
                return;
            }

            AnimationController.stop(target);

            Identifier pose = POSES.getOrDefault(payload.pose(), null);
            if (pose != null) {
                AnimationController.play(target, pose);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(StopSitV1S2CPayload.ID, (payload, context) -> {
            assert CLIENT.player != null;

            UUID target = payload.targetPlayer();
            if (target.equals(CLIENT.player.getUuid())) {
                this.animationIdx = 0;
            }

            AnimationController.stop(target);
        });
    }

    private ActionResult onRightClickBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (ClientConfig.getConfig().enableClickToSit) {
            if (!(player instanceof ClientPlayerEntity localPlayer) || !world.isClient) {
                return ActionResult.PASS;
            }

            if (localPlayer.getMainHandStack().isEmpty()) {
                ItemStack stack = localPlayer.getMainHandStack();
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                    BlockPos pos = hit.getBlockPos();

                    // Add a distance check
                    if (!pos.isWithinDistance(localPlayer.getPos(), 2.5)) {
                        return ActionResult.PASS;
                    }

                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock() instanceof StairsBlock) {
                        double offsetFactor = 0.4;
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 0.5;
                        double z = pos.getZ() + 0.5;
                        switch (state.get(StairsBlock.FACING)) {
                            case NORTH:
                                z += offsetFactor;
                                break;
                            case SOUTH:
                                z -= offsetFactor;
                                break;
                            case WEST:
                                x += offsetFactor;
                                break;
                            case EAST:
                                x -= offsetFactor;
                        }

                        localPlayer.setPos(x, y, z);

                        switch (state.get(StairsBlock.FACING)) {
                            case NORTH:
                                localPlayer.setYaw(0.0F);
                                break;
                            case SOUTH:
                                localPlayer.setYaw(180.0F);
                                break;
                            case WEST:
                                localPlayer.setYaw(270.0F);
                                break;
                            case EAST:
                                localPlayer.setYaw(90.0F);
                        }

                        localPlayer.setPitch(0.0F);

                        AnimationStack animStack = PlayerAnimationAccess.getPlayerAnimLayer(localPlayer);
                        if (animStack != null) {
                            this.stopCurrentAnimation();
                            this.animationIdx = 0;
                            this.playAnimation(ANIM_STAIRS);
                            this.setThirdPersonIfEnabled();
                        }

                        return ActionResult.SUCCESS;
                    }

                }
            }
        }
        return ActionResult.PASS;
    }

    private void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        if (client.isWindowFocused()) {
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (sitKey.wasPressed() && stack != null) {
                this.stopCurrentAnimation();
                this.handleSitKey(player);
            }

            boolean moving = player.getVelocity()
                    .horizontalLengthSquared() > 1.0E-4 || player.isInSneakingPose() || player.isSwimming();
            if (stack != null && moving) {
                this.stopCurrentAnimation();
                this.animationIdx = 0;

                ClientPlayNetworking.send(StopSitV1C2SPayload.INSTANCE);
            }

            long delayMs = ClientConfig.getConfig().afkSitDelaySeconds * 1000L;
            if (AnimationController.clientNotSitting() && System.currentTimeMillis() - delayMs >= System.currentTimeMillis() && stack != null) {
                this.playAnimation(ANIM_GROUND);
                this.setThirdPersonIfEnabled();
            }
        }
    }

    private void handleSitKey(ClientPlayerEntity player) {
        if (player.isSneaking()) {
            return;
        }

        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVector().multiply(2.0);
        BlockHitResult tr = player.clientWorld.raycast(new RaycastContext(eye, eye.add(dir), ShapeType.OUTLINE, FluidHandling.NONE, player));
        if (tr.getType() == HitResult.Type.BLOCK) {
            Block block = player.clientWorld.getBlockState(tr.getBlockPos()).getBlock();
            if (block instanceof CampfireBlock) {
                this.playAnimation(new Identifier[]{ Identifier.of("sittingplus", "campfiresit") });
                this.setThirdPersonIfEnabled();
                return;
            }

            if (block instanceof FurnaceBlock) {
                this.playAnimation(new Identifier[]{ Identifier.of("sittingplus", "furnacesit") });
                this.setThirdPersonIfEnabled();
                return;
            }
        }

        ItemStack heldStack = player.getMainHandStack();
        Item heldItem = heldStack.getItem();
        if (heldStack.isIn(ItemTags.SWORDS)) {
            this.playAnimation(ANIM_SWORDS);
        } else if (heldItem instanceof AxeItem) {
            this.playAnimation(ANIM_AXE);
        } else if (heldItem instanceof ShovelItem) {
            this.playAnimation(ANIM_SHOVEL);
        } else if (heldItem instanceof FishingRodItem) {
            this.playAnimation(ANIM_FISHING_ROD);
        } else {
            Vec3d start = player.getPos();
            Vec3d end = start.subtract(0.0, 1.5, 0.0);
            BlockHitResult result = player.clientWorld.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, player));
            if (result.getType() == HitResult.Type.BLOCK) {
                Block targetBlock = player.clientWorld.getBlockState(result.getBlockPos()).getBlock();
                if (targetBlock instanceof StairsBlock) {
                    this.playAnimation(ANIM_STAIRS);
                    this.setThirdPersonIfEnabled();
                    return;
                }

                if (targetBlock instanceof FenceBlock) {
                    this.playAnimation(ANIM_FENCES);
                    this.setThirdPersonIfEnabled();
                    return;
                }

                if (targetBlock instanceof BedBlock) {
                    this.playAnimation(ANIM_BEDS);
                    this.setThirdPersonIfEnabled();
                    return;
                }
            }

            this.playAnimation(ANIM_GROUND);
            this.setThirdPersonIfEnabled();
        }
    }

    private void playAnimation(Identifier[] list) {
        if (list.length == 0 || CLIENT.player == null) {
            return;
        }

        this.animationIdx %= list.length;
        Identifier id = list[(this.animationIdx++) % list.length];
        AnimationController.play(CLIENT.player.getUuid(), id);

        if (serverCompatible) {
            ClientPlayNetworking.send(new SitV1C2SPayload(id.getPath()));
        }
    }

    private void stopCurrentAnimation() {
        if (CLIENT.player == null || AnimationController.clientNotSitting()) {
            return;
        }

        AnimationController.stop(CLIENT.player.getUuid());
        if (ClientConfig.getConfig().enableThirdPersonOnSit && previousPerspective != null && MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_BACK) {
            MinecraftClient.getInstance().options.setPerspective(previousPerspective);
        }

        previousPerspective = null;
    }

    private void setThirdPersonIfEnabled() {
        if (ClientConfig.getConfig().enableThirdPersonOnSit) {
            GameOptions opts = MinecraftClient.getInstance().options;
            Perspective current = opts.getPerspective();
            if (current == Perspective.FIRST_PERSON) {
                previousPerspective = current;
                opts.setPerspective(Perspective.THIRD_PERSON_BACK);
            } else if (current == Perspective.THIRD_PERSON_FRONT) {
                // Fix annoying perspective issue
                previousPerspective = null;
            }
        }
    }
}
