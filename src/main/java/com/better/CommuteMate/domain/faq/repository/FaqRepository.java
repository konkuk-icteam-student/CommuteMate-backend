package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    // 제목에 특정 키워드가 포함된 FAQ 검색 (부분 일치)
    List<Faq> findByTitleContaining(String keyword);

    // 특정 대분류(category) 기준으로 FAQ 조회
    List<Faq> findByCategory(String category);

    // 특정 소분류(SubCategory) 기준으로 FAQ 조회
    List<Faq> findBySubCategoryEntity(SubCategory subCategory);

    // 작성자 이름 기준으로 FAQ 조회
    List<Faq> findByWriterName(String writerName);

    // 삭제되지 않은 FAQ만 조회
    List<Faq> findByDeletedFlagFalse();
}