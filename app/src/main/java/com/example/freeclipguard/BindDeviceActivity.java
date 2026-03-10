package com.example.freeclipguard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.ui.BondedDeviceAdapter;
import com.example.freeclipguard.util.PermissionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BindDeviceActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BoundDeviceStore boundDeviceStore;
    private BondedDeviceAdapter bondedDeviceAdapter;
    private TextView emptyDevicesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_device);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        boundDeviceStore = new BoundDeviceStore(this);

        emptyDevicesText = findViewById(R.id.emptyDevicesText);
        Button refreshBondedDevicesButton = findViewById(R.id.refreshBondedDevicesButton);
        RecyclerView bondedDevicesRecycler = findViewById(R.id.bondedDevicesRecycler);
        bondedDevicesRecycler.setLayoutManager(new LinearLayoutManager(this));

        bondedDeviceAdapter = new BondedDeviceAdapter(this::bindSelectedDevice);
        bondedDevicesRecycler.setAdapter(bondedDeviceAdapter);

        refreshBondedDevicesButton.setOnClickListener(view -> loadBondedDevices());
        loadBondedDevices();
    }

    @SuppressLint("MissingPermission")
    private void loadBondedDevices() {
        if (!PermissionHelper.hasBluetoothConnectPermission(this)) {
            Toast.makeText(this, "请先授予蓝牙连接权限", Toast.LENGTH_SHORT).show();
            emptyDevicesText.setVisibility(View.VISIBLE);
            return;
        }
        if (bluetoothAdapter == null) {
            emptyDevicesText.setText("设备不支持蓝牙");
            emptyDevicesText.setVisibility(View.VISIBLE);
            return;
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> sortedDevices = new ArrayList<>(bondedDevices);
        sortedDevices.sort(Comparator.comparingInt(this::scoreDevice).reversed());
        bondedDeviceAdapter.submitList(sortedDevices);
        emptyDevicesText.setVisibility(sortedDevices.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("MissingPermission")
    private void bindSelectedDevice(BluetoothDevice bluetoothDevice) {
        try {
            boundDeviceStore.saveBoundDevice(bluetoothDevice.getName(), bluetoothDevice.getAddress());
            Toast.makeText(this, "已绑定：" + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
            finish();
        }
        catch (SecurityException exception) {
            Toast.makeText(this, "读取设备信息失败，请检查蓝牙权限", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private int scoreDevice(BluetoothDevice bluetoothDevice) {
        try {
            String name = bluetoothDevice.getName();
            if (name == null) {
                return 0;
            }
            String normalizedName = name.toLowerCase(Locale.getDefault());
            if (normalizedName.contains("freeclip")) {
                return 100;
            }
            if (normalizedName.contains("huawei")) {
                return 80;
            }
        }
        catch (SecurityException ignored) {
            return 0;
        }
        return 10;
    }
}
