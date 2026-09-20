# 출근 및 출퇴근 이력 API

출근은 학생 권한(RL01)의 `POST /api/v1/home/check-in`을 사용합니다.

## 오늘 일정 조회

`GET /api/v1/home/today`

응답의 근무 카드에 있는 scheduleIds를 출근 요청에 그대로 전달합니다.

## 홈 출근

`POST /api/v1/home/check-in`

```json
{
  "scheduleIds": [1]
}
```

로그인한 학생의 오늘 일정이며 연속된 슬롯이어야 합니다.
이미 출근한 일정은 다시 출근할 수 없으며, 첫 슬롯 시작 후 10분을 넘으면 출근이 거부됩니다.
성공 시 출근 기록을 저장하고 scheduleIds와 checkInTime을 반환합니다.
저장 확정 후 학생의 기관에 설정된 네이트온 팀룸으로 알림을 한 번 보냅니다.
알림 실패가 출근 성공을 취소하지는 않습니다.

환경변수 및 배포 설정은 [네이트온 알림 설정](../deployment/nateon.md)을 참고하세요.

## 오늘 출퇴근 기록 조회

`GET /api/attendance/today`

로그인한 학생의 오늘 출퇴근 기록을 조회합니다.

## 날짜별 출퇴근 기록 조회

`GET /api/attendance/history?date=2026-09-13`

날짜 형식은 YYYY-MM-DD입니다. 두 이력 조회 API 모두 학생 권한(RL01)이 필요합니다.
