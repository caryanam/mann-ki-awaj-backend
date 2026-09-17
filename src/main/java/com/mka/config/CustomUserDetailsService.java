package com.mka.config;

import com.mka.entity.Admin;
import com.mka.entity.User;
import com.mka.repository.AdminRepository;
import com.mka.repository.UserRepository;
import com.mka.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        String input = identifier.trim();
        String normalizedMobile = PhoneNumberUtil.normalizeMobile(input);

        // 1. Search Admin repository by email or normalized mobile
        Optional<Admin> adminOptional = adminRepository.findByEmail(input.toLowerCase());
        if (adminOptional.isEmpty() && normalizedMobile != null) {
            adminOptional = adminRepository.findByMobileNumber(normalizedMobile);
        }

        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();

            return new org.springframework.security.core.userdetails.User(
                    admin.getEmail(),
                    admin.getPassword(),
                    admin.getActive(),
                    true,
                    true,
                    true,
                    Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + admin.getRole().name()
                            )
                    )
            );
        }

        // 2. Search User repository by email or normalized mobile
        Optional<User> userOptional = userRepository.findByEmail(input.toLowerCase());
        if (userOptional.isEmpty() && normalizedMobile != null) {
            userOptional = userRepository.findByMobileNumber(normalizedMobile);
        }

        // Fallback for raw input matching mobile
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByMobileNumber(input);
        }

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return new UserPrincipal(user);
        }

        throw new UsernameNotFoundException(
                "User or Admin not found with identifier: " + identifier
        );
    }
}