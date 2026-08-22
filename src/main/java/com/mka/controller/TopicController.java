package com.mka.controller;

import com.mka.entity.CustomTopic;
import com.mka.repository.CustomTopicRepository;
import com.mka.repository.CommentRepository;
import com.mka.enums.CommentStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mka.enums.PostTopic;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TopicController {

    private final CustomTopicRepository customTopicRepository;
    private final CommentRepository commentRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTopics(
            @RequestParam(required = false) PostTopic parentTopic) {
        List<CustomTopic> topics = parentTopic == null
                ? customTopicRepository.findAll()
                : customTopicRepository.findByParentTopicOrderByLabelAsc(parentTopic);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", topics.stream().map(topic -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", topic.getId());
            item.put("name", topic.getName());
            item.put("label", topic.getLabel());
            item.put("icon", topic.getIcon());
            item.put("createdByUsername", topic.getCreatedByUsername());
            item.put("parentTopic", topic.getParentTopic());
            item.put("createdAt", topic.getCreatedAt());
            item.put("commentCount", commentRepository.countByCustomTopicIdAndStatus(topic.getId(), CommentStatus.ACTIVE));
            return item;
        }).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/parents")
    public ResponseEntity<Map<String, Object>> getParentTopics() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", List.of(
                PostTopic.FEELINGS.name(),
                PostTopic.EXPRESSION.name(),
                PostTopic.LIFE_WORK.name(),
                PostTopic.SOCIETY_POLITICS.name(),
                PostTopic.ENTERTAINMENT.name(),
                PostTopic.SPORTS.name(),
                PostTopic.GENERAL.name()
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTopic(@RequestBody TopicRequest request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Topic name is required");
            return ResponseEntity.badRequest().body(err);
        }

        String cleanName = request.getName().trim().toUpperCase().replaceAll("[^A-Z0-9_]", "");
        String icon = request.getIcon() != null && !request.getIcon().trim().isEmpty() ? request.getIcon().trim() : "💡";

        CustomTopic existing = customTopicRepository.findByNameIgnoreCase(cleanName).orElse(null);
        if (existing != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", existing);
            return ResponseEntity.ok(response);
        }

        CustomTopic topic = CustomTopic.builder()
                .name(cleanName)
                .label(cleanName.replace("_", " "))
                .icon(icon)
                .createdByUsername(request.getCreatedByUsername() != null ? request.getCreatedByUsername() : "@anonymous")
                .parentTopic(request.getParentTopic() != null ? request.getParentTopic() : PostTopic.GENERAL)
                .build();

        CustomTopic saved = customTopicRepository.save(topic);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @Data
    public static class TopicRequest {
        private String name;
        private String icon;
        private String createdByUsername;
        private PostTopic parentTopic;
    }
}
