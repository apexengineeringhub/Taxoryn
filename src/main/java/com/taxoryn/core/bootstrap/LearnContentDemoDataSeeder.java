package com.taxoryn.core.bootstrap;

import com.taxoryn.module.content.entity.*;
import com.taxoryn.module.content.repository.ContentRepository;
import com.taxoryn.module.content.repository.ContentTagRepository;
import com.taxoryn.module.marketplace.entity.TaxServiceCategoryEntity;
import com.taxoryn.module.marketplace.entity.TaxServiceEntity;
import com.taxoryn.module.marketplace.repository.TaxServiceCategoryRepository;
import com.taxoryn.module.marketplace.repository.TaxServiceRepository;
import com.taxoryn.module.user.entity.UserEntity;
import com.taxoryn.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Seeds realistic demo content for Taxoryn Learn across all content types
 * (ARTICLE, GUIDE, FAQ, TAX_UPDATE, VIDEO) linked to the Controlled Tax Service Master.
 * <p>
 * Runs only in {@code dev} and {@code demo} profiles at {@code @Order(3)} (after
 * {@link DemoDataSeeder} and {@link MarketplaceDemoDataSeeder}).
 * Idempotent: checks by slug before creating.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "demo", "test"})
@Order(3)
public class LearnContentDemoDataSeeder implements CommandLineRunner {

    private final ContentRepository contentRepository;
    private final ContentTagRepository tagRepository;
    private final TaxServiceCategoryRepository categoryRepository;
    private final TaxServiceRepository taxServiceRepository;
    private final UserRepository userRepository;
    private final org.springframework.core.env.Environment environment;

    @Override
    public void run(String... args) {
        List<String> activeProfiles = java.util.Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("prod") || activeProfiles.contains("production")) {
            log.error("CRITICAL SECURITY GUARD: LearnContentDemoDataSeeder execution blocked because production profile is active!");
            return;
        }

        // Fast-path for warm restarts: content items are the last artifact this seeder
        // produces, so their presence means categories/services/tags were already fully
        // seeded on a prior run. Skip re-walking every ensure-check (dozens of individual
        // SELECTs for categories, services, tags, and each article/video).
        if (contentRepository.count() > 0) {
            log.info("Learn content already seeded — skipping re-seed pass.");
            return;
        }
        seedLearnContent();
    }

    private TaxServiceCategoryEntity ensureCategory(String code, String name, String description, int sortOrder) {
        return categoryRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> categoryRepository.save(
                        TaxServiceCategoryEntity.builder()
                                .code(code)
                                .name(name)
                                .description(description)
                                .icon("FileText")
                                .sortOrder(sortOrder)
                                .isActive(true)
                                .build()
                ));
    }

    private TaxServiceEntity ensureService(TaxServiceCategoryEntity category, String code, String name, String description, int sortOrder) {
        return taxServiceRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> taxServiceRepository.save(
                        TaxServiceEntity.builder()
                                .categoryId(category != null ? category.getId() : null)
                                .category(category)
                                .code(code)
                                .name(name)
                                .description(description)
                                .sortOrder(sortOrder)
                                .isActive(true)
                                .build()
                ));
    }

    private void seedLearnContent() {
        // Ensure categories exist
        TaxServiceCategoryEntity incomeTaxCat = ensureCategory("INCOME_TAX", "Income Tax", "Individual and business income tax services", 1);
        TaxServiceCategoryEntity gstCat = ensureCategory("GST", "GST", "Goods & Services Tax registration, filing and advisory", 2);
        TaxServiceCategoryEntity tdsCat = ensureCategory("TDS", "TDS", "Tax Deducted at Source compliance and filing", 3);

        // Ensure tax services exist
        TaxServiceEntity itrFiling = ensureService(incomeTaxCat, "ITR_FILING", "ITR Filing & Computation", "Income tax return preparation, computation and e-filing for individuals and businesses.", 1);
        TaxServiceEntity itrPlanning = ensureService(incomeTaxCat, "ITR_PLANNING", "Tax Planning & Advisory", "Comprehensive tax planning, old vs new regime comparison, and investment advisory.", 2);
        TaxServiceEntity itrNotice = ensureService(incomeTaxCat, "ITR_NOTICE_ASSISTANCE", "ITR Notice & Scrutiny", "Handling Section 143(1), 142(1), 148 notices and rectification requests.", 3);
        TaxServiceEntity itrRefund = ensureService(incomeTaxCat, "ITR_REFUND_ASSISTANCE", "ITR Refund Reissue Assistance", "Resolving failed refunds, bank account re-validation, and refund tracking.", 4);
        TaxServiceEntity gstReg = ensureService(gstCat, "GST_REGISTRATION", "New GST Registration", "New GSTIN application, amendments, and ARN tracking for businesses and freelancers.", 1);
        TaxServiceEntity gstFiling = ensureService(gstCat, "GST_RETURN_FILING", "Monthly & Quarterly GST Filing", "Filing GSTR-1, GSTR-3B, GSTR-4, and reconciliations for regular and composition dealers.", 2);
        TaxServiceEntity gstAdvisory = ensureService(gstCat, "GST_ADVISORY", "GST Advisory & Notice Management", "Handling GST DRC notices, ASMT-10 scrutiny, and input tax credit reconciliations.", 3);
        TaxServiceEntity tdsFiling = ensureService(tdsCat, "TDS_RETURN_FILING", "Quarterly TDS Return Filing", "Preparing and filing Form 24Q, 26Q, 27Q, generating Form 16/16A.", 1);
        TaxServiceEntity tdsCompliance = ensureService(tdsCat, "TDS_COMPLIANCE", "TDS Compliance & 26AS/AIS Reconciliation", "Reconciliation of tax credits across 26AS, AIS, and TIS, challan verification.", 2);

        // Resolve author
        UserEntity author = userRepository.findAll().stream().findFirst().orElse(null);
        UUID authorId = author != null ? author.getId() : null;

        // Tags
        ContentTagEntity tagIncomeTax = ensureTag("Income Tax", "income-tax");
        ContentTagEntity tagItr1 = ensureTag("ITR-1", "itr-1");
        ContentTagEntity tagSalaried = ensureTag("Salaried", "salaried");
        ContentTagEntity tagTaxPlanning = ensureTag("Tax Planning", "tax-planning");
        ContentTagEntity tagNewRegime = ensureTag("New Tax Regime", "new-tax-regime");
        ContentTagEntity tagGst = ensureTag("GST", "gst");
        ContentTagEntity tagGstr1 = ensureTag("GSTR-1", "gstr-1");
        ContentTagEntity tagGstr3b = ensureTag("GSTR-3B", "gstr-3b");
        ContentTagEntity tagMsme = ensureTag("MSME", "msme");
        ContentTagEntity tagStartup = ensureTag("Startup", "startup");
        ContentTagEntity tagNotice = ensureTag("Tax Notice", "tax-notice");
        ContentTagEntity tagRefund = ensureTag("Tax Refund", "tax-refund");
        ContentTagEntity tagTds = ensureTag("TDS", "tds");
        ContentTagEntity tagForm26as = ensureTag("Form 26AS", "form-26as");
        ContentTagEntity tagAis = ensureTag("AIS", "ais");
        ContentTagEntity tagCompliance = ensureTag("Compliance", "compliance");

        // 1. ARTICLE: Complete Guide to Filing ITR-1 (Sahaj)
        createContentIfAbsent(
                ContentType.ARTICLE,
                "Complete Guide to Filing ITR-1 (Sahaj) for Salaried Individuals in AY 2026-27",
                "complete-guide-itr-1-sahaj-ay-2026-27",
                "A comprehensive step-by-step tutorial on calculating taxable salary income, claiming standard deduction, selecting new vs old tax regime, and e-verifying Form ITR-1 with Aadhaar OTP.",
                """
                ## 1. Who is Eligible to File ITR-1 (Sahaj)?
                
                ITR-1 (Sahaj) is designed for resident individuals having total income up to **₹50 Lakhs** from the following sources:
                - Income from Salary or Pension
                - Income from One House Property (excluding cases where loss is brought forward from previous years)
                - Income from Other Sources (Interest on savings/FD, family pension, dividends, etc.)
                - Agricultural Income up to **₹5,000**
                
                > **Note:** If you are a Director in a company, hold unlisted equity shares, or have foreign assets/income, you must file **ITR-2** or **ITR-3** instead.
                
                ---
                
                ## 2. Essential Documents Checklist
                
                Before opening the e-filing portal, make sure you have gathered:
                1. **Form 16 (Part A & Part B)** provided by your employer.
                2. **Annual Information Statement (AIS) & Taxpayer Information Summary (TIS)** downloaded from the Income Tax portal.
                3. **Form 26AS** to cross-verify TDS deposited with the Central Government.
                4. **Bank Account Statements** for all active accounts to report interest income.
                5. **Rent receipts, Home Loan interest certificates, and Medical Insurance receipts** (if choosing Old Regime).
                
                ---
                
                ## 3. Step-by-Step Filing Workflow
                
                ### Step 1: Log in and Select Assessment Year
                Visit `incometax.gov.in`, sign in with PAN and password, and navigate to **e-File > Income Tax Returns > File Income Tax Return**. Select **AY 2026-27** and Online mode.
                
                ### Step 2: Confirm Pre-filled Data
                Review personal details, gross salary, allowances under Section 10, and verify pre-filled TDS credits matching your AIS.
                
                ### Step 3: Choose Tax Regime
                Under the default New Tax Regime, enjoy updated slab rates and a **₹75,000 Standard Deduction**. If opting for the Old Regime, submit Form 10-IEA if applicable and declare 80C/80D deductions.
                
                ### Step 4: Calculate Tax Liability & E-Verify
                Check refund amount or self-assessment tax dues under Section 140A. Finally, e-verify using **Aadhaar OTP** or **Net Banking EVC** within 30 days to complete your submission.
                """,
                "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=800&q=80",
                "How to File ITR-1 Sahaj Online AY 2026-27 | Taxoryn Learn",
                "Learn how to file ITR-1 Sahaj online for AY 2026-27. Step-by-step tutorial on Form 16, AIS reconciliation, deductions, and Aadhaar OTP verification.",
                null,
                null,
                incomeTaxCat,
                itrFiling,
                Set.of(itrFiling, itrPlanning),
                Set.of(tagIncomeTax, tagItr1, tagSalaried),
                authorId,
                Instant.now().minus(14, ChronoUnit.DAYS)
        );

        // 2. ARTICLE: Section 80C vs New Tax Regime
        createContentIfAbsent(
                ContentType.ARTICLE,
                "Section 80C vs New Tax Regime: Which One Saves More Tax for You?",
                "section-80c-vs-new-tax-regime-comparison",
                "Detailed mathematical breakdown comparing the deductions under the Old Tax Regime with the revised tax slabs and standard deduction of the New Tax Regime.",
                """
                ## The Great Indian Tax Dilemma
                
                With continuous revisions to the **New Tax Regime** (Section 115BAC), Indian salaried taxpayers often wonder whether sacrificing Chapter VI-A deductions (Section 80C, 80D, 80CCD) is worth the lower tax rates.
                
                ### Key Differences at a Glance
                
                | Parameter | Old Tax Regime | New Tax Regime (Default) |
                | :--- | :--- | :--- |
                | **Standard Deduction** | ₹50,000 | ₹75,000 |
                | **Basic Exemption Limit** | ₹2,50,000 | ₹3,00,000 |
                | **Section 80C Limit** | Up to ₹1,50,000 | Not Allowed |
                | **Section 80D (Health)** | Up to ₹25,000 / ₹50,000 | Not Allowed |
                | **HRA / Home Loan (Sec 24)** | Allowed | Not Allowed (Self-occupied) |
                | **Full Tax Rebate (Sec 87A)** | Up to ₹5 Lakhs taxable income | Up to ₹7 Lakhs taxable income |
                
                ---
                
                ## The Breakeven Formula
                
                For a gross salary between **₹12 Lakhs and ₹15 Lakhs**, you typically need total deductions exceeding **₹3.75 Lakhs to ₹4.25 Lakhs** (including 80C + 80D + HRA + Home loan interest) for the Old Regime to yield lower total tax.
                
                ### Professional Recommendation
                If you do not have substantial home loan interest or high HRA outgo, the New Tax Regime provides higher take-home salary with zero documentation overhead. Consult a tax professional for personalized salary structuring.
                """,
                "https://images.unsplash.com/photo-1554224154-26032ffc0d07?auto=format&fit=crop&w=800&q=80",
                "Section 80C vs New Tax Regime Comparison | Taxoryn",
                "Compare Old vs New Tax Regime for salaried employees. Calculate breakeven point between 80C deductions, HRA, and revised New Tax Regime slab rates.",
                null,
                null,
                incomeTaxCat,
                itrPlanning,
                Set.of(itrPlanning, itrFiling),
                Set.of(tagIncomeTax, tagTaxPlanning, tagNewRegime, tagSalaried),
                authorId,
                Instant.now().minus(10, ChronoUnit.DAYS)
        );

        // 3. GUIDE: GST Return Filing Manual for MSMEs
        createContentIfAbsent(
                ContentType.GUIDE,
                "GST Return Filing: Step-by-Step Practical Compliance Manual for MSMEs",
                "gst-return-filing-step-by-step-manual-msme",
                "Understand the end-to-end filing workflow for GSTR-1, GSTR-3B, QRMP Scheme, input tax credit reconciliation with GSTR-2B, and avoiding late fee penalties.",
                """
                ## Overview of Monthly & Quarterly GST Compliance
                
                For regular GST registered taxpayers in India, timely filing of GST returns is mandatory to avoid suspension of GSTIN, interest on tax liabilities (18% p.a.), and blocking of E-Way Bill generation.
                
                ---
                
                ## 1. Primary Returns Explained
                
                ### GSTR-1: Outward Supplies Statement
                - **What it contains:** Details of all B2B invoices, B2C supplies, credit/debit notes, and export invoices issued during the tax period.
                - **Due Date:** 11th of the succeeding month (Monthly filers) or 13th of the month succeeding the quarter (QRMP filers).
                - **Rule 59(6) Impact:** GSTR-1 cannot be filed if GSTR-3B for the previous period is unfiled.
                
                ### GSTR-3B: Monthly Summary Return & Tax Payment
                - **What it contains:** Summary of outward taxable value, eligible and ineligible Input Tax Credit (ITC), reverse charge liability, and tax payments.
                - **Due Date:** 20th of the succeeding month (Monthly filers) or 22nd/24th of the quarter-end month (QRMP state-wise).
                
                ---
                
                ## 2. Best Practices for ITC Reconciliation (GSTR-2B)
                
                1. Always reconcile purchase registers with **auto-drafted GSTR-2B** before finalizing GSTR-3B.
                2. Do not claim provisional ITC for invoices not reflected in GSTR-2B under Section 16(2)(aa).
                3. Reverse ITC under Rule 37 if payment to vendor is not made within 180 days from invoice date.
                """,
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=800&q=80",
                "GST Return Filing Practical Manual for MSMEs | Taxoryn Guides",
                "Comprehensive manual on filing GSTR-1 and GSTR-3B returns. Master GSTR-2B ITC matching, QRMP deadlines, and GST compliance rules.",
                null,
                null,
                gstCat,
                gstFiling,
                Set.of(gstFiling, gstAdvisory),
                Set.of(tagGst, tagGstr1, tagGstr3b, tagMsme, tagCompliance),
                authorId,
                Instant.now().minus(7, ChronoUnit.DAYS)
        );

        // 4. GUIDE: New GST Registration Checklist
        createContentIfAbsent(
                ContentType.GUIDE,
                "New GST Registration Checklist for Proprietorships, LLPs, and Companies",
                "new-gst-registration-complete-checklist-process",
                "Mandatory documents, threshold turnover limits (₹40 Lakhs for goods, ₹20 Lakhs for services), place of business proof, and biometric Aadhaar authentication procedure.",
                """
                ## When is GST Registration Mandatory?
                
                Under the Central Goods and Services Tax Act, 2017:
                - **Goods Suppliers:** Aggregate annual turnover exceeding **₹40 Lakhs** (₹20 Lakhs in special category states).
                - **Service Providers:** Aggregate annual turnover exceeding **₹20 Lakhs** (₹10 Lakhs in special category states).
                - **Mandatory Cases:** Inter-state taxable supplies, E-commerce sellers, Reverse Charge Mechanism (RCM) recipients, and casual taxable persons.
                
                ---
                
                ## Required Documents by Entity Type
                
                ### Sole Proprietorship
                - PAN Card and Aadhaar Card of Proprietor
                - Passport size photograph
                - Electricity Bill / Property Tax Receipt of Principal Place of Business
                - Rent Agreement & No Objection Certificate (NOC) from Property Owner
                - Cancelled Cheque or Bank Statement showing Name and IFSC
                
                ### Private Limited Company / LLP
                - Company / LLP PAN Card
                - Certificate of Incorporation (COI) & MOA/AOA or LLP Agreement
                - Board Resolution / Authorization Letter for Authorized Signatory
                - PAN & Aadhaar of all Directors / Designated Partners
                
                ---
                
                ## Timeline & Verification Process
                
                GST applications with successful **Aadhaar OTP Authentication** are generally approved within **7 working days** without physical site inspection.
                """,
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80",
                "New GST Registration Checklist & Step-by-Step Guide | Taxoryn",
                "Complete guide and document checklist for new GST registration in India. Covers sole proprietorships, LLPs, Private Limited firms, and Aadhaar authentication.",
                null,
                null,
                gstCat,
                gstReg,
                Set.of(gstReg, gstAdvisory),
                Set.of(tagGst, tagStartup, tagMsme, tagCompliance),
                authorId,
                Instant.now().minus(5, ChronoUnit.DAYS)
        );

        // 5. FAQ: Income Tax Defective Return Notice under Section 139(9)
        createContentIfAbsent(
                ContentType.FAQ,
                "Income Tax Defective Return Notice under Section 139(9): Causes & Rectification",
                "income-tax-defective-return-notice-section-139-9-faq",
                "Common reasons why the IT Department issues Section 139(9) notices, time limits for response (15 days), and step-by-step instructions on submitting a corrected return.",
                """
                ## Frequently Asked Questions on Section 139(9) Notices
                
                ### Q1: What is a Defective Return Notice under Section 139(9)?
                **Answer:** When the Income Tax Department's Centralized Processing Centre (CPC) finds errors, inconsistencies, missing schedules, or incomplete audit information during automated validation of your ITR, it marks the return as "Defective" and issues a communication under Section 139(9).
                
                ---
                
                ### Q2: What are the most common causes for a defective notice?
                **Answer:**
                1. **TDS Claimed vs Income Not Declared:** Tax deducted in 26AS is claimed, but the corresponding gross revenue is missing in Schedule BP or Other Sources.
                2. **Balance Sheet & P&L Incomplete:** Declaring business income under regular scheme without filling balance sheet details.
                3. **Wrong ITR Form Chosen:** E.g., filing ITR-1 when presumptive business income under 44AD is present.
                4. **Tax Audit Report Missing:** Form 3CA/3CB not filed where accounts were required to be audited.
                
                ---
                
                ### Q3: What is the deadline to respond?
                **Answer:** You must respond and submit the corrected return within **15 days** from the date of receipt of the notice. If you need more time, you can apply for an extension on the portal.
                
                ---
                
                ### Q4: How do I submit the response online?
                **Answer:**
                1. Log in to `incometax.gov.in`.
                2. Go to **Pending Actions > e-Proceedings**.
                3. Locate Notice u/s 139(9) and click **View Details > Submit Response**.
                4. Select "Agree", upload corrected JSON/fill revised schedules, and submit with OTP verification.
                """,
                "https://images.unsplash.com/photo-1450133064473-71024230f91b?auto=format&fit=crop&w=800&q=80",
                "Defective Return Notice u/s 139(9) FAQ & Rectification | Taxoryn",
                "How to resolve Income Tax Defective Return Notice under Section 139(9). Common errors, 15-day response window, and step-by-step rectification process.",
                null,
                null,
                incomeTaxCat,
                itrNotice,
                Set.of(itrNotice, itrFiling),
                Set.of(tagIncomeTax, tagNotice, tagCompliance),
                authorId,
                Instant.now().minus(4, ChronoUnit.DAYS)
        );

        // 6. FAQ: Delayed Income Tax Refund
        createContentIfAbsent(
                ContentType.FAQ,
                "Delayed Income Tax Refund: Why Is Your Refund Not Processed and How to Track Status?",
                "delayed-income-tax-refund-reasons-tracking-faq",
                "Find out why income tax refunds get delayed after ITR filing, how to revalidate pre-validated bank accounts, response to outstanding demands, and filing grievance on e-filing.",
                """
                ## Frequently Asked Questions on Delayed Income Tax Refunds
                
                ### Q1: How long does CPC take to process income tax refunds?
                **Answer:** For e-verified returns with no discrepancies, refunds are typically credited within **2 to 6 weeks** from the date of ITR processing under Section 143(1).
                
                ---
                
                ### Q2: Why has my refund not been credited yet?
                **Answer:** Common reasons include:
                - **Bank Account Not Validated:** Your PAN and Name in bank records do not match the IT database, or the account is inactive.
                - **Outstanding Tax Demands (Section 245):** CPC has proposed adjusting your current year refund against unpaid tax demands from earlier assessment years.
                - **ITR Under Review / Scrutiny:** Selected for detailed verification or manual processing.
                - **Unverified ITR:** Return was submitted but e-verification was not completed within the 30-day window.
                
                ---
                
                ### Q3: How do I check my live refund status?
                **Answer:**
                - On the e-Filing Portal: Go to **e-File > Income Tax Returns > View Filed Returns**.
                - On NSDL TIN portal: Use the `tin.tin.nsdl.com/oltas/refundstatus.html` page by entering PAN and Assessment Year.
                """,
                "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=800&q=80",
                "Income Tax Refund Delay Reasons & Live Tracking FAQ | Taxoryn",
                "Why is your Income Tax refund delayed? Common bank account validation errors, Section 245 adjustments, and step-by-step refund reissue guide.",
                null,
                null,
                incomeTaxCat,
                itrRefund,
                Set.of(itrRefund, itrFiling),
                Set.of(tagIncomeTax, tagRefund, tagCompliance),
                authorId,
                Instant.now().minus(3, ChronoUnit.DAYS)
        );

        // 7. TAX_UPDATE: Key Statutory Deadlines for FY 2026-27
        createContentIfAbsent(
                ContentType.TAX_UPDATE,
                "Key Income Tax, GST, and TDS Statutory Compliance Deadlines for FY 2026-27",
                "key-income-tax-gst-deadlines-budget-amendments-2026-27",
                "Monthly and quarterly statutory compliance calendar for advance tax instalments, TDS Challan 281 deposit, GSTR-1 / GSTR-3B due dates, and annual audit filings.",
                """
                ## Compliance Calendar & Regulatory Highlights for FY 2026-27
                
                Stay ahead of statutory deadlines to ensure zero interest, late fees, and penalty exposure.
                
                ---
                
                ### Monthly Recurring Obligations
                - **7th of Every Month:** Deposit of TDS/TCS deducted in the preceding month (Challan ITNS 281).
                - **11th of Every Month:** Filing of GSTR-1 (Outward Supplies) by monthly taxpayers.
                - **13th of Every Month:** Filing of Invoice Furnishing Facility (IFF) by QRMP taxpayers.
                - **20th of Every Month:** Filing of GSTR-3B summary return and net cash tax payment.
                
                ---
                
                ### Advance Tax Instalments (Individuals & Corporates)
                - **15th June 2026:** 1st Instalment (15% of estimated tax liability)
                - **15th September 2026:** 2nd Instalment (45% of estimated tax liability)
                - **15th December 2026:** 3rd Instalment (75% of estimated tax liability)
                - **15th March 2027:** 4th Instalment (100% of estimated tax liability)
                
                ---
                
                ### Quarterly TDS Return Deadlines (Form 24Q, 26Q, 27Q)
                - **Q1 (Apr - Jun):** 31st July 2026
                - **Q2 (Jul - Sep):** 31st October 2026
                - **Q3 (Oct - Dec):** 31st January 2027
                - **Q4 (Jan - Mar):** 31st May 2027
                """,
                "https://images.unsplash.com/photo-1506784365847-bbad939e9335?auto=format&fit=crop&w=800&q=80",
                "Tax Compliance Calendar & Deadlines FY 2026-27 | Taxoryn Learn",
                "Comprehensive statutory compliance calendar for Income Tax, GST, TDS, and Advance Tax instalments for Financial Year 2026-27.",
                null,
                null,
                gstCat,
                gstFiling,
                Set.of(gstFiling, tdsFiling, itrFiling),
                Set.of(tagCompliance, tagGst, tagIncomeTax, tagTds),
                authorId,
                Instant.now().minus(2, ChronoUnit.DAYS)
        );

        // 8. VIDEO: Demystifying TDS on Salary & Form 26AS vs AIS Reconciliation
        createContentIfAbsent(
                ContentType.VIDEO,
                "Demystifying TDS on Salary (Section 192) & Form 26AS vs AIS Reconciliation",
                "demystifying-tds-salary-form-26as-ais-reconciliation-video",
                "Watch a licensed Chartered Accountant explain how employers calculate TDS under Section 192, how to read Form 16, and how to reconcile with Annual Information Statement (AIS).",
                """
                ## Video Overview: Salary TDS & Tax Statement Reconciliation
                
                In this masterclass session, our expert practitioner breaks down:
                
                1. **How Employers Compute Monthly TDS:** Applying average tax rates under Section 192 on estimated annual salary income.
                2. **Deciphering Form 16:**
                   - **Part A:** Certificate issued under TRACES showing quarter-wise TDS deposit and BSR challan details.
                   - **Part B:** Comprehensive computation of salary breakdown, Section 10 exemptions (HRA, LTA), Standard Deduction, and Chapter VI-A deductions.
                3. **Step-by-Step AIS & Form 26AS Reconciliation:**
                   - Identifying mismatch between salary paid and TDS credits in Part A of Form 26AS.
                   - Checking high-value financial transactions (SFT) recorded in the Annual Information Statement (AIS).
                   - Submitting online feedback on AIS for incorrect or duplicate reporting.
                """,
                "https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=800&q=80",
                "TDS on Salary & AIS Reconciliation Video Guide | Taxoryn",
                "Video masterclass on Section 192 salary TDS calculation, Form 16 breakdown, and Form 26AS vs AIS reconciliation for tax filing.",
                "dQw4w9WgXcQ",
                780,
                tdsCat,
                tdsFiling,
                Set.of(tdsFiling, tdsCompliance, itrFiling),
                Set.of(tagTds, tagSalaried, tagForm26as, tagAis, tagIncomeTax),
                authorId,
                Instant.now().minus(1, ChronoUnit.DAYS)
        );

        // 9. DRAFT ARTICLE: Transfer Pricing Guidelines (Draft example for Admin panel review)
        createContentIfAbsent(
                ContentType.ARTICLE,
                "Draft: Transfer Pricing Documentation & Arm's Length Masterclass",
                "draft-transfer-pricing-documentation-guidelines",
                "Internal draft on Section 92E accountant report, Master File, Local File, and benchmark methods under Indian Transfer Pricing regulations.",
                """
                ## Internal Editorial Draft
                
                This draft article covers international transaction benchmarks, Comparable Uncontrolled Price (CUP) method, Transactional Net Margin Method (TNMM), and safe harbour rules for IT/ITES entities.
                """,
                null,
                "Transfer Pricing Guidelines Draft",
                "Internal editorial draft for transfer pricing.",
                null,
                null,
                incomeTaxCat,
                null,
                Collections.emptySet(),
                Set.of(tagCompliance),
                authorId,
                null // DRAFT status
        );

        log.info("Taxoryn Learn demo content seed check complete.");
    }

    private ContentTagEntity ensureTag(String name, String slug) {
        return tagRepository.findBySlug(slug)
                .orElseGet(() -> tagRepository.save(
                        ContentTagEntity.builder()
                                .name(name)
                                .slug(slug)
                                .createdAt(Instant.now())
                                .build()
                ));
    }

    private void createContentIfAbsent(
            ContentType contentType,
            String title,
            String slug,
            String summary,
            String body,
            String thumbnailUrl,
            String seoTitle,
            String metaDescription,
            String youtubeVideoId,
            Integer videoDurationSeconds,
            TaxServiceCategoryEntity category,
            TaxServiceEntity primaryTaxService,
            Set<TaxServiceEntity> taxServices,
            Set<ContentTagEntity> tags,
            UUID authorId,
            Instant publishedAt
    ) {
        if (contentRepository.existsBySlug(slug)) {
            return;
        }

        ContentStatus status = publishedAt != null ? ContentStatus.PUBLISHED : ContentStatus.DRAFT;

        ContentEntity content = ContentEntity.builder()
                .contentType(contentType)
                .title(title)
                .slug(slug)
                .summary(summary)
                .body(body)
                .thumbnailUrl(thumbnailUrl)
                .featuredImageUrl(thumbnailUrl)
                .altText(title)
                .seoTitle(seoTitle)
                .metaDescription(metaDescription)
                .youtubeVideoId(youtubeVideoId)
                .videoDurationSeconds(videoDurationSeconds)
                .status(status)
                .versionNumber(1)
                .categoryId(category != null ? category.getId() : null)
                .category(category)
                .taxServiceId(primaryTaxService != null ? primaryTaxService.getId() : null)
                .taxService(primaryTaxService)
                .taxServices(new HashSet<>(taxServices))
                .tags(new HashSet<>(tags))
                .scope(ContentOwnershipScope.PLATFORM)
                .authorId(authorId)
                .publishedAt(publishedAt)
                .build();

        contentRepository.save(content);
        log.info("Seeded Taxoryn Learn demo content: [{}] '{}' (slug: {})", contentType, title, slug);
    }
}
