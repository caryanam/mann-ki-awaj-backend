package com.mka.repository;

import com.mka.entity.CustomTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomTopicRepository extends JpaRepository<CustomTopic, Long> {
    Optional<CustomTopic> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
