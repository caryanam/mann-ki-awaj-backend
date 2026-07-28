package com.mka.config;

import com.mka.entity.Admin;
import com.mka.entity.User;
import com.mka.repository.AdminRepository;
import com.mka.repository.UserRepository;
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
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // First search in Admin table
        Optional<Admin> adminOptional =
                adminRepository.findByEmail(email);

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

        // If admin not found, search in User table
        Optional<User> userOptional =
                userRepository.findByEmail(email);

        if (userOptional.isPresent()) {

            User user = userOptional.get();

            return new UserPrincipal(user);

        }

        throw new UsernameNotFoundException(
                "User or Admin not found with email: " + email
        );
    }
}