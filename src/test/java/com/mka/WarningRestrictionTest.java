package com.mka;

import com.mka.dto.request.CreatePostRequest;
import com.mka.dto.request.SendWarningRequest;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.enums.WarningLevel;
import com.mka.repository.UserRepository;
import com.mka.service.AdminService;
import com.mka.service.PostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@Transactional
public class WarningRestrictionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PostService postService;

    private User testUser;

    @BeforeEach
    public void setup() {
        String testEmail = "test_warning_user_" + System.currentTimeMillis() + "@mka.com";
        testUser = User.builder()
                .fullName("Test Warning User")
                .email(testEmail)
                .mobileNumber("999" + (System.currentTimeMillis() % 10000000))
                .password("encoded_pass")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .emailVerified(true)
                .mobileVerified(true)
                .warningCount(0)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Verify that issuing 2nd Warning restricts user from creating posts for 48 hours")
    public void testPostingRestrictionAfterSecondWarning() {
        // 1. Issue 1st Warning
        adminService.sendWarning(testUser.getId(), SendWarningRequest.builder()
                .warningLevel(WarningLevel.FIRST)
                .message("First mild warning for inappropriate title")
                .build());

        User afterFirstWarning = userRepository.findById(testUser.getId()).orElseThrow();
        Assertions.assertEquals(1, afterFirstWarning.getWarningCount());
        Assertions.assertTrue(afterFirstWarning.getActive());

        // 2. Issue 2nd Warning - User should now be restricted (muted for 48 hours)
        adminService.sendWarning(testUser.getId(), SendWarningRequest.builder()
                .warningLevel(WarningLevel.SECOND)
                .message("Second warning for policy violation")
                .build());

        User afterSecondWarning = userRepository.findById(testUser.getId()).orElseThrow();
        Assertions.assertEquals(2, afterSecondWarning.getWarningCount());
        Assertions.assertNotNull(afterSecondWarning.getMutedUntil());
        Assertions.assertTrue(LocalDateTime.now().isBefore(afterSecondWarning.getMutedUntil()));

        // 3. Attempt to create post after 2nd Warning - Should fail with IllegalArgumentException
        CreatePostRequest postReq = CreatePostRequest.builder()
                .title("Test Restricted Post")
                .content("This post should be blocked because user has received 2nd warning.")
                .build();

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> postService.createPost(testUser.getEmail(), postReq)
        );

        Assertions.assertTrue(exception.getMessage().contains("restricted from creating new posts"));
        System.out.println("✅ TEST PASSED: Post creation blocked after 2nd warning with message: " + exception.getMessage());
    }

    @Test
    @DisplayName("Verify that 3rd Warning results in permanent account suspension")
    public void testAccountSuspensionAfterThirdWarning() {
        adminService.sendWarning(testUser.getId(), SendWarningRequest.builder()
                .warningLevel(WarningLevel.FINAL)
                .message("Third strike warning for repeated violations")
                .build());

        User afterFinalWarning = userRepository.findById(testUser.getId()).orElseThrow();
        Assertions.assertFalse(afterFinalWarning.getActive(), "Account must be suspended (active = false)");

        CreatePostRequest postReq = CreatePostRequest.builder()
                .title("Test Post After Ban")
                .content("This post should be blocked due to account suspension.")
                .build();

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> postService.createPost(testUser.getEmail(), postReq)
        );

        Assertions.assertTrue(exception.getMessage().contains("permanently suspended"));
        System.out.println("✅ TEST PASSED: Account suspended after 3rd warning with message: " + exception.getMessage());
    }
}
