package com.better.CommuteMate.auth.api;

import com.better.CommuteMate.auth.api.dto.RegisterRequest;
import com.better.CommuteMate.auth.application.AuthService;
import com.better.CommuteMate.auth.domain.UserEntity;
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
