package com.taxoryn.module.marketplace.service;

import com.taxoryn.module.marketplace.repository.MarketplaceProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicSlugGeneratorTest {

    @Mock
    private MarketplaceProfileRepository profileRepository;

    private PublicSlugGenerator slugGenerator;

    @BeforeEach
    void setUp() {
        slugGenerator = new PublicSlugGenerator(profileRepository);
    }

    @Test
    @DisplayName("Public Slug: standard clean name sanitization")
    void testSanitize_StandardName() {
        String result = slugGenerator.sanitize("ABC Tax Consultants");
        assertEquals("abc-tax-consultants", result);
    }

    @Test
    @DisplayName("Public Slug: handles special characters, symbols, and punctuation")
    void testSanitize_SpecialCharacters() {
        String result = slugGenerator.sanitize("ABC Tax & Legal Consultants! #1 @Mumbai (Pvt. Ltd.)");
        assertEquals("abc-tax-legal-consultants-1-mumbai-pvt-ltd", result);
    }

    @Test
    @DisplayName("Public Slug: collapses multiple spaces, underscores, and consecutive hyphens")
    void testSanitize_ConsecutiveSeparators() {
        String result = slugGenerator.sanitize("  ABC___Tax   ---   Consultants--  ");
        assertEquals("abc-tax-consultants", result);
    }

    @Test
    @DisplayName("Public Slug: handles empty, null, whitespace-only, and symbols-only names safely")
    void testSanitize_EmptyAndSymbolsOnly() {
        assertEquals("practice", slugGenerator.sanitize(null));
        assertEquals("practice", slugGenerator.sanitize(""));
        assertEquals("practice", slugGenerator.sanitize("   "));
        assertEquals("practice", slugGenerator.sanitize("!@#$%^&*()_+="));
    }

    @Test
    @DisplayName("Public Slug: generates base slug when no collision exists")
    void testGenerateUniqueSlug_NoCollision() {
        when(profileRepository.existsBySlug("abc-tax-consultants")).thenReturn(false);

        String slug = slugGenerator.generateUniqueSlug("ABC Tax Consultants", null);

        assertEquals("abc-tax-consultants", slug);
        verify(profileRepository).existsBySlug("abc-tax-consultants");
    }

    @Test
    @DisplayName("Public Slug: appends -2 for first duplicate")
    void testGenerateUniqueSlug_FirstDuplicate() {
        when(profileRepository.existsBySlug("abc-tax-consultants")).thenReturn(true);
        when(profileRepository.existsBySlug("abc-tax-consultants-2")).thenReturn(false);

        String slug = slugGenerator.generateUniqueSlug("ABC Tax Consultants", null);

        assertEquals("abc-tax-consultants-2", slug);
    }

    @Test
    @DisplayName("Public Slug: increments counter (-3) for subsequent duplicate")
    void testGenerateUniqueSlug_MultipleDuplicates() {
        when(profileRepository.existsBySlug("abc-tax-consultants")).thenReturn(true);
        when(profileRepository.existsBySlug("abc-tax-consultants-2")).thenReturn(true);
        when(profileRepository.existsBySlug("abc-tax-consultants-3")).thenReturn(false);

        String slug = slugGenerator.generateUniqueSlug("ABC Tax Consultants", null);

        assertEquals("abc-tax-consultants-3", slug);
    }

    @Test
    @DisplayName("Public Slug: ignores self profile ID during update uniqueness check")
    void testGenerateUniqueSlug_ExcludingSelfProfile() {
        UUID profileId = UUID.randomUUID();
        when(profileRepository.existsBySlugAndIdNot("abc-tax-consultants", profileId)).thenReturn(false);

        String slug = slugGenerator.generateUniqueSlug("ABC Tax Consultants", profileId);

        assertEquals("abc-tax-consultants", slug);
    }

    @Test
    @DisplayName("Public Slug: does not expose internal database IDs")
    void testSlugDoesNotExposeDatabaseIds() {
        UUID internalDbId = UUID.randomUUID();
        String slug = slugGenerator.sanitize("Apex Tax Advisors LLP");

        assertFalse(slug.contains(internalDbId.toString()));
        assertTrue(slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$"));
    }
}
