package com.mka.service.impl;

import com.mka.dto.request.CreateCommentRequest;
import com.mka.dto.response.CommentResponse;
import com.mka.entity.Comment;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        aiService.moderateContent(request.getContent());

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
    public CommentResponse replyToComment(String email, Long commentId, CreateCommentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment parentComment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        aiService.moderateContent(request.getContent());

        Profile userProfile = profileRepository.findByUser(user).orElse(null);
        String avatar = userProfile != null && userProfile.getAvatar() != null
                ? userProfile.getAvatar() : "avatar_default";
        String senderHandle = userProfile != null && userProfile.getUsername() != null
                ? (userProfile.getUsername().startsWith("@") ? userProfile.getUsername() : "@" + userProfile.getUsername())
                : (user.getUsername() != null ? (user.getUsername().startsWith("@") ? user.getUsername() : "@" + user.getUsername()) : "@user");

        Comment reply = Comment.builder()
                .post(parentComment.getPost())
                .parentComment(parentComment)
                .user(user)
                .authorAvatar(avatar)
                .originalContent(request.getContent())
                .originalLanguage(request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(0L)
                .build();

        Comment savedReply = commentRepository.save(reply);

        Post post = parentComment.getPost();
        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postRepository.save(post);

        if (!parentComment.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                    parentComment.getUser(),
                    user,
                    avatar,
                    NotificationType.REPLY,
                    senderHandle + " replied to your comment",
                    post.getId()
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

        return rootComments.map(c -> {
            CommentResponse resp = mapToResponse(c, user);
            List<Comment> childReplies = commentRepository.findByParentCommentIdAndStatus(c.getId(), CommentStatus.ACTIVE);
            resp.setReplies(childReplies.stream().map(reply -> mapToResponse(reply, user)).collect(Collectors.toList()));
            return resp;
        });
    }

    @Override
    @Transactional
    public CommentResponse updateComment(String email, Long id, CreateCommentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(id, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("User not authorized to update this comment");
        }

        aiService.moderateContent(request.getContent());

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
        long current = post.getCommentCount() != null ? post.getCommentCount() : 0;
        post.setCommentCount(Math.max(0, current - 1));
        postRepository.save(post);
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
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(comment.getUser().getId())
                .username(handle)
                .authorUsername(handle)
                .authorAvatar(comment.getAuthorAvatar())
                .originalContent(comment.getOriginalContent())
                .translatedContent(translated)
                .originalLanguage(comment.getOriginalLanguage())
                .displayLanguage(userLang != null ? userLang : comment.getOriginalLanguage())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0L)
                .reactionCounts(reactionCounts)
                .isLikedByCurrentUser(isLiked)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
