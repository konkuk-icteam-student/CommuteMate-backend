package com.better.CommuteMate.auth.application;

/**
 * 인증 관련 오류 메시지 중앙 관리
 */
public enum AuthErrorCode {
    // 회원가입 관련
    EMAIL_ALREADY_REGISTERED("이미 가입된 이메일입니다."),

    // 로그인 관련
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),

    // JWT 토큰 관련
    AUTHORIZATION_HEADER_MISSING("Authorization 헤더가 누락되었거나 형식이 올바르지 않습니다."),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다."),

    // 이메일 인증 관련 (OTP)
    VERIFICATION_CODE_NOT_FOUND("인증번호를 찾을 수 없습니다. 인증번호를 다시 요청해주세요."),
    INVALID_VERIFICATION_CODE("인증번호가 일치하지 않습니다."),
    EXPIRED_VERIFICATION_CODE("인증번호가 만료되었습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_NOT_VERIFIED("이메일 인증이 완료되지 않았습니다. 인증번호를 먼저 확인해주세요."),
    MAX_VERIFICATION_ATTEMPTS_EXCEEDED("인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),

    // 기타
    INTERNAL_AUTH_ERROR("인증 처리 중 오류가 발생했습니다.");

    private final String message;

    AuthErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
