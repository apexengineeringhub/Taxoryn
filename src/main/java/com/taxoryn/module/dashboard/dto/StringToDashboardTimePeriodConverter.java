package com.taxoryn.module.dashboard.dto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDashboardTimePeriodConverter implements Converter<String, DashboardTimePeriod> {

    @Override
    public DashboardTimePeriod convert(String source) {
        return DashboardTimePeriod.fromString(source);
    }
}
