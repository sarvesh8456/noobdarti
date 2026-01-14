/*
 * Decompiled with CFR.
 */
package io.verifyx.models;

public class VerificationResult {
    private final boolean success;
    private final String message;
    private final String playerName;
    private final String playerUUID;

    public VerificationResult(boolean success, String message) {
        this(success, message, null, null);
    }

    public VerificationResult(boolean success, String message, String playerName, String playerUUID) {
        this.success = success;
        this.message = message;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }
}

