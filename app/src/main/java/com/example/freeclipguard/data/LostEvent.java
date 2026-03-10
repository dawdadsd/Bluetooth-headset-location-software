package com.example.freeclipguard.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lost_events")
public class LostEvent {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String deviceName;
    public String deviceAddress;
    public long eventTimeMs;
    public Double latitude;
    public Double longitude;
    public Float accuracyMeters;
    public Long locationSampleTimeMs;
    public String eventSource;
    public String eventType;
    public Integer rssi;
    public boolean atHome;
    public String note;
}
