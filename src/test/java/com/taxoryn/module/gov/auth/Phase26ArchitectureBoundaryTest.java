package com.taxoryn.module.gov.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class Phase26ArchitectureBoundaryTest {

    private static final List<String> FORBIDDEN_IN_GOV = List.of(
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository",
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository",
            "com.taxoryn.module.consent.entity",
            "com.taxoryn.module.consent.repository"
    );

    private static final List<String> FORBIDDEN_IN_GST = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository",
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository",
            "com.taxoryn.module.consent.entity",
            "com.taxoryn.module.consent.repository"
    );

    private static final List<String> FORBIDDEN_IN_ITR = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure",
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository",
            "com.taxoryn.module.consent.entity",
            "com.taxoryn.module.consent.repository"
    );

    private static final List<String> FORBIDDEN_IN_TDS = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure",
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository",
            "com.taxoryn.module.consent.entity",
            "com.taxoryn.module.consent.repository"
    );

    private static final List<String> FORBIDDEN_IN_CONSENT = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure",
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository",
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository"
    );

    @Test
    @DisplayName("Government module has zero compile-time coupling with compliance & consent domain entities/repos")
    void testGovModuleBoundaries() throws Exception {
        assertNoForbiddenDependencies(Paths.get("src/main/java/com/taxoryn/module/gov"), FORBIDDEN_IN_GOV);
    }

    @Test
    @DisplayName("GST module has zero compile-time coupling with other tax domains, gov persistence, or consent entities")
    void testGstModuleBoundaries() throws Exception {
        assertNoForbiddenDependencies(Paths.get("src/main/java/com/taxoryn/module/gst"), FORBIDDEN_IN_GST);
    }

    @Test
    @DisplayName("ITR module has zero compile-time coupling with other tax domains, gov persistence, or consent entities")
    void testItrModuleBoundaries() throws Exception {
        assertNoForbiddenDependencies(Paths.get("src/main/java/com/taxoryn/module/itr"), FORBIDDEN_IN_ITR);
    }

    @Test
    @DisplayName("TDS module has zero compile-time coupling with other tax domains, gov persistence, or consent entities")
    void testTdsModuleBoundaries() throws Exception {
        assertNoForbiddenDependencies(Paths.get("src/main/java/com/taxoryn/module/tds"), FORBIDDEN_IN_TDS);
    }

    @Test
    @DisplayName("Consent module has zero compile-time coupling with gov persistence or tax domain entities/repos")
    void testConsentModuleBoundaries() throws Exception {
        assertNoForbiddenDependencies(Paths.get("src/main/java/com/taxoryn/module/consent"), FORBIDDEN_IN_CONSENT);
    }

    private void assertNoForbiddenDependencies(Path moduleDir, List<String> forbiddenImports) throws Exception {
        if (!Files.exists(moduleDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(moduleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : forbiddenImports) {
                        assertThat(line)
                                .withFailMessage("Architecture boundary violation in %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }
}
