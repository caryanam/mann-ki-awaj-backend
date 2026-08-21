package com.mka.service;

import com.mka.dto.request.CreateInquiryRequest;
import com.mka.dto.response.EnquiryResponse;
import com.mka.entity.Enquiry;
import com.mka.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;

    @Transactional
    public String createEnquiry(CreateInquiryRequest request) {
        String ticketId = generateTicketId();
        Enquiry enquiry = Enquiry.builder()
                .ticketId(ticketId)
                .category(request.getCategory())
                .name(request.getName())
                .email(request.getEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .imageUrl(request.getImageUrl())
                .status("PENDING")
                .build();


        enquiryRepository.save(enquiry);
        return ticketId;
    }

    public List<EnquiryResponse> getAllEnquiries() {
        return enquiryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(EnquiryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnquiryResponse updateStatus(Long id, String status, String adminNotes) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enquiry not found with ID: " + id));

        if (status != null && !status.trim().isEmpty()) {
            enquiry.setStatus(status.toUpperCase().trim());
        }
        if (adminNotes != null) {
            enquiry.setAdminNotes(adminNotes.trim());
        }

        Enquiry saved = enquiryRepository.save(enquiry);
        return EnquiryResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteEnquiry(Long id) {
        if (!enquiryRepository.existsById(id)) {
            throw new RuntimeException("Enquiry not found with ID: " + id);
        }
        enquiryRepository.deleteById(id);
    }

    private String generateTicketId() {
        Random random = new Random();
        int num = 10000 + random.nextInt(90000);
        return "MKA-INQ-" + num;
    }
}
