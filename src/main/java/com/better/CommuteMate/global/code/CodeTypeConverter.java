package com.better.CommuteMate.global.code;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA에서 CodeType Enum을 DB의 CHAR(4) 컬럼과 매핑하기 위한 컨버터
 * Enum을 full_code(String)로 변환하여 저장하고, 조회 시 다시 Enum으로 변환
 */
@Converter(autoApply = true)
public class CodeTypeConverter implements AttributeConverter<CodeType, String> {

    /**
     * Enum → DB: CodeType을 full_code 문자열로 변환
     */
    @Override
    public String convertToDatabaseColumn(CodeType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getFullCode();
    }

    /**
     * DB → Enum: full_code 문자열을 CodeType으로 변환
     */
    @Override
    public CodeType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return CodeType.fromFullCode(dbData);
    }
}
