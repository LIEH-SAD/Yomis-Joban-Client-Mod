package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Station;

/**
 * Read-only view of an MTR platform, so that no MTR type has to cross the script boundary.<br>
 * Besides the script-style accessors, the MTR getter names used by JCM 2.3 scripts
 * ({@code getName()}, {@code getStationName()}, ...) are provided as well.
 * @author LX86 (JCM)
 */
public class PlatformWrapper {
    private final long id;
    private final String name;
    private final int color;
    private final String stationName;

    private PlatformWrapper(long id, String name, int color, String stationName) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.color = color;
        this.stationName = stationName == null ? "" : stationName;
    }

    static PlatformWrapper of(Platform platform) {
        if (platform == null) return null;
        final Station station = ClientData.DATA_CACHE.platformIdToStation.get(platform.id);
        return new PlatformWrapper(platform.id, platform.name, platform.color, station == null ? platform.name : station.name);
    }

    static PlatformWrapper of(long platformId) {
        return of(ClientData.DATA_CACHE.platformIdMap.get(platformId));
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int color() {
        return color;
    }

    /** The name of the station this platform belongs to, falling back to the platform name. */
    public String stationName() {
        return stationName;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public String getStationName() {
        return stationName;
    }

    @Override
    public String toString() {
        return name;
    }
}
