package com.hufsphere.linkboard.domain.converter;

import com.hufsphere.linkboard.domain.SourceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SourceTypeConverter implements AttributeConverter<SourceType, String> {

    @Override
    public String convertToDatabaseColumn(SourceType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SourceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SourceType.fromValue(dbData);
    }
}
