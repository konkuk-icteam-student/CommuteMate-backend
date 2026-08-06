package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.BulkApproveWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkChangeRequestBulkServiceTest {

    @Mock AdminWorkChangeRequestProcessService processService;

    AdminWorkChangeRequestBulkService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkChangeRequestBulkService(processService);
    }

    @Test
    @DisplayName("일괄 승인 - 요청별 성공 및 실패 결과를 독립적으로 반환한다")
    void returnsIndependentResultForEveryRequest() {
        LocalDateTime processedAt = LocalDateTime.of(2026, 6, 13, 14, 30);
        when(processService.process(
                eq(1L), any(ProcessWorkChangeRequest.class), eq(99L), eq(10L)
        )).thenReturn(new ProcessWorkChangeResponse(
                1L, "CS02", processedAt, null, List.of(), List.of()
        ));
        when(processService.process(
                eq(2L), any(ProcessWorkChangeRequest.class), eq(99L), eq(10L)
        )).thenThrow(CustomException.of(
                ScheduleErrorCode.CHANGE_REQUEST_ALREADY_PROCESSED
        ));
        when(processService.process(
                eq(3L), any(ProcessWorkChangeRequest.class), eq(99L), eq(10L)
        )).thenThrow(CustomException.of(
                ScheduleErrorCode.CHANGE_REQUEST_CAPACITY_EXCEEDED
        ));
        when(processService.process(
                eq(4L), any(ProcessWorkChangeRequest.class), eq(99L), eq(10L)
        )).thenThrow(CustomException.of(
                ScheduleErrorCode.CHANGE_REQUEST_NOT_FOUND
        ));

        var response = service.approve(
                new BulkApproveWorkChangeRequest(List.of(1L, 2L, 3L, 4L)),
                99L,
                10L
        );

        assertThat(response.summary.totalCount()).isEqualTo(4);
        assertThat(response.summary.successCount()).isEqualTo(1);
        assertThat(response.summary.failCount()).isEqualTo(3);
        assertThat(response.results)
                .extracting(result -> result.resultCode())
                .containsExactly(
                        "SUCCESS",
                        "ALREADY_PROCESSED",
                        "CAPACITY_EXCEEDED",
                        "NOT_FOUND"
                );
        assertThat(response.results.get(0).processedAt()).isEqualTo(processedAt);
        assertThat(response.results.get(1).processedAt()).isNull();
    }

    @Test
    @DisplayName("일괄 승인 - 요청 ID 목록이 비어 있으면 실패한다")
    void rejectsEmptyRequestIds() {
        assertThatThrownBy(() -> service.approve(
                new BulkApproveWorkChangeRequest(List.of()), 99L, 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("요청 ID 목록이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("일괄 승인 - 요청 ID 목록에 null이 포함되면 실패한다")
    void rejectsNullRequestId() {
        assertThatThrownBy(() -> service.approve(
                new BulkApproveWorkChangeRequest(
                        java.util.Arrays.asList(1L, null)
                ),
                99L,
                10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("요청 ID 목록이 올바르지 않습니다.");
    }
}
