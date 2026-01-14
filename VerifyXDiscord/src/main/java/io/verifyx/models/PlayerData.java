/*
 * Decompiled with CFR.
 */
package io.verifyx.models;

import java.time.Instant;

public class PlayerData {
    private String playerName;
    private String playerUUID;
    private String discordId;
    private String discordName;
    private Instant verifiedAt;
    private Instant lastJoin;
    private int totalJoins;

    public PlayerData(String playerName, String playerUUID) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.verifiedAt = Instant.now();
        this.lastJoin = Instant.now();
        this.totalJoins = 1;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }

    public String getDiscordId() {
        return this.discordId;
    }

    public String getDiscordName() {
        return this.discordName;
    }

    public Instant getVerifiedAt() {
        return this.verifiedAt;
    }

    public Instant getLastJoin() {
        return this.lastJoin;
    }

    public int getTotalJoins() {
        return this.totalJoins;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public void setLastJoin(Instant lastJoin) {
        this.lastJoin = lastJoin;
    }

    public void setTotalJoins(int totalJoins) {
        this.totalJoins = totalJoins;
    }

    public void recordJoin() {
        this.lastJoin = Instant.now();
        ++this.totalJoins;
    }
}

