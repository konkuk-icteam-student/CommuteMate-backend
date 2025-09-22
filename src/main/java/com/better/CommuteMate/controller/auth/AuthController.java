package com.better.CommuteMate.controller.auth;

import com.better.CommuteMate.controller.auth.dto.RegisterRequest;
import com.better.CommuteMate.application.auth.AuthService;
import com.better.CommuteMate.domain.user.entity.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = authService.register(request);
        return ResponseEntity.ok(user);
    }
}
