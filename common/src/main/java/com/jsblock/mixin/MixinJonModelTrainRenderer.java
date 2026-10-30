package com.jsblock.mixin;

import com.jsblock.client.ClientConfig;
import mtr.data.TrainClient;
import mtr.render.JonModelTrainRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows hiding the train the local player is currently riding on.
 * @since 1.2.12
 */
@Mixin(value = JonModelTrainRenderer.class, remap = false)
public class MixinJonModelTrainRenderer {

	@Shadow(remap = false)
	private TrainClient train;

	@Inject(method = "renderCar", at = @At("HEAD"), cancellable = true, remap = false)
	private void jsblock$hideRidingVehicle(int carNumber, double x, double y, double z, float yaw, float pitch, boolean isEndCar, boolean isEndCarReversed, CallbackInfo ci) {
		if(!ClientConfig.getHideRidingVehicle() || train == null) {
			return;
		}

		final LocalPlayer player = Minecraft.getInstance().player;

		if(player != null && ((MixinTrainAccessor) (Object) train).getRidingEntities().contains(player.getUUID())) {
			ci.cancel();
		}
	}
}
