package com.taxoryn.core.bootstrap;

import com.taxoryn.module.marketplace.entity.*;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationMode;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.ConsultationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceConsultationEntity.PaymentStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerProfileStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceCustomerProfileEntity.CustomerType;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.LeadStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceLeadEntity.Urgency;
import com.taxoryn.module.marketplace.entity.MarketplaceProfileEntity.VerificationStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceProposalEntity.ProposalStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceReviewEntity.ReviewStatus;
import com.taxoryn.module.marketplace.entity.MarketplaceServiceEntity.PricingType;
import com.taxoryn.module.marketplace.repository.*;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.role.repository.RoleRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.entity.UserEntity.UserStatus;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Completes the Demo V1 marketplace journey on top of what {@link DemoDataSeeder}
 * already creates (organizations, staff, marketplace profiles, services, inbound leads).
 * <p>
 * Adds: the controlled Tax Service Master (categories + services), practice locations,
 * practice&rarr;service mappings, practice verifications, demo customer accounts,
 * customer tax requirements, an enquiry-to-consultation chain, and verified reviews.
 * <p>
 * Runs only for {@code dev} and {@code demo} profiles, and after {@link DemoDataSeeder}
 * (which seeds the organizations/marketplace profiles this class depends on).
 * Every write is idempotent (guarded by existence checks) so this is safe to run on
 * every application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "demo"})
@Order(2)
public class MarketplaceDemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final OrganizationRepository organizationRepository;
    private final MarketplaceProfileRepository marketplaceProfileRepository;
    private final PracticeLocationRepository practiceLocationRepository;
    private final PracticeServiceRepository practiceServiceRepository;
    private final MarketplaceVerificationRepository verificationRepository;
    private final TaxServiceCategoryRepository taxServiceCategoryRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final MarketplaceCustomerProfileRepository customerProfileRepository;
    private final CustomerTaxRequirementRepository requirementRepository;
    private final MarketplaceLeadRepository leadRepository;
    private final MarketplaceProposalRepository proposalRepository;
    private final MarketplaceConsultationRepository consultationRepository;
    private final MarketplaceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.core.env.Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> activeProfiles = java.util.Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("prod") || activeProfiles.contains("production")) {
            log.error("CRITICAL SECURITY GUARD: MarketplaceDemoDataSeeder execution blocked because production profile is active!");
            return;
        }

        try {
            seedControlledTaxServiceMaster();
        } catch (Exception ex) {
            log.error("Failed seeding controlled tax service master", ex);
        }

        // Fast-path for warm restarts: verifications and leads are the last artifacts
        // each of these two cascades produces. If both already exist, every ensure-check
        // inside them would be a no-op anyway, so skip re-walking them entirely.
        if (verificationRepository.count() > 0 && leadRepository.count() > 0) {
            log.info("Marketplace demo journey already seeded — skipping re-verification pass.");
            return;
        }

        try {
            seedPracticeLocationsServicesAndVerification();
        } catch (Exception ex) {
            log.error("Failed seeding practice locations/services/verification", ex);
        }

        try {
            seedFullCustomerJourney();
        } catch (Exception ex) {
            log.error("Failed seeding full customer journey demo data", ex);
        }
    }

    // =========================================================================
    // 1. Controlled Tax Service Master (categories + services) — see V25
    // =========================================================================
    private void seedControlledTaxServiceMaster() {
        TaxServiceCategoryEntity incomeTax = ensureCategory("INCOME_TAX", "Income Tax", "Individual and business income tax services", 1);
        TaxServiceCategoryEntity gst = ensureCategory("GST", "GST", "Goods & Services Tax registration, filing and advisory", 2);
        TaxServiceCategoryEntity tds = ensureCategory("TDS", "TDS", "Tax Deducted at Source compliance and filing", 3);

        ensureService(incomeTax, "ITR_FILING", "Income Tax Return Filing", "Preparation and e-filing of annual income tax return", 1);
        ensureService(incomeTax, "ITR_CORRECTION", "Income Tax Return Correction", "Revision or correction of a previously filed return", 2);
        ensureService(incomeTax, "ITR_NOTICE_ASSISTANCE", "Income Tax Notice Assistance", "Guidance and representation for income tax notices", 3);
        ensureService(incomeTax, "ITR_REFUND_ASSISTANCE", "Income Tax Refund Assistance", "Tracking and resolving delayed or stuck refunds", 4);
        ensureService(incomeTax, "ITR_PLANNING", "Income Tax Planning", "Forward-looking tax planning and advisory", 5);

        ensureService(gst, "GST_REGISTRATION", "GST Registration", "New GST registration for businesses and professionals", 1);
        ensureService(gst, "GST_RETURN_FILING", "GST Return Filing", "Monthly/quarterly GSTR return preparation and filing", 2);
        ensureService(gst, "GST_ADVISORY", "GST Advisory", "GST structuring, ITC optimization and compliance advisory", 3);

        ensureService(tds, "TDS_RETURN_FILING", "TDS Return Filing", "Quarterly TDS return preparation and filing", 1);
        ensureService(tds, "TDS_COMPLIANCE", "TDS Compliance", "TDS deduction, deposit and certificate compliance advisory", 2);

        log.info("Controlled tax service master seed check complete.");
    }

    private TaxServiceCategoryEntity ensureCategory(String code, String name, String description, int sortOrder) {
        return taxServiceCategoryRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> {
                    TaxServiceCategoryEntity saved = taxServiceCategoryRepository.save(
                            TaxServiceCategoryEntity.builder()
                                    .code(code)
                                    .name(name)
                                    .description(description)
                                    .sortOrder(sortOrder)
                                    .isActive(true)
                                    .build());
                    log.info("Seeded tax service category: {}", code);
                    return saved;
                });
    }

    private void ensureService(TaxServiceCategoryEntity category, String code, String name, String description, int sortOrder) {
        if (taxServiceRepository.existsByCodeIgnoreCase(code)) {
            return;
        }
        taxServiceRepository.save(
                TaxServiceEntity.builder()
                        .categoryId(category.getId())
                        .code(code)
                        .name(name)
                        .description(description)
                        .sortOrder(sortOrder)
                        .isActive(true)
                        .build());
        log.info("Seeded tax service: {}", code);
    }

    // =========================================================================
    // 2. Practice Locations, Practice<->Service mapping, Verification
    // =========================================================================
    private void seedPracticeLocationsServicesAndVerification() {
        for (OrganizationEntity org : organizationRepository.findAll()) {
            marketplaceProfileRepository.findByOrganizationId(org.getId()).ifPresent(profile -> {
                seedPracticeLocation(org, profile);
                seedPracticeServices(org, profile);
                seedVerification(org, profile);
            });
        }
    }

    private void seedPracticeLocation(OrganizationEntity org, MarketplaceProfileEntity profile) {
        if (practiceLocationRepository.countByMarketplaceProfileId(profile.getId()) > 0) {
            return;
        }
        boolean isMundeshwari = profile.getDisplayName().toUpperCase().contains("MUNDESHWARI");

        practiceLocationRepository.save(
                PracticeLocationEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(profile.getId())
                        .locationName(isMundeshwari ? "Patna Head Office" : "Bangalore Head Office")
                        .addressLine1(profile.getAddress())
                        .city(profile.getCity())
                        .state(profile.getState())
                        .stateCode(isMundeshwari ? "BR" : "KA")
                        .pincode(profile.getPincode())
                        .latitude(isMundeshwari ? new BigDecimal("25.594100") : new BigDecimal("12.971600"))
                        .longitude(isMundeshwari ? new BigDecimal("85.137600") : new BigDecimal("77.594600"))
                        .isPrimary(true)
                        .isActive(true)
                        .build());
        log.info("Seeded primary practice location for profile: {}", profile.getSlug());
    }

    private void seedPracticeServices(OrganizationEntity org, MarketplaceProfileEntity profile) {
        if (!practiceServiceRepository.findByMarketplaceProfileId(profile.getId()).isEmpty()) {
            return;
        }
        boolean isMundeshwari = profile.getDisplayName().toUpperCase().contains("MUNDESHWARI");
        List<String> codes = isMundeshwari
                ? List.of("ITR_NOTICE_ASSISTANCE", "ITR_CORRECTION", "GST_ADVISORY", "TDS_COMPLIANCE")
                : List.of("ITR_FILING", "ITR_PLANNING", "GST_REGISTRATION", "GST_RETURN_FILING", "TDS_RETURN_FILING");

        List<TaxServiceEntity> services = taxServiceRepository.findByCodeInIgnoreCase(codes);
        for (TaxServiceEntity service : services) {
            practiceServiceRepository.save(
                    PracticeServiceEntity.builder()
                            .organizationId(org.getId())
                            .marketplaceProfileId(profile.getId())
                            .taxServiceId(service.getId())
                            .isActive(true)
                            .build());
        }
        log.info("Seeded {} controlled service links for profile: {}", services.size(), profile.getSlug());
    }

    private void seedVerification(OrganizationEntity org, MarketplaceProfileEntity profile) {
        if (!verificationRepository.findByOrganizationId(org.getId()).isEmpty()) {
            return;
        }
        boolean isMundeshwari = profile.getDisplayName().toUpperCase().contains("MUNDESHWARI");

        verificationRepository.save(
                MarketplaceVerificationEntity.builder()
                        .organizationId(org.getId())
                        .marketplaceProfileId(profile.getId())
                        .professionalBody(isMundeshwari ? "Bar Council of India" : "Institute of Chartered Accountants of India")
                        .membershipNumber(isMundeshwari ? "BCI-DEMO-88213" : "ICAI-DEMO-114402")
                        .firmRegistrationNumber(isMundeshwari ? null : "FRN-123456N")
                        .verificationStatus(VerificationStatus.VERIFIED)
                        .verifiedAt(Instant.now())
                        .verifiedBy("demo-seed")
                        .build());
        log.info("Seeded verification record for profile: {}", profile.getSlug());
    }

    // =========================================================================
    // 3. Full Customer Journey — two demo customers at different pipeline stages
    // =========================================================================
    private void seedFullCustomerJourney() {
        Optional<MarketplaceProfileEntity> apex = marketplaceProfileRepository.findAll().stream()
                .filter(p -> !p.getDisplayName().toUpperCase().contains("MUNDESHWARI"))
                .findFirst();
        Optional<MarketplaceProfileEntity> mundeshwari = marketplaceProfileRepository.findAll().stream()
                .filter(p -> p.getDisplayName().toUpperCase().contains("MUNDESHWARI"))
                .findFirst();

        apex.ifPresent(profile -> seedCompletedJourney(
                profile,
                "priya.kulkarni.demo@example.com", "Priya", "Kulkarni",
                "+919845011223", "Bangalore", "Karnataka", "560001",
                CustomerType.INDIVIDUAL, CustomerTaxpayerType.SALARIED,
                "ITR_FILING",
                "Need help filing my income tax return for FY 2025-26. I have salary income and some mutual fund capital gains."));

        mundeshwari.ifPresent(profile -> seedInProgressJourney(
                profile,
                "rohit.verma.demo@example.com", "Rohit", "Verma",
                "+919812349988", "Patna", "Bihar", "800001",
                CustomerType.BUSINESS, CustomerTaxpayerType.BUSINESS_OWNER,
                "GST_REGISTRATION",
                "Starting a new trading business and need GST registration done along with initial compliance guidance."));
    }

    /** Customer A: full journey through to a completed consultation + verified review. */
    private void seedCompletedJourney(MarketplaceProfileEntity profile, String email, String firstName, String lastName,
                                       String phone, String city, String state, String pincode,
                                       CustomerType customerType, CustomerTaxpayerType taxpayerType,
                                       String taxServiceCode, String requirementDescription) {
        MarketplaceCustomerProfileEntity customer = ensureCustomer(email, firstName, lastName, phone, city, state, pincode, customerType);
        if (customer == null || requirementRepository.countByCustomerId(customer.getId()) > 0) {
            return;
        }

        TaxServiceEntity taxService = taxServiceRepository.findByCodeIgnoreCase(taxServiceCode).orElse(null);
        if (taxService == null) {
            return;
        }

        CustomerTaxRequirementEntity requirement = requirementRepository.save(
                CustomerTaxRequirementEntity.builder()
                        .customerId(customer.getId())
                        .taxServiceId(taxService.getId())
                        .status(TaxRequirementStatus.SUBMITTED)
                        .customerType(taxpayerType)
                        .financialYear("2025-26")
                        .description(requirementDescription)
                        .city(city)
                        .state(state)
                        .pincode(pincode)
                        .searchRadiusKm(15)
                        .build());

        MarketplaceLeadEntity lead = leadRepository.save(
                MarketplaceLeadEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .taxRequirementId(requirement.getId())
                        .taxServiceId(taxService.getId())
                        .financialYear("2025-26")
                        .customerType(taxpayerType)
                        .earlyEnquiryMessage(requirementDescription)
                        .isContactMasked(false)
                        .clientName(customer.getDisplayName())
                        .clientEmail(customer.getEmail())
                        .clientPhone(customer.getPhone())
                        .serviceCategory(taxService.getName())
                        .requirementDescription(requirementDescription)
                        .urgency(Urgency.STANDARD)
                        .leadStatus(LeadStatus.CONVERTED)
                        .build());

        MarketplaceProposalEntity proposal = proposalRepository.save(
                MarketplaceProposalEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .leadId(lead.getId())
                        .proposalTitle("Income Tax Return Filing — FY 2025-26")
                        .scopeOfWork("Preparation and e-filing of ITR-1 covering salary income and capital gains from mutual funds, including Form 16 reconciliation.")
                        .deliverables("Computation Sheet, ITR-V Acknowledgement, Filing Confirmation")
                        .feeAmount(new BigDecimal("2499.00"))
                        .pricingType(PricingType.FIXED)
                        .estimatedTimelineDays(5)
                        .proposalStatus(ProposalStatus.ACCEPTED)
                        .accessToken(UUID.randomUUID().toString().replace("-", ""))
                        .validUntil(LocalDate.now().plusDays(14))
                        .acceptedAt(Instant.now())
                        .build());

        consultationRepository.save(
                MarketplaceConsultationEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .leadId(lead.getId())
                        .clientName(customer.getDisplayName())
                        .clientEmail(customer.getEmail())
                        .clientPhone(customer.getPhone())
                        .topic("ITR Filing Kickoff Call")
                        .consultationMode(ConsultationMode.VIDEO)
                        .bookingDate(LocalDate.now().minusDays(2))
                        .startTime("11:00")
                        .endTime("11:30")
                        .feeAmount(BigDecimal.ZERO)
                        .paymentStatus(PaymentStatus.WAIVED)
                        .consultationStatus(ConsultationStatus.COMPLETED)
                        .notes("Documents collected. Filing in progress.")
                        .build());

        reviewRepository.save(
                MarketplaceReviewEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .reviewerName(customer.getDisplayName())
                        .rating(5)
                        .reviewTitle("Quick and hassle-free filing")
                        .reviewComment("Very smooth process, explained everything clearly and filed my return well before the deadline.")
                        .serviceTaken(taxService.getName())
                        .isVerifiedClient(true)
                        .status(ReviewStatus.APPROVED)
                        .build());

        log.info("Seeded completed customer journey for {} -> {}", customer.getEmail(), profile.getSlug());
    }

    /** Customer B: earlier in the pipeline — submitted requirement + a pending proposal, no consultation yet. */
    private void seedInProgressJourney(MarketplaceProfileEntity profile, String email, String firstName, String lastName,
                                        String phone, String city, String state, String pincode,
                                        CustomerType customerType, CustomerTaxpayerType taxpayerType,
                                        String taxServiceCode, String requirementDescription) {
        MarketplaceCustomerProfileEntity customer = ensureCustomer(email, firstName, lastName, phone, city, state, pincode, customerType);
        if (customer == null || requirementRepository.countByCustomerId(customer.getId()) > 0) {
            return;
        }

        TaxServiceEntity taxService = taxServiceRepository.findByCodeIgnoreCase(taxServiceCode).orElse(null);
        if (taxService == null) {
            return;
        }

        CustomerTaxRequirementEntity requirement = requirementRepository.save(
                CustomerTaxRequirementEntity.builder()
                        .customerId(customer.getId())
                        .taxServiceId(taxService.getId())
                        .status(TaxRequirementStatus.SUBMITTED)
                        .customerType(taxpayerType)
                        .financialYear("2025-26")
                        .description(requirementDescription)
                        .city(city)
                        .state(state)
                        .pincode(pincode)
                        .searchRadiusKm(20)
                        .build());

        MarketplaceLeadEntity lead = leadRepository.save(
                MarketplaceLeadEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .taxRequirementId(requirement.getId())
                        .taxServiceId(taxService.getId())
                        .financialYear("2025-26")
                        .customerType(taxpayerType)
                        .earlyEnquiryMessage(requirementDescription)
                        .isContactMasked(true)
                        .clientName(customer.getDisplayName())
                        .clientEmail(customer.getEmail())
                        .clientPhone(customer.getPhone())
                        .serviceCategory(taxService.getName())
                        .requirementDescription(requirementDescription)
                        .urgency(Urgency.URGENT)
                        .leadStatus(LeadStatus.CONTACTED)
                        .build());

        proposalRepository.save(
                MarketplaceProposalEntity.builder()
                        .organizationId(profile.getOrganizationId())
                        .marketplaceProfileId(profile.getId())
                        .customerId(customer.getId())
                        .leadId(lead.getId())
                        .proposalTitle("GST Registration — New Trading Business")
                        .scopeOfWork("End-to-end GST registration filing with the department, including document preparation and application tracking.")
                        .deliverables("GSTIN Certificate, Application Reference Number, Compliance Checklist")
                        .feeAmount(new BigDecimal("1999.00"))
                        .pricingType(PricingType.FIXED)
                        .estimatedTimelineDays(7)
                        .proposalStatus(ProposalStatus.SENT)
                        .accessToken(UUID.randomUUID().toString().replace("-", ""))
                        .validUntil(LocalDate.now().plusDays(10))
                        .build());

        log.info("Seeded in-progress customer journey for {} -> {}", customer.getEmail(), profile.getSlug());
    }

    private MarketplaceCustomerProfileEntity ensureCustomer(String email, String firstName, String lastName, String phone,
                                                              String city, String state, String pincode, CustomerType customerType) {
        Optional<MarketplaceCustomerProfileEntity> existing = customerProfileRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            // A user exists but no customer profile — inconsistent state, skip rather than corrupt it.
            log.warn("User exists for {} without a marketplace customer profile; skipping demo seed for this customer.", email);
            return null;
        }

        RoleEntity customerRole = roleRepository.findByCodeAndIsSystemRoleTrue("MARKETPLACE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code("MARKETPLACE_CUSTOMER")
                        .name("Marketplace Customer")
                        .description("Individual or Business accessing tax marketplace services")
                        .isSystemRole(true)
                        .build()));

        UserEntity user = userRepository.save(
                UserEntity.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .firstName(firstName)
                        .lastName(lastName)
                        .phone(phone)
                        .status(UserStatus.ACTIVE)
                        .organizationId(null)
                        .roles(new HashSet<>(Set.of(customerRole)))
                        .build());

        MarketplaceCustomerProfileEntity profile = customerProfileRepository.save(
                MarketplaceCustomerProfileEntity.builder()
                        .userId(user.getId())
                        .customerType(customerType)
                        .firstName(firstName)
                        .lastName(lastName)
                        .displayName(firstName + " " + lastName)
                        .email(email)
                        .phone(phone)
                        .city(city)
                        .state(state)
                        .pincode(pincode)
                        .preferredLanguage("English")
                        .status(CustomerProfileStatus.ACTIVE)
                        .build());

        log.info("Seeded demo customer account: {} — see README for demo credentials", email);
        return profile;
    }
}
