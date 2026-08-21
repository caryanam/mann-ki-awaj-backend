package com.mka.service;

import com.mka.entity.Post;
import com.mka.entity.User;
import com.mka.entity.UserHidePost;
import com.mka.repository.PostRepository;
import com.mka.repository.UserHidePostRepository;
import com.mka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserHidePostService {

    private final UserHidePostRepository userHidePostRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public void hidePost(String userIdentifier, Long postId) {
        if (userIdentifier == null || userIdentifier.isBlank() || postId == null) return;

        User user = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (user == null) return;

        if (!userHidePostRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            Post post = postRepository.findById(postId).orElse(null);
            String title = post != null ? (post.getTitle() != null && !post.getTitle().isBlank() ? post.getTitle() : (post.getOriginalContent() != null ? post.getOriginalContent() : post.getDescription())) : ("Post #" + postId);
            String author = post != null ? (post.getUsername() != null ? post.getUsername() : (post.getUser() != null ? post.getUser().getUsername() : "@anonymous")) : "@anonymous";


            UserHidePost hide = UserHidePost.builder()
                    .user(user)
                    .postId(postId)
                    .postTitle(title != null && title.length() > 250 ? title.substring(0, 247) + "..." : title)
                    .authorUsername(author)
                    .build();
            userHidePostRepository.save(hide);
        }
    }

    @Transactional
    public void unhidePost(String userIdentifier, Long postId) {
        if (userIdentifier == null || userIdentifier.isBlank() || postId == null) return;

        User user = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (user == null) return;

        userHidePostRepository.deleteByUserIdAndPostId(user.getId(), postId);
    }

    @Transactional(readOnly = true)
    public List<UserHidePost> getHiddenPosts(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) return Collections.emptyList();

        User user = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (user == null) return Collections.emptyList();

        return userHidePostRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
