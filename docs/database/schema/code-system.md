# 코드 시스템 (CodeType)

## 📑 목차
- [개요](#-개요)
- [CodeType Enum 구조](#-codetype-enum-구조)
- [코드 분류](#-코드-분류)
- [JPA 매핑 방식](#-jpa-매핑-방식)
- [관련 문서](#-관련-문서)

---

## 📖 개요

CommuteMate는 `CodeType` Enum을 통해 상태/역할/업무 타입 등의 코드 값을 타입 안전하게 관리합니다.

**파일 위치**: `src/main/java/com/better/CommuteMate/global/code/CodeType.java`

---

## 🔢 CodeType Enum 구조

```java
public enum CodeType {
    WS01("WS", "01", "REQUESTED", "신청"),
    WS02("WS", "02", "APPROVED", "승인"),
    WS03("WS", "03", "REJECTED", "반려"),
    WS04("WS", "04", "CANCELLED", "취소"),
    // ...

    private final String majorCode;
    private final String subCode;
    private final String codeName;
    private final String codeValue;

    public String getFullCode() { return majorCode + subCode; }
}
```

---

## 📚 코드 분류

### WS - 근무 상태
| 코드 | 의미 |
|------|------|
| WS01 | 신청 |
| WS02 | 승인 |
| WS03 | 반려 |
| WS04 | 취소 |

### CR - 변경 요청 타입
| 코드 | 의미 |
|------|------|
| CR01 | 수정 요청 |
| CR02 | 삭제 요청 |

### CS - 변경 요청 상태
| 코드 | 의미 |
|------|------|
| CS01 | 대기 |
| CS02 | 승인 |
| CS03 | 거절 |

### CT - 출퇴근 체크 타입
| 코드 | 의미 |
|------|------|
| CT01 | 출근 체크 |
| CT02 | 퇴근 체크 |

### TT - 업무 타입
| 코드 | 의미 |
|------|------|
| TT01 | 정기 업무 |
| TT02 | 비정기 업무 |

### RL - 역할
| 코드 | 의미 |
|------|------|
| RL01 | 학생 |
| RL02 | 관리자 |

---

## 🧩 JPA 매핑 방식

코드 값은 `@Enumerated(EnumType.STRING)`으로 저장됩니다.

```java
@Enumerated(EnumType.STRING)
@Column(name = "status_code", columnDefinition = "CHAR(4)")
private CodeType statusCode;
```

---

## 🔗 관련 문서

- [근무 일정 스키마](./schedule.md)
- [출퇴근 스키마](./attendance.md)
- [User 스키마](./user.md)
