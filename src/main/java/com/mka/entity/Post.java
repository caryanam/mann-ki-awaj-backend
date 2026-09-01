package com.mka.entity;

import com.mka.enums.PostStatus;
import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 500)
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String authorAvatar;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(nullable = false, length = 10)
    private String originalLanguage = "EN";

    @Column(name = "topic", nullable = false, length = 100)
    private String topic = "GENERAL";

    @Column(name = "subtopic", length = 100)
    private String subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostType type = PostType.TEXT;


    @Column(length = 255)
    private String imageUrl;

    @Column(name = "audio_url", length = 1024)
    private String audioUrl;

    @Column(name = "is_music_community")
    private Boolean isMusicCommunity = false;

    @Column(name = "music_track_id")
    private Long musicTrackId;

    @Column(length = 150)
    private String movieName;

    private Integer movieRating;

    private Boolean isSpoiler = false;

    @Column(length = 50)
    private String mood;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Post() {}

    public Post(Long id, User user, String username, String title, String summary, String caption, String description, String authorAvatar, String originalContent, String originalLanguage, String topic, PostType type, String imageUrl, Long likeCount, Long commentCount, PostStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.username = username;
        this.title = title;
        this.summary = summary;
        this.caption = caption;
        this.description = description;
        this.authorAvatar = authorAvatar;
        this.originalContent = originalContent;
        this.originalLanguage = originalLanguage != null ? originalLanguage : "EN";
        this.topic = topic != null ? topic : "GENERAL";
        this.type = type != null ? type : PostType.TEXT;

        this.imageUrl = imageUrl;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.commentCount = commentCount != null ? commentCount : 0L;
        this.status = status != null ? status : PostStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getFormattedPostId() {
        return id != null ? String.format("POST_%02d", id) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getSubtopic() { return subtopic; }
    public void setSubtopic(String subtopic) { this.subtopic = subtopic; }

    public PostType getType() { return type; }
    public void setType(PostType type) { this.type = type; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }

    public Integer getMovieRating() { return movieRating; }
    public void setMovieRating(Integer movieRating) { this.movieRating = movieRating; }

    public Boolean getIsSpoiler() { return isSpoiler != null ? isSpoiler : false; }
    public void setIsSpoiler(Boolean isSpoiler) { this.isSpoiler = isSpoiler; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }

    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsMusicCommunity() { return isMusicCommunity; }
    public void setIsMusicCommunity(Boolean isMusicCommunity) { this.isMusicCommunity = isMusicCommunity; }

    public Long getMusicTrackId() { return musicTrackId; }
    public void setMusicTrackId(Long musicTrackId) { this.musicTrackId = musicTrackId; }

    public static PostBuilder builder() { return new PostBuilder(); }

    public static class PostBuilder {
        private Long id;
        private User user;
        private String username;
        private String title;
        private String summary;
        private String caption;
        private String description;
        private String authorAvatar;
        private String originalContent;
        private String originalLanguage = "EN";
        private String topic = "GENERAL";
        private String subtopic;
        private PostType type = PostType.TEXT;
        private String imageUrl;
        private String audioUrl;
        private Boolean isMusicCommunity = false;
        private Long musicTrackId;
        private String movieName;
        private Integer movieRating;
        private Boolean isSpoiler = false;
        private String mood;
        private Long likeCount = 0L;
        private Long commentCount = 0L;
        private PostStatus status = PostStatus.ACTIVE;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PostBuilder id(Long id) { this.id = id; return this; }
        public PostBuilder user(User user) { this.user = user; return this; }
        public PostBuilder username(String username) { this.username = username; return this; }
        public PostBuilder title(String title) { this.title = title; return this; }
        public PostBuilder summary(String summary) { this.summary = summary; return this; }
        public PostBuilder caption(String caption) { this.caption = caption; return this; }
        public PostBuilder description(String description) { this.description = description; return this; }
        public PostBuilder authorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; return this; }
        public PostBuilder originalContent(String originalContent) { this.originalContent = originalContent; return this; }
        public PostBuilder originalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; return this; }
        public PostBuilder topic(String topic) { this.topic = topic; return this; }
        public PostBuilder subtopic(String subtopic) { this.subtopic = subtopic; return this; }
        public PostBuilder type(PostType type) { this.type = type; return this; }
        public PostBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public PostBuilder audioUrl(String audioUrl) { this.audioUrl = audioUrl; return this; }
        public PostBuilder isMusicCommunity(Boolean isMusicCommunity) { this.isMusicCommunity = isMusicCommunity; return this; }
        public PostBuilder musicTrackId(Long musicTrackId) { this.musicTrackId = musicTrackId; return this; }
        public PostBuilder movieName(String movieName) { this.movieName = movieName; return this; }
        public PostBuilder movieRating(Integer movieRating) { this.movieRating = movieRating; return this; }
        public PostBuilder isSpoiler(Boolean isSpoiler) { this.isSpoiler = isSpoiler; return this; }
        public PostBuilder mood(String mood) { this.mood = mood; return this; }
        public PostBuilder likeCount(Long likeCount) { this.likeCount = likeCount; return this; }
        public PostBuilder commentCount(Long commentCount) { this.commentCount = commentCount; return this; }
        public PostBuilder status(PostStatus status) { this.status = status; return this; }
        public PostBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PostBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Post build() {
            Post post = new Post();
            post.setId(id);
            post.setUser(user);
            post.setUsername(username);
            post.setTitle(title);
            post.setSummary(summary);
            post.setCaption(caption);
            post.setDescription(description);
            post.setAuthorAvatar(authorAvatar);
            post.setOriginalContent(originalContent);
            post.setOriginalLanguage(originalLanguage);
            post.setTopic(topic);
            post.setSubtopic(subtopic);
            post.setType(type);

            post.setImageUrl(imageUrl);
            post.setAudioUrl(audioUrl);
            post.setIsMusicCommunity(isMusicCommunity);
            post.setMusicTrackId(musicTrackId);
            post.setMovieName(movieName);
            post.setMovieRating(movieRating);
            post.setIsSpoiler(isSpoiler);
            post.setMood(mood);
            post.setLikeCount(likeCount);
            post.setCommentCount(commentCount);
            post.setStatus(status);
            post.setCreatedAt(createdAt);
            post.setUpdatedAt(updatedAt);
            return post;
        }
    }
}
