package com.better.CommuteMate.domain.code.repository;

import com.better.CommuteMate.domain.code.entity.CodeMajor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeMajorRepository extends JpaRepository<CodeMajor, String> {
}