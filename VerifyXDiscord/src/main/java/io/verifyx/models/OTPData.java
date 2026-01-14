/*
 * Decompiled with CFR.
 */
package io.verifyx.models;

import java.time.Instant;

public class OTPData {
    private final String otp;
    private final String playerName;
    private final String playerUUID;
    private final String ipAddress;
    private final Instant expiresAt;
    private boolean used;
    private Instant usedAt;

    public OTPData(String otp, String playerName, String playerUUID, String ipAddress, Instant expiresAt) {
        this.otp = otp;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public String getOtp() {
        return this.otp;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public boolean isUsed() {
        return this.used;
    }

    public Instant getUsedAt() {
        return this.usedAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }
}

