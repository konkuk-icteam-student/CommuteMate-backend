# Chat (챗봇) API

## 📑 목차
- [개요](#-개요)
- [인증](#-인증)
- [챗봇 질의](#-챗봇-질의)
- [관련 문서](#-관련-문서)

---

## 📖 개요

규정(사규)과 FAQ에 대해 자연어로 질문하면 RAG(검색 증강 생성) 기반 챗봇이 답변하는 API입니다.

프론트엔드는 이 API(Spring 백엔드)만 호출하면 되며, RAG/LLM 서비스(`regulation-rag`)를 직접 호출할 필요가 없습니다.

```
프론트엔드 → CommuteMate-backend (Spring) → regulation-rag (FastAPI, RAG+LLM)
```

Spring 백엔드가 내부적으로 `regulation-rag`의 `POST /api/v1/chat/query`를 호출해 응답을 받아 그대로 프론트에 전달합니다.

**Base Path**: `/api/chat`

---

## 🔐 인증

현재 `SecurityConfig` 기준으로 인증이 강제되지 않습니다. (`permitAll`) — 다른 FAQ API와 동일합니다.

---

## 💬 챗봇 질의

### Endpoint:
**POST /api/chat/query**

규정/FAQ에 대해 자유 형식으로 질문하고 답변을 받는 API입니다.

⚠️ **응답 지연 주의**: 내부적으로 LLM을 호출하므로 응답까지 **최대 약 120초**가 걸릴 수 있습니다. 프론트엔드에서는:
- axios/fetch 타임아웃을 120초 이상으로 넉넉히 설정해야 합니다.
- 응답 대기 중 로딩 인디케이터(스피너 등)를 반드시 표시해야 합니다.

### Request Body:
```json
{
  "query": "연차는 며칠까지 이월할 수 있나요?"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `query` | string | ✅ | 질문 내용 (1~2000자) |

### Response (200 OK) — 규정 근거가 있는 경우:
```json
{
  "isSuccess": true,
  "message": "챗봇 질의 성공",
  "details": {
    "timestamp": "2026-08-01T10:00:00",
    "answer": "연차는 다음 해로 최대 5일까지 이월할 수 있습니다. (근태관리규정 제12조)",
    "regulationSources": [
      {
        "source": "근태관리규정.pdf",
        "page": 4,
        "chunkIndex": 2,
        "score": 0.87
      }
    ],
    "faqSources": [],
    "conflictDetected": false
  }
}
```

### Response (200 OK) — FAQ만 매칭된 경우:
FAQ 근거만으로 답변한 경우, `answer` 앞에 안내 문구가 포함될 수 있습니다 (예: "관련 FAQ를 참고한 답변입니다.").
```json
{
  "isSuccess": true,
  "message": "챗봇 질의 성공",
  "details": {
    "timestamp": "2026-08-01T10:00:00",
    "answer": "관련 FAQ를 참고한 답변입니다. 비밀번호 재설정은 로그인 화면의 '비밀번호 찾기'를 이용하세요.",
    "regulationSources": [],
    "faqSources": [
      { "faqId": 42, "chunkIndex": 0, "score": 0.91 }
    ],
    "conflictDetected": false
  }
}
```

### Response (200 OK) — 매칭되는 규정/FAQ가 없는 경우:
LLM을 호출하지 않고 즉시 안내 문구만 반환합니다.
```json
{
  "isSuccess": true,
  "message": "챗봇 질의 성공",
  "details": {
    "timestamp": "2026-08-01T10:00:00",
    "answer": "관련 규정을 찾을 수 없습니다.",
    "regulationSources": [],
    "faqSources": [],
    "conflictDetected": false
  }
}
```

### Response (200 OK) — 규정 간 충돌이 감지된 경우:
`conflictDetected: true`이면 프론트에서 "답변에 상충되는 규정이 있을 수 있으니 담당자에게 재확인하세요" 같은 경고 문구를 함께 표시해야 합니다.
```json
{
  "isSuccess": true,
  "message": "챗봇 질의 성공",
  "details": {
    "timestamp": "2026-08-01T10:00:00",
    "answer": "관련 규정 조항 간 내용이 상충되어 정확한 답변이 어렵습니다. 담당 부서에 확인해주세요.",
    "regulationSources": [
      { "source": "근태관리규정.pdf", "page": 4, "chunkIndex": 2, "score": 0.81 },
      { "source": "근태관리규정_개정안.pdf", "page": 2, "chunkIndex": 0, "score": 0.79 }
    ],
    "faqSources": [],
    "conflictDetected": true
  }
}
```

### Response (200 OK) — RAG 서비스 장애 시 (폴백):
`regulation-rag` 호출이 실패하거나 타임아웃되면, 500 에러 대신 아래와 같은 폴백 응답을 200 OK로 반환합니다. 프론트는 `answer` 문구를 그대로 사용자에게 보여주면 됩니다.
```json
{
  "isSuccess": true,
  "message": "챗봇 질의 성공",
  "details": {
    "timestamp": "2026-08-01T10:00:00",
    "answer": "일시적으로 챗봇을 이용할 수 없습니다. 잠시 후 다시 시도해주세요.",
    "regulationSources": [],
    "faqSources": [],
    "conflictDetected": false
  }
}
```

### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `answer` | string | 챗봇 답변 텍스트 |
| `regulationSources` | array | 답변 근거가 된 규정 출처 목록 (없으면 빈 배열) |
| `regulationSources[].source` | string | 규정 문서명 |
| `regulationSources[].page` | number | 페이지 번호 |
| `regulationSources[].chunkIndex` | number | 문서 내 청크(조각) 인덱스 |
| `regulationSources[].score` | number | 관련도 점수 (0~1) |
| `faqSources` | array | 답변 근거가 된 FAQ 출처 목록 (없으면 빈 배열) |
| `faqSources[].faqId` | number | FAQ ID (`GET /api/faq/{faqId}`로 상세 조회 가능) |
| `faqSources[].chunkIndex` | number | FAQ 내 청크 인덱스 |
| `faqSources[].score` | number | 관련도 점수 (0~1) |
| `conflictDetected` | boolean | 규정 간 내용 충돌이 감지되었는지 여부 — `true`면 경고 UI 표시 권장 |

---

## 🔗 관련 문서

- [API 문서 홈](./README.md)
- [FAQ API](./faq.md)
- `regulation-rag` 서비스 자체 문서(내부 RAG 구현): 프로젝트 루트의 `regulation-rag/README.md`, OpenAPI docs (`/docs`, RAG 서비스 자체 포트 기준 — 프론트에서 직접 호출하지 않음)
