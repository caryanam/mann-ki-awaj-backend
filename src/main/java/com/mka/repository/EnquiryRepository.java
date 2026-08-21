package com.mka.repository;

import com.mka.entity.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    Optional<Enquiry> findByTicketId(String ticketId);
    List<Enquiry> findAllByOrderByCreatedAtDesc();
    List<Enquiry> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
