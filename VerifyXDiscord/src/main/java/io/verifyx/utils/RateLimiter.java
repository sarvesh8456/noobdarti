/*
 * Decompiled with CFR.
 */
package io.verifyx.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<String, AttemptRecord>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long COOLDOWN_SECONDS = 60L;

    public boolean canAttempt(String identifier) {
        AttemptRecord record = this.attempts.get(identifier);
        if (record == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now < record.cooldownUntil) {
            return false;
        }
        if (now - record.lastAttemptTime > TimeUnit.MINUTES.toMillis(5L)) {
            this.attempts.remove(identifier);
            return true;
        }
        return true;
    }

    public void recordAttempt(String identifier, boolean success) {
        AttemptRecord record = this.attempts.computeIfAbsent(identifier, k -> new AttemptRecord());
        long now = System.currentTimeMillis();
        if (success) {
            this.attempts.remove(identifier);
        } else {
            ++record.attempts;
            record.lastAttemptTime = now;
            if (record.attempts >= 3) {
                record.cooldownUntil = now + TimeUnit.SECONDS.toMillis(60L);
                record.attempts = 0;
            }
        }
    }

    public long getWaitTime(String identifier) {
        AttemptRecord record = this.attempts.get(identifier);
        if (record == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        if (now < record.cooldownUntil) {
            return (record.cooldownUntil - now) / 1000L;
        }
        return 0L;
    }

    public void clearAttempts(String identifier) {
        this.attempts.remove(identifier);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        this.attempts.entrySet().removeIf(entry -> {
            AttemptRecord record = (AttemptRecord)entry.getValue();
            return now - record.lastAttemptTime > TimeUnit.MINUTES.toMillis(30L);
        });
    }

    private static class AttemptRecord {
        int attempts = 0;
        long lastAttemptTime = 0L;
        long cooldownUntil = 0L;

        private AttemptRecord() {
        }
    }
}

