package com.jsblock.scripting.pids;

import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * The read-only "world" object handed to a PIDS script.
 * @author LX86 (JCM)
 */
public class PIDSWrapper {
    private final BlockEntityMapper blockEntity;
    private final String[] customMessages;
    private final boolean[] hiddenRows;
    private final List<Long> targetPlatformIds;
    private final ArrivalsWrapper arrivals;
    private final float width;
    private final float height;

    public PIDSWrapper(BlockEntityMapper blockEntity, String[] customMessages, boolean[] hiddenRows, List<Long> targetPlatformIds, float width, float height) {
        this.blockEntity = blockEntity;
        this.customMessages = customMessages == null ? new String[0] : customMessages;
        this.hiddenRows = hiddenRows == null ? new boolean[0] : hiddenRows;
        this.targetPlatformIds = targetPlatformIds;
        this.arrivals = ArrivalsWrapper.fromPlatformIds(targetPlatformIds, blockEntity.getBlockPos());
        this.width = width;
        this.height = height;
    }

    public String getType() {
        return "pids";
    }

    public int getRows() {
        return customMessages.length;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public List<Long> getTargetPlatformIds() {
        return targetPlatformIds;
    }

    /** The upcoming arrivals of this PIDS, for {@code pids.arrivals().get(i)}. */
    public ArrivalsWrapper arrivals() {
        return arrivals;
    }

    public BlockPos blockPos() {
        return blockEntity.getBlockPos();
    }

    public boolean isKeyBlock() {
        return true;
    }

    public boolean isRowHidden(int index) {
        return index >= 0 && index < hiddenRows.length && hiddenRows[index];
    }

    public String getCustomMessage(int index) {
        return index >= 0 && index < customMessages.length ? customMessages[index] : "";
    }
}
