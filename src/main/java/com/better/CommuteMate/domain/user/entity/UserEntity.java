package com.better.CommuteMate.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "organization_id", length = 36, nullable = false)
    private String organizationId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "role_code", length = 4, nullable = false)
    private String roleCode;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public static UserEntity create(String email, String rawPassword, String name, String organizationId, String roleCode) {
        return UserEntity.builder()
                .userId(UUID.randomUUID().toString())
                .organizationId(organizationId)
                .email(email)
                .password(rawPassword)
                .name(name)
                .roleCode(roleCode)
                .createdAt(Instant.now())
                .createdBy(null)
                .updatedAt(null)
                .updatedBy(null)
                .build();
    }
}
