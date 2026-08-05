package com.mka.util.translation;

import com.mka.enums.translation.EntityType;
import com.mka.enums.translation.SupportedLanguage;

/**
 * Utility methods for translation operations, language code normalization, and cache keys.
 */
public final class TranslationUtils {

    private TranslationUtils() {
        // Utility class
    }

    public static String generateCacheKey(EntityType entityType, Long entityId, SupportedLanguage targetLanguage) {
        if (entityType == null || entityId == null || targetLanguage == null) {
            return null;
        }
        return String.format("%s:%d:%s", entityType.name(), entityId, targetLanguage.getCode());
    }

    public static String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    public static boolean isSameLanguage(SupportedLanguage lang1, SupportedLanguage lang2) {
        if (lang1 == null || lang2 == null) {
            return false;
        }
        return lang1 == lang2;
    }
}
