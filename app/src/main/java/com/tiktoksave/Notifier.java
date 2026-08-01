package com.tiktoksave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public final class Notifier {

    private static final String CHANNEL = "downloads";

    private Notifier() {
    }

    private static NotificationManager nm(Context ctx) {
        return (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("TikTok Plugin download progress");
            nm(ctx).createNotificationChannel(ch);
        }
    }

    public static void downloading(Context ctx, int id, String title) {
        if (!Settings.notifyEnabled()) return;
        try {
            ensureChannel(ctx);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CHANNEL)
                    : new Notification.Builder(ctx);
            b.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(title)
                    .setContentText("Downloading…")
                    .setOngoing(true)
                    .setAutoCancel(false);
            nm(ctx).notify(id, b.build());
        } catch (Throwable ignored) {
        }
    }

    public static void done(Context ctx, int id, String title, Uri uri, boolean isVideo) {
        if (!Settings.notifyEnabled()) return;
        try {
            ensureChannel(ctx);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CHANNEL)
                    : new Notification.Builder(ctx);
            b.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(title)
                    .setContentText("Saved to Downloads")
                    .setAutoCancel(true);
            if (uri != null && Build.VERSION.SDK_INT >= 29) {
                Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, isVideo ? "video/mp4" : "image/jpeg");
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                b.setContentIntent(android.app.PendingIntent.getActivity(ctx, id, view,
                        Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                : android.app.PendingIntent.FLAG_UPDATE_CURRENT));
            }
            nm(ctx).notify(id, b.build());
        } catch (Throwable ignored) {
        }
    }

    public static void failed(Context ctx, int id, String title) {
        if (!Settings.notifyEnabled()) return;
        try {
            ensureChannel(ctx);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CHANNEL)
                    : new Notification.Builder(ctx);
            b.setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(title)
                    .setContentText("Download failed")
                    .setAutoCancel(true);
            nm(ctx).notify(id, b.build());
        } catch (Throwable ignored) {
        }
    }

    public static void cancel(Context ctx, int id) {
        try {
            nm(ctx).cancel(id);
        } catch (Throwable ignored) {
        }
    }
}
