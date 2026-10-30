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

    public ArrivalsWrapper(List<ArrivalWrapper> arrivals) {
        this.arrivals = Collections.unmodifiableList(new ArrayList<>(arrivals));
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

    /** @return the i-th upcoming arrival, or {@code null} when out of range. */
    public ArrivalWrapper get(int i) {
        return i < 0 || i >= arrivals.size() ? null : arrivals.get(i);
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

    public List<Long> platformIds() {
        final List<Long> ids = new ArrayList<>();
        for (final ArrivalWrapper arrival : arrivals) {
            if (!ids.contains(arrival.platformId())) ids.add(arrival.platformId());
        }
        return ids;
    }

    @Override
    public String toString() {
        return "Arrivals[" + arrivals.size() + "]";
    }
}
