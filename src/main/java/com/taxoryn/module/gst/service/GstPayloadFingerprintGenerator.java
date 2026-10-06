package com.taxoryn.module.gst.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.taxoryn.module.gst.dto.GstReturnTotalsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes deterministic SHA-256 payload fingerprints for GST return submissions.
 */
@Slf4j
@Component
public class GstPayloadFingerprintGenerator {

    private final ObjectMapper deterministicMapper;

    public GstPayloadFingerprintGenerator() {
        this.deterministicMapper = new ObjectMapper();
        this.deterministicMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.deterministicMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public String generateFingerprint(String gstin, String returnType, String returnPeriod, GstReturnTotalsDto totals, Map<String, Object> sections) {
        try {
            Map<String, Object> canonicalMap = new TreeMap<>();
            canonicalMap.put("gstin", gstin != null ? gstin.trim().toUpperCase() : "");
            canonicalMap.put("returnType", returnType != null ? returnType.trim().toUpperCase().replace("-", "") : "");
            canonicalMap.put("returnPeriod", returnPeriod != null ? returnPeriod.trim() : "");

            if (totals != null) {
                Map<String, Object> totalsMap = new TreeMap<>();
                totalsMap.put("taxableValue", totals.getTaxableValue() != null ? totals.getTaxableValue().toPlainString() : "0.00");
                totalsMap.put("igst", totals.getIgst() != null ? totals.getIgst().toPlainString() : "0.00");
                totalsMap.put("cgst", totals.getCgst() != null ? totals.getCgst().toPlainString() : "0.00");
                totalsMap.put("sgst", totals.getSgst() != null ? totals.getSgst().toPlainString() : "0.00");
                totalsMap.put("cess", totals.getCess() != null ? totals.getCess().toPlainString() : "0.00");
                totalsMap.put("totalTax", totals.getTotalTax() != null ? totals.getTotalTax().toPlainString() : "0.00");
                totalsMap.put("totalItc", totals.getTotalItc() != null ? totals.getTotalItc().toPlainString() : "0.00");
                canonicalMap.put("totals", totalsMap);
            }

            if (sections != null && !sections.isEmpty()) {
                canonicalMap.put("sections", new TreeMap<>(sections));
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
            log.error("Failed to generate deterministic payload fingerprint", e);
            return "FP-GEN-FALLBACK-" + System.currentTimeMillis();
        }
    }
}
