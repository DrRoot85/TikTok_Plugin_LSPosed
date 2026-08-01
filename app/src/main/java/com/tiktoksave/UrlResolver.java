package com.tiktoksave;

import java.util.List;

/**
 * Resolves the best downloadable URL for an Aweme.
 *
 * Priority (no-watermark mode):
 *   downloadNoWatermarkAddr -> newDownloadAddr -> downloadAddr -> h264PlayAddr -> playAddr
 * The last two are watermarked playback streams; when no-watermark mode is on
 * they are converted (playwm -> play) which often yields a clean CDN copy.
 *
 * Only the URL string is touched. The bytes are fetched from TikTok's own CDN
 * with a plain HTTP client. No third-party services, no Telegram, nothing else.
 */
public final class UrlResolver {

    private UrlResolver() {
    }

    public static String videoUrl(Object aweme, boolean noWatermark) {
        try {
            Object video = AwemeReflect.getField(aweme, "video");
            if (video == null) return null;

            if (noWatermark) {
                String u = AwemeReflect.firstUrl(AwemeReflect.getField(video, "downloadNoWatermarkAddr"));
                if (usable(u)) return u;
                u = AwemeReflect.firstUrl(AwemeReflect.getField(video, "newDownloadAddr"));
                if (usable(u)) return u;
                u = AwemeReflect.firstUrl(AwemeReflect.getField(video, "downloadAddr"));
                if (usable(u)) return u;
            }

            String u = AwemeReflect.firstUrl(AwemeReflect.getField(video, "h264PlayAddr"));
            if (!usable(u)) u = AwemeReflect.firstUrl(AwemeReflect.getField(video, "playAddr"));
            if (!usable(u)) return null;
            if (noWatermark) u = stripWatermark(u);
            return u;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Number of images in a photo-mode post. */
    public static int imageCount(Object aweme) {
        try {
            Object list = AwemeReflect.getField(aweme, "imageInfos");
            if (list instanceof List) return ((List<?>) list).size();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** Image URL list for photo-mode posts. */
    public static java.util.List<String> imageUrls(Object aweme) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            Object list = AwemeReflect.getField(aweme, "imageInfos");
            if (list instanceof List) {
                for (Object info : (List<?>) list) {
                    // ImageInfo -> UrlModel[urlList] -> url
                    Object urlList = AwemeReflect.getField(info, "urlList");
                    if (urlList instanceof List && !((List<?>) urlList).isEmpty()) {
                        String u = String.valueOf(((List<?>) urlList).get(0));
                        if (usable(u)) out.add(u);
                    } else {
                        Object u2 = AwemeReflect.getField(info, "uri");
                        if (u2 != null) {
                            String u = String.valueOf(u2);
                            if (usable(u)) out.add(u);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static boolean usable(String u) {
        if (u == null || u.isEmpty()) return false;
        String lower = u.toLowerCase();
        // Skip playlists / streams we can't handle as direct files.
        return !lower.contains("m3u8") && !lower.endsWith(".ts");
    }

    private static String stripWatermark(String u) {
        String s = u;
        if (s.contains("playwm")) s = s.replace("playwm", "play");
        s = s.replaceAll("[?&]watermark=1", "&watermark=0");
        return s;
    }
}
