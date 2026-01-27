# CommuteMate 개발자 온보딩 가이드

**CommuteMate 백엔드** 팀에 오신 것을 환영합니다! 👋
이 가이드는 개발 환경을 설정하고 프로젝트를 시작하는 데 도움을 주기 위해 작성되었습니다.

## 📑 목차
- [필수 조건](#-필수-조건-prerequisites)
- [시작하기](#-시작하기)
- [프로젝트 구조](#-프로젝트-구조)
- [📚 문서 가이드](#-문서-가이드) ⭐ **중요**
- [Git 워크플로우](#-git-워크플로우)
- [도움말](#-도움이-필요하신가요)

---

## 📋 필수 조건 (Prerequisites)

시작하기 전에 다음 항목들이 설치되어 있는지 확인해 주세요:

- **Java Development Kit (JDK) 17**: 이 프로젝트는 Java 17을 사용합니다.
- **Docker & Docker Compose**: 데이터베이스 및 로컬 서버 실행을 위해 필요합니다.
- **Git**: 버전 관리를 위해 필요합니다.
- **IntelliJ IDEA** (권장): 또는 선호하는 Java IDE.

---

## 🚀 시작하기

### 1. 저장소 클론 (Clone Repository)
```bash
git clone https://github.com/konkuk-icteam-student/CommuteMate-backend.git
cd CommuteMate-backend
```

### 2. 환경 설정 (Configure Environment)
프로젝트 루트에 `.env` 파일을 생성하고 다음 키들을 설정합니다 (예제 파일이 있다면 복사해서 사용하세요):
```properties
# Database
DB_NAME=commutemate
DB_USERNAME=commutemate_user
DB_PASSWORD=your_password
DB_PORT=5432

# JWT
JWT_SECRET=your_super_secret_key_must_be_long_enough
```
*참고: 로컬 개발 시 `src/main/resources/application.yaml`은 프로파일 설정에 따라 H2 (In-memory) 또는 로컬 PostgreSQL을 사용하도록 구성되어 있습니다.*

### 3. 프로젝트 빌드 (Build)
Gradle Wrapper를 사용합니다.
```bash
./gradlew clean build
```

### 4. 로컬 실행 (Run Locally)
**옵션 A: Gradle을 통해 실행 (H2 데이터베이스)**
```bash
./gradlew bootRun
```
*기본적으로 H2를 사용할 수 있습니다. `application.yaml`을 확인하세요.*

**옵션 B: Docker Compose를 통해 실행 (PostgreSQL)**
```bash
docker-compose up -d --build
```
이 명령어는 Postgres DB와 백엔드 서버 컨테이너를 모두 시작합니다.

---

## 📂 프로젝트 구조

```
src/main/java/com/better/CommuteMate/
├── auth/          # 로그인, 회원가입 (JWT)
├── schedule/      # 근무 일정 로직
├── attendance/    # QR 출퇴근
├── domain/        # 엔티티 (User, Organization 등)
├── global/        # 공통 설정, 예외 처리
└── ...
```

아키텍처에 대한 자세한 내용은 **[docs/architecture/overview.md](docs/architecture/overview.md)**를 참조하세요.

---

## 📚 문서 가이드

### 🗺️ 문서 네비게이션 맵

신규 개발자라면 다음 순서로 문서를 읽는 것을 권장합니다:

#### 1. **프로젝트 이해하기** (필수)
   - 📖 [아키텍처 개요](docs/architecture/README.md) - 전체 구조 파악
   - 🏗️ [코드베이스 구조](docs/architecture/codebase-structure.md) - 폴더/패키지 구조
   - 🎯 [설계 결정 사항](docs/architecture/design-decisions.md) - 주요 아키텍처 결정

#### 2. **개발 규약 숙지** (필수)
   - 📐 [개발 규약 요약](docs/conventions/README.md) - 모든 규약 한눈에 보기
   - ⚠️ [에러 처리 규약](docs/conventions/error-handling.md) - 예외 처리 표준
   - 🔌 [API 설계 규약](docs/conventions/api-conventions.md) - Request/Response 표준
   - 💻 [코딩 스타일](docs/conventions/code-style.md) - Java/Spring 스타일 가이드

#### 3. **데이터베이스 이해** (권장)
   - 🗄️ [데이터베이스 스키마](docs/database/README.md) - ERD + 테이블 목록
   - 🔗 테이블 관계도 문서는 별도 제공되지 않습니다. (스키마 문서 참고)
   - 💾 [코드 시스템](docs/database/schema/code-system.md) - CodeType Enum 상세

#### 4. **API 개발** (권장)
   - 🔌 [API 참조 요약](docs/api/README.md) - 모든 엔드포인트 목록
   - 도메인별 API 상세:
     - [인증 API](docs/api/auth.md) - 로그인, 회원가입, 토큰 관리
     - [근무 일정 API](docs/api/schedule.md) - 일정 신청, 수정, 조회
     - [출퇴근 API](docs/api/attendance.md) - QR 체크인/아웃
     - [관리자 API](docs/api/admin.md) - 월별 제한, 변경 요청 처리
     - [대시보드 API](docs/api/home.md) - 홈 화면 데이터

#### 5. **배포 및 운영** (선택)
   - 🚀 [배포 가이드 개요](docs/deployment/README.md)
   - 🏗️ [인프라 구조](docs/deployment/infra.md) - Docker, 서버 요구사항
   - 📦 [배포 가이드](docs/deployment/deployment-guide.md) - 단계별 배포 절차

---

### 📂 문서 카테고리별 안내

#### 🔌 API 문서 ([`docs/api/`](docs/api/README.md))
각 도메인별 REST API 엔드포인트, 요청/응답 예시, 에러 처리 등을 상세히 설명합니다.

- **요약 문서**: [API README](docs/api/README.md) - 모든 API 목록, Base URL, 인증 방식, 공통 규칙
- **상세 문서**:
  - [인증 API](docs/api/auth.md) - 로그인, 회원가입, 토큰 재발급, 로그아웃
  - [근무 일정 API](docs/api/schedule.md) - 일정 신청, 수정, 조회, 월별 제한 관리
  - [출퇴근 API](docs/api/attendance.md) - QR 체크인/체크아웃, 출퇴근 이력 조회
  - [관리자 API](docs/api/admin.md) - 월별 제한 설정, 신청 기간 설정, 변경 요청 처리
  - [대시보드 API](docs/api/home.md) - 홈 화면 데이터, 근무 시간 요약

#### 🗄️ 데이터베이스 문서 ([`docs/database/`](docs/database/README.md))
엔티티, 테이블 스키마, 관계, 인덱스 등 데이터베이스 설계를 상세히 설명합니다.

- **요약 문서**: [DB README](docs/database/README.md) - ERD 링크, 전체 테이블 목록, 도메인별 분류
- **스키마 문서** ([`docs/database/schema/`](docs/database/schema/)):
  - [사용자/조직](docs/database/schema/user.md) - `user`, `organization` 테이블
  - [근무 일정](docs/database/schema/schedule.md) - `work_schedule`, `monthly_schedule_config` 테이블
  - [출퇴근](docs/database/schema/attendance.md) - `work_attendance` 테이블
  - [코드 시스템](docs/database/schema/code-system.md) - CodeType Enum, `code`, `code_major`, `code_sub` 테이블
  - [FAQ](docs/database/schema/faq.md) - `faq`, `category`, `faq_history` 테이블
- **관계 문서**: 별도 문서 없음 (각 스키마 문서의 관계 섹션 참고)

#### 📐 개발 규약 ([`docs/conventions/`](docs/conventions/README.md))
프로젝트 전반의 코딩 표준, API 설계 원칙, 에러 처리 규칙을 설명합니다.

- **요약 문서**: [규약 README](docs/conventions/README.md) - 모든 규약 목록 및 핵심 원칙
- **상세 문서**:
  - [에러 처리 규약](docs/conventions/error-handling.md) - 예외 계층, HTTP 상태 코드 매핑, Response 형식
  - [API 설계 규약](docs/conventions/api-conventions.md) - Request/Response 표준, 네이밍 규칙, 페이징
  - [코딩 스타일](docs/conventions/code-style.md) - Java/Spring 스타일 가이드, 패키지 구조

#### 🏗️ 아키텍처 문서 ([`docs/architecture/`](docs/architecture/README.md))
시스템 설계, 계층 구조, 모듈 구성을 설명합니다.

- **요약 문서**: [아키텍처 README](docs/architecture/README.md) - 아키텍처 개요 및 문서 목록
- **상세 문서**:
  - [아키텍처 개요](docs/architecture/overview.md) - 계층형 아키텍처, 모듈 구조, 흐름도
  - [코드베이스 구조](docs/architecture/codebase-structure.md) - 패키지별 설명, 파일 조직
  - [설계 결정 사항](docs/architecture/design-decisions.md) - ADR (Architecture Decision Records)

#### 🚀 배포 문서 ([`docs/deployment/`](docs/deployment/README.md))
인프라 구조, 서버 설정, 배포 절차를 설명합니다.

- **요약 문서**: [배포 README](docs/deployment/README.md) - 배포 프로세스 개요
- **상세 문서**:
  - [인프라 구조](docs/deployment/infra.md) - Docker Compose, 서버 요구사항, 네트워크 구성
  - [배포 가이드](docs/deployment/deployment-guide.md) - 단계별 배포 절차, CI/CD 파이프라인

---

### 🔍 빠른 참조 (Quick Reference)

자주 찾는 문서들:

| 주제 | 문서 | 설명 |
|------|------|------|
| **전체 API 목록** | [API README](docs/api/README.md) | 모든 엔드포인트 요약 및 공통 규칙 |
| **에러 처리 방법** | [에러 처리 규약](docs/conventions/error-handling.md) | 예외 처리 표준 및 HTTP 상태 코드 |
| **테이블 스키마** | [DB README](docs/database/README.md) | ERD + 전체 테이블 목록 |
| **CodeType Enum** | [코드 시스템](docs/database/schema/code-system.md) | WS, CR, CS, CT, TT, RL 코드 설명 |
| **계층형 아키텍처** | [아키텍처 개요](docs/architecture/overview.md) | Controller→Application→Domain 구조 |
| **배포 방법** | [배포 가이드](docs/deployment/deployment-guide.md) | Docker Compose 기반 배포 절차 |
| **근무 일정 API** | [근무 일정 API](docs/api/schedule.md) | 가장 많이 사용되는 API |
| **User 테이블** | [사용자/조직 스키마](docs/database/schema/user.md) | 사용자 및 조직 테이블 구조 |

---

### 📖 문서 읽기 팁

#### 목차 활용
모든 상세 문서는 **목차 (Table of Contents)**가 있습니다. 목차의 링크를 클릭하여 원하는 섹션으로 빠르게 이동할 수 있습니다.

#### 관련 문서 섹션
각 문서 하단에는 **관련 문서** 섹션이 있어 연관된 문서로 쉽게 이동할 수 있습니다.

#### 인라인 링크
문서 내용 중 다른 문서를 참조하는 부분은 **파란색 링크**로 표시되어 있습니다. 클릭하여 상세 내용을 확인할 수 있습니다.

---

## 🤝 Git 워크플로우

우리는 표준 Git 워크플로우를 따릅니다:

1.  **Main Branch**: `main` (배포 가능한 코드)
2.  **Develop Branch**: `develop` (통합 브랜치)
3.  **Feature Branches**: `feature/feature-name` (`develop`에서 생성)

### 커밋 메시지 컨벤션
Conventional Commits 스타일을 사용합니다:
- `feat: 로그인 API 추가`
- `fix: 일정 서비스의 NPE 해결`
- `docs: README 업데이트`
- `refactor: 인증 컨트롤러 정리`

자세한 내용은 [코딩 스타일 가이드](docs/conventions/code-style.md#git-컨벤션)를 참고하세요.

---

## ❓ 도움이 필요하신가요?

- **문서 탐색**: [docs/README.md](docs/README.md)에서 전체 문서 맵을 확인하세요.
- **아키텍처 질문**: [아키텍처 문서](docs/architecture/README.md)를 참고하세요.
- **API 사용법**: [API 문서](docs/api/README.md)를 참고하세요.
- **에러 해결**: [에러 처리 규약](docs/conventions/error-handling.md)을 확인하세요.
- **팀 커뮤니케이션**: 팀 채널에 문의해 주세요.

즐거운 코딩 되세요! 🚀
