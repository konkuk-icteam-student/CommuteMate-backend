package com.better.CommuteMate.domain.faq.repository;

import com.better.CommuteMate.domain.faq.entity.FaqRelation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqRelationRepository extends JpaRepository<FaqRelation, Long> {

    @Modifying
    @Query("DELETE FROM FaqRelation fr WHERE fr.relatedFaq.id = :relatedFaqId")
    void deleteByRelatedFaqId(Long relatedFaqId);
}