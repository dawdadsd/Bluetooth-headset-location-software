package com.example.freeclipguard.data;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.freeclipguard.location.LocationSnapshotProvider;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.model.LocationSnapshot;
import com.example.freeclipguard.util.DisconnectOverlayManager;
import com.example.freeclipguard.util.NotificationHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class LostEventRepository {

    private static final long DUPLICATE_WINDOW_MS = 45_000L;
    private static final String EVENT_TYPE_CONNECTED = "CONNECTED";
    private static final String EVENT_TYPE_DISCONNECTED = "DISCONNECTED";

    private static volatile LostEventRepository instance;

    private final Context appContext;
    private final AppDatabase appDatabase;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private LostEventRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(appContext);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static LostEventRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (LostEventRepository.class) {
                if (instance == null) {
                    instance = new LostEventRepository(context);
                }
            }
        }
        return instance;
    }

    public void recordDisconnect(BoundDeviceStore boundDeviceStore,
            @Nullable BluetoothDevice bluetoothDevice,
            String source,
            @Nullable Integer rssi,
            @Nullable String note,
            @Nullable Runnable onComplete) {
        recordEvent(boundDeviceStore, bluetoothDevice, source, EVENT_TYPE_DISCONNECTED, rssi, note, true, onComplete);
    }

    public void recordConnect(BoundDeviceStore boundDeviceStore,
            @Nullable BluetoothDevice bluetoothDevice,
            String source,
            @Nullable Integer rssi,
            @Nullable String note,
            @Nullable Runnable onComplete) {
        recordEvent(boundDeviceStore, bluetoothDevice, source, EVENT_TYPE_CONNECTED, rssi, note, false, onComplete);
    }

    private void recordEvent(BoundDeviceStore boundDeviceStore,
            @Nullable BluetoothDevice bluetoothDevice,
            String source,
            String eventType,
            @Nullable Integer rssi,
            @Nullable String note,
            boolean notify,
            @Nullable Runnable onComplete) {
        executorService.execute(() -> {
            BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
            boolean connected = EVENT_TYPE_CONNECTED.equals(eventType);
            if (connected) {
                boundDeviceStore.markConnectedNow();
            }
            else {
                boundDeviceStore.setDeviceConnected(false);
            }
            String disconnectPrompt = boundDeviceStore.getDisconnectAlertPrompt();
            LocationSnapshot snapshot = LocationSnapshotProvider.getFreshSnapshot(appContext);
            boolean atHome = boundDeviceStore.isAtHome(snapshot);

            LostEvent event = new LostEvent();
            event.deviceName = extractName(bluetoothDevice, boundDevice.getName());
            event.deviceAddress = extractAddress(bluetoothDevice, boundDevice.getAddress());
            event.eventTimeMs = System.currentTimeMillis();
            event.latitude = snapshot == null ? null : snapshot.getLatitude();
            event.longitude = snapshot == null ? null : snapshot.getLongitude();
            event.accuracyMeters = snapshot == null ? null : snapshot.getAccuracyMeters();
            event.locationSampleTimeMs = snapshot == null ? null : snapshot.getTimestampMs();
            event.eventSource = source;
            event.eventType = eventType;
            event.rssi = rssi;
            event.atHome = atHome;
            event.note = note;
            if (shouldSkipDuplicate(event)) {
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
                return;
            }
            event.id = appDatabase.lostEventDao().insert(event);

            if (connected) {
                boundDeviceStore.setDisconnectNotified(false);
            } else if (notify && !boundDeviceStore.isDisconnectNotified()) {
                DisconnectOverlayManager.show(appContext, event, disconnectPrompt);
                NotificationHelper.showDisconnectAlert(appContext, event, disconnectPrompt);
                boundDeviceStore.setDisconnectNotified(true);
            }
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void loadLatest(Consumer<LostEvent> consumer) {
        executorService.execute(() -> {
            LostEvent latest = appDatabase.lostEventDao().findLatest();
            mainHandler.post(() -> consumer.accept(latest));
        });
    }

    public void loadRecent(int limit, Consumer<List<LostEvent>> consumer) {
        executorService.execute(() -> {
            List<LostEvent> events = appDatabase.lostEventDao().listRecent(limit);
            mainHandler.post(() -> consumer.accept(events));
        });
    }

    private String extractName(@Nullable BluetoothDevice bluetoothDevice, String fallback) {
        try {
            return bluetoothDevice != null && bluetoothDevice.getName() != null ? bluetoothDevice.getName() : fallback;
        } catch (SecurityException ignored) {
            return fallback;
        }
    }

    private String extractAddress(@Nullable BluetoothDevice bluetoothDevice, String fallback) {
        try {
            return bluetoothDevice != null && bluetoothDevice.getAddress() != null ? bluetoothDevice.getAddress()
                    : fallback;
        } catch (SecurityException ignored) {
            return fallback;
        }
    }

    private boolean shouldSkipDuplicate(LostEvent event) {
        if (event.deviceAddress == null || event.deviceAddress.isBlank()) {
            return false;
        }
        LostEvent latest = appDatabase.lostEventDao().findLatestForDeviceAndType(event.deviceAddress, event.eventType);
        if (latest == null) {
            return false;
        }
        return latest.eventTimeMs >= event.eventTimeMs - DUPLICATE_WINDOW_MS;
    }
}
