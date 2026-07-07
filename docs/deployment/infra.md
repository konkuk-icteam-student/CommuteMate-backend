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

CommuteMate는 **Docker Compose** 기반의 **3-Tier 아키텍처**로 배포됩니다.
프론트엔드(Nginx), 백엔드(Spring Boot), 데이터베이스(PostgreSQL)가 각각 컨테이너로 실행되며,
Host Network Mode를 사용하여 localhost를 공유하고 성능을 최적화합니다.

### 3-Tier 아키텍처 계층별 역할

| 계층 | 기술 스택 | 역할 | 포트 |
|------|-----------|------|------|
| **프레젠테이션 계층** | Nginx + React | SPA 제공, API 프록시, WebSocket 프록시 | 80 |
| **애플리케이션 계층** | Spring Boot | REST API, 비즈니스 로직, 인증/인가 | 8080 |
| **데이터 계층** | PostgreSQL 18 | 데이터 저장, 트랜잭션 관리 | 5432 |

---

## 🖥️ 서버 요구 사항

### 최소 사양

| 항목 | 사양 | 비고 |
|------|------|------|
| **OS** | Ubuntu 20.04 LTS | 또는 유사한 Linux 배포판 |
| **CPU** | 2 Cores 이상 | Java + PostgreSQL + Docker 오버헤드 고려 |
| **RAM** | 4.5GB 이상 | Frontend (512MB) + Backend (3GB) + DB (1GB) |
| **Disk** | 20GB 이상 | 로그 및 데이터베이스 볼륨 포함 |
| **Network** | 공인 IP 또는 도메인 | 포트 80, 8080, 5432 개방 필요 |

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

### 전체 구조 (3-Tier)

```
사용자 (브라우저)
    ↓ HTTP/HTTPS :80
┌─────────────────────────────────────┐
│         Host Server                 │
│  ┌───────────────────────────────┐  │
│  │  Docker Compose               │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │  frontend (Nginx)       │  │  │
│  │  │  - Port: 80             │  │  │
│  │  │  - SPA 라우팅          │  │  │
│  │  │  - API 프록시 → :8080  │  │  │
│  │  │  - WebSocket 프록시     │  │  │
│  │  │  - Health Check: ✓      │  │  │
│  │  └─────────────────────────┘  │  │
│  │           ↓ localhost:8080     │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │  app (Spring Boot)      │  │  │
│  │  │  - Port: 8080           │  │  │
│  │  │  - REST API             │  │  │
│  │  │  - Depends: postgres    │  │  │
│  │  │  - Image: GHCR          │  │  │
│  │  └─────────────────────────┘  │  │
│  │           ↓ localhost:5432     │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │  postgres (DB)          │  │  │
│  │  │  - Port: 5432           │  │  │
│  │  │  - Volume: postgres_data│  │  │
│  │  │  - Health Check: ✓      │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 요청 흐름

1. **사용자 요청** → Nginx (포트 80)
2. **정적 파일 요청** (HTML, CSS, JS) → Nginx가 직접 응답
3. **API 요청** (`/api/*`) → Nginx가 `localhost:8080`으로 프록시
4. **WebSocket 요청** (`/ws/*`) → Nginx가 `localhost:8080`으로 프록시
5. **백엔드 처리** → Spring Boot가 비즈니스 로직 실행
6. **데이터베이스 쿼리** → `localhost:5432`로 PostgreSQL 접근

### 0. frontend (프론트엔드)

**이미지**: `ghcr.io/{org}/{frontend-image}:latest` (Multi-stage 빌드)

**포트**: `80` (Host Network Mode)

**컨테이너 정보**:
```yaml
frontend:
  network_mode: "host"
  image: ${FRONTEND_DOCKER_REGISTRY}/${FRONTEND_DOCKER_IMAGE_NAME}:${FRONTEND_IMAGE_TAG}
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:80/health"]
    interval: 30s
    timeout: 10s
    retries: 3
```

**빌드 구조 (Multi-stage Dockerfile)**:

```dockerfile
# Stage 1: Build (Node 20 + pnpm)
FROM node:20-alpine AS builder
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@latest --activate
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

# Stage 2: Production (Nginx alpine)
FROM nginx:alpine AS production
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Nginx 설정 요약 (`fe_cicd/nginx.conf`)**:

| 기능 | 설명 | 경로 |
|------|------|------|
| **SPA 라우팅** | `try_files $uri /index.html` | `/` |
| **API 프록시** | `proxy_pass http://localhost:8080` | `/api` |
| **WebSocket 프록시** | `Upgrade` 헤더 전달 | `/ws` |
| **헬스체크** | `return 200 "healthy\n"` | `/health` |
| **gzip 압축** | 텍스트 리소스 압축 | 전역 |
| **정적 파일 캐싱** | `expires 1y` | `.js, .css, .png` 등 |

**프록시 동작 원리**:

```
사용자 요청: http://example.com/api/auth/login
    ↓
Nginx 수신 (포트 80)
    ↓
location /api 매칭
    ↓
proxy_pass http://localhost:8080
    ↓
Spring Boot 처리 (포트 8080)
    ↓
응답 → Nginx → 사용자
```

**Host Network Mode 장점**:
- ✅ Nginx와 Spring Boot가 **localhost를 공유**
- ✅ `proxy_pass http://localhost:8080`으로 간단히 프록시 가능
- ✅ 별도 Docker 네트워크 설정 불필요
- ✅ 성능 오버헤드 최소화

---

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

**이미지**: `ghcr.io/konkuk-icorganization-student/commutemate-server:latest`

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
# Docker 이미지 설정 - 백엔드
# ================================
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE_NAME=konkuk-icorganization-student/commutemate-server
IMAGE_TAG=latest

# ================================
# Docker 이미지 설정 - 프론트엔드
# ================================
FRONTEND_DOCKER_REGISTRY=ghcr.io
FRONTEND_DOCKER_IMAGE_NAME=konkuk-icorganization-student/commutemate-frontend
FRONTEND_IMAGE_TAG=latest

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
- **3개 컨테이너 모두 localhost 공유** (frontend, app, postgres)

**장점**:
- ✅ 성능 향상 (네트워크 오버헤드 감소)
- ✅ 포트 매핑 설정 단순화
- ✅ 로컬 서비스와 직접 통신 가능
- ✅ **Nginx → Spring Boot 프록시가 localhost로 간단히 구현**

**단점**:
- ⚠️ 호스트 프로세스와 포트 충돌 가능
- ⚠️ Docker 네트워크 격리 없음
- ⚠️ Windows/Mac에서 지원 제한 (Linux 전용)

### Nginx → Spring Boot 프록시 동작 원리

**Host Network Mode에서 localhost 공유**:

```
┌─────────────────────────────────────┐
│         Host Server (localhost)     │
│  ┌───────────────────────────────┐  │
│  │  frontend 컨테이너            │  │
│  │  - listen 80                  │  │
│  │  - proxy_pass http://localhost:8080 (✅ 호스트의 localhost) │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  app 컨테이너                 │  │
│  │  - listen 8080                │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Nginx 설정 예시**:

```nginx
# API 프록시
location /api {
    proxy_pass http://localhost:8080;  # ← Host의 localhost:8080으로 전달
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# WebSocket 프록시
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;      # ← WebSocket Upgrade 헤더
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400;  # WebSocket 타임아웃 (24시간)
}
```

**포트 사용**:
| 서비스 | 포트 | 용도 | 헬스 체크 |
|--------|------|------|----------|
| **frontend** | 80 | Nginx (SPA + 프록시) | `curl http://localhost:80/health` |
| **app** | 8080 | Spring Boot 애플리케이션 | `curl http://localhost:8080/actuator/health` |
| **postgres** | 5432 | PostgreSQL 데이터베이스 | `docker-compose exec postgres pg_isready -U ${DB_USERNAME}` |

**포트 충돌 확인**:
```bash
# 포트 사용 확인
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :5432

# 프로세스 종료 (필요 시)
sudo kill -9 <PID>
```

### WebSocket 프록시 상세

**Spring Boot WebSocket 설정**:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // ← Nginx가 이 경로로 프록시
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Nginx WebSocket 프록시 설정**:

```nginx
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;

    # WebSocket Upgrade 헤더 전달 (필수)
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    # 기본 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # WebSocket 타임아웃 (24시간)
    proxy_read_timeout 86400;
}
```

**WebSocket 연결 흐름**:

```
클라이언트 → ws://example.com/ws
    ↓
Nginx (포트 80) → HTTP Upgrade 요청 수신
    ↓
Upgrade 헤더 전달 → http://localhost:8080/ws
    ↓
Spring Boot → WebSocket 핸드셰이크
    ↓
양방향 WebSocket 연결 성립
```

### 방화벽 설정

```bash
# UFW 방화벽 (Ubuntu)
sudo ufw allow 80/tcp    # 프론트엔드 포트 (필수)
sudo ufw allow 8080/tcp  # 백엔드 포트 (개발/디버깅용)
sudo ufw allow 5432/tcp  # 데이터베이스 포트 (외부 접근 시)
sudo ufw enable

# 방화벽 상태 확인
sudo ufw status
```

**프로덕션 권장 설정**:

```bash
# 프론트엔드(80)만 외부 개방
sudo ufw allow 80/tcp

# 백엔드/DB는 localhost만 접근 (외부 차단)
# → Host Network Mode에서 자동으로 localhost만 접근 가능
```

**이유**:
- ✅ 사용자는 프론트엔드(포트 80)로만 접근
- ✅ Nginx가 내부적으로 `localhost:8080`으로 프록시
- ✅ 백엔드(8080), DB(5432)는 외부 노출 불필요
- ✅ 보안 강화 (공격 표면 최소화)

---

## 🔍 모니터링 및 로그

### 로그 확인

**프론트엔드 로그 실시간 확인**:
```bash
# Nginx 로그
docker-compose -f fe_cicd/docker-compose.yaml logs -f frontend

# 또는 컨테이너 이름 사용
docker logs -f commutemate-frontend
```

**백엔드 로그 실시간 확인**:
```bash
docker-compose logs -f app
```

**DB 로그 실시간 확인**:
```bash
docker-compose logs -f postgres
```

**모든 서비스 로그**:
```bash
# 백엔드 + DB
docker-compose logs -f

# 전체 (프론트 + 백엔드 + DB)
docker-compose logs -f && docker-compose -f fe_cicd/docker-compose.yaml logs -f frontend
```

**특정 시간대 로그**:
```bash
docker-compose logs --since 30m frontend  # 최근 30분
docker-compose logs --tail 100 app        # 마지막 100줄
```

**Nginx 로그 분석**:

```bash
# 접근 로그 (요청 경로, 상태 코드)
docker exec commutemate-frontend tail -f /var/log/nginx/access.log

# 에러 로그
docker exec commutemate-frontend tail -f /var/log/nginx/error.log

# 특정 경로 필터링
docker logs commutemate-frontend 2>&1 | grep "/api"
docker logs commutemate-frontend 2>&1 | grep "/ws"
```

### 서비스 상태 확인

**컨테이너 상태**:
```bash
docker-compose ps
```

**헬스 체크**:
```bash
# ① Frontend 헬스 체크
curl http://localhost:80/health
# 예상 응답: healthy

# ② Backend 헬스 체크
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

# ③ Database 헬스 체크
docker-compose exec postgres pg_isready -U ${DB_USERNAME}
# 예상 응답: /var/run/postgresql:5432 - accepting connections
```

**통합 헬스 체크 스크립트**:

```bash
#!/bin/bash
# health_check.sh - 3-Tier 전체 헬스 체크

echo "🔍 CommuteMate 3-Tier 헬스 체크"
echo "================================"

# Frontend
if curl -sf http://localhost:80/health > /dev/null 2>&1; then
    echo "✅ Frontend (Nginx): healthy"
else
    echo "❌ Frontend (Nginx): unhealthy"
fi

# Backend
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Backend (Spring Boot): healthy"
else
    echo "❌ Backend (Spring Boot): unhealthy"
fi

# Database
if docker-compose exec -T postgres pg_isready -U commutemate_user > /dev/null 2>&1; then
    echo "✅ Database (PostgreSQL): healthy"
else
    echo "❌ Database (PostgreSQL): unhealthy"
fi

echo "================================"
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

## 📡 Nginx 고급 설정

### 성능 튜닝

**워커 프로세스 설정** (`nginx.conf` 글로벌):

```nginx
# CPU 코어 수에 맞춰 자동 설정
worker_processes auto;

# 워커 연결 수 (기본 1024)
events {
    worker_connections 2048;
}
```

**버퍼 및 타임아웃 최적화**:

```nginx
http {
    # 클라이언트 요청 버퍼
    client_body_buffer_size 128k;
    client_max_body_size 10m;

    # 백엔드 프록시 버퍼
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;
    proxy_busy_buffers_size 8k;

    # 타임아웃 설정
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
}
```

### 보안 강화

**보안 헤더 추가**:

```nginx
server {
    # 보안 헤더
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;

    # HSTS (HTTPS 전용)
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}
```

**Rate Limiting** (DDoS 방어):

```nginx
http {
    # 요청 속도 제한 존 정의
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

    server {
        location /api {
            # API 요청 속도 제한 (초당 10개, 버스트 20개)
            limit_req zone=api_limit burst=20 nodelay;
            limit_req_status 429;

            proxy_pass http://localhost:8080;
        }
    }
}
```

**IP 블랙리스트/화이트리스트**:

```nginx
# 특정 IP 차단
location /admin {
    deny 192.168.1.100;
    allow all;
    proxy_pass http://localhost:8080;
}

# 특정 IP만 허용
location /internal-api {
    allow 10.0.0.0/8;
    deny all;
    proxy_pass http://localhost:8080;
}
```

### 로깅 최적화

**JSON 로그 형식** (파싱 용이):

```nginx
http {
    log_format json_combined escape=json
    '{'
        '"time":"$time_iso8601",'
        '"remote_addr":"$remote_addr",'
        '"method":"$request_method",'
        '"uri":"$request_uri",'
        '"status":$status,'
        '"body_bytes":$body_bytes_sent,'
        '"referer":"$http_referer",'
        '"user_agent":"$http_user_agent",'
        '"request_time":$request_time,'
        '"upstream_response_time":"$upstream_response_time"'
    '}';

    access_log /var/log/nginx/access.log json_combined;
}
```

**로그 로테이션** (`/etc/logrotate.d/nginx`):

```bash
/var/log/nginx/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        if [ -f /var/run/nginx.pid ]; then
            kill -USR1 `cat /var/run/nginx.pid`
        fi
    endscript
}
```

### HTTPS 설정 (Let's Encrypt)

**Certbot 설치 및 인증서 발급**:

```bash
# Certbot 설치
sudo apt-get install certbot python3-certbot-nginx

# 인증서 발급 (자동 Nginx 설정)
sudo certbot --nginx -d example.com -d www.example.com

# 자동 갱신 테스트
sudo certbot renew --dry-run
```

**HTTPS 리다이렉트** (`nginx.conf`):

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name example.com www.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 서버
server {
    listen 443 ssl http2;
    server_name example.com www.example.com;

    # Let's Encrypt 인증서
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # SSL 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # ... (나머지 설정)
}
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
