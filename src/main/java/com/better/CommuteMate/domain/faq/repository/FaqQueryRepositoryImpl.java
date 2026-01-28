package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.category.entity.QManagerCategory;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.QFaq;
import com.better.CommuteMate.domain.manager.entity.QManager;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class FaqQueryRepositoryImpl implements FaqQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Faq> searchFaqs( Long teamId, Long categoryId, String keyword, FaqSearchScope searchScope, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        QFaq faq = QFaq.faq;
        QManagerCategory mc = QManagerCategory.managerCategory;
        QManager manager = QManager.manager;

        BooleanBuilder where = new BooleanBuilder();

        if (categoryId != null) {
            where.and(faq.category.id.eq(categoryId));
        }

        if (teamId != null) {
            where.and(
                    JPAExpressions
                            .selectOne()
                            .from(mc)
                            .join(mc.manager, manager)
                            .where(
                                    mc.category.eq(faq.category),
                                    manager.team.id.eq(teamId)
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
                case TITLE -> where.and(faq.title.containsIgnoreCase(keyword));
                case CONTENT -> where.and(faq.content.containsIgnoreCase(keyword));
                case WRITER -> where.and(faq.writer.name.containsIgnoreCase(keyword));
            }
        }

        if (startDate != null) {
            where.and(faq.createdAt.goe(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            where.and(faq.createdAt.loe(endDate.atTime(23, 59, 59)));
        }

        List<Faq> contents = queryFactory
                .selectFrom(faq)
                .join(faq.category).fetchJoin()
                .where(where)
                .orderBy(faq.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(faq.count())
                .from(faq)
                .where(where)
                .fetchOne();

        return new PageImpl<>(contents, pageable, total == null ? 0 : total);
    }
}
