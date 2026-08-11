package com.better.CommuteMate.admin.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAdminWorkerRequest(
        @Size(max = 50, message = "근무 인원 정보 입력값이 올바르지 않습니다.") String name,
        @Size(max = 30, message = "근무 인원 정보 입력값이 올바르지 않습니다.") String studentId,
        @Size(max = 100, message = "근무 인원 정보 입력값이 올바르지 않습니다.") String department,
        @Min(value = 1, message = "근무 인원 정보 입력값이 올바르지 않습니다.")
        @Max(value = 4, message = "근무 인원 정보 입력값이 올바르지 않습니다.") Integer grade,
        @Size(max = 30, message = "근무 인원 정보 입력값이 올바르지 않습니다.") String phoneNumber
) {
    public boolean isEmpty() {
        return name == null && studentId == null && department == null && grade == null && phoneNumber == null;
    }
}
