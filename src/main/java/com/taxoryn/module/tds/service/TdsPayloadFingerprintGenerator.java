package com.taxoryn.module.tds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.taxoryn.module.tds.dto.TdsReturnTotalsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes deterministic SHA-256 canonical payload fingerprints for TDS/TCS quarterly statement submissions.
 */
@Slf4j
@Component
public class TdsPayloadFingerprintGenerator {

    private final ObjectMapper deterministicMapper;

    public TdsPayloadFingerprintGenerator() {
        this.deterministicMapper = new ObjectMapper();
        this.deterministicMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.deterministicMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public String generateFingerprint(
            String tan,
            String formType,
            String financialYear,
            String quarter,
            String assessmentYear,
            String deductorType,
            TdsReturnTotalsDto totals,
            List<Map<String, Object>> challans,
            List<Map<String, Object>> deductees,
            Map<String, Object> metadata) {
        try {
            Map<String, Object> canonicalMap = new TreeMap<>();
            canonicalMap.put("tan", tan != null ? tan.trim().toUpperCase() : "");
            canonicalMap.put("formType", normalizeFormType(formType));
            canonicalMap.put("financialYear", financialYear != null ? financialYear.trim() : "");
            canonicalMap.put("quarter", quarter != null ? quarter.trim().toUpperCase() : "");
            canonicalMap.put("assessmentYear", assessmentYear != null ? assessmentYear.trim() : "");
            canonicalMap.put("deductorType", deductorType != null ? deductorType.trim().toUpperCase() : "COMPANY");

            if (totals != null) {
                Map<String, Object> totalsMap = new TreeMap<>();
                totalsMap.put("totalAmountPaid", formatDecimal(totals.getTotalAmountPaid()));
                totalsMap.put("totalTaxDeducted", formatDecimal(totals.getTotalTaxDeducted()));
                totalsMap.put("totalTaxDeposited", formatDecimal(totals.getTotalTaxDeposited()));
                totalsMap.put("totalInterest", formatDecimal(totals.getTotalInterest()));
                totalsMap.put("totalLateFee", formatDecimal(totals.getTotalLateFee()));
                totalsMap.put("totalPenalty", formatDecimal(totals.getTotalPenalty()));
                totalsMap.put("totalChallanAmount", formatDecimal(totals.getTotalChallanAmount()));
                totalsMap.put("totalDeducteeCount", totals.getTotalDeducteeCount() != null ? totals.getTotalDeducteeCount() : 0L);
                totalsMap.put("totalChallanCount", totals.getTotalChallanCount() != null ? totals.getTotalChallanCount() : 0L);
                canonicalMap.put("totals", totalsMap);
            }

            if (challans != null && !challans.isEmpty()) {
                List<Map<String, Object>> sortedChallans = new ArrayList<>();
                for (Map<String, Object> challan : challans) {
                    if (challan != null) {
                        sortedChallans.add(new TreeMap<>(challan));
                    }
                }
                canonicalMap.put("challans", sortedChallans);
            }

            if (deductees != null && !deductees.isEmpty()) {
                List<Map<String, Object>> sortedDeductees = new ArrayList<>();
                for (Map<String, Object> deductee : deductees) {
                    if (deductee != null) {
                        sortedDeductees.add(new TreeMap<>(deductee));
                    }
                }
                canonicalMap.put("deductees", sortedDeductees);
            }

            if (metadata != null && !metadata.isEmpty()) {
                canonicalMap.put("metadata", new TreeMap<>(metadata));
            }

            String json = deterministicMapper.writeValueAsString(canonicalMap);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Failed to generate deterministic TDS payload fingerprint", e);
            return "TDS-FP-FALLBACK-" + System.currentTimeMillis();
        }
    }

    public static String normalizeFormType(String formType) {
        if (formType == null) return "";
        String clean = formType.trim().toUpperCase().replace("-", "_");
        if (!clean.startsWith("FORM_") && (clean.endsWith("Q") || clean.endsWith("EQ") || clean.endsWith("QB") || clean.endsWith("QC") || clean.endsWith("QD") || clean.endsWith("QE"))) {
            return "FORM_" + clean;
        }
        return clean;
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
    }
}
