package com.mka.service.impl;

import com.mka.dto.request.CreatePostRequest;
import com.mka.dto.request.UpdatePostRequest;
import com.mka.dto.response.PostResponse;
import com.mka.entity.Post;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.PostStatus;
import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import com.mka.enums.ReactionType;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.PostLikeRepository;
import com.mka.repository.PostReactionRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.SavedPostRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AiService;
import com.mka.service.PostService;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReactionRepository postReactionRepository;
    private final SavedPostRepository savedPostRepository;
    private final AiService aiService;
    private final TranslationService translationService;

    @Override
    @Transactional
    public PostResponse createPost(String email, CreatePostRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Profile profile = profileRepository.findByUser(user).orElse(null);
        String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : "avatar_default";
        String preferredLang = profile != null && profile.getPreferredLanguage() != null ? profile.getPreferredLanguage() : "EN";
        String handle = profile != null && profile.getUsername() != null ? profile.getUsername() : (user.getEmail() != null ? user.getEmail().split("@")[0] : "user_" + user.getId());

        String originalLang = preferredLang != null && !preferredLang.isBlank() ? preferredLang : "EN";

        StringBuilder fullPostTextBuilder = new StringBuilder();
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            fullPostTextBuilder.append("Title: ").append(request.getTitle()).append("\n");
        }
        if (request.getSummary() != null && !request.getSummary().isBlank()) {
            fullPostTextBuilder.append("Summary: ").append(request.getSummary()).append("\n");
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            fullPostTextBuilder.append("Content: ").append(request.getContent()).append("\n");
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            fullPostTextBuilder.append("Description: ").append(request.getDescription()).append("\n");
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            fullPostTextBuilder.append("Image Attached: ").append(request.getImageUrl()).append("\n");
        }
        String fullPostText = fullPostTextBuilder.length() > 0 ? fullPostTextBuilder.toString().trim() : (request.getContent() != null ? request.getContent() : "");

        aiService.moderateContent(user, fullPostText, "POST");

        Post post = Post.builder()
                .user(user)
                .username(handle)
                .title(request.getTitle())
                .summary(request.getSummary())
                .caption(request.getCaption())
                .description(request.getDescription())
                .authorAvatar(avatar)
                .originalContent(request.getContent())
                .originalLanguage(originalLang)
                .topic(request.getTopic() != null ? request.getTopic() : PostTopic.GENERAL)
                .type(request.getType() != null ? request.getType() : PostType.TEXT)
                .imageUrl(request.getImageUrl())
                .likeCount(0L)
                .commentCount(0L)
                .status(PostStatus.ACTIVE)
                .build();

        Post savedPost = postRepository.save(post);

        return mapPostToResponse(savedPost, user, preferredLang);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(String email, PostTopic topic, Pageable pageable) {
        User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;
        Profile profile = user != null ? profileRepository.findByUser(user).orElse(null) : null;
        String userLang = profile != null && profile.getPreferredLanguage() != null ? profile.getPreferredLanguage() : "EN";

        Page<Post> posts;
        if (topic != null) {
            posts = postRepository.findByStatusAndTopic(PostStatus.ACTIVE, topic, pageable);
        } else {
            posts = postRepository.findByStatus(PostStatus.ACTIVE, pageable);
        }

        return posts.map(p -> mapPostToResponse(p, user, userLang));
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(String email, Long id) {
        User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;
        Profile profile = user != null ? profileRepository.findByUser(user).orElse(null) : null;
        String userLang = profile != null && profile.getPreferredLanguage() != null ? profile.getPreferredLanguage() : "EN";

        Post post = postRepository.findByIdAndStatus(id, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        return mapPostToResponse(post, user, userLang);
    }

    @Override
    @Transactional
    public PostResponse updatePost(String email, Long id, UpdatePostRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(id, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User not authorized to update this post");
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            aiService.moderateContent(user, request.getContent(), "POST");
            post.setOriginalContent(request.getContent());
        }

        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getSummary() != null) post.setSummary(request.getSummary());
        if (request.getCaption() != null) post.setCaption(request.getCaption());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getTopic() != null) post.setTopic(request.getTopic());
        if (request.getType() != null) post.setType(request.getType());
        if (request.getImageUrl() != null) post.setImageUrl(request.getImageUrl());

        Post updated = postRepository.save(post);

        Profile profile = profileRepository.findByUser(user).orElse(null);
        String userLang = profile != null && profile.getPreferredLanguage() != null ? profile.getPreferredLanguage() : "EN";

        return mapPostToResponse(updated, user, userLang);
    }

    @Override
    @Transactional
    public void deletePost(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(id, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User not authorized to delete this post");
        }

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    private PostResponse mapPostToResponse(Post post, User currentUser, String targetLanguage) {
        String translated = post.getOriginalContent();
        String translatedTitle = post.getTitle();

        if (targetLanguage != null && !targetLanguage.equalsIgnoreCase(post.getOriginalLanguage())) {
            try {
                if (translationService != null && post.getOriginalContent() != null && !post.getOriginalContent().isBlank()) {
                    TranslationResponse response = translationService.translate(
                            post.getOriginalContent(),
                            post.getOriginalLanguage(),
                            targetLanguage
                    );
                    if (response != null && response.getTranslatedText() != null) {
                        translated = response.getTranslatedText();
                    }
                }
            } catch (Throwable ex) {
                log.warn("Post content translation skipped/failed [Post ID: {}]: {}", post.getId(), ex.getMessage());
            }

            if (post.getTitle() != null && !post.getTitle().isBlank()) {
                try {
                    if (translationService != null) {
                        TranslationResponse titleResp = translationService.translate(
                                post.getTitle(),
                                post.getOriginalLanguage(),
                                targetLanguage
                        );
                        if (titleResp != null && titleResp.getTranslatedText() != null) {
                            translatedTitle = titleResp.getTranslatedText();
                        }
                    }
                } catch (Throwable ex) {
                    log.warn("Post title translation skipped/failed [Post ID: {}]: {}", post.getId(), ex.getMessage());
                }
            }
        }

        Map<ReactionType, Long> reactionCounts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            long count = postReactionRepository.countByPostIdAndReactionType(post.getId(), type);
            if (count > 0) {
                reactionCounts.put(type, count);
            }
        }

        ReactionType userReaction = currentUser != null
                ? postReactionRepository.findByPostIdAndUserId(post.getId(), currentUser.getId())
                .map(com.mka.entity.PostReaction::getReactionType).orElse(null)
                : null;

        boolean isLiked = currentUser != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        boolean isSaved = currentUser != null && savedPostRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId());

        Profile authorProfile = post.getUser() != null ? profileRepository.findByUser(post.getUser()).orElse(null) : null;
        String handle = authorProfile != null && authorProfile.getUsername() != null
                ? authorProfile.getUsername()
                : (post.getUsername() != null && !post.getUsername().contains(" ") ? post.getUsername() : (post.getUser() != null && post.getUser().getEmail() != null ? post.getUser().getEmail().split("@")[0] : "anonymous"));

        return PostResponse.builder()
                .id(post.getId())
                .postId(post.getFormattedPostId())
                .authorId(post.getUser() != null ? post.getUser().getId() : null)
                .username(handle)
                .title(post.getTitle())
                .translatedTitle(translatedTitle)
                .summary(post.getSummary())
                .caption(post.getCaption())
                .description(post.getDescription())
                .authorAvatar(post.getAuthorAvatar())
                .originalContent(post.getOriginalContent())
                .translatedContent(translated)
                .originalLanguage(post.getOriginalLanguage())
                .displayLanguage(targetLanguage != null ? targetLanguage : post.getOriginalLanguage())
                .topic(post.getTopic())
                .type(post.getType())
                .imageUrl(post.getImageUrl())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0L)
                .reactionCounts(reactionCounts)
                .userReaction(userReaction)
                .isLikedByCurrentUser(isLiked)
                .isSavedByCurrentUser(isSaved)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
