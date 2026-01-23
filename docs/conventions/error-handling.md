# 에러 처리 규약

## 📑 목차
- [개요](#-개요)
- [응답 형식](#-응답-형식)
- [예외 계층 구조](#-예외-계층-구조)
- [에러 코드 위치](#-에러-코드-위치)
- [대표 처리 흐름](#-대표-처리-흐름)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 백엔드는 `GlobalExceptionHandler`를 통해 예외를 처리합니다.
`BasicException` 기반 예외는 HTTP 상태 코드와 메시지를 포함한 공통 응답으로 변환됩니다.

---

## 📋 응답 형식

모든 API 응답은 `Response<T>` 래퍼를 사용합니다.
에러 응답의 `details`는 `ErrorResponseDetail`(timestamp 포함)이며, 필요 시 도메인별 필드를 확장합니다.

**표준 에러 응답 구조**:
```json
{
  "isSuccess": false,
  "message": "에러 메시지",
  "details": {
    "timestamp": "2026-01-22T14:30:00"
  }
}
```

**참고**:
- 에러 코드는 응답 본문에 포함되지 않습니다.
- `details`는 도메인별 에러 상세가 있을 때만 필드를 추가합니다.

**예시 (일정 부분 실패)**:
```json
{
  "isSuccess": false,
  "message": "신청하신 일정 중 실패한 일정이 존재합니다.",
  "details": {
    "success": [
      { "start": "2026-01-22T09:00:00", "end": "2026-01-22T12:00:00" }
    ],
    "failure": [
      { "start": "2026-01-22T14:00:00", "end": "2026-01-22T16:00:00" }
    ]
  }
}
```

---

## 🏗️ 예외 계층 구조

- `BasicException` (프로젝트 공통 예외)
  - `CustomErrorCode`를 통해 HTTP 상태와 메시지를 보유
  - 필요 시 `ErrorResponseDetail`을 포함
- `MethodArgumentNotValidException`
  - 유효성 검증 실패 시 400 반환
- `AccessDeniedException`
  - 권한 부족 시 403 반환

---

## 🔢 에러 코드 위치

**공통/도메인별 에러 코드**:
- `global/exceptions/error/GlobalErrorCode.java`
- `global/exceptions/error/AuthErrorCode.java`
- `global/exceptions/error/TaskErrorCode.java`
- `global/exceptions/error/CategoryErrorCode.java`
- `global/exceptions/error/FaqErrorCode.java`
- `global/exceptions/error/ManagerErrorCode.java`
- `schedule/application/exceptions/ScheduleErrorCode.java`
- `attendance/application/exception/AttendanceErrorCode.java`

---

## ✅ 대표 처리 흐름

1. 서비스에서 `BasicException` 또는 도메인 예외 발생
2. `GlobalExceptionHandler`가 예외를 받아 응답 생성
3. `Response` 래퍼로 일관된 JSON 반환

---

## 🔗 관련 문서

- [API 설계 규약](./api-conventions.md)
- [인증 API](../api/auth.md)
