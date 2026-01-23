# FAQ 시스템

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

FAQ 시스템은 **자주 묻는 질문과 답변을 카테고리별로 관리**하며, **모든 수정 이력을 추적**하는 시스템입니다.

**주요 용도**:
- FAQ 게시글 작성 및 관리
- 카테고리별 FAQ 분류
- 민원인 이름 및 답변 추적
- FAQ 수정 이력 자동 기록
- 소프트 삭제(deletedFlag) 기반 삭제 관리
- 카테고리별 담당 매니저 배정

---

## 🗄️ 테이블 구조

### 1. faq 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | FAQ ID |
| title | VARCHAR(100) | NOT NULL | FAQ 제목 |
| complainant_name | VARCHAR(30) | | 민원인 이름 |
| content | TEXT | NOT NULL | FAQ 내용 (질문) |
| answer | TEXT | NOT NULL | FAQ 답변 |
| etc | TEXT | | 비고 |
| writer_id | BIGINT | FK → user(id), NOT NULL | 작성자 (User 엔티티 참조) |
| last_edited_at | DATETIME | NOT NULL | 마지막 수정 일시 |
| category_id | BIGINT | FK → category(id), NOT NULL | 카테고리 |
| created_at | DATETIME | NOT NULL | 생성 일시 |
| deleted_flag | BOOLEAN | NOT NULL, DEFAULT FALSE | 삭제 여부 (소프트 삭제) |
| deleted_at | DATETIME | | 삭제 일시 |

**인덱스**:
- PRIMARY KEY: `id`
- FOREIGN KEY: `writer_id` → `user(id)`
- FOREIGN KEY: `category_id` → `category(id)`
- INDEX: `deleted_flag` (활성 FAQ 조회 최적화)
- INDEX: `category_id, deleted_flag` (카테고리별 활성 FAQ 조회)

**Java 코드**:
```java
@Entity
@Table(name = "faq")
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "complainant_name", length = 30)
    private String complainantName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String etc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;  // User 엔티티 참조

    @Column(name = "last_edited_at", nullable = false)
    private LocalDateTime lastEditedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

---

### 2. faq_history 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| title | VARCHAR(100) | NOT NULL | FAQ 제목 (스냅샷) |
| complainant_name | VARCHAR(30) | | 민원인 이름 (스냅샷) |
| content | TEXT | NOT NULL | FAQ 내용 (스냅샷) |
| answer | TEXT | NOT NULL | FAQ 답변 (스냅샷) |
| etc | TEXT | | 비고 (스냅샷) |
| writer_name | VARCHAR(30) | NOT NULL | 작성자 이름 (스냅샷) |
| edited_at | DATETIME | NOT NULL | 수정 일시 |
| category_name | VARCHAR(100) | NOT NULL | 카테고리명 (스냅샷) |
| faq_id | BIGINT | FK → faq(id), NOT NULL | 원본 FAQ |

**인덱스**:
- PRIMARY KEY: `id`
- FOREIGN KEY: `faq_id` → `faq(id)`
- INDEX: `faq_id, edited_at` (FAQ별 이력 조회 최적화)

**Java 코드**:
```java
@Entity
@Table(name = "faq_history")
public class FaqHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "complainant_name", length = 30)
    private String complainantName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String etc;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "faq_history_managers",
        joinColumns = @JoinColumn(name = "faq_history_id")
    )
    @Column(name = "manager_name", length = 30, nullable = false)
    private List<String> managerNames;  // 담당 매니저 이름 목록

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;
}
```

---

### 3. faq_history_managers 테이블 (ElementCollection)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| faq_history_id | BIGINT | FK → faq_history(id), NOT NULL | 이력 ID |
| manager_name | VARCHAR(30) | NOT NULL | 담당 매니저 이름 |

**설명**:
- FaqHistory의 `managerNames` 필드를 저장하는 별도 테이블
- `@ElementCollection` 어노테이션으로 자동 생성
- 한 이력에 여러 매니저 이름을 저장 가능

**인덱스**:
- FOREIGN KEY: `faq_history_id` → `faq_history(id)`

---

### 4. category 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 카테고리 ID |
| name | VARCHAR(100) | NOT NULL | 카테고리명 |
| favorite | BOOLEAN | NOT NULL, DEFAULT FALSE | 즐겨찾기 여부 |
| managers | List<ManagerCategory> | | 담당 매니저 목록 (OneToMany 관계) |

**인덱스**:
- PRIMARY KEY: `id`
- INDEX: `favorite` (즐겨찾기 조회 최적화)

**Java 코드**:
```java
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "favorite", nullable = false)
    @Builder.Default
    private boolean favorite = false;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ManagerCategory> managers = new ArrayList<>();
}
```

**managers 관계**:
- **타입**: OneToMany
- **매핑**: `mappedBy = "category"`
- **Cascade**: `CascadeType.ALL` (카테고리 삭제 시 모든 매니저 배정도 삭제)
- **OrphanRemoval**: `true` (매니저 배정 제거 시 자동 삭제)
- **용도**: 카테고리에 배정된 모든 매니저 조회
- **양방향 관계**: ManagerCategory.category ↔ Category.managers

---

### 5. manager_category 테이블

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 배정 ID |
| manager_id | BIGINT | FK → manager(id), NOT NULL | 매니저 |
| category_id | BIGINT | FK → category(id), NOT NULL | 카테고리 |
| assigned_at | DATETIME | NOT NULL | 배정 일시 |

**설명**:
- 매니저와 카테고리의 다대다 관계를 중간 테이블로 표현
- 한 매니저가 여러 카테고리를 담당 가능
- 한 카테고리에 여러 매니저 배정 가능

**인덱스**:
- PRIMARY KEY: `id`
- FOREIGN KEY: `manager_id` → `manager(id)`
- FOREIGN KEY: `category_id` → `category(id)`
- UNIQUE INDEX: `manager_id, category_id` (중복 배정 방지)

**Java 코드**:
```java
@Entity
@Table(name = "manager_category")
public class ManagerCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
```

---

## 🔗 관계

### 1. Faq → User (ManyToOne)
- **설명**: 한 FAQ는 한 명의 작성자(User)를 가짐
- **관계 타입**: N:1
- **외래키**: `writer_id → user(id)`
- **특징**: User 엔티티 참조 (NOT VARCHAR)

**Java 코드**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "writer_id", nullable = false)
private User writer;
```

---

### 2. Faq → Category (ManyToOne)
- **설명**: 한 FAQ는 한 개의 카테고리에 속함
- **관계 타입**: N:1
- **외래키**: `category_id → category(id)`

**Java 코드**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

---

### 3. FaqHistory → Faq (ManyToOne)
- **설명**: 한 이력은 한 개의 원본 FAQ를 참조
- **관계 타입**: N:1
- **외래키**: `faq_id → faq(id)`
- **특징**: Cascade 삭제 없음 (이력 보존)

**Java 코드**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "faq_id", nullable = false)
private Faq faq;
```

---

### 4. Category ↔ Manager (ManyToMany via ManagerCategory)
- **설명**: 카테고리와 매니저는 다대다 관계
- **중간 테이블**: `manager_category`
- **특징**: 배정 일시(`assigned_at`) 추적 가능

**Java 코드**:
```java
// Category.java
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ManagerCategory> managers = new ArrayList<>();

// Manager.java (예상)
@OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ManagerCategory> managerCategories = new ArrayList<>();
```

---

## 📊 ERD 표현

```
┌─────────────────────┐
│        User         │
│─────────────────────│
│ id (PK)             │
│ name                │
│ email               │
└─────────────────────┘
          │
          │ 1:N (writer)
          ▼
┌─────────────────────────┐         ┌─────────────────────┐
│         Faq             │   N:1   │      Category       │
│─────────────────────────│◄────────│─────────────────────│
│ id (PK)                 │         │ id (PK)             │
│ title                   │         │ name                │
│ complainant_name        │         │ favorite            │
│ content                 │         └─────────────────────┘
│ answer                  │                   │
│ etc                     │                   │ 1:N
│ writer_id (FK)          │                   ▼
│ last_edited_at          │         ┌─────────────────────┐
│ category_id (FK)        │         │  ManagerCategory    │
│ created_at              │         │─────────────────────│
│ deleted_flag            │         │ id (PK)             │
│ deleted_at              │         │ manager_id (FK)     │───┐
└─────────────────────────┘         │ category_id (FK)    │   │
          │                         │ assigned_at         │   │
          │ 1:N                     └─────────────────────┘   │
          ▼                                                   │
┌─────────────────────────┐                                   │ N:1
│     FaqHistory          │                                   ▼
│─────────────────────────│                         ┌─────────────────────┐
│ id (PK)                 │                         │      Manager        │
│ title                   │                         │─────────────────────│
│ complainant_name        │                         │ id (PK)             │
│ content                 │                         │ name                │
│ answer                  │                         │ team                │
│ etc                     │                         │ phonenum            │
│ writer_name             │                         │ created_at          │
│ edited_at               │                         └─────────────────────┘
│ category_name           │
│ faq_id (FK)             │
└─────────────────────────┘
          │
          │ 1:N (ElementCollection)
          ▼
┌─────────────────────────┐
│ faq_history_managers    │
│─────────────────────────│
│ faq_history_id (FK)     │
│ manager_name            │
└─────────────────────────┘
```

---

## 🎯 주요 기능

### 1. FAQ 생성 및 관리
- FAQ 작성 (제목, 내용, 답변, 민원인, 비고)
- 카테고리 지정
- 작성자(User) 연결
- 자동 타임스탬프 (created_at, last_edited_at)

### 2. 소프트 삭제
- `deletedFlag = true` 설정으로 논리적 삭제
- `deletedAt` 시간 기록
- 물리적 삭제 없이 이력 보존

### 3. 수정 이력 추적
- FAQ 수정 시 FaqHistory 자동 생성
- 모든 필드 스냅샷 저장
- 담당 매니저 목록 저장 (managerNames)
- 시간별 변경 이력 조회 가능

### 4. 카테고리 관리
- FAQ를 카테고리별로 분류
- 즐겨찾기 기능
- 카테고리별 담당 매니저 배정

### 5. 담당 매니저 배정
- 카테고리에 여러 매니저 배정 가능
- 배정 일시 자동 기록
- 이력에 매니저 이름 자동 기록

---

## 💡 사용 예시

### 1. FAQ 생성

```java
User writer = userRepository.findById(1L)
    .orElseThrow(() -> new UserNotFoundException());

Category category = categoryRepository.findById(1L)
    .orElseThrow(() -> new CategoryNotFoundException());

Faq faq = Faq.create(
    "출근 인증은 어떻게 하나요?",  // title
    "홍길동",  // complainantName
    "출근 인증 방법을 알고 싶습니다.",  // content
    "모바일 앱에서 QR코드를 스캔하여 인증합니다.",  // answer
    "참고: 앱 버전 2.0 이상 필요",  // etc
    writer,
    category
);

faqRepository.save(faq);
```

---

### 2. FAQ 수정 및 이력 생성

```java
// FAQ 조회
Faq faq = faqRepository.findById(1L)
    .orElseThrow(() -> new FaqNotFoundException());

// 수정 전 이력 저장
FaqHistory history = FaqHistory.create(faq);
faqHistoryRepository.save(history);

// FAQ 수정
faq.update(
    "출근 인증 방법 안내",  // 새로운 제목
    "홍길동",
    "출근 인증은 어떻게 하나요?",
    "모바일 앱 또는 웹에서 QR코드를 스캔하여 인증합니다.",  // 수정된 답변
    "참고: 웹 버전 추가됨",  // 수정된 비고
    category,
    writer
);

faqRepository.save(faq);
```

---

### 3. 소프트 삭제

```java
Faq faq = faqRepository.findById(1L)
    .orElseThrow(() -> new FaqNotFoundException());

// 소프트 삭제
faq.setDeletedFlag(true);
faq.setDeletedAt(LocalDateTime.now());

faqRepository.save(faq);
```

---

### 4. 활성 FAQ 조회 (deletedFlag = false)

```java
// 리포지토리 메서드 예시
List<Faq> activeFaqs = faqRepository.findByDeletedFlagFalse();

// 카테고리별 활성 FAQ 조회
List<Faq> categoryFaqs = faqRepository
    .findByCategoryIdAndDeletedFlagFalse(categoryId);
```

---

### 5. FAQ 이력 조회

```java
Faq faq = faqRepository.findById(1L)
    .orElseThrow(() -> new FaqNotFoundException());

// 해당 FAQ의 모든 수정 이력 조회 (최신순)
List<FaqHistory> histories = faqHistoryRepository
    .findByFaqOrderByEditedAtDesc(faq);

// 특정 시점의 스냅샷 조회
FaqHistory snapshot = histories.get(0);
System.out.println("제목: " + snapshot.getTitle());
System.out.println("작성자: " + snapshot.getWriterName());
System.out.println("담당 매니저: " + snapshot.getManagerNames());
System.out.println("수정 시간: " + snapshot.getEditedAt());
```

---

### 6. 카테고리에 매니저 배정

```java
Manager manager = managerRepository.findById(1L)
    .orElseThrow(() -> new ManagerNotFoundException());

Category category = categoryRepository.findById(1L)
    .orElseThrow(() -> new CategoryNotFoundException());

// 매니저 배정
ManagerCategory assignment = ManagerCategory.assign(manager, category);
managerCategoryRepository.save(assignment);
```

---

### 7. 카테고리의 담당 매니저 조회

```java
Category category = categoryRepository.findById(1L)
    .orElseThrow(() -> new CategoryNotFoundException());

// 담당 매니저 목록 조회
List<Manager> managers = category.getManagers().stream()
    .map(ManagerCategory::getManager)
    .collect(Collectors.toList());

// 매니저 이름 목록
List<String> managerNames = managers.stream()
    .map(Manager::getName)
    .collect(Collectors.toList());
```

---

### 8. FaqHistory 생성 로직 (팩토리 메서드)

```java
public static FaqHistory create(Faq faq) {
    return FaqHistory.builder()
        .faq(faq)
        .title(faq.getTitle())
        .complainantName(faq.getComplainantName())
        .content(faq.getContent())
        .answer(faq.getAnswer())
        .etc(faq.getEtc())
        .writerName(faq.getWriter().getName())
        .managerNames(
            faq.getCategory().getManagers().stream()
                .map(mc -> mc.getManager().getName())
                .toList()
        )
        .categoryName(faq.getCategory().getName())
        .build();
}
```

---

## 🔗 관련 문서

### 이 문서와 연관된 문서
- **필수**: [User 스키마](./user.md) - 작성자(writer) 관계
- **필수**: [Manager 스키마](./manager.md) - ManagerCategory 관계
- **참고**: [FAQ API](../../api/faq.md) - FAQ CRUD 엔드포인트
- **참고**: [Category API](../../api/category.md) - 카테고리 관리 엔드포인트

### 상위/하위 문서
- ⬆️ **상위**: [데이터베이스 스키마 홈](../README.md)
- ➡️ **관련**: [FAQ API](../../api/faq.md)

### 코드 위치
- **Faq Entity**: `src/main/java/com/better/CommuteMate/domain/faq/entity/Faq.java`
- **FaqHistory Entity**: `src/main/java/com/better/CommuteMate/domain/faq/entity/FaqHistory.java`
- **Category Entity**: `src/main/java/com/better/CommuteMate/domain/category/entity/Category.java`
- **ManagerCategory Entity**: `src/main/java/com/better/CommuteMate/domain/category/entity/ManagerCategory.java`
- **Faq Repository**: `src/main/java/com/better/CommuteMate/domain/faq/repository/FaqRepository.java`
- **FaqHistory Repository**: `src/main/java/com/better/CommuteMate/domain/faq/repository/FaqHistoryRepository.java`
- **Category Repository**: `src/main/java/com/better/CommuteMate/domain/category/repository/CategoryRepository.java`

---

## 📝 참고사항

### 비즈니스 규칙
1. **작성자 필수**: 모든 FAQ는 반드시 작성자(User)를 가져야 함
2. **카테고리 필수**: 모든 FAQ는 반드시 카테고리에 속해야 함
3. **이력 보존**: FAQ 삭제 시에도 이력(FaqHistory)은 보존됨
4. **소프트 삭제**: 물리적 삭제 대신 `deletedFlag = true` 사용
5. **자동 타임스탬프**: created_at, last_edited_at은 @PrePersist/@PreUpdate로 자동 설정

### 성능 최적화
1. **Lazy Loading**: User, Category, Faq 관계는 지연 로딩
2. **인덱스**: deleted_flag, category_id에 인덱스 추가
3. **Batch Fetch**: N+1 문제 방지를 위해 `@BatchSize` 고려
4. **ElementCollection 최적화**: managerNames는 별도 테이블로 분리

### 확장 가능성
- FAQ 조회수 추적 (view_count 필드)
- FAQ 좋아요/싫어요 기능
- FAQ 태그 시스템 (ManyToMany)
- 전문 검색(Full-Text Search) 기능
- 첨부파일 지원 (별도 테이블)

### 주의사항
1. **writer는 User 엔티티**: VARCHAR가 아닌 FK 관계
2. **FaqHistory의 managerNames**: List<String> ElementCollection
3. **ManagerCategory의 manager_id**: user_id가 아님
4. **SubCategory 미존재**: 현재 코드베이스에 SubCategory 엔티티는 없음
5. **이력 생성 시점**: FAQ 수정 전에 FaqHistory 생성 필요

---

**마지막 업데이트**: 2026-01-23
