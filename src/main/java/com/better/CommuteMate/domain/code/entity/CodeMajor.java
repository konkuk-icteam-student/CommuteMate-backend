package com.better.CommuteMate.domain.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_major")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeMajor {

    @Id
    @Column(name = "major_code", columnDefinition = "CHAR(2)", nullable = false)
    private String majorCode;

    @Column(name = "major_name", length = 100, nullable = false)
    private String majorName;
}