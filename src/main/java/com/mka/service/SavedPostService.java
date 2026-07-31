package com.mka.service;

import com.mka.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SavedPostService {

    void savePost(String email, Long postId);

    void unsavePost(String email, Long postId);

    Page<PostResponse> getSavedPosts(String email, Pageable pageable);
}
