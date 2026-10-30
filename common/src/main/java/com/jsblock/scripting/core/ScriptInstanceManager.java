package com.jsblock.scripting.core;

import com.jsblock.scripting.primitive.ScriptInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Keeps track of all live script instances and drops the ones whose owner went away.
 * @author LX86 (JCM)
 */
public class ScriptInstanceManager {
    private final Map<Object, ScriptInstance<?>> instances = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends ScriptInstance<?>> T computeIfAbsent(Object key, Supplier<T> factory) {
        return (T) instances.computeIfAbsent(key, k -> factory.get());
    }

    public ScriptInstance<?> get(Object key) {
        return instances.get(key);
    }

    public void remove(Object key) {
        instances.remove(key);
    }

    /** Remove invalidated instances. @return how many were removed */
    public int tick() {
        final List<Object> toRemove = new ArrayList<>();
        for (final Map.Entry<Object, ScriptInstance<?>> entry : instances.entrySet()) {
            if (entry.getValue().shouldInvalidate()) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(instances::remove);
        return toRemove.size();
    }

    public void reset() {
        instances.clear();
    }

    public int size() {
        return instances.size();
    }
}
