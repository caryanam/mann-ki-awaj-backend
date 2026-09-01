package com.mka.repository;

import com.mka.entity.CustomTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.mka.enums.PostTopic;

@Repository
public interface CustomTopicRepository extends JpaRepository<CustomTopic, Long> {
    Optional<CustomTopic> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<CustomTopic> findByParentTopicOrderByLabelAsc(PostTopic parentTopic);

    Page<CustomTopic> findByNameContainingIgnoreCaseOrCreatedByUsernameContainingIgnoreCase(
            String name, String createdByUsername, Pageable pageable);

    Page<CustomTopic> findByParentTopic(PostTopic parentTopic, Pageable pageable);

    Page<CustomTopic> findByParentTopicAndNameContainingIgnoreCase(
            PostTopic parentTopic, String name, Pageable pageable);
}

