package com.mka.controller;

import com.mka.config.UserPrincipal;
import com.mka.dto.request.SendMessageRequest;
import com.mka.dto.response.ApiResponse;
import com.mka.dto.response.ChatMessageResponse;
import com.mka.dto.response.ChatRoomResponse;
import com.mka.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Real-time & REST Direct Messaging APIs")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private String resolveUsername(Object principalObj) {
        if (principalObj instanceof UserPrincipal principal) {
            return principal.getUsername();
        } else if (principalObj != null) {
            return principalObj.toString();
        }
        return "";
    }

    @PostMapping("/rooms/private/{targetUserId}")
    @Operation(summary = "Get or create a private anonymous chat room with another user")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreatePrivateRoom(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable Long targetUserId) {

        String identifier = resolveUsername(principalObj);
        ChatRoomResponse room = chatService.getOrCreatePrivateRoom(identifier, targetUserId);
        return ResponseEntity.ok(
                ApiResponse.<ChatRoomResponse>builder()
                        .success(true)
                        .message("Chat room retrieved/created successfully")
                        .data(room)
                        .build()
        );
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get all active chat rooms for current user")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getUserRooms(
            @AuthenticationPrincipal Object principalObj) {

        String identifier = resolveUsername(principalObj);
        List<ChatRoomResponse> rooms = chatService.getUserRooms(identifier);
        return ResponseEntity.ok(
                ApiResponse.<List<ChatRoomResponse>>builder()
                        .success(true)
                        .message("User chat rooms retrieved successfully")
                        .data(rooms)
                        .build()
        );
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message in a chat room")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @AuthenticationPrincipal Object principalObj,
            @Valid @RequestBody SendMessageRequest request) {

        String identifier = resolveUsername(principalObj);
        ChatMessageResponse msg = chatService.sendMessage(identifier, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ChatMessageResponse>builder()
                        .success(true)
                        .message("Message sent successfully")
                        .data(msg)
                        .build()
        );
    }

    @GetMapping("/messages/{roomId}")
    @Operation(summary = "Get paginated message history for a chat room")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getRoomMessages(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String identifier = resolveUsername(principalObj);
        Page<ChatMessageResponse> messages = chatService.getRoomMessages(
                identifier, roomId, PageRequest.of(page, size));

        return ResponseEntity.ok(
                ApiResponse.<Page<ChatMessageResponse>>builder()
                        .success(true)
                        .message("Room messages retrieved successfully")
                        .data(messages)
                        .build()
        );
    }

    @PutMapping("/rooms/{roomId}/accept")
    @Operation(summary = "Accept a pending chat room request")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> acceptChatRequest(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable Long roomId) {
        String identifier = resolveUsername(principalObj);
        ChatRoomResponse room = chatService.acceptRoomRequest(identifier, roomId);
        return ResponseEntity.ok(
                ApiResponse.<ChatRoomResponse>builder()
                        .success(true)
                        .message("Chat request accepted successfully")
                        .data(room)
                        .build()
        );
    }

    @PutMapping("/rooms/{roomId}/reject")
    @Operation(summary = "Reject/Decline a pending chat room request")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> rejectChatRequest(
            @AuthenticationPrincipal Object principalObj,
            @PathVariable Long roomId) {
        String identifier = resolveUsername(principalObj);
        ChatRoomResponse room = chatService.rejectRoomRequest(identifier, roomId);
        return ResponseEntity.ok(
                ApiResponse.<ChatRoomResponse>builder()
                        .success(true)
                        .message("Chat request rejected successfully")
                        .data(room)
                        .build()
        );
    }
}
