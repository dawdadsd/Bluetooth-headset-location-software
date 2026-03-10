package com.example.freeclipguard.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.freeclipguard.R;
import com.example.freeclipguard.data.LostEvent;

public final class DisconnectOverlayManager {

    private static final long AUTO_DISMISS_DELAY_MS = 20_000L;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Nullable
    private static WindowManager windowManager;
    @Nullable
    private static View overlayView;
    @Nullable
    private static Runnable autoDismissRunnable;

    private DisconnectOverlayManager() {
    }

    public static boolean show(Context context, LostEvent event, String prompt) {
        Context appContext = context.getApplicationContext();
        if (!PermissionHelper.hasOverlayPermission(appContext)) {
            return false;
        }
        MAIN_HANDLER.post(() -> showInternal(appContext, event, prompt));
        return true;
    }

    private static void showInternal(Context context, LostEvent event, String prompt) {
        dismissInternal();
        WindowManager manager = context.getSystemService(WindowManager.class);
        if (manager == null) {
            return;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.overlay_disconnect_alert, null, false);
        TextView titleText = view.findViewById(R.id.overlayTitleText);
        TextView messageText = view.findViewById(R.id.overlayMessageText);
        TextView detailText = view.findViewById(R.id.overlayDetailText);
        Button openMapButton = view.findViewById(R.id.overlayOpenMapButton);
        Button dismissButton = view.findViewById(R.id.overlayDismissButton);

        titleText.setText(R.string.overlay_disconnect_title);
        messageText.setText(prompt);
        detailText.setText(context.getString(
                R.string.overlay_disconnect_detail,
                Formatters.formatTime(event.eventTimeMs),
                Formatters.formatCoordinates(event.latitude, event.longitude)
        ));

        if (event.latitude == null || event.longitude == null) {
            openMapButton.setVisibility(View.GONE);
        }
        else {
            openMapButton.setOnClickListener(view1 -> {
                MapIntentHelper.openMap(context, event);
                dismiss();
            });
        }
        dismissButton.setOnClickListener(view12 -> dismiss());

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.CENTER;

        manager.addView(view, layoutParams);
        windowManager = manager;
        overlayView = view;
        autoDismissRunnable = DisconnectOverlayManager::dismissInternal;
        MAIN_HANDLER.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY_MS);
    }

    public static void dismiss() {
        MAIN_HANDLER.post(DisconnectOverlayManager::dismissInternal);
    }

    private static void dismissInternal() {
        if (autoDismissRunnable != null) {
            MAIN_HANDLER.removeCallbacks(autoDismissRunnable);
            autoDismissRunnable = null;
        }
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeViewImmediate(overlayView);
            }
            catch (Exception ignored) {
            }
        }
        overlayView = null;
        windowManager = null;
    }
}
