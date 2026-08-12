package com.better.CommuteMate.domain.user.repository;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(Long userId);
    Optional<User> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    List<User> findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
            Long organizationId,
            CodeType roleCode,
            String name
    );
    boolean existsByEmail(String email);

    Page<User> findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCase(
            Long organizationId,
            CodeType roleCode,
            String name,
            Pageable pageable
    );
}
