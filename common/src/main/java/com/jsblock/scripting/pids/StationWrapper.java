package com.jsblock.scripting.pids;

import mtr.data.Station;

/**
 * Read-only view of an MTR station, so that no MTR type has to cross the script boundary.<br>
 * JCM 2.3 hands scripts the raw MTR {@code Station} object, so existing scripts address it with the
 * MTR getter names ({@code getName()}, {@code getColor()}, ...). Those getters are exposed here in
 * addition to the shorter script-style accessors.
 * @author LX86 (JCM)
 */
public class StationWrapper {
    private final long id;
    private final String name;
    private final int color;
    private final int zone;

    private StationWrapper(long id, String name, int color, int zone) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.color = color;
        this.zone = zone;
    }

    static StationWrapper of(Station station) {
        return station == null ? null : new StationWrapper(station.id, station.name, station.color, station.zone);
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

    public int zone() {
        return zone;
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

    public int getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return name;
    }
}
