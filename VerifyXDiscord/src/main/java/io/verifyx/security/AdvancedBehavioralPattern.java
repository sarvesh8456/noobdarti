/*
 * Decompiled with CFR.
 */
package io.verifyx.security;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class AdvancedBehavioralPattern
implements Serializable {
    private String patternName;
    private double riskScore;
    private String description;
    private final AtomicLong lastSeen = new AtomicLong(System.currentTimeMillis());

    public AdvancedBehavioralPattern(String patternName, double riskScore, String description) {
        this.patternName = patternName;
        this.riskScore = riskScore;
        this.description = description;
    }

    public String getPatternName() {
        return this.patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public double getRiskScore() {
        return this.riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getLastSeen() {
        return this.lastSeen.get();
    }

    public void updateLastSeen() {
        this.lastSeen.set(System.currentTimeMillis());
    }

    public String toString() {
        return String.format("Pattern{name='%s', risk=%.1f, desc='%s'}", this.patternName, this.riskScore, this.description);
    }
}

