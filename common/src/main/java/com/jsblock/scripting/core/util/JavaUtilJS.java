package com.jsblock.scripting.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers exposed to scripts as {@code Java}.
 * @author LX86 (JCM)
 */
public class JavaUtilJS {

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(List<?> list, Class<T> clazz) {
        final Object[] array = (Object[]) java.lang.reflect.Array.newInstance(clazz, list.size());
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return (T[]) array;
    }

    public List<Object> toList(Object... objs) {
        final List<Object> list = new ArrayList<>();
        java.util.Collections.addAll(list, objs);
        return list;
    }

    public String classOf(Object obj) {
        return obj == null ? "null" : obj.getClass().getName();
    }

    public boolean isNull(Object obj) {
        return obj == null || obj == com.jsblock.lib.org.mozilla.javascript.Undefined.instance;
    }
}
