package com.example.freeclipguard;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.freeclipguard.companion.CompanionAssociationManager;
import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.util.PermissionHelper;

import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private static final int REQUEST_BASIC_PERMISSIONS = 201;
    private static final int REQUEST_BACKGROUND_LOCATION = 202;

    private BoundDeviceStore boundDeviceStore;
    private TextView basicPermissionStatusText;
    private TextView backgroundPermissionStatusText;
    private TextView deviceBindingStatusText;
    private TextView companionStatusWizardText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        boundDeviceStore = new BoundDeviceStore(this);
        basicPermissionStatusText = findViewById(R.id.basicPermissionStatusText);
        backgroundPermissionStatusText = findViewById(R.id.backgroundPermissionStatusText);
        deviceBindingStatusText = findViewById(R.id.deviceBindingStatusText);
        companionStatusWizardText = findViewById(R.id.companionStatusWizardText);

        Button requestBasicPermissionsButton = findViewById(R.id.requestBasicPermissionsButton);
        Button openLocationSettingsButton = findViewById(R.id.openLocationSettingsButton);
        Button openBindDeviceButton = findViewById(R.id.openBindDeviceButton);
        Button enableCompanionButton = findViewById(R.id.enableCompanionButton);
        Button finishOnboardingButton = findViewById(R.id.finishOnboardingButton);

        requestBasicPermissionsButton.setOnClickListener(view -> requestBasicPermissions());
        openLocationSettingsButton.setOnClickListener(view -> handleBackgroundLocationStep());
        openBindDeviceButton.setOnClickListener(view -> startActivity(new Intent(this, BindDeviceActivity.class)));
        enableCompanionButton.setOnClickListener(view -> enableCompanionEnhancement());
        finishOnboardingButton.setOnClickListener(view -> finishOnboarding());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CompanionAssociationManager.REQUEST_ASSOCIATION) {
            return;
        }
        if (resultCode == RESULT_OK && !boundDeviceStore.isCompanionEnabled()) {
            CompanionAssociationManager.startPresenceObservation(this, boundDeviceStore,
                    new CompanionAssociationManager.ResultCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(OnboardingActivity.this, message, Toast.LENGTH_SHORT).show();
                            refreshStatus();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(OnboardingActivity.this, message, Toast.LENGTH_SHORT).show();
                            refreshStatus();
                        }
                    });
            return;
        }
        refreshStatus();
    }

    private void requestBasicPermissions() {
        List<String> missingPermissions = PermissionHelper.getMissingPermissions(this);
        if (missingPermissions.isEmpty()) {
            Toast.makeText(this, R.string.status_permissions_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), REQUEST_BASIC_PERMISSIONS);
    }

    private void handleBackgroundLocationStep() {
        if (PermissionHelper.hasBackgroundLocationPermission(this)) {
            Toast.makeText(this, R.string.toast_background_already_granted, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, R.string.toast_background_not_needed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && PermissionHelper.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_BACKGROUND_LOCATION);
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        Toast.makeText(this, R.string.toast_background_settings_opened, Toast.LENGTH_LONG).show();
    }

    private void enableCompanionEnhancement() {
        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        if (!boundDevice.isConfigured()) {
            Toast.makeText(this, R.string.toast_companion_need_device, Toast.LENGTH_SHORT).show();
            return;
        }
        CompanionAssociationManager.associate(this, boundDeviceStore, new CompanionAssociationManager.ResultCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(OnboardingActivity.this, message, Toast.LENGTH_SHORT).show();
                refreshStatus();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(OnboardingActivity.this, message, Toast.LENGTH_SHORT).show();
                refreshStatus();
            }
        });
    }

    private void finishOnboarding() {
        boundDeviceStore.setOnboardingCompleted(true);
        Toast.makeText(this, R.string.toast_setup_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void refreshStatus() {
        basicPermissionStatusText.setText(PermissionHelper.getMissingPermissions(this).isEmpty()
                ? getString(R.string.status_permissions_ready)
                : getString(R.string.status_permissions_missing));

        backgroundPermissionStatusText.setText(PermissionHelper.hasBackgroundLocationPermission(this)
                ? getString(R.string.status_background_ready)
                : getString(R.string.status_background_missing));

        BoundDevice boundDevice = boundDeviceStore.getBoundDevice();
        deviceBindingStatusText.setText(boundDevice.isConfigured()
                ? getString(R.string.status_device_bound_prefix) + boundDevice.getName() + " (" + boundDevice.getAddress() + ")"
                : getString(R.string.status_device_missing));

        companionStatusWizardText.setText(CompanionAssociationManager.getStatusText(this, boundDeviceStore));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }
}
