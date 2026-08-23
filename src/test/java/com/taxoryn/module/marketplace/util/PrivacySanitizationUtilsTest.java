package com.taxoryn.module.marketplace.util;

import com.taxoryn.module.marketplace.entity.CustomerTaxpayerType;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacySanitizationUtilsTest {

    @Test
    @DisplayName("Should redact Indian PAN pattern from customer inquiry text")
    void shouldRedactPanFromCustomerText() {
        String input = "My PAN is ABCDE1234F and I need help with ITR-2 filing.";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("ABCDE1234F");
        assertThat(sanitized).contains("[PROTECTED-PAN]");
        assertThat(sanitized).contains("and I need help with ITR-2 filing.");
    }

    @Test
    @DisplayName("Should redact Indian Aadhaar numbers from customer inquiry text")
    void shouldRedactAadhaarFromCustomerText() {
        String input = "Linked Aadhaar 5432 1098 7654 for verification purpose.";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("5432 1098 7654");
        assertThat(sanitized).contains("[PROTECTED-AADHAAR]");
    }

    @Test
    @DisplayName("Should redact GSTIN from customer inquiry text")
    void shouldRedactGstinFromCustomerText() {
        String input = "Our enterprise GSTIN is 27ABCDE1234F1Z5, need GSTR-9 annual filing.";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("27ABCDE1234F1Z5");
        assertThat(sanitized).contains("[PROTECTED-GSTIN]");
    }

    @Test
    @DisplayName("Should redact Bank Account numbers from customer text")
    void shouldRedactBankAccountNumbers() {
        String input = "Refund stuck in bank account 9876543210123 for assessment year 2024-25.";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("9876543210123");
        assertThat(sanitized).contains("[PROTECTED-BANK-A/C]");
    }

    @Test
    @DisplayName("Should redact explicit salary and income figures from customer text")
    void shouldRedactDirectSalaryAndIncomeDisclosures() {
        String input = "I have a salary: 32 lakh and capital gains: 14 lakh from mutual funds.";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("32 lakh");
        assertThat(sanitized).doesNotContain("14 lakh");
        assertThat(sanitized).contains("[FINANCIAL-DISCLOSURE-PROTECTED]");
    }

    @Test
    @DisplayName("Should strip HTML tags from customer input")
    void shouldStripHtmlTags() {
        String input = "<script>alert('xss')</script><b>Need ITR filing</b>";
        String sanitized = PrivacySanitizationUtils.sanitizeForEarlyEnquiry(input);

        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).doesNotContain("<b>");
        assertThat(sanitized).contains("Need ITR filing");
    }

    @Test
    @DisplayName("Should generate standard privacy-safe early enquiry summary")
    void shouldGenerateSafeDefaultEarlyEnquirySummary() {
        TaxServiceEntity service = TaxServiceEntity.builder()
                .code("INCOME_TAX_RETURN")
                .name("Income Tax Return (ITR) Filing")
                .build();

        String summary = PrivacySanitizationUtils.generateSafeEarlyEnquirySummary(
                service,
                CustomerTaxpayerType.SALARIED,
                "2025-26"
        );

        assertThat(summary).isEqualTo("Seeking professional assistance for Income Tax Return (ITR) Filing (FY 2025-26) as a Salaried Individual.");
    }

    @Test
    @DisplayName("Should mask email address correctly")
    void shouldMaskEmailCorrectly() {
        assertThat(PrivacySanitizationUtils.maskEmail("rahul.sharma@gmail.com")).isEqualTo("r***a@gmail.com");
        assertThat(PrivacySanitizationUtils.maskEmail("me@taxoryn.com")).isEqualTo("m***@taxoryn.com");
        assertThat(PrivacySanitizationUtils.maskEmail(null)).isEqualTo("c***r@customer.taxoryn");
    }

    @Test
    @DisplayName("Should mask phone number correctly")
    void shouldMaskPhoneCorrectly() {
        assertThat(PrivacySanitizationUtils.maskPhone("+919876543210")).isEqualTo("+91******3210");
        assertThat(PrivacySanitizationUtils.maskPhone("9876543210")).isEqualTo("+91******3210");
        assertThat(PrivacySanitizationUtils.maskPhone(null)).isEqualTo("******0000");
    }
}
