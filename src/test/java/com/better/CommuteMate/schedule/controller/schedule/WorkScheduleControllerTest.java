package com.better.CommuteMate.schedule.controller.schedule;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeCommand;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleChangeResultCommand;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeRequest;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleChangeResponseDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkScheduleControllerTest {

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private WorkScheduleController controller;

    @Test
    @DisplayName("POST /api/v1/work-schedules/apply - 근무 신청 성공")
    void applyWorkSchedule_Success() {
        WorkScheduleChangeRequest.Slot slot = new WorkScheduleChangeRequest.Slot(
                LocalDate.of(2026, 8, 10),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        );
        WorkScheduleChangeRequest request = new WorkScheduleChangeRequest(List.of(slot), List.of());
        WorkScheduleChangeResponseDetail.Slot resultSlot = new WorkScheduleChangeResponseDetail.Slot(
                LocalDateTime.of(2026, 8, 10, 9, 0),
                LocalDateTime.of(2026, 8, 10, 11, 0)
        );
        when(scheduleService.changeWorkSchedules(any(WorkScheduleChangeCommand.class)))
                .thenReturn(WorkScheduleChangeResultCommand.of(List.of(resultSlot), List.of()));

        var response = controller.applyWorkSchedule(
                request,
                new CustomUserDetails(User.builder().userId(1L).build())
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ArgumentCaptor<WorkScheduleChangeCommand> captor = ArgumentCaptor.forClass(WorkScheduleChangeCommand.class);
        verify(scheduleService).changeWorkSchedules(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().addSlots()).hasSize(1);
    }
}
