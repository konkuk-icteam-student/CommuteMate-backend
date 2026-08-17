package com.better.CommuteMate.mypage.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MyPageInfoResponse extends ResponseDetail {

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    @Schema(description = "역할 표시명", example = "학생")
    private String roleName;

    @Schema(description = "소속 조직명", example = "건국대학교 정보운영팀")
    private String organizationName;

    @Schema(description = "학과명 (프로필 미등록 시 null)", example = "컴퓨터공학과", nullable = true)
    private String department;

    @Schema(description = "학번 (프로필 미등록 시 null)", example = "202412345", nullable = true)
    private String studentId;

    @Schema(description = "이번 주 근무 현황")
    private PeriodHours week;

    @Schema(description = "이번 달 근무 현황")
    private PeriodHours month;

    @Builder
    public MyPageInfoResponse(String userName, String roleName, String organizationName,
                               String department, String studentId,
                               PeriodHours week, PeriodHours month) {
        super();
        this.userName = userName;
        this.roleName = roleName;
        this.organizationName = organizationName;
        this.department = department;
        this.studentId = studentId;
        this.week = week;
        this.month = month;
    }

    @Getter
    @NoArgsConstructor
    public static class PeriodHours {

        @Schema(description = "완료된 근무 시간 (시간 단위)", example = "3")
        private int workedHours;

        @Schema(description = "근무 한도 시간 (시간 단위)", example = "13")
        private int limitHours;

        @Builder
        public PeriodHours(int workedHours, int limitHours) {
            this.workedHours = workedHours;
            this.limitHours = limitHours;
        }
    }
}
