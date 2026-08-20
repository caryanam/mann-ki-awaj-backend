package com.mka.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsernameAiCheckResult {
    private boolean allowed;
    private String reason;

    public static UsernameAiCheckResult approved() {
        return UsernameAiCheckResult.builder().allowed(true).reason(null).build();
    }

    public static UsernameAiCheckResult rejected(String reason) {
        return UsernameAiCheckResult.builder().allowed(false).reason(reason).build();
    }
}
