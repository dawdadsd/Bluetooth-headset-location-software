package com.example.freeclipguard.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.freeclipguard.monitor.DeviceMonitor;

public class BluetoothStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        DeviceMonitor.handleBluetoothBroadcast(context.getApplicationContext(), intent, pendingResult);
    }
}
