package com.jsblock.scripting.pids;

import com.jsblock.data.PIDSPreset;
import com.jsblock.scripting.primitive.ParsedScript;
import com.jsblock.scripting.primitive.ScriptInstance;
import mtr.mappings.BlockEntityMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A live PIDS script, bound to one PIDS block entity.
 * @author LX86 (JCM)
 */
public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final BlockEntityMapper blockEntity;
    private final PIDSPreset preset;
    private volatile List<PIDSDrawCall<?>> renderCalls = Collections.emptyList();

    public PIDSScriptInstance(ParsedScript parsedScript, PIDSScriptContext context, PIDSWrapper wrapper, BlockEntityMapper blockEntity, PIDSPreset preset) {
        super(context, parsedScript);
        setWrapperObject(wrapper);
        this.blockEntity = blockEntity;
        this.preset = preset;
    }

    public void saveRenderCalls(List<PIDSDrawCall<?>> calls) {
        final List<PIDSDrawCall<?>> valid = new ArrayList<>(calls.size());
        for (final PIDSDrawCall<?> call : calls) {
            if (call.validate()) {
                valid.add(call);
            }
        }
        // Replacing the whole list avoids the render thread observing a half-cleared buffer.
        renderCalls = Collections.unmodifiableList(valid);
    }

    public List<PIDSDrawCall<?>> getRenderCalls() {
        return renderCalls;
    }

    public PIDSPreset getPreset() {
        return preset;
    }

    @Override
    public boolean shouldInvalidate() {
        return blockEntity == null || blockEntity.isRemoved() || blockEntity.getLevel() == null;
    }
}
