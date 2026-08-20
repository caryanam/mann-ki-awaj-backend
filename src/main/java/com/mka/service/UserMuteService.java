package com.mka.service;

import com.mka.entity.User;
import com.mka.entity.UserMute;
import com.mka.repository.UserMuteRepository;
import com.mka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMuteService {

    private final UserMuteRepository userMuteRepository;
    private final UserRepository userRepository;

    @Transactional
    public void muteUser(String userIdentifier, String targetUsername) {
        if (userIdentifier == null || userIdentifier.isBlank() || targetUsername == null || targetUsername.trim().isEmpty()) return;
        String cleanHandle = targetUsername.trim().replace("@", "");

        User muter = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (muter == null) return;

        if (!userMuteRepository.existsByMuterIdAndMutedUsernameIgnoreCase(muter.getId(), cleanHandle)) {
            UserMute mute = UserMute.builder()
                    .muter(muter)
                    .mutedUsername(cleanHandle)
                    .build();
            userMuteRepository.save(mute);
        }
    }

    @Transactional
    public void unmuteUser(String userIdentifier, String targetUsername) {
        if (userIdentifier == null || userIdentifier.isBlank() || targetUsername == null || targetUsername.trim().isEmpty()) return;
        String cleanHandle = targetUsername.trim().replace("@", "");

        User muter = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (muter == null) return;

        userMuteRepository.deleteByMuterIdAndMutedUsernameIgnoreCase(muter.getId(), cleanHandle);
    }

    @Transactional(readOnly = true)
    public List<String> getMutedUsers(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) return Collections.emptyList();

        User muter = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByMobileNumber(userIdentifier).orElse(null));
        if (muter == null) return Collections.emptyList();

        return userMuteRepository.findByMuterId(muter.getId())
                .stream()
                .map(UserMute::getMutedUsername)
                .collect(Collectors.toList());
    }
}
