package com.example.freeclipguard.model;

public final class HomeLocation {

    private final double latitude;
    private final double longitude;
    private final float radiusMeters;

    public HomeLocation(double latitude, double longitude, float radiusMeters) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getRadiusMeters() {
        return radiusMeters;
    }
}
