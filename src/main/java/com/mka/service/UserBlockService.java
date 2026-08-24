package com.mka.service;

import com.mka.entity.User;
import com.mka.entity.UserBlock;
import com.mka.repository.UserBlockRepository;
import com.mka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void blockUser(String userIdentifier, String targetUsername) {
        if (userIdentifier == null || userIdentifier.isBlank() || targetUsername == null || targetUsername.trim().isEmpty()) return;
        String cleanHandle = targetUsername.trim().replace("@", "");

        User blocker = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (blocker == null) return;

        if (!userBlockRepository.existsByBlockerIdAndBlockedUsernameIgnoreCase(blocker.getId(), cleanHandle)) {
            UserBlock block = UserBlock.builder()
                    .blocker(blocker)
                    .blockedUsername(cleanHandle)
                    .build();
            userBlockRepository.save(block);
        }
    }

    @Transactional
    public void unblockUser(String userIdentifier, String targetUsername) {
        if (userIdentifier == null || userIdentifier.isBlank() || targetUsername == null || targetUsername.trim().isEmpty()) return;
        String cleanHandle = targetUsername.trim().replace("@", "");

        User blocker = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (blocker == null) return;

        userBlockRepository.deleteByBlockerIdAndBlockedUsernameIgnoreCase(blocker.getId(), cleanHandle);
    }

    @Transactional(readOnly = true)
    public List<String> getBlockedUsers(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) return Collections.emptyList();

        User blocker = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (blocker == null) return Collections.emptyList();

        return userBlockRepository.findByBlockerId(blocker.getId())
                .stream()
                .map(UserBlock::getBlockedUsername)
                .collect(Collectors.toList());
    }
}
