package com.mka.service;

public interface LikeService {

    void likePost(String email, Long postId);

    void unlikePost(String email, Long postId);

    void likeComment(String email, Long commentId);

    void unlikeComment(String email, Long commentId);
}
