package com.example.freeclipguard.companion;

import android.companion.CompanionDeviceService;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.data.LostEventRepository;
import com.example.freeclipguard.model.BoundDevice;

public class FreeClipCompanionService extends CompanionDeviceService {

    @Override
    public void onDeviceAppeared(String address) {
        BoundDeviceStore boundDeviceStore = new BoundDeviceStore(this);
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (boundDevice.matchesAddress(address)) {
            boundDeviceStore.markConnectedNow();
        }
    }

    @Override
    public void onDeviceDisappeared(String address) {
        BoundDeviceStore boundDeviceStore = new BoundDeviceStore(this);
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.matchesAddress(address)) {
            return;
        }
        LostEventRepository.getInstance(this).recordDisconnect(
                boundDeviceStore,
                null,
                "COMPANION_DISAPPEARED",
                null,
                "伴生设备服务检测到耳机离开范围",
                null
        );
    }
}
