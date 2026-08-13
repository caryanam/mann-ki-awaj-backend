package com.mka.scheduler;

import com.mka.enums.PostStatus;
import com.mka.repository.PostRepository;
import com.mka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // Runs daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeSoftDeletedItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Starting cleanup scheduler task for soft-deleted posts and accounts (cutoff: {})", cutoff);

        try {
            int purgedPostsCount = postRepository.deleteByStatusAndUpdatedAtBefore(PostStatus.DELETED, cutoff);
            log.info("Successfully purged {} soft-deleted posts permanently from database.", purgedPostsCount);
        } catch (Exception ex) {
            log.error("Error purging soft-deleted posts: {}", ex.getMessage(), ex);
        }

        try {
            int purgedUsersCount = userRepository.deleteByDeletedTrueAndUpdatedAtBefore(cutoff);
            log.info("Successfully purged {} soft-deleted user accounts permanently from database.", purgedUsersCount);
        } catch (Exception ex) {
            log.error("Error purging soft-deleted user accounts: {}", ex.getMessage(), ex);
        }
    }
}
