package com.mka.dto.response;

import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import com.mka.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String postId;
    private Long authorId;
    private String username;
    private String title;
    private String translatedTitle;
    private String summary;
    private String caption;
    private String description;
    private String authorAvatar;
    private String originalContent;
    private String translatedContent;
    private String originalLanguage;
    private String displayLanguage;
    private PostTopic topic;
    private PostType type;
    private String imageUrl;
    private String movieName;
    private Integer movieRating;
    private Boolean isSpoiler;
    private String mood;
    private long likeCount;
    private long commentCount;
    private Map<ReactionType, Long> reactionCounts;
    private ReactionType userReaction;
    private boolean isLikedByCurrentUser;
    private boolean isSavedByCurrentUser;
    private LocalDateTime createdAt;
}
