package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "업무명은 필수입니다.")
    @Size(max = 200, message = "업무명은 200자 이내로 입력해주세요.")
    private String title;

    @NotNull(message = "업무 날짜는 필수입니다.")
    private LocalDate taskDate;

    @NotNull(message = "업무 시간은 필수입니다.")
    private LocalTime taskTime;

    private String taskType; // TT01(정기) or TT02(비정기), 미입력 시 TT01
}
