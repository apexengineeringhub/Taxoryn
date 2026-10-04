package com.taxoryn.module.gov;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GovModuleArchitectureBoundaryTest {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "com.taxoryn.module.gst.entity",
            "com.taxoryn.module.gst.repository",
            "com.taxoryn.module.itr.entity",
            "com.taxoryn.module.itr.repository",
            "com.taxoryn.module.tds.entity",
            "com.taxoryn.module.tds.repository",
            "com.taxoryn.module.compliance.entity"
    );

    @Test
    @DisplayName("Government module must have zero direct compile-time coupling with compliance domain entities")
    void testGovernmentModuleDoesNotImportDomainEntities() throws Exception {
        Path govModuleDir = Paths.get("src/main/java/com/taxoryn/module/gov");
        assertThat(Files.exists(govModuleDir))
                .withFailMessage("com.taxoryn.module.gov source directory not found at " + govModuleDir.toAbsolutePath())
                .isTrue();

        try (Stream<Path> stream = Files.walk(govModuleDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (String forbidden : FORBIDDEN_IMPORTS) {
                        assertThat(line)
                                .withFailMessage("Architectural boundary violation in file %s at line %d: Found forbidden dependency '%s'",
                                        javaFile.getFileName(), i + 1, forbidden)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }
}
