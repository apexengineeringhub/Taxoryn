package com.taxoryn.module.notification.whatsapp;

import com.taxoryn.module.notification.whatsapp.util.PhoneNumberNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "9876543210, +919876543210",
            "+919876543210, +919876543210",
            "919876543210, +919876543210",
            "09876543210, +919876543210",
            "+14155552671, +14155552671",
            "14155552671, +14155552671",
            "  98765 43210  , +919876543210",
            "(987) 654-3210, +919876543210",
            "+44 7911 123456, +447911123456"
    })
    @DisplayName("Normalizes various phone formats to standard E.164")
    void testNormalizeValidPhones(String input, String expected) {
        assertThat(PhoneNumberNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Handles null and blank phone gracefully")
    void testNormalizeNullOrBlank() {
        assertThat(PhoneNumberNormalizer.normalize(null)).isNull();
        assertThat(PhoneNumberNormalizer.normalize("")).isNull();
        assertThat(PhoneNumberNormalizer.normalize("   ")).isNull();
    }
}
