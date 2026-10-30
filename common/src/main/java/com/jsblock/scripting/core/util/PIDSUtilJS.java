package com.jsblock.scripting.core.util;

import java.util.Date;

/**
 * PIDS text helpers exposed to scripts as {@code PIDSUtil}.
 * Mirrors the helper of the same name in JCM, so existing PIDS scripts keep working.
 * @author LX86 (JCM)
 */
public class PIDSUtilJS {
    private PIDSUtilJS() {
    }

    public static String getETAText(double arrival, String arrivedText) {
        return getETAText((long) arrival, arrivedText);
    }

    public static String getETAText(long arrival) {
        return getETAText(arrival, "");
    }

    public static String getETAText(long arrival, String arrivedText) {
        final long willArriveIn = arrival - System.currentTimeMillis();
        final long willArriveInSec = Math.floorDiv(willArriveIn, 1000);

        if (willArriveInSec <= 0) {
            return arrivedText == null ? "" : arrivedText;
        } else if (willArriveIn <= 60000) {
            return willArriveInSec + " 秒|" + willArriveInSec + " sec";
        } else if (willArriveIn <= 120000) {
            return Math.floorDiv(willArriveInSec, 60) + " 分鐘|" + Math.floorDiv(willArriveInSec, 60) + " min";
        } else {
            return Math.floorDiv(willArriveInSec, 60) + " 分鐘|" + Math.floorDiv(willArriveInSec, 60) + " mins";
        }
    }

    public static String formatTime(double inGameTime) {
        return formatTime(inGameTime, false);
    }

    public static String formatTime(double inGameTime, boolean padZero) {
        final double timeNow = inGameTime + 6000;
        final double hrs = (timeNow / 1000) % 24;
        final double mins = (hrs - Math.floor(hrs)) * 60;
        final String hourText = padZero ? zeroPad((int) Math.floor(hrs)) : String.valueOf((int) Math.floor(hrs));
        return hourText + ":" + zeroPad((int) Math.floor(mins % 60));
    }

    public static String getCarText(double len) {
        final int carCount = (int) len;
        return carCount <= 1 ? carCount + "卡|" + carCount + "-car" : carCount + "卡|" + carCount + "-cars";
    }

    public static String formatDateTime(Date date) {
        if (date == null) return "";
        return zeroPad(date.getHours()) + ":" + zeroPad(date.getMinutes());
    }

    private static String zeroPad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
