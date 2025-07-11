package uwu.sittingplus.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uwu.sittingplus.client.AnimationController;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntity", at = @At("RETURN"))
    public void addEntity(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        AnimationController.tryPlayPending(player.getUuid());
    }
}
