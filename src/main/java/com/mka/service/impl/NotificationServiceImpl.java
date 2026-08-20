package com.mka.service.impl;

import com.corundumstudio.socketio.SocketIOServer;
import com.mka.dto.response.NotificationResponse;
import com.mka.entity.Notification;
import com.mka.entity.User;
import com.mka.enums.NotificationType;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.NotificationRepository;
import com.mka.repository.UserRepository;
import com.mka.repository.ProfileRepository;
import com.mka.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SocketIOServer socketIOServer;

    @Override
    @Transactional
    public void createNotification(User recipient, User sender, String senderAvatar, NotificationType type, String message, Long targetId) {
        if (recipient == null) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .senderAvatar(senderAvatar)
                .type(type)
                .message(message)
                .targetId(targetId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Real-time broadcast to recipient's personal user room
        try {
            NotificationResponse resp = toResponse(saved);
            if (resp != null) {
                socketIOServer.getRoomOperations("user_" + recipient.getId()).sendEvent("new_notification", resp);
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast real-time notification: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String identifier, Pageable pageable) {
        if (identifier == null || identifier.isBlank()) {
            return Page.empty(pageable);
        }

        try {
            User user = userRepository.findByEmail(identifier)
                    .orElseGet(() -> userRepository.findByMobileNumber(identifier).orElse(null));

            if (user == null) {
                return Page.empty(pageable);
            }

            Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable);
            if (notifications == null) {
                return Page.empty(pageable);
            }

            return notifications.map(this::toResponse);
        } catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markAsRead(String email, Long notificationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to user");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        notificationRepository.markAllAsReadByRecipientId(user.getId());
    }

    @Override
    @Transactional
    public void deleteNotification(String identifier, Long notificationId) {
        if (notificationId == null) return;
        try {
            notificationRepository.deleteNotificationByIdCustom(notificationId);
        } catch (Exception e) {
            System.err.println("Notice deleting notification #" + notificationId + ": " + e.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        if (notification == null) return null;

        String senderUsername = "System";
        if (notification.getSender() != null) {
            com.mka.entity.Profile p = profileRepository.findByUser(notification.getSender()).orElse(null);
            if (p != null && p.getUsername() != null && !p.getUsername().isBlank()) {
                String u = p.getUsername().trim();
                senderUsername = u.startsWith("@") ? u : "@" + u;
            } else if (notification.getSender().getEmail() != null) {
                senderUsername = "@" + notification.getSender().getEmail().split("@")[0];
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .type(notification.getType())
                .message(notification.getMessage() != null ? notification.getMessage() : "")
                .isRead(notification.getIsRead() != null ? notification.getIsRead() : false)
                .senderUsername(senderUsername)
                .senderAvatar(notification.getSenderAvatar())
                .targetId(notification.getTargetId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
