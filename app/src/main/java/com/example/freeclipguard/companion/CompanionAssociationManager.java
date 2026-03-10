package com.example.freeclipguard.companion;

import android.app.Activity;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.freeclipguard.R;
import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.model.BoundDevice;

import java.util.regex.Pattern;

public final class CompanionAssociationManager {

    public static final int REQUEST_ASSOCIATION = 9001;

    private CompanionAssociationManager() {
    }

    public interface ResultCallback {
        void onSuccess(String message);

        void onFailure(String message);
    }

    public static boolean isSupported(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)
                && context.getSystemService(CompanionDeviceManager.class) != null;
    }

    public static boolean isPresenceObservationSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static String getStatusText(Context context, BoundDeviceStore boundDeviceStore) {
        if (!isSupported(context)) {
            return context.getString(R.string.status_companion_unsupported);
        }
        if (boundDeviceStore.isCompanionEnabled()) {
            return context.getString(R.string.status_companion_ready);
        }
        return context.getString(R.string.status_companion_disabled);
    }

    public static void associate(Activity activity, BoundDeviceStore boundDeviceStore, ResultCallback resultCallback) {
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.isConfigured()) {
            resultCallback.onFailure(activity.getString(R.string.toast_companion_need_device));
            return;
        }
        CompanionDeviceManager manager = activity.getSystemService(CompanionDeviceManager.class);
        if (manager == null || !isSupported(activity)) {
            resultCallback.onFailure(activity.getString(R.string.status_companion_unsupported));
            return;
        }

        BluetoothDeviceFilter.Builder filterBuilder = new BluetoothDeviceFilter.Builder()
                .setNamePattern(Pattern.compile(".*(FreeClip|HUAWEI).*", Pattern.CASE_INSENSITIVE));
        if (!boundDevice.getAddress().isBlank()) {
            filterBuilder.setAddress(boundDevice.getAddress());
        }

        AssociationRequest request = new AssociationRequest.Builder()
                .addDeviceFilter(filterBuilder.build())
                .setSingleDevice(true)
                .build();

        CompanionDeviceManager.Callback callback = new CompanionDeviceManager.Callback() {
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                launchChooser(activity, chooserLauncher, resultCallback);
            }

            @Override
            public void onFailure(CharSequence error) {
                runOnMain(resultCallback, false, error == null ? activity.getString(R.string.toast_companion_associate_failed) : error.toString());
            }
        };

        manager.associate(request, callback, new Handler(Looper.getMainLooper()));
    }

    public static void startPresenceObservation(Context context, BoundDeviceStore boundDeviceStore, @Nullable ResultCallback callback) {
        if (!isSupported(context) || !isPresenceObservationSupported()) {
            if (callback != null) {
                callback.onFailure(context.getString(R.string.status_companion_unsupported));
            }
            return;
        }
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.isConfigured()) {
            if (callback != null) {
                callback.onFailure(context.getString(R.string.toast_companion_need_device));
            }
            return;
        }
        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);
        if (manager == null) {
            if (callback != null) {
                callback.onFailure(context.getString(R.string.status_companion_unsupported));
            }
            return;
        }
        try {
            manager.startObservingDevicePresence(boundDevice.getAddress());
            boundDeviceStore.setCompanionEnabled(true);
            if (callback != null) {
                callback.onSuccess(context.getString(R.string.toast_companion_observing));
            }
        }
        catch (Exception exception) {
            boundDeviceStore.setCompanionEnabled(false);
            if (callback != null) {
                callback.onFailure(context.getString(R.string.toast_companion_not_ready));
            }
        }
    }

    private static void launchChooser(Activity activity, IntentSender chooserLauncher, ResultCallback callback) {
        try {
            activity.startIntentSenderForResult(chooserLauncher, REQUEST_ASSOCIATION, null, 0, 0, 0);
        }
        catch (IntentSender.SendIntentException exception) {
            runOnMain(callback, false, activity.getString(R.string.toast_companion_chooser_failed));
        }
    }

    private static void runOnMain(ResultCallback callback, boolean success, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (success) {
                callback.onSuccess(message);
            }
            else {
                callback.onFailure(message);
            }
        });
    }
}
