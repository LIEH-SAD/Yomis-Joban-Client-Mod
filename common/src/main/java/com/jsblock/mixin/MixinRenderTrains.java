package com.jsblock.mixin;

import com.jsblock.client.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.data.Rail;
import mtr.render.RenderTrains;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Allows disabling the rendering of rails and signals via the client config.
 * @since 1.2.12
 */
@Mixin(value = RenderTrains.class, remap = false)
public class MixinRenderTrains {

	@Inject(method = "renderRailStandard(Lnet/minecraft/world/level/Level;Lmtr/data/Rail;FZF)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean showOneWayArrows, float transportModeRailWidth, CallbackInfo ci) {
		if(ClientConfig.getRailRenderDisabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderRailStandard(Lnet/minecraft/world/level/Level;Lmtr/data/Rail;FZFLjava/lang/String;FFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void renderRailStandardColored(Level world, Rail rail, float yOffset, boolean showOneWayArrows, float transportModeRailWidth, String texture, float u1, float v1, float u2, float v2, CallbackInfo ci) {
		if(ClientConfig.getRailRenderDisabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderRailStandardSafe(Lnet/minecraft/world/level/Level;Lmtr/data/Rail;FZF)V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void renderRailStandardSafe(Level world, Rail rail, float yOffset, boolean showOneWayArrows, float transportModeRailWidth, CallbackInfo ci) {
		if(ClientConfig.getRailRenderDisabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSignalsStandard", at = @At("HEAD"), cancellable = true, remap = false)
	private static void renderSignalsStandard(Level world, PoseStack matrices, MultiBufferSource vertexConsumers, Rail rail, BlockPos posStart, BlockPos posEnd, CallbackInfo ci) {
		if(ClientConfig.getRailRenderDisabled()) {
			ci.cancel();
		}
	}
}
