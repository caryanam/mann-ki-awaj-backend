package com.mka.service;

import com.mka.dto.request.SendMessageRequest;
import com.mka.dto.response.ChatMessageResponse;
import com.mka.dto.response.ChatRoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    ChatRoomResponse getOrCreatePrivateRoom(String email, Long recipientUserId);

    List<ChatRoomResponse> getUserRooms(String email);

    ChatMessageResponse sendMessage(String email, SendMessageRequest request);

    Page<ChatMessageResponse> getRoomMessages(String email, Long roomId, Pageable pageable);

    ChatRoomResponse acceptRoomRequest(String email, Long roomId);

    ChatRoomResponse rejectRoomRequest(String email, Long roomId);
}
