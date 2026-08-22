package com.mka.config;

import com.mka.entity.Admin;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.repository.AdminRepository;
import com.mka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
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

        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User adminUser = User.builder()
                    .fullName("System Admin")
                    .email("admin@gmail.com")
                    .mobileNumber("9999999999")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .active(true)
                    .deleted(false)
                    .emailVerified(true)
                    .mobileVerified(true)
                    .build();

            userRepository.save(adminUser);
        }
    }
}