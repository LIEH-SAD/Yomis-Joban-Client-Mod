package com.jsblock.scripting.pids;

import mtr.data.Route;

/**
 * Read-only view of an MTR route, so that no MTR type has to cross the script boundary.
 * @author LX86 (JCM)
 */
public class RouteWrapper {
    private final long id;
    private final String name;
    private final String number;
    private final int color;
    private final String circularState;

    private RouteWrapper(long id, String name, String number, int color, String circularState) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.number = number == null ? "" : number;
        this.color = color;
        this.circularState = circularState;
    }

    static RouteWrapper of(Route route) {
        if (route == null) return null;
        final String routeNumber = route.isLightRailRoute && route.lightRailRouteNumber != null && !route.lightRailRouteNumber.isEmpty() ? route.lightRailRouteNumber : route.name;
        final Route.CircularState state = route.circularState == null ? Route.CircularState.NONE : route.circularState;
        return new RouteWrapper(route.id, route.name, routeNumber, route.color, state.name());
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

    @Override
    public String toString() {
        return name;
    }
}
