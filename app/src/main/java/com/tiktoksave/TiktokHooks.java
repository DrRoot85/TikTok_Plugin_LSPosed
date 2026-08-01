package com.tiktoksave;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

/**
 * All hooks against TikTok (com.zhiliaoapp.musically).
 * Hook points verified against the unpacked modded-TikTok 45.5.3 dex
 * (same codebase family as official 41.3.x):
 *
 *  - Aweme.getVideo()                 -> track currently viewed Aweme
 *  - Video.{getPlayAddr,getDownloadAddr,getDownloadNoWatermarkAddr,
 *         getNewDownloadAddr,getH264PlayAddr,getH265PlayAddr} -> mark access
 *  - FeedItemList.getAwemeList()      -> cache last feed for "download all"
 *  - Activity.onPostResume/onResume   -> attach the floating download button
 */
public final class TiktokHooks {

    private static final String[] VIDEO_GETTERS = {
            "getPlayAddr", "getDownloadAddr", "getDownloadNoWatermarkAddr",
            "getNewDownloadAddr", "getH264PlayAddr", "getH265PlayAddr"
    };

    private TiktokHooks() {
    }

    public static void install(XposedInterface xi, ClassLoader cl) throws Throwable {
        // -- Aweme.getVideo -> track current
        Class<?> aweme = cl.loadClass("com.ss.android.ugc.aweme.feed.model.Aweme");
        Method getVideo = aweme.getMethod("getVideo");
        xi.deoptimize(getVideo);
        xi.hook(getVideo).intercept(chain -> {
            Object result = chain.proceed();
            if (result != null) CurrentAweme.onVideo(result, chain.getThisObject());
            return result;
        });

        // -- Video getters -> mark current
        Class<?> video = cl.loadClass("com.ss.android.ugc.aweme.feed.model.Video");
        for (String name : VIDEO_GETTERS) {
            try {
                Method m = video.getMethod(name);
                xi.deoptimize(m);
                xi.hook(m).intercept(chain -> {
                    Object r = chain.proceed();
                    CurrentAweme.onVideoAccess(chain.getThisObject());
                    return r;
                });
            } catch (NoSuchMethodException ignored) {
                // getter not present in this build
            }
        }

        // -- FeedItemList.getAwemeList -> cache feed
        try {
            Class<?> feedItemList = cl.loadClass("com.ss.android.ugc.aweme.feed.model.FeedItemList");
            Method getList = feedItemList.getMethod("getAwemeList");
            xi.deoptimize(getList);
            xi.hook(getList).intercept(chain -> {
                Object r = chain.proceed();
                if (r instanceof java.util.List) FeedCache.setFeed((java.util.List<?>) r);
                return r;
            });
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }

        // -- Activity lifecycle -> FAB
        try {
            Class<?> activity = Class.forName("android.app.Activity", false, cl);
            Method onPostResume = activity.getMethod("onPostResume");
            xi.deoptimize(onPostResume);
            xi.hook(onPostResume).intercept(chain -> {
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
            Method onResume = activity.getMethod("onResume");
            xi.deoptimize(onResume);
            xi.hook(onResume).intercept(chain -> {
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
        } catch (NoSuchMethodException ignored) {
        }
    }
}
