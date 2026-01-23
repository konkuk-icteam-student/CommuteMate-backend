# FAQ 시스템 스키마

## 📑 목차
- [개요](#-개요)
- [엔티티](#-엔티티)
  - [Category](#category)
  - [ManagerCategory](#managercategory)
  - [Faq](#faq)
  - [FaqHistory](#faqhistory)
- [테이블 구조](#-테이블-구조)
- [관련 문서](#-관련-문서)

---

## 📖 개요

FAQ/카테고리/매니저 매핑을 위한 데이터 구조를 정리한 문서입니다.

---

## 🧩 엔티티

### Category
- **파일**: `domain/category/entity/Category.java`
- 주요 필드: `id`, `name`, `favorite`

### ManagerCategory
- **파일**: `domain/category/entity/ManagerCategory.java`
- 매니저(User)와 카테고리의 매핑 테이블
- 주요 필드: `id`, `user_id`, `category_id`, `assigned_at`, `active`

### Faq
- **파일**: `domain/faq/entity/Faq.java`
- 카테고리(FK)와 작성자/수정자(User FK)를 참조
- 주요 필드: `id`, `category_id`, `title`, `content`, `etc`, `attachment_url`, `writer_name`, `last_edited_at`, `last_editor_name`, `manager`, `created_at`, `deleted_flag`, `deleted_at`, `writer_id`, `last_editor_id`

### FaqHistory
- **파일**: `domain/faq/entity/FaqHistory.java`
- FAQ 수정 이력 저장
- 주요 필드: `id`, `faq_id`, `title`, `category`, `content`, `attachment_url`, `manager`, `writer_name`, `editor_name`, `edited_at`

---

## 🗂️ 테이블 구조

### category
```sql
CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    favorite BOOLEAN NOT NULL DEFAULT FALSE
);
```

### manager_category
```sql
CREATE TABLE manager_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    category_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);
```

### faq
```sql
CREATE TABLE faq (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    etc TEXT,
    attachment_url VARCHAR(150),
    writer_name VARCHAR(30) NOT NULL,
    last_edited_at DATETIME NOT NULL,
    last_editor_name VARCHAR(30) NOT NULL,
    manager VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME,
    writer_id INT NOT NULL,
    last_editor_id INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (writer_id) REFERENCES user(user_id),
    FOREIGN KEY (last_editor_id) REFERENCES user(user_id)
);
```

### faq_history
```sql
CREATE TABLE faq_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    attachment_url VARCHAR(150),
    manager VARCHAR(30) NOT NULL,
    writer_name VARCHAR(30) NOT NULL,
    editor_name VARCHAR(30) NOT NULL,
    edited_at DATETIME NOT NULL,
    faq_id BIGINT NOT NULL,
    FOREIGN KEY (faq_id) REFERENCES faq(id)
);
```

---

## 🔗 관련 문서

- [API 문서 - 카테고리](../../api/category.md)
- [API 문서 - FAQ](../../api/faq.md)
- [DB 문서 - User](./user.md)
