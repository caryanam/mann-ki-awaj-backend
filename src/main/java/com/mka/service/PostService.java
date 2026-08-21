package com.mka.service;

import com.mka.dto.request.CreatePostRequest;
import com.mka.dto.request.UpdatePostRequest;
import com.mka.dto.response.PostResponse;
import com.mka.enums.PostTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse createPost(String email, CreatePostRequest request);

    Page<PostResponse> getFeed(String email, String topic, Pageable pageable);


    PostResponse getPostById(String email, Long id);

    PostResponse updatePost(String email, Long id, UpdatePostRequest request);

    void deletePost(String email, Long id);
}
