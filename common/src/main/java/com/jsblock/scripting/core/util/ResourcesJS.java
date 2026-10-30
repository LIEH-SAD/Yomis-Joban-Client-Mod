package com.jsblock.scripting.core.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Resource helpers exposed to scripts as the {@code Resources} global.
 * Must be a public top-level class, otherwise Rhino cannot reach its members.
 * @author LX86 (JCM)
 */
public class ResourcesJS {

    public DataReaderJS read(Object id) {
        return ScriptResourceUtil.read(String.valueOf(id));
    }

    public boolean exist(Object id) {
        return ScriptResourceUtil.exist(String.valueOf(id));
    }

    public ResourceLocation id(Object id) {
        return ScriptResourceUtil.id(String.valueOf(id));
    }

    public ResourceLocation idr(Object id) {
        return ScriptResourceUtil.idr(String.valueOf(id));
    }
}
