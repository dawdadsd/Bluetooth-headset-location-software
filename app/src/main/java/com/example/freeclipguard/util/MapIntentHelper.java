package com.example.freeclipguard.util;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.example.freeclipguard.MapDispatchActivity;
import com.example.freeclipguard.data.LostEvent;

public final class MapIntentHelper {

    private static final String AMAP_PACKAGE = "com.autonavi.minimap";
    private static final String SOURCE_APPLICATION = "FreeClipGuard";
    private static final String POINT_NAME = "FreeClip Last Seen";

    private MapIntentHelper() {
    }

    public static boolean openMap(Context context, LostEvent event) {
        if (event.latitude == null || event.longitude == null) {
            return false;
        }
        Intent intent = buildBestIntent(event);
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
            return true;
        }
        catch (ActivityNotFoundException exception) {
            try {
                Intent fallbackIntent = buildGeoIntent(event);
                if (!(context instanceof android.app.Activity)) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(fallbackIntent);
                return true;
            }
            catch (ActivityNotFoundException ignored) {
                return false;
            }
        }
    }

    @Nullable
    public static PendingIntent buildMapPendingIntent(Context context, LostEvent event, int requestCode) {
        if (event.latitude == null || event.longitude == null) {
            return null;
        }
        Intent intent = MapDispatchActivity.createIntent(context, event);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static Intent buildBestIntent(LostEvent event) {
        Intent amapIntent = buildAmapIntent(event);
        amapIntent.setPackage(AMAP_PACKAGE);
        return amapIntent;
    }

    private static Intent buildAmapIntent(LostEvent event) {
        String uri = "androidamap://viewMap?sourceApplication=" + Uri.encode(SOURCE_APPLICATION)
                + "&poiname=" + Uri.encode(POINT_NAME)
                + "&lat=" + event.latitude
                + "&lon=" + event.longitude
                + "&dev=0";
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    }

    private static Intent buildGeoIntent(LostEvent event) {
        String geoUri = "geo:" + event.latitude + "," + event.longitude
                + "?q=" + event.latitude + "," + event.longitude + "(" + Uri.encode(POINT_NAME) + ")";
        return new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
    }
}
