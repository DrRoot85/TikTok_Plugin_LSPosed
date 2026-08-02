package com.tiktoksave;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;

/**
 * All hooks against TikTok builds (com.zhiliaoapp.musically / com.ss.android.ugc.trill).
 * Hook points verified against the unpacked modded-TikTok 45.5.3 dex
 * (same codebase family as official 41.3.x):
 *
 *  - Aweme.getVideo()                 -> track currently viewed Aweme
 *  - Video.{getPlayAddr,getDownloadAddr,getDownloadNoWatermarkAddr,
 *         getNewDownloadAddr,getH264PlayAddr,getH265PlayAddr} -> mark access
 *  - FeedItemList.getAwemeList()      -> cache last feed for "download all"
 *  - Activity.onPostResume/onResume   -> attach the floating download button
 *
 * Every hook group is isolated: a class/method missing in one build (Lite vs
 * full, regional variants, mods) only skips that group. Each successful hook
 * registration is counted and logged so LSPosed -> Logs shows exactly what
 * attached and what was skipped and why.
 */
public final class TiktokHooks {

    private static final String TAG = ModuleMain.TAG;

    private static final String[] VIDEO_GETTERS = {
            "getPlayAddr", "getDownloadAddr", "getDownloadNoWatermarkAddr",
            "getNewDownloadAddr", "getH264PlayAddr", "getH265PlayAddr"
    };

    private static int installed;

    private TiktokHooks() {
    }

    /**
     * Installs all hook groups. Returns the number of successful hook
     * registrations (for the "N hooks active" diagnostic).
     */
    public static int install(XposedModule module, ClassLoader cl) {
        installed = 0;
        FabView.setModule(module);
        hookAwemeGetVideo(module, cl);
        hookVideoGetters(module, cl);
        hookFeedList(module, cl);
        hookActivity(module, cl);
        return installed;
    }

    // -- Aweme.getVideo -> track currently viewed video
    private static void hookAwemeGetVideo(XposedModule module, ClassLoader cl) {
        try {
            Class<?> aweme = cl.loadClass("com.ss.android.ugc.aweme.feed.model.Aweme");
            Method getVideo = aweme.getMethod("getVideo");
            module.deoptimize(getVideo);
            module.hook(getVideo).intercept(chain -> {
                Object result = chain.proceed();
                if (result != null) CurrentAweme.onVideo(result, chain.getThisObject());
                return result;
            });
            ok(module, "Aweme.getVideo");
        } catch (Throwable t) {
            fail(module, "Aweme.getVideo", t);
        }
    }

    // -- Video getters -> mark access on the current video
    private static void hookVideoGetters(XposedModule module, ClassLoader cl) {
        try {
            Class<?> video = cl.loadClass("com.ss.android.ugc.aweme.feed.model.Video");
            for (String name : VIDEO_GETTERS) {
                try {
                    Method m = video.getMethod(name);
                    module.deoptimize(m);
                    module.hook(m).intercept(chain -> {
                        Object r = chain.proceed();
                        CurrentAweme.onVideoAccess(chain.getThisObject());
                        return r;
                    });
                    ok(module, "Video." + name);
                } catch (Throwable t) {
                    fail(module, "Video." + name, t);
                }
            }
        } catch (Throwable t) {
            fail(module, "Video class", t);
        }
    }

    // -- FeedItemList.getAwemeList -> cache last feed for "download all"
    private static void hookFeedList(XposedModule module, ClassLoader cl) {
        try {
            Class<?> feedItemList = cl.loadClass("com.ss.android.ugc.aweme.feed.model.FeedItemList");
            Method getList = feedItemList.getMethod("getAwemeList");
            module.deoptimize(getList);
            module.hook(getList).intercept(chain -> {
                Object r = chain.proceed();
                if (r instanceof List) FeedCache.setFeed((List<?>) r);
                return r;
            });
            ok(module, "FeedItemList.getAwemeList");
        } catch (Throwable t) {
            fail(module, "FeedItemList.getAwemeList", t);
        }
    }

    // -- Activity lifecycle -> floating button. Framework class: present in every build.
    private static void hookActivity(XposedModule module, ClassLoader cl) {
        try {
            Class<?> activity = Class.forName("android.app.Activity", false, cl);
            try {
                Method onCreate = activity.getMethod("onCreate", android.os.Bundle.class);
                module.deoptimize(onCreate);
                module.hook(onCreate).intercept(chain -> {
                    Object r = chain.proceed();
                    Object self = chain.getThisObject();
                    if (self instanceof android.app.Activity) {
                        try {
                            FabView.attach((android.app.Activity) self);
                        } catch (Throwable ignored) {
                        }
                    }
                    return r;
                });
                ok(module, "Activity.onCreate (FAB)");
            } catch (Throwable t) {
                fail(module, "Activity.onCreate", t);
            }
            try {
                Method onPostResume = activity.getMethod("onPostResume");
                module.deoptimize(onPostResume);
                module.hook(onPostResume).intercept(chain -> {
                    Object r = chain.proceed();
                    Object self = chain.getThisObject();
                    if (self instanceof android.app.Activity) {
                        try {
                            FabView.attach((android.app.Activity) self);
                        } catch (Throwable ignored) {
                        }
                    }
                    return r;
                });
                ok(module, "Activity.onPostResume (FAB)");
            } catch (Throwable t) {
                fail(module, "Activity.onPostResume", t);
            }
            try {
                Method onResume = activity.getMethod("onResume");
                module.deoptimize(onResume);
                module.hook(onResume).intercept(chain -> {
                    Object r = chain.proceed();
                    Object self = chain.getThisObject();
                    if (self instanceof android.app.Activity) {
                        try {
                            FabView.attach((android.app.Activity) self);
                        } catch (Throwable ignored) {
                        }
                    }
                    return r;
                });
                ok(module, "Activity.onResume (FAB)");
            } catch (Throwable t) {
                fail(module, "Activity.onResume", t);
            }
        } catch (Throwable t) {
            fail(module, "Activity class", t);
        }
    }

    private static void ok(XposedModule module, String what) {
        installed++;
        Log.i(TAG, "hook active: " + what);
        module.log(Log.INFO, TAG, "hook active: " + what);
    }

    private static void fail(XposedModule module, String what, Throwable t) {
        Log.w(TAG, "hook skipped: " + what + " -> " + t);
        module.log(Log.WARN, TAG, "hook skipped: " + what + " -> " + t);
    }
}
