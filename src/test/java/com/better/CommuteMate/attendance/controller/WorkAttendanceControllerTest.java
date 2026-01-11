package com.better.CommuteMate.attendance.controller;

import com.better.CommuteMate.attendance.application.WorkAttendanceService;
import com.better.CommuteMate.attendance.controller.WorkAttendanceController;
import com.better.CommuteMate.attendance.controller.dto.CheckInRequest;
import com.better.CommuteMate.attendance.controller.dto.CheckOutRequest;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkAttendanceController.class)
class WorkAttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkAttendanceService workAttendanceService;
    
    @MockBean
    private com.better.CommuteMate.global.security.jwt.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.better.CommuteMate.auth.application.CustomUserDetailsService customUserDetailsService;
    @MockBean
    private com.better.CommuteMate.auth.application.TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("QR 토큰 발급 API - 성공")
    @WithMockUser
    void getQrToken_Success() throws Exception {
        QrTokenResponse response = QrTokenResponse.builder()
                .token("test-token")
                .validSeconds(60)
                .expiresAt(LocalDateTime.now().plusSeconds(60))
                .build();

        given(workAttendanceService.generateQrToken()).willReturn(response);

        mockMvc.perform(get("/api/v1/attendance/qr-token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.token").value("test-token"));
    }

    @Test
    @DisplayName("출근 체크 API - 성공")
    @WithMockUser
    void checkIn_Success() throws Exception {
        CheckInRequest request = new CheckInRequest();
        String json = "{\"qrToken\":\"valid-token\"}";

        doNothing().when(workAttendanceService).checkIn(anyInt(), anyString());

        User user = User.builder().userId(1).email("test@test.com").roleCode(CodeType.RL01).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("퇴근 체크 API - 성공")
    @WithMockUser
    void checkOut_Success() throws Exception {
        String json = "{\"qrToken\":\"valid-token\"}";

        doNothing().when(workAttendanceService).checkOut(anyInt(), anyString());

        User user = User.builder().userId(1).email("test@test.com").roleCode(CodeType.RL01).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
