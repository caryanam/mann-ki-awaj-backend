package com.mka.dto.request;

import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    @NotBlank(message = "Post content cannot be empty")
    @Size(max = 2500, message = "Post content cannot exceed 2500 characters")
    private String content;

    private String title;

    private String summary;

    private String caption;

    private String description;

    private String username;

    private String imageUrl;

    private String originalLanguage;

    private PostTopic topic;

    private PostType type;
}
