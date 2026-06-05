# CommuteMate 백엔드 문서

## 📑 목차
- [개요](#-개요)
- [문서 구조](#-문서-구조)
- [빠른 시작](#-빠른-시작)
- [주요 문서 카테고리](#-주요-문서-카테고리)
- [문서 탐색 가이드](#-문서-탐색-가이드)
- [기여 가이드](#-기여-가이드)

---

## 📖 개요

CommuteMate 백엔드 프로젝트의 종합 문서 허브입니다.
이 문서에서 API 명세, 데이터베이스 스키마, 아키텍처 설계, 개발 규약, 배포 가이드 등
모든 기술 문서에 접근할 수 있습니다.

---

## 📂 문서 구조

```
docs/
├── README.md                    # 📍 현재 문서 (문서 허브)
├── onboard.md                   # 온보딩 가이드
│
├── api/                         # 📡 API 문서
│   ├── README.md                # API 문서 개요
│   ├── auth.md                  # 인증 API
│   ├── schedule.md              # 근무 일정 API
│   ├── attendance.md            # 출퇴근 API
│   ├── home.md                  # 홈/대시보드 API
│   ├── task.md                  # 업무/템플릿 API
│   ├── category.md              # 카테고리 API
│   ├── faq.md                   # FAQ API
│   ├── manager.md               # 매니저 API
│   └── admin.md                 # 관리자 근무 일정 API
│
├── database/                    # 💾 데이터베이스 문서
│   ├── README.md                # DB 문서 개요
│   ├── schema/                  # 스키마 문서
│   │   ├── code-system.md       # CodeType Enum 시스템
│   │   ├── user.md              # User 스키마
│   │   ├── schedule.md          # Schedule 스키마
│   │   ├── attendance.md        # Attendance 스키마
│   │   └── faq.md               # FAQ/Category 스키마
│
├── architecture/                # 🏗️ 아키텍처 문서
│   ├── README.md                # 아키텍처 개요
│   ├── overview.md              # 계층형 아키텍처 상세
│   ├── codebase-structure.md   # 코드베이스 구조
│   └── design-decisions.md      # 설계 결정 기록 (ADR)
│
├── conventions/                 # 📋 개발 규약
│   ├── README.md                # 규약 개요
│   ├── error-handling.md        # 예외 처리 규약
│   ├── api-conventions.md       # API 설계 규약
│   └── code-style.md            # 코딩 스타일 가이드
│
└── deployment/                  # 🚀 배포 문서
    ├── README.md                # 배포 문서 개요
    ├── infra.md                 # 인프라 구성
    └── deployment-guide.md      # 배포 가이드
```

---

## 🚀 빠른 시작

### 신규 개발자를 위한 추천 학습 경로

```
1️⃣ 프로젝트 이해
   └─> 📄 onboard.md (온보딩 가이드)

2️⃣ 아키텍처 파악
   └─> 🏗️ architecture/README.md
       └─> architecture/overview.md

3️⃣ 개발 규약 학습
   └─> 📋 conventions/README.md
       ├─> conventions/code-style.md
       ├─> conventions/api-conventions.md
       └─> conventions/error-handling.md

4️⃣ API 이해
   └─> 📡 api/README.md
       └─> api/auth.md (예시)

5️⃣ 데이터베이스 이해
   └─> 💾 database/README.md
       └─> database/schema/code-system.md

6️⃣ 배포 방법 학습 (선택)
   └─> 🚀 deployment/README.md
       └─> deployment/deployment-guide.md
```

### 상황별 빠른 참조

| 상황 | 참고 문서 |
|------|----------|
| **새로운 API 개발** | [API 규약](./conventions/api-conventions.md) → [API 문서](./api/README.md) |
| **데이터베이스 테이블 추가** | [DB 스키마](./database/README.md) |
| **예외 처리 방법** | [에러 처리 규약](./conventions/error-handling.md) |
| **코드 스타일 확인** | [코딩 스타일](./conventions/code-style.md) |
| **시스템 구조 이해** | [아키텍처 개요](./architecture/overview.md) |
| **배포 방법** | [배포 가이드](./deployment/deployment-guide.md) |

---

## 📚 주요 문서 카테고리

### 📡 [API 문서](./api/README.md)
**목적**: REST API 엔드포인트 명세 및 사용법

**포함 내용**:
- 인증 API (로그인, 회원가입, 토큰 관리)
- 근무 일정 API (신청, 조회, 수정, 삭제)
- 출퇴근 API (QR 체크, 이력 조회)
- 홈/대시보드 API (상태, 요약)
- 관리자 API (설정, 변경 요청 처리)

**주요 파일**:
- [api/README.md](./api/README.md) - API 문서 개요
- [api/auth.md](./api/auth.md) - 인증 API 상세

---

### 💾 [데이터베이스 문서](./database/README.md)
**목적**: 데이터베이스 스키마 및 ERD

**포함 내용**:
- 테이블 구조 (User, WorkSchedule, WorkAttendance 등)
- ERD (Entity Relationship Diagram)
- CodeType Enum 시스템
- 테이블 간 관계 및 제약 조건

**주요 파일**:
- [database/README.md](./database/README.md) - DB 문서 개요
- [database/schema/code-system.md](./database/schema/code-system.md) - CodeType Enum

---

### 🏗️ [아키텍처 문서](./architecture/README.md)
**목적**: 시스템 아키텍처 설계 및 구조

**포함 내용**:
- 계층형 아키텍처 (Controller → Application → Domain)
- 모듈 구조 (auth, schedule, domain, global)
- 요청 처리 흐름
- 보안 아키텍처 (JWT, Spring Security)
- 설계 결정 기록 (ADR)

**주요 파일**:
- [architecture/README.md](./architecture/README.md) - 아키텍처 개요
- [architecture/overview.md](./architecture/overview.md) - 상세 구조
- [architecture/codebase-structure.md](./architecture/codebase-structure.md) - 파일 조직
- [architecture/design-decisions.md](./architecture/design-decisions.md) - ADR

---

### 📋 [개발 규약](./conventions/README.md)
**목적**: 일관된 코드 품질 및 스타일 유지

**포함 내용**:
- 에러 처리 표준 (BasicException, Response<T>)
- API 설계 규약 (REST 원칙, 네이밍)
- 코딩 스타일 (네이밍, 패키지 구조, Git 컨벤션)
- 코드 예시 및 베스트 프랙티스

**주요 파일**:
- [conventions/README.md](./conventions/README.md) - 규약 개요
- [conventions/error-handling.md](./conventions/error-handling.md) - 예외 처리
- [conventions/api-conventions.md](./conventions/api-conventions.md) - API 설계
- [conventions/code-style.md](./conventions/code-style.md) - 코딩 스타일

---

### 🚀 [배포 문서](./deployment/README.md)
**목적**: 인프라 설정 및 배포 절차

**포함 내용**:
- 서버 요구 사항
- Docker Compose 구성
- CI/CD 파이프라인 (GitHub Actions)
- 배포 절차 (초기, 정기, 수동)
- 롤백 및 복구 방법
- 문제 해결 가이드

**주요 파일**:
- [deployment/README.md](./deployment/README.md) - 배포 문서 개요
- [deployment/infra.md](./deployment/infra.md) - 인프라 구성
- [deployment/deployment-guide.md](./deployment/deployment-guide.md) - 배포 가이드

---

## 🧭 문서 탐색 가이드

### 역할별 필독 문서

#### 👨‍💻 백엔드 개발자
**필수**:
1. [온보딩 가이드](./onboard.md)
2. [아키텍처 개요](./architecture/overview.md)
3. [코딩 스타일](./conventions/code-style.md)
4. [API 규약](./conventions/api-conventions.md)
5. [에러 처리](./conventions/error-handling.md)

**권장**:
- [데이터베이스 스키마](./database/README.md)
- [설계 결정 기록](./architecture/design-decisions.md)

#### 👨‍💼 프론트엔드 개발자
**필수**:
1. [API 문서](./api/README.md)
2. [인증 API](./api/auth.md)
3. [에러 처리](./conventions/error-handling.md)

**권장**:
- [데이터베이스 스키마](./database/README.md) (도메인 이해)

#### 🛠️ DevOps / 인프라 담당자
**필수**:
1. [인프라 구성](./deployment/infra.md)
2. [배포 가이드](./deployment/deployment-guide.md)

**권장**:
- [아키텍처 개요](./architecture/overview.md)

#### 📊 프로젝트 매니저
**필수**:
1. [온보딩 가이드](./onboard.md)
2. [아키텍처 개요](./architecture/overview.md)
3. [API 문서](./api/README.md)

---

## 📝 기여 가이드

### 문서 작성 원칙

1. **명확성**: 기술 용어는 처음 사용 시 설명 추가
2. **구조화**: 목차, 섹션 구분, 코드 블록 활용
3. **예시**: 코드 예시는 실제 프로젝트 코드 기반
4. **상호 참조**: 관련 문서 링크 추가
5. **최신성**: 코드 변경 시 문서도 함께 업데이트

### 문서 업데이트 시기

**필수**:
- 새로운 API 엔드포인트 추가 시
- 데이터베이스 스키마 변경 시
- 아키텍처 변경 시 (ADR 추가)

**권장**:
- 주요 버그 수정 시
- 새로운 개발 규약 도입 시
- 배포 프로세스 변경 시

### 문서 작성 템플릿

```markdown
# 문서 제목

## 📑 목차
- [개요](#-개요)
- [주요 섹션](#-주요-섹션)
- [관련 문서](#-관련-문서)

---

## 📖 개요
문서의 목적 및 범위 설명

## 🎯 주요 섹션
상세 내용

## 🔗 관련 문서
- [관련 문서 1](링크)
- [관련 문서 2](링크)
```

---

## 🔗 외부 리소스

### 프로젝트 관련
- **ERD**: [dbdiagram.io](https://dbdiagram.io/d/ku_ict-68db5736d2b621e422822757)
- **저장소**: [GitHub](https://github.com/konkuk-icorganization-student/CommuteMate-backend)
- **이슈 트래커**: [GitHub Issues](https://github.com/konkuk-icorganization-student/CommuteMate-backend/issues)

### 기술 스택 문서
- **Spring Boot**: [공식 문서](https://spring.io/projects/spring-boot)
- **Spring Security**: [공식 문서](https://spring.io/projects/spring-security)
- **JPA/Hibernate**: [공식 문서](https://hibernate.org/orm/documentation/)
- **PostgreSQL**: [공식 문서](https://www.postgresql.org/docs/)

---

## 📞 문의 및 지원

문서 관련 문의사항이나 개선 제안이 있으시면:
- **GitHub Issues**: 문서 개선 제안 및 오류 제보
- **Pull Requests**: 문서 수정 기여

---

**마지막 업데이트**: 2026-01-22
**문서 버전**: 2.0.0
**관리자**: CommuteMate 개발팀
