package com.tiktoksave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Last fetched feed list, for "download all visible videos". */
public final class FeedCache {

    private static volatile List<Object> feed = Collections.emptyList();

    private FeedCache() {
    }

    public static void setFeed(List<?> list) {
        if (list == null) return;
        feed = new ArrayList<>(list);
    }

    public static List<Object> getFeed() {
        return feed;
    }
}
