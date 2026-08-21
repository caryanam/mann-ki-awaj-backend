package com.mka.dto.response;

import com.mka.entity.Enquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryResponse {
    private Long id;
    private String ticketId;
    private String category;
    private String name;
    private String email;
    private String subject;
    private String message;
    private String status;
    private String adminNotes;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnquiryResponse fromEntity(Enquiry e) {
        if (e == null) return null;
        return EnquiryResponse.builder()
                .id(e.getId())
                .ticketId(e.getTicketId())
                .category(e.getCategory())
                .name(e.getName())
                .email(e.getEmail())
                .subject(e.getSubject())
                .message(e.getMessage())
                .status(e.getStatus())
                .adminNotes(e.getAdminNotes())
                .imageUrl(e.getImageUrl())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

}
