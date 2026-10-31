package com.jsblock.scripting.pids;

import mtr.data.Station;

/**
 * Read-only view of an MTR station, so that no MTR type has to cross the script boundary.
 * @author LX86 (JCM)
 */
public class StationWrapper {
    private final long id;
    private final String name;
    private final int color;

    private StationWrapper(long id, String name, int color) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.color = color;
    }

    static StationWrapper of(Station station) {
        return station == null ? null : new StationWrapper(station.id, station.name, station.color);
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
