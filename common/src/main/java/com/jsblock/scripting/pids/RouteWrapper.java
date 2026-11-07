package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only view of an MTR route, so that no MTR type has to cross the script boundary.<br>
 * JCM 2.3 scripts walk a route with {@code getPlatforms()}, which is provided here in addition
 * to the script-style accessors.
 * @author LX86 (JCM)
 */
public class RouteWrapper {
    private final long id;
    private final String name;
    private final String number;
    private final int color;
    private final String circularState;
    private final List<Long> platformIds;

    private RouteWrapper(long id, String name, String number, int color, String circularState, List<Long> platformIds) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.number = number == null ? "" : number;
        this.color = color;
        this.circularState = circularState;
        this.platformIds = platformIds;
    }

    static RouteWrapper of(Route route) {
        if (route == null) return null;
        final String routeNumber = route.isLightRailRoute && route.lightRailRouteNumber != null && !route.lightRailRouteNumber.isEmpty() ? route.lightRailRouteNumber : route.name;
        final Route.CircularState state = route.circularState == null ? Route.CircularState.NONE : route.circularState;
        /* MTR 3.x says ANTICLOCKWISE, JCM 2.x says ANTI_CLOCKWISE; scripts are written against the latter. */
        final String circularState = state == Route.CircularState.ANTICLOCKWISE ? "ANTI_CLOCKWISE" : state.name();
        final List<Long> platformIds = new ArrayList<>();
        if (route.platformIds != null) {
            for (final Route.RoutePlatform routePlatform : route.platformIds) {
                platformIds.add(routePlatform.platformId);
            }
        }
        return new RouteWrapper(route.id, route.name, routeNumber, route.color, circularState, Collections.unmodifiableList(platformIds));
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** The light rail route number when the route is a light rail route, otherwise the route name. */
    public String number() {
        return number;
    }

    public int color() {
        return color;
    }

    public String circularState() {
        return circularState;
    }

    public List<Long> platformIds() {
        return platformIds;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public int getColor() {
        return color;
    }

    public String getCircularState() {
        return circularState;
    }

    public List<Long> getPlatformIds() {
        return platformIds;
    }

    /** The platforms the route stops at, in route order. */
    public List<PlatformWrapper> getPlatforms() {
        final List<PlatformWrapper> platforms = new ArrayList<>(platformIds.size());
        for (final long platformId : platformIds) {
            final PlatformWrapper platform = PlatformWrapper.of(ClientData.DATA_CACHE.platformIdMap.get(platformId));
            if (platform != null) {
                platforms.add(platform);
            }
        }
        return platforms;
    }

    @Override
    public String toString() {
        return name;
    }
}
