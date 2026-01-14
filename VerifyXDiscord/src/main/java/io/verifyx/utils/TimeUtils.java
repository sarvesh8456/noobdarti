/*
 * Decompiled with CFR.
 */
package io.verifyx.utils;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {
    public static String formatDuration(long seconds) {
        if (seconds < 60L) {
            return seconds + " second" + (seconds != 1L ? "s" : "");
        }
        if (seconds < 3600L) {
            long minutes = seconds / 60L;
            return minutes + " minute" + (minutes != 1L ? "s" : "");
        }
        long hours = seconds / 3600L;
        return hours + " hour" + (hours != 1L ? "s" : "");
    }

    public static Instant getFutureTime(int minutes) {
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }

    public static boolean isExpired(Instant expiryTime) {
        return Instant.now().isAfter(expiryTime);
    }

    public static long getSecondsUntil(Instant futureTime) {
        return Duration.between(Instant.now(), futureTime).getSeconds();
    }
}

