package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.Platform;
import mtr.data.Route;
import mtr.data.ScheduleEntry;

import java.util.Collections;
import java.util.List;

/**
 * One train arrival exposed to PIDS scripts.
 * Adapted to the MTR 3.x data model: MTR 3.x only provides {@code arrivalMillis},
 * {@code trainCars}, {@code routeId} and {@code currentStationIndex} per schedule entry,
 * so realtime information (deviation / departure time) is not available and is reported as neutral values.
 * @author LX86 (JCM)
 */
public class ArrivalWrapper {
    /**
     * The stand-in handed out for a row with no train, once a preset has shown it reads past the end
     * of the list without checking. Its arrival time is "now", so a preset computing a countdown
     * reads it as arriving immediately rather than as a train that left hours ago.
     */
    private static final ArrivalWrapper ABSENT = new ArrivalWrapper();

    private final ScheduleEntry scheduleEntry;
    private final long platformId;
    private final Route route;

    public ArrivalWrapper(ScheduleEntry scheduleEntry, long platformId) {
        this.scheduleEntry = scheduleEntry;
        this.platformId = platformId;
        this.route = ClientData.DATA_CACHE.routeIdMap.get(scheduleEntry.routeId);
    }

    private ArrivalWrapper() {
        this.scheduleEntry = null;
        this.platformId = 0;
        this.route = null;
    }

    /** @return the placeholder for an empty row; see {@link ArrivalsWrapper} */
    public static ArrivalWrapper absent() {
        return ABSENT;
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
        return scheduleEntry == null ? System.currentTimeMillis() : scheduleEntry.arrivalMillis;
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
        return scheduleEntry == null ? 0 : scheduleEntry.currentStationIndex;
    }

    /** JCM 2.x's name for {@link #stationIndex()}. */
    public int currentStationIndex() {
        return stationIndex();
    }

    /** The route type as its enum name ({@code NORMAL}, {@code LIGHT_RAIL} or {@code HIGH_SPEED}). */
    public String routeType() {
        return route == null || route.routeType == null ? "NORMAL" : route.routeType.name();
    }

    /** Whether MTR marks the route of this arrival as light rail. */
    public boolean isLightRailRoute() {
        return route != null && route.isLightRailRoute;
    }

    /** Whether the train has already departed, which MTR 3.x does not track; always {@code false}. */
    public boolean departed() {
        return false;
    }

    public boolean terminating() {
        return route != null && stationIndex() >= route.platformIds.size() - 1;
    }

    public long routeId() {
        return scheduleEntry == null ? 0 : scheduleEntry.routeId;
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
     * The circular state of the arrival's route as a string, so that no MTR type has to be exposed to
     * scripts.<br>
     * MTR 3.x spells the anticlockwise case {@code ANTICLOCKWISE} while JCM 2.x uses
     * {@code ANTI_CLOCKWISE}; the JCM 2.x spelling is returned so presets written against it keep
     * matching, and {@code CLOCKWISE} is unchanged.
     */
    public String circularState() {
        if (route == null || route.circularState == null) return "NONE";
        return switch (route.circularState) {
            case CLOCKWISE -> "CLOCKWISE";
            case ANTICLOCKWISE -> "ANTI_CLOCKWISE";
            case NONE -> "NONE";
        };
    }

    public long platformId() {
        return platformId;
    }

    public String platformName() {
        final Platform platform = ClientData.DATA_CACHE.platformIdMap.get(platformId);
        return platform == null || platform.name == null ? "" : platform.name;
    }

    public int carCount() {
        return scheduleEntry == null ? 0 : scheduleEntry.trainCars;
    }

    /**
     * The scheduled departure time of this arrival, in milliseconds since the epoch.
     * MTR 3.x only exposes the arrival time, which is reported here as well.
     */
    public long departureTime() {
        return arrivalTime();
    }

    /** MTR 3.x does not expose a departure index; always -1. */
    public int departureIndex() {
        return -1;
    }

    /** The route of this arrival. */
    public RouteWrapper route() {
        return RouteWrapper.of(route);
    }

    /** The individual cars of this arrival; MTR 3.x does not expose them, so the list is empty. */
    public List<Object> cars() {
        return Collections.emptyList();
    }

    /** The platform this arrival serves. */
    public PlatformWrapper platform() {
        return PlatformWrapper.of(platformId);
    }

    @Override
    public String toString() {
        return "Arrival[" + routeNumber() + " " + destination() + ", " + carCount() + " cars]";
    }
}
