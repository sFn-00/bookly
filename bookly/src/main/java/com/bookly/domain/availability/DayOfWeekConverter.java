package com.bookly.domain.availability;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

@Converter
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek,Integer> {
    @Override
    public Integer convertToDatabaseColumn(DayOfWeek attribute) {
        return attribute.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer dbData) {
        return DayOfWeek.of(dbData);
    }
}
