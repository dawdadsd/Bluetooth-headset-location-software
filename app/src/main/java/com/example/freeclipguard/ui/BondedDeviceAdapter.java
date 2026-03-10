package com.example.freeclipguard.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freeclipguard.R;

import java.util.ArrayList;
import java.util.List;

public final class BondedDeviceAdapter extends RecyclerView.Adapter<BondedDeviceAdapter.BondedDeviceViewHolder> {

    public interface OnBindClickedListener {
        void onBindClicked(BluetoothDevice bluetoothDevice);
    }

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final OnBindClickedListener onBindClickedListener;

    public BondedDeviceAdapter(OnBindClickedListener onBindClickedListener) {
        this.onBindClickedListener = onBindClickedListener;
    }

    public void submitList(List<BluetoothDevice> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BondedDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_bonded_device, parent, false);
        return new BondedDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BondedDeviceViewHolder holder, int position) {
        holder.bind(devices.get(position), onBindClickedListener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static final class BondedDeviceViewHolder extends RecyclerView.ViewHolder {

        private final TextView deviceNameText;
        private final TextView deviceAddressText;
        private final Button bindThisDeviceButton;

        BondedDeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameText = itemView.findViewById(R.id.deviceNameText);
            deviceAddressText = itemView.findViewById(R.id.deviceAddressText);
            bindThisDeviceButton = itemView.findViewById(R.id.bindThisDeviceButton);
        }

        @SuppressLint("MissingPermission")
        void bind(BluetoothDevice bluetoothDevice, OnBindClickedListener listener) {
            String deviceName;
            String deviceAddress;
            try {
                deviceName = bluetoothDevice.getName();
                deviceAddress = bluetoothDevice.getAddress();
            }
            catch (SecurityException ignored) {
                deviceName = "读取名称失败";
                deviceAddress = "读取地址失败";
            }
            deviceNameText.setText(deviceName == null ? "未命名设备" : deviceName);
            deviceAddressText.setText(deviceAddress);
            bindThisDeviceButton.setOnClickListener(view -> listener.onBindClicked(bluetoothDevice));
        }
    }
}
