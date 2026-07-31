package com.mka.dto.request;

import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePostRequest {

    @NotBlank(message = "Post content cannot be empty")
    @Size(max = 2000, message = "Post content cannot exceed 2000 characters")
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

    public CreatePostRequest() {}

    public CreatePostRequest(String content, String title, String summary, String caption, String description, String username, String imageUrl, String originalLanguage, PostTopic topic, PostType type) {
        this.content = content;
        this.title = title;
        this.summary = summary;
        this.caption = caption;
        this.description = description;
        this.username = username;
        this.imageUrl = imageUrl;
        this.originalLanguage = originalLanguage;
        this.topic = topic;
        this.type = type;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public PostTopic getTopic() { return topic; }
    public void setTopic(PostTopic topic) { this.topic = topic; }

    public PostType getType() { return type; }
    public void setType(PostType type) { this.type = type; }

    public static CreatePostRequestBuilder builder() { return new CreatePostRequestBuilder(); }

    public static class CreatePostRequestBuilder {
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

        public CreatePostRequestBuilder content(String content) { this.content = content; return this; }
        public CreatePostRequestBuilder title(String title) { this.title = title; return this; }
        public CreatePostRequestBuilder summary(String summary) { this.summary = summary; return this; }
        public CreatePostRequestBuilder caption(String caption) { this.caption = caption; return this; }
        public CreatePostRequestBuilder description(String description) { this.description = description; return this; }
        public CreatePostRequestBuilder username(String username) { this.username = username; return this; }
        public CreatePostRequestBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public CreatePostRequestBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }
        public CreatePostRequestBuilder topic(PostTopic topic) { this.topic = topic; return this; }
        public CreatePostRequestBuilder type(PostType type) { this.type = type; return this; }

        public CreatePostRequest build() {
            return new CreatePostRequest(content, title, summary, caption, description, username, imageUrl, originalLanguage, topic, type);
        }
    }
}
