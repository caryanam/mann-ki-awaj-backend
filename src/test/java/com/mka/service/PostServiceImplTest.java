package com.mka.service;

import com.mka.dto.request.CreatePostRequest;
import com.mka.dto.response.PostResponse;
import com.mka.entity.Post;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.PostStatus;
import com.mka.enums.PostTopic;
import com.mka.enums.PostType;
import com.mka.enums.Role;
import com.mka.repository.*;
import com.mka.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostReactionRepository postReactionRepository;

    @Mock
    private SavedPostRepository savedPostRepository;

    @Mock
    private AiService aiService;

    @Mock
    private com.mka.translation.service.TranslationService translationService;

    @InjectMocks
    private PostServiceImpl postService;

    private User testUser;
    private Profile testProfile;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .build();

        testProfile = Profile.builder()
                .id(1L)
                .user(testUser)
                .username("test_user")
                .avatar("avatar_default")
                .preferredLanguage("EN")
                .build();

        testPost = Post.builder()
                .id(10L)
                .user(testUser)
                .authorAvatar("avatar_default")
                .originalContent("Hello World")
                .originalLanguage("EN")
                .topic(PostTopic.GENERAL)
                .type(PostType.TEXT)
                .status(PostStatus.ACTIVE)
                .likeCount(0L)
                .commentCount(0L)
                .build();
    }

    @Test
    void testCreatePost_Success() {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello World");
        request.setTopic(PostTopic.GENERAL);
        request.setType(PostType.TEXT);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        doNothing().when(aiService).moderateContent("Hello World");
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse response = postService.createPost("test@example.com", request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Hello World", response.getOriginalContent());
        verify(aiService).moderateContent("Hello World");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void testGetFeed_WithTopicFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        when(postRepository.findByStatusAndTopic(PostStatus.ACTIVE, PostTopic.TECH, pageable)).thenReturn(page);

        Page<PostResponse> result = postService.getFeed("test@example.com", PostTopic.TECH, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(postRepository).findByStatusAndTopic(PostStatus.ACTIVE, PostTopic.TECH, pageable);
    }
}
