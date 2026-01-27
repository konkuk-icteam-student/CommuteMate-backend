# 아키텍처 (Architecture)

## 📑 목차
- [개요](#-개요)
- [아키텍처 문서 목록](#-아키텍처-문서-목록)
- [핵심 설계 원칙](#-핵심-설계-원칙)
- [빠른 참조](#-빠른-참조)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 프로젝트의 소프트웨어 아키텍처 설계와 구조를 설명합니다.
계층형 아키텍처를 기반으로 모듈화된 구조를 채택하여 유지보수성과 확장성을 확보합니다.

---

## 📚 아키텍처 문서 목록

### 🏗️ [아키텍처 개요](./overview.md)
시스템의 전체 아키텍처 구조와 설계 원칙

**주요 내용**:
- 계층형 아키텍처 (Layered Architecture)
- 모듈별 책임과 역할
- 요청 처리 흐름 (Request Flow)
- 보안 아키텍처 (JWT 인증)
- 데이터베이스 연동 구조

**바로가기**: [overview.md →](./overview.md)

---

### 📁 [코드베이스 구조](./codebase-structure.md)
프로젝트의 물리적 구조와 파일 조직

**주요 내용**:
- 기술 스택 및 의존성
- 프로젝트 디렉토리 구조
- 모듈별 패키지 조직
- 계층별 책임 분리
- 설정 파일 위치 및 역할
- 디자인 패턴 적용

**바로가기**: [codebase-structure.md →](./codebase-structure.md)

---

### 💡 [설계 결정 기록](./design-decisions.md)
주요 아키텍처 결정 사항 및 근거 (ADR)

**주요 내용**:
- 계층형 아키텍처 채택 이유
- JWT 인증 방식 선택
- CodeType Enum 시스템 도입
- Response<T> 래퍼 패턴 적용
- 예외 처리 전략
- 날짜/시간 표준화 (ISO-8601)

**바로가기**: [design-decisions.md →](./design-decisions.md)

---

## 🎯 핵심 설계 원칙

### 1. 계층 분리 (Layered Architecture)
- **Controller**: HTTP 요청/응답 처리
- **Application**: 비즈니스 로직 및 트랜잭션 관리
- **Domain**: 엔티티 정의 및 데이터 접근
- **Infrastructure**: 외부 시스템 연동

**장점**: 각 계층의 독립적인 테스트 및 변경 가능

### 2. 모듈화 (Modularity)
- 도메인별 독립적인 모듈 구성 (auth, schedule, domain/*)
- 각 모듈은 자체 Controller, Service, Repository 보유
- 모듈 간 의존성 최소화

**장점**: 기능 확장 및 유지보수 용이

### 3. 의존성 역전 (Dependency Inversion)
- 인터페이스 기반 설계 (Repository 인터페이스)
- 구체적인 구현에 의존하지 않음
- 생성자 주입 방식으로 의존성 관리

**장점**: 테스트 용이성, 결합도 감소

### 4. 단일 책임 (Single Responsibility)
- 각 클래스는 하나의 명확한 책임만 가짐
- 서비스 클래스는 특정 도메인 로직에만 집중
- Controller는 요청 라우팅에만 집중

**장점**: 코드 이해도 향상, 변경 영향 최소화

---

## 🔍 빠른 참조

### 자주 찾는 아키텍처 정보

| 항목 | 문서 | 설명 |
|------|------|------|
| **계층 구조** | [overview.md](./overview.md#계층형-아키텍처) | Controller → Application → Domain 흐름 |
| **요청 흐름** | [overview.md](./overview.md#요청-처리-흐름) | HTTP 요청부터 응답까지 전체 흐름 |
| **모듈 구조** | [overview.md](./overview.md#모듈-구조) | auth, schedule, domain, global 모듈 설명 |
| **보안 구조** | [overview.md](./overview.md#보안-아키텍처) | JWT 인증 및 필터 체인 |
| **프로젝트 구조** | [codebase-structure.md](./codebase-structure.md#프로젝트-구조) | 디렉토리 및 파일 조직 |
| **기술 스택** | [codebase-structure.md](./codebase-structure.md#기술-스택) | 사용 기술 및 버전 정보 |
| **설계 결정** | [design-decisions.md](./design-decisions.md) | 주요 아키텍처 선택 이유 |

### 핵심 흐름 다이어그램

**인증 요청 흐름**:
```
Client → Controller → AuthService → UserRepository → Database
         ↓
      JwtTokenProvider (토큰 생성)
         ↓
      Response<LoginResponse>
```

**근무 일정 신청 흐름**:
```
Client → WorkScheduleController
         ↓
      ScheduleService
         ↓
      ScheduleValidator (검증)
         ↓
      WorkSchedulesRepository
         ↓
      Database
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [API 문서](../api/README.md) - API 엔드포인트 및 사용법
- **필수**: [데이터베이스 문서](../database/README.md) - 데이터베이스 스키마 및 관계
- **필수**: [개발 규약](../conventions/README.md) - 코딩 스타일 및 API 설계 규약
- **참고**: [온보딩 가이드](../onboard.md) - 프로젝트 시작 가이드

### 상위/하위 문서
- ⬆️ **상위**: [문서 홈](../README.md)
- ⬇️ **하위**:
  - [아키텍처 개요](./overview.md)
  - [코드베이스 구조](./codebase-structure.md)
  - [설계 결정 기록](./design-decisions.md)

### 실무 적용
- **신규 개발자**: overview.md → codebase-structure.md 순서로 학습
- **기능 개발**: overview.md에서 계층 구조 확인 → API/Database 문서 참고
- **아키텍처 변경**: design-decisions.md에서 기존 결정 사항 확인 후 진행
