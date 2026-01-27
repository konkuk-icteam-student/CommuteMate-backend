# 인프라 구성 (Infrastructure Setup)

## 📑 목차
- [개요](#-개요)
- [서버 요구 사항](#-서버-요구-사항)
- [Docker 아키텍처](#-docker-아키텍처)
- [환경 변수 설정](#-환경-변수-설정)
- [네트워크 구성](#-네트워크-구성)
- [모니터링 및 로그](#-모니터링-및-로그)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate 백엔드는 **Docker Compose** 기반으로 배포됩니다.
PostgreSQL 데이터베이스와 Spring Boot 애플리케이션이 컨테이너로 실행되며,
Host Network Mode를 사용하여 성능을 최적화합니다.

---

## 🖥️ 서버 요구 사항

### 최소 사양

| 항목 | 사양 | 비고 |
|------|------|------|
| **OS** | Ubuntu 20.04 LTS | 또는 유사한 Linux 배포판 |
| **CPU** | 2 Cores 이상 | Java + PostgreSQL + Docker 오버헤드 고려 |
| **RAM** | 4GB 이상 | 최소 4GB, 권장 8GB |
| **Disk** | 20GB 이상 | 로그 및 데이터베이스 볼륨 포함 |
| **Network** | 공인 IP 또는 도메인 | 포트 8080, 5432 개방 필요 |

### 권장 사양

| 항목 | 사양 | 비고 |
|------|------|------|
| **OS** | Ubuntu 22.04 LTS | 최신 LTS 버전 |
| **CPU** | 4 Cores | 동시 사용자 증가 시 |
| **RAM** | 8GB | 안정적인 운영 환경 |
| **Disk** | 50GB SSD | SSD 사용 시 성능 향상 |

### 필수 소프트웨어

```bash
# Docker 설치
sudo apt-get update
sudo apt-get install -y docker.io docker-compose

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 사용자 권한 추가 (선택)
sudo usermod -aG docker $USER
```

---

## 🐳 Docker 아키텍처

### 전체 구조

```
┌─────────────────────────────────────┐
│         Host Server                 │
│  ┌───────────────────────────────┐  │
│  │  Docker Compose               │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │  postgres 컨테이너      │  │  │
│  │  │  - Port: 5432           │  │  │
│  │  │  - Volume: postgres_data│  │  │
│  │  │  - Health Check: ✓      │  │  │
│  │  └─────────────────────────┘  │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │  app 컨테이너           │  │  │
│  │  │  - Port: 8080           │  │  │
│  │  │  - Depends: postgres    │  │  │
│  │  │  - Image: GHCR          │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 1. postgres (데이터베이스)

**이미지**: `postgres:18`

**포트**: `5432` (Host Network Mode)

**볼륨**:
```yaml
volumes:
  - postgres_data:/var/lib/postgresql/data
```

**헬스 체크**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**환경 변수**:
```yaml
environment:
  - POSTGRES_DB=${DB_NAME}
  - POSTGRES_USER=${DB_USERNAME}
  - POSTGRES_PASSWORD=${DB_PASSWORD}
```

### 2. app (백엔드 애플리케이션)

**이미지**: `ghcr.io/konkuk-icteam-student/commutemate-server:latest`

**포트**: `8080` (Host Network Mode)

**의존성**:
```yaml
depends_on:
  postgres:
    condition: service_healthy
```

**환경 변수**: `.env` 파일에서 로드

**재시작 정책**: `always`

### docker-compose.yaml 예시

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:18
    network_mode: "host"
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  app:
    image: ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${IMAGE_TAG}
    network_mode: "host"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      - DB_URL=jdbc:postgresql://localhost:5432/${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - DB_DRIVER=org.postgresql.Driver
      - DB_POOL_SIZE=${DB_POOL_SIZE:-10}
      - JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
      - JPA_DDL_AUTO=${JPA_DDL_AUTO:-update}
      - JWT_SECRET=${JWT_SECRET}
      - JWT_ACCESS_EXPIRATION_MS=${JWT_ACCESS_EXPIRATION_MS}
      - JWT_REFRESH_EXPIRATION_MS=${JWT_REFRESH_EXPIRATION_MS}
      - JWT_ALGORITHM=${JWT_ALGORITHM:-HmacSHA256}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - FRONTEND_URL=${FRONTEND_URL}
      - SCHEDULE_CONCURRENT_MAX=${SCHEDULE_CONCURRENT_MAX:-5}
      - SPRING_APPLICATION_NAME=${SPRING_APPLICATION_NAME:-CommuteMate}
    restart: unless-stopped

volumes:
  postgres_data:
```

---

## ⚙️ 환경 변수 설정

### .env 파일 구조

**위치**: 배포 서버의 `docker-compose.yaml`과 같은 디렉토리

**⚠️ 중요**: 이 파일을 저장소에 커밋하지 마십시오.

```bash
# ================================
# Docker 이미지 설정
# ================================
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE_NAME=konkuk-icteam-student/commutemate-server
IMAGE_TAG=latest

# ================================
# 데이터베이스 설정
# ================================
DB_NAME=commutemate
DB_USERNAME=commutemate_user
DB_PASSWORD=secure_password_here
DB_POOL_SIZE=10

# ================================
# 보안 (JWT)
# ================================
# 최소 256비트(32자) 이상의 랜덤 문자열 사용
JWT_SECRET=your_very_long_and_secure_random_string_here
JWT_ACCESS_EXPIRATION_MS=3600000      # 1시간 (밀리초)
JWT_REFRESH_EXPIRATION_MS=604800000   # 7일 (밀리초)

# ================================
# 메일 (Gmail SMTP)
# ================================
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password-here

# ================================
# 애플리케이션 설정
# ================================
SPRING_APPLICATION_NAME=CommuteMate
SCHEDULE_CONCURRENT_MAX=5
```

### 환경 변수 설명

#### Docker 설정
- `DOCKER_REGISTRY`: Docker 이미지 레지스트리 (GHCR)
- `DOCKER_IMAGE_NAME`: 이미지 이름 (GitHub 저장소 경로)
- `IMAGE_TAG`: 이미지 태그 (latest, v1.0.0 등)

#### 데이터베이스 설정
- `DB_NAME`: 데이터베이스 이름
- `DB_USERNAME`: 데이터베이스 사용자 이름
- `DB_PASSWORD`: 데이터베이스 비밀번호 (강력한 비밀번호 사용)
- `DB_POOL_SIZE`: 커넥션 풀 크기

#### JWT 보안 설정
- `JWT_SECRET`: JWT 서명 키 (최소 256비트)
  ```bash
  # 생성 방법
  openssl rand -base64 32
  ```
- `JWT_ACCESS_EXPIRATION_MS`: AccessToken 유효 기간 (밀리초)
- `JWT_REFRESH_EXPIRATION_MS`: RefreshToken 유효 기간 (밀리초)

#### 메일 설정
- `MAIL_USERNAME`: Gmail 계정
- `MAIL_PASSWORD`: Gmail 앱 비밀번호
  - Gmail 설정 → 보안 → 2단계 인증 → 앱 비밀번호 생성

#### 애플리케이션 설정
- `SPRING_APPLICATION_NAME`: 애플리케이션 이름
- `SCHEDULE_CONCURRENT_MAX`: 월별 최대 동시 근무 인원수 기본값

---

## 🌐 네트워크 구성

### Host Network Mode

**설정**: `network_mode: "host"`

**특징**:
- 컨테이너가 호스트의 네트워킹 네임스페이스를 공유
- 컨테이너의 `localhost:8080` = 호스트의 `localhost:8080`
- 포트 매핑(`-p`) 불필요

**장점**:
- ✅ 성능 향상 (네트워크 오버헤드 감소)
- ✅ 포트 매핑 설정 단순화
- ✅ 로컬 서비스와 직접 통신 가능

**단점**:
- ⚠️ 호스트 프로세스와 포트 충돌 가능
- ⚠️ Docker 네트워크 격리 없음
- ⚠️ Windows/Mac에서 지원 제한 (Linux 전용)

**포트 사용**:
| 서비스 | 포트 | 용도 |
|--------|------|------|
| **app** | 8080 | Spring Boot 애플리케이션 |
| **postgres** | 5432 | PostgreSQL 데이터베이스 |

**포트 충돌 확인**:
```bash
# 포트 사용 확인
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :5432

# 프로세스 종료 (필요 시)
sudo kill -9 <PID>
```

### 방화벽 설정

```bash
# UFW 방화벽 (Ubuntu)
sudo ufw allow 8080/tcp  # 애플리케이션 포트
sudo ufw allow 5432/tcp  # 데이터베이스 포트 (필요 시)
sudo ufw enable

# 방화벽 상태 확인
sudo ufw status
```

---

## 🔍 모니터링 및 로그

### 로그 확인

**앱 로그 실시간 확인**:
```bash
docker-compose logs -f app
```

**DB 로그 실시간 확인**:
```bash
docker-compose logs -f postgres
```

**모든 서비스 로그**:
```bash
docker-compose logs -f
```

**특정 시간대 로그**:
```bash
docker-compose logs --since 30m app  # 최근 30분
docker-compose logs --tail 100 app    # 마지막 100줄
```

### 서비스 상태 확인

**컨테이너 상태**:
```bash
docker-compose ps
```

**헬스 체크**:
```bash
# Spring Actuator 헬스 체크
curl http://localhost:8080/actuator/health

# 예상 응답
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

**데이터베이스 연결 확인**:
```bash
# PostgreSQL 접속
docker-compose exec postgres psql -U ${DB_USERNAME} -d ${DB_NAME}

# 테이블 목록 확인
\dt

# 종료
\q
```

### 리소스 사용량 확인

**컨테이너 리소스 모니터링**:
```bash
docker stats
```

**디스크 사용량**:
```bash
# Docker 볼륨 확인
docker volume ls

# 볼륨 상세 정보
docker volume inspect commutemate_postgres_data
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [배포 가이드](./deployment-guide.md) - 배포 절차 및 자동화
- **참고**: [아키텍처 개요](../architecture/overview.md) - 시스템 아키텍처
- **참고**: [데이터베이스 스키마](../database/README.md) - 데이터베이스 구조

### 상위/하위 문서
- ⬆️ **상위**: [배포 README](./README.md)
- ➡️ **관련**: [배포 가이드](./deployment-guide.md)

### 실무 적용
- **초기 설정**: 서버 요구 사항 확인 → Docker 설치 → .env 파일 설정
- **모니터링**: 주기적인 로그 확인 및 헬스 체크
- **문제 발생**: 로그 분석 및 컨테이너 재시작
