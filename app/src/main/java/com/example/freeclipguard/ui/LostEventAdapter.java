package com.example.freeclipguard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freeclipguard.R;
import com.example.freeclipguard.data.LostEvent;
import com.example.freeclipguard.util.Formatters;

import java.util.ArrayList;
import java.util.List;

public final class LostEventAdapter extends RecyclerView.Adapter<LostEventAdapter.LostEventViewHolder> {

    public interface OnOpenMapClickedListener {
        void onOpenMapClicked(LostEvent event);
    }

    private final List<LostEvent> events = new ArrayList<>();
    private final OnOpenMapClickedListener onOpenMapClickedListener;

    public LostEventAdapter(OnOpenMapClickedListener onOpenMapClickedListener) {
        this.onOpenMapClickedListener = onOpenMapClickedListener;
    }

    public void submitList(List<LostEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LostEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_lost_event, parent, false);
        return new LostEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LostEventViewHolder holder, int position) {
        holder.bind(events.get(position), onOpenMapClickedListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static final class LostEventViewHolder extends RecyclerView.ViewHolder {

        private final TextView eventTitleText;
        private final TextView eventMetaText;
        private final TextView eventLocationText;
        private final Button openMapButton;

        LostEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitleText = itemView.findViewById(R.id.eventTitleText);
            eventMetaText = itemView.findViewById(R.id.eventMetaText);
            eventLocationText = itemView.findViewById(R.id.eventLocationText);
            openMapButton = itemView.findViewById(R.id.openMapButton);
        }

        void bind(LostEvent event, OnOpenMapClickedListener listener) {
            eventTitleText.setText(Formatters.formatEventTitle(event));
            eventMetaText.setText(Formatters.formatEventMeta(event));
            eventLocationText.setText(Formatters.formatEventLocation(event));
            openMapButton.setOnClickListener(view -> listener.onOpenMapClicked(event));
            openMapButton.setEnabled(event.latitude != null && event.longitude != null);
        }
    }
}
