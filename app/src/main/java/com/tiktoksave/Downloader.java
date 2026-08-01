package com.tiktoksave;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Download engine. Pulls the resolved TikTok CDN URL with a plain
 * HttpURLConnection (no custom SSL, no MITM), streams it to MediaStore/disk.
 */
public final class Downloader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final AtomicInteger NOTIF_ID = new AtomicInteger(1000);
    private static final Set<String> IN_PROGRESS = new HashSet<>();
    private static final Object LOCK = new Object();

    private static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Mobile Safari/537.36";

    private Downloader() {
    }

    public static boolean isBusy() {
        synchronized (LOCK) {
            return !IN_PROGRESS.isEmpty();
        }
    }

    /** Download the Aweme currently on screen (video or photos). */
    public static void downloadCurrent(final Context ctx) {
        final Object aweme = CurrentAweme.getCurrent();
        if (aweme == null) {
            toast(ctx, "Nothing to download");
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                downloadAweme(ctx, aweme);
            } catch (Throwable t) {
                toast(ctx, "Download failed");
            }
        });
    }

    /** Download every video/photo set in the last loaded feed. */
    public static void downloadFeed(final Context ctx) {
        List<Object> feed = FeedCache.getFeed();
        if (feed.isEmpty()) {
            toast(ctx, "No feed cached yet");
            return;
        }
        toast(ctx, "Downloading " + feed.size() + " posts…");
        EXECUTOR.execute(() -> {
            int ok = 0;
            for (Object aweme : feed) {
                if (aweme == null) continue;
                try {
                    downloadAweme(ctx, aweme);
                    ok++;
                } catch (Throwable ignored) {
                }
            }
            toast(ctx, "Downloaded " + ok + " posts");
        });
    }

    private static void downloadAweme(Context ctx, Object aweme) throws Exception {
        String id = AwemeReflect.awemeId(aweme);
        if (id == null) id = String.valueOf(System.currentTimeMillis());
        String base = MediaSaver.sanitizeFilename(Settings.filenamePrefix() + "_" + id);

        boolean noWm = Settings.noWatermark();
        String videoUrl = UrlResolver.videoUrl(aweme, noWm);
        if (videoUrl != null) {
            String filename = base + ".mp4";
            if (!claim(filename)) return;
            int nid = NOTIF_ID.incrementAndGet();
            try {
                Notifier.downloading(ctx, nid, filename);
                fetchAndSave(ctx, videoUrl, filename, "video/mp4", nid, true);
            } finally {
                release(filename);
            }
            return;
        }

        // Photo mode
        if (Settings.saveImages()) {
            List<String> urls = UrlResolver.imageUrls(aweme);
            int i = 1;
            for (String u : urls) {
                String filename = base + "_" + i + ".jpg";
                if (!claim(filename)) continue;
                int nid = NOTIF_ID.incrementAndGet();
                try {
                    Notifier.downloading(ctx, nid, filename);
                    fetchAndSave(ctx, u, filename, "image/jpeg", nid, false);
                } finally {
                    release(filename);
                }
                i++;
            }
        }
    }

    private static boolean claim(String filename) {
        synchronized (LOCK) {
            if (IN_PROGRESS.contains(filename)) return false;
            IN_PROGRESS.add(filename);
            return true;
        }
    }

    private static void release(String filename) {
        synchronized (LOCK) {
            IN_PROGRESS.remove(filename);
        }
    }

    private static void fetchAndSave(Context ctx, String url, String filename, String mime, int nid, boolean isVideo) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.connect();
            int code = conn.getResponseCode();
            if (code != 200) {
                Notifier.failed(ctx, nid, filename);
                return;
            }
            String ct = conn.getContentType();
            if (ct != null && (ct.contains("text/html") || ct.contains("application/json"))) {
                Notifier.failed(ctx, nid, filename);
                return;
            }
            InputStream in = conn.getInputStream();
            Uri uri = MediaSaver.save(ctx, filename, mime, in, Settings.folder());
            Notifier.done(ctx, nid, filename, uri, isVideo);
            toast(ctx, "Saved: " + filename);
        } catch (Exception e) {
            Notifier.failed(ctx, nid, filename);
            throw e;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void toast(Context ctx, String msg) {
        try {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
