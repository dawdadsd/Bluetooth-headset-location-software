package com.example.freeclipguard.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LostEventDao {

    @Insert
    long insert(LostEvent event);

    @Query("SELECT * FROM lost_events ORDER BY eventTimeMs DESC LIMIT 1")
    LostEvent findLatest();

    @Query("SELECT * FROM lost_events WHERE deviceAddress = :deviceAddress ORDER BY eventTimeMs DESC LIMIT 1")
    LostEvent findLatestForDevice(String deviceAddress);

    @Query("SELECT * FROM lost_events ORDER BY eventTimeMs DESC LIMIT :limit")
    List<LostEvent> listRecent(int limit);
}
