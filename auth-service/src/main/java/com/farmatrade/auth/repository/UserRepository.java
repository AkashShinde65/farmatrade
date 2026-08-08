package com.farmatrade.auth.repository;

import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCaseOrMobile(String email, String mobile);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    boolean existsByAadhaarHash(String aadhaarHash);
    boolean existsByRole(Role role);
    long countByRoleAndEnabledTrue(Role role);
    Page<User> findAll(Specification<User> specification, Pageable pageable);
}
