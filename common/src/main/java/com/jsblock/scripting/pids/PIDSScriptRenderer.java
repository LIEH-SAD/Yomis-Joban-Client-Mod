package com.jsblock.scripting.pids;

import com.jsblock.client.ClientConfig;
import com.jsblock.data.PIDSPreset;
import com.jsblock.scripting.Scripting;
import com.jsblock.scripting.primitive.ParsedScript;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Runs the PIDS script attached to a preset and draws its output.
 * The {@link PoseStack} is expected to already be placed by JCM 2.x's script panel transform, so
 * one unit of it is one script unit (one ninety-sixth of a block) and the draw calls must not
 * scale again.
 * @author LX86 (JCM)
 */
public final class PIDSScriptRenderer {
    private static final double DEFAULT_Z_ORDER_STEP = 0.0002;

    private PIDSScriptRenderer() {
    }

    /**
     * @return {@code true} when a script ran and painted the panel, {@code false} when there was
     * none to run -- no scripting support, scripts switched off in the config, or a preset whose
     * script could not be resolved. The caller falls back to its built-in drawing in that case, so
     * a broken pack still shows its background instead of a blank panel.
     */
    public static boolean render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light, BlockEntityMapper entity, PIDSPreset preset, String pidsType, String[] customMessages, boolean[] hiddenRows, List<Long> targetPlatformIds, float canvasWidth, float canvasHeight, boolean hidePlatforms, float canvasUnitsPerMeter) {
        if (ClientConfig.getSkipScriptParsing() || !Scripting.isInitialized() || preset == null) return false;

        final ParsedScript parsedScript = Scripting.resolvePidsScript(preset);
        if (parsedScript == null) return false;

        final boolean autoDetectedPlatforms = targetPlatformIds == null || targetPlatformIds.isEmpty();
        final PIDSWrapper wrapper = new PIDSWrapper(entity, customMessages, hiddenRows, targetPlatformIds, canvasWidth, canvasHeight, pidsType, autoDetectedPlatforms, hidePlatforms);

        /* One instance per block entity, rebuilt whenever the block switches to another preset */
        PIDSScriptInstance instance = Scripting.getInstanceManager().get(entity) instanceof PIDSScriptInstance ? (PIDSScriptInstance) Scripting.getInstanceManager().get(entity) : null;

        if (instance == null || instance.getPreset() != preset || instance.getScript() != parsedScript) {
            Scripting.getInstanceManager().remove(entity);
            instance = Scripting.getInstanceManager().computeIfAbsent(entity,
                    () -> new PIDSScriptInstance(parsedScript, new PIDSScriptContext(), wrapper, entity, preset));
        }

        instance.setWrapperObject(parsedScript.usesLenientArrivals() ? wrapper.withLenientArrivals() : wrapper);

        final PIDSScriptInstance finalInstance = instance;
        parsedScript.invokeRenderFunctions(finalInstance, () -> {
            if (finalInstance.getContextObject() instanceof PIDSScriptContext) {
                finalInstance.saveRenderCalls(((PIDSScriptContext) finalInstance.getContextObject()).getDrawCalls());
            }
        });

        final PIDSScriptContext context = finalInstance.getContextObject() instanceof PIDSScriptContext ? (PIDSScriptContext) finalInstance.getContextObject() : null;
        /* One z-order slot is zOrderStep blocks apart, which is converted into canvas units here */
        final double zOrderStep = (context == null ? DEFAULT_Z_ORDER_STEP : context.getZOrderStep()) * canvasUnitsPerMeter;

        final List<PIDSDrawCall<?>> renderCalls = finalInstance.getRenderCalls();
        for (final PIDSDrawCall<?> call : renderCalls) {
            matrices.pushPose();
            if ((call.hasExplicitZOrder() || call.hasAutoZOrder()) && zOrderStep != 0) {
                /* A higher z-order is closer to the viewer, as in JCM 2.3 */
                matrices.translate(0, 0, -call.getZOrder() * zOrderStep);
            }
            if (call.getMatrices() != null) {
                call.getMatrices().apply(matrices);
            }
            call.render(matrices, vertexConsumers, immediate, font, facing, light);
            matrices.popPose();
        }

        return true;
    }
}
