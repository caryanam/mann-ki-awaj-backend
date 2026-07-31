package com.mka.service.impl;

import com.mka.dto.response.PostResponse;
import com.mka.entity.Post;
import com.mka.entity.SavedPost;
import com.mka.entity.User;
import com.mka.enums.PostStatus;
import com.mka.enums.ReactionType;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.PostLikeRepository;
import com.mka.repository.PostReactionRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.SavedPostRepository;
import com.mka.repository.UserRepository;
import com.mka.service.SavedPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SavedPostServiceImpl implements SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReactionRepository postReactionRepository;

    @Override
    @Transactional
    public void savePost(String email, Long postId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (savedPostRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            throw new ResourceAlreadyExistsException("Post already saved");
        }

        SavedPost savedPost = SavedPost.builder()
                .user(user)
                .post(post)
                .build();

        savedPostRepository.save(savedPost);
    }

    @Override
    @Transactional
    public void unsavePost(String email, Long postId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        SavedPost savedPost = savedPostRepository.findByUserIdAndPostId(user.getId(), postId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved post relationship not found"));

        savedPostRepository.delete(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getSavedPosts(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Page<SavedPost> savedPosts = savedPostRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return savedPosts.map(sp -> mapPostToResponse(sp.getPost(), user));
    }

    private PostResponse mapPostToResponse(Post post, User user) {
        Map<ReactionType, Long> reactionCounts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            long count = postReactionRepository.countByPostIdAndReactionType(post.getId(), type);
            if (count > 0) {
                reactionCounts.put(type, count);
            }
        }

        boolean isLiked = user != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
        boolean isSaved = user != null && savedPostRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .postId("POST-" + post.getId())
                .authorId(post.getUser() != null ? post.getUser().getId() : null)
                .username(post.getUser() != null ? post.getUser().getFullName() : "Anonymous")
                .authorAvatar(post.getAuthorAvatar())
                .originalContent(post.getOriginalContent())
                .translatedContent(post.getOriginalContent())
                .originalLanguage(post.getOriginalLanguage())
                .displayLanguage(post.getOriginalLanguage())
                .topic(post.getTopic())
                .type(post.getType())
                .imageUrl(post.getImageUrl())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0L)
                .reactionCounts(reactionCounts)
                .isLikedByCurrentUser(isLiked)
                .isSavedByCurrentUser(isSaved)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
