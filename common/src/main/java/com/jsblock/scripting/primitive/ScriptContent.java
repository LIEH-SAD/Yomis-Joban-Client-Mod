package com.jsblock.scripting.primitive;

import net.minecraft.resources.ResourceLocation;

/**
 * A single unit of script source code together with the location it came from.
 * The location is used as the source name in stack traces.
 * @author LX86 (JCM)
 */
public class ScriptContent {
    private final ResourceLocation location;
    private final String content;

    public ScriptContent(ResourceLocation location, String content) {
        if (location == null) throw new IllegalArgumentException("Script location must not be null!");
        if (content == null) throw new IllegalArgumentException("Script content must not be null!");
        this.location = location;
        this.content = content;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return location.getNamespace() + ":" + location.getPath();
    }
}
