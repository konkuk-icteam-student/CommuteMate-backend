# Manager 엔티티

## 📋 목차
- [개요](#-개요)
- [테이블 구조](#-테이블-구조)
- [관계](#-관계)
- [ERD 표현](#-erd-표현)
- [주요 기능](#-주요-기능)
- [사용 예시](#-사용-예시)
- [관련 문서](#-관련-문서)

---

## 📖 개요

FAQ 시스템에서 **카테고리별 담당 매니저**를 관리하는 엔티티입니다.

**주요 용도**:
- FAQ 카테고리에 담당 매니저 배정
- FAQ 수정 시 담당 매니저 정보 기록
- 카테고리별 책임자 관리

---

## 🗄️ 테이블 구조

### manager 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 매니저 ID |
| name | VARCHAR(30) | NOT NULL | 매니저 이름 |
| organization | VARCHAR(50) | | 조직 팀 |
| phonenum | VARCHAR(20) | | 연락처 |
| created_at | DATETIME | NOT NULL | 생성 일시 |

**인덱스**:
- PRIMARY KEY: `id`
- INDEX: `name` (매니저 이름으로 검색 최적화)

---

## 🔗 관계

### 1. ManagerCategory (OneToMany)
- **설명**: 한 매니저가 여러 카테고리를 담당할 수 있음
- **관계 타입**: 1:N
- **매핑 방식**: `mappedBy = "manager"`
- **Cascade**: `CascadeType.ALL`
- **OrphanRemoval**: `true`

**Java 코드**:
```java
@OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ManagerCategory> managerCategories = new ArrayList<>();
```

**의미**:
- 매니저 삭제 시 해당 매니저의 모든 카테고리 배정도 함께 삭제
- 카테고리 배정 제거 시 고아 객체 자동 삭제

### 2. Category (ManyToMany - ManagerCategory를 통한 간접 관계)
- **설명**: 매니저와 카테고리는 다대다 관계
- **중간 테이블**: `manager_category`
- **특징**: 배정 일시 추적 가능

---

## 📊 ERD 표현

```
┌─────────────────┐
│    Manager      │
├─────────────────┤
│ id (PK)         │
│ name            │
│ organization            │
│ phonenum        │
│ created_at      │
└─────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────┐
│  ManagerCategory    │
├─────────────────────┤
│ id (PK)             │
│ manager_id (FK)     │───┐
│ category_id (FK)    │   │
│ assigned_at         │   │
└─────────────────────┘   │
                          │
                          │ N:1
                          ▼
                  ┌─────────────────┐
                  │    Category     │
                  ├─────────────────┤
                  │ id (PK)         │
                  │ name            │
                  │ favorite        │
                  └─────────────────┘
```

---

## 🎯 주요 기능

### 1. 카테고리 배정
- 매니저를 특정 FAQ 카테고리에 배정
- 한 매니저가 여러 카테고리 담당 가능
- 배정 일시 자동 기록

### 2. 담당 카테고리 조회
- 매니저가 담당하는 모든 카테고리 목록 조회
- 카테고리별 담당 매니저 목록 조회

### 3. 이력 추적
- FAQ 수정 시 담당 매니저 정보 기록
- FaqHistory에 매니저 이름 목록 저장
- 책임자 추적 및 감사 로그

### 4. 팀/연락처 관리
- 매니저 조직 팀 정보 관리
- 연락처 정보 저장 (선택 사항)

---

## 💡 사용 예시

### 1. 매니저 생성

```java
Manager manager = Manager.builder()
    .name("홍길동")
    .organization("고객지원팀")
    .phonenum("010-1234-5678")
    .build();

managerRepository.save(manager);
```

### 2. 카테고리 배정

```java
// 매니저 조회
Manager manager = managerRepository.findById(1L)
    .orElseThrow(() -> new ManagerNotFoundException());

// 카테고리 조회
Category category = categoryRepository.findById(1L)
    .orElseThrow(() -> new CategoryNotFoundException());

// 배정 생성
ManagerCategory assignment = ManagerCategory.builder()
    .manager(manager)
    .category(category)
    .assignedAt(LocalDateTime.now())
    .build();

managerCategoryRepository.save(assignment);
```

### 3. 매니저의 담당 카테고리 조회

```java
Manager manager = managerRepository.findById(1L)
    .orElseThrow(() -> new ManagerNotFoundException());

List<Category> categories = manager.getManagerCategories().stream()
    .map(ManagerCategory::getCategory)
    .collect(Collectors.toList());
```

### 4. 카테고리의 담당 매니저 조회

```java
Category category = categoryRepository.findById(1L)
    .orElseThrow(() -> new CategoryNotFoundException());

List<Manager> managers = category.getManagers().stream()
    .map(ManagerCategory::getManager)
    .collect(Collectors.toList());
```

### 5. FAQ 이력에 매니저 정보 기록

```java
// FAQ 수정 시 담당 매니저 목록 가져오기
List<String> managerNames = faq.getCategory().getManagers().stream()
    .map(mc -> mc.getManager().getName())
    .collect(Collectors.toList());

// FaqHistory에 저장
FaqHistory history = FaqHistory.builder()
    .title(faq.getTitle())
    .content(faq.getContent())
    .managerNames(managerNames)  // 매니저 이름 목록
    .writerName(faq.getWriter().getName())
    .editedAt(LocalDateTime.now())
    .faq(faq)
    .build();

faqHistoryRepository.save(history);
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [FAQ 시스템](./faq.md) - Faq, FaqHistory, Category 엔티티
- **필수**: [ManagerCategory 관계](./faq.md#managercategory-테이블) - 매니저-카테고리 매핑

### 상위/하위 문서
- ⬆️ **상위**: [데이터베이스 스키마 홈](../README.md)
- ➡️ **관련**: [FAQ API](../../api/faq.md)
- ➡️ **관련**: [Category API](../../api/category.md)

### 코드 위치
- **Entity**: `src/main/java/com/better/CommuteMate/domain/manager/entity/Manager.java`
- **Repository**: `src/main/java/com/better/CommuteMate/domain/manager/repository/ManagerRepository.java`
- **ManagerCategory Entity**: `src/main/java/com/better/CommuteMate/domain/category/entity/ManagerCategory.java`

---

## 📝 참고사항

### 비즈니스 규칙
1. **중복 배정 방지**: 동일한 매니저가 같은 카테고리에 중복 배정되지 않도록 검증 필요
2. **활성 상태 관리**: 현재 담당 중인 카테고리만 조회 (필요시 active 플래그 추가 고려)
3. **이력 보존**: 매니저 삭제 시 기존 FAQ 이력의 매니저 이름은 보존됨

### 성능 최적화
1. **Lazy Loading**: ManagerCategory 목록은 지연 로딩으로 설정
2. **인덱스**: name 컬럼에 인덱스 추가하여 검색 성능 향상
3. **Batch Fetch**: N+1 문제 방지를 위해 `@BatchSize` 고려

### 확장 가능성
- 매니저 역할/권한 추가 (예: 수석 매니저, 일반 매니저)
- 매니저 활성/비활성 상태 관리
- 매니저별 처리 FAQ 통계
- 이메일 알림 기능 통합

---

**마지막 업데이트**: 2026-01-23
