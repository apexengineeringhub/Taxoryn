package com.taxoryn.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class FlywayMigrationValidationTest {

    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    @Test
    @DisplayName("Verify all Flyway migration versions are unique and deterministic in classpath:db/migration")
    void testFlywayMigrationVersionsAreUnique() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");

        assertThat(resources).isNotEmpty();

        Set<Integer> versionNumbers = new HashSet<>();
        List<String> duplicateVersions = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            assertThat(filename).isNotNull();

            Matcher matcher = MIGRATION_FILE_PATTERN.matcher(filename);
            assertThat(matcher.matches())
                    .withFailMessage("Migration filename %s does not match expected Flyway naming convention V<version>__<description>.sql", filename)
                    .isTrue();

            int version = Integer.parseInt(matcher.group(1));
            if (!versionNumbers.add(version)) {
                duplicateVersions.add(filename + " (Version: " + version + ")");
            }
        }

        assertThat(duplicateVersions)
                .withFailMessage("Found duplicate Flyway migration versions: %s", duplicateVersions)
                .isEmpty();

        // Verify all versions from 1 to latest are present without unexpected duplicates
        int maxVersion = versionNumbers.stream().max(Integer::compareTo).orElse(0);
        assertThat(maxVersion).isEqualTo(64);
        assertThat(versionNumbers).hasSize(64);
    }

    @Test
    @DisplayName("Verify no stray Flyway migration files exist in classpath root")
    void testNoStrayMigrationFilesInClasspathRoot() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] rootResources = resolver.getResources("classpath:V*.sql");

        assertThat(rootResources)
                .withFailMessage("Stray Flyway migration files found in classpath root instead of classpath:db/migration")
                .isEmpty();
    }

    @Test
    @DisplayName("Verify Flyway migration discovery and metadata resolution")
    void testFlywayInfoAndDiscovery() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "")
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .load();

        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] allMigrations = infoService.all();

        assertThat(allMigrations).isNotEmpty();
        assertThat(allMigrations.length).isEqualTo(64);

        Set<String> discoveredVersions = new HashSet<>();
        for (MigrationInfo info : allMigrations) {
            String versionStr = info.getVersion().getVersion();
            assertThat(discoveredVersions.add(versionStr))
                    .withFailMessage("Flyway discovered duplicate migration version: %s", versionStr)
                    .isTrue();
        }

        MigrationInfo v63 = infoService.all()[62];
        assertThat(v63.getVersion().getVersion()).isEqualTo("63");
        assertThat(v63.getDescription()).isEqualTo("correct legacy document scan status");

        MigrationInfo v64 = infoService.all()[63];
        assertThat(v64.getVersion().getVersion()).isEqualTo("64");
        assertThat(v64.getDescription()).isEqualTo("add profile image support");
    }

    @Test
    @DisplayName("Verify V63 migration script contents enforce PENDING_SCAN default and LEGACY_UNSCANNED transition")
    void testV63MigrationScriptContents() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource v63Resource = resolver.getResource("classpath:db/migration/V63__correct_legacy_document_scan_status.sql");

        assertThat(v63Resource.exists()).isTrue();
        String sql = new String(v63Resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(sql).contains("ALTER TABLE documents");
        assertThat(sql).contains("ALTER COLUMN scan_status SET DEFAULT 'PENDING_SCAN'");
        assertThat(sql).contains("UPDATE documents");
        assertThat(sql).contains("SET scan_status = 'LEGACY_UNSCANNED'");
        assertThat(sql).contains("WHERE scan_status = 'CLEAN' AND (scanned_at IS NULL OR scanner_name IS NULL)");
    }
}
