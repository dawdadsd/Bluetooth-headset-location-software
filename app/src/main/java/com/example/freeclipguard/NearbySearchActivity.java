package com.example.freeclipguard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.util.Formatters;
import com.example.freeclipguard.util.PermissionHelper;

public class NearbySearchActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BoundDeviceStore boundDeviceStore;
    private BoundDevice boundDevice;
    private TextView searchStatusText;
    private TextView savedPlaceText;
    private TextView rssiText;
    private BroadcastReceiver discoveryReceiver;
    private boolean receiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_search);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        boundDeviceStore = new BoundDeviceStore(this);
        boundDevice = boundDeviceStore.getBoundDevice();

        searchStatusText = findViewById(R.id.searchStatusText);
        savedPlaceText = findViewById(R.id.savedPlaceText);
        rssiText = findViewById(R.id.rssiText);
        Button startSearchButton = findViewById(R.id.startSearchButton);
        Button stopSearchButton = findViewById(R.id.stopSearchButton);

        searchStatusText.setText(boundDevice.isConfigured()
                ? "目标设备：" + boundDevice.getName() + " (" + boundDevice.getAddress() + ")"
                : "当前没有绑定设备");
        updateSavedPlaceSummary();
        rssiText.setText("还没有扫描到目标耳机");

        startSearchButton.setOnClickListener(view -> startSearch());
        stopSearchButton.setOnClickListener(view -> stopSearch());

        discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    searchStatusText.setText("正在搜索附近蓝牙设备...");
                    return;
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    searchStatusText.setText("搜索结束。没搜到不代表耳机不在，可能它当前没有广播。可以换房间、开盒再试。");
                    return;
                }
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    handleDeviceFound(intent);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSavedPlaceSummary();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            registerReceiver(discoveryReceiver, filter);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSearch();
        if (receiverRegistered) {
            unregisterReceiver(discoveryReceiver);
            receiverRegistered = false;
        }
    }

    @SuppressLint("MissingPermission")
    private void startSearch() {
        if (!boundDevice.isConfigured()) {
            Toast.makeText(this, "请先绑定耳机", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PermissionHelper.hasBluetoothScanPermission(this)) {
            Toast.makeText(this, "请先授予蓝牙扫描权限", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "请先打开系统蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        boolean success = bluetoothAdapter.startDiscovery();
        searchStatusText.setText(success ? "正在发起搜索..." : "无法开始搜索，请稍后重试");
    }

    @SuppressLint("MissingPermission")
    private void stopSearch() {
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void handleDeviceFound(Intent intent) {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
        if (bluetoothDevice == null || !matchesBoundDevice(bluetoothDevice)) {
            return;
        }
        searchStatusText.setText("找到目标耳机了，试着朝信号变强的方向移动");
        rssiText.setText(Formatters.describeRssi(rssi));
    }

    private void updateSavedPlaceSummary() {
        savedPlaceText.setText(Formatters.formatManualPlaceSummary(
                boundDeviceStore.getManualPlaceNote(),
                boundDeviceStore.getManualPlaceSavedAt()
        ));
    }

    @SuppressLint("MissingPermission")
    private boolean matchesBoundDevice(BluetoothDevice bluetoothDevice) {
        try {
            String candidateAddress = bluetoothDevice.getAddress();
            if (boundDevice.matchesAddress(candidateAddress)) {
                return true;
            }
            String candidateName = bluetoothDevice.getName();
            return candidateName != null && boundDevice.getName().equalsIgnoreCase(candidateName);
        }
        catch (SecurityException ignored) {
            return false;
        }
    }
}
