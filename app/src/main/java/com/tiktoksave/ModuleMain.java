package com.tiktoksave;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

/**
 * Module entry point (declared in META-INF/xposed/java_init.list).
 *
 * Uses the modern libxposed API (>= 101, LSPosed 1.9+ / 2.x).
 * Targets only the official TikTok app (com.zhiliaoapp.musically), main process.
 */
public class ModuleMain extends XposedModule {

    public static final String TAG = "TikTokSave";
    public static final String TIKTOK_PACKAGE = "com.zhiliaoapp.musically";

    private String processName;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        if (!TIKTOK_PACKAGE.equals(param.getPackageName())) return;
        if (processName != null && !processName.equals(TIKTOK_PACKAGE)) {
            // Only the main UI process gets hooks/downloads.
            return;
        }
        try {
            Settings.init(getRemotePreferences(Settings.PREFS_GROUP));
        } catch (Throwable t) {
            log(Log.WARN, TAG, "remote prefs unavailable, using defaults", t);
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!TIKTOK_PACKAGE.equals(param.getPackageName())) return;
        if (processName != null && !processName.equals(TIKTOK_PACKAGE)) return;
        try {
            TiktokHooks.install(this, param.getClassLoader());
            log(Log.INFO, TAG, "hooks installed");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to install hooks", t);
        }
    }
}
