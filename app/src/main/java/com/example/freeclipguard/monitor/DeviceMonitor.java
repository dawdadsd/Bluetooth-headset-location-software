package com.example.freeclipguard.monitor;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.data.LostEventRepository;
import com.example.freeclipguard.model.BoundDevice;

public final class DeviceMonitor {

    private static final String TAG = "DeviceMonitor";

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
                boundDeviceStore.markConnectedNow();
                pendingResult.finish();
                return;
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                LostEventRepository.getInstance(context).recordDisconnect(
                        boundDeviceStore,
                        bluetoothDevice,
                        "ACL_DISCONNECTED",
                        null,
                        "系统蓝牙连接断开",
                        pendingResult::finish
                );
                return;
            }
        }
        catch (Exception exception) {
            Log.e(TAG, "Failed to process bluetooth broadcast", exception);
        }
        pendingResult.finish();
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
