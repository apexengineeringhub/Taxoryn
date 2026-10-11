package com.taxoryn.module.billing.service;

import com.taxoryn.module.billing.dto.TdsCreditDto;
import com.taxoryn.module.billing.dto.TdsReconciliationSummaryDto;

import java.util.List;
import java.util.UUID;

public interface TdsReconciliationService {

    TdsCreditDto recordTdsCredit(TdsCreditDto dto);

    List<TdsCreditDto> getTdsCredits(UUID clientId, String financialYear);

    TdsReconciliationSummaryDto runReconciliation(UUID clientId, String financialYear);

    TdsReconciliationSummaryDto getReconciliationSummary(UUID clientId, String financialYear);
}
