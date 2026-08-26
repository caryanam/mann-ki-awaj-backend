package com.mka.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mka.enums.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePostRequest {


    @NotBlank(message = "Post content cannot be empty")
    @Size(max = 2500, message = "Post content cannot exceed 2500 characters")
    private String content;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Summary cannot exceed 500 characters")
    private String summary;

    @Size(max = 300, message = "Caption cannot exceed 300 characters")
    private String caption;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;

    @Size(max = 1024, message = "Image URL cannot exceed 1024 characters")
    private String imageUrl;

    @Size(max = 1024, message = "Audio URL cannot exceed 1024 characters")
    private String audioUrl;

    @Size(max = 10, message = "Original language code cannot exceed 10 characters")
    private String originalLanguage;

    private String topic;

    private String subtopic;

    private PostType type;


    private String movieName;

    private Integer movieRating;

    private Boolean isSpoiler;

    private String mood;
}
