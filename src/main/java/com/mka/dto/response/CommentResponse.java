package com.mka.dto.response;

import com.mka.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long parentCommentId;
    private Long authorId;
    private String username;
    private String authorUsername;
    private String authorAvatar;
    private String originalContent;
    private String translatedContent;
    private String originalLanguage;
    private String displayLanguage;
    private long likeCount;
    private Map<ReactionType, Long> reactionCounts;
    private boolean isLikedByCurrentUser;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;
}
