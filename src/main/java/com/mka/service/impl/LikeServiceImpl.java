package com.mka.service.impl;

import com.mka.entity.Comment;
import com.mka.entity.CommentLike;
import com.mka.entity.Post;
import com.mka.entity.PostLike;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.NotificationType;
import com.mka.enums.PostStatus;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.CommentLikeRepository;
import com.mka.repository.CommentRepository;
import com.mka.repository.PostLikeRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.LikeService;
import com.mka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void likePost(String email, Long postId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!postLikeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            try {
                PostLike like = PostLike.builder()
                        .post(post)
                        .user(user)
                        .build();
                postLikeRepository.save(like);

                postRepository.incrementLikeCount(postId);

                if (!post.getUser().getId().equals(user.getId())) {
                    Profile profile = profileRepository.findByUser(user).orElse(null);
                    String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : null;

                    notificationService.createNotification(
                            post.getUser(),
                            user,
                            avatar,
                            NotificationType.POST_LIKE,
                            user.getFullName() + " liked your post",
                            post.getId()
                    );
                }
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Ignore concurrent duplicate like attempt
            }
        }
    }

    @Override
    @Transactional
    public void unlikePost(String email, Long postId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        postLikeRepository.findByPostIdAndUserId(postId, user.getId()).ifPresent(like -> {
            postLikeRepository.delete(like);
            postRepository.decrementLikeCount(postId);
        });
    }

    @Override
    @Transactional
    public void likeComment(String email, Long commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, user.getId())) {
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(like);

            comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);
            commentRepository.save(comment);

            if (!comment.getUser().getId().equals(user.getId())) {
                Profile profile = profileRepository.findByUser(user).orElse(null);
                String avatar = profile != null && profile.getAvatar() != null ? profile.getAvatar() : null;

                notificationService.createNotification(
                        comment.getUser(),
                        user,
                        avatar,
                        NotificationType.COMMENT,
                        user.getFullName() + " liked your comment",
                        comment.getId()
                );
            }
        }
    }

    @Override
    @Transactional
    public void unlikeComment(String email, Long commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        commentLikeRepository.findByCommentIdAndUserId(commentId, user.getId()).ifPresent(like -> {
            commentLikeRepository.delete(like);
            long current = comment.getLikeCount() != null ? comment.getLikeCount() : 0;
            comment.setLikeCount(Math.max(0, current - 1));
            commentRepository.save(comment);
        });
    }
}
