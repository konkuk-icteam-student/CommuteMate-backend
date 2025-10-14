package com.better.CommuteMate.domain.code.entity;

import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.code.CodeTypeConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code", indexes = {
    @Index(name = "uq_code_major_sub", columnList = "major_code, sub_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Code {

    @Id
    @Convert(converter = CodeTypeConverter.class)
    @Column(name = "full_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType fullCode;

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