package com.jsblock.scripting.api;

/**
 * A single allow/deny rule for the script class shutter.
 * @author LX86 (JCM)
 */
public class ClassRule {
    private final String match;
    private final boolean isWildcard;

    public ClassRule(String match, boolean isWildcard) {
        this.match = match;
        this.isWildcard = isWildcard;
    }

    public static ClassRule parse(String str) {
        if (str.endsWith(".*")) {
            return new ClassRule(str.substring(0, str.length() - 2), true);
        }
        return new ClassRule(str, false);
    }

    public boolean match(String className) {
        return isWildcard ? className.startsWith(match) : className.equals(match);
    }
}
