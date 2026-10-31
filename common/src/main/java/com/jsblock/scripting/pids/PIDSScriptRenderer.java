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
 * The {@link PoseStack} is expected to already be translated to the top-left corner of the screen,
 * with one unit being one texel of the preset background.
 * @author LX86 (JCM)
 */
public final class PIDSScriptRenderer {
    private static final double DEFAULT_Z_ORDER_STEP = 0.0002;

    private PIDSScriptRenderer() {
    }

    public static void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light, BlockEntityMapper entity, PIDSPreset preset, String pidsType, String[] customMessages, boolean[] hiddenRows, List<Long> targetPlatformIds, float canvasWidth, float canvasHeight, boolean hidePlatforms, float canvasUnitsPerMeter) {
        if (ClientConfig.getSkipScriptParsing() || !Scripting.isInitialized() || preset == null) return;

        final ParsedScript parsedScript = Scripting.resolvePidsScript(preset);
        if (parsedScript == null) return;

        final boolean autoDetectedPlatforms = targetPlatformIds == null || targetPlatformIds.isEmpty();
        final PIDSWrapper wrapper = new PIDSWrapper(entity, customMessages, hiddenRows, targetPlatformIds, canvasWidth, canvasHeight, pidsType, autoDetectedPlatforms, hidePlatforms);

        /* One instance per block entity, rebuilt whenever the block switches to another preset */
        PIDSScriptInstance instance = Scripting.getInstanceManager().get(entity) instanceof PIDSScriptInstance ? (PIDSScriptInstance) Scripting.getInstanceManager().get(entity) : null;

        if (instance == null || instance.getPreset() != preset || instance.getScript() != parsedScript) {
            Scripting.getInstanceManager().remove(entity);
            instance = Scripting.getInstanceManager().computeIfAbsent(entity,
                    () -> new PIDSScriptInstance(parsedScript, new PIDSScriptContext(), wrapper, entity, preset));
        }

        instance.setWrapperObject(wrapper);

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
    }
}
