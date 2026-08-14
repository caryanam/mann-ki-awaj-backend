package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    private boolean successful;
    private boolean flagged;
    private Map<String, Boolean> categories;
    private Map<String, Double> categoryScores;
    private List<String> appliedInputTypes;
    private String reason;
    private String errorCode;
    private String userMessage;

    public static ModerationResult failClosed(String errorCode, String userMessage) {
        return ModerationResult.builder()
                .successful(false)
                .flagged(false)
                .errorCode(errorCode)
                .userMessage(userMessage != null ? userMessage : "Content safety verification is temporarily unavailable. Please try again.")
                .build();
    }

    public static ModerationResult flagged(String reason, Map<String, Boolean> categories, Map<String, Double> scores) {
        return ModerationResult.builder()
                .successful(true)
                .flagged(true)
                .reason(reason)
                .categories(categories)
                .categoryScores(scores)
                .userMessage("This content cannot be posted because it violates our community guidelines.")
                .build();
    }

    public static ModerationResult approved() {
        return ModerationResult.builder()
                .successful(true)
                .flagged(false)
                .userMessage("Content approved.")
                .build();
    }
}
