package com.tiktoksave;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes downloaded bytes to user-visible storage:
 *  - Android 10+: MediaStore Downloads collection (no permissions needed)
 *  - below:    public Downloads/<folder> directory (TikTok already holds storage permission)
 */
public final class MediaSaver {

    private MediaSaver() {
    }

    public static Uri save(Context ctx, String displayName, String mime, InputStream in, String folder) throws Exception {
        String safeFolder = sanitize(folder);
        if (Build.VERSION.SDK_INT >= 29) {
            return saveToMediaStore(ctx, displayName, mime, in, safeFolder);
        }
        return saveToFile(ctx, displayName, in, safeFolder);
    }

    private static Uri saveToMediaStore(Context ctx, String displayName, String mime, InputStream in, String folder) throws Exception {
        boolean video = mime != null && mime.startsWith("video/");
        ContentResolver cr = ctx.getContentResolver();
        Uri collection = video ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime != null ? mime : "application/octet-stream");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + folder);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = cr.insert(collection, values);
        if (uri == null) throw new Exception("MediaStore insert failed");
        try {
            OutputStream os = cr.openOutputStream(uri);
            if (os == null) throw new Exception("MediaStore openOutputStream failed");
            try (OutputStream out = os; InputStream in2 = in) {
                byte[] buf = new byte[65536];
                int n;
                long total = 0;
                while ((n = in2.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    total += n;
                }
                out.flush();
            }
        } catch (Exception e) {
            cr.delete(uri, null, null);
            throw e;
        }
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        cr.update(uri, values, null, null);
        return uri;
    }

    private static Uri saveToFile(Context ctx, String displayName, InputStream in, String folder) throws Exception {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder);
        if (!dir.exists() && !dir.mkdirs()) {
            // Fallback to app-external dir if public dir is not writable
            dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), folder);
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("cannot create save dir");
        }
        File f = new File(dir, displayName);
        try (OutputStream out = new FileOutputStream(f); InputStream in2 = in) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in2.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        }
        return Uri.fromFile(f);
    }

    private static String sanitize(String folder) {
        if (folder == null) return "TikTokSave";
        String s = folder.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (s.isEmpty() || s.equals(".") || s.equals("..")) return "TikTokSave";
        if (s.length() > 60) s = s.substring(0, 60);
        return s;
    }

    public static String sanitizeFilename(String name) {
        String s = name == null ? "TikTok" : name;
        s = s.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (s.isEmpty()) s = "TikTok";
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }
}
