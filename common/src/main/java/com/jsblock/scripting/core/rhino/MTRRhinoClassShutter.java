package com.jsblock.scripting.core.rhino;

import com.jsblock.lib.org.mozilla.javascript.ClassShutter;

/**
 * Bridges the scripting class shutter to Rhino.
 * @author LX86 (JCM)
 */
public class MTRRhinoClassShutter implements ClassShutter {
    private final com.jsblock.scripting.api.MTRClassShutter classRestrictor;

    public MTRRhinoClassShutter(com.jsblock.scripting.api.MTRClassShutter classShutter) {
        this.classRestrictor = classShutter;
    }

    @Override
    public boolean visibleToScripts(String fullClassName) {
        return classRestrictor.visibleToScripts(fullClassName);
    }
}
