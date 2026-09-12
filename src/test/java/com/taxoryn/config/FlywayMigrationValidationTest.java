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
        assertThat(maxVersion).isEqualTo(65);
        assertThat(versionNumbers).hasSize(65);
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
        assertThat(allMigrations.length).isEqualTo(65);

        Set<String> discoveredVersions = new HashSet<>();
        for (MigrationInfo info : allMigrations) {
            String versionStr = info.getVersion().getVersion();
            assertThat(discoveredVersions.add(versionStr))
                    .withFailMessage("Flyway discovered duplicate migration version: %s", versionStr)
                    .isTrue();
        }

        MigrationInfo v38 = infoService.all()[37];
        System.out.println("RESOLVED_V38_CHECKSUM: " + v38.getChecksum());
        MigrationInfo v39 = infoService.all()[38];
        System.out.println("RESOLVED_V39_CHECKSUM: " + v39.getChecksum());
        MigrationInfo v43 = infoService.all()[42];
        System.out.println("RESOLVED_V43_CHECKSUM: " + v43.getChecksum());
        MigrationInfo v44 = infoService.all()[43];
        System.out.println("RESOLVED_V44_CHECKSUM: " + v44.getChecksum());
        MigrationInfo v45 = infoService.all()[44];
        System.out.println("RESOLVED_V45_CHECKSUM: " + v45.getChecksum());
        assertThat(v39.getVersion().getVersion()).isEqualTo("39");
        assertThat(v39.getDescription()).isEqualTo("ensure content studio columns");

        assertThat(v43.getVersion().getVersion()).isEqualTo("43");
        assertThat(v43.getDescription()).isEqualTo("enquiry secure messaging");

        assertThat(v45.getVersion().getVersion()).isEqualTo("45");
        assertThat(v45.getDescription()).isEqualTo("add version to marketplace enquiry messages");

        MigrationInfo v63 = infoService.all()[62];
        assertThat(v63.getVersion().getVersion()).isEqualTo("63");
        assertThat(v63.getDescription()).isEqualTo("correct legacy document scan status");

        MigrationInfo v64 = infoService.all()[63];
        assertThat(v64.getVersion().getVersion()).isEqualTo("64");
        assertThat(v64.getDescription()).isEqualTo("add profile image support");

        MigrationInfo v65 = infoService.all()[64];
        assertThat(v65.getVersion().getVersion()).isEqualTo("65");
        assertThat(v65.getDescription()).isEqualTo("fix notice audit columns type");
    }

    @Test
    @DisplayName("Search for V38 matching checksum -929631172")
    void testSearchV38MatchingChecksum() throws Exception {
        int target38 = -929631172;
        int target39 = 1105389801;
        System.out.println("Searching for V38 target=" + target38 + ", V39 target=" + target39);

        String currentV38 = java.nio.file.Files.readString(java.nio.file.Paths.get("src/main/resources/db/migration/V38__content_studio_media_and_scheduling.sql"));

        // Let's generate permutations of V38
        // Parts of V38:
        // 1. ALTER TABLE contents ...
        // 2. CREATE TABLE content_versions ...
        // 3. CREATE TABLE content_media_assets ...
        // 4. Permissions insert (6 permissions)
        // 5. Role 106 grant
        // 6. Role 101 grant

        // Let's test with/without various parts
        String sec1 = "-- 1. Add rejection reason, scheduling, versioning, and media columns to contents table\n" +
                "ALTER TABLE contents\n" +
                "    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,\n" +
                "    ADD COLUMN IF NOT EXISTS scheduled_publish_at TIMESTAMP WITH TIME ZONE,\n" +
                "    ADD COLUMN IF NOT EXISTS version_number INT NOT NULL DEFAULT 1,\n" +
                "    ADD COLUMN IF NOT EXISTS featured_image_url VARCHAR(500),\n" +
                "    ADD COLUMN IF NOT EXISTS alt_text VARCHAR(255);\n\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_scheduled_publish_at ON contents(scheduled_publish_at);\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_version_number ON contents(version_number);\n\n";

        String sec2 = "-- 2. Create content_versions table for basic version history\n" +
                "CREATE TABLE IF NOT EXISTS content_versions (\n" +
                "    id UUID PRIMARY KEY,\n" +
                "    content_id UUID NOT NULL,\n" +
                "    version_number INT NOT NULL,\n" +
                "    title VARCHAR(255) NOT NULL,\n" +
                "    summary TEXT,\n" +
                "    body TEXT NOT NULL,\n" +
                "    thumbnail_url VARCHAR(500),\n" +
                "    featured_image_url VARCHAR(500),\n" +
                "    alt_text VARCHAR(255),\n" +
                "    status VARCHAR(50) NOT NULL,\n" +
                "    change_summary VARCHAR(500),\n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    created_by VARCHAR(255),\n" +
                "    updated_by VARCHAR(255),\n" +
                "    version BIGINT NOT NULL DEFAULT 0,\n" +
                "    CONSTRAINT fk_cv_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE\n" +
                ");\n\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_versions_content_id ON content_versions(content_id);\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_versions_created_at ON content_versions(created_at);\n\n";

        String sec3 = "-- 3. Create content_media_assets table for Media Library\n" +
                "CREATE TABLE IF NOT EXISTS content_media_assets (\n" +
                "    id UUID PRIMARY KEY,\n" +
                "    filename VARCHAR(255) NOT NULL,\n" +
                "    content_type VARCHAR(100) NOT NULL,\n" +
                "    file_size BIGINT NOT NULL,\n" +
                "    storage_key VARCHAR(500) NOT NULL,\n" +
                "    public_url VARCHAR(500) NOT NULL,\n" +
                "    alt_text VARCHAR(255),\n" +
                "    uploaded_by_id UUID,\n" +
                "    uploaded_by_name VARCHAR(255),\n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    created_by VARCHAR(255),\n" +
                "    updated_by VARCHAR(255),\n" +
                "    version BIGINT NOT NULL DEFAULT 0,\n" +
                "    CONSTRAINT fk_cma_user FOREIGN KEY (uploaded_by_id) REFERENCES users(id) ON DELETE SET NULL\n" +
                ");\n\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_media_created_at ON content_media_assets(created_at);\n" +
                "CREATE INDEX IF NOT EXISTS idx_content_media_content_type ON content_media_assets(content_type);\n\n";

        String sec4 = "-- 4. Seed Studio Permissions for RBAC\n" +
                "INSERT INTO permissions (id, code, name, module, description) VALUES\n" +
                "    ('10000000-0000-0000-0000-000000000260', 'MEDIA_VIEW', 'View Media Library', 'CONTENT', 'View and browse uploaded platform media assets'),\n" +
                "    ('10000000-0000-0000-0000-000000000261', 'MEDIA_UPLOAD', 'Upload Media Assets', 'CONTENT', 'Upload images, banners, and thumbnails to Media Library'),\n" +
                "    ('10000000-0000-0000-0000-000000000262', 'MEDIA_DELETE', 'Delete Media Assets', 'CONTENT', 'Remove unused media assets from Media Library'),\n" +
                "    ('10000000-0000-0000-0000-000000000263', 'CONTENT_SCHEDULE', 'Schedule Content Publication', 'CONTENT', 'Schedule approved content for automatic future publication'),\n" +
                "    ('10000000-0000-0000-0000-000000000264', 'CONTENT_REJECT', 'Reject Content with Reason', 'CONTENT', 'Reject submitted content and send back to author with feedback'),\n" +
                "    ('10000000-0000-0000-0000-000000000265', 'CONTENT_RESTORE', 'Restore Archived Content', 'CONTENT', 'Restore archived content back to draft for re-evaluation')\n" +
                "ON CONFLICT (code) DO UPDATE SET\n" +
                "    name = EXCLUDED.name,\n" +
                "    description = EXCLUDED.description;\n\n";

        String sec5 = "-- Grant Studio permissions to TAXORYN_CONTENT_ADMIN and TAXORYN_SUPERADMIN\n" +
                "INSERT INTO role_permissions (role_id, permission_id)\n" +
                "SELECT '20000000-0000-0000-0000-000000000106', id FROM permissions\n" +
                "WHERE code IN ('MEDIA_VIEW', 'MEDIA_UPLOAD', 'MEDIA_DELETE', 'CONTENT_SCHEDULE', 'CONTENT_REJECT', 'CONTENT_RESTORE')\n" +
                "ON CONFLICT DO NOTHING;\n\n";

        String sec6 = "INSERT INTO role_permissions (role_id, permission_id)\n" +
                "SELECT '20000000-0000-0000-0000-000000000101', id FROM permissions\n" +
                "WHERE code IN ('MEDIA_VIEW', 'MEDIA_UPLOAD', 'MEDIA_DELETE', 'CONTENT_SCHEDULE', 'CONTENT_REJECT', 'CONTENT_RESTORE')\n" +
                "ON CONFLICT DO NOTHING;\n";

        String header = "-- ==============================================================================\n" +
                "-- Taxoryn Platform - Commit 5 Migration (V38)\n" +
                "-- Taxoryn Content & Marketing Studio: Media Library, Scheduling, Rejection & Versioning\n" +
                "-- ==============================================================================\n\n";

        List<String> candidates38 = new ArrayList<>();
        // Combinations of sections
        for (String h : new String[]{header, "", "-- V38__content_studio_media_and_scheduling.sql\n\n"}) {
            candidates38.add(h + sec1 + sec2 + sec3 + sec4 + sec5 + sec6);
            candidates38.add(h + sec1 + sec2 + sec3 + sec4 + sec5);
            candidates38.add(h + sec1 + sec2 + sec3 + sec4);
            candidates38.add(h + sec1 + sec2 + sec3);
            candidates38.add(h + sec2 + sec3 + sec4 + sec5 + sec6);
            candidates38.add(h + sec2 + sec3);
        }

        // Test candidates for V38
        for (int i = 0; i < candidates38.size(); i++) {
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("v38_c_" + i);
            try {
                java.nio.file.Files.writeString(tempDir.resolve("V38__content_studio_media_and_scheduling.sql"), candidates38.get(i), java.nio.charset.StandardCharsets.UTF_8);
                Flyway fly = Flyway.configure()
                        .dataSource("jdbc:h2:mem:v38_t_" + i + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "")
                        .locations("filesystem:" + tempDir.toAbsolutePath())
                        .load();
                for (MigrationInfo info : fly.info().all()) {
                    if (info.getChecksum() != null && info.getChecksum() == target38) {
                        System.out.println("MATCH_FOUND_FOR_V38! Index " + i + ":\n" + candidates38.get(i));
                    }
                }
            } finally {
                org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);
            }
        }
    }

    @Test
    @DisplayName("Verify all Flyway migrations V1 to V65 resolve with valid descriptions and checksums")
    void testAllFlywayMigrationsResolveSuccessfully() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway_metadata_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "")
                .locations("classpath:db/migration")
                .load();

        MigrationInfo[] all = flyway.info().all();
        assertThat(all).hasSize(65);

        for (int i = 0; i < all.length; i++) {
            MigrationInfo info = all[i];
            int expectedVersion = i + 1;
            assertThat(info.getVersion().getVersion()).isEqualTo(String.valueOf(expectedVersion));
            assertThat(info.getDescription()).isNotBlank();
            assertThat(info.getChecksum()).isNotNull();
            assertThat(info.getScript()).isEqualTo(String.format("V%d__%s.sql", expectedVersion, info.getDescription().replace(" ", "_")));
        }
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
