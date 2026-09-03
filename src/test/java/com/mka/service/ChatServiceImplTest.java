package com.mka.service;

import com.mka.dto.request.SendMessageRequest;
import com.mka.dto.response.ChatMessageResponse;
import com.mka.dto.response.ChatRoomResponse;
import com.mka.entity.ChatMessage;
import com.mka.entity.ChatRoom;
import com.mka.entity.Profile;
import com.mka.entity.User;
import com.mka.enums.MessageType;
import com.mka.enums.Role;
import com.mka.exception.GlobalExceptionHandler;
import com.mka.repository.ChatMessageRepository;
import com.mka.repository.ChatRoomRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.impl.ChatServiceImpl;
import com.corundumstudio.socketio.BroadcastOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AiService aiService;

    @Mock
    private com.corundumstudio.socketio.SocketIOServer socketIOServer;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.mka.config.PresenceManager presenceManager;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User senderUser;
    private User targetUser;
    private ChatRoom chatRoom;

    @Mock
    private BroadcastOperations broadcastOperations;

    @BeforeEach
    void setUp() {
        senderUser = User.builder().id(1L).email("sender@example.com").role(Role.USER).build();
        targetUser = User.builder().id(2L).email("target@example.com").role(Role.USER).build();

        chatRoom = ChatRoom.builder()
                .id(100L)
                .user1(senderUser)
                .user2(targetUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testGetOrCreatePrivateRoom_ExistingRoom() {
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(chatRoomRepository.findByUsers(senderUser, targetUser)).thenReturn(Optional.of(chatRoom));
        when(profileRepository.findByUser(targetUser)).thenReturn(Optional.of(
                Profile.builder().user(targetUser).avatar("avatar_2").username("user_2").build()
        ));

        ChatRoomResponse response = chatService.getOrCreatePrivateRoom("sender@example.com", 2L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(2L, response.getOtherParticipantId());
    }

    @Test
    void testSendMessage_Participant1CanSend() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(100L);
        request.setContent("Hello there!");

        ChatMessage message = ChatMessage.builder()
                .id(50L)
                .room(chatRoom)
                .sender(senderUser)
                .senderAvatar("avatar_1")
                .content("Hello there!")
                .messageType(MessageType.TEXT)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));
        doNothing().when(aiService).moderateContent(any(), any(), any());
        when(profileRepository.findByUser(senderUser)).thenReturn(Optional.of(
                Profile.builder().user(senderUser).avatar("avatar_1").preferredLanguage("EN").build()
        ));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);
        when(socketIOServer.getRoomOperations(anyString())).thenReturn(broadcastOperations);

        ChatMessageResponse response = chatService.sendMessage("sender@example.com", request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Hello there!", response.getContent());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(broadcastOperations, atLeastOnce()).sendEvent(eq("receive_message"), any());
    }

    @Test
    void testSendMessage_Participant2CanSend() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(100L);
        request.setContent("Reply from participant two");

        ChatMessage savedMessage = ChatMessage.builder()
                .id(51L)
                .room(chatRoom)
                .sender(targetUser)
                .senderAvatar("avatar_2")
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));
        when(profileRepository.findByUser(targetUser)).thenReturn(Optional.of(
                Profile.builder().user(targetUser).avatar("avatar_2").username("user_2").build()
        ));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(socketIOServer.getRoomOperations(anyString())).thenReturn(broadcastOperations);

        ChatMessageResponse response = chatService.sendMessage("target@example.com", request);

        assertEquals(51L, response.getId());
        assertEquals(2L, response.getSenderId());
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(broadcastOperations, atLeastOnce()).sendEvent(eq("receive_message"), any());
    }

    @Test
    void testSendMessage_UnrelatedUserGetsForbiddenWithoutPersistenceOrBroadcast() {
        User unrelatedUser = User.builder().id(3L).email("unrelated@example.com").role(Role.USER).build();
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(100L);
        request.setContent("Unauthorized message");

        when(userRepository.findByEmail("unrelated@example.com")).thenReturn(Optional.of(unrelatedUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> chatService.sendMessage("unrelated@example.com", request));
        ResponseEntity<Map<String, Object>> errorResponse =
                new GlobalExceptionHandler().handleAccessDeniedException(exception);

        assertEquals("User is not a participant in this chat room.", exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, errorResponse.getStatusCode());
        verifyNoInteractions(aiService);
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verifyNoInteractions(socketIOServer, broadcastOperations, notificationService);
    }

    @Test
    void testRejectRoomRequest_AuthorizedRecipient_Success() {
        ChatRoom pendingRoom = ChatRoom.builder()
                .id(100L)
                .participant1(senderUser)
                .participant2(targetUser)
                .requestSenderId(1L)
                .requestStatus("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(pendingRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRoomResponse response = chatService.rejectRoomRequest("target@example.com", 100L);

        assertNotNull(response);
        assertEquals("REJECTED", response.getRequestStatus());
        verify(chatRoomRepository).save(pendingRoom);
    }

    @Test
    void testRejectRoomRequest_UnrelatedUser_ThrowsException() {
        User unrelatedUser = User.builder().id(3L).email("unrelated@example.com").role(Role.USER).build();
        ChatRoom pendingRoom = ChatRoom.builder()
                .id(100L)
                .participant1(senderUser)
                .participant2(targetUser)
                .requestSenderId(1L)
                .requestStatus("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("unrelated@example.com")).thenReturn(Optional.of(unrelatedUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(pendingRoom));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                chatService.rejectRoomRequest("unrelated@example.com", 100L)
        );

        assertEquals("User is not a participant in this chat room.", ex.getMessage());
        assertEquals("PENDING", pendingRoom.getRequestStatus());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void testRejectRoomRequest_RequestSender_ThrowsException() {
        ChatRoom pendingRoom = ChatRoom.builder()
                .id(100L)
                .participant1(senderUser)
                .participant2(targetUser)
                .requestSenderId(1L)
                .requestStatus("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(pendingRoom));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                chatService.rejectRoomRequest("sender@example.com", 100L)
        );

        assertEquals("Sender cannot reject their own request.", ex.getMessage());
        assertEquals("PENDING", pendingRoom.getRequestStatus());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void testRejectRoomRequest_NonExistentRoom_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.mka.exception.ResourceNotFoundException.class, () ->
                chatService.rejectRoomRequest("target@example.com", 999L)
        );
    }

    @Test
    void testRejectRoomRequest_NonExistentUser_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(com.mka.exception.ResourceNotFoundException.class, () ->
                chatService.rejectRoomRequest("nonexistent@example.com", 100L)
        );
    }
}
