package com.mka.service.impl;

import com.corundumstudio.socketio.SocketIOServer;
import com.mka.config.PresenceManager;
import com.mka.dto.request.SendMessageRequest;
import com.mka.dto.response.ChatMessageResponse;
import com.mka.dto.response.ChatRoomResponse;
import com.mka.entity.ChatMessage;
import com.mka.entity.ChatRoom;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.MessageType;
import com.mka.exception.ResourceNotFoundException;
import com.mka.repository.ChatMessageRepository;
import com.mka.repository.ChatRoomRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.enums.NotificationType;
import com.mka.service.NotificationService;
import com.mka.service.AiService;
import com.mka.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AiService aiService;
    private final SocketIOServer socketIOServer;
    private final NotificationService notificationService;
    private final PresenceManager presenceManager;

    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        return userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByMobileNumber(identifier).orElse(null));
    }

    @Override
    @Transactional
    public ChatRoomResponse getOrCreatePrivateRoom(String identifier, Long recipientUserId) {
        User currentUser = findUserByIdentifier(identifier);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found with identifier: " + identifier);
        }

        User recipientUser = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + recipientUserId));

        ChatRoom room = chatRoomRepository.findByUsers(currentUser, recipientUser)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder()
                                .participant1(currentUser)
                                .participant2(recipientUser)
                                .requestStatus("PENDING")
                                .requestSenderId(currentUser.getId())
                                .build()
                ));

        return mapRoomToResponse(room, currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getUserRooms(String identifier) {
        User currentUser = findUserByIdentifier(identifier);
        if (currentUser == null) return List.of();

        List<ChatRoom> rooms = chatRoomRepository.findByParticipant(currentUser);
        return rooms.stream()
                .map(room -> mapRoomToResponse(room, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(String identifier, SendMessageRequest request) {
        User sender = findUserByIdentifier(identifier);
        if (sender == null) {
            throw new ResourceNotFoundException("User not found with identifier: " + identifier);
        }

        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + request.getRoomId()));

        aiService.moderateContent(sender, request.getContent(), "MESSAGE");

        Profile senderProfile = profileRepository.findByUser(sender).orElse(null);
        String avatar = senderProfile != null && senderProfile.getAvatar() != null ? senderProfile.getAvatar() : "avatar_default";

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .senderAvatar(avatar)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        ChatMessageResponse response = mapMessageToResponse(savedMessage);

        // Broadcast the message via Socket.IO to room operations AND personal user rooms
        try {
            socketIOServer.getRoomOperations("room_" + room.getId()).sendEvent("receive_message", response);
            if (room.getParticipant1() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant1().getId()).sendEvent("receive_message", response);
            }
            if (room.getParticipant2() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant2().getId()).sendEvent("receive_message", response);
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast message: " + e.getMessage());
        }

        // Notify recipient about new chat message
        User recipient = room.getParticipant1().getId().equals(sender.getId()) ? room.getParticipant2() : room.getParticipant1();
        if (recipient != null && !recipient.getId().equals(sender.getId())) {
            notificationService.createNotification(
                    recipient,
                    sender,
                    avatar,
                    NotificationType.CHAT_MESSAGE,
                    response.getSenderUsername() + " sent you a message",
                    room.getId()
            );
        }

        return response;
    }

    @Override
    @Transactional
    public Page<ChatMessageResponse> getRoomMessages(String identifier, Long roomId, Pageable pageable) {
        User currentUser = findUserByIdentifier(identifier);
        if (currentUser == null) return Page.empty(pageable);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + roomId));

        if (!room.getParticipant1().getId().equals(currentUser.getId()) &&
                !room.getParticipant2().getId().equals(currentUser.getId())) {
            return Page.empty(pageable);
        }

        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        List<ChatMessage> unread = messagesPage.getContent().stream()
                .filter(m -> !m.getSender().getId().equals(currentUser.getId()) && (m.getIsRead() == null || !m.getIsRead()))
                .peek(m -> m.setIsRead(true))
                .collect(Collectors.toList());

        if (!unread.isEmpty()) {
            chatMessageRepository.saveAll(unread);
        }

        return messagesPage.map(this::mapMessageToResponse);
    }

    @Override
    @Transactional
    public ChatRoomResponse acceptRoomRequest(String identifier, Long roomId) {
        User currentUser = findUserByIdentifier(identifier);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found: " + identifier);
        }
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        if ("ACCEPTED".equalsIgnoreCase(room.getRequestStatus())) {
            return mapRoomToResponse(room, currentUser);
        }

        room.setRequestStatus("ACCEPTED");
        ChatRoom saved = chatRoomRepository.save(room);
        ChatRoomResponse resp = mapRoomToResponse(saved, currentUser);

        // Broadcast status change via Socket.IO to room and user channels
        try {
            socketIOServer.getRoomOperations("room_" + roomId).sendEvent("room_status_change", resp);
            if (room.getParticipant1() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant1().getId()).sendEvent("room_status_change", resp);
            }
            if (room.getParticipant2() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant2().getId()).sendEvent("room_status_change", resp);
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast room_status_change: " + e.getMessage());
        }
        return resp;
    }

    @Override
    @Transactional
    public ChatRoomResponse rejectRoomRequest(String identifier, Long roomId) {
        User currentUser = findUserByIdentifier(identifier);
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found: " + identifier);
        }
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        if (room.getRequestSenderId() != null && room.getRequestSenderId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Sender cannot reject their own request.");
        }

        room.setRequestStatus("REJECTED");
        ChatRoom saved = chatRoomRepository.save(room);
        ChatRoomResponse resp = mapRoomToResponse(saved, currentUser);

        // Broadcast status change via Socket.IO to room and user channels
        try {
            socketIOServer.getRoomOperations("room_" + roomId).sendEvent("room_status_change", resp);
            if (room.getParticipant1() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant1().getId()).sendEvent("room_status_change", resp);
            }
            if (room.getParticipant2() != null) {
                socketIOServer.getRoomOperations("user_" + room.getParticipant2().getId()).sendEvent("room_status_change", resp);
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast room_status_change: " + e.getMessage());
        }
        return resp;
    }

    private ChatRoomResponse mapRoomToResponse(ChatRoom room, User currentUser) {
        User otherUser = room.getParticipant1().getId().equals(currentUser.getId())
                ? room.getParticipant2()
                : room.getParticipant1();

        Profile otherProfile = profileRepository.findByUser(otherUser).orElse(null);
        String otherAvatar = otherProfile != null && otherProfile.getAvatar() != null ? otherProfile.getAvatar() : "avatar_default";
        String otherUsername = otherProfile != null && otherProfile.getUsername() != null ? otherProfile.getUsername() : (otherUser.getUsername() != null ? otherUser.getUsername() : "user_" + otherUser.getId());

        Profile p1Profile = profileRepository.findByUser(room.getParticipant1()).orElse(null);
        Profile p2Profile = profileRepository.findByUser(room.getParticipant2()).orElse(null);

        String p1Username = p1Profile != null && p1Profile.getUsername() != null ? p1Profile.getUsername() : (room.getParticipant1().getUsername() != null ? room.getParticipant1().getUsername() : "user_" + room.getParticipant1().getId());
        String p2Username = p2Profile != null && p2Profile.getUsername() != null ? p2Profile.getUsername() : (room.getParticipant2().getUsername() != null ? room.getParticipant2().getUsername() : "user_" + room.getParticipant2().getId());

        ChatMessage lastMsg = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getId()).orElse(null);
        long unreadCount = currentUser != null ? chatMessageRepository.countByRoomIdAndSenderIdNotAndIsReadFalse(room.getId(), currentUser.getId()) : 0L;

        boolean isOtherOnline = presenceManager.isUserOnline(otherUser.getId());
        LocalDateTime otherLastSeen = presenceManager.getLastSeen(otherUser.getId());

        return ChatRoomResponse.builder()
                .id(room.getId())
                .participant1Id(room.getParticipant1().getId())
                .participant2Id(room.getParticipant2().getId())
                .participant1Username(p1Username)
                .participant2Username(p2Username)
                .participant1Avatar(p1Profile != null ? p1Profile.getAvatar() : "avatar_default")
                .participant2Avatar(p2Profile != null ? p2Profile.getAvatar() : "avatar_default")
                .otherParticipantId(otherUser.getId())
                .otherParticipantUsername(otherUsername)
                .otherParticipantAvatar(otherAvatar)
                .otherParticipantIsOnline(isOtherOnline)
                .otherParticipantLastSeen(otherLastSeen)
                .lastMessage(lastMsg != null ? mapMessageToResponse(lastMsg) : null)
                .unreadCount(unreadCount)
                .hasUnread(unreadCount > 0)
                .requestStatus(room.getRequestStatus())
                .requestSenderId(room.getRequestSenderId())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse mapMessageToResponse(ChatMessage message) {
        Profile senderProfile = profileRepository.findByUser(message.getSender()).orElse(null);
        String senderUsername = senderProfile != null && senderProfile.getUsername() != null ? senderProfile.getUsername() : (message.getSender().getUsername() != null ? message.getSender().getUsername() : "user_" + message.getSender().getId());

        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderUsername(senderUsername)
                .senderAvatar(message.getSenderAvatar())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
