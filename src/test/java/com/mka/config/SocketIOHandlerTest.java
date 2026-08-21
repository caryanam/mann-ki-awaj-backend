package com.mka.config;

import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.mka.entity.ChatRoom;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.repository.ChatRoomRepository;
import com.mka.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocketIOHandlerTest {

    @Mock
    private SocketIOServer server;

    @Mock
    private PresenceManager presenceManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private com.mka.repository.ProfileRepository profileRepository;

    @Mock
    private SocketIOClient client;

    @Mock
    private HandshakeData handshakeData;

    @Mock
    private UserDetails userDetails;

    @Captor
    private ArgumentCaptor<ConnectListener> connectListenerCaptor;

    @Captor
    private ArgumentCaptor<DisconnectListener> disconnectListenerCaptor;

    @Captor
    private ArgumentCaptor<DataListener<String>> joinRoomListenerCaptor;

    @Captor
    private ArgumentCaptor<DataListener<String>> joinUserRoomListenerCaptor;

    @Captor
    private ArgumentCaptor<DataListener<Object>> heartbeatListenerCaptor;

    private SocketIOHandler handler;
    private User activeUser;
    private User participant2;

    @BeforeEach
    void setUp() {
        handler = new SocketIOHandler(server, presenceManager, jwtService, userDetailsService, userRepository, chatRoomRepository, profileRepository);

        verify(server).addConnectListener(connectListenerCaptor.capture());
        verify(server).addDisconnectListener(disconnectListenerCaptor.capture());
        verify(server).addEventListener(eq("join_room"), eq(String.class), joinRoomListenerCaptor.capture());
        verify(server).addEventListener(eq("join_user_room"), eq(String.class), joinUserRoomListenerCaptor.capture());
        verify(server).addEventListener(eq("presence_heartbeat"), eq(Object.class), heartbeatListenerCaptor.capture());

        activeUser = User.builder().id(10L).email("user10@example.com").active(true).role(Role.USER).build();
        participant2 = User.builder().id(20L).email("user20@example.com").active(true).role(Role.USER).build();
    }

    @Test
    void testConnect_ValidToken_AuthenticatesClient() {
        when(client.getHandshakeData()).thenReturn(handshakeData);
        when(handshakeData.getSingleUrlParam("token")).thenReturn("valid_jwt");
        when(jwtService.extractUsername("valid_jwt")).thenReturn("user10@example.com");
        when(userDetailsService.loadUserByUsername("user10@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("valid_jwt", userDetails)).thenReturn(true);
        when(userRepository.findByEmail("user10@example.com")).thenReturn(Optional.of(activeUser));

        connectListenerCaptor.getValue().onConnect(client);

        verify(client).set("authenticatedUserId", 10L);
        verify(client).set("authenticatedUserEmail", "user10@example.com");
        verify(client, never()).disconnect();
    }

    @Test
    void testConnect_MissingToken_DisconnectsClient() {
        when(client.getHandshakeData()).thenReturn(handshakeData);
        when(handshakeData.getSingleUrlParam("token")).thenReturn(null);

        connectListenerCaptor.getValue().onConnect(client);

        verify(client).disconnect();
        verify(client, never()).set(eq("authenticatedUserId"), any());
    }

    @Test
    void testConnect_InactiveUser_DisconnectsClient() {
        User inactiveUser = User.builder().id(11L).email("inactive@example.com").active(false).role(Role.USER).build();

        when(client.getHandshakeData()).thenReturn(handshakeData);
        when(handshakeData.getSingleUrlParam("token")).thenReturn("valid_jwt");
        when(jwtService.extractUsername("valid_jwt")).thenReturn("inactive@example.com");
        when(userDetailsService.loadUserByUsername("inactive@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("valid_jwt", userDetails)).thenReturn(true);
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        connectListenerCaptor.getValue().onConnect(client);

        verify(client).disconnect();
        verify(client, never()).set(eq("authenticatedUserId"), any());
    }

    @Test
    void testJoinRoom_AuthorizedParticipant_Success() throws Exception {
        ChatRoom room = ChatRoom.builder().id(100L).participant1(activeUser).participant2(participant2).build();

        when(client.get("authenticatedUserId")).thenReturn(10L);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        joinRoomListenerCaptor.getValue().onData(client, "100", null);

        verify(client).joinRoom("room_100");
    }

    @Test
    void testJoinRoom_UnrelatedUser_RejectsJoin() throws Exception {
        User unrelatedUser = User.builder().id(99L).email("unrelated@example.com").active(true).build();
        ChatRoom room = ChatRoom.builder().id(100L).participant1(activeUser).participant2(participant2).build();

        when(client.get("authenticatedUserId")).thenReturn(99L);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        joinRoomListenerCaptor.getValue().onData(client, "100", null);

        verify(client, never()).joinRoom("room_100");
    }

    @Test
    void testJoinUserRoom_OwnUserRoom_Success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(client.get("authenticatedUserId")).thenReturn(10L);
        when(client.getSessionId()).thenReturn(sessionId);

        joinUserRoomListenerCaptor.getValue().onData(client, "10", null);

        verify(client).joinRoom("user_10");
        verify(presenceManager).registerSession(eq(10L), any(), eq(sessionId));
    }

    @Test
    void testJoinUserRoom_OtherUserRoom_RejectsJoin() throws Exception {
        when(client.get("authenticatedUserId")).thenReturn(10L);

        joinUserRoomListenerCaptor.getValue().onData(client, "20", null);

        verify(client, never()).joinRoom(anyString());
    }

    @Test
    void testPresenceHeartbeat_UsesAuthenticatedUserId() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(client.get("authenticatedUserId")).thenReturn(10L);
        when(client.getSessionId()).thenReturn(sessionId);

        heartbeatListenerCaptor.getValue().onData(client, "spoofed_payload_999", null);

        verify(presenceManager).touchHeartbeat(10L);
        verify(presenceManager).registerSession(eq(10L), any(), eq(sessionId));
    }
}

