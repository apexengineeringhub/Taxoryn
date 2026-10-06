package com.taxoryn.module.tds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TdsModuleArchitectureBoundaryTest {

    private static final List<String> FORBIDDEN_IN_TDS = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure.secret",
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository"
    );

    private static final List<String> FORBIDDEN_IN_GOV = List.of(
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository"
    );

    private static final List<String> FORBIDDEN_IN_GST = List.of(
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository"
    );

    private static final List<String> FORBIDDEN_IN_ITR = List.of(
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository"
    );

    @Test
    @DisplayName("TDS module must not import Government persistence, Gov secrets, GST or ITR persistence")
    void testTdsModuleDoesNotImportGovGstOrItrPersistence() throws Exception {
        Path tdsModuleDir = Paths.get("src/main/java/com/taxoryn/module/tds");
        assertThat(Files.exists(tdsModuleDir)).isTrue();

        try (Stream<Path> stream = Files.walk(tdsModuleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : FORBIDDEN_IN_TDS) {
                        assertThat(line)
                                .withFailMessage("Boundary violation in TDS file %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Government module must not import TDS persistence entities or repositories")
    void testGovModuleDoesNotImportTdsPersistence() throws Exception {
        Path govModuleDir = Paths.get("src/main/java/com/taxoryn/module/gov");
        assertThat(Files.exists(govModuleDir)).isTrue();

        try (Stream<Path> stream = Files.walk(govModuleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : FORBIDDEN_IN_GOV) {
                        assertThat(line)
                                .withFailMessage("Boundary violation in GOV file %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("GST module must not import TDS persistence entities or repositories")
    void testGstModuleDoesNotImportTdsPersistence() throws Exception {
        Path gstModuleDir = Paths.get("src/main/java/com/taxoryn/module/gst");
        assertThat(Files.exists(gstModuleDir)).isTrue();

        try (Stream<Path> stream = Files.walk(gstModuleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : FORBIDDEN_IN_GST) {
                        assertThat(line)
                                .withFailMessage("Boundary violation in GST file %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("ITR module must not import TDS persistence entities or repositories")
    void testItrModuleDoesNotImportTdsPersistence() throws Exception {
        Path itrModuleDir = Paths.get("src/main/java/com/taxoryn/module/itr");
        assertThat(Files.exists(itrModuleDir)).isTrue();

        try (Stream<Path> stream = Files.walk(itrModuleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : FORBIDDEN_IN_ITR) {
                        assertThat(line)
                                .withFailMessage("Boundary violation in ITR file %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }
}
