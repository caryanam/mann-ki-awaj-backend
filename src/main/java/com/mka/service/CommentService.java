package com.mka.service;

import com.mka.dto.request.CreateCommentRequest;
import com.mka.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse createComment(String email, Long postId, CreateCommentRequest request);

    CommentResponse createTopicComment(String email, Long topicId, CreateCommentRequest request);

    CommentResponse replyToComment(String email, Long commentId, CreateCommentRequest request);

    Page<CommentResponse> getCommentsByPostId(String email, Long postId, Pageable pageable);

    Page<CommentResponse> getCommentsByTopicId(String email, Long topicId, Pageable pageable);

    CommentResponse updateComment(String email, Long id, CreateCommentRequest request);

    void deleteComment(String email, Long id);
}
