# 업무 관리 시스템 (Task System)

## 📑 목차
- [개요](#개요)
- [테이블 구조](#테이블-구조)
- [필드 설명](#필드-설명)
- [관계](#관계)
- [CodeType 연동](#codetype-연동)
- [사용 예시](#사용-예시)
- [주의사항](#주의사항)
- [관련 문서](#관련-문서)

---

## 📖 개요

업무 관리 시스템은 일일 업무와 업무 템플릿을 관리합니다. 정기적으로 반복되는 업무는 템플릿으로 저장하여 재사용할 수 있으며, 개별 업무의 할당, 완료 추적을 지원합니다.

### 주요 특징
- **계층적 구조**: Task (일일 업무) ← TaskTemplate (템플릿) ← TaskTemplateItem (템플릿 항목)
- **템플릿 기반 일괄 생성**: 템플릿에서 일일 업무 자동 생성
- **완료 상태 추적**: 업무별 완료 여부 관리
- **업무 유형 분류**: TT 코드로 정기/비정기 업무 구분
- **할당 관리**: 담당자 할당 및 변경 지원

### 엔티티 위치
```
src/main/java/com/better/CommuteMate/domain/task/entity/
├── Task.java
├── TaskTemplate.java
├── TaskTemplateItem.java
└── repository/
    ├── TaskRepository.java
    ├── TaskTemplateRepository.java
    └── TaskTemplateItemRepository.java
```

---

## 🗂️ 테이블 구조

### 1. task 테이블

**목적**: 일일 업무 정보 저장

```sql
CREATE TABLE task (
    task_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '업무명',
    assignee_id INT NOT NULL COMMENT '담당자 ID',
    task_date DATE NOT NULL COMMENT '업무 일자',
    task_time TIME NOT NULL COMMENT '업무 시간',
    task_type CHAR(4) NOT NULL COMMENT 'TT01: 정기, TT02: 비정기',
    is_completed TINYINT(1) NOT NULL DEFAULT FALSE COMMENT '완료 여부',
    completed_by_name VARCHAR(50) COMMENT '실제 수행자 이름',
    completed_time TIME COMMENT '실제 수행 시간',
    created_at DATETIME NOT NULL,
    created_by INT,
    updated_at DATETIME NOT NULL,
    updated_by INT,

    PRIMARY KEY (task_id),
    FOREIGN KEY (assignee_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_task_date (task_date),
    INDEX idx_task_assignee (assignee_id)
);
```

### 2. task_template 테이블

**목적**: 업무 템플릿 정보 저장

```sql
CREATE TABLE task_template (
    template_id BIGINT NOT NULL AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL COMMENT '템플릿명',
    description VARCHAR(500) COMMENT '설명',
    is_active TINYINT(1) NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL,
    created_by INT,
    updated_at DATETIME NOT NULL,
    updated_by INT,

    PRIMARY KEY (template_id)
);
```

### 3. task_template_item 테이블

**목적**: 템플릿 내 업무 항목 정보 저장

```sql
CREATE TABLE task_template_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT '템플릿 ID',
    title VARCHAR(200) NOT NULL COMMENT '업무명',
    default_assignee_id INT COMMENT '기본 담당자 ID',
    task_time TIME NOT NULL COMMENT '업무 시간',
    task_type CHAR(4) NOT NULL COMMENT 'TT01: 정기, TT02: 비정기',
    display_order INT DEFAULT 0 COMMENT '표시 순서',

    PRIMARY KEY (item_id),
    FOREIGN KEY (template_id) REFERENCES task_template(template_id) ON DELETE CASCADE,
    FOREIGN KEY (default_assignee_id) REFERENCES user(user_id) ON DELETE SET NULL,
    INDEX idx_template (template_id)
);
```

---

## 📋 필드 설명

### task 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **task_id** | BIGINT | NO | AUTO_INCREMENT | 업무 ID (Primary Key) |
| **title** | VARCHAR(200) | NO | - | 업무명 (최대 200자) |
| **assignee_id** | INT | NO | - | 담당자 ID (Foreign Key → user.user_id) |
| **task_date** | DATE | NO | - | 업무 일자 (YYYY-MM-DD) |
| **task_time** | TIME | NO | - | 업무 시간 (HH:MM:SS) |
| **task_type** | CHAR(4) | NO | - | 업무 유형 코드 (TT01: 정기, TT02: 비정기) |
| **is_completed** | TINYINT(1) | NO | FALSE | 완료 여부 (FALSE: 미완료, TRUE: 완료) |
| **completed_by_name** | VARCHAR(50) | YES | NULL | 실제 수행자 이름 (완료 기록용) |
| **completed_time** | TIME | YES | NULL | 실제 수행 시간 (완료 기록용) |
| **created_at** | DATETIME | NO | - | 생성 시간 (@PrePersist에서 자동 설정) |
| **created_by** | INT | YES | NULL | 생성자 ID |
| **updated_at** | DATETIME | NO | - | 수정 시간 (@PreUpdate에서 자동 설정) |
| **updated_by** | INT | YES | NULL | 수정자 ID |

### task_template 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **template_id** | BIGINT | NO | AUTO_INCREMENT | 템플릿 ID (Primary Key) |
| **template_name** | VARCHAR(100) | NO | - | 템플릿명 (최대 100자) |
| **description** | VARCHAR(500) | YES | NULL | 설명 (최대 500자) |
| **is_active** | TINYINT(1) | NO | TRUE | 활성화 여부 (FALSE: 비활성, TRUE: 활성) |
| **created_at** | DATETIME | NO | - | 생성 시간 |
| **created_by** | INT | YES | NULL | 생성자 ID |
| **updated_at** | DATETIME | NO | - | 수정 시간 |
| **updated_by** | INT | YES | NULL | 수정자 ID |

### task_template_item 테이블

| 필드명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **item_id** | BIGINT | NO | AUTO_INCREMENT | 항목 ID (Primary Key) |
| **template_id** | BIGINT | NO | - | 템플릿 ID (Foreign Key → task_template.template_id) |
| **title** | VARCHAR(200) | NO | - | 업무명 (최대 200자) |
| **default_assignee_id** | INT | YES | NULL | 기본 담당자 ID (Foreign Key → user.user_id) |
| **task_time** | TIME | NO | - | 업무 시간 (HH:MM:SS) |
| **task_type** | CHAR(4) | NO | - | 업무 유형 코드 (TT01: 정기, TT02: 비정기) |
| **display_order** | INT | NO | 0 | 표시 순서 (정렬 기준) |

---

## 🔗 관계

### ERD (Entity Relationship Diagram)
```
┌───────────────────┐
│      User         │
├───────────────────┤
│ user_id (PK)      │
└────────┬──────────┘
         │
    ┌────┴───────────────────────────────┐
    │                                    │
    │ (1:N)                              │ (1:N)
    │                                    │
┌───▼──────────────────┐     ┌──────────▼──────────────┐
│ Task (1:N)           │     │ TaskTemplateItem (1:N)  │
├──────────────────────┤     ├───────────────────────┤
│ task_id (PK)         │     │ item_id (PK)          │
│ assignee_id (FK)     │     │ default_assignee (FK) │
│ task_date            │     │ task_time             │
│ task_type            │     │ task_type             │
│ is_completed         │     │ display_order         │
└──────────────────────┘     └─────────┬──────────────┘
                                       │
                                 (1:N) │
                                       │
                             ┌─────────▼─────────┐
                             │ TaskTemplate      │
                             ├───────────────────┤
                             │ template_id (PK)  │
                             │ template_name     │
                             │ is_active         │
                             └───────────────────┘
```

### 관계 상세

| 관계 | 설명 | 타입 | 비고 |
|------|------|------|------|
| User → Task | 사용자가 할당된 여러 업무 | 1:N | assignee_id FK, cascade delete |
| User → TaskTemplateItem | 기본 담당자로 지정된 항목 | 1:N | default_assignee_id FK, SET NULL |
| TaskTemplate → TaskTemplateItem | 템플릿 내 여러 항목 | 1:N | template_id FK, cascade delete |

---

## 🔢 CodeType 연동

### Task.taskType / TaskTemplateItem.taskType (업무 유형)

**CodeType Enum**: `TT` (업무 유형)

| 코드 | 값 | 한글명 | 영문명 | 설명 |
|------|-----|--------|--------|------|
| **TT01** | `REGULAR` | 정기 | Regular | 정기적으로 반복되는 업무 |
| **TT02** | `IRREGULAR` | 비정기 | Irregular | 일시적/특수 업무 |

### 사용 방식

```java
// Java에서 taskType 사용
task.setTaskType(CodeType.TT01);  // 정기 업무

// 업무 유형 조회
if (task.getTaskType().equals(CodeType.TT01)) {
    // 정기 업무 처리
}

// 전체 코드 값 조회
String fullCode = CodeType.TT01.getFullCode();  // "TT01"
CodeType code = CodeType.fromFullCode("TT01");  // CodeType.TT01
```

---

## 💻 사용 예시

### 1. 업무 템플릿 생성

```java
// 템플릿 생성
TaskTemplate template = TaskTemplate.create(
    "월요일 정기 업무",
    "매주 월요일에 수행할 정기 업무",
    adminId
);

taskTemplateRepository.save(template);
```

### 2. 템플릿 항목 추가

```java
// 템플릿 항목 1: 아침 회의
TaskTemplateItem item1 = TaskTemplateItem.create(
    "아침 회의",
    manager,  // 담당자
    LocalTime.of(09, 0),  // 09:00
    CodeType.TT01,  // 정기
    1  // 순서
);

// 템플릿 항목 2: 보고서 작성
TaskTemplateItem item2 = TaskTemplateItem.create(
    "보고서 작성",
    null,  // 담당자 미정
    LocalTime.of(14, 0),  // 14:00
    CodeType.TT01,  // 정기
    2  // 순서
);

template.addItem(item1);
template.addItem(item2);
taskTemplateRepository.save(template);
```

### 3. 템플릿에서 일일 업무 생성

```java
// 템플릿에서 특정 날짜의 업무 생성
LocalDate taskDate = LocalDate.of(2025, 12, 15);

List<Task> newTasks = new ArrayList<>();
for (TaskTemplateItem item : template.getItems()) {
    User assignee = item.getDefaultAssignee() != null
        ? item.getDefaultAssignee()
        : manager;  // 기본 담당자 미정 시 관리자

    Task task = Task.create(
        item.getTitle(),
        assignee,
        taskDate,
        item.getTaskTime(),
        item.getTaskType(),
        adminId
    );

    newTasks.add(task);
}

taskRepository.saveAll(newTasks);
```

### 4. 일일 업무 조회

```java
// 특정 날짜의 모든 업무
List<Task> dayTasks = taskRepository.findByTaskDate(LocalDate.now());

// 특정 담당자의 업무
List<Task> assigneeTasks = taskRepository.findByAssignee(user);

// 특정 날짜의 특정 담당자 업무
List<Task> userDayTasks = taskRepository
    .findByAssigneeAndTaskDate(user, LocalDate.now());

// 미완료 업무만
List<Task> incompleteTasks = taskRepository
    .findByIsCompletedFalse();
```

### 5. 업무 완료 처리

```java
// 방법 1: 단순 완료 토글
Task task = taskRepository.findById(taskId).orElse(null);

if (task != null) {
    task.toggleComplete(userId);  // 완료 상태 토글
    taskRepository.save(task);
}

// 방법 2: 명시적으로 완료 설정
task.setCompleted(true, userId);
taskRepository.save(task);

// 방법 3: 완료 기록 (실제 수행자, 수행 시간 포함)
task.completeRecord(
    "홍길동",  // 실제 수행자 이름
    LocalTime.of(15, 30),  // 실제 수행 시간
    userId  // 수정자 ID
);
taskRepository.save(task);
// → is_completed=true, completed_by_name="홍길동", completed_time="15:30:00"
```

### 6. 업무 수정

```java
// 업무 정보 수정
Task task = taskRepository.findById(taskId).orElse(null);

if (task != null) {
    task.update(
        "새로운 업무명",
        newAssignee,
        LocalTime.of(15, 0),
        userId  // 수정자
    );
    taskRepository.save(task);
}
```

### 7. 템플릿 관리

```java
// 템플릿 조회
TaskTemplate template = taskTemplateRepository.findById(templateId).orElse(null);

// 활성화된 템플릿만 조회
List<TaskTemplate> activeTemplates = taskTemplateRepository
    .findByIsActive(true);

// 템플릿 업데이트
template.update("새로운 템플릿명", "설명", adminId);
template.setActive(false, adminId);  // 비활성화
taskTemplateRepository.save(template);
```

---

## ⚠️ 주의사항

### 1. 템플릿 항목 순서
- `display_order`는 정렬 순서 지정 (기본값: 0)
- 템플릿 저장 시 자동으로 `displayOrder ASC, taskTime ASC` 순으로 정렬
- 순서 변경 후 반드시 저장

```java
// ✅ 올바른 예 - displayOrder 지정
item1.setDisplayOrder(1);
item2.setDisplayOrder(2);
template.replaceItems(Arrays.asList(item1, item2));
```

### 2. 담당자 할당
- Task는 **필수** 담당자 필요 (assignee_id NOT NULL)
- TaskTemplateItem은 **선택** 담당자 (default_assignee_id nullable)
- 템플릿 항목에 기본 담당자 미정 시 일일 업무 생성 시 관리자로 할당

```java
// ❌ 잘못된 예 - 담당자 없이 생성
Task task = Task.create(
    "업무명",
    null,  // 담당자 필수
    taskDate,
    taskTime,
    taskType,
    createdBy
);

// ✅ 올바른 예
Task task = Task.create(
    "업무명",
    assignee,  // 필수
    taskDate,
    taskTime,
    taskType,
    createdBy
);
```

### 3. 완료 상태 처리
- `is_completed` 기본값: **FALSE**
- 토글 메서드 또는 명시적 설정 메서드 사용
- **완료 기록** (`completeRecord`): 실제 수행자와 수행 시간을 함께 기록

```java
// 방법 1: 토글 사용
task.toggleComplete(userId);  // FALSE → TRUE, TRUE → FALSE

// 방법 2: 명시적 설정
task.setCompleted(true, userId);  // 명확히 TRUE로 설정

// 방법 3: 완료 기록 (권장)
task.completeRecord(
    "실제 수행자 이름",
    LocalTime.of(15, 30),  // 실제 수행 시간
    userId
);
// → is_completed, completed_by_name, completed_time 모두 설정
```

**완료 기록의 활용**:
- `assignee`(할당된 담당자)와 실제 수행자가 다를 경우 추적
- 업무 수행 시간 기록으로 통계 분석 가능
- 예: 담당자는 A이지만 실제 수행자는 B인 경우

### 4. 템플릿 활성화 상태
- `is_active` 기본값: **TRUE**
- 일괄 생성 시 활성화된 템플릿만 사용

```java
// ✅ 업무 생성 전에 활성화 상태 확인
if (template.getIsActive()) {
    // 일일 업무 생성
}
```

### 5. 캐스케이드 삭제
- TaskTemplate 삭제 시 모든 TaskTemplateItem 자동 삭제 (CASCADE)
- User 삭제 시 Task 자동 삭제 (CASCADE)
- default_assignee 삭제 시 NULL로 설정 (SET NULL)

```java
// TaskTemplate 삭제
taskTemplateRepository.deleteById(templateId);
// → 모든 TaskTemplateItem 자동 삭제

// User 삭제
userRepository.deleteById(userId);
// → 사용자 할당 Task 모두 삭제 (주의!)
```

### 6. 시간 필드 처리
- Task.taskTime: **TIME** 타입 (시:분:초)
- TaskTemplateItem.taskTime: **TIME** 타입 (시:분:초)
- 시간대(Timezone) 고려 필요

```java
// ✅ 올바른 예
LocalTime taskTime = LocalTime.of(09, 30, 0);  // 09:30:00
task.setTaskTime(taskTime);

// ❌ 피해야 할 예
LocalTime taskTime = LocalTime.now();  // 부정확한 시간
```

### 7. 데이터 일관성
- 동일 날짜에 동일 담당자의 중복 업무 방지 로직 필요
- 템플릿 항목의 taskTime은 고유할 것을 권장 (같은 시간 여러 업무 피하기)

```java
// ✅ 권장: 중복 업무 조회 후 검증
List<Task> existingTasks = taskRepository
    .findByAssigneeAndTaskDateAndTaskTime(user, taskDate, taskTime);

if (!existingTasks.isEmpty()) {
    throw new DuplicateTaskException("해당 시간에 이미 업무가 있습니다.");
}
```

---

## 🔗 관련 문서

- **docs/database/schema/user.md** - 사용자(User) 엔티티 상세
- **docs/database/schema/schedule.md** - 근무 일정 엔티티 상세
- **docs/database/schema/attendance.md** - 출근 기록 엔티티 상세
- **docs/api/endpoints-summary.md** - 업무 API 상세 로직
- **docs/conventions/error-handling.md** - 에러/예외 처리 규칙
- **docs/database/schema/code-system.md** - CodeType Enum 전체 시스템
- **CLAUDE.md** - 프로젝트 구조 및 기술 스택
