package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.CreateCommentRequest;
import com.mka.dto.response.CommentResponse;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceNotFoundException;
import com.mka.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicAndCommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email("user@test.com").role(Role.USER).active(true).build();
        principal = new UserPrincipal(user);
    }

    @Test
    void testGetTopicComments_ValidNumericId_Returns200() {
        when(commentService.getCommentsByTopicId(eq("user@test.com"), eq(9L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<?> response = commentController.getTopicComments(principal, 9L, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetTopicComments_MissingNumericId_Throws404() {
        when(commentService.getCommentsByTopicId(any(), eq(999L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Topic not found"));

        assertThrows(ResourceNotFoundException.class, () -> {
            commentController.getTopicComments(principal, 999L, 0, 20);
        });
    }

    @Test
    void testCreateTopicComment_ValidAuth_Returns201() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Hello topic");

        when(commentService.createTopicComment(eq("user@test.com"), eq(9L), any(CreateCommentRequest.class)))
                .thenReturn(CommentResponse.builder().id(100L).originalContent("Hello topic").build());

        ResponseEntity<com.mka.dto.response.ApiResponse<CommentResponse>> response =
                commentController.createTopicComment(principal, 9L, req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
    }
}
