# 배포 가이드 (Deployment Guide)

## 📑 목차
- [개요](#-개요)
- [CI/CD 파이프라인](#-cicd-파이프라인)
- [초기 배포](#-초기-배포)
- [정기 배포](#-정기-배포)
- [수동 배포](#-수동-배포)
- [롤백 및 복구](#-롤백-및-복구)
- [문제 해결](#-문제-해결)
- [배포 체크리스트](#-배포-체크리스트)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate는 **GitHub Actions**를 통한 CI/CD 자동화와 **Docker Compose** 기반 배포를 지원합니다.
이 문서는 초기 배포부터 정기 배포, 롤백까지 전체 배포 프로세스를 안내합니다.

### 배포 전략

CommuteMate는 **통합 배포**와 **개별 배포** 두 가지 전략을 지원합니다:

| 전략 | 설명 | 사용 시점 | 명령어 |
|------|------|----------|--------|
| **통합 배포** | 프론트 + 백엔드 동시 배포 | 전체 시스템 업데이트 | `docker-compose up -d` (전체) |
| **개별 배포** | 프론트 또는 백엔드만 배포 | UI 변경만, API 변경만 | `docker-compose up -d frontend` 등 |

**이미지 관리**:
- **프론트엔드 이미지**: `ghcr.io/{org}/commutemate-frontend:latest`
- **백엔드 이미지**: `ghcr.io/{org}/commutemate-server:latest`
- 각 이미지는 독립적으로 빌드되고 배포됨

---

## 🔄 CI/CD 파이프라인

### GitHub Actions 워크플로우

**파일 위치**: `.github/workflows/deploy.yml`

### 전체 흐름 (3-Tier)

```
코드 푸시 (main 브랜치)
  ↓
GitHub Actions 트리거
  ↓
① 프론트엔드 Build & Test
  - pnpm install
  - ESLint 코드 품질 검사
  - pnpm build (React → /dist)
  ↓
② 프론트엔드 Docker 빌드
  - Multi-stage Dockerfile (Node 20 → Nginx alpine)
  - GHCR에 이미지 푸시
  ↓
③ 백엔드 Build & Test
  - Gradle 빌드
  - 단위 테스트 실행
  - 코드 품질 검사
  ↓
④ 백엔드 Docker 빌드
  - Dockerfile 기반 이미지 빌드 (Spring Boot)
  - GHCR에 이미지 푸시
  ↓
⑤ Deploy 단계
  - 운영 서버 SSH 접속
  - 배포 스크립트 실행
  - docker-compose pull (프론트 + 백엔드)
  - docker-compose up -d (순서: postgres → app → frontend)
  ↓
배포 완료
  - 3-Tier 헬스 체크 (Frontend, Backend, DB)
  - Slack/Discord 알림 (선택)
```

### 워크플로우 설정

실제 워크플로우는 아래 파일을 기준으로 합니다.
- `.github/workflows/cicd.yaml` (main/dev/feature 브랜치, 테스트 + GHCR 빌드/푸시 + SSH 배포)
- `.github/workflows/deploy.yml` (dev 브랜치, 소스 전송 후 서버에서 Docker 빌드/배포)

요약:
- Java 17 사용
- `cicd.yaml`은 테스트/이미지 푸시/서버 배포를 분리
- `deploy.yml`은 서버에 소스를 복사한 뒤 `deploy.sh`를 실행

### GitHub Secrets 설정

**Settings → Secrets and variables → Actions**에서 다음 시크릿 추가:

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `SERVER_HOST` | 운영 서버 IP 또는 도메인 | `123.456.78.90` |
| `SERVER_USER` | SSH 사용자 이름 | `deploy` |
| `SERVER_SSH_KEY` | SSH Private Key | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `FRONTEND_IMAGE_NAME` | 프론트엔드 Docker 이미지 이름 | `konkuk-icteam-student/commutemate-frontend` |

**SSH Key 생성**:
```bash
# 로컬에서 SSH 키 생성
ssh-keygen -t rsa -b 4096 -C "deploy@commutemate"

# Public Key를 서버에 추가
ssh-copy-id -i ~/.ssh/id_rsa.pub deploy@your-server-ip

# Private Key를 GitHub Secrets에 추가
cat ~/.ssh/id_rsa
```

---

## 🚀 초기 배포

### 1. 서버 준비

**서버 접속**:
```bash
ssh user@your-server-ip
```

**필수 소프트웨어 설치**:
```bash
# 시스템 업데이트
sudo apt-get update
sudo apt-get upgrade -y

# Docker 설치
sudo apt-get install -y docker.io docker-compose

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 사용자 권한 추가
sudo usermod -aG docker $USER
newgrp docker

# 설치 확인
docker --version
docker-compose --version
```

### 2. 배포 디렉토리 생성

```bash
# 배포 디렉토리 생성
sudo mkdir -p /home/deploy/commutemate
sudo chown $USER:$USER /home/deploy/commutemate
cd /home/deploy/commutemate
```

### 3. 설정 파일 준비

**docker-compose.yaml 생성**:
```bash
nano docker-compose.yaml
```

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

**프론트엔드 docker-compose.yaml 생성** (`fe_cicd/docker-compose.yaml`):

```bash
mkdir -p fe_cicd
nano fe_cicd/docker-compose.yaml
```

```yaml
services:
  frontend:
    network_mode: "host"
    image: ${FRONTEND_DOCKER_REGISTRY}/${FRONTEND_DOCKER_IMAGE_NAME}:${FRONTEND_IMAGE_TAG}
    container_name: commutemate-frontend
    restart: unless-stopped
    environment:
      TZ: Asia/Seoul
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:80/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
```

**.env 파일 생성**:
```bash
nano .env
```

```bash
# Docker 이미지 - 백엔드
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE_NAME=konkuk-icteam-student/commutemate-server
IMAGE_TAG=latest

# Docker 이미지 - 프론트엔드
FRONTEND_DOCKER_REGISTRY=ghcr.io
FRONTEND_DOCKER_IMAGE_NAME=konkuk-icteam-student/commutemate-frontend
FRONTEND_IMAGE_TAG=latest

# 데이터베이스
DB_NAME=commutemate
DB_USERNAME=commutemate_user
DB_PASSWORD=your_secure_password_here
DB_POOL_SIZE=10
JPA_DDL_AUTO=update

# JWT (랜덤 문자열 생성: openssl rand -base64 32)
JWT_SECRET=your_jwt_secret_here
JWT_ACCESS_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000
JWT_ALGORITHM=HmacSHA256

# 메일
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# 기타
FRONTEND_URL=http://localhost:3000
SCHEDULE_CONCURRENT_MAX=5
SPRING_APPLICATION_NAME=CommuteMate
```

### 4. 초기 배포 실행 (통합 배포)

```bash
# ① 백엔드 이미지 가져오기 및 시작
docker-compose pull
docker-compose up -d

# ② 프론트엔드 이미지 가져오기 및 시작
docker-compose -f fe_cicd/docker-compose.yaml pull
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# ③ 전체 서비스 상태 확인
docker-compose ps
docker-compose -f fe_cicd/docker-compose.yaml ps

# ④ 로그 확인
docker-compose logs -f app       # 백엔드
docker logs -f commutemate-frontend  # 프론트엔드
```

**간편한 통합 배포 스크립트**:

```bash
#!/bin/bash
# deploy_all.sh - 전체 스택 배포

echo "🚀 CommuteMate 전체 스택 배포"
echo "=============================="

# Backend
echo "📦 백엔드 배포 중..."
docker-compose pull
docker-compose up -d
echo "✅ 백엔드 배포 완료"

# Frontend
echo "🎨 프론트엔드 배포 중..."
docker-compose -f fe_cicd/docker-compose.yaml pull
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend
echo "✅ 프론트엔드 배포 완료"

# Health check
sleep 5
echo "🔍 헬스 체크 중..."
curl -sf http://localhost:80/health && echo "✅ Frontend: healthy" || echo "❌ Frontend: unhealthy"
curl -sf http://localhost:8080/actuator/health && echo "✅ Backend: healthy" || echo "❌ Backend: unhealthy"

echo "=============================="
echo "✅ 배포 완료!"
```

### 5. 헬스 체크 (3-Tier)

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

# ④ 웹 브라우저 접속 테스트
# http://your-server-ip 접속하여 프론트엔드 확인
```

---

## 📅 정기 배포

### 자동 배포 (CI/CD)

**트리거 방법**:
```bash
# 1. 로컬에서 코드 변경
git add .
git commit -m "feat: 새로운 기능 추가"

# 2. main 브랜치에 푸시
git push origin main

# 3. GitHub Actions 자동 실행
# - 빌드 & 테스트
# - Docker 이미지 빌드
# - 운영 서버 배포
```

**배포 진행 상태 확인**:
- GitHub 저장소 → Actions 탭에서 워크플로우 실행 상태 확인

**배포 완료 확인**:
```bash
# 서버 접속
ssh deploy@your-server-ip

# 서비스 상태 확인
cd /home/deploy/commutemate
docker-compose ps

# 최신 로그 확인
docker-compose logs --tail 50 app
```

---

## 🎨 프론트엔드 배포

### 프론트엔드 이미지 빌드 (로컬 테스트)

**개발 환경에서 이미지 빌드**:

```bash
# 프로젝트 루트에서 실행
cd fe_cicd

# Docker 이미지 빌드
docker build -t commutemate-frontend:test .

# 로컬 테스트 실행
docker run -d -p 80:80 --name test-frontend commutemate-frontend:test

# 테스트
curl http://localhost:80/health
# 브라우저에서 http://localhost 접속

# 테스트 완료 후 정리
docker stop test-frontend
docker rm test-frontend
```

### Nginx 설정 변경 시나리오

#### Scenario 1: 백엔드 포트 변경

**상황**: Spring Boot 포트를 8080 → 9090으로 변경

```nginx
# fe_cicd/nginx.conf 수정
location /api {
    proxy_pass http://localhost:9090;  # ← 8080에서 9090으로 변경
    # ... (나머지 설정 동일)
}

location /ws {
    proxy_pass http://localhost:9090;  # ← 8080에서 9090으로 변경
    # ... (나머지 설정 동일)
}
```

**배포 절차**:

```bash
# 1. 이미지 재빌드 (Nginx 설정 포함)
docker build -t ghcr.io/{org}/commutemate-frontend:latest fe_cicd/

# 2. 이미지 푸시
docker push ghcr.io/{org}/commutemate-frontend:latest

# 3. 서버에서 배포
ssh deploy@your-server
docker-compose -f fe_cicd/docker-compose.yaml pull frontend
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# 4. 검증
curl http://localhost:80/api/health  # → localhost:9090으로 프록시됨
```

#### Scenario 2: 새로운 프록시 경로 추가

**상황**: `/uploads` 경로를 백엔드로 프록시

```nginx
# fe_cicd/nginx.conf에 추가
location /uploads {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    # 파일 업로드 크기 제한
    client_max_body_size 100m;
}
```

#### Scenario 3: WebSocket 타임아웃 연장

**상황**: WebSocket 연결이 자주 끊김

```nginx
# fe_cicd/nginx.conf 수정
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    # 타임아웃 연장 (기본 86400초 → 172800초)
    proxy_read_timeout 172800;  # 48시간
    proxy_send_timeout 172800;
}
```

### 프론트엔드만 재배포

**사용 시점**:
- UI 디자인 변경
- 정적 파일 업데이트
- Nginx 설정 변경
- 백엔드는 변경 없음

**절차**:

```bash
# 1. 서버 접속
ssh deploy@your-server-ip

# 2. 프론트엔드 이미지 가져오기
docker-compose -f fe_cicd/docker-compose.yaml pull frontend

# 3. 프론트엔드만 재시작
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# 4. 헬스 체크
curl http://localhost:80/health

# 5. 로그 확인
docker logs -f commutemate-frontend
```

### Nginx 설정 즉시 적용 (무중단)

**nginx.conf만 변경한 경우**:

```bash
# 1. 컨테이너에 새 설정 복사
docker cp fe_cicd/nginx.conf commutemate-frontend:/etc/nginx/conf.d/default.conf

# 2. Nginx 설정 문법 검증
docker exec commutemate-frontend nginx -t

# 3. Nginx 리로드 (무중단)
docker exec commutemate-frontend nginx -s reload

# 4. 검증
curl -I http://localhost:80/api
```

---

## 🔧 수동 배포

### 5가지 배포 시나리오

#### Scenario 1: 전체 스택 재배포

**사용 시점**: 프론트 + 백엔드 모두 업데이트

```bash
# 서버 접속
ssh deploy@your-server-ip
cd /home/deploy/commutemate

# 모든 이미지 가져오기
docker-compose pull
docker-compose -f fe_cicd/docker-compose.yaml pull frontend

# 순서대로 재시작 (postgres → app → frontend)
docker-compose up -d
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# 헬스 체크
curl http://localhost:80/health
curl http://localhost:8080/actuator/health
```

#### Scenario 2: 프론트엔드만 재배포

**사용 시점**: UI 변경, Nginx 설정 변경

```bash
# 프론트엔드 이미지 가져오기
docker-compose -f fe_cicd/docker-compose.yaml pull frontend

# 프론트엔드만 재시작
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# 헬스 체크
curl http://localhost:80/health
docker logs --tail 50 commutemate-frontend
```

#### Scenario 3: 백엔드만 재배포

**사용 시점**: API 로직 변경, 버그 수정

```bash
# 백엔드 이미지 가져오기
docker-compose pull app

# 백엔드만 재시작
docker-compose up -d app

# 헬스 체크
curl http://localhost:8080/actuator/health
docker-compose logs --tail 50 app
```

#### Scenario 4: Nginx 설정만 변경

**사용 시점**: 프록시 설정, 타임아웃, 보안 헤더 변경

```bash
# 방법 1: 이미지 재빌드 없이 즉시 적용 (임시)
docker cp fe_cicd/nginx.conf commutemate-frontend:/etc/nginx/conf.d/default.conf
docker exec commutemate-frontend nginx -t
docker exec commutemate-frontend nginx -s reload

# 방법 2: 이미지 재빌드 (영구 적용)
# 로컬에서 이미지 빌드 → 푸시 → 서버에서 pull & restart
```

#### Scenario 5: 환경 변수만 변경

**사용 시점**: DB 비밀번호, JWT 시크릿 변경

```bash
# .env 파일 수정
nano .env
# 예: JWT_SECRET 변경

# 변경된 환경 변수 적용 (컨테이너 재시작)
docker-compose restart app

# 또는 전체 재시작 (안전)
docker-compose down
docker-compose up -d

# 변경 사항 확인
docker-compose exec app env | grep JWT_SECRET
docker-compose logs -f app
```

---

## ↩️ 롤백 및 복구

### 이전 버전으로 롤백

#### 1. 백엔드 롤백

```bash
# .env 파일에서 IMAGE_TAG 변경
nano .env
# IMAGE_TAG=v1.2.3 (이전 버전)

# 백엔드 서비스 재시작
docker-compose pull app
docker-compose up -d app

# 헬스 체크
curl http://localhost:8080/actuator/health
```

#### 2. 프론트엔드 롤백

```bash
# .env 파일에서 FRONTEND_IMAGE_TAG 변경
nano .env
# FRONTEND_IMAGE_TAG=v1.1.0 (이전 버전)

# 프론트엔드 서비스 재시작
docker-compose -f fe_cicd/docker-compose.yaml pull frontend
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend

# 헬스 체크
curl http://localhost:80/health
```

#### 3. 전체 롤백

```bash
# .env 파일에서 두 태그 모두 변경
nano .env
# IMAGE_TAG=v1.2.3
# FRONTEND_IMAGE_TAG=v1.1.0

# 전체 재시작
docker-compose pull
docker-compose -f fe_cicd/docker-compose.yaml pull frontend
docker-compose up -d
docker-compose -f fe_cicd/docker-compose.yaml up -d frontend
```

#### 4. 이미지 이력 확인

```bash
# GHCR에서 사용 가능한 이미지 태그 확인
# 백엔드: https://github.com/orgs/konkuk-icteam-student/packages/container/commutemate-server/versions
# 프론트엔드: https://github.com/orgs/konkuk-icteam-student/packages/container/commutemate-frontend/versions
```

### 데이터베이스 복구

#### 백업 (정기적으로 수행)

```bash
# PostgreSQL 백업
docker-compose exec postgres pg_dump -U ${DB_USERNAME} ${DB_NAME} > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 복원

```bash
# PostgreSQL 복원
cat backup_YYYYMMDD_HHMMSS.sql | docker-compose exec -T postgres psql -U ${DB_USERNAME} ${DB_NAME}
```

### 긴급 복구 절차

**전체 서비스 중단 및 재시작**:
```bash
# 서비스 중지
docker-compose down

# 볼륨 삭제 (데이터 초기화 - 주의!)
docker volume rm commutemate_postgres_data

# 서비스 재시작
docker-compose up -d
```

---

## 🔍 문제 해결

### 자주 발생하는 문제

#### 1. 컨테이너가 시작되지 않음

**증상**:
```bash
docker-compose ps
# app 컨테이너가 "Restarting" 상태
```

**해결 방법**:
```bash
# 로그 확인
docker-compose logs app

# 일반적인 원인
# - 환경 변수 오류: .env 파일 확인
# - DB 연결 실패: postgres 헬스 체크 확인
# - 포트 충돌: 8080 포트 사용 중인 프로세스 종료
```

#### 2. 데이터베이스 연결 실패

**증상**:
```
ERROR: could not connect to database
```

**해결 방법**:
```bash
# postgres 상태 확인
docker-compose ps postgres

# postgres 로그 확인
docker-compose logs postgres

# 헬스 체크 확인
docker-compose exec postgres pg_isready -U ${DB_USERNAME}

# postgres 재시작
docker-compose restart postgres
```

#### 3. 메모리 부족

**증상**:
```
OOMKilled (Out of Memory)
```

**해결 방법**:
```bash
# 메모리 사용량 확인
docker stats

# 메모리 제한 설정 (docker-compose.yaml)
services:
  app:
    mem_limit: 2g
    mem_reservation: 1g
```

#### 4. 디스크 공간 부족

**해결 방법**:
```bash
# 디스크 사용량 확인
df -h

# 사용하지 않는 Docker 리소스 정리
docker system prune -a

# 로그 파일 정리
sudo journalctl --vacuum-time=7d
```

#### 5. Nginx 502 Bad Gateway

**증상**:
```
브라우저에서 502 Bad Gateway 에러
```

**원인 및 해결**:

```bash
# 원인 1: 백엔드가 다운됨
docker-compose ps app
docker-compose logs app

# 해결: 백엔드 재시작
docker-compose restart app

# 원인 2: 백엔드 포트 불일치
# Nginx 설정: proxy_pass http://localhost:8080
# 실제 백엔드: 포트 9090으로 실행 중
# 해결: Nginx 설정 또는 백엔드 포트 수정

# 원인 3: Host Network Mode 미사용
# 해결: docker-compose.yaml에서 network_mode: "host" 확인
```

#### 6. 프론트엔드 정적 파일 404

**증상**:
```
브라우저 콘솔: Failed to load resource: net::ERR_NAME_NOT_RESOLVED
정적 파일 (JS, CSS) 404 에러
```

**원인 및 해결**:

```bash
# 원인: 빌드 파일이 Nginx에 복사되지 않음
docker exec commutemate-frontend ls /usr/share/nginx/html

# 해결 1: 이미지 재빌드
cd fe_cicd
docker build -t commutemate-frontend:latest .

# 해결 2: Dockerfile 확인
# COPY --from=builder /app/dist /usr/share/nginx/html
# dist 경로가 올바른지 확인 (Vite는 dist, CRA는 build)
```

#### 7. CORS 에러

**증상**:
```
Access to XMLHttpRequest at 'http://example.com/api/...' has been blocked by CORS policy
```

**원인 및 해결**:

```bash
# 원인 1: Nginx 프록시 헤더 누락
# 해결: nginx.conf에 CORS 헤더 추가
location /api {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header Origin $http_origin;

    # CORS 헤더 추가 (필요 시)
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
    add_header Access-Control-Allow-Headers "Content-Type, Authorization" always;
}

# 원인 2: Spring Boot CORS 설정 미흡
# 해결: WebConfig에서 CORS 허용 확인
```

#### 8. WebSocket 연결 실패

**증상**:
```
WebSocket connection to 'ws://example.com/ws' failed
```

**원인 및 해결**:

```bash
# 원인 1: Nginx에서 Upgrade 헤더 미전달
# 해결: nginx.conf 확인
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;      # ← 필수
    proxy_set_header Connection "upgrade";        # ← 필수
    proxy_read_timeout 86400;
}

# 원인 2: WebSocket 타임아웃
# 해결: proxy_read_timeout 증가

# 원인 3: Spring Boot WebSocket 엔드포인트 불일치
# Nginx: /ws → Spring Boot: /stomp (불일치)
# 해결: 경로 통일
```

#### 9. 프론트엔드 컨테이너 재시작 반복

**증상**:
```bash
docker-compose -f fe_cicd/docker-compose.yaml ps
# frontend 상태: Restarting
```

**원인 및 해결**:

```bash
# 원인 1: 포트 80 충돌
sudo netstat -tulpn | grep :80
sudo kill -9 <PID>

# 원인 2: 메모리 부족
docker stats commutemate-frontend
# 해결: 메모리 제한 증가 (docker-compose.yaml)
services:
  frontend:
    mem_limit: 1g

# 원인 3: Nginx 설정 문법 오류
docker logs commutemate-frontend | grep "nginx:"
# 해결: 로컬에서 nginx -t로 검증 후 재배포
```

#### 10. API 프록시가 동작하지 않음

**증상**:
```
http://example.com/api/auth/login → 404 Not Found
```

**원인 및 해결**:

```bash
# 원인 1: Nginx location 설정 누락
# 해결: nginx.conf 확인
location /api {
    proxy_pass http://localhost:8080;  # ← 이 블록이 있는지 확인
}

# 원인 2: proxy_pass 경로 불일치
location /api {
    proxy_pass http://localhost:8080;     # ← /api/... → localhost:8080/api/...
    # proxy_pass http://localhost:8080/;  # ← /api/... → localhost:8080/... (/ 추가 시)
}

# 원인 3: Host Network Mode 미사용
# docker-compose.yaml에서 network_mode: "host" 확인

# 디버깅: Nginx 로그 확인
docker logs commutemate-frontend 2>&1 | grep "/api"
```

### 로그 분석

**앱 로그 확인**:
```bash
# 에러 로그만 확인
docker-compose logs app | grep ERROR

# 특정 시간대 로그
docker-compose logs --since 30m app

# 실시간 로그 (Ctrl+C로 종료)
docker-compose logs -f app
```

---

## ✅ 배포 체크리스트

### 배포 전 체크리스트

- [ ] **코드 리뷰 완료** (프론트 + 백엔드)
- [ ] **단위 테스트 통과** (프론트 + 백엔드)
- [ ] **통합 테스트 통과**
- [ ] **프론트엔드 빌드 성공** (`pnpm build` 에러 없음)
- [ ] **.env 파일 백업**
- [ ] **데이터베이스 백업 수행**
- [ ] **배포 계획 공유** (팀원 공지)
- [ ] **롤백 계획 수립** (이전 이미지 태그 확인)

### 배포 중 체크리스트

- [ ] **GitHub Actions 성공 확인** (프론트 + 백엔드 워크플로우)
- [ ] **프론트엔드 Docker 이미지 빌드 성공**
- [ ] **백엔드 Docker 이미지 빌드 성공**
- [ ] **GHCR에 이미지 푸시 완료** (2개 이미지)
- [ ] **서버 SSH 접속 성공**
- [ ] **docker-compose pull 완료** (프론트 + 백엔드)
- [ ] **서비스 재시작 완료** (순서: postgres → app → frontend)

### 배포 후 체크리스트

- [ ] **컨테이너 상태 확인** (`docker-compose ps` 전체)
- [ ] **Frontend 헬스 체크 통과** (`curl http://localhost:80/health`)
- [ ] **Backend 헬스 체크 통과** (`curl http://localhost:8080/actuator/health`)
- [ ] **Database 헬스 체크 통과** (`pg_isready`)
- [ ] **웹 브라우저 접속 테스트** (`http://your-server-ip`)
  - [ ] 프론트엔드 페이지 로드 확인
  - [ ] 콘솔 에러 없음
- [ ] **주요 API 엔드포인트 테스트**
  - [ ] 로그인 (`POST /api/auth/login`)
  - [ ] 근무 일정 조회 (`GET /api/schedules`)
- [ ] **Nginx 프록시 동작 확인**
  - [ ] `/api` 경로가 백엔드로 프록시됨
  - [ ] `/ws` WebSocket 연결 성공
- [ ] **로그에 에러 없음** 확인 (프론트 + 백엔드)
- [ ] **데이터베이스 연결 확인**
- [ ] **배포 완료 공지** (팀원, Slack/Discord)
- [ ] **모니터링 대시보드 확인**

### 긴급 롤백 체크리스트

- [ ] **롤백 사유 파악** (프론트/백엔드/전체)
- [ ] **이전 버전 태그 확인** (GHCR 패키지 버전)
- [ ] **.env 파일에서 태그 변경**
  - [ ] `IMAGE_TAG` (백엔드)
  - [ ] `FRONTEND_IMAGE_TAG` (프론트엔드)
- [ ] **docker-compose pull && up -d** (해당 서비스)
- [ ] **헬스 체크 통과 확인** (3-Tier 전체)
- [ ] **웹 브라우저 접속 확인**
- [ ] **롤백 완료 공지**
- [ ] **사후 분석 일정 수립**

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [인프라 구성](./infra.md) - 서버 요구 사항 및 Docker 설정
- **참고**: [API 문서](../api/README.md) - 배포 후 테스트할 API 엔드포인트
- **참고**: [아키텍처 개요](../architecture/overview.md) - 시스템 구조 이해

### 상위/하위 문서
- ⬆️ **상위**: [배포 README](./README.md)
- ➡️ **관련**: [인프라 구성](./infra.md)

### 실무 적용
- **초기 배포**: 이 문서의 "초기 배포" 섹션 참고
- **정기 배포**: CI/CD 자동화 활용
- **긴급 배포**: 수동 배포 절차 참고
- **문제 발생**: 문제 해결 섹션 참고 및 로그 분석
