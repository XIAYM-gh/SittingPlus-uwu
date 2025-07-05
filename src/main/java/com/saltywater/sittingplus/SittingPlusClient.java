package com.saltywater.sittingplus;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class SittingPlusClient implements ClientModInitializer {
    private static final Identifier[] ANIM_STAIRS = new Identifier[]{ Identifier.of("sittingplus", "chairsitting"), Identifier.of("sittingplus", "chairsitting2"), Identifier.of("sittingplus", "chairsitting3"), Identifier.of("sittingplus", "chairsitting4") };
    private static final Identifier[] ANIM_GROUND = new Identifier[]{ Identifier.of("sittingplus", "kneesitting"), Identifier.of("sittingplus", "buttsit"), Identifier.of("sittingplus", "buttsit2"), Identifier.of("sittingplus", "kneeleaning") };
    private static final Identifier[] ANIM_FENCES = new Identifier[]{ Identifier.of("sittingplus", "fencesitting"), Identifier.of("sittingplus", "fencesitting2") };
    private static final Identifier[] ANIM_BEDS = new Identifier[]{ Identifier.of("sittingplus", "bedlyingdown"), Identifier.of("sittingplus", "bedlyingdown2"), Identifier.of("sittingplus", "bedlyingdown3") };
    private static final Identifier[] ANIM_SWORDS = new Identifier[]{ Identifier.of("sittingplus", "swordsit"), Identifier.of("sittingplus", "swordsit2") };
    private static final Identifier[] ANIM_AXE = new Identifier[]{ Identifier.of("sittingplus", "sittingaxe") };
    private static final Identifier[] ANIM_SHOVEL = new Identifier[]{ Identifier.of("sittingplus", "sittingshovel") };
    private static final Identifier[] ANIM_FISHINGROD = new Identifier[]{ Identifier.of("sittingplus", "fishing") };
    private static KeyBinding sitKey;
    private static KeyframeAnimationPlayer sitAnimationPlayer;
    private static Perspective previousPerspective = null;
    private int animationState = 0;

    public void onInitializeClient() {
        sitKey = new KeyBinding("key.sittingplus.sit", Type.KEYSYM, GLFW.GLFW_KEY_X, "category.sittingplus");
        KeyBindingHelper.registerKeyBinding(sitKey);
        UseBlockCallback.EVENT.register(this::onRightClickBlock);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        WorldRenderEvents.START.register(this::onWorldRenderStart);
    }

    private ActionResult onRightClickBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (SittingPlusConfig.getConfig().enableClickToSit) {
            if (!(player instanceof ClientPlayerEntity local) || !world.isClient) {
                return ActionResult.PASS;
            }

            if (local.getMainHandStack().isEmpty()) {
                ItemStack stack = local.getMainHandStack();
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                    BlockPos pos = hit.getBlockPos();

                    // Add a distance check because this mod is now client-side only
                    if (!pos.isWithinDistance(local.getPos(), 2.5)) {
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

                        local.setPos(x, y, z);

                        switch (state.get(StairsBlock.FACING)) {
                            case NORTH:
                                local.setYaw(0.0F);
                                break;
                            case SOUTH:
                                local.setYaw(180.0F);
                                break;
                            case WEST:
                                local.setYaw(270.0F);
                                break;
                            case EAST:
                                local.setYaw(90.0F);
                        }

                        local.setPitch(0.0F);

                        AnimationStack animStack = PlayerAnimationAccess.getPlayerAnimLayer(local);
                        if (animStack != null) {
                            this.stopCurrentAnimation(animStack);
                            this.animationState = 0;
                            this.playAnimation(animStack, ANIM_STAIRS);
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
        if (player != null && client.isWindowFocused()) {
            AnimationStack stack = PlayerAnimationAccess.getPlayerAnimLayer(player);
            if (sitKey.wasPressed() && stack != null) {
                this.stopCurrentAnimation(stack);
                this.handleSitKey(player, stack);
            }

            boolean moving = player.getVelocity()
                    .horizontalLengthSquared() > 1.0E-4 || player.isInSneakingPose() || player.isSwimming();
            if (stack != null && moving) {
                this.stopCurrentAnimation(stack);
                this.animationState = 0;
            }

            long delayMs = SittingPlusConfig.getConfig().afkSitDelaySeconds * 1000L;
            if (sitAnimationPlayer == null && System.currentTimeMillis() - delayMs >= System.currentTimeMillis() && stack != null) {
                this.playAnimation(stack, ANIM_GROUND);
                this.setThirdPersonIfEnabled();
            }
        }
    }

    private void onWorldRenderStart(WorldRenderContext ctx) {
        if (sitAnimationPlayer != null) {
            if (!SittingPlusConfig.getConfig().onlyLowerCameraInFirstPerson || MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON) {
                MatrixStack stack = ctx.matrixStack();
                if (stack != null) {
                    stack.translate(0.0, 0.7, 0.0);
                }
            }
        }
    }

    private void handleSitKey(ClientPlayerEntity player, AnimationStack stack) {
        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVector().multiply(2.0);
        BlockHitResult tr = player.clientWorld.raycast(new RaycastContext(eye, eye.add(dir), ShapeType.OUTLINE, FluidHandling.NONE, player));
        if (tr.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            Block block = player.clientWorld.getBlockState(tr.getBlockPos()).getBlock();
            if (block instanceof CampfireBlock) {
                this.playAnimation(stack, new Identifier[]{ Identifier.of("sittingplus", "campfiresit") });
                this.setThirdPersonIfEnabled();
                return;
            }

            if (block instanceof FurnaceBlock) {
                this.playAnimation(stack, new Identifier[]{ Identifier.of("sittingplus", "furnacesit") });
                this.setThirdPersonIfEnabled();
                return;
            }
        }

        ItemStack heldStack = player.getMainHandStack();
        Item heldItem = heldStack.getItem();
        if (heldStack.isIn(ItemTags.SWORDS)) {
            this.playAnimation(stack, ANIM_SWORDS);
        } else if (heldItem instanceof AxeItem) {
            this.playAnimation(stack, ANIM_AXE);
        } else if (heldItem instanceof ShovelItem) {
            this.playAnimation(stack, ANIM_SHOVEL);
        } else if (heldItem instanceof FishingRodItem) {
            this.playAnimation(stack, ANIM_FISHINGROD);
        } else {
            Vec3d start = player.getPos();
            Vec3d end = start.subtract(0.0, 1.5, 0.0);
            BlockHitResult result = player.clientWorld.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, player));
            if (result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                Block targetBlock = player.clientWorld.getBlockState(result.getBlockPos()).getBlock();
                if (targetBlock instanceof StairsBlock) {
                    this.playAnimation(stack, ANIM_STAIRS);
                    this.setThirdPersonIfEnabled();
                    return;
                }

                if (targetBlock instanceof FenceBlock) {
                    this.playAnimation(stack, ANIM_FENCES);
                    this.setThirdPersonIfEnabled();
                    return;
                }

                if (targetBlock instanceof BedBlock) {
                    this.playAnimation(stack, ANIM_BEDS);
                    this.setThirdPersonIfEnabled();
                    return;
                }
            }

            this.playAnimation(stack, ANIM_GROUND);
            this.setThirdPersonIfEnabled();
        }
    }

    private void playAnimation(AnimationStack stack, Identifier[] list) {
        if (list.length != 0) {
            this.animationState %= list.length;
            Identifier id = list[this.animationState];
            if (PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation anim) {
                sitAnimationPlayer = new KeyframeAnimationPlayer(anim);
                stack.addAnimLayer(0, sitAnimationPlayer);
                this.animationState = (this.animationState + 1) % list.length;
            }
        }
    }

    private void stopCurrentAnimation(AnimationStack stack) {
        if (sitAnimationPlayer != null) {
            stack.removeLayer(0);
            sitAnimationPlayer = null;
            if (SittingPlusConfig.getConfig().enableThirdPersonOnSit && previousPerspective != null) {
                MinecraftClient.getInstance().options.setPerspective(previousPerspective);
            }

            previousPerspective = null;
        }
    }

    private void setThirdPersonIfEnabled() {
        if (SittingPlusConfig.getConfig().enableThirdPersonOnSit) {
            GameOptions opts = MinecraftClient.getInstance().options;
            Perspective current = opts.getPerspective();
            if (current == Perspective.FIRST_PERSON) {
                previousPerspective = current;
                opts.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        }
    }
}
