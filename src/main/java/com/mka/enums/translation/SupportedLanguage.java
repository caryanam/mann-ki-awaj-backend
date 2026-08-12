package com.mka.enums.translation;

/**
 * Enumeration representing supported languages in the system,
 * mapping standard ISO codes, display names, and FLORES language identifiers.
 */
public enum SupportedLanguage {
    EN("English", "en", "eng_Latn"),
    HI("Hindi", "hi", "hin_Deva"),
    BN("Bengali", "bn", "ben_Beng"),
    MR("Marathi", "mr", "mar_Deva"),
    TE("Telugu", "te", "tel_Telu"),
    TA("Tamil", "ta", "tam_Taml"),
    GU("Gujarati", "gu", "guj_Gujr"),
    UR("Urdu", "ur", "urd_Arab"),
    KN("Kannada", "kn", "kan_Knda"),
    OR("Odia", "or", "ory_Orya"),
    ML("Malayalam", "ml", "mal_Mlym"),
    PA("Punjabi", "pa", "pan_Guru"),
    AS("Assamese", "as", "asm_Beng"),
    SAT("Santali", "sat", "sat_Olck"),
    KS("Kashmiri", "ks", "kas_Deva"),
    MNI("Manipuri", "mni", "mni_Beng"),
    DOI("Dogri", "doi", "doi_Deva"),
    BHO("Bhojpuri", "bho", "bho_Deva");

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
