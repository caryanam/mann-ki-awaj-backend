package com.mka.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public final class MediaUrlUtils {

    private static String configuredBaseUrl = "";

    public MediaUrlUtils(@Value("${app.backend-base-url:}") String baseUrl) {
        setBaseUrl(baseUrl);
    }

    public static synchronized void setBaseUrl(String baseUrl) {
        if (baseUrl != null) {
            String trimmed = baseUrl.trim();
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            configuredBaseUrl = trimmed;
        } else {
            configuredBaseUrl = "";
        }
    }

    public static String getBaseUrl() {
        if (configuredBaseUrl != null && !configuredBaseUrl.isEmpty()) {
            return configuredBaseUrl;
        }
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String toAbsoluteUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String clean = path.trim();
        if (clean.startsWith("data:")) {
            return clean;
        }

        // If path already contains host with internal uploads or media, normalize it to relative
        if (clean.matches("(?i)^https?://[^/]+/(uploads|media|api/admin/music|api/music)/.*")) {
            clean = clean.replaceFirst("(?i)^https?://[^/]+", "");
        } else if (clean.startsWith("http://") || clean.startsWith("https://")) {
            return clean;
        }

        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }

        String base = getBaseUrl();
        if (base == null || base.isEmpty()) {
            return clean;
        }

        return base + clean;
    }
}

