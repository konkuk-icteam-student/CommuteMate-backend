# CommuteMate-backend
출근부 시스템 백엔드 레포지토리

## 프로젝트 구조

### 주요 패키지
- `domain/` - 도메인 엔티티 및 리포지토리
- `application/` - 비즈니스 로직 및 서비스
- `controller/` - REST API 컨트롤러
- `global/` - 전역 설정 및 공통 코드
  - `code/` - **CodeType Enum 및 Converter**
  - `config/` - Spring 설정
  - `security/` - 보안 설정
  - `exceptions/` - 예외 처리
  - `controller/` - 전역 예외 핸들러

## 코드 시스템

시스템 전체에서 사용하는 코드 값(역할, 상태, 타입 등)은 `global/code/CodeType.java` Enum으로 중앙 관리됩니다.

### 코드 분류
- **WS**: 근무신청 상태 (REQUESTED, APPROVED, REJECTED)
- **CR**: 요청 유형 (EDIT, DELETE)
- **CS**: 요청 상태 (PENDING, APPROVED, REJECTED)
- **CT**: 출근 인증 타입 (CHECK_IN, CHECK_OUT)
- **TT**: 업무 유형 (REGULAR, IRREGULAR)
- **RL**: 사용자 역할 (STUDENT, ADMIN)

자세한 내용은 [`sdd/code-system.md`](./sdd/code-system.md) 참고

## 문서

- [코드 시스템 설계](./sdd/code-system.md) - CodeType Enum 기반 코드 관리 시스템
- [예외 처리 규칙](./sdd/convention.md) - 에러 및 예외 처리 컨벤션
