package com.jsblock.scripting.core.util;

import mtr.MTRClient;
import mtr.data.IGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jsblock.render.RenderPIDSBase.SWITCH_LANGUAGE_TICKS;

/**
 * Text helpers exposed to scripts as {@code TextUtil}.
 * Mirrors the helper of the same name in JCM, so existing PIDS scripts keep working.
 * @author Zbx1425 / LX86 (JCM)
 */
public class TextUtilJS {
    private TextUtilJS() {
    }

    public static String getCjkParts(String src) {
        return getCjkMatching(src, true);
    }

    public static String getNonCjkParts(String src) {
        return getCjkMatching(src, false);
    }

    public static String getExtraParts(String src) {
        return getExtraMatching(src, true);
    }

    public static String getNonExtraParts(String src) {
        return getExtraMatching(src, false);
    }

    public static String getNonCjkAndExtraParts(String src) {
        final String extraParts = getExtraMatching(src, true).trim();
        return getCjkMatching(src, false).trim() + (extraParts.isEmpty() ? "" : "|" + extraParts);
    }

    public static boolean isCjk(String src) {
        return src != null && IGui.isCjk(src);
    }

    /** Pick one of the {@code |}-separated variants based on the game tick. */
    public static String cycleString(String mtrString) {
        return cycleString(mtrString, SWITCH_LANGUAGE_TICKS);
    }

    public static String cycleString(String mtrString, int switchDuration) {
        if (mtrString == null) return "";

        final List<String> split = new ArrayList<>(Arrays.asList(mtrString.split("\\|")));
        if (split.isEmpty()) return "";

        final int duration = switchDuration <= 0 ? SWITCH_LANGUAGE_TICKS : switchDuration;
        return split.get(((int) MTRClient.getGameTick() / duration) % split.size());
    }

    private static String getExtraMatching(String src, boolean extra) {
        if (src == null) return "";
        if (src.contains("||")) {
            return src.split("\\|\\|", 2)[extra ? 1 : 0].trim();
        }
        return extra ? "" : src;
    }

    private static String getCjkMatching(String src, boolean isCjk) {
        if (src == null) return "";
        if (src.contains("||")) src = src.split("\\|\\|", 2)[0];
        final String[] stringSplit = src.split("\\|");
        final StringBuilder result = new StringBuilder();

        for (final String stringSplitPart : stringSplit) {
            if (IGui.isCjk(stringSplitPart) == isCjk) {
                if (result.length() > 0) result.append(' ');
                result.append(stringSplitPart);
            }
        }
        return result.toString().trim();
    }
}
