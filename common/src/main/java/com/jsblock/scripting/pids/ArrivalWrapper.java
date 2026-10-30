package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.ScheduleEntry;

/**
 * One train arrival exposed to PIDS scripts.
 * Adapted to the MTR 3.x data model: MTR 3.x only provides {@code arrivalMillis},
 * {@code trainCars}, {@code routeId} and {@code currentStationIndex} per schedule entry,
 * so realtime information (deviation / departure time) is not available and is reported as neutral values.
 * @author LX86 (JCM)
 */
public class ArrivalWrapper {
    private final ScheduleEntry scheduleEntry;
    private final long platformId;
    private final Route route;

    public ArrivalWrapper(ScheduleEntry scheduleEntry, long platformId) {
        this.scheduleEntry = scheduleEntry;
        this.platformId = platformId;
        this.route = ClientData.DATA_CACHE.routeIdMap.get(scheduleEntry.routeId);
    }

    public String destination() {
        if (route == null) return "";
        try {
            return ClientData.DATA_CACHE.getFormattedRouteDestination(route, scheduleEntry.currentStationIndex, "");
        } catch (Exception e) {
            return "";
        }
    }

    public long arrivalTime() {
        return scheduleEntry.arrivalMillis;
    }

    public boolean arrived() {
        return arrivalTime() <= System.currentTimeMillis();
    }

    /** MTR 3.x does not track realtime deviation; always 0. */
    public long deviation() {
        return 0;
    }

    /** MTR 3.x has no realtime arrivals; schedule data is treated as realtime. */
    public boolean realtime() {
        return true;
    }

    public int stationIndex() {
        return scheduleEntry.currentStationIndex;
    }

    public boolean terminating() {
        return route != null && scheduleEntry.currentStationIndex >= route.platformIds.size() - 1;
    }

    public long routeId() {
        return scheduleEntry.routeId;
    }

    public String routeName() {
        return route == null || route.name == null ? "" : route.name;
    }

    public String routeNumber() {
        if (route == null) return "";
        if (route.isLightRailRoute && route.lightRailRouteNumber != null && !route.lightRailRouteNumber.isEmpty()) {
            return route.lightRailRouteNumber;
        }
        return routeName();
    }

    public int routeColor() {
        return route == null ? 0xFFFFFFFF : route.color;
    }

    /**
     * The circular state of the arrival's route as its enum name ({@code NONE}, {@code CLOCKWISE}
     * or {@code ANTICLOCKWISE}), so that no MTR type has to be exposed to scripts.
     */
    public String circularState() {
        return (route == null ? Route.CircularState.NONE : route.circularState).name();
    }

    public long platformId() {
        return platformId;
    }

    public String platformName() {
        final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        return platform == null || platform.name == null ? "" : platform.name;
    }

    public int carCount() {
        return scheduleEntry.trainCars;
    }

    @Override
    public String toString() {
        return "Arrival[" + routeNumber() + " " + destination() + ", " + carCount() + " cars]";
    }
}
