package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.home.controller.dto.AdminAttendanceSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHomeServiceTest {

    @Mock
    private WorkSchedulesRepository scheduleRepository;

    @Mock
    private WorkAttendanceRepository attendanceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Test
    void returnsAttendanceAndTaskSummary() {
        AdminHomeService service = new AdminHomeService(
                scheduleRepository,
                attendanceRepository,
                taskRepository
        );
        LocalDate date = LocalDate.of(2026, 4, 15);
        LocalTime start = LocalTime.of(9, 0);

        User workingUser = user(1L);
        User notCheckedInUser = user(2L);
        User completedUser = user(3L);
        WorkSchedule workingSchedule = schedule(11L, workingUser, date, start);
        WorkSchedule notCheckedInSchedule = schedule(12L, notCheckedInUser, date, start);
        WorkSchedule completedSchedule = schedule(13L, completedUser, date, start);
        List<WorkSchedule> schedules = List.of(
                workingSchedule,
                notCheckedInSchedule,
                completedSchedule
        );

        WorkAttendance lateCheckIn = attendance(
                workingSchedule,
                CodeType.CT01,
                LocalDateTime.of(date, start.plusMinutes(11))
        );
        WorkAttendance onTimeCheckIn = attendance(
                completedSchedule,
                CodeType.CT01,
                LocalDateTime.of(date, start.plusMinutes(10))
        );
        WorkAttendance checkOut = attendance(
                completedSchedule,
                CodeType.CT02,
                LocalDateTime.of(date, start.plusHours(3))
        );
        List<WorkAttendance> attendances = List.of(lateCheckIn, onTimeCheckIn, checkOut);
        List<Task> tasks = List.of(
                Task.builder().isCompleted(true).build(),
                Task.builder().isCompleted(false).build()
        );

        when(scheduleRepository.findAllByUser_OrganizationIdAndDateAndStatusCode(
                10L, date, CodeType.WS02
        )).thenReturn(schedules);
        when(attendanceRepository.findAllByScheduleIn(schedules)).thenReturn(attendances);
        when(taskRepository.findAllByAssignee_OrganizationIdAndTaskDate(10L, date))
                .thenReturn(tasks);

        AdminAttendanceSummaryResponse response =
                service.getAttendanceSummary(10L, "2026-04-15");

        assertThat(response.date).isEqualTo(date);
        assertThat(response.currentWorkingCount).isEqualTo(1);
        assertThat(response.notCheckedInCount).isEqualTo(1);
        assertThat(response.lateCount).isEqualTo(1);
        assertThat(response.todayTask.completedCount()).isEqualTo(1);
        assertThat(response.todayTask.totalCount()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidDate() {
        AdminHomeService service = new AdminHomeService(
                scheduleRepository,
                attendanceRepository,
                taskRepository
        );

        assertThatThrownBy(() -> service.getAttendanceSummary(10L, "2026-02-30"))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 날짜 값이 올바르지 않습니다.");
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .organizationId(10L)
                .build();
    }

    private WorkSchedule schedule(
            Long scheduleId,
            User user,
            LocalDate date,
            LocalTime start
    ) {
        return WorkSchedule.builder()
                .scheduleId(scheduleId)
                .user(user)
                .date(date)
                .startTime(start)
                .endTime(start.plusHours(3))
                .statusCode(CodeType.WS02)
                .build();
    }

    private WorkAttendance attendance(
            WorkSchedule schedule,
            CodeType checkType,
            LocalDateTime checkTime
    ) {
        return WorkAttendance.builder()
                .schedule(schedule)
                .user(schedule.getUser())
                .checkTypeCode(checkType)
                .checkTime(checkTime)
                .build();
    }
}
