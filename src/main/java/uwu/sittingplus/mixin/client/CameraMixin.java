package uwu.sittingplus.mixin.client;

import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uwu.sittingplus.client.AnimationController;
import uwu.sittingplus.client.ClientConfig;
import uwu.sittingplus.client.SittingPlusClient;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private Vec3d pos;

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    public void getPos(CallbackInfoReturnable<Vec3d> cir) {
        if (AnimationController.clientNotSitting()
                || ClientConfig.getConfig().onlyLowerCameraInFirstPerson && SittingPlusClient.CLIENT.options.getPerspective() == Perspective.FIRST_PERSON
                || SittingPlusClient.CLIENT.cameraEntity == null) {
            return;
        }

        cir.setReturnValue(pos.subtract(0, SittingPlusClient.CLIENT.cameraEntity.getStandingEyeHeight() * 0.3, 0));
    }
}
