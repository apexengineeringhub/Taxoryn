import os
import csv

public_dir = os.path.join(os.path.dirname(__file__), "..", "frontend", "public")
os.makedirs(public_dir, exist_ok=True)

# 1. Sample ITR Client Profiles Migration Data
profiles_data = [
    {
        "PAN": "AAACZ1234D",
        "Client Name": "Apex Engineering Solutions Pvt Ltd",
        "Taxpayer Category": "COMPANY",
        "Default ITR Form": "ITR_6",
        "Residential Status": "RESIDENT",
        "Email": "tax@apexengineering.com",
        "Phone": "9876543210",
        "Constitution": "PRIVATE_LIMITED"
    },
    {
        "PAN": "AABFA1234F",
        "Client Name": "MAA MUNDESHWARI TAX CONSULTANCY",
        "Taxpayer Category": "FIRM",
        "Default ITR Form": "ITR_5",
        "Residential Status": "RESIDENT",
        "Email": "pawanadv@gmail.com",
        "Phone": "9876500001",
        "Constitution": "PARTNERSHIP"
    },
    {
        "PAN": "ABCPJ9876M",
        "Client Name": "Pawan Pathak & Associates",
        "Taxpayer Category": "INDIVIDUAL",
        "Default ITR Form": "ITR_4",
        "Residential Status": "RESIDENT",
        "Email": "info@pawanpathak.com",
        "Phone": "9876500002",
        "Constitution": "PROPRIETORSHIP"
    },
    {
        "PAN": "AABCM5678K",
        "Client Name": "Zenith Infotech Private Limited",
        "Taxpayer Category": "COMPANY",
        "Default ITR Form": "ITR_6",
        "Residential Status": "RESIDENT",
        "Email": "finance@zenithtech.in",
        "Phone": "9876500003",
        "Constitution": "PRIVATE_LIMITED"
    },
    {
        "PAN": "AAACS2345P",
        "Client Name": "Skyline Logistics LLP",
        "Taxpayer Category": "LLP",
        "Default ITR Form": "ITR_5",
        "Residential Status": "RESIDENT",
        "Email": "accounts@skylinelogistics.in",
        "Phone": "9876500004",
        "Constitution": "LLP"
    },
    {
        "PAN": "BKRPK8899L",
        "Client Name": "Dr. Rajesh Kumar Sharma",
        "Taxpayer Category": "INDIVIDUAL",
        "Default ITR Form": "ITR_1",
        "Residential Status": "RESIDENT",
        "Email": "dr.rajesh@gmail.com",
        "Phone": "9876500005",
        "Constitution": "INDIVIDUAL"
    },
    {
        "PAN": "CGTPV4455Q",
        "Client Name": "Vikram Malhotra (Capital Gains)",
        "Taxpayer Category": "INDIVIDUAL",
        "Default ITR Form": "ITR_2",
        "Residential Status": "RESIDENT",
        "Email": "vikram.m@outlook.com",
        "Phone": "9876500006",
        "Constitution": "INDIVIDUAL"
    },
    {
        "PAN": "AAATH1122R",
        "Client Name": "Heritage Educational Trust",
        "Taxpayer Category": "TRUST",
        "Default ITR Form": "ITR_7",
        "Residential Status": "RESIDENT",
        "Email": "trustee@heritagetrust.org",
        "Phone": "9876500007",
        "Constitution": "TRUST"
    }
]

profiles_csv_path = os.path.join(public_dir, "sample_itr_profiles_migration.csv")
with open(profiles_csv_path, mode="w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=list(profiles_data[0].keys()))
    writer.writeheader()
    writer.writerows(profiles_data)
print(f"Created {profiles_csv_path}")

# 2. Sample Historical ITR Returns Migration Data
returns_data = [
    {
        "PAN": "AAACZ1234D",
        "Client Name": "Apex Engineering Solutions Pvt Ltd",
        "Assessment Year": "2026-27",
        "Financial Year": "2025-26",
        "ITR Form": "ITR_6",
        "Filing Status": "FILED",
        "ITR-V Ack Number": "123456789012345",
        "Filing Date": "2026-07-28",
        "Due Date": "2026-10-31",
        "Notes": "Audited Corporate return e-filed and e-verified"
    },
    {
        "PAN": "AABFA1234F",
        "Client Name": "MAA MUNDESHWARI TAX CONSULTANCY",
        "Assessment Year": "2026-27",
        "Financial Year": "2025-26",
        "ITR Form": "ITR_5",
        "Filing Status": "FILED",
        "ITR-V Ack Number": "234567890123456",
        "Filing Date": "2026-07-25",
        "Due Date": "2026-07-31",
        "Notes": "Partnership firm return filed via DSC"
    },
    {
        "PAN": "ABCPJ9876M",
        "Client Name": "Pawan Pathak & Associates",
        "Assessment Year": "2026-27",
        "Financial Year": "2025-26",
        "ITR Form": "ITR_4",
        "Filing Status": "COMPLETED",
        "ITR-V Ack Number": "345678901234567",
        "Filing Date": "2026-07-20",
        "Due Date": "2026-07-31",
        "Notes": "Section 44AD presumptive taxation return completed"
    },
    {
        "PAN": "AABCM5678K",
        "Client Name": "Zenith Infotech Private Limited",
        "Assessment Year": "2025-26",
        "Financial Year": "2024-25",
        "ITR Form": "ITR_6",
        "Filing Status": "COMPLETED",
        "ITR-V Ack Number": "456789012345678",
        "Filing Date": "2025-10-29",
        "Due Date": "2025-10-31",
        "Notes": "Prior AY company return processed by CPC"
    },
    {
        "PAN": "AAACS2345P",
        "Client Name": "Skyline Logistics LLP",
        "Assessment Year": "2026-27",
        "Financial Year": "2025-26",
        "ITR Form": "ITR_5",
        "Filing Status": "READY_TO_FILE",
        "ITR-V Ack Number": "",
        "Filing Date": "",
        "Due Date": "2026-07-31",
        "Notes": "Computation vetted and ready for client sign-off"
    },
    {
        "PAN": "BKRPK8899L",
        "Client Name": "Dr. Rajesh Kumar Sharma",
        "Assessment Year": "2026-27",
        "Financial Year": "2025-26",
        "ITR Form": "ITR_1",
        "Filing Status": "FILED",
        "ITR-V Ack Number": "567890123456789",
        "Filing Date": "2026-07-15",
        "Due Date": "2026-07-31",
        "Notes": "Salary & Interest income Sahaj return filed"
    }
]

returns_csv_path = os.path.join(public_dir, "sample_itr_returns_migration.csv")
with open(returns_csv_path, mode="w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=list(returns_data[0].keys()))
    writer.writeheader()
    writer.writerows(returns_data)
print(f"Created {returns_csv_path}")
