package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Public SEO Slug Generator for Taxoryn Practice Marketplace Profiles.
 * Generates URL-safe, unique, stable, and database-ID-agnostic public slugs.
 *
 * Example:
 *   "ABC Tax Consultants" -> "abc-tax-consultants"
 *   If duplicate: "abc-tax-consultants-2", "abc-tax-consultants-3", etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicSlugGenerator {

    private static final String DEFAULT_SLUG_PREFIX = "practice";
    private static final Pattern NON_ALPHANUMERIC_SEQUENCE = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final MarketplaceProfileRepository profileRepository;

    /**
     * Sanitizes any raw string into a clean, URL-safe lowercase slug format.
     * Safely handles empty/null inputs, special characters, unicode accents, and consecutive hyphens.
     *
     * @param raw the raw input string (e.g. "ABC Tax & Legal Consultants! #1")
     * @return clean URL-safe slug string (e.g. "abc-tax-legal-consultants-1")
     */
    public String sanitize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_SLUG_PREFIX;
        }

        // 1. Normalize unicode diacritics / accents (e.g. café -> cafe)
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // 2. Convert to lowercase
        String lower = normalized.toLowerCase(Locale.ROOT);

        // 3. Replace all non-alphanumeric sequences (spaces, underscores, punctuation, symbols) with a hyphen
        String hyphenated = NON_ALPHANUMERIC_SEQUENCE.matcher(lower).replaceAll("-");

        // 4. Strip leading and trailing hyphens
        String stripped = hyphenated.replaceAll("^-+|-+$", "");

        // 5. Fallback if stripped output is blank
        if (!StringUtils.hasText(stripped)) {
            return DEFAULT_SLUG_PREFIX;
        }

        return stripped;
    }

    /**
     * Generates a unique, collision-free public slug based on the raw name.
     * Starts duplicate numbering at -2 (e.g. "abc-tax-consultants", "abc-tax-consultants-2").
     *
     * @param rawName          the raw practice name or base text
     * @param excludeProfileId optional profile ID to exclude when checking collisions (for updates)
     * @return unique public URL slug
     */
    public String generateUniqueSlug(String rawName, UUID excludeProfileId) {
        String baseSlug = sanitize(rawName);

        if (!isSlugTaken(baseSlug, excludeProfileId)) {
            return baseSlug;
        }

        // Duplicate numbering starts at -2
        int count = 2;
        String uniqueSlug = baseSlug + "-" + count;
        while (isSlugTaken(uniqueSlug, excludeProfileId)) {
            count++;
            uniqueSlug = baseSlug + "-" + count;
        }

        log.debug("Generated unique slug '{}' from base '{}'", uniqueSlug, baseSlug);
        return uniqueSlug;
    }

    /**
     * Checks if a candidate slug is already taken by another practice profile.
     *
     * @param slug             the candidate slug to check
     * @param excludeProfileId optional profile ID to exclude from collision check
     * @return true if slug is already taken, false if available
     */
    public boolean isSlugTaken(String slug, UUID excludeProfileId) {
        if (!StringUtils.hasText(slug)) {
            return true;
        }
        if (excludeProfileId != null) {
            return profileRepository.existsBySlugAndIdNot(slug, excludeProfileId);
        }
        return profileRepository.existsBySlug(slug);
    }

    /**
     * Validates whether a candidate custom slug satisfies standard URL slug constraints.
     *
     * @param slug the slug candidate to validate
     * @return true if slug matches alphanumeric hyphen format
     */
    public boolean isValidSlugFormat(String slug) {
        return StringUtils.hasText(slug) && SLUG_FORMAT.matcher(slug).matches();
    }
}
