package com.better.CommuteMate.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AdminAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        boolean workerQuery = request.getRequestURI().startsWith("/api/v1/admin/workers");
        boolean workerUpdate = workerQuery && request.getMethod().equals("PATCH");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isSuccess", false);
        body.put("code", workerQuery ? "ADMIN_ACCESS_DENIED" : "ACCESS_DENIED");
        body.put("message", workerUpdate ? "근무 인원 정보를 수정할 권한이 없습니다."
                : workerQuery ? "근무 인원 조회 권한이 없습니다." : "해당 작업을 수행할 권한이 없습니다.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
