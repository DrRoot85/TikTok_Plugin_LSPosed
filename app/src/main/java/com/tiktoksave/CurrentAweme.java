package com.tiktoksave;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the Aweme object the user is currently watching.
 * Video instances are mapped back to their owning Aweme via weak references
 * so the map never leaks.
 */
public final class CurrentAweme {

    private static final Map<Object, WeakReference<Object>> VIDEO_TO_AWEME = new ConcurrentHashMap<>();
    private static volatile Object current;

    private CurrentAweme() {
    }

    /** Called from the Aweme.getVideo() hook. */
    public static void onVideo(Object video, Object aweme) {
        if (video == null || aweme == null) return;
        VIDEO_TO_AWEME.put(video, new WeakReference<>(aweme));
        current = aweme;
    }

    /** Called from Video.*Addr() getter hooks. */
    public static void onVideoAccess(Object video) {
        if (video == null) return;
        WeakReference<Object> ref = VIDEO_TO_AWEME.get(video);
        if (ref != null) {
            Object a = ref.get();
            if (a != null) current = a;
        }
    }

    public static Object getCurrent() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}
