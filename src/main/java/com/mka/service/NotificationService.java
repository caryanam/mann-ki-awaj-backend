package com.mka.service;

import com.mka.dto.response.NotificationResponse;
import com.mka.entity.User;
import com.mka.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void createNotification(User recipient, User sender, String senderAvatar, NotificationType type, String message, Long targetId);

    Page<NotificationResponse> getUserNotifications(String email, Pageable pageable);

    long getUnreadCount(String email);

    void markAsRead(String email, Long notificationId);

    void markAllAsRead(String email);
}
