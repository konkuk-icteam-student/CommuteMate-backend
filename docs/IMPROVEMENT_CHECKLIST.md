# 문서 개선 체크리스트

**작성일**: 2026-01-23
**현재 점수**: 85/100
**마지막 업데이트**: 2026-01-23 (종합 API 및 데이터베이스 스키마 문서화 완료)

---

## ✅ 완료된 개선 사항

### Phase 1: sdd/ 폴더 정리 (완료)
- [x] sdd/ 폴더와 docs/ 폴더 중복 분석
- [x] 유용한 문서 docs/로 이동
  - [x] sdd/summary.md → docs/api/endpoints-summary.md
  - [x] sdd/faq-system.md → docs/database/schema/faq.md
- [x] 모든 참조 링크 업데이트
- [x] sdd/ 폴더 삭제

### Phase 2: Priority 1 - 데이터베이스 스키마 문서 완성 (2026-01-23 완료)
- [x] `docs/database/schema/user.md` (339줄) - User 엔티티 상세 문서
  - [x] 테이블 구조 (컬럼 상세, 인덱스)
  - [x] 역할 시스템 (RL01, RL02)
  - [x] 6가지 관계 상세 설명 (Organization, WorkSchedule, WorkAttendance, Faq, ManagerCategory)
  - [x] 생성/수정 로직, Factory 메서드
  - [x] 보안 고려사항 (비밀번호 암호화, 이메일 유일성, RefreshToken)
  - [x] 5가지 주요 쿼리 패턴 및 최적화

- [x] `docs/database/schema/schedule.md` (415줄) - WorkSchedule/MonthlyScheduleConfig 상세
  - [x] work_schedule 테이블 구조 및 상태 코드
  - [x] monthly_schedule_config 테이블 구조
  - [x] 시간 범위 검증 로직
  - [x] 4가지 엔드포인트별 쿼리 패턴

- [x] `docs/database/schema/attendance.md` (380줄) - WorkAttendance 시스템
  - [x] 테이블 구조 및 인덱싱 전략
  - [x] 출근 인증 타입 (CT01: CHECK_IN, CT02: CHECK_OUT)
  - [x] WorkSchedule 연계 관계

- [x] `docs/database/schema/task.md` (378줄) - Task 시스템
  - [x] Task, TaskTemplate, TaskTemplateItem 테이블 구조
  - [x] 템플릿 기반 일괄 생성 로직

- [x] `docs/database/schema/faq.md` (기존) - FAQ 시스템
- [x] `docs/database/schema/code-system.md` (기존) - CodeType Enum 시스템

### Phase 3: Priority 2 - API 문서 완성 (2026-01-23 완료)
- [x] `docs/api/admin.md` (1293줄) - AdminScheduleController 완전 문서화
  - [x] 4개 엔드포인트 상세 문서 (monthly-limit, set-apply-term, 조회 2개)
  - [x] 모든 HTTP 상태 코드 (200, 400, 401, 403, 404, 422, 500, 503)
  - [x] 4가지 에러 시나리오 + 해결 방법
  - [x] 4가지 실제 사용 예시 (cURL)

- [x] `docs/api/schedule.md` (364줄) - WorkSchedule API
- [x] `docs/api/attendance.md` (308줄) - WorkAttendance API
- [x] `docs/api/auth.md` (181줄) - 인증 API
- [x] `docs/api/home.md` (605줄) - 홈화면/대시보드 API
- [x] `docs/api/task.md` (872줄) - Task 시스템 API
- [x] `docs/api/endpoints-summary.md` - 전체 엔드포인트 요약
- [x] `docs/api/README.md` (10.9KB) - API 문서 네비게이션 홈

### Phase 4: 문서 상호 연결 (2026-01-23 완료)
- [x] 모든 API 문서에 "관련 문서" 섹션 추가
- [x] 모든 스키마 문서에 "관련 문서" 섹션 추가
- [x] 모든 문서 간 상호 참조 링크 구성

---

## 🔴 Priority 1: 긴급 (높은 영향도) - ✅ 완료 (2026-01-23)

### 1. 데이터베이스 스키마 문서 생성 - ✅ 완료
**해결**: 모든 핵심 테이블에 대한 스키마 문서가 완성됨

**완성된 문서**:
- [x] `docs/database/schema/user.md` (339줄)
  - User 테이블 구조 및 9개 컬럼 상세
  - roleCode (CodeType.RL01: 학생, RL02: 관리자)
  - 6가지 관계 (Organization, WorkSchedule, WorkAttendance, Faq 2개, ManagerCategory)
  - 생성/수정 로직 및 Factory 메서드
  - 보안 고려사항 5가지
  - 주요 쿼리 패턴 5가지

- [x] `docs/database/schema/schedule.md` (415줄)
  - WorkSchedule 테이블 구조 및 9개 컬럼
  - statusCode (CodeType.WS01~WS04) 상세
  - MonthlyScheduleConfig 테이블 (월별 제한 관리)
  - 시간 범위 검증 로직
  - 4가지 관계 설명

- [x] `docs/database/schema/attendance.md` (380줄)
  - WorkAttendance 테이블 구조 및 8개 컬럼
  - checkTypeCode (CodeType.CT01: 출근, CT02: 퇴근)
  - 인덱싱 전략 (user_id, check_time 복합 인덱스)
  - WorkSchedule과의 관계

- [x] `docs/database/schema/faq.md` (기존, 완료)
  - Category, SubCategory, Faq, FaqHistory 4개 엔티티

- [x] `docs/database/schema/task.md` (378줄)
  - Task, TaskTemplate, TaskTemplateItem 테이블 구조
  - typeCode (CodeType.TT01: 정기, TT02: 비정기)
  - 템플릿 기반 일괄 생성 로직

**소요 시간**: 약 4시간 (예상 2-3시간 대비 실제 완수)
**영향도**: 높음 - 신규 개발자 온보딩 시 필수 ✅ 달성

---

## 🟡 Priority 2: 중요 (중간 영향도) - ✅ 완료 (2026-01-23)

### 2. Task 시스템 문서화 - ✅ 완료
**해결**: Task 모듈 전체 문서화 완성

**완성된 작업**:
- [x] `docs/api/task.md` (872줄)
  - 일일 업무 등록/수정/완료 API
  - 템플릿 생성 및 적용 API
  - 관리자 업무 일괄 관리 API
  - 8가지 엔드포인트 상세 문서

- [x] CLAUDE.md에 Task 모듈 섹션 추가
  - 주요 API 엔드포인트 표
  - 사용 예시 및 실제 시나리오

- [x] `docs/database/schema/task.md` (378줄)
  - Task, TaskTemplate, TaskTemplateItem 상세
  - 템플릿 기반 생성 로직

**소요 시간**: 약 3시간 (예상 1-2시간 대비 실제 완수)
**영향도**: 중간 - Task 기능 사용 시 필요 ✅ 달성

---

### 3. API 문서 일관성 개선 - ✅ 완료
**해결**: 모든 API 문서가 일관된 형식으로 통합 완료

**완성된 작업**:
- [x] `docs/api/admin.md` (1293줄)
  - 관리자 전용 API 완전 분리 및 문서화
  - AdminScheduleController 4개 엔드포인트
  - 월별 제한 설정, 신청 기간 관리
  - 변경 요청 처리, 근무 현황 조회

- [x] `docs/api/home.md` (605줄)
  - 오늘의 근무 시간 조회
  - 출퇴근 상태 조회
  - 근무 현황 조회

- [x] 모든 API 문서 형식 통일
  - ✅ 요청/응답 예시 JSON 형식 통일 (모든 API)
  - ✅ HTTP 상태 코드 설명 통일 (200, 400, 401, 403, 404, 422, 500, 503)
  - ✅ 에러 처리 섹션 통일 (4가지 시나리오 포함)
  - ✅ 사용 예시 통일 (cURL 4+ 예제)

**문서 일관성 지표**:
- 8개 API 문서 모두 동일한 구조 적용
- 모든 문서에 "관련 문서" 섹션 추가
- 모든 엔드포인트에 Request/Response 예시 포함
- 모든 에러 응답에 해결 방법 포함

**소요 시간**: 약 6시간 (예상 2시간 대비 실제 완수)
**영향도**: 중간 - API 사용 편의성 향상 ✅ 달성

---

## 🟢 Priority 3: 권장 (낮은 영향도)

### 4. 문서 간 네비게이션 개선
**문제**: 문서 간 링크가 부족하거나 일방향임

**필요한 작업**:
- [ ] 각 문서 하단에 "관련 문서" 섹션 추가
  - 상위 문서 (⬆️)
  - 하위 문서 (⬇️)
  - 관련 문서 (➡️)

- [ ] `docs/conventions/README.md` 생성
  - 모든 규약 문서 목록
  - 각 규약의 요약

- [ ] `docs/database/README.md` 생성
  - 전체 ERD 링크
  - 각 스키마 문서 목록
  - 테이블 간 관계도

**예상 소요 시간**: 1시간
**영향도**: 낮음 - 문서 탐색 편의성 향상

---

### 5. 예제 코드 추가
**문제**: 일부 문서에 실제 사용 예제가 부족함

**필요한 작업**:
- [ ] `docs/conventions/error-handling.md`에 더 많은 예제 추가
  - 각 HTTP 상태 코드별 실제 코드 예제
  - 커스텀 예외 생성 전체 과정 예제

- [ ] `docs/database/schema/code-system.md`에 예제 추가
  - CodeType 추가 전체 과정 (Enum → DB 마이그레이션)
  - 엔티티에서 사용하는 예제

**예상 소요 시간**: 1시간
**영향도**: 낮음 - 이해도 향상

---

## 📊 통계

### 현재 문서 현황
- **전체 문서**: 40+ 개
- **총 라인 수**: ~4,431 lines
- **주요 섹션**:
  - API 문서: 4개 (auth, schedule, attendance, 신규: endpoints-summary)
  - 아키텍처: 2개 (overview, codebase-structure)
  - 규약: 1개 (error-handling)
  - 데이터베이스: 3개 (code-system, faq, ERD)
  - 온보딩: 3개 (README, onboard, CLAUDE.md)

### 예상 완료 시간
- **Priority 1**: 2-3시간
- **Priority 2**: 3-4시간
- **Priority 3**: 2시간
- **총합**: 7-9시간

---

## 🎯 다음 단계 추천

1. **먼저 할 일**: Priority 1 - 데이터베이스 스키마 문서 5개 생성
   - 가장 자주 참조되는 문서
   - 신규 개발자 온보딩 시 필수

2. **그 다음**: Priority 2 - Task 시스템 및 API 문서 개선
   - 최근 추가된 기능 문서화
   - API 문서 일관성 확보

3. **여유 있을 때**: Priority 3 - 네비게이션 및 예제 개선
   - 문서 품질 향상
   - 사용 편의성 개선

---

## 📝 참고사항

### 문서 작성 시 지켜야 할 원칙
1. **일관된 형식**: 같은 종류의 문서는 같은 구조 사용
2. **명확한 예제**: 추상적 설명보다 구체적 예제 선호
3. **상호 참조**: 관련 문서는 반드시 링크로 연결
4. **최신 유지**: 코드 변경 시 문서도 함께 업데이트

### 템플릿 활용
- 데이터베이스 스키마: `docs/database/schema/faq.md` 참고
- API 문서: `docs/api/schedule.md` 참고
- 규약 문서: `docs/conventions/error-handling.md` 참고
