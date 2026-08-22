package com.mka.service.impl;

import com.mka.dto.request.CreateCommentRequest;
import com.mka.dto.response.CommentResponse;
import com.mka.entity.Comment;
import com.mka.entity.CustomTopic;
import com.mka.entity.Post;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.NotificationType;
import com.mka.enums.PostStatus;
import com.mka.enums.ReactionType;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.CommentLikeRepository;
import com.mka.repository.CommentReactionRepository;
import com.mka.repository.CommentRepository;
import com.mka.repository.CustomTopicRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.AiService;
import com.mka.service.CommentService;
import com.mka.service.NotificationService;
import com.mka.translation.dto.TranslationResponse;
import com.mka.translation.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CustomTopicRepository customTopicRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final AiService aiService;
    private final NotificationService notificationService;
    private final TranslationService translationService;

    @Override
    @Transactional
    public CommentResponse createComment(String email, Long postId, CreateCommentRequest request) {
        validateNewComment(request);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        moderateTextIfPresent(user, request);

        Profile userProfile = profileRepository.findByUser(user).orElse(null);
        String avatar = userProfile != null && userProfile.getAvatar() != null
                ? userProfile.getAvatar() : "avatar_default";
        String senderHandle = userProfile != null && userProfile.getUsername() != null
                ? (userProfile.getUsername().startsWith("@") ? userProfile.getUsername() : "@" + userProfile.getUsername())
                : (user.getUsername() != null ? (user.getUsername().startsWith("@") ? user.getUsername() : "@" + user.getUsername()) : "@user");

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .authorAvatar(avatar)
                .originalContent(request.getContent())
                .originalLanguage(request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(0L)
                .build();

        Comment savedComment = commentRepository.save(comment);

        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postRepository.save(post);

        if (!post.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                    post.getUser(),
                    user,
                    avatar,
                    NotificationType.COMMENT,
                    senderHandle + " commented on your post",
                    post.getId()
            );
        }

        return mapToResponse(savedComment, user);
    }

    @Override
    @Transactional
    public CommentResponse createTopicComment(String email, Long topicId, CreateCommentRequest request) {
        validateNewComment(request);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        CustomTopic topic = customTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));

        moderateTextIfPresent(user, request);
        Profile profile = profileRepository.findByUser(user).orElse(null);
        String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : "avatar_default";

        Comment comment = Comment.builder()
                .customTopic(topic)
                .user(user)
                .authorAvatar(avatar)
                .originalContent(request.getContent())
                .imageUrl(request.getImageUrl())
                .imageUrl(request.getImageUrl())
                .originalLanguage(request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(0L)
                .build();
        return mapToResponse(commentRepository.save(comment), user);
    }

    @Override
    @Transactional
    public CommentResponse replyToComment(String email, Long commentId, CreateCommentRequest request) {
        validateNewComment(request);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment parentComment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        moderateTextIfPresent(user, request);

        Profile userProfile = profileRepository.findByUser(user).orElse(null);
        String avatar = userProfile != null && userProfile.getAvatar() != null
                ? userProfile.getAvatar() : "avatar_default";
        String senderHandle = userProfile != null && userProfile.getUsername() != null
                ? (userProfile.getUsername().startsWith("@") ? userProfile.getUsername() : "@" + userProfile.getUsername())
                : (user.getUsername() != null ? (user.getUsername().startsWith("@") ? user.getUsername() : "@" + user.getUsername()) : "@user");

        Comment reply = Comment.builder()
                .post(parentComment.getPost())
                .customTopic(parentComment.getCustomTopic())
                .parentComment(parentComment)
                .user(user)
                .authorAvatar(avatar)
                .originalContent(request.getContent())
                .imageUrl(request.getImageUrl())
                .originalLanguage(request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(0L)
                .build();

        Comment savedReply = commentRepository.save(reply);

        Post post = parentComment.getPost();
        if (post != null) {
            post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
            postRepository.save(post);
        }

        if (!parentComment.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                    parentComment.getUser(),
                    user,
                    avatar,
                    NotificationType.REPLY,
                    senderHandle + " replied to your comment",
                    post != null ? post.getId() : parentComment.getCustomTopic().getId()
            );
        }

        return mapToResponse(savedReply, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPostId(String email, Long postId, Pageable pageable) {
        User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;

        Page<Comment> rootComments = commentRepository.findByPostIdAndParentCommentIsNullAndStatus(
                postId, CommentStatus.ACTIVE, pageable);

        if (rootComments.isEmpty()) {
            return rootComments.map(c -> null);
        }

        List<Long> rootCommentIds = rootComments.getContent().stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> allReplies = commentRepository.findByParentCommentIdInAndStatus(rootCommentIds, CommentStatus.ACTIVE);

        Map<Long, List<Comment>> repliesByParentId = allReplies.stream()
                .filter(r -> r.getParentComment() != null)
                .collect(Collectors.groupingBy(r -> r.getParentComment().getId()));

        List<Comment> allComments = new java.util.ArrayList<>(rootComments.getContent());
        allComments.addAll(allReplies);

        List<Long> allCommentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());

        java.util.Set<Long> authorUserIds = allComments.stream()
                .map(c -> c.getUser() != null ? c.getUser().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Profile> authorProfiles = authorUserIds.isEmpty() ? List.of() :
                profileRepository.findByUserIdIn(new java.util.ArrayList<>(authorUserIds));

        Map<Long, Profile> authorProfilesByUserId = authorProfiles.stream()
                .filter(p -> p.getUser() != null)
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (p1, p2) -> p1));

        String userLang = "EN";
        if (user != null) {
            Profile currentUserProfile = authorProfilesByUserId.get(user.getId());
            if (currentUserProfile == null) {
                currentUserProfile = profileRepository.findByUser(user).orElse(null);
            }
            if (currentUserProfile != null && currentUserProfile.getPreferredLanguage() != null) {
                userLang = currentUserProfile.getPreferredLanguage();
            }
        }

        Map<Long, Map<ReactionType, Long>> reactionCountsByCommentId = new java.util.HashMap<>();
        if (!allCommentIds.isEmpty()) {
            List<CommentReactionRepository.CommentReactionCountProjection> counts =
                    commentReactionRepository.findReactionCountsByCommentIdIn(allCommentIds);
            for (CommentReactionRepository.CommentReactionCountProjection projection : counts) {
                reactionCountsByCommentId
                        .computeIfAbsent(projection.getCommentId(), k -> new EnumMap<>(ReactionType.class))
                        .put(projection.getReactionType(), projection.getCount());
            }
        }

        java.util.Set<Long> userLikedCommentIds = (user != null && !allCommentIds.isEmpty())
                ? new java.util.HashSet<>(commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIdIn(user.getId(), allCommentIds))
                : java.util.Set.of();

        final String finalUserLang = userLang;

        java.util.function.Function<Comment, CommentResponse> mapCommentInBatch = (c) -> {
            String translated = c.getOriginalContent();
            if (finalUserLang != null && !finalUserLang.equalsIgnoreCase(c.getOriginalLanguage())) {
                try {
                    TranslationResponse resp = translationService.translate(
                            c.getOriginalContent(),
                            c.getOriginalLanguage(),
                            finalUserLang
                    );
                    if (resp != null && resp.getTranslatedText() != null) {
                        translated = resp.getTranslatedText();
                    }
                } catch (Exception ex) {
                    // Fallback gracefully
                }
            }

            Map<ReactionType, Long> reactionCounts = reactionCountsByCommentId.getOrDefault(c.getId(), Map.of());
            boolean isLiked = userLikedCommentIds.contains(c.getId());

            Profile authorProfile = c.getUser() != null ? authorProfilesByUserId.get(c.getUser().getId()) : null;
            String handle = authorProfile != null && authorProfile.getUsername() != null
                    ? authorProfile.getUsername()
                    : (c.getUser() != null && c.getUser().getEmail() != null ? c.getUser().getEmail().split("@")[0] : "anonymous");

            return CommentResponse.builder()
                    .id(c.getId())
                    .postId(c.getPost() != null ? c.getPost().getId() : null)
                    .topicId(c.getCustomTopic() != null ? c.getCustomTopic().getId() : null)
                    .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                    .authorId(c.getUser() != null ? c.getUser().getId() : null)
                    .username(handle)
                    .authorUsername(handle)
                    .authorAvatar(c.getAuthorAvatar())
                    .originalContent(c.getOriginalContent())
                    .translatedContent(translated)
                    .imageUrl(c.getImageUrl())
                    .originalLanguage(c.getOriginalLanguage())
                    .displayLanguage(finalUserLang != null ? finalUserLang : c.getOriginalLanguage())
                    .likeCount(c.getLikeCount() != null ? c.getLikeCount() : 0L)
                    .reactionCounts(reactionCounts)
                    .isLikedByCurrentUser(isLiked)
                    .createdAt(c.getCreatedAt())
                    .build();
        };

        return rootComments.map(root -> {
            CommentResponse resp = mapCommentInBatch.apply(root);
            List<Comment> childReplies = repliesByParentId.getOrDefault(root.getId(), List.of());
            resp.setReplies(childReplies.stream().map(mapCommentInBatch).collect(Collectors.toList()));
            return resp;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByTopicId(String email, Long topicId, Pageable pageable) {
        customTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));
        User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;
        Page<Comment> comments = commentRepository.findByCustomTopicIdAndParentCommentIsNullAndStatus(
                topicId, CommentStatus.ACTIVE, pageable);
        return comments.map(comment -> {
            CommentResponse response = mapToResponse(comment, user);
            response.setReplies(commentRepository.findByParentCommentIdAndStatus(comment.getId(), CommentStatus.ACTIVE)
                    .stream().map(reply -> mapToResponse(reply, user)).collect(Collectors.toList()));
            return response;
        });
    }

    @Override
    @Transactional
    public CommentResponse updateComment(String email, Long id, CreateCommentRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(id, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User not authorized to update this comment");
        }

        moderateTextIfPresent(user, request);

        comment.setOriginalContent(request.getContent());
        Comment updated = commentRepository.save(comment);

        return mapToResponse(updated, user);
    }

    @Override
    @Transactional
    public void deleteComment(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(id, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User not authorized to delete this comment");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);

        Post post = comment.getPost();
        if (post != null) {
            long current = post.getCommentCount() != null ? post.getCommentCount() : 0;
            post.setCommentCount(Math.max(0, current - 1));
            postRepository.save(post);
        }
    }

    private void validateNewComment(CreateCommentRequest request) {
        boolean hasText = request != null && request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasImage = request != null && request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty();
        if (!hasText && !hasImage) {
            throw new IllegalArgumentException("Comment text or image is required");
        }
        if (!hasText) request.setContent("");
    }

    private void moderateTextIfPresent(User user, CreateCommentRequest request) {
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            aiService.moderateContent(user, request.getContent(), "COMMENT");
        }
    }

    private CommentResponse mapToResponse(Comment comment, User currentUser) {
        String userLang = "EN";
        if (currentUser != null) {
            Profile profile = profileRepository.findByUser(currentUser).orElse(null);
            if (profile != null && profile.getPreferredLanguage() != null) {
                userLang = profile.getPreferredLanguage();
            }
        }

        String translated = comment.getOriginalContent();
        if (userLang != null && !userLang.equalsIgnoreCase(comment.getOriginalLanguage())) {
            try {
                TranslationResponse resp = translationService.translate(
                        comment.getOriginalContent(),
                        comment.getOriginalLanguage(),
                        userLang
                );
                if (resp != null && resp.getTranslatedText() != null) {
                    translated = resp.getTranslatedText();
                }
            } catch (Exception ex) {
                // Fallback gracefully to original text on translation failure
            }
        }

        Map<ReactionType, Long> reactionCounts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            long count = commentReactionRepository.countByCommentIdAndReactionType(comment.getId(), type);
            if (count > 0) {
                reactionCounts.put(type, count);
            }
        }

        boolean isLiked = currentUser != null && commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUser.getId());

        Profile authorProfile = comment.getUser() != null ? profileRepository.findByUser(comment.getUser()).orElse(null) : null;
        String handle = authorProfile != null && authorProfile.getUsername() != null
                ? authorProfile.getUsername()
                : (comment.getUser() != null && comment.getUser().getEmail() != null ? comment.getUser().getEmail().split("@")[0] : "anonymous");

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .topicId(comment.getCustomTopic() != null ? comment.getCustomTopic().getId() : null)
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(comment.getUser().getId())
                .username(handle)
                .authorUsername(handle)
                .authorAvatar(comment.getAuthorAvatar())
                .originalContent(comment.getOriginalContent())
                .translatedContent(translated)
                .imageUrl(comment.getImageUrl())
                .originalLanguage(comment.getOriginalLanguage())
                .displayLanguage(userLang != null ? userLang : comment.getOriginalLanguage())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0L)
                .reactionCounts(reactionCounts)
                .isLikedByCurrentUser(isLiked)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
