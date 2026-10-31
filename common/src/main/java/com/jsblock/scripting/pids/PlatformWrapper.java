package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.Platform;

/**
 * Read-only view of an MTR platform, so that no MTR type has to cross the script boundary.
 * @author LX86 (JCM)
 */
public class PlatformWrapper {
    private final long id;
    private final String name;
    private final int color;

    private PlatformWrapper(long id, String name, int color) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.color = color;
    }

    static PlatformWrapper of(Platform platform) {
        return platform == null ? null : new PlatformWrapper(platform.id, platform.name, platform.color);
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

    @Override
    public String toString() {
        return name;
    }
}
