# CommuteMate Error Convention

## Overview
CommuteMate 백엔드는 `GlobalExceptionHandler`를 통해 모든 예외를 일관된 JSON 형식으로 반환합니다.

## Response Format
```json
{
  "isSuccess": false,
  "message": "사용자에게 보여줄 에러 메시지",
  "details": {
    "errorCode": "ERROR_CODE_NAME",
    "timestamp": "2025-01-01T12:00:00",
    ...additional_details
  }
}
```

---

## Error Codes by Module

### 1. Global (`GlobalErrorCode`)
| Code | HTTP Status | Description |
|---|---|---|
| `NOT_FOUND` | 207 | 리소스를 찾을 수 없음 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |

### 2. Auth (`AuthErrorCode`)
| Code | HTTP Status | Description |
|---|---|---|
| `EMAIL_ALREADY_REGISTERED` | 409 | 이미 가입된 이메일 |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `INVALID_REFRESH_TOKEN` | 401 | 리프레시 토큰 유효하지 않음 |
| `VERIFICATION_CODE_NOT_FOUND` | 404 | 인증코드 없음 |
| `EXPIRED_VERIFICATION_CODE` | 410 | 인증코드 만료 |

### 3. Schedule (`ScheduleErrorCode`)
| Code | HTTP Status | Description |
|---|---|---|
| `SCHEDULE_PARTIAL_FAILURE` | 207 | 일부 일정 신청 실패 |
| `SCHEDULE_FAILURE` | 422 | 모든 일정 신청 실패 |
| `INVALID_APPLY_TERM` | 400 | 신청 기간 오류 |
| `TOTAL_WORK_TIME_EXCEEDED` | 400 | 월 최대 근무 시간 초과 |
| `PAST_MONTH_MODIFICATION_NOT_ALLOWED` | 400 | 지난달 일정 수정 불가 |

### 4. Attendance (`AttendanceErrorCode`)
| Code | HTTP Status | Description |
|---|---|---|
| `INVALID_QR_TOKEN` | 400 | QR 토큰 유효하지 않음 |
| `NOT_WORK_TIME` | 400 | 출퇴근 가능 시간이 아님 |
| `ALREADY_CHECKED_IN` | 409 | 이미 출근 상태임 |

### 5. FAQ & Category
- `CategoryErrorCode`: 카테고리 중복, 삭제 불가 등
---

## Exception Handling Strategy
1. **Custom Exception**: 모든 비즈니스 예외는 `BasicException`을 상속받아 구현합니다.
2. **Centralized Handler**: `GlobalExceptionHandler`에서 예외를 잡아 표준 응답으로 변환합니다.
3. **Log & Response**: 서버 로그에는 상세한 `logMessage`를, 클라이언트 응답에는 친절한 `message`를 보냅니다.
