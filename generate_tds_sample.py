import csv
import os

def create_sample_tds_files():
    # 1. Sample TDS Profiles CSV
    profiles_data = [
        ["TAN", "Client Name", "PAN", "Deductor Type", "Responsible Person", "Officer PAN", "Designation", "Email", "Mobile"],
        ["BLRP12345A", "Acme Corporation Pvt Ltd", "AABCA1234K", "COMPANY", "Rajesh Sharma", "ABCPS9876K", "Managing Director", "tax@acme.com", "9876543210"],
        ["DELC98765B", "Apex Direct Solutions LLP", "AACCA9876L", "LLP", "Amit Verma", "VERPA1234M", "Designated Partner", "accounts@apexdirect.in", "9812345678"],
        ["MUMP45678C", "Sunil & Associates", "AADFS4567N", "FIRM", "Sunil Mehta", "MEHTS5678P", "Senior Partner", "sunil@sunilassociates.com", "9823456789"],
        ["HYDA23456D", "TechNova Systems India Ltd", "AABCT5678G", "COMPANY", "Venkatesh Rao", "RAOPV1234H", "Director Finance", "finance@technova.io", "9849012345"],
        ["PUNC34567E", "Kulkarni Engineering Works", "AAEFK3456J", "INDIVIDUAL_HUF", "Anand Kulkarni", "KULPA9876K", "Proprietor", "anand@kulkarniengg.com", "9850123456"]
    ]

    with open("sample_tds_profiles.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerows(profiles_data)
    print("Created sample_tds_profiles.csv")

    # 2. Sample TDS Returns CSV
    returns_data = [
        ["TAN", "Client Name", "Form", "Quarter", "Financial Year", "Assessment Year", "Due Date", "Status", "Token Number", "Filing Date", "TDS Deducted", "TDS Deposited"],
        ["BLRP12345A", "Acme Corporation Pvt Ltd", "26Q", "Q1", "2026-27", "2027-28", "2026-07-31", "FILED", "010020304050601", "2026-07-28", 45000, 45000],
        ["BLRP12345A", "Acme Corporation Pvt Ltd", "24Q", "Q1", "2026-27", "2027-28", "2026-07-31", "FILED", "010020304050602", "2026-07-29", 120000, 120000],
        ["DELC98765B", "Apex Direct Solutions LLP", "26Q", "Q1", "2026-27", "2027-28", "2026-07-31", "PENDING", "", "", 18500, 18500],
        ["MUMP45678C", "Sunil & Associates", "26Q", "Q1", "2026-27", "2027-28", "2026-07-31", "DRAFT", "", "", 32000, 32000],
        ["HYDA23456D", "TechNova Systems India Ltd", "26Q", "Q1", "2026-27", "2027-28", "2026-07-31", "FILED", "010020304050603", "2026-07-30", 85000, 85000],
        ["PUNC34567E", "Kulkarni Engineering Works", "26Q", "Q1", "2026-27", "2027-28", "2026-07-31", "UNDER_REVIEW", "", "", 12500, 12500]
    ]

    with open("sample_tds_returns.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerows(returns_data)
    print("Created sample_tds_returns.csv")

if __name__ == "__main__":
    create_sample_tds_files()
