package com.mka.dto.request;

import com.mka.enums.WarningLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendWarningRequest {
    @NotNull(message = "Warning level is required")
    private WarningLevel warningLevel;

    @NotBlank(message = "Warning message is required")
    private String message;

    public WarningLevel getWarningLevel() { return warningLevel; }
    public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
