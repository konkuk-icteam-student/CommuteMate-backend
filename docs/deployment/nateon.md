# 네이트온 출근 알림

각 기관의 관리자 팀룸에서 Incoming Webhook을 생성하고, 백엔드 실행 환경에
`NATEON_WEBHOOK_ORG_<기관ID>` 이름으로 설정합니다. 기관 ID는 DB user.organization_id입니다.
팀룸 번호나 임의로 정한 순번이 아닙니다.

아래 숫자와 주소는 설명용 예시이며 실제 기관이나 웹훅을 생성하지 않습니다.

```dotenv
NATEON_WEBHOOK_ORG_17=https://teamroom.nate.com/api/webhook/발급받은_A기관_주소
NATEON_WEBHOOK_ORG_42=https://teamroom.nate.com/api/webhook/발급받은_B기관_주소
```

환경변수를 하나도 설정하지 않아도 앱은 실행됩니다. 등록되지 않은 기관이나 빈 주소는
알림만 생략하며, 다른 기관의 웹훅으로 대신 보내지 않습니다.
이전 NATEON_WEBHOOK_URL, NATEON_ORGANIZATION_ID 설정은 더 이상 사용하지 않습니다.

## 실행 환경 설정

- Docker Compose: 서버의 기존 `.env`에 기관별 변수를 추가합니다. app 서비스가
  `env_file: .env`를 통해 해당 파일의 변수를 컨테이너에 전달합니다.
  변경된 이미지를 배포하고 `docker compose up -d --force-recreate app`으로 재생성합니다.
  새 기관이 추가될 때 YAML을 수정할 필요는 없습니다.
- IDE: 실행 구성의 환경변수에 추가한 뒤 앱을 다시 시작합니다.
- 직접 실행: 백엔드 프로세스 환경변수로 전달합니다. Spring Boot는 `.env`를 자동으로 읽지 않습니다.

웹훅 URL은 비밀값입니다. 저장소에 커밋하거나 로그에 출력하지 마세요.
Docker Compose의 `.env`는 Git에서 제외되어 있으며 기존 배포에도 필요한 파일입니다.

## 발송 동작

학생의 DB 기관 ID로 해당 환경변수를 조회하여 출근 기록의 트랜잭션 커밋 후 한 번 발송합니다.
연속 일정 여러 개를 저장해도 한 번만 알립니다. 홈 출근 검증 실패, 중복 출근, 롤백 시에는 발송하지 않습니다.
content 필드는 UTF-8 URL 인코딩된 application/x-www-form-urlencoded로 전송합니다.
전송은 비동기이며 5초 제한을 적용합니다. 실패해도 출근 기록은 유지됩니다.
현재는 실패 로그만 남기며 자동 재시도나 영속 발송 큐는 없습니다.
서버가 종료되는 순간의 알림은 유실될 수 있습니다.

설정 후 해당 기관의 정상 출근으로 팀룸 메시지를 확인하세요.
HTTP 성공 응답만으로 실제 메시지 수신을 보장하지 않으므로 팀룸 확인이 필요합니다.

출근 알림은 POST /api/v1/home/check-in에 연결되어 있습니다. 홈 출근 성공 후 발송하며, 이미 저장된 과거 출근은 소급 발송하지 않습니다.
