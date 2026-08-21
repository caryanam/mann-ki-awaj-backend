package com.mka.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_registrations", indexes = {
        @Index(name = "idx_pending_reg_email", columnList = "email"),
        @Index(name = "idx_pending_reg_mobile", columnList = "mobile_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PendingRegistration extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    public Boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
