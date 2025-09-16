package com.better.CommuteMate.auth.application;

import com.better.CommuteMate.auth.controller.dto.RegisterRequest;
import com.better.CommuteMate.auth.domain.UserEntity;
import com.better.CommuteMate.auth.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserEntity user = UserEntity.create(
                request.getEmail(),
                hashedPassword,
                request.getName(),
                request.getOrganizationId(),
                "RL02" //임시로 넣은 코드
        );
        return userRepository.save(user);
    }
}
