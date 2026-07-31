package com.mka.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 500, message = "Comment content cannot exceed 500 characters")
    private String content;

    private String originalLanguage;

    public CreateCommentRequest() {}

    public CreateCommentRequest(String content, String originalLanguage) {
        this.content = content;
        this.originalLanguage = originalLanguage;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public static CreateCommentRequestBuilder builder() { return new CreateCommentRequestBuilder(); }

    public static class CreateCommentRequestBuilder {
        private String content;
        private String originalLanguage;

        public CreateCommentRequestBuilder content(String content) { this.content = content; return this; }
        public CreateCommentRequestBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }

        public CreateCommentRequest build() {
            return new CreateCommentRequest(content, originalLanguage);
        }
    }
}
