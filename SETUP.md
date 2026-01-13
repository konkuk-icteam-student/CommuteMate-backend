# CommuteMate Backend 서버 세팅 가이드

## 📋 목차
1. [서버 요구사항](#서버-요구사항)
2. [서버 초기 세팅](#서버-초기-세팅)
3. [GitHub Secrets 설정](#github-secrets-설정)
4. [배포 프로세스](#배포-프로세스)
5. [트러블슈팅](#트러블슈팅)

---

## 🖥️ 서버 요구사항

### 최소 사양
- **OS**: Ubuntu 20.04 LTS 이상
- **CPU**: 2 Core 이상
- **RAM**: 4GB 이상
- **Disk**: 20GB 이상
- **Network**: 공인 IP 또는 도메인

### 필수 소프트웨어
- Docker 20.10 이상
- Docker Compose 2.0 이상
- SSH 서버

---

## 🚀 서버 초기 세팅

### 1. Docker 설치

```bash
# Docker 설치 스크립트 실행
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 로그아웃 후 재로그인하여 권한 적용
# 또는 다음 명령어 실행
newgrp docker

# Docker 버전 확인
docker --version
```

### 2. Docker Compose 설치

```bash
# Docker Compose 설치 (최신 버전)
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 버전 확인
docker-compose --version
```

### 3. 배포 디렉토리 생성

```bash
# 배포 디렉토리 생성 (예: /home/deploy/commutemate)
mkdir -p /home/deploy/commutemate
cd /home/deploy/commutemate
```

### 4. .env 파일 생성

```bash
# .env.example을 복사하여 .env 파일 생성
# (GitHub에서 .env.example 다운로드)
wget https://raw.githubusercontent.com/konkuk-icteam-student/CommuteMate-backend/main/.env.example -O .env

# .env 파일 편집
nano .env
```

**필수 설정 항목**:
```bash
# Docker 이미지
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE_NAME=konkuk-icteam-student/commutemate-server
IMAGE_TAG=sha-initial  # CI/CD에서 자동으로 업데이트됨

# PostgreSQL 데이터베이스
DB_NAME=commutemate
DB_USERNAME=commutemate_user
DB_PASSWORD=강력한_비밀번호_입력
DB_PORT=5432
DB_POOL_SIZE=10

# JPA/Hibernate
JPA_DDL_AUTO=update  # 운영: update, 개발: create-drop

# JWT 토큰 (보안 강화를 위해 랜덤 문자열 생성)
# 생성 방법: openssl rand -base64 32
JWT_SECRET=여기에_강력한_랜덤_문자열_입력
JWT_ACCESS_EXPIRATION_MS=3600000     # 1시간
JWT_REFRESH_EXPIRATION_MS=604800000  # 7일
JWT_ALGORITHM=HmacSHA256

# Gmail SMTP (이메일 인증용)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=앱_비밀번호_16자리

# 프론트엔드 URL
FRONTEND_URL=https://your-frontend-domain.com

# 애플리케이션
APP_PORT=8080
SPRING_APPLICATION_NAME=CommuteMate
SCHEDULE_CONCURRENT_MAX=5
```

### 5. 권한 설정

```bash
# .env 파일 보안 강화 (읽기 전용)
chmod 600 .env

# 디렉토리 소유자 확인
ls -la
```

---

## 🔐 GitHub Secrets 설정

GitHub 리포지토리 → Settings → Secrets and variables → Actions → New repository secret

### 필수 Secrets

| Secret 이름 | 설명 | 예시 |
|-------------|------|------|
| `SSH_HOST` | 서버 IP 또는 도메인 | `123.456.789.0` |
| `SSH_USER` | SSH 사용자명 | `deploy` |
| `SSH_PASSWORD` | SSH 비밀번호 | `your-ssh-password` |
| `DEPLOY_PATH` | 배포 디렉토리 절대 경로 | `/home/deploy/commutemate` |

### ⚠️ 보안 주의사항

- SSH 비밀번호는 **매우 강력한 비밀번호**를 사용하세요
- 가능하면 SSH 키 방식을 사용하는 것이 더 안전합니다
- 서버에서 SSH 비밀번호 인증이 활성화되어 있어야 합니다

---

## 🚀 배포 프로세스

### 자동 배포 (CI/CD)

1. **main 브랜치에 Push**
   ```bash
   git push origin main
   ```

2. **GitHub Actions 자동 실행**
   - Job 1: 테스트 실행
   - Job 2: Docker 이미지 빌드 및 GHCR 푸시
   - Job 3: SSH로 서버 접속 → Docker Compose 배포

3. **배포 과정**
   ```
   CI/CD → 이미지 빌드 (sha-abc123...)
        → 서버 SSH 접속
        → .env의 IMAGE_TAG 업데이트
        → docker-compose pull app
        → docker-compose up -d
        → 배포 완료!
   ```

### 수동 배포 (서버에서 직접)

```bash
# 1. 배포 디렉토리로 이동
cd /home/deploy/commutemate

# 2. .env 파일에서 IMAGE_TAG 수정
nano .env
# IMAGE_TAG=sha-원하는커밋해시

# 3. GHCR 로그인 (Personal Access Token 필요)
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 4. 최신 이미지 Pull
docker-compose pull app

# 5. 서비스 재시작
docker-compose up -d

# 6. 로그 확인
docker-compose logs -f app
```

### 롤백 (이전 버전으로 복구)

```bash
# 1. 이전 커밋의 SHA 확인
git log --oneline -10  # 최근 10개 커밋 보기

# 2. .env 파일에서 IMAGE_TAG를 이전 커밋으로 변경
nano .env
# IMAGE_TAG=sha-이전커밋해시

# 3. Docker Compose 재배포
docker-compose pull app
docker-compose up -d

# 4. 롤백 확인
docker-compose ps
docker-compose logs --tail=50 app
```

---

## 🛠️ 주요 명령어

### 서비스 관리

```bash
# 서비스 시작
docker-compose up -d

# 서비스 중지
docker-compose down

# 서비스 재시작
docker-compose restart

# 실행 중인 컨테이너 확인
docker-compose ps

# 로그 확인 (실시간)
docker-compose logs -f app

# 로그 확인 (마지막 100줄)
docker-compose logs --tail=100 app

# PostgreSQL 접속
docker-compose exec postgres psql -U commutemate_user -d commutemate
```

### 데이터베이스 백업/복원

```bash
# PostgreSQL 백업
docker-compose exec -T postgres pg_dump -U commutemate_user commutemate > backup_$(date +%Y%m%d_%H%M%S).sql

# PostgreSQL 복원
docker-compose exec -T postgres psql -U commutemate_user commutemate < backup_20250113_120000.sql
```

### 이미지 정리

```bash
# 사용하지 않는 이미지 삭제
docker image prune -f

# 사용하지 않는 모든 리소스 삭제 (주의!)
docker system prune -a --volumes
```

---

## 🔍 트러블슈팅

### 1. 포트 충돌 (Port already in use)

**문제**: 8080 또는 5432 포트가 이미 사용 중

**해결**:
```bash
# 포트 사용 프로세스 확인
sudo lsof -i :8080
sudo lsof -i :5432

# 프로세스 종료
sudo kill -9 <PID>

# 또는 .env에서 포트 변경
APP_PORT=8081
DB_PORT=5433
```

### 2. PostgreSQL 연결 실패

**문제**: `Connection refused` 또는 `Cannot connect to database`

**해결**:
```bash
# PostgreSQL 컨테이너 상태 확인
docker-compose ps postgres

# PostgreSQL 로그 확인
docker-compose logs postgres

# 헬스체크 상태 확인
docker inspect commutemate-postgres | grep -A 10 Health

# PostgreSQL 재시작
docker-compose restart postgres
```

### 3. 이미지 Pull 실패

**문제**: `unauthorized: authentication required`

**해결**:
```bash
# GHCR 로그인 확인
docker login ghcr.io

# 로그인 재시도
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 이미지가 Public인지 확인
# GitHub 리포지토리 → Packages → commutemate-server → Visibility
```

### 4. .env 파일 오류

**문제**: 환경 변수가 제대로 로드되지 않음

**해결**:
```bash
# .env 파일 형식 확인 (줄바꿈, 공백 제거)
cat -A .env  # 숨겨진 문자 확인

# docker-compose로 환경변수 확인
docker-compose config

# .env 파일 재생성
cp .env.example .env
nano .env
```

### 5. 디스크 공간 부족

**문제**: `no space left on device`

**해결**:
```bash
# 디스크 사용량 확인
df -h

# Docker 디스크 사용량 확인
docker system df

# 사용하지 않는 리소스 정리
docker system prune -a --volumes

# 오래된 이미지 삭제
docker images | grep commutemate-server
docker rmi <IMAGE_ID>
```

### 6. SSH 연결 실패 (CI/CD)

**문제**: GitHub Actions에서 SSH 연결 실패

**해결**:
```bash
# 1. 서버에서 SSH 비밀번호 인증이 활성화되어 있는지 확인
sudo nano /etc/ssh/sshd_config

# 다음 설정이 있어야 함:
# PasswordAuthentication yes
# PermitRootLogin no  # (root 로그인은 차단 권장)

# 2. sshd 재시작 (설정 변경 시)
sudo systemctl restart sshd

# 3. 로컬에서 SSH 접속 테스트
ssh your-user@your-server-ip
# 비밀번호 입력하여 접속되는지 확인

# 4. GitHub Secrets에 SSH_PASSWORD가 정확히 등록되어 있는지 확인

# 5. SSH 로그 확인 (서버)
sudo tail -f /var/log/auth.log
```

---

## 📊 모니터링

### 서비스 상태 확인

```bash
# 컨테이너 리소스 사용량
docker stats

# 실시간 로그 모니터링
docker-compose logs -f --tail=100

# 헬스체크 상태
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### 애플리케이션 헬스체크

```bash
# Spring Boot Actuator 엔드포인트
curl http://localhost:8080/actuator/health

# 응답 예시
# {"status":"UP"}
```

---
### Rolling Update (Docker Compose 기본)

```bash
# docker-compose up -d는 자동으로 롤링 업데이트 수행
# - 새 컨테이너 생성
# - 헬스체크 통과
# - 이전 컨테이너 종료
```
