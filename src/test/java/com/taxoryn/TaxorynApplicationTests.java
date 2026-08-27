package com.taxoryn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TaxorynApplication.class)
@ActiveProfiles("test")
class TaxorynApplicationTests {

    @Test
    @DisplayName("Context loads successfully in test profile")
    void contextLoads() {
    }
}
