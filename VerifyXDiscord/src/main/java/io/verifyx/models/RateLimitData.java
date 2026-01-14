/*
 * Decompiled with CFR.
 */
package io.verifyx.models;

public class RateLimitData {
    private int failedAttempts = 0;
    private long lastAttemptTime = System.currentTimeMillis() / 1000L;
    private boolean blocked = false;
    private long blockDuration = 0L;

    public int getFailedAttempts() {
        return this.failedAttempts;
    }

    public long getLastAttemptTime() {
        return this.lastAttemptTime;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public long getBlockDuration() {
        return this.blockDuration;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setLastAttemptTime(long lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setBlockDuration(long blockDuration) {
        this.blockDuration = blockDuration;
    }

    public void incrementAttempts() {
        ++this.failedAttempts;
        this.lastAttemptTime = System.currentTimeMillis() / 1000L;
    }

    public void resetAttempts() {
        this.failedAttempts = 0;
        this.blocked = false;
        this.blockDuration = 0L;
    }

    public void block(long seconds) {
        this.blocked = true;
        this.blockDuration = seconds;
        this.lastAttemptTime = System.currentTimeMillis() / 1000L;
    }

    public boolean isCurrentlyBlocked() {
        if (!this.blocked) {
            return false;
        }
        long elapsed = System.currentTimeMillis() / 1000L - this.lastAttemptTime;
        if (elapsed >= this.blockDuration) {
            this.blocked = false;
            this.blockDuration = 0L;
            return false;
        }
        return true;
    }

    public long getTimeRemaining() {
        if (!this.blocked) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() / 1000L - this.lastAttemptTime;
        long remaining = this.blockDuration - elapsed;
        return Math.max(0L, remaining);
    }
}

