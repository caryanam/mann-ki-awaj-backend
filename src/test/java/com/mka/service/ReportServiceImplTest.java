package com.mka.service;

import com.mka.dto.request.CreateReportRequest;
import com.mka.dto.response.ReportResponse;
import com.mka.entity.Post;
import com.mka.entity.Report;
import com.mka.entity.User;
import com.mka.enums.PostStatus;
import com.mka.enums.ReportReason;
import com.mka.enums.ReportStatus;
import com.mka.enums.Role;
import com.mka.repository.CommentRepository;
import com.mka.repository.PostRepository;
import com.mka.repository.ReportRepository;
import com.mka.repository.UserRepository;
import com.mka.service.impl.ReportServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.mka.repository.ProfileRepository profileRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("reporter@example.com").role(Role.USER).build();
        testPost = Post.builder().id(10L).user(testUser).status(PostStatus.ACTIVE).build();
    }

    @Test
    void testReportPost_Success() {
        CreateReportRequest request = new CreateReportRequest();
        request.setReason(ReportReason.HATE_SPEECH);
        request.setDescription("Inappropriate content");

        Report savedReport = Report.builder()
                .id(100L)
                .reporter(testUser)
                .contentType("POST")
                .contentId(10L)
                .reason(ReportReason.HATE_SPEECH)
                .description("Inappropriate content")
                .status(ReportStatus.PENDING)
                .build();

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testUser));
        when(postRepository.findByIdAndStatus(10L, PostStatus.ACTIVE)).thenReturn(Optional.of(testPost));
        when(reportRepository.existsByReporterIdAndContentTypeAndContentId(1L, "POST", 10L)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        ReportResponse response = reportService.reportPost("reporter@example.com", 10L, request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("POST", response.getContentType());
        assertEquals(ReportReason.HATE_SPEECH, response.getReason());
    }

    @Test
    void testGetMyReports_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Report report = Report.builder()
                .id(100L)
                .reporter(testUser)
                .contentType("POST")
                .contentId(10L)
                .reason(ReportReason.HATE_SPEECH)
                .status(ReportStatus.PENDING)
                .build();

        Page<Report> page = new PageImpl<>(List.of(report));

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testUser));
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<ReportResponse> result = reportService.getMyReports("reporter@example.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
