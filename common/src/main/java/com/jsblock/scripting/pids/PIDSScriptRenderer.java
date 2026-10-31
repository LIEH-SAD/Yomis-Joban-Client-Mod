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
    private PIDSScriptRenderer() {
    }

    public static void render(PoseStack matrices, MultiBufferSource vertexConsumers, MultiBufferSource.BufferSource immediate, Font font, Direction facing, int light, BlockEntityMapper entity, PIDSPreset preset, String[] customMessages, boolean[] hiddenRows, List<Long> targetPlatformIds, float canvasWidth, float canvasHeight) {
        if (ClientConfig.getSkipScriptParsing() || !Scripting.isInitialized() || preset == null) return;

        final ParsedScript parsedScript = Scripting.resolvePidsScript(preset);
        if (parsedScript == null) return;

        final PIDSWrapper wrapper = new PIDSWrapper(entity, customMessages, hiddenRows, targetPlatformIds, canvasWidth, canvasHeight);

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

        final List<PIDSDrawCall<?>> renderCalls = finalInstance.getRenderCalls();
        for (int i = 0; i < renderCalls.size(); i++) {
            renderCalls.get(i).render(matrices, vertexConsumers, immediate, font, facing, light);
        }
    }
}
