package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkAttendanceServiceTest {
    @Test
    @DisplayName("출퇴근 이력 조회 - 요청 날짜의 자정부터 다음날 자정까지 조회")
    void queriesRequestedDate() {
        WorkAttendanceRepository repository = mock(WorkAttendanceRepository.class);
        LocalDate date = LocalDate.of(2026, 9, 13);
        when(repository.findByUser_UserIdAndCheckTimeBetween(1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        assertThat(new WorkAttendanceService(repository).getAttendanceHistory(1L, date)).isEmpty();
        verify(repository).findByUser_UserIdAndCheckTimeBetween(1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
