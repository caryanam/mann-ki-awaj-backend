package com.mka.entity;

import com.mka.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 100)
    private String fullName;

    @Column(nullable = false, unique = true,length = 100)
    private String email;

    @Column(nullable = false, unique = true,length = 10)
    private String mobileNumber;

    @Column(nullable = false,length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active =true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}
