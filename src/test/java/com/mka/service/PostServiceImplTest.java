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
        doNothing().when(aiService).moderateContent(any(), any(), any());
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostResponse response = postService.createPost("test@example.com", request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Hello World", response.getOriginalContent());
        verify(aiService).moderateContent(any(), any(), any());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void testGetFeed_WithTopicFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        when(postRepository.findByStatusAndTopic(PostStatus.ACTIVE, PostTopic.TECH, pageable)).thenReturn(page);
        when(profileRepository.findByUserIdIn(anyList())).thenReturn(List.of(testProfile));
        when(postReactionRepository.findReactionCountsByPostIdIn(anyList())).thenReturn(Collections.emptyList());
        when(postReactionRepository.findByUserIdAndPostIdIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(postLikeRepository.findLikedPostIdsByUserIdAndPostIdIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(savedPostRepository.findSavedPostIdsByUserIdAndPostIdIn(anyLong(), anyList())).thenReturn(Collections.emptyList());

        Page<PostResponse> result = postService.getFeed("test@example.com", PostTopic.TECH, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(postRepository).findByStatusAndTopic(PostStatus.ACTIVE, PostTopic.TECH, pageable);
    }

    @Test
    void testGetFeed_AuthenticatedUser_UsesBatchQueriesSingleTime() {
        Pageable pageable = PageRequest.of(0, 10);
        Post post2 = Post.builder().id(20L).user(testUser).originalContent("Second Post").status(PostStatus.ACTIVE).build();
        Page<Post> page = new PageImpl<>(List.of(testPost, post2));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.of(testProfile));
        when(postRepository.findByStatus(PostStatus.ACTIVE, pageable)).thenReturn(page);

        when(profileRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of(testProfile));

        PostReactionRepository.PostReactionCountProjection proj1 = mock(PostReactionRepository.PostReactionCountProjection.class);
        when(proj1.getPostId()).thenReturn(10L);
        when(proj1.getReactionType()).thenReturn(com.mka.enums.ReactionType.RELATE);
        when(proj1.getCount()).thenReturn(5L);

        when(postReactionRepository.findReactionCountsByPostIdIn(List.of(10L, 20L))).thenReturn(List.of(proj1));

        com.mka.entity.PostReaction userReaction1 = com.mka.entity.PostReaction.builder()
                .post(testPost).user(testUser).reactionType(com.mka.enums.ReactionType.RELATE).build();
        when(postReactionRepository.findByUserIdAndPostIdIn(1L, List.of(10L, 20L))).thenReturn(List.of(userReaction1));

        when(postLikeRepository.findLikedPostIdsByUserIdAndPostIdIn(1L, List.of(10L, 20L))).thenReturn(List.of(10L));
        when(savedPostRepository.findSavedPostIdsByUserIdAndPostIdIn(1L, List.of(10L, 20L))).thenReturn(List.of(20L));

        Page<PostResponse> result = postService.getFeed("test@example.com", null, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        PostResponse resp1 = result.getContent().get(0);
        assertEquals(10L, resp1.getId());
        assertTrue(resp1.isLikedByCurrentUser());
        assertFalse(resp1.isSavedByCurrentUser());
        assertEquals(com.mka.enums.ReactionType.RELATE, resp1.getUserReaction());
        assertEquals(5L, resp1.getReactionCounts().get(com.mka.enums.ReactionType.RELATE));

        PostResponse resp2 = result.getContent().get(1);
        assertEquals(20L, resp2.getId());
        assertFalse(resp2.isLikedByCurrentUser());
        assertTrue(resp2.isSavedByCurrentUser());
        assertNull(resp2.getUserReaction());

        // Verify batch queries were invoked exactly ONCE for the feed page of 2 posts
        verify(profileRepository, times(1)).findByUserIdIn(anyList());
        verify(postReactionRepository, times(1)).findReactionCountsByPostIdIn(anyList());
        verify(postReactionRepository, times(1)).findByUserIdAndPostIdIn(anyLong(), anyList());
        verify(postLikeRepository, times(1)).findLikedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());
        verify(savedPostRepository, times(1)).findSavedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());

        // Verify old per-post query methods were NEVER invoked during feed mapping
        verify(postLikeRepository, never()).existsByPostIdAndUserId(anyLong(), anyLong());
        verify(postReactionRepository, never()).findByPostIdAndUserId(anyLong(), anyLong());
        verify(postReactionRepository, never()).countByPostIdAndReactionType(anyLong(), any());
        verify(savedPostRepository, never()).existsByUserIdAndPostId(anyLong(), anyLong());
    }

    @Test
    void testGetFeed_AnonymousUser_SkipsUserSpecificQueries() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(testPost));

        when(postRepository.findByStatus(PostStatus.ACTIVE, pageable)).thenReturn(page);
        when(profileRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of(testProfile));
        when(postReactionRepository.findReactionCountsByPostIdIn(List.of(10L))).thenReturn(Collections.emptyList());

        Page<PostResponse> result = postService.getFeed(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        PostResponse resp = result.getContent().get(0);
        assertFalse(resp.isLikedByCurrentUser());
        assertFalse(resp.isSavedByCurrentUser());
        assertNull(resp.getUserReaction());

        // Verify batch queries for public data ran once
        verify(profileRepository, times(1)).findByUserIdIn(anyList());
        verify(postReactionRepository, times(1)).findReactionCountsByPostIdIn(anyList());

        // Verify user-specific batch queries were NEVER invoked for anonymous feed
        verify(postReactionRepository, never()).findByUserIdAndPostIdIn(anyLong(), anyList());
        verify(postLikeRepository, never()).findLikedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());
        verify(savedPostRepository, never()).findSavedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());
    }

    @Test
    void testGetFeed_EmptyFeed_ZeroBatchQueriesExecuted() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(Collections.emptyList());

        when(postRepository.findByStatus(PostStatus.ACTIVE, pageable)).thenReturn(emptyPage);

        Page<PostResponse> result = postService.getFeed(null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        verify(profileRepository, never()).findByUserIdIn(anyList());
        verify(postReactionRepository, never()).findReactionCountsByPostIdIn(anyList());
        verify(postReactionRepository, never()).findByUserIdAndPostIdIn(anyLong(), anyList());
        verify(postLikeRepository, never()).findLikedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());
        verify(savedPostRepository, never()).findSavedPostIdsByUserIdAndPostIdIn(anyLong(), anyList());
    }
}
