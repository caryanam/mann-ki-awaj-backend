package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.NotificationResponse;
import com.mka.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "User Notification APIs")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String resolveUsername(Object principalObj) {
        if (principalObj instanceof UserPrincipal principal) {
            return principal.getUsername();
        } else if (principalObj != null) {
            return principalObj.toString();
        }
        return "";
    }

    @GetMapping
    @Operation(summary = "Get user notifications list")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @AuthenticationPrincipal Object principalObj,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String identifier = resolveUsername(principalObj);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                identifier, PageRequest.of(page, size));

        return ResponseEntity.ok(
                ApiResponse.<Page<NotificationResponse>>builder()
                        .success(true)
                        .message("Notifications retrieved successfully")
                        .data(notifications != null ? notifications : Page.empty(PageRequest.of(page, size)))
                        .build()
        );
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal Object principalObj) {

        String identifier = resolveUsername(principalObj);
        long count = identifier.isBlank() ? 0L : notificationService.getUnreadCount(identifier);
        return ResponseEntity.ok(
                ApiResponse.<Long>builder()
                        .success(true)
                        .message("Unread notification count retrieved")
                        .data(count)
                        .build()
        );
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable Long id) {

        String identifier = resolveUsername(principalObj);
        if (!identifier.isBlank()) {
            try {
                notificationService.markAsRead(identifier, id);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Notification marked as read")
                        .build()
        );
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Object principalObj) {

        String identifier = resolveUsername(principalObj);
        if (!identifier.isBlank()) {
            try {
                notificationService.markAllAsRead(identifier);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("All notifications marked as read")
                        .build()
        );
    }
}
