package com.mka.service;

import com.mka.dto.request.ReactionRequest;

public interface ReactionService {

    void reactToPost(String email, Long postId, ReactionRequest request);

    void removePostReaction(String email, Long postId);

    void reactToComment(String email, Long commentId, ReactionRequest request);

    void removeCommentReaction(String email, Long commentId);
}
