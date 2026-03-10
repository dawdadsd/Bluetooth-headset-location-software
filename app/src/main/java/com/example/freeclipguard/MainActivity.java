package com.example.freeclipguard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.freeclipguard.companion.CompanionAssociationManager;
import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.data.LostEventRepository;
import com.example.freeclipguard.location.LocationSnapshotProvider;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.model.LocationSnapshot;
import com.example.freeclipguard.util.Formatters;
import com.example.freeclipguard.util.PermissionHelper;
import com.example.freeclipguard.util.PlaybackStatusResolver;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final float HOME_RADIUS_METERS = 120F;
    private static final long PLAYBACK_REFRESH_INTERVAL_MS = 3_000L;

    private BoundDeviceStore boundDeviceStore;
    private LostEventRepository lostEventRepository;
    private TextView boundDeviceText;
    private TextView playbackStatusText;
    private TextView manualPlaceSummaryText;
    private TextView homeStatusText;
    private TextView lastEventText;
    private TextView companionStatusText;
    private EditText manualPlaceInput;
    private boolean onboardingAutoLaunched;
    private final Handler playbackStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable playbackStatusRefreshTask = new Runnable() {
        @Override
        public void run() {
            refreshPlaybackStatus();
            playbackStatusHandler.postDelayed(this, PLAYBACK_REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boundDeviceStore = new BoundDeviceStore(this);
        lostEventRepository = LostEventRepository.getInstance(this);

        boundDeviceText = findViewById(R.id.boundDeviceText);
        playbackStatusText = findViewById(R.id.playbackStatusText);
        manualPlaceSummaryText = findViewById(R.id.manualPlaceSummaryText);
        homeStatusText = findViewById(R.id.homeStatusText);
        lastEventText = findViewById(R.id.lastEventText);
        companionStatusText = findViewById(R.id.companionStatusText);
        manualPlaceInput = findViewById(R.id.manualPlaceInput);

        Button bindDeviceButton = findViewById(R.id.bindDeviceButton);
        Button setHomeButton = findViewById(R.id.setHomeButton);
        Button searchNearbyButton = findViewById(R.id.searchNearbyButton);
        Button viewHistoryButton = findViewById(R.id.viewHistoryButton);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button setupWizardButton = findViewById(R.id.setupWizardButton);
        Button alertSettingsButton = findViewById(R.id.alertSettingsButton);
        Button saveManualPlaceButton = findViewById(R.id.saveManualPlaceButton);

        bindDeviceButton.setOnClickListener(view -> startActivity(new Intent(this, BindDeviceActivity.class)));
        setHomeButton.setOnClickListener(view -> setCurrentLocationAsHome());
        searchNearbyButton.setOnClickListener(view -> startNearbySearch());
        viewHistoryButton.setOnClickListener(view -> startActivity(new Intent(this, EventHistoryActivity.class)));
        refreshButton.setOnClickListener(view -> refreshSummary());
        setupWizardButton.setOnClickListener(view -> startActivity(new Intent(this, OnboardingActivity.class)));
        alertSettingsButton.setOnClickListener(view -> startActivity(new Intent(this, AlertSettingsActivity.class)));
        saveManualPlaceButton.setOnClickListener(view -> saveManualPlaceNote());

        manualPlaceInput.setText(boundDeviceStore.getManualPlaceNote());
    }

    @Override
    protected void onStart() {
        super.onStart();
        playbackStatusHandler.post(playbackStatusRefreshTask);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensurePermissions();
        if (boundDeviceStore.isCompanionEnabled()) {
            CompanionAssociationManager.startPresenceObservation(this, boundDeviceStore, null);
        }
        refreshSummary();
        if (!boundDeviceStore.isOnboardingCompleted() && !onboardingAutoLaunched) {
            onboardingAutoLaunched = true;
            startActivity(new Intent(this, OnboardingActivity.class));
        }
    }

    @Override
    protected void onStop() {
        playbackStatusHandler.removeCallbacks(playbackStatusRefreshTask);
        super.onStop();
    }

    private void ensurePermissions() {
        List<String> missingPermissions = PermissionHelper.getMissingPermissions(this);
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void refreshSummary() {
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        boundDeviceText.setText(Formatters.formatDeviceSummary(boundDevice, boundDeviceStore.getLastConnectedAt()));
        refreshPlaybackStatus();
        manualPlaceSummaryText.setText(Formatters.formatManualPlaceSummary(
                boundDeviceStore.getManualPlaceNote(),
                boundDeviceStore.getManualPlaceSavedAt()
        ));
        homeStatusText.setText(Formatters.formatHomeSummary(boundDeviceStore.getHomeLocation()));
        companionStatusText.setText(CompanionAssociationManager.getStatusText(this, boundDeviceStore));
        lostEventRepository.loadLatest(event -> lastEventText.setText(Formatters.formatLastEventSummary(event)));
    }

    private void refreshPlaybackStatus() {
        playbackStatusText.setText(PlaybackStatusResolver.resolveStatus(this, boundDeviceStore));
    }

    private void saveManualPlaceNote() {
        String note = manualPlaceInput.getText() == null ? "" : manualPlaceInput.getText().toString().trim();
        boundDeviceStore.saveManualPlaceNote(note);
        refreshSummary();
        Toast.makeText(this,
                note.isBlank() ? R.string.toast_manual_place_cleared : R.string.toast_manual_place_saved,
                Toast.LENGTH_SHORT).show();
    }

    private void setCurrentLocationAsHome() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            ensurePermissions();
            Toast.makeText(this, R.string.toast_need_location_for_home, Toast.LENGTH_SHORT).show();
            return;
        }
        LocationSnapshot snapshot = LocationSnapshotProvider.getBestEffortSnapshot(this);
        if (snapshot == null) {
            Toast.makeText(this, R.string.toast_need_location_service, Toast.LENGTH_SHORT).show();
            return;
        }
        boundDeviceStore.saveHomeLocation(snapshot, HOME_RADIUS_METERS);
        refreshSummary();
        Toast.makeText(this, R.string.toast_home_saved, Toast.LENGTH_SHORT).show();
    }

    private void startNearbySearch() {
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.isConfigured()) {
            Toast.makeText(this, R.string.toast_bind_before_search, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, NearbySearchActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            refreshSummary();
        }
    }
}
