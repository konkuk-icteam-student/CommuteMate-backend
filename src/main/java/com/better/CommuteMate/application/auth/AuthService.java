package com.better.CommuteMate.application.auth;

import com.better.CommuteMate.controller.auth.dto.RegisterRequest;
import com.better.CommuteMate.domain.user.entity.UserEntity;
import com.better.CommuteMate.domain.auth.repository.UserRepository;
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
