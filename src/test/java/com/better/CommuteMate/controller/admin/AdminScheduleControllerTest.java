package com.better.CommuteMate.controller.admin;

import com.better.CommuteMate.auth.application.CustomUserDetailsService;
import com.better.CommuteMate.auth.application.TokenBlacklistService;
import com.better.CommuteMate.global.security.jwt.JwtTokenProvider;
import com.better.CommuteMate.schedule.application.MonthlyScheduleConfigService;
import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleConfigCommand;
import com.better.CommuteMate.schedule.application.dtos.SetApplyTermCommand;
import com.better.CommuteMate.schedule.controller.admin.AdminScheduleController;
import com.better.CommuteMate.schedule.controller.admin.dtos.SetMonthlyLimitRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.SetApplyTermRequest;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MonthlyScheduleConfigService monthlyScheduleConfigService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("POST /api/v1/admin/schedule/monthly-limit - 신규 월별 제한 설정 성공")
    void setMonthlyLimit_NewLimit_Success() throws Exception {
        // Given
        SetMonthlyLimitRequest request = new SetMonthlyLimitRequest(2025, 10, 6);

        MonthlyScheduleConfig savedLimit = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(2025)
                .scheduleMonth(10)
                .maxConcurrent(6)
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigService.setMonthlyLimit(any(MonthlyScheduleConfigCommand.class)))
                .thenReturn(savedLimit);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/schedule/monthly-limit")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("월별 스케줄 제한이 설정되었습니다."))
                .andExpect(jsonPath("$.details.scheduleYear").value(2025))
                .andExpect(jsonPath("$.details.scheduleMonth").value(10))
                .andExpect(jsonPath("$.details.maxConcurrent").value(6));

        verify(monthlyScheduleConfigService, times(1))
                .setMonthlyLimit(any(MonthlyScheduleConfigCommand.class));
    }

    @Test
    @DisplayName("POST /api/v1/admin/schedule/monthly-limit - 기존 월별 제한 업데이트 성공")
    void setMonthlyLimit_UpdateExisting_Success() throws Exception {
        // Given
        SetMonthlyLimitRequest request = new SetMonthlyLimitRequest(2025, 10, 8);

        MonthlyScheduleConfig updatedLimit = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(2025)
                .scheduleMonth(10)
                .maxConcurrent(8)
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigService.setMonthlyLimit(any(MonthlyScheduleConfigCommand.class)))
                .thenReturn(updatedLimit);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/schedule/monthly-limit")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("월별 스케줄 제한이 설정되었습니다."))
                .andExpect(jsonPath("$.details.maxConcurrent").value(8));

        verify(monthlyScheduleConfigService, times(1))
                .setMonthlyLimit(any(MonthlyScheduleConfigCommand.class));
    }

    @Test
    @DisplayName("GET /api/v1/admin/schedule/monthly-limit/{year}/{month} - 데이터 존재 시 200 OK")
    void getMonthlyLimit_DataExists_Returns200() throws Exception {
        // Given
        Integer year = 2025;
        Integer month = 10;

        MonthlyScheduleConfig limit = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(year)
                .scheduleMonth(month)
                .maxConcurrent(6)
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigService.getMonthlyLimit(year, month))
                .thenReturn(Optional.of(limit));

        // When & Then
        mockMvc.perform(get("/api/v1/admin/schedule/monthly-limit/{year}/{month}", year, month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("월별 스케줄 제한을 조회했습니다."))
                .andExpect(jsonPath("$.details.scheduleYear").value(2025))
                .andExpect(jsonPath("$.details.scheduleMonth").value(10))
                .andExpect(jsonPath("$.details.maxConcurrent").value(6));

        verify(monthlyScheduleConfigService, times(1)).getMonthlyLimit(year, month);
    }

    @Test
    @DisplayName("GET /api/v1/admin/schedule/monthly-limit/{year}/{month} - 데이터 없을 시 404 NOT_FOUND")
    void getMonthlyLimit_DataNotExists_Returns404() throws Exception {
        // Given
        Integer year = 2025;
        Integer month = 11;

        when(monthlyScheduleConfigService.getMonthlyLimit(year, month))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/admin/schedule/monthly-limit/{year}/{month}", year, month))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("해당 월의 스케줄 제한 설정을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(monthlyScheduleConfigService, times(1)).getMonthlyLimit(year, month);
    }

    @Test
    @DisplayName("GET /api/v1/admin/schedule/monthly-limits - 모든 데이터 조회 성공")
    void getAllMonthlyLimits_Success() throws Exception {
        // Given
        MonthlyScheduleConfig limit1 = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(2025)
                .scheduleMonth(10)
                .maxConcurrent(6)
                .createdBy(1)
                .updatedBy(1)
                .build();

        MonthlyScheduleConfig limit2 = MonthlyScheduleConfig.builder()
                .limitId(2)
                .scheduleYear(2025)
                .scheduleMonth(11)
                .maxConcurrent(5)
                .createdBy(1)
                .updatedBy(1)
                .build();

        List<MonthlyScheduleConfig> limits = List.of(limit1, limit2);
        when(monthlyScheduleConfigService.getAllMonthlyLimits()).thenReturn(limits);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/schedule/monthly-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("모든 월별 스케줄 제한을 조회했습니다."))
                .andExpect(jsonPath("$.details.limits").isArray())
                .andExpect(jsonPath("$.details.limits.length()").value(2))
                .andExpect(jsonPath("$.details.limits[0].year").value(2025))
                .andExpect(jsonPath("$.details.limits[0].month").value(10))
                .andExpect(jsonPath("$.details.limits[0].maxConcurrent").value(6))
                .andExpect(jsonPath("$.details.limits[1].year").value(2025))
                .andExpect(jsonPath("$.details.limits[1].month").value(11))
                .andExpect(jsonPath("$.details.limits[1].maxConcurrent").value(5));

        verify(monthlyScheduleConfigService, times(1)).getAllMonthlyLimits();
    }

    @Test
    @DisplayName("GET /api/v1/admin/schedule/monthly-limits - 빈 리스트 반환")
    void getAllMonthlyLimits_EmptyList() throws Exception {
        // Given
        when(monthlyScheduleConfigService.getAllMonthlyLimits()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/admin/schedule/monthly-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("모든 월별 스케줄 제한을 조회했습니다."))
                .andExpect(jsonPath("$.details.limits").isArray())
                .andExpect(jsonPath("$.details.limits.length()").value(0));

        verify(monthlyScheduleConfigService, times(1)).getAllMonthlyLimits();
    }

    @Test
    @DisplayName("POST /api/v1/admin/schedule/set-apply-term - 신청 기간 설정 성공 (신규 생성)")
    void setApplyTerm_NewConfig_Success() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 1, 9, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 15, 18, 0, 0);
        SetApplyTermRequest request = new SetApplyTermRequest(2025, 11, startTime, endTime);

        MonthlyScheduleConfig savedConfig = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(2025)
                .scheduleMonth(11)
                .applyStartTime(startTime)
                .applyEndTime(endTime)
                .maxConcurrent(10)
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigService.setApplyTerm(any(SetApplyTermCommand.class)))
                .thenReturn(savedConfig);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/schedule/set-apply-term")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("신청 기간이 설정되었습니다."))
                .andExpect(jsonPath("$.details.scheduleYear").value(2025))
                .andExpect(jsonPath("$.details.scheduleMonth").value(11))
                .andExpect(jsonPath("$.details.applyStartTime").exists())
                .andExpect(jsonPath("$.details.applyEndTime").exists());

        verify(monthlyScheduleConfigService, times(1))
                .setApplyTerm(any(SetApplyTermCommand.class));
    }

    @Test
    @DisplayName("POST /api/v1/admin/schedule/set-apply-term - 신청 기간 업데이트 성공")
    void setApplyTerm_UpdateExisting_Success() throws Exception {
        // Given
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 5, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 20, 17, 0, 0);
        SetApplyTermRequest request = new SetApplyTermRequest(2025, 11, startTime, endTime);

        MonthlyScheduleConfig updatedConfig = MonthlyScheduleConfig.builder()
                .limitId(1)
                .scheduleYear(2025)
                .scheduleMonth(11)
                .applyStartTime(startTime)
                .applyEndTime(endTime)
                .maxConcurrent(10)
                .createdBy(1)
                .updatedBy(1)
                .build();

        when(monthlyScheduleConfigService.setApplyTerm(any(SetApplyTermCommand.class)))
                .thenReturn(updatedConfig);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/schedule/set-apply-term")
                        .header("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("신청 기간이 설정되었습니다."));

        verify(monthlyScheduleConfigService, times(1))
                .setApplyTerm(any(SetApplyTermCommand.class));
    }
}
