package com.better.CommuteMate.user.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 조회 응답 DTO
 * <p>
 * 사용자의 기본 정보(ID, 이메일, 이름, 역할, 조직)를 포함합니다.
 * </p>
 */
@Getter
public class UserInfoResponse extends ResponseDetail {
    
    /** 사용자 ID (PK) */
    private final Integer userId;
    
    /** 사용자 이메일 */
    private final String email;
    
    /** 사용자 이름 */
    private final String name;
    
    /** 사용자 역할 (학생/사원, 관리자 등) */
    private final CodeType role;
    
    /** 소속 조직 ID */
    private final Integer organizationId;

    /**
     * User 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param user 사용자 엔티티
     */
    @Builder
    public UserInfoResponse(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRoleCode();
        this.organizationId = user.getOrganizationId();
    }
}
