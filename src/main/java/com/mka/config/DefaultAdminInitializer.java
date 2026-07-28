package com.mka.config;

import com.mka.entity.Admin;
import com.mka.enums.Role;
import com.mka.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!adminRepository.existsByRole(Role.ADMIN)) {

            Admin admin = Admin.builder()
                    .fullName("System Admin")
                    .email("admin@gmail.com")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .active(true)
                    .deleted(false)
                    .build();

            adminRepository.save(admin);

            System.out.println("==========================================");
            System.out.println(" Default Admin Created Successfully ");
            System.out.println(" Email    : admin@gmail.com");
            System.out.println(" Password : Admin@123");
            System.out.println("==========================================");
        }
    }
}