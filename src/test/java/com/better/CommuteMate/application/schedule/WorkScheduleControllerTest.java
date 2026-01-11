package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleService;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.WorkScheduleController;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ApplyWorkSchedule;
import com.better.CommuteMate.schedule.controller.schedule.dtos.ModifyWorkScheduleDTO;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleResponse;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.auth.application.CustomUserDetails;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    private CustomUserDetails createMockUserDetails(int userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("test" + userId + "@test.com");
        user.setPassword("password");
        user.setRoleCode(CodeType.RL01);
        user.setName("Test User");
        user.setOrganizationId(1);
        return new CustomUserDetails(user);
    }

    // ========== POST /api/v1/work-schedules/apply 테스트 ==========

    @Test
    @DisplayName("근무 일정 신청 - 성공")
    void applyWorkSchedule_Success() throws Exception {
        // Given
        WorkScheduleDTO slot1 = new WorkScheduleDTO(
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0)
        );
        WorkScheduleDTO slot2 = new WorkScheduleDTO(
                LocalDateTime.of(2025, 11, 2, 14, 0),
                LocalDateTime.of(2025, 11, 2, 16, 0)
        );

        ApplyWorkSchedule request = new ApplyWorkSchedule(List.of(slot1, slot2));

        ApplyScheduleResultCommand resultCommand = new ApplyScheduleResultCommand(
                List.of(
                        new WorkScheduleDTO(LocalDateTime.of(2025, 11, 1, 9, 0),
                                LocalDateTime.of(2025, 11, 1, 11, 0)),
                        new WorkScheduleDTO(LocalDateTime.of(2025, 11, 2, 14, 0),
                                LocalDateTime.of(2025, 11, 2, 16, 0))
                ),
                List.of(),
                List.of()
        );

        when(scheduleService.applyWorkSchedules(anyList())).thenReturn(resultCommand);

        // When & Then
        mockMvc.perform(post("/api/v1/work-schedules/apply")
                        .with(user(createMockUserDetails(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("신청하신 일정이 모두 등록되었습니다."));

        verify(scheduleService, times(1)).applyWorkSchedules(anyList());
    }

    @Test
    @DisplayName("근무 일정 신청 - 빈 요청")
    void applyWorkSchedule_EmptyRequest() throws Exception {
        // Given
        ApplyWorkSchedule request = new ApplyWorkSchedule(List.of());

        ApplyScheduleResultCommand resultCommand = ApplyScheduleResultCommand.from(
                List.of(), List.of()
        );

        when(scheduleService.applyWorkSchedules(anyList())).thenReturn(resultCommand);

        // When & Then
        mockMvc.perform(post("/api/v1/work-schedules/apply")
                        .with(user(createMockUserDetails(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(scheduleService, times(1)).applyWorkSchedules(anyList());
    }

    // ========== PATCH /api/v1/work-schedules/modify 테스트 ==========

    @Test
    @DisplayName("근무 일정 수정 - 성공")
    void modifyWorkSchedule_Success() throws Exception {
        // Given
        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                List.of(addSlot),  // 추가할 일정들
                List.of(1, 2),  // 취소할 일정 ID들
                "일정 변경"  // 변경 사유
        );

        doNothing().when(scheduleService).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());

        // When & Then
        mockMvc.perform(patch("/api/v1/work-schedules/modify")
                        .with(user(createMockUserDetails(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("신청하신 일정이 모두 수정(요청)되었습니다."));

        verify(scheduleService, times(1)).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());
    }

    @Test
    @DisplayName("근무 일정 수정 - 취소만 수행")
    void modifyWorkSchedule_CancelOnly() throws Exception {
        // Given
        ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                List.of(),  // 추가 없이 취소만
                List.of(1, 2),
                "일정 취소"  // 변경 사유
        );

        doNothing().when(scheduleService).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());

        // When & Then
        mockMvc.perform(patch("/api/v1/work-schedules/modify")
                        .with(user(createMockUserDetails(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(scheduleService, times(1)).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());
    }

    @Test
    @DisplayName("근무 일정 수정 - 추가만 수행")
    void modifyWorkSchedule_AddOnly() throws Exception {
        // Given
        WorkScheduleDTO addSlot = new WorkScheduleDTO(
                LocalDateTime.of(2025, 11, 3, 9, 0),
                LocalDateTime.of(2025, 11, 3, 11, 0)
        );

        ModifyWorkScheduleDTO request = new ModifyWorkScheduleDTO(
                List.of(addSlot),  // 취소 없이 추가만
                List.of(),
                "일정 추가"  // 변경 사유
        );

        doNothing().when(scheduleService).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());

        // When & Then
        mockMvc.perform(patch("/api/v1/work-schedules/modify")
                        .with(user(createMockUserDetails(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(scheduleService, times(1)).modifyWorkSchedules(any(ModifyWorkScheduleDTO.class), anyInt());
    }

    // ========== GET /api/v1/work-schedules 테스트 ==========

    @Test
    @DisplayName("나의 근무 일정 조회 - 성공")
    void getWorkSchedules_Success() throws Exception {
        // Given
        List<WorkScheduleResponse> schedules = List.of(
                new WorkScheduleResponse(1, LocalDateTime.of(2025, 11, 1, 9, 0),
                        LocalDateTime.of(2025, 11, 1, 11, 0), CodeType.WS02),
                new WorkScheduleResponse(2, LocalDateTime.of(2025, 11, 2, 14, 0),
                        LocalDateTime.of(2025, 11, 2, 16, 0), CodeType.WS02)
        );

        when(scheduleService.getWorkSchedules(1, 2025, 11)).thenReturn(schedules);

        // When & Then
        mockMvc.perform(get("/api/v1/work-schedules")
                        .param("year", "2025")
                        .param("month", "11")
                        .with(user(createMockUserDetails(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("근무 일정 조회 성공"))
                .andExpect(jsonPath("$.details.schedules").isArray())
                .andExpect(jsonPath("$.details.schedules.length()").value(2));

        verify(scheduleService, times(1)).getWorkSchedules(1, 2025, 11);
    }

    @Test
    @DisplayName("나의 근무 일정 조회 - 빈 결과")
    void getWorkSchedules_EmptyResult() throws Exception {
        // Given
        when(scheduleService.getWorkSchedules(1, 2025, 12)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/work-schedules")
                        .param("year", "2025")
                        .param("month", "12")
                        .with(user(createMockUserDetails(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.schedules").isArray())
                .andExpect(jsonPath("$.details.schedules.length()").value(0));

        verify(scheduleService, times(1)).getWorkSchedules(1, 2025, 12);
    }

    // ========== GET /api/v1/work-schedules/{scheduleId} 테스트 ==========

    @Test
    @DisplayName("특정 근무 일정 조회 - 성공")
    void getWorkSchedule_Success() throws Exception {
        WorkScheduleResponse schedule = new WorkScheduleResponse(
                1,
                LocalDateTime.of(2025, 11, 1, 9, 0),
                LocalDateTime.of(2025, 11, 1, 11, 0),
                CodeType.WS02
        );

        when(scheduleService.getWorkSchedule(1, 1)).thenReturn(schedule);

        String result = mockMvc.perform(get("/api/v1/work-schedules/1")
                        .with(user(createMockUserDetails(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("근무 일정 상세 조회 성공"))
                .andReturn().getResponse().getContentAsString();

        System.out.println("Actual response: " + result);

        verify(scheduleService, times(1)).getWorkSchedule(1, 1);
    }

    // ========== DELETE /api/v1/work-schedules/{scheduleId} 테스트 ==========

    @Test
    @DisplayName("근무 일정 취소 - 성공")
    void deleteWorkSchedule_Success() throws Exception {
        // Given
        doNothing().when(scheduleService).deleteWorkSchedule(1, 1);

        // When & Then
        mockMvc.perform(delete("/api/v1/work-schedules/1")
                        .with(user(createMockUserDetails(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("근무 일정 취소(요청) 성공"));

        verify(scheduleService, times(1)).deleteWorkSchedule(1, 1);
    }

    @Test
    @DisplayName("근무 일정 취소 - 다른 사용자 ID")
    void deleteWorkSchedule_DifferentUser() throws Exception {
        // Given
        doNothing().when(scheduleService).deleteWorkSchedule(2, 1);

        // When & Then
        mockMvc.perform(delete("/api/v1/work-schedules/1")
                        .with(user(createMockUserDetails(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(scheduleService, times(1)).deleteWorkSchedule(2, 1);
    }
}