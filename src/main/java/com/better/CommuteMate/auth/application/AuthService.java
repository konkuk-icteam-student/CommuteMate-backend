package com.better.CommuteMate.auth.application;

import com.better.CommuteMate.auth.controller.dto.RegisterRequest;
import com.better.CommuteMate.domain.emailverification.entity.EmailVerificationCode;
import com.better.CommuteMate.domain.emailverification.repository.EmailVerificationCodeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.security.jwt.JwtTokenProvider;
import com.better.CommuteMate.auth.application.dto.AuthTokens;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenBlacklistService tokenBlacklistService,
                       EmailVerificationCodeRepository emailVerificationCodeRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.emailService = emailService;
    }

    /**
     * @param email 인증받을 이메일
     */
    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(AuthErrorCode.EMAIL_ALREADY_REGISTERED.getMessage());
        }

        // 기존 인증 코드가 있으면 삭제(재발송 일 경우)
        if (emailVerificationCodeRepository.existsByEmail(email)) {
            emailVerificationCodeRepository.deleteByEmail(email);
        }

        // 6자리 인증번호 생성 및 저장
        EmailVerificationCode code = EmailVerificationCode.createForEmail(email);
        emailVerificationCodeRepository.save(code);

        // 인증번호 이메일 발송
        emailService.sendVerificationCode(email, code.getCode());
    }

    /**
     *
     * @param email 이메일
     * @param code 사용자가 입력한 6자리 코드
     */
    @Transactional
    public void verifyCode(String email, String code) {
        // 1. 이메일로 인증 코드 조회
        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(AuthErrorCode.VERIFICATION_CODE_NOT_FOUND.getMessage()));

        // 2. 최대 시도 횟수 확인 (5회 초과 시 차단)
        if (verificationCode.isMaxAttemptsExceeded()) {
            emailVerificationCodeRepository.delete(verificationCode);
            throw new IllegalStateException(AuthErrorCode.MAX_VERIFICATION_ATTEMPTS_EXCEEDED.getMessage());
        }

        // 3. 코드 일치 및 만료 확인
        if (!verificationCode.verifyCode(code)) {
            // 실패 카운트가 verifyCode 내부에서 증가됨
            emailVerificationCodeRepository.save(verificationCode);

            if (verificationCode.isExpired()) {
                emailVerificationCodeRepository.delete(verificationCode);
                throw new IllegalStateException(AuthErrorCode.EXPIRED_VERIFICATION_CODE.getMessage());
            }

            // 최대 시도 횟수 초과 확인 (방금 증가한 카운트 기준)
            if (verificationCode.isMaxAttemptsExceeded()) {
                emailVerificationCodeRepository.delete(verificationCode);
                throw new IllegalStateException(AuthErrorCode.MAX_VERIFICATION_ATTEMPTS_EXCEEDED.getMessage());
            }

            throw new IllegalArgumentException(AuthErrorCode.INVALID_VERIFICATION_CODE.getMessage());
        }

        // 4. 인증 성공
        verificationCode.setVerified(true);
        emailVerificationCodeRepository.save(verificationCode);
    }

    /**
     * @param request 회원가입 정보
     * @return 생성된 User
     */
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail();

        // 1. 이메일 인증 여부 확인
        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByEmailAndVerifiedTrue(email)
                .orElseThrow(() -> new IllegalStateException(AuthErrorCode.EMAIL_NOT_VERIFIED.getMessage()));

        // 2. 코드 만료 확인 (인증 후 일정 시간 내에 가입해야 함)
        if (verificationCode.isExpired()) {
            emailVerificationCodeRepository.delete(verificationCode);
            throw new IllegalStateException(AuthErrorCode.EXPIRED_VERIFICATION_CODE.getMessage());
        }

        // 3. 이미 가입된 이메일인지 다시 확인 (동시성 문제 방지)
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(AuthErrorCode.EMAIL_ALREADY_REGISTERED.getMessage());
        }

        // 4. 비밀번호 암호화
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 5. User 생성 및 저장
        User user = User.create(
                email,
                hashedPassword,
                request.getName(),
                request.getOrganizationId(),
                request.getRoleCode()
        );
        User savedUser = userRepository.save(user);

        // 6. 인증 코드 삭제
        emailVerificationCodeRepository.delete(verificationCode);

        return savedUser;
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(AuthErrorCode.INVALID_CREDENTIALS.getMessage()));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
        }
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRoleCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRoleCode());
        // refresh token 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        long expiresAt = jwtTokenProvider.getExpiration(accessToken);
        return new AuthTokens(accessToken, refreshToken, expiresAt);
    }

    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // access token 블랙리스트에 추가
            tokenBlacklistService.blacklist(token);
            try {
                String email = jwtTokenProvider.getEmail(token);
                userRepository.findByEmail(email).ifPresent(user -> {
                    user.setRefreshToken(null);
                    userRepository.save(user);
                });
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(AuthErrorCode.USER_NOT_FOUND.getMessage()));
        String stored = user.getRefreshToken();
        if (stored == null || !stored.equals(refreshToken)) {
            throw new IllegalArgumentException(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
        }
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRoleCode());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRoleCode());
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);
        long expiresAt = jwtTokenProvider.getExpiration(newAccessToken);
        return new AuthTokens(newAccessToken, newRefreshToken, expiresAt);
    }
}
