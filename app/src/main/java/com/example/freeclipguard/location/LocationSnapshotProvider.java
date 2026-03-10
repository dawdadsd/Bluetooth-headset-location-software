package com.example.freeclipguard.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.Nullable;

import com.example.freeclipguard.model.LocationSnapshot;
import com.example.freeclipguard.util.PermissionHelper;

import java.util.List;

public final class LocationSnapshotProvider {

    private LocationSnapshotProvider() {
    }

    @Nullable
    @SuppressLint("MissingPermission")
    public static LocationSnapshot getBestEffortSnapshot(Context context) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return null;
        }
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            return null;
        }
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location candidate = locationManager.getLastKnownLocation(provider);
            if (candidate == null) {
                continue;
            }
            if (bestLocation == null || isBetter(candidate, bestLocation)) {
                bestLocation = candidate;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return new LocationSnapshot(
                bestLocation.getLatitude(),
                bestLocation.getLongitude(),
                bestLocation.getAccuracy(),
                bestLocation.getTime(),
                bestLocation.getProvider()
        );
    }

    private static boolean isBetter(Location candidate, Location currentBest) {
        if (candidate.getTime() > currentBest.getTime() + 30_000L) {
            return true;
        }
        if (candidate.getAccuracy() < currentBest.getAccuracy()) {
            return true;
        }
        return candidate.getTime() > currentBest.getTime();
    }
}
