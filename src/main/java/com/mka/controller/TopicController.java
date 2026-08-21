package com.mka.controller;

import com.mka.entity.CustomTopic;
import com.mka.repository.CustomTopicRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TopicController {

    private final CustomTopicRepository customTopicRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTopics() {
        List<CustomTopic> topics = customTopicRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", topics);
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
    }
}
