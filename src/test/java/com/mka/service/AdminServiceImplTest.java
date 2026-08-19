package com.mka.service;

import com.mka.dto.request.SendWarningRequest;
import com.mka.dto.response.AdminDashboardResponse;
import com.mka.dto.response.AdminUserResponse;
import com.mka.dto.response.ReviewQueueResponse;
import com.mka.entity.ContentReviewQueue;
import com.mka.entity.User;
import com.mka.enums.NotificationType;
import com.mka.enums.PostStatus;
import com.mka.enums.ReportStatus;
import com.mka.enums.ReviewStatus;
import com.mka.enums.Role;
import com.mka.enums.WarningLevel;
import com.mka.repository.*;
import com.mka.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContentReviewQueueRepository reviewQueueRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User targetUser;

    @BeforeEach
    void setUp() {
        targetUser = User.builder()
                .id(10L)
                .email("user10@example.com")
                .fullName("User Ten")
                .role(Role.USER)
                .active(true)
                .build();
    }

    @Test
    void testGetDashboardStats() {
        when(userRepository.count()).thenReturn(100L);
        when(postRepository.countByStatus(PostStatus.ACTIVE)).thenReturn(450L);
        when(commentRepository.count()).thenReturn(200L);
        when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(5L);
        when(userRepository.countByActiveFalse()).thenReturn(2L);

        AdminDashboardResponse stats = adminService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(100L, stats.getTotalUsers());
        assertEquals(450L, stats.getTotalPosts());
        assertEquals(5L, stats.getTotalPendingReports());
        assertEquals(2L, stats.getTotalBlockedUsers());
    }

    @Test
    void testBlockUser() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));

        adminService.blockUser(10L);

        assertFalse(targetUser.getActive());
        verify(userRepository).save(targetUser);
        verify(notificationService).createNotification(
                eq(targetUser),
                isNull(),
                eq("system_avatar"),
                eq(NotificationType.ACCOUNT_BLOCKED),
                anyString(),
                isNull()
        );
    }

    @Test
    void testSendWarning() {
        SendWarningRequest request = new SendWarningRequest();
        request.setWarningLevel(WarningLevel.FIRST);
        request.setMessage("Repeated abusive language violation");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));

        adminService.sendWarning(10L, request);

        verify(notificationService).createNotification(
                eq(targetUser),
                isNull(),
                eq("system_avatar"),
                eq(NotificationType.WARNING),
                contains("Strike 1 Warning: Repeated abusive language violation"),
                isNull()
        );
    }

    @Test
    void testApproveQueueItem() {
        ContentReviewQueue queueItem = ContentReviewQueue.builder()
                .id(1L)
                .status(ReviewStatus.PENDING_REVIEW)
                .build();

        when(reviewQueueRepository.findById(1L)).thenReturn(Optional.of(queueItem));

        adminService.approveQueueItem(1L);

        assertEquals(ReviewStatus.APPROVED, queueItem.getStatus());
        verify(reviewQueueRepository).save(queueItem);
    }

    @Test
    void testResolveReport_Success() {
        com.mka.entity.Report report = com.mka.entity.Report.builder()
                .id(50L)
                .status(ReportStatus.PENDING)
                .build();

        when(reportRepository.findById(50L)).thenReturn(Optional.of(report));

        adminService.resolveReport(50L);

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        verify(reportRepository).save(report);
    }

    @Test
    void testResolveReport_AlreadyResolved_ThrowsResourceAlreadyExistsException() {
        com.mka.entity.Report report = com.mka.entity.Report.builder()
                .id(50L)
                .status(ReportStatus.RESOLVED)
                .build();

        when(reportRepository.findById(50L)).thenReturn(Optional.of(report));

        assertThrows(com.mka.exception.ResourceAlreadyExistsException.class, () ->
                adminService.resolveReport(50L)
        );
    }

    @Test
    void testRejectReport_AlreadyRejected_ThrowsResourceAlreadyExistsException() {
        com.mka.entity.Report report = com.mka.entity.Report.builder()
                .id(50L)
                .status(ReportStatus.REJECTED)
                .build();

        when(reportRepository.findById(50L)).thenReturn(Optional.of(report));

        assertThrows(com.mka.exception.ResourceAlreadyExistsException.class, () ->
                adminService.rejectReport(50L)
        );
    }
}
