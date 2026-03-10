package com.example.freeclipguard;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freeclipguard.data.LostEvent;
import com.example.freeclipguard.data.LostEventRepository;
import com.example.freeclipguard.ui.LostEventAdapter;
import com.example.freeclipguard.util.MapIntentHelper;

public class EventHistoryActivity extends AppCompatActivity {

    private LostEventRepository lostEventRepository;
    private LostEventAdapter lostEventAdapter;
    private TextView emptyHistoryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        lostEventRepository = LostEventRepository.getInstance(this);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);

        RecyclerView historyRecycler = findViewById(R.id.historyRecycler);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        lostEventAdapter = new LostEventAdapter(this::openMap);
        historyRecycler.setAdapter(lostEventAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lostEventRepository.loadRecent(50, events -> {
            lostEventAdapter.submitList(events);
            emptyHistoryText.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void openMap(LostEvent event) {
        if (event.latitude == null || event.longitude == null) {
            Toast.makeText(this, R.string.toast_no_valid_location, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!MapIntentHelper.openMap(this, event)) {
            Toast.makeText(this, R.string.toast_no_map_app, Toast.LENGTH_SHORT).show();
            return;
        }
    }
}
