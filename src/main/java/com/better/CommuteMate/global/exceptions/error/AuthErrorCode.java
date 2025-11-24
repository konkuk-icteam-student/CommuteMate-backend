package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements CustomErrorCode {
    // 회원가입 관련
    EMAIL_ALREADY_REGISTERED(
            "이미 가입된 이메일입니다.",
            "[Error] : 이메일 중복",
            HttpStatus.CONFLICT
    ),

    // 로그인 관련
    INVALID_CREDENTIALS(
            "이메일 또는 비밀번호가 올바르지 않습니다.",
            "[Error] : 로그인 실패 - 잘못된 인증 정보",
            HttpStatus.UNAUTHORIZED
    ),

    // JWT 토큰 관련
    AUTHORIZATION_HEADER_MISSING(
            "Authorization 헤더가 누락되었거나 형식이 올바르지 않습니다.",
            "[Error] : Authorization 헤더 누락",
            HttpStatus.BAD_REQUEST
    ),
    USER_NOT_FOUND(
            "사용자를 찾을 수 없습니다.",
            "[Error] : 사용자 조회 실패",
            HttpStatus.NOT_FOUND
    ),
    INVALID_REFRESH_TOKEN(
            "유효하지 않은 리프레시 토큰입니다.",
            "[Error] : 리프레시 토큰 검증 실패",
            HttpStatus.UNAUTHORIZED
    ),

    // 이메일 인증 관련 (OTP)
    VERIFICATION_CODE_NOT_FOUND(
            "인증번호를 찾을 수 없습니다. 인증번호를 다시 요청해주세요.",
            "[Error] : 인증번호 미존재",
            HttpStatus.NOT_FOUND
    ),
    INVALID_VERIFICATION_CODE(
            "인증번호가 일치하지 않습니다.",
            "[Error] : 인증번호 불일치",
            HttpStatus.BAD_REQUEST
    ),
    EXPIRED_VERIFICATION_CODE(
            "인증번호가 만료되었습니다. 인증번호를 다시 요청해주세요.",
            "[Error] : 인증번호 만료",
            HttpStatus.GONE
    ),
    EMAIL_NOT_VERIFIED(
            "이메일 인증이 완료되지 않았습니다. 인증번호를 먼저 확인해주세요.",
            "[Error] : 이메일 미인증",
            HttpStatus.FORBIDDEN
    ),
    MAX_VERIFICATION_ATTEMPTS_EXCEEDED(
            "인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.",
            "[Error] : 인증 시도 횟수 초과",
            HttpStatus.TOO_MANY_REQUESTS
    ),

    // 기타
    INTERNAL_AUTH_ERROR(
            "인증 처리 중 오류가 발생했습니다.",
            "[Error] : 인증 처리 내부 오류",
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}
