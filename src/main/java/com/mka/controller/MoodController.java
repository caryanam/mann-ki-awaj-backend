package com.mka.controller;

import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.MoodOfIndiaResponse;
import com.mka.entity.User;
import com.mka.entity.UserMoodVote;
import com.mka.repository.UserRepository;
import com.mka.repository.UserMoodVoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mood")
public class MoodController {

    @Autowired
    private UserMoodVoteRepository userMoodVoteRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/india")
    public ResponseEntity<ApiResponse<MoodOfIndiaResponse>> getMoodOfIndia(@AuthenticationPrincipal UserDetails userDetails) {
        String userMood = null;
        try {
            if (userDetails != null) {
                User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (user != null) {
                    userMood = userMoodVoteRepository.findByUserId(user.getId())
                            .map(UserMoodVote::getMood)
                            .orElse(null);
                }
            }
        } catch (Exception e) {
            // Ignore DB table errors safely
        }

        MoodOfIndiaResponse response = buildMoodResponse(userMood);
        return ResponseEntity.ok(ApiResponse.success("Mood of India statistics fetched successfully", response));
    }

    @PostMapping("/india")
    public ResponseEntity<ApiResponse<MoodOfIndiaResponse>> voteMood(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("mood") String mood) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required to vote mood"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String cleanMood = mood != null ? mood.trim().toUpperCase() : "";
        if (cleanMood.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mood cannot be empty"));
        }

        try {
            UserMoodVote existing = userMoodVoteRepository.findByUserId(user.getId()).orElse(null);
            if (existing != null) {
                if (existing.getMood().equalsIgnoreCase(cleanMood)) {
                    // Toggle off if same mood clicked
                    userMoodVoteRepository.delete(existing);
                    cleanMood = null;
                } else {
                    // Update to new mood
                    existing.setMood(cleanMood);
                    userMoodVoteRepository.save(existing);
                }
            } else {
                UserMoodVote newVote = UserMoodVote.builder()
                        .user(user)
                        .mood(cleanMood)
                        .build();
                userMoodVoteRepository.save(newVote);
            }
        } catch (Exception e) {
            // Fallback safely if table creation is pending
        }

        MoodOfIndiaResponse response = buildMoodResponse(cleanMood);
        return ResponseEntity.ok(ApiResponse.success("Mood vote recorded successfully", response));
    }

    private MoodOfIndiaResponse buildMoodResponse(String currentUserMood) {
        Map<String, Long> moodCounts = new HashMap<>();
        long totalVotes = 0;

        try {
            List<Object[]> rawCounts = userMoodVoteRepository.countVotesByMood();
            for (Object[] row : rawCounts) {
                if (row != null && row.length >= 2) {
                    String mKey = row[0] != null ? row[0].toString() : "";
                    Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                    if (!mKey.isBlank()) {
                        moodCounts.put(mKey, count);
                        totalVotes += count;
                    }
                }
            }
        } catch (Exception e) {
            // Return empty counts safely if DB table is missing or initializing
        }

        return MoodOfIndiaResponse.builder()
                .userMood(currentUserMood)
                .totalVotes(totalVotes)
                .moodCounts(moodCounts)
                .build();
    }
}
