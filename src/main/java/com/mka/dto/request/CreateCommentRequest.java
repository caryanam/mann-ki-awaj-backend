package com.mka.dto.request;

import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    @Size(max = 500, message = "Comment content cannot exceed 500 characters")
    private String content;

    @Size(max = 10, message = "Original language code cannot exceed 10 characters")
    private String originalLanguage;

    @Size(max = 500, message = "Comment image URL cannot exceed 500 characters")
    private String imageUrl;

    public CreateCommentRequest() {}

    public CreateCommentRequest(String content, String originalLanguage) {
        this.content = content;
        this.originalLanguage = originalLanguage;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public static CreateCommentRequestBuilder builder() { return new CreateCommentRequestBuilder(); }

    public static class CreateCommentRequestBuilder {
        private String content;
        private String originalLanguage;
        private String imageUrl;

        public CreateCommentRequestBuilder content(String content) { this.content = content; return this; }
        public CreateCommentRequestBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }
        public CreateCommentRequestBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }

        public CreateCommentRequest build() {
            CreateCommentRequest request = new CreateCommentRequest(content, originalLanguage);
            request.setImageUrl(imageUrl);
            return request;
        }
    }
}
