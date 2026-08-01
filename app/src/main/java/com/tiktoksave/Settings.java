package com.tiktoksave;

import android.content.SharedPreferences;

/**
 * Module settings. Backed by LSPosed remote preferences so the same values
 * are visible both inside the hooked TikTok process and in the module's own
 * settings activity (via the libxposed service).
 *
 * This is the ONLY settings source. No Telegram, no remote config, no network.
 */
public final class Settings {

    public static final String PREFS_GROUP = "tiktoksave";

    private static volatile SharedPreferences prefs;

    private Settings() {
    }

    public static void init(SharedPreferences p) {
        prefs = p;
    }

    public static String folder() {
        return s("save_folder", "TikTokSave");
    }

    public static boolean noWatermark() {
        return b("no_watermark", true);
    }

    public static boolean saveImages() {
        return b("save_images", true);
    }

    public static boolean notifyEnabled() {
        return b("notify", true);
    }

    public static boolean fabEnabled() {
        return b("fab", true);
    }

    public static String filenamePrefix() {
        return s("filename_prefix", "TikTok");
    }

    private static String s(String key, String def) {
        try {
            SharedPreferences p = prefs;
            return p != null ? p.getString(key, def) : def;
        } catch (Throwable t) {
            return def;
        }
    }

    private static boolean b(String key, boolean def) {
        try {
            SharedPreferences p = prefs;
            return p != null ? p.getBoolean(key, def) : def;
        } catch (Throwable t) {
            return def;
        }
    }
}
