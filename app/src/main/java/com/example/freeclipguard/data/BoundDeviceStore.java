package com.example.freeclipguard.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import androidx.annotation.Nullable;

import com.example.freeclipguard.R;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.model.HomeLocation;
import com.example.freeclipguard.model.LocationSnapshot;

public final class BoundDeviceStore {

    private static final String PREFS_NAME = "freeclip_guard_prefs";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_HOME_LATITUDE = "home_latitude";
    private static final String KEY_HOME_LONGITUDE = "home_longitude";
    private static final String KEY_HOME_RADIUS = "home_radius";
    private static final String KEY_HOME_ENABLED = "home_enabled";
    private static final String KEY_LAST_CONNECTED_AT = "last_connected_at";
    private static final String KEY_INTRO_COMPLETED = "intro_completed";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_COMPANION_ENABLED = "companion_enabled";
    private static final String KEY_DEVICE_CONNECTED = "device_connected";
    private static final String KEY_DISCONNECT_ALERT_PROMPT = "disconnect_alert_prompt";
    private static final String KEY_SAVED_PLACE_NOTE = "saved_place_note";
    private static final String KEY_SAVED_PLACE_AT = "saved_place_at";
    private static final String KEY_DISCONNECT_NOTIFIED = "disconnect_notified";

    private final Context appContext;
    private final SharedPreferences preferences;

    public BoundDeviceStore(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveBoundDevice(String name, String address) {
        String existingAddress = preferences.getString(KEY_DEVICE_ADDRESS, "");
        preferences.edit()
                .putString(KEY_DEVICE_NAME, name)
                .putString(KEY_DEVICE_ADDRESS, address)
                .putBoolean(KEY_DEVICE_CONNECTED, false)
                .putBoolean(KEY_COMPANION_ENABLED, existingAddress != null && existingAddress.equalsIgnoreCase(address)
                        ? preferences.getBoolean(KEY_COMPANION_ENABLED, false)
                        : false)
                .apply();
    }

    public BoundDevice getBoundDevice() {
        return new BoundDevice(
                preferences.getString(KEY_DEVICE_NAME, ""),
                preferences.getString(KEY_DEVICE_ADDRESS, ""));
    }

    public void saveHomeLocation(LocationSnapshot snapshot, float radiusMeters) {
        preferences.edit()
                .putBoolean(KEY_HOME_ENABLED, true)
                .putString(KEY_HOME_LATITUDE, Double.toString(snapshot.getLatitude()))
                .putString(KEY_HOME_LONGITUDE, Double.toString(snapshot.getLongitude()))
                .putFloat(KEY_HOME_RADIUS, radiusMeters)
                .apply();
    }

    @Nullable
    public HomeLocation getHomeLocation() {
        if (!preferences.getBoolean(KEY_HOME_ENABLED, false)) {
            return null;
        }
        String latitude = preferences.getString(KEY_HOME_LATITUDE, null);
        String longitude = preferences.getString(KEY_HOME_LONGITUDE, null);
        if (latitude == null || longitude == null) {
            return null;
        }
        try {
            return new HomeLocation(
                    Double.parseDouble(latitude),
                    Double.parseDouble(longitude),
                    preferences.getFloat(KEY_HOME_RADIUS, 120F));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public boolean isAtHome(@Nullable LocationSnapshot snapshot) {
        HomeLocation homeLocation = getHomeLocation();
        if (homeLocation == null || snapshot == null) {
            return false;
        }
        float[] results = new float[1];
        Location.distanceBetween(
                homeLocation.getLatitude(),
                homeLocation.getLongitude(),
                snapshot.getLatitude(),
                snapshot.getLongitude(),
                results);
        return results[0] <= homeLocation.getRadiusMeters();
    }

    public void markConnectedNow() {
        preferences.edit()
                .putLong(KEY_LAST_CONNECTED_AT, System.currentTimeMillis())
                .putBoolean(KEY_DEVICE_CONNECTED, true)
                .apply();
    }

    public long getLastConnectedAt() {
        return preferences.getLong(KEY_LAST_CONNECTED_AT, 0L);
    }

    public void setDeviceConnected(boolean connected) {
        preferences.edit().putBoolean(KEY_DEVICE_CONNECTED, connected).apply();
    }

    public boolean isDeviceConnected() {
        return preferences.getBoolean(KEY_DEVICE_CONNECTED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public void setIntroCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_INTRO_COMPLETED, completed).apply();
    }

    public boolean isIntroCompleted() {
        return preferences.getBoolean(KEY_INTRO_COMPLETED, false);
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setCompanionEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_COMPANION_ENABLED, enabled).apply();
    }

    public boolean isCompanionEnabled() {
        return preferences.getBoolean(KEY_COMPANION_ENABLED, false);
    }

    public void saveDisconnectAlertPrompt(String prompt) {
        preferences.edit().putString(KEY_DISCONNECT_ALERT_PROMPT, prompt == null ? "" : prompt.trim()).apply();
    }

    public String getDisconnectAlertPrompt() {
        String prompt = preferences.getString(KEY_DISCONNECT_ALERT_PROMPT, "");
        if (prompt == null || prompt.isBlank()) {
            return appContext.getString(R.string.default_disconnect_prompt);
        }
        return prompt;
    }

    public void saveManualPlaceNote(String note) {
        String trimmedNote = note == null ? "" : note.trim();
        preferences.edit()
                .putString(KEY_SAVED_PLACE_NOTE, trimmedNote)
                .putLong(KEY_SAVED_PLACE_AT, trimmedNote.isBlank() ? 0L : System.currentTimeMillis())
                .apply();
    }

    public String getManualPlaceNote() {
        return preferences.getString(KEY_SAVED_PLACE_NOTE, "");
    }

    public long getManualPlaceSavedAt() {
        return preferences.getLong(KEY_SAVED_PLACE_AT, 0L);
    }

    public void setDisconnectNotified(boolean notified) {
        preferences.edit().putBoolean(KEY_DISCONNECT_NOTIFIED, notified).apply();
    }

    public boolean isDisconnectNotified() {
        return preferences.getBoolean(KEY_DISCONNECT_NOTIFIED, false);
    }
}
