package com.jsblock.scripting.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Limits which java classes a script is able to reach.
 * When the shutter is disabled every class is visible (except this one).
 * @author LX86 (JCM)
 */
public class MTRClassShutter {
    /**
     * Root packages that Forgix prepends to every class it packs into the merged distribution jar,
     * e.g. {@code com.jsblock.scripting.api.TextWrapper} becomes
     * {@code fabric.com.jsblock.scripting.api.TextWrapper}. Rules are written against the
     * unrelocated names, so both spellings have to match.
     */
    private static final String[] RELOCATION_PREFIXES = {"fabric.", "forge.", "quilt."};

    private boolean shutterEnabled = true;
    private final List<ClassRule> allowedScriptClasses = new ArrayList<>();
    private final List<ClassRule> deniedScriptClasses = new ArrayList<>();

    public MTRClassShutter() {
        allowClass(
                ClassRule.parse("java.awt.*"),
                ClassRule.parse("java.time.*"),
                ClassRule.parse("java.lang.*"),
                ClassRule.parse("java.math.BigDecimal"),
                ClassRule.parse("java.math.BigInteger"),
                ClassRule.parse("java.util.*"),
                ClassRule.parse("java.text.*"),
                ClassRule.parse("javax.imageio.*"),
                ClassRule.parse("sun.java2d.*"),
                ClassRule.parse("sun.font.*"),
                ClassRule.parse("sun.awt.*"),
                ClassRule.parse("java.io.Closeable"),
                ClassRule.parse("java.io.InputStream"),
                ClassRule.parse("java.io.OutputStream"),
                ClassRule.parse("jdk.*"),
                // Scripting core
                ClassRule.parse("com.jsblock.scripting.core.util.*"),
                ClassRule.parse("com.jsblock.scripting.api.*"),
                ClassRule.parse("com.jsblock.scripting.pids.*"),
                ClassRule.parse("com.jsblock.scripting.resource.*"),
                // Rhino JS engine
                ClassRule.parse("com.jsblock.lib.org.mozilla.javascript.*")
        );
        denyClass(
                ClassRule.parse("java.lang.invoke.*"),
                ClassRule.parse("java.lang.reflect.*"),
                ClassRule.parse("java.lang.Process"),
                ClassRule.parse("java.lang.ProcessBuilder"),
                ClassRule.parse("java.lang.Class"),
                ClassRule.parse("java.lang.ClassLoader"),
                ClassRule.parse("java.lang.Shutdown"),
                ClassRule.parse("java.lang.SecurityManager"),
                ClassRule.parse("java.lang.System"),
                ClassRule.parse("java.util.jar.*"),
                ClassRule.parse("java.util.zip.*"),
                ClassRule.parse("jdk.internal.*"),
                ClassRule.parse("jdk.nashorn.*")
        );
    }

    public void setEnabled(boolean shutterEnabled) {
        this.shutterEnabled = shutterEnabled;
    }

    public void allowClass(ClassRule... classRules) {
        allowedScriptClasses.addAll(Arrays.asList(classRules));
    }

    public void denyClass(ClassRule... classRules) {
        deniedScriptClasses.addAll(Arrays.asList(classRules));
    }

    public boolean visibleToScripts(String fullClassName) {
        return !fullClassName.equals(MTRClassShutter.class.getName()) && isClassAllowed(fullClassName);
    }

    public boolean isClassAllowed(String className) {
        if (!shutterEnabled) {
            return true;
        }

        if (className.startsWith("[")) {
            final String type = className.substring(className.lastIndexOf('[') + 1);
            if (type.startsWith("B") || type.startsWith("C") || type.startsWith("D") || type.startsWith("F") || type.startsWith("I") || type.startsWith("J") || type.startsWith("S") || type.startsWith("Z")) {
                return true;
            }
            if (type.startsWith("L")) {
                className = type.substring(1, type.length() - 1);
            }
        }

        for (final ClassRule allowedClassRule : allowedScriptClasses) {
            if (allowedClassRule.match(className) || allowedClassRule.match(stripRelocationPrefix(className))) {
                for (final ClassRule deniedClassRule : deniedScriptClasses) {
                    if (deniedClassRule.match(className) || deniedClassRule.match(stripRelocationPrefix(className))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static String stripRelocationPrefix(String className) {
        for (final String prefix : RELOCATION_PREFIXES) {
            if (className.startsWith(prefix) && className.length() > prefix.length()) {
                return className.substring(prefix.length());
            }
        }
        return className;
    }
}
