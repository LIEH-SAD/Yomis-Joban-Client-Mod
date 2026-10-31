package com.jsblock.scripting.pids;

import com.jsblock.scripting.api.Vector3f;
import mtr.client.ClientData;
import mtr.data.RailwayData;
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
    private final String type;
    private final boolean autoDetectedPlatforms;
    private final boolean platformNumberHidden;

    public PIDSWrapper(BlockEntityMapper blockEntity, String[] customMessages, boolean[] hiddenRows, List<Long> targetPlatformIds, float width, float height, String type, boolean autoDetectedPlatforms, boolean platformNumberHidden) {
        this.blockEntity = blockEntity;
        this.customMessages = customMessages == null ? new String[0] : customMessages;
        this.hiddenRows = hiddenRows == null ? new boolean[0] : hiddenRows;
        this.targetPlatformIds = targetPlatformIds;
        this.arrivals = ArrivalsWrapper.fromPlatformIds(targetPlatformIds, blockEntity == null ? null : blockEntity.getBlockPos());
        this.width = width;
        this.height = height;
        this.type = type == null ? "pids" : type;
        this.autoDetectedPlatforms = autoDetectedPlatforms;
        this.platformNumberHidden = platformNumberHidden;
    }

    /** The PIDS type, e.g. {@code lcd_pids}, {@code rv_pids}, {@code rv_pids_sil_1} or {@code rv_pids_sil_2}. */
    public String getType() {
        return type;
    }

    public int getRows() {
        return customMessages.length;
    }

    public int getWidth() {
        return Math.round(width);
    }

    public int getHeight() {
        return Math.round(height);
    }

    public List<Long> getTargetPlatformIds() {
        return targetPlatformIds;
    }

    /** The upcoming arrivals of this PIDS, for {@code pids.arrivals().get(i)}. */
    public ArrivalsWrapper arrivals() {
        return arrivals;
    }

    public Vector3f blockPos() {
        if (blockEntity == null) return Vector3f.ZERO;
        final BlockPos pos = blockEntity.getBlockPos();
        return new Vector3f(pos.getX(), pos.getY(), pos.getZ());
    }

    /** The station area this PIDS is in, or {@code null} when the client does not know it. */
    public StationWrapper station() {
        if (blockEntity == null) return null;
        try {
            return StationWrapper.of(RailwayData.getStation(ClientData.STATIONS, ClientData.DATA_CACHE, blockEntity.getBlockPos()));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isKeyBlock() {
        return true;
    }

    /** Whether this PIDS has no platform configured and falls back to the closest platform. */
    public boolean isPlatformAutoDetected() {
        return autoDetectedPlatforms;
    }

    /** Whether the platform number of the arrivals is hidden on this PIDS. */
    public boolean isPlatformNumberHidden() {
        return platformNumberHidden;
    }

    public boolean isRowHidden(int index) {
        return index >= 0 && index < hiddenRows.length && hiddenRows[index];
    }

    public String getCustomMessage(int index) {
        return index >= 0 && index < customMessages.length ? customMessages[index] : "";
    }
}
