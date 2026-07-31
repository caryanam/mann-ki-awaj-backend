package com.mka.dto.request;

import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePostRequest {

    @Size(max = 2000, message = "Post content cannot exceed 2000 characters")
    private String content;

    private String title;

    private String summary;

    private String caption;

    private String description;

    private String username;

    private PostTopic topic;

    private PostType type;

    private String imageUrl;

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

    public PostTopic getTopic() { return topic; }
    public void setTopic(PostTopic topic) { this.topic = topic; }

    public PostType getType() { return type; }
    public void setType(PostType type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
