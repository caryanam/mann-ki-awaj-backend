package com.mka.enums.translation;

/**
 * Enumeration representing supported languages for translation services,
 * mapping standard ISO codes, display names, and IndicTrans2 language identifiers.
 */
public enum SupportedLanguage {
    EN("English", "en", "eng_Latn"),
    HI("Hindi", "hi", "hin_Deva"),
    MR("Marathi", "mr", "mar_Deva"),
    PA("Punjabi", "pa", "pan_Guru"),
    TA("Tamil", "ta", "tam_Taml"),
    TE("Telugu", "te", "tel_Telu"),
    GU("Gujarati", "gu", "guj_Gujr"),
    BN("Bengali", "bn", "ben_Beng"),
    KN("Kannada", "kn", "kan_Knda"),
    ML("Malayalam", "ml", "mal_Mlym"),
    OR("Odia", "or", "ory_Orya"),
    AS("Assamese", "as", "asm_Beng"),
    UR("Urdu", "ur", "urd_Arab");

    private final String displayName;
    private final String code;
    private final String indicTransCode;

    SupportedLanguage(String displayName, String code, String indicTransCode) {
        this.displayName = displayName;
        this.code = code;
        this.indicTransCode = indicTransCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getIndicTransCode() {
        return indicTransCode;
    }

    /**
     * Resolves a language code string to a SupportedLanguage instance.
     * Defaults to EN if unsupported or null.
     */
    public static SupportedLanguage fromCode(String codeStr) {
        if (codeStr == null || codeStr.trim().isEmpty()) {
            return EN;
        }
        String cleanCode = codeStr.trim();
        for (SupportedLanguage lang : values()) {
            if (lang.name().equalsIgnoreCase(cleanCode) ||
                lang.getCode().equalsIgnoreCase(cleanCode) ||
                lang.getIndicTransCode().equalsIgnoreCase(cleanCode) ||
                lang.getDisplayName().equalsIgnoreCase(cleanCode)) {
                return lang;
            }
        }
        return EN;
    }
}
