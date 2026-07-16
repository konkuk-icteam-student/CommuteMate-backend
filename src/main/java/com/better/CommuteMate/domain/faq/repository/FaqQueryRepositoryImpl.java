package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.category.entity.QCategory;
import com.better.CommuteMate.domain.category.entity.QManagerCategory;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.QFaq;
import com.better.CommuteMate.domain.faq.entity.QFaqCategory;
import com.better.CommuteMate.domain.manager.entity.QManager;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.better.CommuteMate.domain.faq.entity.FaqStatus;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class FaqQueryRepositoryImpl implements FaqQueryRepository {

    private static final double SIMILARITY_THRESHOLD = 0.2;
    private static final double KEYWORD_WEIGHT = 0.3;
    private static final double EMBEDDING_WEIGHT = 0.7;

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Faq> searchFaqs(
            Long organizationId,
            Long categoryId,
            String keyword,
            FaqSearchScope searchScope,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        QFaq faq = QFaq.faq;
        QFaqCategory fc = QFaqCategory.faqCategory;
        QCategory category = QCategory.category;
        QManagerCategory mc = QManagerCategory.managerCategory;
        QManager manager = QManager.manager;

        BooleanBuilder where = new BooleanBuilder();

        where.and(faq.deletedFlag.isFalse());
        where.and(faq.status.eq(FaqStatus.PUBLISHED));

        if (categoryId != null) {
            where.and(fc.category.id.eq(categoryId));
        }

        if (organizationId != null) {
            where.and(
                    JPAExpressions
                            .selectOne()
                            .from(mc)
                            .join(mc.manager, manager)
                            .where(
                                    mc.category.eq(fc.category),
                                    manager.organization.id.eq(organizationId)
                            )
                            .exists()
            );
        }

        if (keyword != null && !keyword.isBlank() && searchScope != null) {

            switch (searchScope) {

                case TITLE_CONTENT -> where.and(
                        faq.title.containsIgnoreCase(keyword)
                                .or(faq.content.containsIgnoreCase(keyword))
                );

                case TITLE ->
                        where.and(faq.title.containsIgnoreCase(keyword));

                case CONTENT ->
                        where.and(faq.content.containsIgnoreCase(keyword));

                case WRITER ->
                        where.and(faq.writer.name.containsIgnoreCase(keyword));
            }
        }

        if (startDate != null) {
            where.and(faq.updatedDate.goe(startDate));
        }

        if (endDate != null) {
            where.and(faq.updatedDate.loe(endDate));
        }

        List<Faq> contents = queryFactory
                .selectDistinct(faq)
                .from(faq)
                .join(faq.faqCategories, fc)
                .join(fc.category, category)
                .where(where)
                .orderBy(faq.updatedDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(faq.countDistinct())
                .from(faq)
                .join(faq.faqCategories, fc)
                .where(where)
                .fetchOne();

        return new PageImpl<>(
                contents,
                pageable,
                total == null ? 0 : total
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Faq> hybridSearch(
            Long organizationId,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            float[] embedding,
            int limit
    ) {
        String embeddingStr = Arrays.toString(embedding).replace(" ", "");

        StringBuilder sql = new StringBuilder("""
        SELECT * FROM (
            SELECT DISTINCT f.*,
                (
                    (CASE
                        WHEN f.title ILIKE CONCAT('%', :keyword, '%') THEN 2.0
                        WHEN f.content ILIKE CONCAT('%', :keyword, '%') THEN 1.0
                        ELSE 0.0
                    END) * :keywordWeight
                    +
                    (1 - (f.embedding <=> CAST(:embedding AS vector))) * :embeddingWeight
                ) AS score
            FROM faq f
            LEFT JOIN faq_category fc ON f.id = fc.faq_id
            WHERE f.deleted_flag = false
                AND f.status = 'PUBLISHED'
        """);

        if (categoryId != null) sql.append(" AND fc.category_id = :categoryId");
        if (organizationId != null) sql.append("""
         AND EXISTS (
            SELECT 1 FROM faq_category fc2
            INNER JOIN manager_category mc ON fc2.category_id = mc.category_id
            INNER JOIN manager mgr ON mc.manager_id = mgr.id
            WHERE fc2.faq_id = f.id AND mgr.organization_id = :organizationId
        )""");
        if (startDate != null) sql.append(" AND f.updated_date >= :startDate");
        if (endDate != null)   sql.append(" AND f.updated_date <= :endDate");

        sql.append("""
            AND (
                f.title ILIKE CONCAT('%', :keyword, '%')
                OR f.content ILIKE CONCAT('%', :keyword, '%')
                OR (f.embedding IS NOT NULL
                    AND (1 - (f.embedding <=> CAST(:embedding AS vector))) > :similarityThreshold)
            )
        ) ranked
        ORDER BY ranked.score DESC
        LIMIT :limit
    """);

        var query = entityManager.createNativeQuery(sql.toString(), Faq.class)
                .setParameter("keyword", keyword)
                .setParameter("embedding", embeddingStr)
                .setParameter("similarityThreshold", SIMILARITY_THRESHOLD)
                .setParameter("keywordWeight", KEYWORD_WEIGHT)
                .setParameter("embeddingWeight", EMBEDDING_WEIGHT)
                .setParameter("limit", limit);

        if (categoryId != null)     query.setParameter("categoryId", categoryId);
        if (organizationId != null) query.setParameter("organizationId", organizationId);
        if (startDate != null)      query.setParameter("startDate", startDate);
        if (endDate != null)        query.setParameter("endDate", endDate);

        return query.getResultList();
    }
}