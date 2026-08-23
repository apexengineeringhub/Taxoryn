package com.taxoryn.module.marketplace.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxServiceNormalizationUtilsTest {

    @ParameterizedTest(name = "normalize(\"{0}\") -> \"{1}\"")
    @CsvSource({
            "'  ITR  ', 'itr'",
            "'ITR Filing', 'itr filing'",
            "'I.T.R. - Filing', 'itr filing'",
            "'Income Tax Return (ITR)', 'income tax return itr'",
            "'GSTR-3B Filing', 'gstr 3b filing'",
            "'Tally / Book-keeping', 'tally book keeping'",
            "'MSME / Udyam Registration', 'msme udyam registration'",
            "'', ''",
            "'   ', ''"
    })
    @DisplayName("Should normalize diverse tax search queries and aliases accurately")
    void testNormalize(String input, String expected) {
        assertEquals(expected, TaxServiceNormalizationUtils.normalize(input));
    }

    @ParameterizedTest(name = "toCode(\"{0}\") -> \"{1}\"")
    @CsvSource({
            "'Income Tax Return', 'INCOME_TAX_RETURN'",
            "'GST Return Filing', 'GST_RETURN_FILING'",
            "'MSME / Udyam Registration', 'MSME_UDYAM_REGISTRATION'",
            "'Section 8 Company', 'SECTION_8_COMPANY'",
            "'TDS 26Q & 24Q', 'TDS_26Q_24Q'",
            "'', ''"
    })
    @DisplayName("Should generate standard uppercase snake_case service code")
    void testToCode(String input, String expected) {
        assertEquals(expected, TaxServiceNormalizationUtils.toCode(input));
    }
}
