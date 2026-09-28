package com.taxoryn.module.gmail.service;

import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.module.gmail.dto.GmailMetricsDto;

public interface GmailMetricsService {

    GmailMetricsDto getMetrics(PracticeSecurityScope scope);
}
