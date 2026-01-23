# 개발 규약 (Conventions)

CommuteMate 프로젝트의 모든 개발 규약과 컨벤션을 정의하는 네비게이션 허브입니다.
일관된 코드 품질과 유지보수성을 위해 모든 개발자가 반드시 준수해야 합니다.

---

## 📑 목차

- [개요](#-개요)
- [규약 문서 목록](#-규약-문서-목록)
- [핵심 원칙](#-핵심-원칙)
- [카테고리별 빠른 링크](#-카테고리별-빠른-링크)
- [규약 적용 체크리스트](#-규약-적용-체크리스트)
- [신규 기여자 가이드](#-신규-기여자-가이드)
- [상황별 참고 순서](#-상황별-참고-순서)
- [관련 문서](#-관련-문서)

---

## 📖 개요

### 규약 시스템의 목표

1. **일관성**: 프로젝트 전체에서 통일된 개발 스타일 유지
2. **품질**: 코드 품질 표준화 및 기술 부채 최소화
3. **효율성**: 새로운 개발자의 빠른 적응과 코드 리뷰 간소화
4. **유지보수성**: 시간이 지나도 코드를 쉽게 이해할 수 있도록 유지

### 규약 적용 범위

- ✅ 모든 Java/Spring Boot 코드
- ✅ 모든 API 엔드포인트
- ✅ 모든 데이터베이스 엔티티
- ✅ 모든 커밋 메시지
- ✅ 모든 예외 처리
- ✅ 모든 문서화

---

## 📚 규약 문서 목록

### 📊 전체 규약 테이블

| 번호 | 규약 | 파일 | 주요 내용 | 대상 | 필수도 |
|------|------|------|---------|------|--------|
| 1 | 🔍 에러 처리 | [`error-handling.md`](./error-handling.md) | 예외 계층, 에러 응답 형식, HTTP 상태 코드 매핑 | Backend 개발자 | ⭐⭐⭐ |
| 2 | 🌐 API 설계 | [`api-conventions.md`](./api-conventions.md) | REST 원칙, Request/Response DTO, 네이밍, HTTP 메서드 | Backend, Frontend 개발자 | ⭐⭐⭐ |
| 3 | 💻 코딩 스타일 | [`code-style.md`](./code-style.md) | 네이밍 컨벤션, 패키지 구조, 주석, Git 컨벤션 | 모든 개발자 | ⭐⭐⭐ |

---

## 🎯 핵심 원칙

### 원칙 1: 일관성 (Consistency)

프로젝트 전체에서 동일한 패턴을 반복합니다.

```
동일 목적 = 동일 방식
```

**예시**:
- 모든 에러는 `Response<ErrorResponseDetail>` 형식으로 반환
- 모든 코드 값은 `CodeType` Enum 사용
- 모든 DTO는 Camel case 필드명 사용

---

### 원칙 2: 명확성 (Clarity)

코드는 사람이 읽기 위해 작성됩니다. 의도가 명확해야 합니다.

```
자기 문서화 코드 > 주석 > 복잡한 로직
```

**예시**:
- 메서드명에서 동작이 명확: `validateEmail()` ✅
- 변수명이 의미를 담음: `isValidToken` ✅
- 복잡한 로직에만 주석: 간단한 코드는 주석 불필요 ✅

---

### 원칙 3: 타입 안전성 (Type Safety)

런타임 오류를 컴파일 타임에 잡아냅니다.

```
문자열 하드코딩 (런타임 오류) < CodeType Enum 사용 (컴파일 타임 오류)
```

**예시**:
- `user.setRoleCode(CodeType.RL01)` ✅ (타입 체크)
- `user.setRoleCode("RL01")` ❌ (오타 시 런타임 오류)

---

### 원칙 4: 표준화 (Standardization)

모든 시스템이 동일한 표준을 따릅니다.

```
프로젝트 표준 = 모든 모듈의 기본값
```

**예시**:
- 날짜/시간: ISO-8601 형식 (`2025-01-23T12:00:00`)
- 응답 래퍼: 항상 `Response<T>` 사용
- 예외 기반: 모두 `BasicException` 상속

---

## 🔗 카테고리별 빠른 링크

### API 개발 (Frontend 연계)

**API를 설계/구현할 때 참고**

| 항목 | 문서 | 설명 |
|------|------|------|
| 🌐 REST 설계 원칙 | [api-conventions.md](./api-conventions.md#-rest-api-원칙) | URL 구조, HTTP 메서드 선택 |
| 📝 Request/Response DTO | [api-conventions.md](./api-conventions.md#-requestresponse-구조) | DTO 설계 패턴 |
| 🏷️ 네이밍 규칙 | [api-conventions.md](./api-conventions.md#-네이밍-규칙) | URL, 파라미터, 필드명 |
| 📅 날짜/시간 형식 | [api-conventions.md](./api-conventions.md#-날짜시간-형식) | ISO-8601 표준 |
| 🔐 JWT 인증 | [api-conventions.md](./api-conventions.md#-jwt-인증) | 토큰 처리 방식 |

**예시**: 새로운 일정 조회 API 개발
```
1. api-conventions.md: 리소스 URL 구조 확인
   → GET /api/v1/schedules/{id} ✅ (동사 사용 금지)
2. Request/Response DTO 섹션: 응답 구조 설계
3. 에러 처리: error-handling.md 참고
```

---

### 에러 및 예외 처리

**예외를 정의/처리할 때 참고**

| 항목 | 문서 | 설명 |
|------|------|------|
| 🏗️ 예외 계층 구조 | [error-handling.md](./error-handling.md#-예외-계층-구조) | BasicException 상속 구조 |
| 📊 응답 형식 | [error-handling.md](./error-handling.md#-응답-형식) | JSON 응답 표준 |
| 🔢 HTTP 상태 코드 | [error-handling.md](./error-handling.md#-http-상태-코드-매핑) | 상황별 상태 코드 |
| 📋 에러 코드 정의 | [error-handling.md](./error-handling.md#-모듈별-에러-코드) | Global, Auth, Schedule 등 |
| 💡 사용 예시 | [error-handling.md](./error-handling.md#-사용-예시) | 실제 구현 예시 |

**예시**: 사용자 미존재 에러 처리
```java
1. error-handling.md: UserNotFoundException 정의 확인
2. HTTP 상태 코드 확인: 404 Not Found
3. GlobalExceptionHandler 에서 처리 확인
4. 구현: throw new UserNotFoundException(userId, message);
```

---

### 코드 작성 및 구조

**코드를 작성/리뷰할 때 참고**

| 항목 | 문서 | 설명 |
|------|------|------|
| 🏷️ 클래스 네이밍 | [code-style.md](./code-style.md#-네이밍-컨벤션) | Entity, Controller, Service 등 |
| 📝 메서드 네이밍 | [code-style.md](./code-style.md#메서드-네이밍) | get, find, create, update, delete 등 |
| 📦 패키지 구조 | [code-style.md](./code-style.md#-패키지-구조) | 모듈별 폴더 조직 |
| 💬 주석 작성 | [code-style.md](./code-style.md#-주석-작성-가이드) | 주석의 언제/어떻게 |
| 📄 포맷팅 | [code-style.md](./code-style.md#-포맷팅-규칙) | 들여쓰기, 라인 길이 등 |

**예시**: 새로운 DTO 클래스 작성
```
1. code-style.md: DTO 네이밍 규칙 확인
   → CreateScheduleRequest ✅
2. 필드명은 camelCase 사용
3. 주석은 필요시만 추가
```

---

### Git 및 버전 관리

**커밋을 작성할 때 참고**

| 항목 | 문서 | 설명 |
|------|------|------|
| 📝 커밋 메시지 | [code-style.md](./code-style.md#-git-컨벤션) | Conventional Commits 형식 |
| 🎯 커밋 범위 | [code-style.md](./code-style.md#커밋-메시지-작성-규칙) | feat, fix, docs, style 등 |
| 📋 브랜치 전략 | [code-style.md](./code-style.md#-브랜치-전략) | feature, bugfix, hotfix 등 |

**예시**: 새로운 기능 커밋
```
feat: 사용자 근무일정 조회 API 추가

- WorkScheduleController에 GET /api/v1/schedules 엔드포인트 추가
- ScheduleService에 사용자별 일정 조회 로직 구현
- 에러 처리: 사용자 미존재 시 404 반환
```

---

## 📊 규약 적용 체크리스트

### 신규 API 엔드포인트 개발 체크리스트

```
[ ] api-conventions.md 확인
  [ ] URL 구조가 RESTful인가? (동사 제거, 복수형)
  [ ] 올바른 HTTP 메서드인가? (GET, POST, PUT, DELETE)
  [ ] 버전이 명시되어 있나? (/api/v1/)

[ ] Request/Response DTO 작성
  [ ] DTO 이름이 규칙을 따르는가? (CreateScheduleRequest)
  [ ] 필드명이 camelCase인가?
  [ ] 필드에 @RequestBody, @PathVariable 등이 올바른가?

[ ] 에러 처리 (error-handling.md)
  [ ] 적절한 예외를 정의했나?
  [ ] HTTP 상태 코드가 올바른가?
  [ ] 에러 응답 형식이 Response<ErrorResponseDetail>인가?

[ ] 코딩 스타일 (code-style.md)
  [ ] 메서드명이 동사로 시작하는가?
  [ ] 패키지 구조가 올바른가?
  [ ] 필요한 곳에만 주석을 달았는가?

[ ] Git 커밋 (code-style.md#git-컨벤션)
  [ ] 커밋 메시지가 Conventional Commits 형식인가?
  [ ] 한 커밋이 한 가지 기능만 포함하는가?
```

### 기존 코드 리뷰 체크리스트

```
[ ] API 규약 (api-conventions.md)
  [ ] URL이 RESTful인가?
  [ ] Request/Response 구조가 표준을 따르는가?

[ ] 에러 처리 (error-handling.md)
  [ ] 모든 실패 경로가 올바른 예외를 던지는가?
  [ ] HTTP 상태 코드가 정확한가?
  [ ] 에러 응답 형식이 표준인가?

[ ] 코딩 스타일 (code-style.md)
  [ ] 네이밍 규칙을 따르는가?
  [ ] 패키지 구조가 올바른가?
  [ ] 주석이 명확한가?

[ ] 타입 안전성
  [ ] CodeType Enum을 사용했는가?
  [ ] 문자열 하드코딩이 없는가?
```

---

## 🎓 신규 기여자 가이드

### 1단계: 규약 이해 (30분)

**목표**: 프로젝트의 핵심 규약을 이해하기

1. 이 README의 "핵심 원칙" 섹션 읽기
2. 각 규약 문서의 개요 섹션만 읽기 (5-10분/문서)
   - error-handling.md의 "개요" 섹션
   - api-conventions.md의 "개요" 섹션
   - code-style.md의 "개요" 섹션

**학습 성과**:
- 프로젝트의 4대 핵심 원칙을 설명할 수 있음
- 각 규약이 해결하는 문제를 이해함

---

### 2단계: 첫 번째 코드 작성 (1-2시간)

**목표**: 규약을 적용하여 간단한 기능 구현

**작업 흐름**:

1. **API 설계** (15분)
   - api-conventions.md의 "REST API 원칙" 섹션 읽기
   - URL 구조 설계 및 코드 리뷰어와 논의

2. **DTO 작성** (20분)
   - api-conventions.md의 "Request/Response 구조" 섹션 참고
   - 필드명은 camelCase 사용

3. **Service 구현** (30분)
   - 일반적인 Spring Boot 서비스 구현

4. **에러 처리** (20분)
   - error-handling.md의 "사용 예시" 섹션 참고
   - 필요한 예외 정의 및 throw

5. **코드 리뷰** (20분)
   - code-style.md 기준으로 코드 검토
   - 메서드명, 변수명, 주석 등 확인

---

### 3단계: 깊이 있는 학습 (2-3시간)

**목표**: 규약의 모든 세부사항을 이해하고 활용

**각 규약별 학습**:

| 규약 | 학습 항목 | 소요시간 |
|------|---------|--------|
| API | Request 유효성 검증, 페이징, JWT 인증 | 1시간 |
| 에러 | 모듈별 에러 코드 정의, 예외 계층 | 1시간 |
| 코딩 | 패키지 구조, Git 컨벤션, 주석 패턴 | 1시간 |

**학습 방법**:
- 각 섹션 읽기
- 프로젝트 코드에서 해당 패턴 찾아보기
- 기존 코드를 규약에 맞게 리팩토링해보기

---

## 📋 상황별 참고 순서

### 상황 1: 새로운 API 엔드포인트 개발

```
1. api-conventions.md
   ↓
2. 코드 작성 (code-style.md 병행)
   ↓
3. 에러 처리 (error-handling.md)
   ↓
4. Git 커밋 (code-style.md#git-컨벤션)
```

**소요시간**: 2-4시간

---

### 상황 2: 새로운 예외 정의

```
1. error-handling.md 전체
   ↓
2. 기존 예외 구현 코드 검토
   ↓
3. 새로운 예외 클래스 작성
```

**소요시간**: 30분-1시간

---

### 상황 3: 코드 리뷰

```
1. 규약 적용 체크리스트 확인
   ↓
2. 각 규약별로 순서대로 검토
   ↓
3. 의견 제시 및 개선 제안
```

**소요시간**: 20-30분/리뷰

---

### 상황 4: 버그 수정

```
1. 버그 원인 파악
   ↓
2. 수정 시 규약 준수 (특히 에러 처리)
   ↓
3. code-style.md#git-컨벤션 참고하여 커밋
```

**소요시간**: 추가 5-10분 (규약 확인)

---

### 상황 5: 패키지/모듈 추가

```
1. code-style.md#-패키지-구조
   ↓
2. 기존 모듈 구조 분석
   ↓
3. 동일한 패턴으로 새 패키지 생성
```

**소요시간**: 20-30분

---

## 🔗 관련 문서

### 필수 참고 문서

| 문서 | 위치 | 설명 | 연관 규약 |
|------|------|------|---------|
| **프로젝트 개요** | [../README.md](../README.md) | 프로젝트 전체 이해 | 모든 규약 |
| **API 문서** | [../api/README.md](../api/README.md) | 구현된 API 예시 | API 설계 규약 |
| **코드 시스템** | [../database/schema/code-system.md](../database/schema/code-system.md) | CodeType Enum 상세 | API, 코딩 스타일 |
| **아키텍처** | [../architecture/overview.md](../architecture/overview.md) | 계층형 아키텍처 | 코딩 스타일 |
| **데이터베이스** | [../database/README.md](../database/README.md) | DB 스키마 | 모든 규약 |

### 규약 문서 내부 연관

```
error-handling.md
  ↓ (응답 형식 정의)
  ← api-conventions.md
  ← code-style.md (예외 클래스 네이밍)

api-conventions.md
  ↓ (DTO 설계)
  ← code-style.md (네이밍 규칙)
  ← error-handling.md (에러 응답)

code-style.md
  ↓ (네이밍, 구조)
  ← api-conventions.md (API 작성 시)
  ← error-handling.md (예외 작성 시)
```

---

## 📌 자주하는 질문 (FAQ)

### Q1: 규약을 모두 외워야 하나?

**A**: 아니요. 필요할 때 이 README에서 해당 규약의 링크를 따라가면 됩니다.
반복적으로 사용하면 자연스럽게 익힙니다.

---

### Q2: 기존 코드가 규약을 어기고 있어요.

**A**: 발견 시에 이슈나 PR을 통해 리팩토링을 제안해주세요.
코드 품질은 점진적으로 개선됩니다.

---

### Q3: 규약에 없는 상황이 발생했어요.

**A**: 팀 리드에게 문의하고, 결정 후 이 문서에 추가하여 공유하세요.

---

### Q4: 어떤 규약부터 익혀야 하나?

**A**: 다음 순서를 추천합니다:
1. 이 README의 "핵심 원칙"
2. code-style.md (기본 코딩)
3. error-handling.md (예외 처리)
4. api-conventions.md (API 설계)

---

### Q5: 규약 준수를 어떻게 확인하나?

**A**:
- 개발 시: 위 체크리스트 활용
- 커밋 전: 규약을 한 번 훑어보기
- 코드 리뷰: 리뷰어가 체크리스트로 검토

---

## 📝 마지막 안내

### 이 문서의 역할

이 README는 모든 규약 문서를 한 곳에서 찾을 수 있는 **네비게이션 허브**입니다.

- ✅ 어떤 규약이 있는지 빠르게 파악
- ✅ 상황별로 필요한 규약 찾기
- ✅ 규약 간 연관성 이해
- ✅ 신규 개발자 온보딩 가이드

상세 내용은 각 규약 문서를 참고하세요.

### 규약 개선 제안

규약이 현실과 맞지 않거나, 더 나은 방법이 있다면:

1. GitHub Issues에 제안하기
2. 팀 미팅에서 논의
3. 합의 후 문서 업데이트

**모든 개발자의 의견은 귀중합니다.**

---

**마지막 업데이트**: 2025-01-23
**문서 유지보수**: 백엔드 팀
**다음 리뷰 예정**: 분기별 (Q1, Q2, Q3, Q4)
