package com.better.CommuteMate.admin.controller;

import com.better.CommuteMate.admin.application.AdminWorkerService;
import com.better.CommuteMate.admin.controller.dto.AdminWorkerDetailResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminWorkerController.class)
@Import(com.better.CommuteMate.global.security.SecurityConfig.class)
class AdminWorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminWorkerService adminWorkerService;

    @MockBean
    private com.better.CommuteMate.global.security.jwt.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.better.CommuteMate.auth.application.CustomUserDetailsService customUserDetailsService;
    @MockBean
    private com.better.CommuteMate.auth.application.TokenBlacklistService tokenBlacklistService;
    @MockBean
    private com.better.CommuteMate.global.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CustomUserDetails adminUserDetails() {
        User admin = User.builder().userId(99L).organizationId(10L)
                .email("admin@test.com").roleCode(CodeType.RL02).build();
        return new CustomUserDetails(admin);
    }

    private AdminWorkerDetailResponse detailResponse(LocalDateTime lastRequestedAt) {
        return new AdminWorkerDetailResponse(
                LocalDate.of(2026, 7, 15), 1L, "홍길동", "202211414", "컴퓨터공학과", 3,
                "010-1234-5678", "test@test.com", LocalDate.of(2022, 3, 1),
                270, 780, 780, 1620, 7L, 3L, 1620L, lastRequestedAt
        );
    }

    @Test
    @DisplayName("GET /api/v1/admin/workers/{userId} - lastRequestedAt이 yyyy-MM-dd HH:mm 형식(T·초·마이크로초 없음)으로 직렬화된다")
    void getWorker_serializesLastRequestedAtAsYyyyMmDdHhMm() throws Exception {
        // 저장값이 09초 918705마이크로초까지 있어도 분 단위로만 직렬화되어야 함
        LocalDateTime lastRequestedAt = LocalDateTime.of(2026, 8, 20, 4, 34, 7, 918705000);
        given(adminWorkerService.getWorker(anyLong(), anyLong(), anyString()))
                .willReturn(detailResponse(lastRequestedAt));

        mockMvc.perform(get("/api/v1/admin/workers/1")
                        .param("date", "2026-07-15")
                        .with(user(adminUserDetails()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.lastRequestedAt").value("2026-08-20 04:34"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/workers/{userId} - lastRequestedAt이 null이면 그대로 null로 직렬화된다")
    void getWorker_lastRequestedAtNullSerializesAsNull() throws Exception {
        given(adminWorkerService.getWorker(anyLong(), anyLong(), anyString()))
                .willReturn(detailResponse(null));

        mockMvc.perform(get("/api/v1/admin/workers/1")
                        .param("date", "2026-07-15")
                        .with(user(adminUserDetails()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.lastRequestedAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.details.submittedMinutes").value(1620));
    }

    @Test
    @DisplayName("GET /api/v1/admin/workers/{userId} - 기존 필드(submittedMinutes 등) 회귀 없음")
    void getWorker_existingFieldsRegressionCheck() throws Exception {
        LocalDateTime lastRequestedAt = LocalDateTime.of(2026, 7, 15, 13, 0);
        given(adminWorkerService.getWorker(anyLong(), anyLong(), anyString()))
                .willReturn(detailResponse(lastRequestedAt));

        mockMvc.perform(get("/api/v1/admin/workers/1")
                        .param("date", "2026-07-15")
                        .with(user(adminUserDetails()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.userId").value(1))
                .andExpect(jsonPath("$.details.name").value("홍길동"))
                .andExpect(jsonPath("$.details.submittedMinutes").value(1620))
                .andExpect(jsonPath("$.details.totalChangeRequestCount").value(7))
                .andExpect(jsonPath("$.details.approvedChangeRequestCount").value(3))
                .andExpect(jsonPath("$.details.lastRequestedAt").value("2026-07-15 13:00"));
    }
}
