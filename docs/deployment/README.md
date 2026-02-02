# 배포 및 인프라 (Deployment & Infrastructure)

## 📑 목차
- [개요](#-개요)
- [배포 문서 목록](#-배포-문서-목록)
- [핵심 배포 원칙](#-핵심-배포-원칙)
- [빠른 참조](#-빠른-참조)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 프로젝트의 인프라 설정, 서버 요구 사항 및 배포 프로세스를 설명합니다.
Docker Compose 기반 배포와 GitHub Actions를 통한 CI/CD 자동화를 제공합니다.

### 3-Tier 아키텍처

CommuteMate는 **3-Tier 아키텍처**로 구성됩니다:

```
사용자 (브라우저)
    ↓ HTTP/HTTPS (포트 80)
┌─────────────────────────────────┐
│  프론트엔드 (Nginx)             │
│  - SPA 라우팅                   │
│  - 정적 파일 제공               │
│  - API 프록시 (/api → :8080)   │
│  - WebSocket 프록시 (/ws)      │
└─────────────────────────────────┘
    ↓ localhost:8080
┌─────────────────────────────────┐
│  백엔드 (Spring Boot)           │
│  - REST API                     │
│  - 비즈니스 로직                │
│  - 인증/인가                    │
└─────────────────────────────────┘
    ↓ localhost:5432
┌─────────────────────────────────┐
│  데이터베이스 (PostgreSQL)      │
│  - 데이터 저장                  │
│  - 트랜잭션 관리                │
└─────────────────────────────────┘
```

---

## 📚 배포 문서 목록

### 🖥️ [인프라 구성](./infra.md)
서버 요구 사항 및 Docker 아키텍처

**주요 내용**:
- 서버 최소 요구 사항 (CPU, RAM, Disk)
- **3-Tier 아키텍처** (frontend + app + postgres)
- **Nginx 프록시 설정** (/api, /ws 프록시 동작)
- **프론트엔드 빌드 및 배포 구조** (Multi-stage Docker)
- 네트워크 설정 (Host Network Mode)
- 환경 변수 (.env) 설정
- 모니터링 및 로그 확인 방법

**바로가기**: [infra.md →](./infra.md)

---

### 🚀 [배포 가이드](./deployment-guide.md)
단계별 배포 절차 및 자동화

**주요 내용**:
- CI/CD 파이프라인 (GitHub Actions)
- **프론트엔드 Docker 이미지 빌드 절차**
- **통합 배포 vs 개별 배포 시나리오**
- 수동 배포 절차
- 환경 설정 (.env 파일)
- 롤백 및 복구 절차
- **프론트엔드 문제 해결** (502, CORS, WebSocket 등)
- 배포 체크리스트

**바로가기**: [deployment-guide.md →](./deployment-guide.md)

---

## 🎯 핵심 배포 원칙

### 1. 컨테이너화 (Containerization)
- **Docker 기반 배포**: 일관된 실행 환경 보장
- **Docker Compose**: 멀티 컨테이너 오케스트레이션
- **이미지 레지스트리**: GHCR (GitHub Container Registry) 사용

**장점**: 환경 독립성, 배포 일관성, 롤백 용이성

### 2. 자동화 (Automation)
- **CI/CD 파이프라인**: GitHub Actions를 통한 자동 배포
- **빌드 자동화**: 코드 푸시 시 자동 빌드 및 테스트
- **배포 자동화**: main 브랜치 머지 시 자동 배포

**장점**: 수동 에러 감소, 배포 속도 향상, 일관된 배포 프로세스

### 3. 환경 분리 (Environment Separation)
- **설정 외부화**: .env 파일을 통한 환경별 설정
- **시크릿 관리**: GitHub Secrets를 통한 민감 정보 관리
- **프로필 분리**: dev, prod 환경별 설정

**장점**: 보안 강화, 환경별 유연한 설정

### 4. 헬스 체크 (Health Check)
- **Frontend 헬스 체크**: Nginx `/health` 엔드포인트
- **Backend 헬스 체크**: Spring Actuator를 통한 상태 모니터링
- **DB 헬스 체크**: postgres 서비스 준비 상태 확인
- **자동 복구**: 컨테이너 실패 시 자동 재시작

**장점**: 안정성 향상, 장애 조기 감지

**통합 헬스 체크**:
```bash
# 3-Tier 전체 헬스 체크
curl http://localhost:80/health     # Frontend (Nginx)
curl http://localhost:8080/actuator/health  # Backend (Spring Boot)
docker-compose exec postgres pg_isready -U ${DB_USERNAME}  # Database
```

---

## 🔍 빠른 참조

### 자주 사용하는 명령어

| 작업 | 명령어 | 설명 |
|------|--------|------|
| **전체 배포** | `docker-compose up -d` | 모든 서비스 시작 (백그라운드) |
| **프론트엔드 배포** | `docker-compose -f fe_cicd/docker-compose.yaml up -d frontend` | 프론트엔드만 배포 |
| **백엔드 배포** | `docker-compose up -d app` | 백엔드만 배포 |
| **재시작** | `docker-compose restart` | 서비스 재시작 |
| **중지** | `docker-compose down` | 서비스 중지 및 제거 |
| **로그 확인 (프론트)** | `docker-compose -f fe_cicd/docker-compose.yaml logs -f frontend` | 프론트엔드 로그 실시간 확인 |
| **로그 확인 (백엔드)** | `docker-compose logs -f app` | 백엔드 로그 실시간 확인 |
| **상태 확인** | `docker-compose ps` | 서비스 상태 확인 |
| **이미지 업데이트** | `docker-compose pull` | 최신 이미지 가져오기 |
| **헬스 체크 (프론트)** | `curl http://localhost:80/health` | 프론트엔드 상태 확인 |
| **헬스 체크 (백엔드)** | `curl http://localhost:8080/actuator/health` | 백엔드 상태 확인 |

### 주요 파일 위치

| 파일 | 위치 | 용도 |
|------|------|------|
| **docker-compose.yaml** | 프로젝트 루트 | 백엔드 Docker Compose 설정 |
| **fe_cicd/docker-compose.yaml** | `fe_cicd/` | 프론트엔드 Docker Compose 설정 |
| **fe_cicd/Dockerfile** | `fe_cicd/` | 프론트엔드 Docker 이미지 빌드 (Multi-stage) |
| **fe_cicd/nginx.conf** | `fe_cicd/` | Nginx 프록시 설정 |
| **.env** | 배포 서버 | 환경 변수 설정 (저장소에 미포함) |
| **deploy.yml** | `.github/workflows/` | CI/CD 워크플로우 |
| **Dockerfile** | 프로젝트 루트 | 백엔드 Docker 이미지 빌드 설정 |

### 서버 요구 사항 (최소)

```yaml
OS: Ubuntu 20.04 LTS
CPU: 2 Cores
RAM: 4.5GB  # Frontend (512MB) + Backend (3GB) + DB (1GB)
Disk: 20GB
Network:
  - Port 80 (Frontend - Nginx)
  - Port 8080 (Backend - Spring Boot)
  - Port 5432 (Database - PostgreSQL)
```

### 배포 흐름 요약

```
코드 푸시 (main 브랜치)
  ↓
GitHub Actions 트리거
  ↓
① 프론트엔드 빌드
  - pnpm install & build
  - Docker 이미지 빌드 (Node 20 → Nginx alpine)
  - GHCR에 이미지 푸시
  ↓
② 백엔드 빌드
  - Gradle 빌드 및 테스트
  - Docker 이미지 빌드 (Spring Boot)
  - GHCR에 이미지 푸시
  ↓
운영 서버 SSH 접속
  ↓
docker-compose pull (프론트 + 백엔드)
  ↓
docker-compose up -d (순서: postgres → app → frontend)
  ↓
배포 완료 (3-Tier 헬스 체크)
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [인프라 구성](./infra.md) - 서버 및 Docker 설정 상세
- **필수**: [배포 가이드](./deployment-guide.md) - 단계별 배포 절차
- **참고**: [아키텍처 개요](../architecture/overview.md) - 시스템 아키텍처 이해
- **참고**: [개발 규약](../conventions/README.md) - 코드 품질 기준

### 상위/하위 문서
- ⬆️ **상위**: [문서 홈](../README.md)
- ⬇️ **하위**:
  - [인프라 구성](./infra.md)
  - [배포 가이드](./deployment-guide.md)

### 실무 적용
- **초기 배포**: infra.md → deployment-guide.md 순서로 진행
- **정기 배포**: CI/CD 파이프라인 자동 실행 (main 브랜치 푸시)
- **긴급 배포**: deployment-guide.md의 수동 배포 절차 참고
- **문제 발생**: deployment-guide.md의 Troubleshooting 섹션 참고
