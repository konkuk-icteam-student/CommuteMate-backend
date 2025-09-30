package com.better.CommuteMate.domain.code.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Code {

    @Id
    @Column(name = "code_id", columnDefinition = "CHAR(4)", nullable = false)
    private String codeId;

    @Column(name = "major_code", columnDefinition = "CHAR(2)", nullable = false)
    private String majorCode;

    @Column(name = "sub_code", columnDefinition = "CHAR(2)", nullable = false)
    private String subCode;

    @Column(name = "code_name", length = 100, nullable = false)
    private String codeName;

    @Column(name = "code_value", length = 100, nullable = false)
    private String codeValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "major_code", referencedColumnName = "major_code", insertable = false, updatable = false),
        @JoinColumn(name = "sub_code", referencedColumnName = "sub_code", insertable = false, updatable = false)
    })
    private CodeSub codeSub;
}