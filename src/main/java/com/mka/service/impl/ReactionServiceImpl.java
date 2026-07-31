package com.mka.service.impl;

import com.mka.dto.request.ReactionRequest;
import com.mka.entity.Comment;
import com.mka.entity.CommentReaction;
import com.mka.entity.Post;
import com.mka.entity.PostReaction;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.PostStatus;
import com.mka.enums.ReactionType;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.CommentReactionRepository;
import com.mka.repository.CommentRepository;
import com.mka.repository.PostReactionRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.UserRepository;
import com.mka.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final PostReactionRepository postReactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void reactToPost(String email, Long postId, ReactionRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        ReactionType reactionType = request.getReactionType() != null ? request.getReactionType() : ReactionType.LIKE;

        Optional<PostReaction> existingReaction = postReactionRepository.findByPostIdAndUserId(postId, user.getId());
        if (existingReaction.isPresent()) {
            PostReaction reaction = existingReaction.get();
            reaction.setReactionType(reactionType);
            postReactionRepository.save(reaction);
        } else {
            PostReaction reaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            postReactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public void removePostReaction(String email, Long postId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        postReactionRepository.findByPostIdAndUserId(postId, user.getId())
                .ifPresent(postReactionRepository::delete);
    }

    @Override
    @Transactional
    public void reactToComment(String email, Long commentId, ReactionRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        ReactionType reactionType = request.getReactionType() != null ? request.getReactionType() : ReactionType.LIKE;

        Optional<CommentReaction> existingReaction = commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId());
        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            reaction.setReactionType(reactionType);
            commentReactionRepository.save(reaction);
        } else {
            CommentReaction reaction = CommentReaction.builder()
                    .comment(comment)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            commentReactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public void removeCommentReaction(String email, Long commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId())
                .ifPresent(commentReactionRepository::delete);
    }
}
