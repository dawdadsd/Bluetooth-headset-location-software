package com.example.freeclipguard.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;

import com.example.freeclipguard.R;
import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.model.BoundDevice;

public final class PlaybackStatusResolver {

    private PlaybackStatusResolver() {
    }

    public static String resolveStatus(Context context, BoundDeviceStore boundDeviceStore) {
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.isConfigured()) {
            return context.getString(R.string.status_playback_unbound);
        }

        boolean audioConnected = boundDeviceStore.isDeviceConnected() || isAnyBluetoothAudioProfileConnected(context);
        AudioManager audioManager = context.getSystemService(AudioManager.class);
        boolean musicActive = audioManager != null && audioManager.isMusicActive();

        if (audioConnected && musicActive) {
            return context.getString(R.string.status_playback_playing, boundDevice.getName());
        }
        if (audioConnected) {
            return context.getString(R.string.status_playback_connected_idle);
        }
        if (musicActive) {
            return context.getString(R.string.status_playback_phone_audio_without_device);
        }
        return context.getString(R.string.status_playback_disconnected);
    }

    public static boolean isAnyBluetoothAudioProfileConnected(Context context) {
        if (!PermissionHelper.hasBluetoothConnectPermission(context)) {
            return false;
        }
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager == null ? BluetoothAdapter.getDefaultAdapter() : bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }
        try {
            return bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                    || bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED;
        }
        catch (SecurityException ignored) {
            return false;
        }
    }
}
