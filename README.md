# CommuteMate-backend
출근부 시스템 백엔드 레포지토리

## 프로젝트 구조

### 주요 패키지
- `domain/` - 도메인 엔티티 및 리포지토리
  - `code/` - 코드 마스터 (CodeMajor, CodeSub, Code)
  - `organization/` - 조직 관리
  - `user/` - 사용자
  - `schedule/` - 근무 일정
  - `workchangerequest/` - 근무 변경 요청
  - `workattendance/` - 출근 기록
  - `faq/` - **FAQ 시스템 (신규)** ✨
- `application/` - 비즈니스 로직 및 서비스
  - `auth/` - 인증
  - `schedule/` - 근무 일정 비즈니스 로직
- `controller/` - REST API 컨트롤러
  - `auth/` - 인증 관련
  - `schedule/` - 근무 일정 관련
  - `admin/` - 관리자 기능
- `global/` - 전역 설정 및 공통 코드
  - `code/` - **CodeType Enum 및 Converter**
  - `config/` - Spring 설정
  - `security/` - 보안 설정
  - `exceptions/` - 예외 처리
  - `controller/` - 전역 예외 핸들러

## 코드 시스템

시스템 전체에서 사용하는 코드 값(역할, 상태, 타입 등)은 `global/code/CodeType.java` Enum으로 중앙 관리됩니다.

### 코드 분류
- **WS**: 근무신청 상태 (REQUESTED, APPROVED, REJECTED)
- **CR**: 요청 유형 (EDIT, DELETE)
- **CS**: 요청 상태 (PENDING, APPROVED, REJECTED)
- **CT**: 출근 인증 타입 (CHECK_IN, CHECK_OUT)
- **TT**: 업무 유형 (REGULAR, IRREGULAR)
- **RL**: 사용자 역할 (STUDENT, ADMIN)

## FAQ 시스템

자주 묻는 질문을 관리하는 FAQ 시스템이 추가되었습니다.

### 주요 기능
- **카테고리 분류**: 대분류(Category) → 소분류(SubCategory)로 계층적 분류
- **작성자/수정자 추적**: Writer, LastEditor로 변경 이력 추적
- **수정 이력 관리**: FaqHistory로 모든 수정 사항 기록
- **소프트 삭제**: deletedFlag를 사용한 논리적 삭제

### 엔티티 구조
- `Category` - 대분류
- `SubCategory` - 소분류
- `Faq` - FAQ 게시글
- `FaqHistory` - 수정 이력

## 데이터베이스 엔티티 목록

https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757

| 도메인 | 엔티티 | 테이블명 | 설명 |
|--------|--------|--------|------|
| code | CodeMajor | code_major | 코드 대분류 |
| code | CodeSub | code_sub | 코드 소분류 |
| code | Code | code | 코드 마스터 |
| organization | Organization | organization | 조직 |
| user | User | user | 사용자 |
| schedule | WorkSchedule | work_schedule | 근무 일정 |
| schedule | MonthlyScheduleLimit | monthly_schedule_limit | 월별 일정 제한 |
| workchangerequest | WorkChangeRequest | work_change_request | 근무 변경 요청 |
| workattendance | WorkAttendance | work_attendance | 출근 기록 |
| **faq** | **Category** | **category** | **FAQ 대분류** |
| **faq** | **SubCategory** | **sub_category** | **FAQ 소분류** |
| **faq** | **Faq** | **faq** | **FAQ 게시글** |
| **faq** | **FaqHistory** | **faq_history** | **FAQ 수정 이력** |