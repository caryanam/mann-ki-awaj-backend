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
import com.mka.repository.ChatMessageRepository;
import com.mka.repository.ChatRoomRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private ChatServiceImpl chatService;

    private User senderUser;
    private User targetUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        senderUser = User.builder().id(1L).email("sender@example.com").role(Role.USER).build();
        targetUser = User.builder().id(2L).email("target@example.com").role(Role.USER).build();

        chatRoom = ChatRoom.builder()
                .id(100L)
                .user1(senderUser)
                .user2(targetUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
    void testSendMessage_Success() {
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
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(chatRoom));
        doNothing().when(aiService).moderateContent("Hello there!");
        when(profileRepository.findByUser(senderUser)).thenReturn(Optional.of(
                Profile.builder().user(senderUser).avatar("avatar_1").preferredLanguage("EN").build()
        ));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);

        ChatMessageResponse response = chatService.sendMessage("sender@example.com", request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Hello there!", response.getContent());
    }
}
