package com.mka.repository;

import com.mka.entity.Admin;
import com.mka.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

   // boolean existsByRole(com.mka.enums.Role role);
   boolean existsByRole(Role role);
}
