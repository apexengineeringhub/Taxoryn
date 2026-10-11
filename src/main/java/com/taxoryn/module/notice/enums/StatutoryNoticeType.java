package com.taxoryn.module.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum StatutoryNoticeType {

    // --- Income Tax Department (ITD) ---
    ITD_143_1(NoticeDepartment.INCOME_TAX, "143(1)", "143(1) - Summary Intimation", "Summary assessment intimation regarding tax computation differences, refunds, or demand."),
    ITD_139_9(NoticeDepartment.INCOME_TAX, "139(9)", "139(9) - Defective Return Notice", "Defective return notice requiring rectification within 15 days."),
    ITD_142_1(NoticeDepartment.INCOME_TAX, "142(1)", "142(1) - Inquiry Before Assessment", "Preliminary questionnaire or requisition of accounts, documents, or return filing."),
    ITD_143_2(NoticeDepartment.INCOME_TAX, "143(2)", "143(2) - Scrutiny Assessment Notice", "Detailed scrutiny notice issued by Assessing Officer / Faceless Assessment Unit."),
    ITD_148(NoticeDepartment.INCOME_TAX, "148", "148 - Income Escaping Assessment", "Reassessment notice for income escaping assessment u/s 147/148."),
    ITD_148A(NoticeDepartment.INCOME_TAX, "148A", "148A - Show Cause for Reassessment", "Show cause notice prior to issuing section 148 reassessment notice."),
    ITD_156(NoticeDepartment.INCOME_TAX, "156", "156 - Notice of Demand", "Notice demanding payment of tax, interest, penalty, fine or other sum."),
    ITD_245(NoticeDepartment.INCOME_TAX, "245", "245 - Intimation for Set-off of Refund", "Proposed adjustment of tax refund against outstanding demand from earlier years."),
    ITD_271_1_C(NoticeDepartment.INCOME_TAX, "271(1)(c)", "271(1)(c) - Penalty for Concealment", "Penalty notice for concealment of particulars of income or furnishing inaccurate particulars."),
    ITD_270A(NoticeDepartment.INCOME_TAX, "270A", "270A - Penalty for Under-reporting", "Penalty notice for under-reporting and misreporting of income."),

    // --- GST Department ---
    GST_ASMT_10(NoticeDepartment.GST, "ASMT-10", "ASMT-10 - Scrutiny of Returns", "Discrepancy notice issued during scrutiny of returns under section 61."),
    GST_DRC_01(NoticeDepartment.GST, "DRC-01", "DRC-01 - Show Cause Notice (SCN)", "Summary of show cause notice for demand of unpaid/short-paid tax under section 73/74."),
    GST_DRC_01A(NoticeDepartment.GST, "DRC-01A", "DRC-01A - Pre-SCN Consultation", "Intimation of tax ascertained as being payable prior to issuance of SCN u/s 73/74."),
    GST_DRC_07(NoticeDepartment.GST, "DRC-07", "DRC-07 - Summary of Order", "Summary of adjudication order raising statutory tax, interest, and penalty demand."),
    GST_GSTR_3A(NoticeDepartment.GST, "GSTR-3A", "GSTR-3A - Notice to Non-filers", "Notice issued to registered person failing to file monthly/quarterly GST returns."),
    GST_CMP_05(NoticeDepartment.GST, "CMP-05", "CMP-05 - SCN for Composition Scheme", "Show cause notice proposing to deny option to pay tax under composition levy."),
    GST_REG_17(NoticeDepartment.GST, "REG-17", "REG-17 - SCN for Cancellation of Registration", "Show cause notice for cancellation of GST registration."),
    GST_REG_23(NoticeDepartment.GST, "REG-23", "REG-23 - SCN for Rejection of Revocation", "Show cause notice for rejection of application for revocation of cancellation of registration."),
    GST_DRC_13(NoticeDepartment.GST, "DRC-13", "DRC-13 - Notice of Recovery from Third Person", "Garnishee notice issued to bank or debtor under section 79."),

    // --- TDS / TRACES ---
    TDS_200A(NoticeDepartment.TDS, "200A", "200A - TDS Return Processing Intimation", "Intimation on processing of quarterly TDS statement with defaults, late filing fee, or interest."),
    TDS_201_1(NoticeDepartment.TDS, "201(1)", "201(1) - Assessee in Default SCN", "Notice treating deductor as assessee in default for failure to deduct or pay TDS."),
    TDS_201_1A(NoticeDepartment.TDS, "201(1A)", "201(1A) - Interest on Late Deduction/Deposit", "Demand notice for mandatory interest on delayed deduction or late deposit of TDS."),
    TDS_234E(NoticeDepartment.TDS, "234E", "234E - Fee for Default in TDS Statement", "Notice for statutory late filing fee of Rs. 200 per day for delayed filing of TDS statements."),
    TDS_SHORT_DEDUCTION(NoticeDepartment.TDS, "Short Deduction", "TRACES - Short Deduction Notice", "Notice generated from TRACES portal identifying PAN errors, higher rates u/s 206AA, or 206AB."),
    TDS_SHORT_PAYMENT(NoticeDepartment.TDS, "Short Payment", "TRACES - Short Payment Notice", "Notice identifying unmatched challans or insufficient tax deposited against deducted tax."),

    // --- Customs & Central Excise ---
    CUSTOMS_SCN(NoticeDepartment.CUSTOMS, "Customs SCN", "Customs - Show Cause Notice u/s 28", "Show cause notice for duty not levied, short-levied, or erroneously refunded."),
    CUSTOMS_SEIZURE(NoticeDepartment.CUSTOMS, "Customs Seizure", "Customs - Seizure Memo u/s 110", "Seizure or confiscation memo issued for goods under section 110/111/112."),
    CUSTOMS_SUMMONS(NoticeDepartment.CUSTOMS, "Customs Summons", "Customs - Summons u/s 108", "Summons to produce documents or give evidence under section 108."),

    // --- Other Statutory / Miscellaneous ---
    OTHER_SUMMONS(NoticeDepartment.OTHER, "Statutory Summons", "Statutory Summons / General Requisition", "General statutory appearance or document production requisition."),
    OTHER_AUDIT(NoticeDepartment.OTHER, "Statutory Audit", "Special / Statutory Audit Notice", "Notice directing special audit or verification by appointed auditor."),
    OTHER_MISC(NoticeDepartment.OTHER, "General Notice", "Miscellaneous Statutory Notice", "Other statutory inquiry, notice, or legal correspondence.");

    private final NoticeDepartment department;
    private final String defaultSection;
    private final String label;
    private final String description;

    public static List<StatutoryNoticeType> getByDepartment(NoticeDepartment department) {
        if (department == null) {
            return Arrays.asList(values());
        }
        return Arrays.stream(values())
                .filter(t -> t.getDepartment() == department)
                .toList();
    }

    public static boolean isValidForDepartment(NoticeDepartment department, String noticeTypeCodeOrLabel) {
        if (noticeTypeCodeOrLabel == null || noticeTypeCodeOrLabel.isBlank()) {
            return false;
        }
        if (department == null || department == NoticeDepartment.OTHER) {
            return true;
        }
        String clean = noticeTypeCodeOrLabel.trim().toLowerCase();

        // 1. Direct match with current department
        List<StatutoryNoticeType> allowed = getByDepartment(department);
        for (StatutoryNoticeType type : allowed) {
            if (type.name().equalsIgnoreCase(clean)
                    || type.getLabel().equalsIgnoreCase(clean)
                    || type.getDefaultSection().equalsIgnoreCase(clean)
                    || clean.contains(type.getDefaultSection().toLowerCase())
                    || type.getLabel().toLowerCase().contains(clean)) {
                return true;
            }
        }

        // 2. Check if it explicitly belongs to another distinct department (cross-department incompatibility check)
        for (StatutoryNoticeType other : values()) {
            if (other.getDepartment() != department && other.getDepartment() != NoticeDepartment.OTHER) {
                String otherSec = other.getDefaultSection().toLowerCase();
                String otherLabel = other.getLabel().toLowerCase();
                if (other.name().equalsIgnoreCase(clean)
                        || otherLabel.equalsIgnoreCase(clean)
                        || clean.startsWith(otherSec)
                        || clean.contains(otherSec)
                        || clean.contains(other.name().toLowerCase())) {
                    return false; // Specifically matches another department, so invalid for this department
                }
            }
        }

        // 3. Check department prefix keywords: e.g. "GST" under INCOME_TAX or "ITD"/"Income Tax" under GST
        if (department != NoticeDepartment.GST && (clean.contains("gst") || clean.startsWith("drc-") || clean.startsWith("asmt-") || clean.startsWith("gstr-"))) {
            return false;
        }
        if (department != NoticeDepartment.INCOME_TAX && (clean.contains("income tax") || clean.startsWith("itd_") || clean.startsWith("143(") || clean.startsWith("148") || clean.startsWith("139("))) {
            return false;
        }
        if (department != NoticeDepartment.TDS && (clean.contains("tds") || clean.contains("traces") || clean.startsWith("200a") || clean.startsWith("201("))) {
            return false;
        }
        if (department != NoticeDepartment.CUSTOMS && clean.contains("customs")) {
            return false;
        }

        // 4. Permissive for general custom/legacy strings as long as not conflicting with another department
        return true;
    }
}
