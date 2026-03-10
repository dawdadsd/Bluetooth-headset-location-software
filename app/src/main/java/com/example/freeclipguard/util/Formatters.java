package com.example.freeclipguard.util;

import androidx.annotation.Nullable;

import com.example.freeclipguard.data.LostEvent;
import com.example.freeclipguard.model.BoundDevice;
import com.example.freeclipguard.model.HomeLocation;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formatters {

    private Formatters() {
    }

    public static String formatDeviceSummary(BoundDevice boundDevice, long lastConnectedAt) {
        if (!boundDevice.isConfigured()) {
            return "当前未绑定耳机";
        }
        String suffix = lastConnectedAt > 0
                ? "，最近连接：" + formatTime(lastConnectedAt)
                : "，还没有记录到连接时间";
        return "已绑定：" + boundDevice.getName() + " (" + boundDevice.getAddress() + ")" + suffix;
    }

    public static String formatHomeSummary(@Nullable HomeLocation homeLocation) {
        if (homeLocation == null) {
            return "家位置：未设置";
        }
        return String.format(Locale.getDefault(),
                "家位置：%.5f, %.5f（半径 %.0f 米）",
                homeLocation.getLatitude(),
                homeLocation.getLongitude(),
                homeLocation.getRadiusMeters());
    }

    public static String formatLastEventSummary(@Nullable LostEvent event) {
        if (event == null) {
            return "最近事件：还没有耳机断开记录";
        }
        return "最近事件：" + formatTime(event.eventTimeMs) + "，" + formatCoordinates(event.latitude, event.longitude)
                + (event.atHome ? "，在家范围内" : "，可能遗落在外面");
    }

    public static String formatManualPlaceSummary(@Nullable String note, long savedAt) {
        if (note == null || note.isBlank()) {
            return "放置记录：还没有手动保存耳机放哪了";
        }
        if (savedAt <= 0) {
            return "放置记录：" + note;
        }
        return "放置记录：" + note + " · 记录于 " + formatTime(savedAt);
    }

    public static String formatEventTitle(LostEvent event) {
        return formatTime(event.eventTimeMs) + " · " + event.deviceName;
    }

    public static String formatEventMeta(LostEvent event) {
        String accuracyText = event.accuracyMeters == null
                ? "精度未知"
                : String.format(Locale.getDefault(), "定位精度 %.0f 米", event.accuracyMeters);
        String locationTimeText = event.locationSampleTimeMs == null
                ? "位置时间未知"
                : "位置样本 " + formatTime(event.locationSampleTimeMs);
        return event.eventSource + " · " + accuracyText + " · " + locationTimeText
                + (event.atHome ? " · 家里范围内" : " · 家外提醒");
    }

    public static String formatEventLocation(LostEvent event) {
        return formatCoordinates(event.latitude, event.longitude);
    }

    public static String formatCoordinates(@Nullable Double latitude, @Nullable Double longitude) {
        if (latitude == null || longitude == null) {
            return "未拿到有效位置";
        }
        return String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude);
    }

    public static String formatTime(long timestampMs) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(timestampMs));
    }

    public static String describeRssi(short rssi) {
        if (rssi >= -60) {
            return rssi + " dBm · 很近";
        }
        if (rssi >= -75) {
            return rssi + " dBm · 在附近";
        }
        if (rssi >= -88) {
            return rssi + " dBm · 偏远";
        }
        return rssi + " dBm · 很弱，可能在另一个房间或已远离";
    }
}
