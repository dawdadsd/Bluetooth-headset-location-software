package com.example.freeclipguard;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freeclipguard.data.BoundDeviceStore;
import com.example.freeclipguard.util.PermissionHelper;

public class AlertSettingsActivity extends AppCompatActivity {

    private BoundDeviceStore boundDeviceStore;
    private EditText disconnectPromptInput;
    private TextView overlayStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_settings);

        boundDeviceStore = new BoundDeviceStore(this);
        disconnectPromptInput = findViewById(R.id.disconnectPromptInput);
        overlayStatusText = findViewById(R.id.overlayStatusText);

        Button saveButton = findViewById(R.id.saveAlertSettingsButton);
        Button overlayPermissionButton = findViewById(R.id.overlayPermissionButton);

        disconnectPromptInput.setText(boundDeviceStore.getDisconnectAlertPrompt());

        saveButton.setOnClickListener(view -> saveSettings());
        overlayPermissionButton.setOnClickListener(view -> openOverlayPermissionSettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlayStatusText.setText(PermissionHelper.hasOverlayPermission(this)
                ? R.string.alert_settings_overlay_enabled
                : R.string.alert_settings_overlay_disabled);
    }

    private void saveSettings() {
        boundDeviceStore.saveDisconnectAlertPrompt(disconnectPromptInput.getText() == null
                ? ""
                : disconnectPromptInput.getText().toString());
        Toast.makeText(this, R.string.toast_alert_settings_saved, Toast.LENGTH_SHORT).show();
    }

    private void openOverlayPermissionSettings() {
        if (PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, R.string.toast_overlay_permission_already_granted, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, R.string.toast_overlay_permission_already_granted, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        Toast.makeText(this, R.string.toast_overlay_permission_opened, Toast.LENGTH_LONG).show();
    }
}
