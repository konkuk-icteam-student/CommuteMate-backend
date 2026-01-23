# API Endpoint Summary & Usage Guide

이 문서는 `home`, `attendance`, `schedule` 모듈의 주요 API 엔드포인트에 대한 상세 설명, 사용 시나리오, 로직 및 엣지 케이스를 정리한 문서입니다.

---

## 1. Home Module (`home`)
홈 화면 대시보드에서 사용자의 현재 상태와 오늘의 요약을 보여주기 위한 API입니다.

### 1.1 오늘의 근무 시간 조회
- **Endpoint**: `GET /api/v1/home/work-time`
- **설명**: 사용자의 오늘(00:00 ~ 23:59) 예정된 근무 일정 개수와, 현재까지의 누적 근무 시간(분 단위)을 조회합니다.
- **로직**:
  1. 오늘 날짜의 유효한(삭제되지 않은) `WorkSchedule`을 모두 조회합니다.
  2. 각 스케줄에 연결된 `WorkAttendance` 기록을 확인합니다.
  3. **시간 계산 로직**:
     - 출근(`CT01`) 기록이 없으면 0분.
     - 퇴근(`CT02`) 기록이 있으면 `Duration(출근, 퇴근)` 계산.
     - 퇴근 기록이 없으면 `Duration(출근, 현재시간)` 계산 (단, 현재시간이 스케줄 종료 시간을 넘어가면 스케줄 종료 시간까지만 인정).
     - 인정 시간은 스케줄의 `startTime` ~ `endTime` 범위 내로 클램핑(Clamping) 됩니다. (지각 시 늦은 시간부터, 조기 출근 시 정시부터 인정)
- **사용 시나리오**: 사용자 홈 화면 상단 "오늘 근무 시간: 4시간 30분" 표시.
- **Edge Cases**:
  - **지각**: 스케줄 시작보다 늦게 찍으면 실제 찍은 시간부터 계산.
  - **조퇴/미퇴근**: 현재 시간이 스케줄 종료 전이라면 실시간으로 근무 시간이 늘어남.
  - **스케줄 없음**: 0분, 0개 반환.

### 1.2 출퇴근 상태 조회
- **Endpoint**: `GET /api/v1/home/attendance-status`
- **설명**: 현재 시간과 스케줄을 비교하여 사용자의 출퇴근 가능 여부 및 상태를 반환합니다.
- **반환 상태 (`AttendanceStatus`)**:
  - `NO_SCHEDULE`: 오늘 일정이 없음.
  - `BEFORE_WORK`: 일정은 있으나 아직 출근 가능 시간 전임.
  - `CAN_CHECK_IN`: 출근 가능 (시작 10분 전 ~ 시작).
  - `LATE_CHECK_IN`: 지각 (시작 시간 경과 ~ 종료 전).
  - `WORKING`: 출근 완료 후 근무 중.
  - `CAN_CHECK_OUT`: 퇴근 가능 (종료 5분 전 ~ 종료 후 1시간).
  - `COMPLETED`: 퇴근 완료 또는 모든 일정 종료.
- **로직**:
  - `findRelevantSchedule`: 현재 시간(`now`)이 `start - 10분` ~ `end + 1시간` 사이에 있는 스케줄을 우선 탐색.
  - 없으면 가장 가까운 미래의 스케줄, 그것도 없으면 마지막 스케줄을 기준으로 판단.
- **Edge Cases**:
  - **연속 스케줄**: A스케줄(10:00~12:00)과 B스케줄(13:00~15:00)이 있을 때, 12:30분은 A의 퇴근 가능 시간이자 B의 출근 전 상태일 수 있음. 로직상 `relevant` 범위에 따라 결정됨.
  - **미퇴근 상태로 종료**: 퇴근 안하고 스케줄 종료+1시간이 지나면 로직에 따라 다음 스케줄 상태로 넘어가거나 종료 처리될 수 있음.

---

## 2. Attendance Module (`attendance`)
QR 코드를 이용한 출퇴근 인증 및 이력 관리를 담당합니다.

### 2.1 QR 토큰 발급 (Admin)
- **Endpoint**: `GET /api/v1/attendance/qr-token`
- **설명**: 관리자 태블릿 등에서 띄울 QR 코드용 토큰을 생성합니다.
- **특이사항**: 토큰은 60초간 유효합니다 (`validSeconds`).
- **사용 시나리오**: 공용 태블릿 화면에서 1분마다 QR 코드를 갱신.

### 2.2 출근 체크 (User)
- **Endpoint**: `POST /api/v1/attendance/check-in`
- **Body**: `{ "qrToken": "..." }`
- **로직**:
  1. 토큰 유효성 검증 (Redis/Memory 등).
  2. 사용자의 오늘 스케줄 조회.
  3. **Check-In 가능 시간 검증**: `now`가 `schedule.start - 10분` ~ `schedule.end` 사이여야 함.
  4. 중복 체크인 검증 (`ALREADY_CHECKED_IN`).
  5. `WorkAttendance` 생성 (타입 `CT01`).
- **Edge Cases**:
  - **QR 만료**: `INVALID_QR_TOKEN` 예외.
  - **너무 이른 출근**: 스케줄 시작 10분 전보다 이르면 체크인 불가 (`NOT_WORK_TIME`).
  - **스케줄 없음**: `NO_SCHEDULE_FOUND`.

### 2.3 퇴근 체크 (User)
- **Endpoint**: `POST /api/v1/attendance/check-out`
- **Body**: `{ "qrToken": "..." }`
- **로직**:
  1. 토큰 검증.
  2. **Check-Out 가능 시간 검증**: `now`가 `schedule.end - 5분` ~ `schedule.end + 1시간` 사이여야 함.
  3. **선행 조건**: 해당 스케줄에 `Check-In` 기록이 반드시 있어야 함 (`CHECK_IN_REQUIRED`).
  4. 중복 체크아웃 검증.
  5. `WorkAttendance` 생성 (타입 `CT02`).
- **Edge Cases**:
  - **출근 안하고 퇴근 시도**: 예외 발생.
  - **너무 이른 퇴근**: 종료 5분 전보다 이르면 퇴근 불가.
  - **너무 늦은 퇴근**: 종료 후 1시간 지나면 퇴근 불가 (자동 퇴근 처리 로직 필요 시 배치로 처리, 현재 API로는 불가).

### 2.4 출퇴근 이력 조회 (User)
- **Endpoint (오늘)**: `GET /api/v1/attendance/today`
  - 오늘 날짜의 출퇴근 이력 리스트 조회.
- **Endpoint (특정일)**: `GET /api/v1/attendance/history?date={yyyy-MM-dd}`
  - 특정 날짜의 출퇴근 이력 리스트 조회.
- **사용 시나리오**: 사용자 화면에서 내가 오늘 언제 찍었는지 확인용.

---

## 3. Schedule Module (`schedule`)
근무 일정을 관리하며, 가장 복잡한 비즈니스 로직(제한 검증, 상태 변경)을 포함합니다.

### 3.1 근무 일정 일괄 신청
- **Endpoint**: `POST /api/v1/work-schedules/apply`
- **로직**:
  - 여러 `slot`을 받아 순차적으로 처리합니다.
  - **검증 로직 (`ScheduleValidator`)**:
    1. **최소 근무 시간**: 1회 2시간 이상 (`MIN_SESSION_MINUTES`).
    2. **월 총량**: 월 최대 27시간 (`MAX_MONTHLY_MINUTES`).
    3. **주 총량**: 주 최대 13시간 (`MAX_WEEKLY_MINUTES`).
    4. **동시 근무 인원**: 15분 단위로 겹치는 인원이 `MonthlyLimit`을 초과하는지 검사.
  - **상태 결정**:
    - `MonthlyScheduleConfig`의 **신청 기간(Apply Term)** 내라면 -> `WS02` (즉시 승인).
    - 신청 기간 외라면 -> `WS01` (승인 대기/신청 상태).
- **응답**: `207 Multi-Status` (일부 성공) 또는 `422 Unprocessable Entity` (전체 실패).
- **Edge Cases**:
  - **부분 성공**: 3개 신청 중 1개만 시간 겹침으로 실패 -> 2개는 저장, 1개는 실패 응답에 포함.
  - **동시성 이슈**: 마지막 1자리를 두고 동시에 신청 시 DB 락이나 검증 로직에 따라 처리 (현재 로직은 애플리케이션 레벨 검증).

### 3.2 근무 일정 수정
- **Endpoint**: `PATCH /api/v1/work-schedules/modify`
- **설명**: 기존 일정을 취소(`cancelScheduleIds`)하고 새로운 일정(`applySlots`)을 추가합니다.
- **제약 사항**: **취소하는 총 시간 == 추가하는 총 시간**이어야 합니다 (`WORK_DURATION_MISMATCH`). "근무 시간 보존의 법칙".
- **로직**:
  - **지난 달 일정 수정 불가**: 현재 월 이전의 기록은 수정 금지.
  - **취소 로직**:
    - 신청 기간 내: 즉시 취소 (`WS04`).
    - 신청 기간 외: 취소 요청 (`CR02` + `CS01`).
  - **추가 로직**:
    - 신청 기간 내: 즉시 승인 (`WS02`).
    - 신청 기간 외: 변경(추가) 요청 (`CR01` + `CS01`).
    - 추가 시 모든 Validator(시간 총량, 동시 인원) 재검증.
- **Edge Cases**:
  - **시간 불일치**: 취소는 2시간인데 추가를 3시간 하려고 하면 전체 롤백.
  - **삭제 대상이 이미 변경 요청 중**: 상태 충돌 가능성.

### 3.3 근무 일정 취소/삭제
- **Endpoint**: `DELETE /api/v1/work-schedules/{scheduleId}`
- **로직**: 단일 건에 대한 취소 처리. `modify`의 취소 로직과 동일하게 기간에 따라 즉시 취소(`WS04`) 또는 취소 요청(`CR02`)으로 나뉨.

### 3.4 근무 일정 조회 (User)
- **Endpoint (월별 조회)**: `GET /api/v1/work-schedules?year={yyyy}&month={MM}`
  - 해당 월의 나의 스케줄 목록 조회.
- **Endpoint (이력 조회)**: `GET /api/v1/work-schedules/history?year={yyyy}&month={MM}`
  - 해당 월의 스케줄 + 실제 출퇴근 기록(attendance)까지 합쳐서 조회. (실근무 시간 포함)
- **Endpoint (상세 조회)**: `GET /api/v1/work-schedules/{scheduleId}`
  - 특정 스케줄의 상세 정보 조회.

---

## 4. Admin Schedule Module (`admin`)
관리자가 시스템 설정을 제어하고 사용자 요청을 처리합니다.

### 4.1 월별 제한 및 신청 기간 설정
- **Endpoint (제한 설정)**: `POST /api/v1/admin/schedule/monthly-limit`
  - 월별 최대 동시 근무 인원(`maxConcurrent`) 설정.
- **Endpoint (기간 설정)**: `POST /api/v1/admin/schedule/set-apply-term`
  - 사용자가 자유롭게 신청 가능한 기간(`applyTerm`) 설정.
- **Endpoint (제한 조회)**: `GET /api/v1/admin/schedule/monthly-limit/{year}/{month}`
- **Endpoint (전체 제한 조회)**: `GET /api/v1/admin/schedule/monthly-limits`
- **Edge Cases**:
  - **기간 역전**: 시작 시간이 종료 시간보다 늦으면 `400` 에러.
  - **설정 미존재**: 조회 시 404가 아닌 기본값 처리 혹은 예외 처리 필요.

### 4.2 변경 요청 일괄 처리
- **Endpoint**: `POST /api/v1/admin/schedule/process-change-request`
- **설명**: 사용자가 신청 기간 외에 보낸 `WS01`(신규 신청), `CR01`(수정), `CR02`(삭제) 요청을 승인(`CS02`)하거나 거부합니다.
- **특이사항**: `requestIds` 리스트의 크기는 반드시 **짝수**여야 합니다. (이유: Controller 레벨 검증 `requestIds().size() % 2 != 0`)
  - *참고: 수정 요청(Modify)은 '삭제 요청 1건 + 추가 요청 1건'이 세트로 오기 때문에 짝수 검증을 하는 것으로 추정되나, 단일 신청(WS01) 처리 시에는 제약이 될 수 있음.*
- **로직**:
  - 승인 시: 해당 스케줄의 상태를 `WS02`(승인) 또는 `WS04`(취소)로 최종 변경.
  - 거부 시: 요청 상태만 `REJECTED`로 변경하고 스케줄은 원복.
- **Endpoint (신청 목록 조회)**: `GET /api/v1/admin/schedule/apply-requests`
  - 승인 대기 중인 모든 근무 신청 목록 조회.

### 4.3 근무 현황 및 통계 조회 (Admin)
- **Endpoint (사용자 근무시간)**: `GET /api/v1/admin/schedule/work-time?userId={id}&year={y}&month={m}`
  - 특정 사용자의 월별 근무 시간 조회.
- **Endpoint (전체 통계)**: `GET /api/v1/admin/schedule/work-time/summary?year={y}&month={m}`
  - 특정 월의 모든 사용자 근무 시간 요약 통계.
- **Endpoint (사용자 근무이력)**: `GET /api/v1/admin/schedule/history?userId={id}&year={y}&month={m}`
  - 특정 사용자의 상세 근무 이력 조회.
- **Endpoint (전체 이력)**: `GET /api/v1/admin/schedule/history/all?year={y}&month={m}`
  - 특정 월의 전체 사용자 근무 이력 조회.

---

## 5. WebSocket Notification (Real-time)
시스템의 스케줄 상태 변경 사항을 클라이언트에게 실시간으로 전파합니다.

### 5.1 Schedule Updates
- **Topic**: `/topic/schedule-updates`
- **설명**: 근무 일정이 승인(`WS02`)되거나, 취소(`WS04`)되는 즉시 해당 변경 사항을 구독자들에게 브로드캐스트합니다.
- **메시지 형식 (`ScheduleUpdateMessage`)**:
  ```json
  {
    "type": "SCHEDULE_UPDATED",
    "updates": [
      {
        "isAdd": true,
        "slotStartTime": "2026-01-11T09:00:00"
      }
    ]
  }
  ```
- **발생 시점**:
  1. **사용자 일정 신청**: 신청 기간 내라서 즉시 승인될 때.
  2. **사용자 일정 취소**: 신청 기간 내라서 즉시 취소될 때.
  3. **관리자 승인 처리**: 관리자가 변경 요청을 승인할 때.
- **용도**: 클라이언트가 캘린더 화면을 즉시 갱신하거나 사용자에게 알림을 표시하는 데 사용됩니다.
