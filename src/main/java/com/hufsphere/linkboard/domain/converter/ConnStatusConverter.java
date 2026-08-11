package com.hufsphere.linkboard.domain.converter;

import com.hufsphere.linkboard.domain.ConnStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ConnStatusConverter implements AttributeConverter<ConnStatus, String> {

    @Override
    public String convertToDatabaseColumn(ConnStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ConnStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ConnStatus.fromValue(dbData);
    }
}
