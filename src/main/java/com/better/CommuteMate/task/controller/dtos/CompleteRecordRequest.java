package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRecordRequest {

    @NotBlank(message = "수행자 이름은 필수입니다.")
    @Size(max = 50, message = "수행자 이름은 50자 이내로 입력해주세요.")
    private String completedByName;

    @NotNull(message = "수행 시간은 필수입니다.")
    private LocalTime completedTime;
}
