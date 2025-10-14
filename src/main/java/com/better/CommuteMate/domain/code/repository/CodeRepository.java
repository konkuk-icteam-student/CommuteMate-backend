package com.better.CommuteMate.domain.code.repository;

import com.better.CommuteMate.domain.code.entity.Code;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeRepository extends JpaRepository<Code, CodeType> {
}