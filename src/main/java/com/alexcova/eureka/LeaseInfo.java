package com.alexcova.eureka;

public class LeaseInfo {
    private long renewalIntervalInSecs;
    private long durationInSecs;
    private long registrationTimestamp;
    private long lastRenewalTimestamp;
    private long evictionTimestamp;
    private long serviceUpTimestamp;

    public long getRenewalIntervalInSecs() {
        return renewalIntervalInSecs;
    }

    public LeaseInfo setRenewalIntervalInSecs(long renewalIntervalInSecs) {
        this.renewalIntervalInSecs = renewalIntervalInSecs;
        return this;
    }

    public long getDurationInSecs() {
        return durationInSecs;
    }

    public LeaseInfo setDurationInSecs(long durationInSecs) {
        this.durationInSecs = durationInSecs;
        return this;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public LeaseInfo setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
        return this;
    }

    public long getLastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public LeaseInfo setLastRenewalTimestamp(long lastRenewalTimestamp) {
        this.lastRenewalTimestamp = lastRenewalTimestamp;
        return this;
    }

    public long getEvictionTimestamp() {
        return evictionTimestamp;
    }

    public LeaseInfo setEvictionTimestamp(long evictionTimestamp) {
        this.evictionTimestamp = evictionTimestamp;
        return this;
    }

    public long getServiceUpTimestamp() {
        return serviceUpTimestamp;
    }

    public LeaseInfo setServiceUpTimestamp(long serviceUpTimestamp) {
        this.serviceUpTimestamp = serviceUpTimestamp;
        return this;
    }
}