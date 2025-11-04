package com.better.CommuteMate.domain.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_sub")
@IdClass(CodeSubId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSub {

    @Id
    @Column(name = "major_code", columnDefinition = "CHAR(2)", nullable = false)
    private String majorCode;

    @Id
    @Column(name = "sub_code", columnDefinition = "CHAR(2)", nullable = false)
    private String subCode;

    @Column(name = "sub_name", length = 100, nullable = false)
    private String subName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_code", insertable = false, updatable = false)
    private CodeMajor codeMajor;
}