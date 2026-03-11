package com.example.freeclipguard.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.freeclipguard.model.LocationSnapshot;
import com.example.freeclipguard.util.PermissionHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LocationSnapshotProvider {

    private static final String TAG = "LocationSnapshotProvider";
    private static final long FUSED_TIMEOUT_MS = 8_000L;
    private static final long LEGACY_CURRENT_LOCATION_TIMEOUT_MS = 6_000L;
    private static final long FRESH_LOCATION_AGE_MS = 20_000L;
    private static final float DESIRABLE_ACCURACY_METERS = 60F;

    private LocationSnapshotProvider() {
    }

    @Nullable
    public static LocationSnapshot getFreshSnapshot(Context context) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return null;
        }

        Location fusedLocation = getFusedLocation(context);
        Location legacyLocation = getLegacyBestLocation(context);
        Location chosen = choosePreferredLocation(fusedLocation, legacyLocation);
        return toSnapshot(chosen);
    }

    @Nullable
    @SuppressLint("MissingPermission")
    public static LocationSnapshot getBestEffortSnapshot(Context context) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return null;
        }

        Location fusedLast = getFusedLastLocation(context);
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        Location legacyLast = null;
        if (locationManager != null) {
            List<String> providers = locationManager.getProviders(true);
            if (providers != null) {
                legacyLast = getBestLastKnownLocation(locationManager, providers);
            }
        }
        return toSnapshot(choosePreferredLocation(fusedLast, legacyLast));
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getFusedLocation(Context context) {
        try {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);

            CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(FRESH_LOCATION_AGE_MS)
                    .setDurationMillis(FUSED_TIMEOUT_MS)
                    .build();

            Task<Location> task = client.getCurrentLocation(request, null);
            Location location = Tasks.await(task, FUSED_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (location != null) {
                Log.d(TAG, "Fused location obtained: accuracy=" + location.getAccuracy() + "m");
            }
            return location;
        } catch (Exception e) {
            Log.d(TAG, "Fused location unavailable: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getFusedLastLocation(Context context) {
        try {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
            Task<Location> task = client.getLastLocation();
            return Tasks.await(task, 3_000L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Location getLegacyBestLocation(Context context) {
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            return null;
        }
        List<String> providers = locationManager.getProviders(true);
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        Location currentBest = getCurrentBestLocation(locationManager, providers, LEGACY_CURRENT_LOCATION_TIMEOUT_MS);
        Location lastKnownBest = getBestLastKnownLocation(locationManager, providers);
        return choosePreferredLocation(currentBest, lastKnownBest);
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getCurrentBestLocation(LocationManager locationManager,
            List<String> providers,
            long timeoutMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        List<Location> currentLocations = Collections.synchronizedList(new ArrayList<>());
        List<CancellationSignal> cancellationSignals = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(providers.size());
        for (String provider : providers) {
            CancellationSignal cancellationSignal = new CancellationSignal();
            cancellationSignals.add(cancellationSignal);
            try {
                locationManager.getCurrentLocation(provider, cancellationSignal, Runnable::run, location -> {
                    if (location != null) {
                        currentLocations.add(location);
                    }
                    countDownLatch.countDown();
                });
            }
            catch (Exception ignored) {
                countDownLatch.countDown();
            }
        }
        try {
            countDownLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }
        catch (Exception ignored) {
        }
        finally {
            for (CancellationSignal cancellationSignal : cancellationSignals) {
                cancellationSignal.cancel();
            }
        }
        return selectBestLocation(currentLocations);
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getBestLastKnownLocation(LocationManager locationManager, List<String> providers) {
        List<Location> locations = new ArrayList<>();
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null) {
                    locations.add(candidate);
                }
            }
            catch (Exception ignored) {
            }
        }
        return selectBestLocation(locations);
    }

    @Nullable
    private static Location choosePreferredLocation(@Nullable Location primary, @Nullable Location secondary) {
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }

        boolean primaryIsFreshAndAccurate = isFresh(primary) && primary.getAccuracy() <= DESIRABLE_ACCURACY_METERS;
        if (primaryIsFreshAndAccurate) {
            return primary;
        }

        return scoreLocation(primary) >= scoreLocation(secondary)
                ? primary
                : secondary;
    }

    @Nullable
    private static Location selectBestLocation(List<Location> locations) {
        Location bestLocation = null;
        for (Location candidate : locations) {
            if (candidate == null) {
                continue;
            }
            if (bestLocation == null || scoreLocation(candidate) > scoreLocation(bestLocation)) {
                bestLocation = candidate;
            }
        }
        return bestLocation;
    }

    private static int scoreLocation(Location location) {
        long ageMs = Math.max(0L, System.currentTimeMillis() - location.getTime());
        int score = 0;

        score -= Math.round(Math.min(location.getAccuracy(), 500F) * 2F);
        score -= (int) Math.min(ageMs / 1000L, 600L);

        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            score += 35;
        }
        else if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            score += 10;
        }
        else if ("fused".equals(location.getProvider())) {
            score += 40;
        }

        if (ageMs <= FRESH_LOCATION_AGE_MS) {
            score += 25;
        }
        if (location.getAccuracy() <= 20F) {
            score += 30;
        }
        else if (location.getAccuracy() <= DESIRABLE_ACCURACY_METERS) {
            score += 15;
        }

        return score;
    }

    private static boolean isFresh(Location location) {
        return System.currentTimeMillis() - location.getTime() <= FRESH_LOCATION_AGE_MS;
    }

    @Nullable
    private static LocationSnapshot toSnapshot(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return new LocationSnapshot(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getTime(),
                location.getProvider()
        );
    }
}
