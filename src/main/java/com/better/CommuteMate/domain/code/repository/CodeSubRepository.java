package com.better.CommuteMate.domain.code.repository;

import com.better.CommuteMate.domain.code.entity.CodeSub;
import com.better.CommuteMate.domain.code.entity.CodeSubId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeSubRepository extends JpaRepository<CodeSub, CodeSubId> {
}