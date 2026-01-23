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

CommuteMate 백엔드는 **GitHub Actions**를 통한 CI/CD 자동화와 **Docker Compose** 기반 배포를 지원합니다.
이 문서는 초기 배포부터 정기 배포, 롤백까지 전체 배포 프로세스를 안내합니다.

---

## 🔄 CI/CD 파이프라인

### GitHub Actions 워크플로우

**파일 위치**: `.github/workflows/deploy.yml`

### 전체 흐름

```
코드 푸시 (main 브랜치)
  ↓
GitHub Actions 트리거
  ↓
① Build & Test 단계
  - Gradle 빌드
  - 단위 테스트 실행
  - 코드 품질 검사
  ↓
② Docker 빌드 단계
  - Dockerfile 기반 이미지 빌드
  - GHCR에 이미지 푸시
  ↓
③ Deploy 단계
  - 운영 서버 SSH 접속
  - 배포 스크립트 실행
  - docker-compose pull & up
  ↓
배포 완료
  - Slack/Discord 알림 (선택)
  - 헬스 체크 확인
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

**.env 파일 생성**:
```bash
nano .env
```

```bash
# Docker 이미지
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE_NAME=konkuk-icteam-student/commutemate-server
IMAGE_TAG=latest

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

### 4. 초기 배포 실행

```bash
# 이미지 가져오기
docker-compose pull

# 서비스 시작
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

### 5. 헬스 체크

```bash
# 앱 상태 확인
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

## 🔧 수동 배포

### CI/CD 없이 직접 배포

**사용 시기**:
- CI/CD 파이프라인 장애
- 긴급 핫픽스 배포
- 설정 변경만 필요한 경우

**절차**:

1. **서버 접속**:
   ```bash
   ssh deploy@your-server-ip
   cd /home/deploy/commutemate
   ```

2. **최신 이미지 가져오기**:
   ```bash
   docker-compose pull
   ```

3. **서비스 재시작**:
   ```bash
   docker-compose up -d
   ```

4. **배포 확인**:
   ```bash
   # 컨테이너 상태
   docker-compose ps

   # 로그 확인
   docker-compose logs -f app

   # 헬스 체크
   curl http://localhost:8080/actuator/health
   ```

### 설정만 변경하는 경우

```bash
# .env 파일 수정
nano .env

# 서비스 재시작 (이미지 재다운로드 없이)
docker-compose restart

# 변경 사항 확인
docker-compose logs -f app
```

---

## ↩️ 롤백 및 복구

### 이전 버전으로 롤백

#### 1. 특정 이미지 태그로 롤백

```bash
# .env 파일에서 IMAGE_TAG 변경
nano .env
# IMAGE_TAG=v1.2.3 (이전 버전)

# 서비스 재시작
docker-compose pull
docker-compose up -d
```

#### 2. 이미지 이력 확인

```bash
# GHCR에서 사용 가능한 이미지 태그 확인
# https://github.com/orgs/konkuk-icteam-student/packages/container/commutemate-server/versions
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

- [ ] **코드 리뷰 완료**
- [ ] **단위 테스트 통과**
- [ ] **통합 테스트 통과**
- [ ] **.env 파일 백업**
- [ ] **데이터베이스 백업 수행**
- [ ] **배포 계획 공유** (팀원 공지)
- [ ] **롤백 계획 수립**

### 배포 중 체크리스트

- [ ] **GitHub Actions 성공 확인**
- [ ] **Docker 이미지 빌드 성공**
- [ ] **GHCR에 이미지 푸시 완료**
- [ ] **서버 SSH 접속 성공**
- [ ] **docker-compose pull 완료**
- [ ] **서비스 재시작 완료**

### 배포 후 체크리스트

- [ ] **컨테이너 상태 확인** (`docker-compose ps`)
- [ ] **헬스 체크 통과** (`/actuator/health`)
- [ ] **주요 API 엔드포인트 테스트**
  - [ ] 로그인 (`POST /api/auth/login`)
  - [ ] 근무 일정 조회 (`GET /api/schedules`)
- [ ] **로그에 에러 없음** 확인
- [ ] **데이터베이스 연결 확인**
- [ ] **배포 완료 공지** (팀원, Slack/Discord)
- [ ] **모니터링 대시보드 확인**

### 긴급 롤백 체크리스트

- [ ] **롤백 사유 파악**
- [ ] **이전 버전 태그 확인**
- [ ] **.env 파일에서 IMAGE_TAG 변경**
- [ ] **docker-compose pull && up -d**
- [ ] **헬스 체크 통과 확인**
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
