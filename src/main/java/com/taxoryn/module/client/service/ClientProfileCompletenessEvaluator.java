package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientProfileCompletenessDto;
import com.taxoryn.module.client.entity.ClientEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, explainable evaluator for Client Profile Completeness.
 * Assesses 5 core dimensions: IDENTITY, CONTACT, ADDRESS, STATUTORY, BUSINESS.
 */
@Component
public class ClientProfileCompletenessEvaluator {

    public ClientProfileCompletenessDto evaluate(ClientEntity client) {
        if (client == null) {
            return ClientProfileCompletenessDto.builder()
                    .complete(false)
                    .completionPercentage(0)
                    .completedSections(List.of())
                    .missingSections(List.of("IDENTITY", "CONTACT", "ADDRESS", "STATUTORY", "BUSINESS"))
                    .sectionStatus(Map.of(
                            "IDENTITY", false,
                            "CONTACT", false,
                            "ADDRESS", false,
                            "STATUTORY", false,
                            "BUSINESS", false
                    ))
                    .build();
        }

        Map<String, Boolean> status = new LinkedHashMap<>();
        List<String> completed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        // 1. IDENTITY: displayName is present and clientType is not null
        boolean identityOk = StringUtils.hasText(client.getDisplayName()) && client.getClientType() != null;
        status.put("IDENTITY", identityOk);
        if (identityOk) completed.add("IDENTITY"); else missing.add("IDENTITY");

        // 2. CONTACT: email or phone is present
        boolean contactOk = StringUtils.hasText(client.getEmail()) || StringUtils.hasText(client.getPhone());
        status.put("CONTACT", contactOk);
        if (contactOk) completed.add("CONTACT"); else missing.add("CONTACT");

        // 3. ADDRESS: city, state, and pincode are present
        boolean addressOk = StringUtils.hasText(client.getCity()) &&
                StringUtils.hasText(client.getState()) &&
                StringUtils.hasText(client.getPincode());
        status.put("ADDRESS", addressOk);
        if (addressOk) completed.add("ADDRESS"); else missing.add("ADDRESS");

        // 4. STATUTORY: PAN, GSTIN, or TAN is present
        boolean statutoryOk = StringUtils.hasText(client.getPan()) ||
                StringUtils.hasText(client.getGstin()) ||
                StringUtils.hasText(client.getTan());
        status.put("STATUTORY", statutoryOk);
        if (statutoryOk) completed.add("STATUTORY"); else missing.add("STATUTORY");

        // 5. BUSINESS: legalName, businessActivity, industry, tradeName, or dateOfIncorporation is present
        boolean businessOk = StringUtils.hasText(client.getLegalName()) ||
                StringUtils.hasText(client.getBusinessActivity()) ||
                StringUtils.hasText(client.getIndustry()) ||
                StringUtils.hasText(client.getTradeName()) ||
                client.getDateOfIncorporation() != null;
        status.put("BUSINESS", businessOk);
        if (businessOk) completed.add("BUSINESS"); else missing.add("BUSINESS");

        int completedCount = completed.size();
        int percentage = (completedCount * 100) / 5;
        boolean isComplete = completedCount == 5;

        return ClientProfileCompletenessDto.builder()
                .complete(isComplete)
                .completionPercentage(percentage)
                .completedSections(completed)
                .missingSections(missing)
                .sectionStatus(status)
                .build();
    }
}
