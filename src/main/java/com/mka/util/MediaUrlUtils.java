package com.mka.util;

public final class MediaUrlUtils {
    public static final String BASE_URL = "https://api.awaazmanki.com";

    private MediaUrlUtils() { }

    public static String toAbsoluteUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String clean = path.trim();
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("data:")) {
            return clean;
        }
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        return BASE_URL + clean;
    }
}
