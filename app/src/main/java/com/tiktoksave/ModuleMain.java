package com.tiktoksave;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

/**
 * Module entry point (declared in META-INF/xposed/java_init.list).
 *
 * Uses the modern libxposed API (>= 102, LSPosed 1.9.2+ / 2.x).
 * Targets official TikTok builds:
 *   com.zhiliaoapp.musically  – TikTok (global & most regions)
 *   com.ss.android.ugc.trill  – TikTok Lite
 *
 * Hook groups are installed independently in TiktokHooks.install, so a class
 * missing in one build never blocks the rest.
 */
public class ModuleMain extends XposedModule {

    public static final String TAG = "TikTokSave";

    public static final String[] TIKTOK_PACKAGES = {
            "com.zhiliaoapp.musically", // TikTok (global / most countries)
            "com.ss.android.ugc.trill"  // TikTok Lite
    };

    private String processName;

    private static boolean isTiktok(String pkg) {
        for (String p : TIKTOK_PACKAGES) {
            if (p.equals(pkg)) return true;
        }
        return false;
    }

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (!isTiktok(param.getPackageName())) return;
        // Only the main UI process gets hooks/downloads.
        if (processName != null && !processName.equals(param.getPackageName())) return;
        try {
            Settings.init(getRemotePreferences(Settings.PREFS_GROUP));
        } catch (Throwable t) {
            log(Log.WARN, TAG, "remote prefs unavailable, using defaults", t);
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!isTiktok(param.getPackageName())) return;
        if (processName != null && !processName.equals(param.getPackageName())) return;
        try {
            TiktokHooks.install(this, param.getClassLoader());
            log(Log.INFO, TAG, "hooks installed in " + param.getPackageName());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to install hooks", t);
        }
    }
}
