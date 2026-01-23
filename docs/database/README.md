# 데이터베이스 스키마

## 📑 목차
- [개요](#-개요)
- [ERD](#-erd)
- [테이블 목록](#-테이블-목록)
- [도메인별 분류](#-도메인별-분류)
- [스키마 문서 목록](#-스키마-문서-목록)
- [빠른 참조](#-빠른-참조)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 백엔드 시스템은 **MySQL** 데이터베이스를 사용합니다.
계층형 아키텍처의 **Domain Layer**에서 JPA 엔티티로 매핑되며, Spring Data JPA를 통해 관리됩니다.

**데이터베이스 특징**:
- **ORM**: JPA/Hibernate
- **명명 규칙**: snake_case
- **타입 안전성**: CodeType Enum을 통한 코드 값 관리
- **감사 로그**: `created_at`, `updated_at` 자동 관리
- **소프트 삭제**: `deleted_flag` 사용

---

## 🗺️ ERD

전체 ERD는 아래 링크에서 확인할 수 있습니다:

**🔗 [DBDiagram - CommuteMate ERD](https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757)**

---

## 📊 테이블 목록

전체 테이블 개수: **15개**

| # | 테이블 명 | 도메인 | 설명 | 상세 문서 |
|---|----------|--------|------|----------|
| 1 | `user` | User | 사용자 계정 정보 | [user.md](./schema/user.md#user-테이블) |
| 2 | `organization` | Organization | 조직/그룹 정의 | [user.md](./schema/user.md#organization-테이블) |
| 3 | `work_schedule` | Schedule | 근무 일정 슬롯 | [schedule.md](./schema/schedule.md#work_schedule-테이블) |
| 4 | `monthly_schedule_limit` | Schedule | 월별 최대 동시 근무 인원 | [schedule.md](./schema/schedule.md#monthly_schedule_limit-테이블) |
| 5 | `work_attendance` | Attendance | 출퇴근 기록 (QR 체크) | [attendance.md](./schema/attendance.md#work_attendance-테이블) |
| 6 | `work_change_request` | ChangeRequest | 일정 변경/삭제 요청 | [schedule.md](./schema/schedule.md#work_change_request-테이블) |
| 7 | `task` | Task | 일일 업무 관리 | [task.md](./schema/task.md#task-테이블) |
| 8 | `task_template` | Task | 업무 템플릿 | [task.md](./schema/task.md#task_template-테이블) |
| 9 | `task_template_item` | Task | 템플릿 항목 | [task.md](./schema/task.md#task_template_item-테이블) |
| 10 | `faq` | FAQ | FAQ 게시글 | [faq.md](./schema/faq.md#faq-테이블) |
| 11 | `category` | FAQ | FAQ 대분류 | [faq.md](./schema/faq.md#category-테이블) |
| 12 | `sub_category` | FAQ | FAQ 소분류 | [faq.md](./schema/faq.md#sub_category-테이블) |
| 13 | `faq_history` | FAQ | FAQ 수정 이력 | [faq.md](./schema/faq.md#faq_history-테이블) |
| 14 | `code` | Code | 코드 마스터 | [code-system.md](./schema/code-system.md#code-테이블) |
| 15 | `code_major` | Code | 코드 대분류 | [code-system.md](./schema/code-system.md#code_major-테이블) |
| 16 | `code_sub` | Code | 코드 소분류 | [code-system.md](./schema/code-system.md#code_sub-테이블) |

---

## 🗂️ 도메인별 분류

### 👤 사용자 및 조직 ([user.md](./schema/user.md))
- **user**: 사용자 계정 (이메일, 비밀번호, 역할)
- **organization**: 조직/그룹 정보

### 📅 근무 일정 ([schedule.md](./schema/schedule.md))
- **work_schedule**: 사용자별 근무 일정 슬롯
- **monthly_schedule_limit**: 월별 최대 동시 근무 인원 설정
- **work_change_request**: 일정 변경/삭제 요청 로그

### ⏰ 출퇴근 ([attendance.md](./schema/attendance.md))
- **work_attendance**: QR 코드 기반 출퇴근 기록

### 📋 업무 관리 ([task.md](./schema/task.md))
- **task**: 일일 업무
- **task_template**: 업무 템플릿
- **task_template_item**: 템플릿 항목

### 💬 FAQ 시스템 ([faq.md](./schema/faq.md))
- **faq**: FAQ 게시글
- **category**: 대분류
- **sub_category**: 소분류
- **faq_history**: 수정 이력

### 🔢 코드 시스템 ([code-system.md](./schema/code-system.md))
- **code**: 코드 마스터 테이블
- **code_major**: 코드 대분류 (WS, CR, CS, CT, TT, RL)
- **code_sub**: 코드 소분류 (01, 02, 03...)

---

## 📂 스키마 문서 목록

### [사용자/조직 스키마](./schema/user.md)
`user`, `organization` 테이블 상세 구조 및 관계

**주요 내용**:
- 사용자 계정 정보 (이메일, 비밀번호, 역할)
- 조직 구조 및 소속 관계
- roleCode를 통한 권한 관리

**바로가기**: [user.md →](./schema/user.md)

---

### [근무 일정 스키마](./schema/schedule.md)
`work_schedule`, `monthly_schedule_limit`, `work_change_request` 테이블 구조

**주요 내용**:
- 근무 일정 신청 및 상태 관리
- 월별 최대 동시 근무 인원 제한
- 일정 변경/삭제 요청 처리
- statusCode를 통한 일정 상태 추적

**바로가기**: [schedule.md →](./schema/schedule.md)

---

### [출퇴근 스키마](./schema/attendance.md)
`work_attendance` 테이블 구조

**주요 내용**:
- QR 코드 기반 출퇴근 체크
- checkTypeCode를 통한 출근/퇴근 구분
- 근무 시간 계산

**바로가기**: [attendance.md →](./schema/attendance.md)

---

### [업무 관리 스키마](./schema/task.md)
`task`, `task_template`, `task_template_item` 테이블 구조

**주요 내용**:
- 일일 업무 등록/수정/완료 처리
- 업무 템플릿 생성 및 요일별/일괄 적용
- taskType을 통한 정기/비정기 업무 구분
- 담당자 할당 및 추적
- 완료 상태 관리

**바로가기**: [task.md →](./schema/task.md)

---

### [코드 시스템](./schema/code-system.md) ⭐ 중요
`code`, `code_major`, `code_sub` 테이블 및 **CodeType Enum**

**주요 내용**:
- CodeType Enum을 통한 타입 안전한 코드 관리
- 코드 분류: WS, CR, CS, CT, TT, RL
- 코드 값 및 의미
- JPA 컨버터 자동 변환

**바로가기**: [code-system.md →](./schema/code-system.md)

---

### [FAQ 시스템](./schema/faq.md)
`faq`, `category`, `sub_category`, `faq_history` 테이블 구조

**주요 내용**:
- 계층적 FAQ 분류 (대분류 → 소분류)
- 작성자/수정자 추적
- 수정 이력 관리 (감사 로그)
- 소프트 삭제

**바로가기**: [faq.md →](./schema/faq.md)

---

## 🔍 빠른 참조

### 주요 테이블

| 테이블 | 주요 컬럼 | 인덱스 | 관계 |
|--------|----------|--------|------|
| **user** | user_id, email, role_code | email (UNIQUE) | → organization |
| **work_schedule** | schedule_id, user_id, status_code | user_id, status_code | ← user |
| **work_attendance** | attendance_id, user_id, check_type_code | user_id, check_time | ← user |
| **code** | code_id, code_major, code_sub | (code_major, code_sub) (UNIQUE) | - |

### CodeType 코드 분류

| 코드 | 이름 | 예시 | 테이블 |
|------|------|------|--------|
| **WS** | 근무 상태 | WS01(신청), WS02(승인), WS03(거부) | work_schedule |
| **CR** | 변경 요청 타입 | CR01(수정), CR02(삭제) | work_change_request |
| **CS** | 변경 요청 상태 | CS01(대기), CS02(승인), CS03(거부) | work_change_request |
| **CT** | 체크 타입 | CT01(출근), CT02(퇴근) | work_attendance |
| **TT** | 업무 타입 | TT01(정기), TT02(비정기) | task |
| **RL** | 역할 | RL01(학생), RL02(관리자) | user |

자세한 내용은 [코드 시스템 문서](./schema/code-system.md)를 참고하세요.

### 자주 사용하는 JOIN 패턴

```sql
-- 사용자의 근무 일정 조회
SELECT u.name, ws.schedule_date, ws.start_time, ws.end_time
FROM user u
INNER JOIN work_schedule ws ON u.user_id = ws.user_id
WHERE ws.status_code = 'WS02';

-- 사용자의 출퇴근 기록 조회
SELECT u.name, wa.check_time, wa.check_type_code
FROM user u
INNER JOIN work_attendance wa ON u.user_id = wa.user_id
WHERE DATE(wa.check_time) = CURDATE();
```

더 많은 예시는 각 도메인별 스키마 문서의 사용 예시 섹션을 참고하세요.

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [코드 시스템](./schema/code-system.md) - CodeType Enum 상세
- **참고**: [API 문서](../api/README.md) - API와 테이블 매핑
- **참고**: [아키텍처 개요](../architecture/overview.md) - Domain Layer 구조
- **참고**: [아키텍처 개요](../architecture/codebase-structure.md) - 엔티티 및 리포지토리 구조

### 상위/하위 문서
- ⬆️ **상위**: [문서 홈](../README.md)
- ⬇️ **하위**:
  - [사용자/조직 스키마](./schema/user.md)
  - [근무 일정 스키마](./schema/schedule.md)
  - [출퇴근 스키마](./schema/attendance.md)
  - [업무 관리 스키마](./schema/task.md)
  - [코드 시스템](./schema/code-system.md)
  - [FAQ 시스템](./schema/faq.md)
