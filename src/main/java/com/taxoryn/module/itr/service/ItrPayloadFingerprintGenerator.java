package com.taxoryn.module.itr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.taxoryn.module.itr.dto.ItrTaxSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes deterministic SHA-256 payload fingerprints for ITR return submissions.
 */
@Slf4j
@Component
public class ItrPayloadFingerprintGenerator {

    private final ObjectMapper deterministicMapper;

    public ItrPayloadFingerprintGenerator() {
        this.deterministicMapper = new ObjectMapper();
        this.deterministicMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.deterministicMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public String generateFingerprint(
            String pan,
            String assessmentYear,
            String returnType,
            String taxpayerType,
            String residentialStatus,
            ItrTaxSummaryDto taxSummary,
            Map<String, Object> incomeDetails,
            Map<String, Object> deductions,
            Map<String, Object> schedules,
            Map<String, Object> bankDetails) {
        try {
            Map<String, Object> canonicalMap = new TreeMap<>();
            canonicalMap.put("pan", pan != null ? pan.trim().toUpperCase() : "");
            canonicalMap.put("assessmentYear", assessmentYear != null ? assessmentYear.trim() : "");
            canonicalMap.put("returnType", normalizeReturnType(returnType));
            canonicalMap.put("taxpayerType", taxpayerType != null ? taxpayerType.trim().toUpperCase() : "INDIVIDUAL");
            canonicalMap.put("residentialStatus", residentialStatus != null ? residentialStatus.trim().toUpperCase() : "RESIDENT");

            if (taxSummary != null) {
                Map<String, Object> totalsMap = new TreeMap<>();
                totalsMap.put("grossTotalIncome", formatDecimal(taxSummary.getGrossTotalIncome()));
                totalsMap.put("totalDeductions", formatDecimal(taxSummary.getTotalDeductions()));
                totalsMap.put("taxableIncome", formatDecimal(taxSummary.getTaxableIncome()));
                totalsMap.put("taxPayable", formatDecimal(taxSummary.getTaxPayable()));
                totalsMap.put("surcharge", formatDecimal(taxSummary.getSurcharge()));
                totalsMap.put("cess", formatDecimal(taxSummary.getCess()));
                totalsMap.put("totalTaxLiability", formatDecimal(taxSummary.getTotalTaxLiability()));
                totalsMap.put("tdsTcsCredit", formatDecimal(taxSummary.getTdsTcsCredit()));
                totalsMap.put("advanceTaxPaid", formatDecimal(taxSummary.getAdvanceTaxPaid()));
                totalsMap.put("selfAssessmentTaxPaid", formatDecimal(taxSummary.getSelfAssessmentTaxPaid()));
                totalsMap.put("totalTaxesPaid", formatDecimal(taxSummary.getTotalTaxesPaid()));
                totalsMap.put("refundDue", formatDecimal(taxSummary.getRefundDue()));
                totalsMap.put("balancePayable", formatDecimal(taxSummary.getBalancePayable()));
                canonicalMap.put("taxSummary", totalsMap);
            }

            if (incomeDetails != null && !incomeDetails.isEmpty()) {
                canonicalMap.put("incomeDetails", new TreeMap<>(incomeDetails));
            }
            if (deductions != null && !deductions.isEmpty()) {
                canonicalMap.put("deductions", new TreeMap<>(deductions));
            }
            if (schedules != null && !schedules.isEmpty()) {
                canonicalMap.put("schedules", new TreeMap<>(schedules));
            }
            if (bankDetails != null && !bankDetails.isEmpty()) {
                canonicalMap.put("bankDetails", new TreeMap<>(bankDetails));
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
            log.error("Failed to generate deterministic ITR payload fingerprint", e);
            return "ITR-FP-FALLBACK-" + System.currentTimeMillis();
        }
    }

    public static String normalizeReturnType(String returnType) {
        if (returnType == null) return "";
        String clean = returnType.trim().toUpperCase();
        if (clean.startsWith("ITR") && clean.length() == 4 && Character.isDigit(clean.charAt(3))) {
            return "ITR-" + clean.charAt(3);
        }
        return clean;
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
    }
}
