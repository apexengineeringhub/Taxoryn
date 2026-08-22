package com.taxoryn.module.tds.service;

import com.taxoryn.module.tds.dto.TdsComputationRequest;
import com.taxoryn.module.tds.dto.TdsComputationResultDto;
import com.taxoryn.module.tds.dto.TdsSectionRateDto;
import com.taxoryn.module.tds.entity.TdsDeducteeEntryEntity.DeducteeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TdsCalculatorService {

    private static final BigDecimal CESS_RATE = new BigDecimal("4.00"); // 4% Health & Education Cess
    private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("200.00"); // Sec 234E
    private static final Map<String, TdsSectionRateDto> SECTION_REGISTRY = new LinkedHashMap<>();

    static {
        registerSection("192", "Salary Income", "FORM_24Q", new BigDecimal("0.00"), new BigDecimal("0.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"), "TDS on salary as per applicable individual income tax slab rates (New/Old regime).");
        registerSection("192A", "Premature EPF Withdrawal", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("50000.00"), new BigDecimal("50000.00"), new BigDecimal("20.00"), "Applicable on EPF withdrawal before 5 years of continuous service exceeding ₹50,000.");
        registerSection("193", "Interest on Securities", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10000.00"), new BigDecimal("10000.00"), new BigDecimal("20.00"), "TDS on interest on debentures, bonds, and listed securities.");
        registerSection("194", "Dividends", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("5000.00"), new BigDecimal("5000.00"), new BigDecimal("20.00"), "TDS on dividend distributed by Indian company if exceeding ₹5,000/year for resident individual.");
        registerSection("194A", "Interest other than Securities (Bank / NBFC / Loans)", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("40000.00"), new BigDecimal("40000.00"), new BigDecimal("20.00"), "Threshold is ₹40,000 (₹50,000 for Senior Citizens) for Banks/Post Office; ₹5,000 for other loans/advances.");
        registerSection("194C", "Payments to Contractors / Subcontractors", "FORM_26Q", new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("100000.00"), new BigDecimal("30000.00"), new BigDecimal("20.00"), "Rate is 1% for Individual/HUF, 2% for Companies/LLPs. Threshold: ₹30,000 single bill or ₹1,00,000 aggregate/FY. Transporter exempt under 194C(6) if declaration + PAN provided.");
        registerSection("194D", "Insurance Commission", "FORM_26Q", new BigDecimal("5.00"), new BigDecimal("10.00"), new BigDecimal("15000.00"), new BigDecimal("15000.00"), new BigDecimal("20.00"), "TDS on insurance agent commission exceeding ₹15,000/year.");
        registerSection("194DA", "Maturity of Life Insurance Policy", "FORM_26Q", new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("100000.00"), new BigDecimal("100000.00"), new BigDecimal("20.00"), "5% on net income portion if maturity proceeds are not exempt under section 10(10D).");
        registerSection("194H", "Commission or Brokerage", "FORM_26Q", new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("15000.00"), new BigDecimal("15000.00"), new BigDecimal("20.00"), "Rate reduced to 2% (w.e.f. Oct 1, 2024). Threshold is ₹15,000 per financial year.");
        registerSection("194I(a)", "Rent on Plant, Machinery & Equipment", "FORM_26Q", new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("240000.00"), new BigDecimal("240000.00"), new BigDecimal("20.00"), "2% on lease/rent of machinery, plant, or factory equipment. Threshold ₹2,40,000/year.");
        registerSection("194I(b)", "Rent on Land, Building, Furniture & Fittings", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("240000.00"), new BigDecimal("240000.00"), new BigDecimal("20.00"), "10% on rent of office space, commercial property, land/building. Threshold ₹2,40,000/year.");
        registerSection("194IA", "TDS on Sale of Immovable Property", "FORM_26QB", new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("5000000.00"), new BigDecimal("5000000.00"), new BigDecimal("20.00"), "1% on total sale consideration or stamp duty value exceeding ₹50,00,000.");
        registerSection("194IB", "Rent paid by Individual / HUF not under Tax Audit", "FORM_26QC", new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("50000.00"), new BigDecimal("50000.00"), new BigDecimal("20.00"), "5% on rent exceeding ₹50,000 per month or part of month.");
        registerSection("194J(a)", "Technical Fees, Royalty & Call Centre", "FORM_26Q", new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("30000.00"), new BigDecimal("30000.00"), new BigDecimal("20.00"), "2% for fees for technical services (FTS), royalty (sale/distribution of film) or call centre operation. Threshold ₹30,000/FY.");
        registerSection("194J(b)", "Professional Fees & Director Remuneration", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("30000.00"), new BigDecimal("30000.00"), new BigDecimal("20.00"), "10% for professional services (legal, CA, medical, engineering, architectural, consulting, director fees). No threshold for director fees.");
        registerSection("194LA", "Compensation on Acquisition of Immovable Property", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("250000.00"), new BigDecimal("250000.00"), new BigDecimal("20.00"), "10% on compensation exceeding ₹2,50,000.");
        registerSection("194M", "Contractor/Professional payments by Individual/HUF", "FORM_26QD", new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("5000000.00"), new BigDecimal("5000000.00"), new BigDecimal("20.00"), "5% on aggregate payments exceeding ₹50,00,000 in a year.");
        registerSection("194N", "Cash Withdrawals", "FORM_26Q", new BigDecimal("2.00"), new BigDecimal("2.00"), new BigDecimal("10000000.00"), new BigDecimal("10000000.00"), new BigDecimal("20.00"), "2% on cash withdrawal exceeding ₹1 Crore (₹20 Lakhs for non-filers).");
        registerSection("194O", "E-commerce Operator", "FORM_26Q", new BigDecimal("0.10"), new BigDecimal("0.10"), new BigDecimal("500000.00"), new BigDecimal("500000.00"), new BigDecimal("5.00"), "0.1% on gross sale of goods/services facilitated via e-commerce marketplace (reduced from 1%).");
        registerSection("194Q", "Purchase of Goods exceeding ₹50 Lakhs", "FORM_26Q", new BigDecimal("0.10"), new BigDecimal("0.10"), new BigDecimal("5000000.00"), new BigDecimal("5000000.00"), new BigDecimal("5.00"), "0.1% on purchase value exceeding ₹50 Lakhs in a FY for buyers with turnover > ₹10 Cr.");
        registerSection("194R", "Perquisites / Benefits in Business or Profession", "FORM_26Q", new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("20000.00"), new BigDecimal("20000.00"), new BigDecimal("20.00"), "10% on value of benefit/perquisite arising from business or profession exceeding ₹20,000/FY.");
        registerSection("194S", "Transfer of Virtual Digital Assets (Crypto / NFT)", "FORM_26QE", new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("50000.00"), new BigDecimal("50000.00"), new BigDecimal("20.00"), "1% on consideration for transfer of VDA (₹10,000 threshold for specified persons, ₹50,000 others).");
        registerSection("195", "Payments to Non-Residents / Foreign Companies", "FORM_27Q", new BigDecimal("20.00"), new BigDecimal("30.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"), "TDS on payments to Non-Residents subject to DTAA treaty rates or domestic rates.");
        registerSection("206C(1)", "TCS on Scrap, Minerals, Timber & Liquor", "FORM_27EQ", new BigDecimal("1.00"), new BigDecimal("1.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5.00"), "TCS on sale of scrap (1%), alcoholic liquor (5%), tendu leaves (5%), timber (2.5%), minerals (1%).");
        registerSection("206C(1G)", "TCS on LRS Foreign Remittance & Overseas Tour", "FORM_27EQ", new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("700000.00"), new BigDecimal("700000.00"), new BigDecimal("20.00"), "TCS @ 5% up to ₹7L, 20% beyond ₹7L for foreign remittance / overseas tour packages.");
        registerSection("206C(1H)", "TCS on Sale of Goods exceeding ₹50 Lakhs", "FORM_27EQ", new BigDecimal("0.10"), new BigDecimal("0.10"), new BigDecimal("5000000.00"), new BigDecimal("5000000.00"), new BigDecimal("5.00"), "0.1% on receipt exceeding ₹50 Lakhs for sellers with turnover > ₹10 Cr.");
    }

    private static void registerSection(
            String sectionCode, String title, String form,
            BigDecimal rateInd, BigDecimal rateOthers,
            BigDecimal threshold, BigDecimal singleLimit,
            BigDecimal nonPanRate, String notes) {
        SECTION_REGISTRY.put(sectionCode.toUpperCase(), TdsSectionRateDto.builder()
                .sectionCode(sectionCode)
                .title(title)
                .returnForm(form)
                .rateIndividual(rateInd)
                .rateOthers(rateOthers)
                .thresholdLimit(threshold)
                .singleTransactionLimit(singleLimit)
                .nonPanRate(nonPanRate)
                .statutoryNotes(notes)
                .build());
    }

    public List<TdsSectionRateDto> getAllSectionRates() {
        return new ArrayList<>(SECTION_REGISTRY.values());
    }

    public TdsSectionRateDto getSectionRate(String sectionCode) {
        if (sectionCode == null) return null;
        String key = sectionCode.toUpperCase().trim();
        if (SECTION_REGISTRY.containsKey(key)) {
            return SECTION_REGISTRY.get(key);
        }
        // Fallback search
        return SECTION_REGISTRY.values().stream()
                .filter(s -> s.getSectionCode().equalsIgnoreCase(key) || key.startsWith(s.getSectionCode().toUpperCase()))
                .findFirst()
                .orElse(null);
    }

    public TdsComputationResultDto computeTds(TdsComputationRequest req) {
        BigDecimal amount = req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO;
        String sectionKey = req.getSectionCode() != null ? req.getSectionCode().toUpperCase().trim() : "194C";
        TdsSectionRateDto rateConfig = getSectionRate(sectionKey);

        String title = rateConfig != null ? rateConfig.getTitle() : "Custom TDS Section " + sectionKey;
        BigDecimal baseRate;

        // 1. Determine applicable base rate
        if (req.getLowerDeductionRate() != null && req.getLowerDeductionRate().compareTo(BigDecimal.ZERO) >= 0) {
            baseRate = req.getLowerDeductionRate();
        } else if (!req.isValidPanProvided()) {
            // Sec 206AA: Higher rate of 20% or statutory rate
            baseRate = rateConfig != null ? rateConfig.getNonPanRate() : new BigDecimal("20.00");
        } else if (req.isSpecifiedNonFiler206AB()) {
            // Sec 206AB: Twice the specified rate or 5% (whichever is higher)
            BigDecimal standardRate = rateConfig != null ? (req.getDeducteeType() == DeducteeType.COMPANY ? rateConfig.getRateOthers() : rateConfig.getRateIndividual()) : new BigDecimal("10.00");
            baseRate = standardRate.multiply(new BigDecimal("2.00")).max(new BigDecimal("5.00"));
        } else if (rateConfig != null) {
            baseRate = req.getDeducteeType() == DeducteeType.COMPANY ? rateConfig.getRateOthers() : rateConfig.getRateIndividual();
        } else {
            baseRate = new BigDecimal("10.00");
        }

        // 2. Check threshold exemptions
        boolean exemptByThreshold = false;
        if (req.isValidPanProvided() && req.getLowerDeductionRate() == null && rateConfig != null) {
            BigDecimal singleLimit = rateConfig.getSingleTransactionLimit();
            BigDecimal aggregateLimit = rateConfig.getThresholdLimit();
            BigDecimal cumulative = req.getCumulativePaidInYear() != null ? req.getCumulativePaidInYear() : BigDecimal.ZERO;
            BigDecimal totalInYear = cumulative.add(amount);

            if (singleLimit.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(singleLimit) <= 0
                    && aggregateLimit.compareTo(BigDecimal.ZERO) > 0 && totalInYear.compareTo(aggregateLimit) <= 0) {
                exemptByThreshold = true;
            } else if (singleLimit.compareTo(BigDecimal.ZERO) == 0 && aggregateLimit.compareTo(BigDecimal.ZERO) > 0 && totalInYear.compareTo(aggregateLimit) <= 0) {
                exemptByThreshold = true;
            }
        }

        BigDecimal effectiveRate = exemptByThreshold ? BigDecimal.ZERO : baseRate;
        BigDecimal baseTds = amount.multiply(effectiveRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 3. Surcharge & Cess calculation
        // For domestic payments, cess is not added to resident TDS except salary/non-resident
        BigDecimal surchargeRate = BigDecimal.ZERO;
        BigDecimal surchargeAmount = BigDecimal.ZERO;
        BigDecimal cessAmount = BigDecimal.ZERO;

        if ("195".equalsIgnoreCase(sectionKey) || "192".equalsIgnoreCase(sectionKey)) {
            cessAmount = baseTds.multiply(CESS_RATE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalTdsDeducted = baseTds.add(surchargeAmount).add(cessAmount);
        BigDecimal netPayable = amount.subtract(totalTdsDeducted);

        // 4. Interest calculation under Section 201(1A)
        BigDecimal delayInDeductionInterest = BigDecimal.ZERO;
        BigDecimal delayInDepositInterest = BigDecimal.ZERO;

        if (req.getPaymentCreditDate() != null && req.getDeductionDate() != null && req.getDeductionDate().isAfter(req.getPaymentCreditDate())) {
            long months = Math.max(1, ChronoUnit.MONTHS.between(req.getPaymentCreditDate().withDayOfMonth(1), req.getDeductionDate().withDayOfMonth(1)) + 1);
            // 1% per month or part of month
            delayInDeductionInterest = totalTdsDeducted.multiply(new BigDecimal(months)).multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        }

        if (req.getDeductionDate() != null && req.getDepositDate() != null && req.getDepositDate().isAfter(req.getDeductionDate())) {
            // Due date is usually 7th of next month
            LocalDate depositDueDate = req.getDeductionDate().plusMonths(1).withDayOfMonth(7);
            if (req.getDepositDate().isAfter(depositDueDate)) {
                long months = Math.max(1, ChronoUnit.MONTHS.between(req.getDeductionDate().withDayOfMonth(1), req.getDepositDate().withDayOfMonth(1)) + 1);
                // 1.5% per month or part of month
                delayInDepositInterest = totalTdsDeducted.multiply(new BigDecimal(months)).multiply(new BigDecimal("0.015")).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalInterest = delayInDeductionInterest.add(delayInDepositInterest);

        // 5. Late filing fee calculation under Section 234E
        int delayDays = 0;
        BigDecimal lateFee = BigDecimal.ZERO;
        if (req.getFilingDueDate() != null && req.getActualFilingDate() != null && req.getActualFilingDate().isAfter(req.getFilingDueDate())) {
            delayDays = (int) ChronoUnit.DAYS.between(req.getFilingDueDate(), req.getActualFilingDate());
            BigDecimal calculatedFee = LATE_FEE_PER_DAY.multiply(new BigDecimal(delayDays));
            // Fee cannot exceed total TDS amount
            lateFee = calculatedFee.min(totalTdsDeducted);
        }

        BigDecimal totalPayableWithPenalties = totalTdsDeducted.add(totalInterest).add(lateFee);

        StringBuilder remarks = new StringBuilder();
        if (exemptByThreshold) {
            remarks.append("Payment is within the statutory exemption threshold limit. No TDS is deductible.");
        } else if (!req.isValidPanProvided()) {
            remarks.append("Higher deduction under Section 206AA applied @ 20% due to absent or invalid PAN.");
        } else if (req.isSpecifiedNonFiler206AB()) {
            remarks.append("Higher deduction under Section 206AB applied for specified non-filer of ITR.");
        } else if (req.getLowerDeductionRate() != null) {
            remarks.append("Lower deduction certificate under Section 197 applied @ ").append(req.getLowerDeductionRate()).append("%.");
        } else {
            remarks.append("Standard statutory rate of ").append(effectiveRate).append("% applied.");
        }

        return TdsComputationResultDto.builder()
                .sectionCode(sectionKey)
                .sectionTitle(title)
                .grossAmount(amount)
                .thresholdExemptionApplicable(exemptByThreshold)
                .effectiveRate(effectiveRate)
                .baseTdsAmount(baseTds)
                .surchargeRate(surchargeRate)
                .surchargeAmount(surchargeAmount)
                .cessAmount(cessAmount)
                .totalTaxDeducted(totalTdsDeducted)
                .netPayableToDeductee(netPayable)
                .delayInDeductionInterest(delayInDeductionInterest)
                .delayInDepositInterest(delayInDepositInterest)
                .totalInterest(totalInterest)
                .delayDays(delayDays)
                .lateFee234E(lateFee)
                .totalPayableWithPenalties(totalPayableWithPenalties)
                .remarks(remarks.toString())
                .build();
    }
}
