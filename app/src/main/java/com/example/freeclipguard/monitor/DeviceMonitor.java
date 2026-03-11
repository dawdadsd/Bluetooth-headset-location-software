package com.example.freeclipguard.monitor;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.data.LostEventRepository;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.util.PlaybackStatusResolver;

public final class DeviceMonitor {

    private static final String TAG = "DeviceMonitor";
    private static final long DISCONNECT_DEBOUNCE_MS = 6_000L;

    private static final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static Runnable pendingDisconnectRunnable;

    private DeviceMonitor() {
    }

    public static void handleBluetoothBroadcast(Context context, Intent intent, BroadcastReceiver.PendingResult pendingResult) {
        try {
            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            BoundDeviceStore boundDeviceStore = new BoundDeviceStore(context);
            BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
            if (!boundDevice.isConfigured() || bluetoothDevice == null || !boundDevice.matchesAddress(safeAddress(bluetoothDevice))) {
                pendingResult.finish();
                return;
            }

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                cancelPendingDisconnect();
                LostEventRepository.getInstance(context).recordConnect(
                        boundDeviceStore,
                        bluetoothDevice,
                        "ACL_CONNECTED",
                        null,
                        "系统蓝牙连接建立，已自动记录当前位置",
                        pendingResult::finish
                );
                return;
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                cancelPendingDisconnect();
                Context appContext = context.getApplicationContext();
                pendingDisconnectRunnable = () -> {
                    pendingDisconnectRunnable = null;
                    if (PlaybackStatusResolver.isAnyBluetoothAudioProfileConnected(appContext)) {
                        Log.d(TAG, "Debounce: BT audio profile still connected, skipping disconnect event");
                        pendingResult.finish();
                        return;
                    }
                    LostEventRepository.getInstance(appContext).recordDisconnect(
                            new BoundDeviceStore(appContext),
                            bluetoothDevice,
                            "ACL_DISCONNECTED",
                            null,
                            "系统蓝牙连接断开",
                            pendingResult::finish
                    );
                };
                debounceHandler.postDelayed(pendingDisconnectRunnable, DISCONNECT_DEBOUNCE_MS);
                return;
            }
        }
        catch (Exception exception) {
            Log.e(TAG, "Failed to process bluetooth broadcast", exception);
        }
        pendingResult.finish();
    }

    private static void cancelPendingDisconnect() {
        if (pendingDisconnectRunnable != null) {
            debounceHandler.removeCallbacks(pendingDisconnectRunnable);
            pendingDisconnectRunnable = null;
        }
    }

    private static String safeAddress(BluetoothDevice bluetoothDevice) {
        try {
            return bluetoothDevice.getAddress();
        }
        catch (SecurityException ignored) {
            return "";
        }
    }
}
