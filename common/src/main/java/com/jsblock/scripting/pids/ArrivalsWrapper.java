package com.jsblock.scripting.pids;

import mtr.client.ClientData;
import mtr.data.RailwayData;
import mtr.data.ScheduleEntry;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The list of upcoming arrivals for one PIDS, exposed to scripts as {@code pids.arrivals()}.
 * Built from the MTR 3.x client schedule cache.
 * @author LX86 (JCM)
 */
public class ArrivalsWrapper {
    private final List<ArrivalWrapper> arrivals;
    private final boolean lenient;

    public ArrivalsWrapper(List<ArrivalWrapper> arrivals) {
        this(arrivals, false);
    }

    private ArrivalsWrapper(List<ArrivalWrapper> arrivals, boolean lenient) {
        this.arrivals = Collections.unmodifiableList(new ArrayList<>(arrivals));
        this.lenient = lenient;
    }

    /**
     * @return the same list with {@link #get(int)} handing out a placeholder past the end instead of
     * {@code null}; {@code this} when already lenient
     */
    public ArrivalsWrapper withLenient() {
        return lenient ? this : new ArrivalsWrapper(arrivals, true);
    }

    /** Whether {@link #get(int)} answers with a placeholder rather than {@code null} past the end. */
    public boolean isLenient() {
        return lenient;
    }

    /**
     * Collect (and sort by arrival time) the upcoming arrivals of the given platforms.
     * When no platform is configured the closest platform to {@code pos} is used, matching vanilla PIDS behaviour.
     */
    public static ArrivalsWrapper fromPlatformIds(List<Long> platformIds, BlockPos pos) {
        final List<Long> resolvedIds = new ArrayList<>();
        if (platformIds != null && !platformIds.isEmpty()) {
            resolvedIds.addAll(platformIds);
        } else if (pos != null) {
            try {
                resolvedIds.add(RailwayData.getClosePlatformId(ClientData.PLATFORMS, ClientData.DATA_CACHE, pos));
            } catch (Exception ignored) {
                // No platform nearby
            }
        }

        final List<ArrivalWrapper> result = new ArrayList<>();
        for (final long platformId : resolvedIds) {
            final Set<ScheduleEntry> schedules = ClientData.SCHEDULES_FOR_PLATFORM.get(platformId);
            if (schedules == null) continue;
            for (final ScheduleEntry scheduleEntry : schedules) {
                result.add(new ArrivalWrapper(scheduleEntry, platformId));
            }
        }

        result.sort((a, b) -> Long.compare(a.arrivalTime(), b.arrivalTime()));
        return new ArrivalsWrapper(result);
    }

    /**
     * @return the i-th upcoming arrival, or {@code null} when out of range -- unless this list is
     * lenient, in which case an empty arrival is returned so a preset that indexes past the end
     * without checking still draws instead of throwing (see
     * {@link com.jsblock.scripting.primitive.ParsedScript#usesLenientArrivals()})
     */
    public ArrivalWrapper get(int i) {
        if (i >= 0 && i < arrivals.size()) return arrivals.get(i);
        return lenient ? ArrivalWrapper.absent() : null;
    }

    public int size() {
        return arrivals.size();
    }

    public boolean isEmpty() {
        return arrivals.isEmpty();
    }

    /** Whether the upcoming trains do not all have the same amount of cars. */
    public boolean mixedCarLength() {
        if (arrivals.isEmpty()) return false;
        final int firstCarCount = arrivals.get(0).carCount();
        for (final ArrivalWrapper arrival : arrivals) {
            if (arrival.carCount() != firstCarCount) return true;
        }
        return false;
    }

    public List<ArrivalWrapper> getAll() {
        return arrivals;
    }

    /**
     * The arrivals as a JavaScript array, for the {@code arrivals.toArray()} call JCM 2.x documents.<br>
     * A Java list has no {@code .length} and no {@code .map}, so the array is built inside the calling
     * scope when one is available and falls back to a Java array otherwise.
     */
    public Object toArray() {
        final Object[] items = arrivals.toArray();
        try {
            final com.jsblock.lib.org.mozilla.javascript.Context cx = com.jsblock.lib.org.mozilla.javascript.Context.getCurrentContext();
            if (cx != null) {
                final com.jsblock.lib.org.mozilla.javascript.Scriptable scope = com.jsblock.lib.org.mozilla.javascript.ScriptRuntime.getTopCallScope(cx);
                final com.jsblock.lib.org.mozilla.javascript.NativeArray array =
                        (com.jsblock.lib.org.mozilla.javascript.NativeArray) cx.newArray(scope, items.length);
                for (int i = 0; i < items.length; i++) {
                    array.put(i, array, items[i]);
                }
                return array;
            }
        } catch (Throwable ignored) {
            /* Not on a script call stack: hand back the plain array, which scripts can still index. */
        }
        return items;
    }

    public List<Long> platformIds() {
        final List<Long> ids = new ArrayList<>();
        for (final ArrivalWrapper arrival : arrivals) {
            if (!ids.contains(arrival.platformId())) ids.add(arrival.platformId());
        }
        return ids;
    }

    /** The platforms this PIDS serves, with their name and colour. */
    public List<PlatformWrapper> platforms() {
        final List<PlatformWrapper> platforms = new ArrayList<>();
        for (final ArrivalWrapper arrival : arrivals) {
            final long platformId = arrival.platformId();
            boolean seen = false;
            for (final PlatformWrapper platform : platforms) {
                if (platform.id() == platformId) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                final PlatformWrapper platform = PlatformWrapper.of(platformId);
                if (platform != null) platforms.add(platform);
            }
        }
        return platforms;
    }

    @Override
    public String toString() {
        return "Arrivals[" + arrivals.size() + "]";
    }
}
