package com.mka.service;

import com.mka.dto.response.CommentResponse;
import com.mka.entity.Comment;
import com.mka.entity.CustomTopic;
import com.mka.entity.Post;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.CommentStatus;
import com.mka.enums.PostStatus;
import com.mka.enums.ReactionType;
import com.mka.enums.Role;
import com.mka.repository.*;
import com.mka.service.impl.CommentServiceImpl;
import com.mka.translation.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CustomTopicRepository customTopicRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private AiService aiService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User testUser;
    private Post testPost;
    private Comment rootComment;
    private Comment replyComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .build();

        testPost = Post.builder()
                .id(100L)
                .user(testUser)
                .status(PostStatus.ACTIVE)
                .commentCount(1L)
                .build();

        rootComment = Comment.builder()
                .id(10L)
                .post(testPost)
                .user(testUser)
                .authorAvatar("avatar_default")
                .originalContent("Root comment")
                .originalLanguage("EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(2L)
                .createdAt(LocalDateTime.now())
                .build();

        replyComment = Comment.builder()
                .id(11L)
                .post(testPost)
                .parentComment(rootComment)
                .user(testUser)
                .authorAvatar("avatar_default")
                .originalContent("Reply comment")
                .originalLanguage("EN")
                .status(CommentStatus.ACTIVE)
                .likeCount(1L)
                .createdAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Test
    void testGetCommentsByPostId_Authenticated_BatchQueriesCalledOnce() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> commentPage = new PageImpl<>(List.of(rootComment), pageable, 1);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(commentRepository.findByPostIdAndParentCommentIsNullAndStatus(eq(100L), eq(CommentStatus.ACTIVE), eq(pageable)))
                .thenReturn(commentPage);
        when(commentRepository.findByParentCommentIdInAndStatus(eq(List.of(10L)), eq(CommentStatus.ACTIVE)))
                .thenReturn(List.of(replyComment));
        when(profileRepository.findByUserIdIn(any())).thenReturn(List.of(Profile.builder().user(testUser).username("testuser").preferredLanguage("EN").build()));

        CommentReactionRepository.CommentReactionCountProjection projection = mock(CommentReactionRepository.CommentReactionCountProjection.class);
        when(projection.getCommentId()).thenReturn(10L);
        when(projection.getReactionType()).thenReturn(ReactionType.AGREE);
        when(projection.getCount()).thenReturn(2L);
        when(commentReactionRepository.findReactionCountsByCommentIdIn(List.of(10L, 11L)))
                .thenReturn(List.of(projection));

        when(commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIdIn(eq(1L), eq(List.of(10L, 11L))))
                .thenReturn(List.of(10L));

        Page<CommentResponse> result = commentService.getCommentsByPostId("user@example.com", 100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        CommentResponse rootResp = result.getContent().get(0);
        assertEquals(10L, rootResp.getId());
        assertTrue(rootResp.isLikedByCurrentUser());
        assertEquals(1, rootResp.getReplies().size());
        assertEquals(11L, rootResp.getReplies().get(0).getId());

        verify(commentRepository, times(1)).findByParentCommentIdInAndStatus(any(), any());
        verify(profileRepository, times(1)).findByUserIdIn(any());
        verify(commentReactionRepository, times(1)).findReactionCountsByCommentIdIn(any());
        verify(commentLikeRepository, times(1)).findLikedCommentIdsByUserIdAndCommentIdIn(any(), any());

        verify(commentRepository, never()).findByParentCommentIdAndStatus(any(), any());
        verify(commentReactionRepository, never()).countByCommentIdAndReactionType(any(), any());
        verify(commentLikeRepository, never()).existsByCommentIdAndUserId(any(), any());
    }

    @Test
    void testGetCommentsByPostId_Anonymous_UserQueriesNeverCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> commentPage = new PageImpl<>(List.of(rootComment), pageable, 1);

        when(commentRepository.findByPostIdAndParentCommentIsNullAndStatus(eq(100L), eq(CommentStatus.ACTIVE), eq(pageable)))
                .thenReturn(commentPage);
        when(commentRepository.findByParentCommentIdInAndStatus(eq(List.of(10L)), eq(CommentStatus.ACTIVE)))
                .thenReturn(List.of());
        when(profileRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(commentReactionRepository.findReactionCountsByCommentIdIn(List.of(10L))).thenReturn(List.of());

        Page<CommentResponse> result = commentService.getCommentsByPostId(null, 100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertFalse(result.getContent().get(0).isLikedByCurrentUser());

        verify(commentLikeRepository, never()).findLikedCommentIdsByUserIdAndCommentIdIn(any(), any());
        verify(commentLikeRepository, never()).existsByCommentIdAndUserId(any(), any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void testGetCommentsByPostId_EmptyResult_NoBatchQueriesExecuted() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(commentRepository.findByPostIdAndParentCommentIsNullAndStatus(eq(100L), eq(CommentStatus.ACTIVE), eq(pageable)))
                .thenReturn(emptyPage);

        Page<CommentResponse> result = commentService.getCommentsByPostId(null, 100L, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());

        verify(commentRepository, never()).findByParentCommentIdInAndStatus(any(), any());
        verify(commentReactionRepository, never()).findReactionCountsByCommentIdIn(any());
        verify(commentLikeRepository, never()).findLikedCommentIdsByUserIdAndCommentIdIn(any(), any());
    }

    @Test
    void testCreateTopicComment_PersistsWithoutPostAndKeepsLegacyDataUntouched() {
        CustomTopic topic = CustomTopic.builder().id(200L).name("INTERSTELLAR")
                .label("INTERSTELLAR").parentTopic(com.mka.enums.PostTopic.MOVIE_REVIEW).build();
        com.mka.dto.request.CreateCommentRequest request =
                new com.mka.dto.request.CreateCommentRequest("A thoughtful movie opinion", "EN");
        request.setImageUrl("/uploads/opinion.jpg");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(customTopicRepository.findById(200L)).thenReturn(Optional.of(topic));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        CommentResponse response = commentService.createTopicComment("user@example.com", 200L, request);

        assertEquals(20L, response.getId());
        assertEquals(200L, response.getTopicId());
        assertNull(response.getPostId());
        assertEquals("https://api.awaazmanki.com/uploads/opinion.jpg", response.getImageUrl());
        verify(postRepository, never()).save(any());
        verify(postRepository, never()).findById(any());
    }

    @Test
    void testReplyToTopicComment_InheritsTopicWithoutPost() {
        CustomTopic topic = CustomTopic.builder().id(200L).name("INTERSTELLAR").build();
        Comment topicComment = Comment.builder().id(20L).customTopic(topic).user(testUser)
                .authorAvatar("avatar_default").originalContent("Root opinion")
                .originalLanguage("EN").status(CommentStatus.ACTIVE).build();
        com.mka.dto.request.CreateCommentRequest request =
                new com.mka.dto.request.CreateCommentRequest("Reply", "EN");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdAndStatus(20L, CommentStatus.ACTIVE)).thenReturn(Optional.of(topicComment));
        when(profileRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        CommentResponse response = commentService.replyToComment("user@example.com", 20L, request);

        assertEquals(200L, response.getTopicId());
        assertNull(response.getPostId());
        verify(postRepository, never()).save(any());
    }
}
