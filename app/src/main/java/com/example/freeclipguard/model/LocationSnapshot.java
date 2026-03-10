package com.example.freeclipguard.model;

public final class LocationSnapshot {

    private final double latitude;
    private final double longitude;
    private final float accuracyMeters;
    private final long timestampMs;
    private final String provider;

    public LocationSnapshot(double latitude, double longitude, float accuracyMeters, long timestampMs, String provider) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.timestampMs = timestampMs;
        this.provider = provider == null ? "unknown" : provider;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getAccuracyMeters() {
        return accuracyMeters;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public String getProvider() {
        return provider;
    }
}
