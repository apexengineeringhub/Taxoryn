package com.taxoryn.module.gst;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GstModuleArchitectureBoundaryTest {

    private static final List<String> FORBIDDEN_IN_GST = List.of(
            "com.taxoryn.module.gov.entity",
            "com.taxoryn.module.gov.repository",
            "com.taxoryn.module.gov.security",
            "com.taxoryn.module.gov.infrastructure"
    );

    private static final List<String> FORBIDDEN_IN_GOV = List.of(
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository"
    );

    @Test
    @DisplayName("GST module must not import Government persistence entities, repositories, or secret infrastructure")
    void testGstModuleDoesNotImportGovPersistenceOrSecrets() throws Exception {
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
    @DisplayName("Government module must not import GST persistence entities or repositories")
    void testGovModuleDoesNotImportGstPersistence() throws Exception {
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
}
