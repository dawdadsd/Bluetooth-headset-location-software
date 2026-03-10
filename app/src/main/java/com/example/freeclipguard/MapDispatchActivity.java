package com.example.freeclipguard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.freeclipguard.data.LostEvent;
import com.example.freeclipguard.util.MapIntentHelper;

public class MapDispatchActivity extends AppCompatActivity {

    private static final String EXTRA_LATITUDE = "extra_latitude";
    private static final String EXTRA_LONGITUDE = "extra_longitude";

    public static Intent createIntent(Context context, LostEvent event) {
        Intent intent = new Intent(context, MapDispatchActivity.class);
        intent.putExtra(EXTRA_LATITUDE, event.latitude);
        intent.putExtra(EXTRA_LONGITUDE, event.longitude);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LostEvent event = buildEventFromIntent();
        if (!MapIntentHelper.openMap(this, event)) {
            Toast.makeText(this, R.string.toast_no_map_app, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private LostEvent buildEventFromIntent() {
        LostEvent event = new LostEvent();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_LATITUDE)) {
                event.latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0D);
            }
            if (intent.hasExtra(EXTRA_LONGITUDE)) {
                event.longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0D);
            }
        }
        return event;
    }
}
