package com.example.freeclipguard.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.freeclipguard.EventHistoryActivity;
import com.example.freeclipguard.R;
import com.example.freeclipguard.data.LostEvent;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "earguard_alerts";
    private static final int ALERT_NOTIFICATION_ID = 1001;
    private static final int HISTORY_REQUEST_CODE = 1;

    private NotificationHelper() {
    }

    public static void showDisconnectAlert(Context context, LostEvent event, String prompt) {
        ensureChannel(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent = new Intent(context, EventHistoryActivity.class);
        PendingIntent historyPendingIntent = PendingIntent.getActivity(
                context,
                HISTORY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent mapPendingIntent = MapIntentHelper.buildMapPendingIntent(context, event, buildMapRequestCode(event));
        PendingIntent contentPendingIntent = mapPendingIntent != null ? mapPendingIntent : historyPendingIntent;

        String contentText = context.getString(
                R.string.notification_disconnect_detail,
                Formatters.formatCoordinates(event.latitude, event.longitude),
                Formatters.formatTime(event.eventTimeMs)
        );
        String bigText = prompt + "\n" + contentText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(prompt)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history,
                        context.getString(R.string.action_open_history),
                        historyPendingIntent);

        if (mapPendingIntent != null) {
            builder.addAction(android.R.drawable.ic_dialog_map,
                    context.getString(R.string.action_open_map),
                    mapPendingIntent);
        }

        try {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIFICATION_ID, builder.build());
        }
        catch (SecurityException ignored) {
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null || notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_desc));
        notificationManager.createNotificationChannel(channel);
    }

    private static int buildMapRequestCode(LostEvent event) {
        if (event.id > 0) {
            return (int) (event.id % Integer.MAX_VALUE);
        }
        return (int) (event.eventTimeMs % Integer.MAX_VALUE);
    }
}
