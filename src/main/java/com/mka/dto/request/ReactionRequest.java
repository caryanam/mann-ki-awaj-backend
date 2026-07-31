package com.mka.dto.request;

import com.mka.enums.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {

    @NotNull(message = "Reaction type is required")
    private ReactionType reactionType;

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }
}
