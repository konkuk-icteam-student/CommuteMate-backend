package com.better.CommuteMate.domain.user.entity;

import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"user\"", indexes = {
    @Index(name = "uq_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType roleCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    // 이 user가 어떤 Categories를 담당하고 있는지 저장하는 리스트, manager role을 갖고 있는 경우만 가능
    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagerCategory> managerCategories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static User create(String email, String rawPassword, String name, Integer organizationId, CodeType roleCode) {
        return User.builder()
                .organizationId(organizationId)
                .email(email)
                .password(rawPassword)
                .name(name)
                .roleCode(roleCode)
                .build();
    }
}
