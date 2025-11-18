# 에러 처리 및 응답 컨벤션 가이드

CommuteMate 백엔드의 Exception, ErrorCode, ResponseDetail 구조와 사용 방법을 설명합니다.

---

## 📋 전체 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│  Application Layer (Service, Controller)                 │
│  - 비즈니스 로직 실행                                     │
│  - CustomErrorCode를 구현한 ErrorCode Enum 사용           │
│  - ErrorResponseDetail 확장 클래스 생성                   │
│  - BasicException의 자식 예외 throw                      │
└─────────────────┬───────────────────────────────────────┘
                  │ throw
                  ▼
┌─────────────────────────────────────────────────────────┐
│  GlobalExceptionHandler (@RestControllerAdvice)          │
│  - BasicException catch                                 │
│  - Response 객체로 변환                                 │
│  - HTTP 상태코드 + 응답 반환                             │
└─────────────────┬───────────────────────────────────────┘
                  │ 변환
                  ▼
┌─────────────────────────────────────────────────────────┐
│  Response (클라이언트에게 반환)                           │
│  {                                                      │
│    "isSuccess": false,                                 │
│    "message": "에러 메시지",                             │
│    "details": { ... ErrorResponseDetail 내용 }          │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
```

---

## 🔴 1단계: ErrorCode 정의 (Enum)

### 구조

각 도메인별로 `CustomErrorCode` 인터페이스를 구현한 Enum을 생성합니다.

**위치**: `{domain}/application/exceptions/{Domain}ErrorCode.java`

**예시**: Schedule 도메인

```java
package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements CustomErrorCode {

    // 상수명(유저 메시지, 로그 메시지, HTTP 상태코드)
    SCHEDULE_PARTIAL_FAILURE(
        "신청하신 일정 중 실패한 일정이 존재합니다.",
        "[Error] : 신청하신 일정 중 실패한 일정이 존재합니다.",
        HttpStatus.MULTI_STATUS  // 207: 일부 성공, 일부 실패
    ),
    SCHEDULE_FAILURE(
        "신청하신 일정이 모두 실패하였습니다.",
        "[Error] : 신청하신 일정이 모두 실패하였습니다.",
        HttpStatus.UNPROCESSABLE_ENTITY  // 422: 모든 요청 실패
    ),
    INVALID_APPLY_TERM(
        "신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.",
        "[Error] : 신청 기간 유효성 검증 실패",
        HttpStatus.BAD_REQUEST  // 400: 잘못된 요청
    );

    private final String message;           // 클라이언트에게 반환할 메시지
    private final String logMessage;        // 서버 로그에 기록할 메시지
    private final HttpStatus status;        // HTTP 상태 코드

    ScheduleErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
    }

    @Override
    public String getLogMessage() {
        return this.logMessage;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }
}
```

### 주요 특징

| 속성 | 설명 | 예시 |
|------|------|------|
| **message** | 클라이언트에게 반환되는 메시지 | "신청하신 일정이 모두 실패하였습니다." |
| **logMessage** | 서버 로그에만 기록되는 메시지 | "[Error] : 신청하신 일정이 모두 실패하였습니다." |
| **status** | HTTP 상태 코드 | `HttpStatus.UNPROCESSABLE_ENTITY` |

---

## 🟡 2단계: ResponseDetail 확장 클래스 생성

ErrorResponseDetail을 상속받아 도메인별 응답 상세 정보를 정의합니다.

### 구조

**위치**: `{domain}/application/exceptions/response/{Name}ResponseDetail.java`

**예시 1**: 배치 일정 신청 실패 응답

```java
package com.better.CommuteMate.schedule.application.exceptions.response;

import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleDTO;
import com.better.CommuteMate.schedule.application.dtos.ApplyScheduleResultCommand;
import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleResponseDetail extends ErrorResponseDetail {

    // 응답 상세 필드들
    private List<WorkScheduleDTO> success;  // 성공한 일정 목록
    private List<WorkScheduleDTO> failure;  // 실패한 일정 목록
    // timestamp는 상위 클래스 ResponseDetail에서 자동으로 제공됨

    // 팩토리 메서드
    public static ScheduleResponseDetail of(ApplyScheduleResultCommand command) {
        return ScheduleResponseDetail.builder()
                .success(command.success())
                .failure(command.fail())
                .build();
    }
}
```

**JSON 응답 예시**:
```json
{
  "isSuccess": false,
  "message": "신청하신 일정 중 실패한 일정이 존재합니다.",
  "details": {
    "timestamp": "2025-11-18T15:30:45.123456",
    "success": [
      { "scheduleId": 1, "scheduleDate": "2025-12-01", ... }
    ],
    "failure": [
      { "scheduleId": 2, "scheduleDate": "2025-12-02", ... }
    ]
  }
}
```

**예시 2**: 신청 기간 검증 실패 응답

```java
package com.better.CommuteMate.schedule.application.exceptions.response;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApplyTermValidationResponseDetail extends ErrorResponseDetail {

    private String errorReason;                 // 에러 원인
    private String receivedApplyStartTime;      // 받은 시작 시간
    private String receivedApplyEndTime;        // 받은 종료 시간

    public static ApplyTermValidationResponseDetail of(
            String applyStartTime,
            String applyEndTime) {
        return ApplyTermValidationResponseDetail.builder()
                .errorReason("신청 시작 시간이 종료 시간보다 늦거나 같습니다.")
                .receivedApplyStartTime(applyStartTime)
                .receivedApplyEndTime(applyEndTime)
                .build();
    }
}
```

**JSON 응답 예시**:
```json
{
  "isSuccess": false,
  "message": "신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.",
  "details": {
    "timestamp": "2025-11-18T15:30:45.123456",
    "errorReason": "신청 시작 시간이 종료 시간보다 늦거나 같습니다.",
    "receivedApplyStartTime": "2025-11-15T18:00:00",
    "receivedApplyEndTime": "2025-11-15T09:00:00"
  }
}
```

### ResponseDetail 작성 가이드

| 항목 | 설명 | 예시 |
|------|------|------|
| **클래스명** | 응답 상황을 명확히 하는 이름 | `ScheduleResponseDetail`, `ApplyTermValidationResponseDetail` |
| **상속** | `ErrorResponseDetail` 상속 | `extends ErrorResponseDetail` |
| **필드** | 클라이언트가 필요한 정보 | 성공/실패 목록, 에러 이유, 검증 입력값 등 |
| **timestamp** | 상위 클래스에서 자동 제공 | 추가 선언 불필요 |
| **팩토리 메서드** | 쉬운 생성을 위한 `of()` 메서드 | `of(Command command)`, `of(String... params)` |

---

## 🟢 3단계: Exception 클래스 생성

BasicException을 상속받아 도메인별 예외를 정의합니다.

### 구조

**위치**: `{domain}/application/exceptions/{Name}Exception.java`

**예시 1**: 배치 일정 신청 부분 실패

```java
package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class SchedulePartialFailureException extends BasicException {

    // 상세 정보 포함
    protected SchedulePartialFailureException(
            CustomErrorCode errorCode,
            ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }

    // 팩토리 메서드 (static factory method)
    public static SchedulePartialFailureException of(
            CustomErrorCode errorCode,
            ErrorResponseDetail errorResponseDetail) {
        return new SchedulePartialFailureException(errorCode, errorResponseDetail);
    }
}
```

**예시 2**: 배치 일정 신청 전체 실패

```java
package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class ScheduleAllFailureException extends BasicException {

    // 상세 정보 포함
    protected ScheduleAllFailureException(
            CustomErrorCode errorCode,
            ErrorResponseDetail errorResponseDetail) {
        super(errorCode, errorCode.getLogMessage(), errorResponseDetail);
    }

    // 팩토리 메서드
    public static ScheduleAllFailureException of(
            CustomErrorCode errorCode,
            ErrorResponseDetail errorResponseDetail) {
        return new ScheduleAllFailureException(errorCode, errorResponseDetail);
    }
}
```

**예시 3**: 일반 예외 (상세 정보 불필요)

```java
package com.better.CommuteMate.schedule.application.exceptions;

import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;

public class ScheduleConfigException extends BasicException {

    protected ScheduleConfigException(CustomErrorCode errorCode) {
        super(errorCode, errorCode.getLogMessage());
    }

    public static ScheduleConfigException of(CustomErrorCode errorCode) {
        return new ScheduleConfigException(errorCode);
    }
}
```

---

## 💻 4단계: Service에서 예외 발생

### 사용 방법

**상세 정보 포함하여 발생**:
```java
// Service에서
public ApplyScheduleResultCommand applySchedules(List<ApplyWorkSchedule> request) {
    List<WorkScheduleDTO> successList = new ArrayList<>();
    List<WorkScheduleDTO> failureList = new ArrayList<>();

    for (ApplyWorkSchedule apply : request) {
        try {
            // 일정 신청 로직
            validateSchedule(apply);
            saveSchedule(apply);
            successList.add(new WorkScheduleDTO(apply));
        } catch (BusinessException e) {
            failureList.add(new WorkScheduleDTO(apply));
        }
    }

    // 모두 실패한 경우
    if (successList.isEmpty() && !failureList.isEmpty()) {
        ScheduleResponseDetail detail = ScheduleResponseDetail.of(
            new ApplyScheduleResultCommand(successList, failureList)
        );
        throw ScheduleAllFailureException.of(
            ScheduleErrorCode.SCHEDULE_FAILURE,
            detail
        );
    }

    // 부분 성공/실패
    if (!failureList.isEmpty()) {
        ScheduleResponseDetail detail = ScheduleResponseDetail.of(
            new ApplyScheduleResultCommand(successList, failureList)
        );
        throw SchedulePartialFailureException.of(
            ScheduleErrorCode.SCHEDULE_PARTIAL_FAILURE,
            detail
        );
    }

    return new ApplyScheduleResultCommand(successList, failureList);
}

// 신청 기간 검증
public void validateApplyTerm(LocalDateTime startTime, LocalDateTime endTime) {
    if (!startTime.isBefore(endTime)) {
        ApplyTermValidationResponseDetail detail =
            ApplyTermValidationResponseDetail.of(
                startTime.toString(),
                endTime.toString()
            );
        throw ScheduleConfigException.of(
            ScheduleErrorCode.INVALID_APPLY_TERM,
            detail
        );
    }
}
```

**상세 정보 불필요한 경우**:
```java
// Controller에서 또는 Service에서
if (monthlyScheduleConfig == null) {
    throw ScheduleConfigException.of(
        ScheduleErrorCode.MONTHLY_SCHEDULE_CONFIG_NOT_FOUND
    );
}
```

---

## 🔵 5단계: GlobalExceptionHandler가 자동 처리

```java
package com.better.CommuteMate.global.controller;

import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.global.exceptions.BasicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BasicException.class)
    protected ResponseEntity<Response> handleBasicException(final BasicException e) {
        // 로그 기록 (로그 메시지 사용)
        log.error("{}: {}", e.getHttpStatus(), e.getMessage(), e);

        // Response 객체 생성 (클라이언트 메시지 + 상세 정보)
        Response response = new Response(
            false,
            e.getMessage(),
            e.getErrorResponseDetail()
        );

        // HTTP 상태코드 + 응답 반환
        return new ResponseEntity<>(response, e.getHttpStatus());
    }
}
```

### 자동 변환 과정

```
1. SchedulePartialFailureException 발생 (메시지 + 상세 정보 포함)
   ↓
2. GlobalExceptionHandler catch (BasicException)
   ↓
3. Response 객체로 변환
   {
     "isSuccess": false,
     "message": errorCode.getMessage(),     // 클라이언트 메시지
     "details": errorResponseDetail         // 상세 정보 (null 가능)
   }
   ↓
4. HTTP 상태코드와 함께 반환
   200 OK
   207 Multi-Status
   400 Bad Request
   404 Not Found
   422 Unprocessable Entity
   500 Internal Server Error
```

---

## 📊 HTTP 상태 코드 가이드
아래 코드만 사용해야 한다는 뜻은 아닙니다. 도메인별로 적절한 상태 코드를 선택하여 사용하세요.

| 상태코드 | 의미 | 사용 경우 | 예시 |
|---------|------|---------|------|
| **200 OK** | 성공 | 요청 성공 | 일정 신청 성공, 조회 성공 |
| **207 Multi-Status** | 부분 성공 | 배치 요청 중 일부만 성공 | 10개 일정 중 7개만 신청됨 |
| **400 Bad Request** | 잘못된 요청 | 입력 값 형식 오류 | 시작 시간 > 종료 시간 |
| **404 Not Found** | 리소스 없음 | 요청한 리소스가 없음 | 특정 월의 설정 미존재 |
| **422 Unprocessable Entity** | 처리 불가 | 요청 형식은 맞으나 비즈니스 규칙 위반 | 모든 일정 신청 실패 |
| **500 Internal Server Error** | 서버 에러 | 예상치 못한 에러 | 데이터베이스 연결 실패 |

---

## 🎯 실전 예시: 일정 신청 엔드포인트

### 성공 응답 (200)

```json
{
  "isSuccess": true,
  "message": "일정이 신청되었습니다.",
  "details": null
}
```

### 부분 실패 응답 (207)

```json
{
  "isSuccess": false,
  "message": "신청하신 일정 중 실패한 일정이 존재합니다.",
  "details": {
    "timestamp": "2025-11-18T15:30:45.123456",
    "success": [
      {
        "scheduleId": 1,
        "scheduleDate": "2025-12-01",
        "startTime": "09:00:00",
        "endTime": "18:00:00"
      }
    ],
    "failure": [
      {
        "scheduleId": 2,
        "scheduleDate": "2025-12-02",
        "startTime": "09:00:00",
        "endTime": "18:00:00"
      }
    ]
  }
}
```

### 전체 실패 응답 (422)

```json
{
  "isSuccess": false,
  "message": "신청하신 일정이 모두 실패하였습니다.",
  "details": {
    "timestamp": "2025-11-18T15:30:45.123456",
    "success": [],
    "failure": [
      {
        "scheduleId": 1,
        "scheduleDate": "2025-12-01",
        "startTime": "09:00:00",
        "endTime": "18:00:00"
      },
      {
        "scheduleId": 2,
        "scheduleDate": "2025-12-02",
        "startTime": "09:00:00",
        "endTime": "18:00:00"
      }
    ]
  }
}
```

### 검증 실패 응답 (400 또는 422)

```json
{
  "isSuccess": false,
  "message": "신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.",
  "details": {
    "timestamp": "2025-11-18T15:30:45.123456",
    "errorReason": "신청 시작 시간이 종료 시간보다 늦거나 같습니다.",
    "receivedApplyStartTime": "2025-11-15T18:00:00",
    "receivedApplyEndTime": "2025-11-15T09:00:00"
  }
}
```

---

## ✅ 체크리스트: 새로운 예외 추가

새로운 도메인에서 예외 처리를 추가할 때 다음 단계를 따르세요.

- [ ] **Step 1**: ErrorCode Enum 생성
  - 위치: `{domain}/application/exceptions/{Name}ErrorCode.java`
  - 구현: `CustomErrorCode` 인터페이스
  - 포함: message, logMessage, status

- [ ] **Step 2**: ResponseDetail 확장 (필요시)
  - 위치: `{domain}/application/exceptions/response/{Name}ResponseDetail.java`
  - 상속: `ErrorResponseDetail`
  - 포함: 클라이언트에게 필요한 상세 정보
  - 추가: 팩토리 메서드 `of()`

- [ ] **Step 3**: Exception 클래스 생성
  - 위치: `{domain}/application/exceptions/{Name}Exception.java`
  - 상속: `BasicException`
  - 포함: 팩토리 메서드 `of()`
  - 필요시: 여러 변형 정의 (상세 정보 포함/미포함)

- [ ] **Step 4**: Service에서 사용
  - ErrorCode 참조
  - ResponseDetail 생성 (필요시)
  - Exception throw

- [ ] **Step 5**: GlobalExceptionHandler 확인
  - `BasicException` 상속하면 자동으로 처리됨
  - 필요시 별도 핸들러 추가

- [ ] **Step 6**: 테스트 작성
  - 예외 발생 테스트
  - HTTP 상태 코드 확인
  - 응답 내용 검증

---

## 📝 주의사항

### ❌ 피해야 할 패턴

```java
// ❌ 나쁜 예: RuntimeException 직접 사용
throw new RuntimeException("something went wrong");

// ❌ 나쁜 예: 명확하지 않은 에러 코드
UNKNOWN_ERROR("알 수 없는 오류입니다.", "...", HttpStatus.INTERNAL_SERVER_ERROR)

// ❌ 나쁜 예: ResponseDetail 없이 필요한 정보 누락
throw new SchedulePartialFailureException(...);  // 성공/실패 목록 정보 누락

// ❌ 나쁜 예: 로그 메시지 = 클라이언트 메시지
ERROR_OCCURRED("에러 발생", "에러 발생", HttpStatus.INTERNAL_SERVER_ERROR)
```

### ✅ 좋은 패턴

```java
// ✅ 좋은 예: 도메인별 ErrorCode와 Exception 사용
throw SchedulePartialFailureException.of(
    ScheduleErrorCode.SCHEDULE_PARTIAL_FAILURE,
    ScheduleResponseDetail.of(command)
);

// ✅ 좋은 예: 명확한 에러 코드와 메시지
SCHEDULE_FAILURE(
    "신청하신 일정이 모두 실패하였습니다.",
    "[Error] : 신청하신 일정이 모두 실패하였습니다.",
    HttpStatus.UNPROCESSABLE_ENTITY
)

// ✅ 좋은 예: 풍부한 ResponseDetail 정보
@Getter
@Builder
public class ScheduleResponseDetail extends ErrorResponseDetail {
    private List<WorkScheduleDTO> success;
    private List<WorkScheduleDTO> failure;
}

// ✅ 좋은 예: 클라이언트 메시지와 로그 메시지 분리
message: "신청하신 일정이 모두 실패하였습니다."
logMessage: "[Error] : 신청하신 일정이 모두 실패하였습니다."
```
