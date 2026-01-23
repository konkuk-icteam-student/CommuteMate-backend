package com.better.CommuteMate.home.controller;

import com.better.CommuteMate.auth.application.CustomUserDetails;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;
    
    @MockBean
    private com.better.CommuteMate.global.security.jwt.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.better.CommuteMate.auth.application.CustomUserDetailsService customUserDetailsService;
    @MockBean
    private com.better.CommuteMate.auth.application.TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("오늘의 근무 시간 조회 API - 성공")
    @WithMockUser
    void getTodayWorkTime_Success() throws Exception {
        // Given
        HomeWorkTimeResponse response = HomeWorkTimeResponse.builder()
                .totalMinutes(480L)
                .scheduleCount(2)
                .build();

        given(homeService.getTodayWorkTime(anyLong())).willReturn(response);

        // Custom UserDetails mock setup
        User user = User.builder().userId(1L).email("test@test.com").roleCode(CodeType.RL01).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // When & Then
        mockMvc.perform(get("/api/v1/home/work-time")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.totalMinutes").value(480))
                .andExpect(jsonPath("$.details.scheduleCount").value(2));
    }

    @Test
    @DisplayName("출퇴근 상태 조회 API - 성공")
    @WithMockUser
    void getAttendanceStatus_Success() throws Exception {
        // Given
        HomeAttendanceStatusResponse response = HomeAttendanceStatusResponse.builder()
                .status(HomeAttendanceStatusResponse.AttendanceStatus.BEFORE_WORK)
                .message("출근 전입니다.")
                .build();

        given(homeService.getAttendanceStatus(anyLong())).willReturn(response);

        User user = User.builder().userId(1L).email("test@test.com").roleCode(CodeType.RL01).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // When & Then
        mockMvc.perform(get("/api/v1/home/attendance-status")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.details.status").value("BEFORE_WORK"))
                .andExpect(jsonPath("$.details.message").value("출근 전입니다."));
    }
}
