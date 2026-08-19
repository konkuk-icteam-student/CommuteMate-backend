package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workplace.entity.Workplace;
import com.better.CommuteMate.domain.workplace.repository.WorkplaceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkAssignmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkAssignmentServiceTest {

    @Mock UserRepository userRepository;
    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkplaceRepository workplaceRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkAttendanceRepository attendanceRepository;

    AdminWorkAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkAssignmentService(
                userRepository, settingRepository, workplaceRepository, scheduleRepository,
                attendanceRepository
        );
    }

    @Test
    @DisplayName("관리자 직접 배치 - 정원을 초과해도 WS02로 생성하고 추가 후 인원을 반환한다")
    void assignsApprovedScheduleEvenWhenCapacityIsExceeded() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(9, 30);
        User user = User.builder()
                .userId(1L)
                .organizationId(10L)
                .name("김송은")
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026)
                .month(9)
                .maxConcurrentWorkers(4)
                .build();
        Workplace workplace = Workplace.builder().build();

        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(settingRepository.findForUpdate(10L, 2026, 9))
                .thenReturn(Optional.of(setting));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(workplace));
        when(scheduleRepository
                .existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
                        1L, date, start, end, List.of(CodeType.WS01, CodeType.WS02)
                ))
                .thenReturn(false);
        when(scheduleRepository.saveAndFlush(any(WorkSchedule.class)))
                .thenAnswer(invocation -> {
                    WorkSchedule schedule = invocation.getArgument(0);
                    return WorkSchedule.builder()
                            .scheduleId(100L)
                            .user(schedule.getUser())
                            .setting(schedule.getSetting())
                            .workplace(schedule.getWorkplace())
                            .date(schedule.getDate())
                            .startTime(schedule.getStartTime())
                            .endTime(schedule.getEndTime())
                            .statusCode(schedule.getStatusCode())
                            .build();
                });
        when(scheduleRepository.countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                setting, date, start, end, CodeType.WS02
        )).thenReturn(5L);

        var response = service.assign(request("09:00", "09:30"), 10L, 99L);

        assertThat(response.getScheduleId()).isEqualTo(100L);
        assertThat(response.getUserName()).isEqualTo("김송은");
        assertThat(response.getCurrentCount()).isEqualTo(5);
        assertThat(response.getMaxConcurrentWorkers()).isEqualTo(4);
        verify(scheduleRepository).saveAndFlush(any(WorkSchedule.class));
    }

    @Test
    @DisplayName("관리자 직접 배치 - 30분 슬롯이 아니면 400 오류를 반환한다")
    void rejectsInvalidThirtyMinuteSlot() {
        assertThatThrownBy(() -> service.assign(request("09:10", "09:40"), 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 시간은 30분 단위로만 지정할 수 있습니다.");
    }

    @Test
    @DisplayName("관리자 직접 배치 - 다른 조직 사용자면 사용자를 찾을 수 없음 오류를 반환한다")
    void hidesUserFromOtherOrganization() {
        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(request("09:00", "09:30"), 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("관리자 직접 배치 - 활성 일정이 같은 슬롯에 있으면 중복 오류를 반환한다")
    void rejectsDuplicateAssignment() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(9, 30);
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .maxConcurrentWorkers(4)
                .build();

        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(settingRepository.findForUpdate(10L, 2026, 9))
                .thenReturn(Optional.of(setting));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(Workplace.builder().build()));
        when(scheduleRepository
                .existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
                        1L, date, start, end, List.of(CodeType.WS01, CodeType.WS02)
                ))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assign(request("09:00", "09:30"), 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 해당 시간에 배치된 사용자입니다.");
    }

    @Test
    @DisplayName("관리자 직접 배치 - 조직의 근무지가 없으면 전용 오류를 반환한다")
    void rejectsAssignmentWhenWorkplaceDoesNotExist() {
        User user = User.builder().userId(1L).organizationId(10L).build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .maxConcurrentWorkers(4)
                .build();

        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(settingRepository.findForUpdate(10L, 2026, 9))
                .thenReturn(Optional.of(setting));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(request("09:00", "09:30"), 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("조직의 근무지를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("관리자 직접 배치 - 취소된 동일 슬롯이 있으면 기존 스케줄을 WS02로 복구한다")
    void restoresCancelledScheduleInsteadOfCreatingAnotherRow() {
        LocalDate date = LocalDate.of(2026, 9, 8);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(9, 30);
        User user = User.builder()
                .userId(1L)
                .organizationId(10L)
                .name("김송은")
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(2026)
                .month(9)
                .maxConcurrentWorkers(4)
                .build();
        Workplace workplace = Workplace.builder().build();
        WorkSchedule cancelled = WorkSchedule.builder()
                .scheduleId(101L)
                .user(user)
                .setting(setting)
                .workplace(workplace)
                .date(date)
                .startTime(start)
                .endTime(end)
                .statusCode(CodeType.WS04)
                .build();

        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(settingRepository.findForUpdate(10L, 2026, 9))
                .thenReturn(Optional.of(setting));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(workplace));
        when(scheduleRepository
                .existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
                        1L, date, start, end, List.of(CodeType.WS01, CodeType.WS02)
                ))
                .thenReturn(false);
        when(scheduleRepository
                .findFirstByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeOrderByUpdatedAtDesc(
                        1L, date, start, end, CodeType.WS04
                ))
                .thenReturn(Optional.of(cancelled));
        when(scheduleRepository.saveAndFlush(cancelled)).thenReturn(cancelled);
        when(scheduleRepository.countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                setting, date, start, end, CodeType.WS02
        )).thenReturn(1L);

        var response = service.assign(request("09:00", "09:30"), 10L, 99L);

        assertThat(response.getScheduleId()).isEqualTo(101L);
        assertThat(cancelled.getStatusCode()).isEqualTo(CodeType.WS02);
        assertThat(cancelled.getUpdatedBy()).isEqualTo("99");
        verify(scheduleRepository).saveAndFlush(cancelled);
    }

    @Test
    @DisplayName("관리자 직접 배치 - 종료된 과거 슬롯이면 출퇴근 기록을 자동 생성한다")
    void createsAttendanceForPastAssignment() {
        LocalDate date = LocalDate.now().minusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(9, 30);
        User user = User.builder()
                .userId(1L)
                .organizationId(10L)
                .name("홍길동")
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(10L)
                .year(date.getYear())
                .month(date.getMonthValue())
                .maxConcurrentWorkers(4)
                .build();
        Workplace workplace = Workplace.builder().build();
        WorkSchedule saved = WorkSchedule.builder()
                .scheduleId(100L)
                .user(user)
                .setting(setting)
                .workplace(workplace)
                .date(date)
                .startTime(start)
                .endTime(end)
                .statusCode(CodeType.WS02)
                .build();

        when(userRepository.findByUserIdAndOrganizationId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(settingRepository.findForUpdate(10L, date.getYear(), date.getMonthValue()))
                .thenReturn(Optional.of(setting));
        when(workplaceRepository.findFirstByOrganizationId(10L))
                .thenReturn(Optional.of(workplace));
        when(scheduleRepository.existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
                1L, date, start, end, List.of(CodeType.WS01, CodeType.WS02)))
                .thenReturn(false);
        when(scheduleRepository.saveAndFlush(any(WorkSchedule.class))).thenReturn(saved);
        when(attendanceRepository.findBySchedule_ScheduleId(100L)).thenReturn(List.of());
        when(scheduleRepository.countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                setting, date, start, end, CodeType.WS02)).thenReturn(1L);

        service.assign(request(date, "09:00", "09:30"), 10L, 99L);

        verify(attendanceRepository).saveAll(org.mockito.ArgumentMatchers.argThat(records -> {
            List<WorkAttendance> attendances = new ArrayList<>();
            records.forEach(attendances::add);
            if (attendances.size() != 2) {
                return false;
            }
            WorkAttendance checkIn = attendances.get(0);
            WorkAttendance checkOut = attendances.get(1);
            return checkIn.getCheckTypeCode() == CodeType.CT01
                    && checkIn.getCheckTime().equals(LocalDateTime.of(date, start))
                    && checkOut.getCheckTypeCode() == CodeType.CT02
                    && checkOut.getCheckTime().equals(LocalDateTime.of(date, end))
                    && Boolean.TRUE.equals(checkIn.getVerified())
                    && Boolean.TRUE.equals(checkOut.getVerified())
                    && checkIn.getCreatedBy().equals(99L)
                    && checkOut.getCreatedBy().equals(99L);
        }));
    }

    private AdminWorkAssignmentRequest request(String startTime, String endTime) {
        return new AdminWorkAssignmentRequest(
                "1", "2026-09-08", startTime, endTime
        );
    }

    private AdminWorkAssignmentRequest request(
            LocalDate date,
            String startTime,
            String endTime
    ) {
        return new AdminWorkAssignmentRequest(
                "1", date.toString(), startTime, endTime
        );
    }
}
