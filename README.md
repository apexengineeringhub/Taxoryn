# Taxoryn — Enterprise Multi-Tenant Tax Practice Management Platform

Taxoryn is a high-performance, modular monolith application built with **Java 21**, **Spring Boot 3.3**, and **PostgreSQL 16**. It is architected from day one with strict multi-tenancy isolation, layered domain modularity, and zero-trust tenant security.

---

## 🏛 Architecture Overview

Taxoryn is organized as a **Modular Monolith** with clean domain-driven boundaries and clear layered responsibilities.

```
com.taxoryn
├── core                        # Shared architectural foundation
│   ├── config                  # Spring Security 6, OpenAPI 3, JPA Auditing, WebMvc
│   ├── domain                  # BaseEntity, AuditableEntity, TenantAuditableEntity
│   ├── dto                     # PageRequestDto, pagination helpers
│   ├── exception               # GlobalExceptionHandler, ErrorCode, AppException hierarchy
│   ├── filter                  # MdcLoggingFilter (Correlation ID), RequestLoggingFilter
│   ├── response                # ApiResponse<T>, PagedResponse<T>, ErrorResponse, ValidationError
│   └── security                # JwtTokenProvider, JwtAuthenticationFilter, TenantContext, SecurityUser, TokenBlacklistService, CustomUserDetailsService
│
└── module                      # Domain-specific business modules
    ├── authentication          # AuthController, AuthService, Login, Refresh, Me, Logout, Team Registration
    ├── organization            # Organization & Settings lifecycle (CRUD, Status, Settings)
    ├── user                    # User management & credentials
    ├── role                    # Role & Permission RBAC (System & Custom Tenant Roles)
    ├── employee                # Employee Management, Hierarchy, Workload Analytics
    ├── client                  # Client master (Individual, LLP, Corporate, etc.)
    ├── task                    # Task & workflow management
    ├── audit                   # Immutable audit log trail
    ├── gst                     # GST returns filing (Foundation)
    ├── itr                     # Income Tax returns computation (Foundation)
    ├── document                # Secure multi-tenant document vault (Foundation)
    ├── compliance              # Statutory compliance calendar (Foundation)
    ├── billing                 # Invoicing & fee management (Foundation)
    └── notification            # Multi-channel alerts (Foundation)
```

---

## 🗄 Database Scripts & Dev Setup

### 1. Database Provisioning & Scripts
Located under the [`scripts/`](file:///d:/Projects/Taxoryn/scripts) directory:
- **[`init-db.sql`](file:///d:/Projects/Taxoryn/scripts/init-db.sql)**: Complete consolidated PostgreSQL 16 DDL schema with extensions, constraints, tables, indexes, permissions catalog, and default system roles.
- **[`seed-dev-data.sql`](file:///d:/Projects/Taxoryn/scripts/seed-dev-data.sql)**: Pre-populated demo tenant ("Apex Tax Advisors LLP") with admin, manager, tax professional, accountant, employees, clients, and workflow tasks.
- **[`setup-db.ps1`](file:///d:/Projects/Taxoryn/scripts/setup-db.ps1)**: One-click PowerShell tool for Windows.
- **[`setup-db.sh`](file:///d:/Projects/Taxoryn/scripts/setup-db.sh)**: One-click Bash tool for Linux / macOS / WSL.

### 2. Automated DB Provisioning Commands
```powershell
# Windows PowerShell (Provision DB + Seed Demo Data)
.\scripts\setup-db.ps1 -SeedData

# Windows PowerShell (Reset and rebuild DB from scratch)
.\scripts\setup-db.ps1 -Reset -SeedData
```

```bash
# Linux / macOS / WSL
chmod +x scripts/setup-db.sh
./scripts/setup-db.sh --seed
```

### 3. Demo Credentials (from `seed-dev-data.sql`)
| Role | Email | Password |
|---|---|---|
| **Org Admin** | `admin@apextax.com` | `Password123!` |
| **Practice Manager** | `manager@apextax.com` | `Password123!` |
| **Senior Tax Pro** | `taxpro@apextax.com` | `Password123!` |
| **Staff Accountant** | `accountant@apextax.com` | `Password123!` |
| **Firm Staff** | `staff@apextax.com` | `Password123!` |

---

## 👥 Organization-Level RBAC

| Role Code | Role Name | Scope & Responsibilities |
|---|---|---|
| `SUPER_ADMIN` | Platform Super Administrator | Full platform administrative access across all tenants |
| `ORG_ADMIN` | Organization Administrator | Full administrative authority within the tenant organization |
| `MANAGER` | Practice Manager | Manages clients, workflows, task assignments, GST/ITR review, and billing |
| `TAX_PROFESSIONAL` | Senior Tax Practitioner | Prepares GST and ITR returns, manages client filings, documents, and tasks |
| `ACCOUNTANT` | Staff Accountant | Prepares GST filings, invoices, receipts, and client data entry |
| `EMPLOYEE` | Firm Employee / Staff | Executes assigned tasks, reviews documents, and views client work |
| `VIEWER` | Read-Only Viewer | Read-only viewing permissions across all practice modules |
| `CLIENT_ADMIN` | Client Administrator | Primary client contact with full access to client portal, documents, profile, and client users |
| `CLIENT_USER` | Client Staff User | Client staff with access to view compliance status and upload documents |

---

### Client Portal Endpoints (`/api/portal` and `/api/v1/portal`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/portal/users` | Register client portal user linked to client record | `ORG_ADMIN` / `CLIENT_ADMIN` |
| `GET` | `/api/v1/portal/dashboard` | **Client Compliance Dashboard** (overview, pending docs, tasks, practitioner info) | `CLIENT_PORTAL_ACCESS` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/profile` | Client profile details | `CLIENT_PORTAL_PROFILE_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `PUT` | `/api/v1/portal/profile` | Update client contact & address | `CLIENT_PORTAL_PROFILE_UPDATE` / `CLIENT_ADMIN` |
| `GET` | `/api/v1/portal/gst-status` | Client GST return filing statuses & ARNs | `CLIENT_PORTAL_STATUS_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/itr-status` | Client ITR filing statuses & ACK numbers | `CLIENT_PORTAL_STATUS_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/documents` | Client private document vault | `CLIENT_PORTAL_DOCUMENT_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/pending-documents` | Pending document requests checklist | `CLIENT_PORTAL_DOCUMENT_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `POST` | `/api/v1/portal/documents/upload` | Upload document to client vault & fulfill request | `CLIENT_PORTAL_DOCUMENT_UPLOAD` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/documents/{id}/download` | Download client's own document | `CLIENT_PORTAL_DOCUMENT_VIEW` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `POST` | `/api/v1/portal/document-requests` | Consultant requests compliance document from client | `CLIENT_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/portal/invoices` | Client views issued invoices and payment receipts | `CLIENT_PORTAL_ACCESS` / `CLIENT_ADMIN` / `CLIENT_USER` |
| `GET` | `/api/v1/portal/invoices/{id}` | Client views full invoice details and line items | `CLIENT_PORTAL_ACCESS` / `CLIENT_ADMIN` / `CLIENT_USER` |

### Client Billing & Invoicing Endpoints (`/api/invoices`, `/api/v1/invoices`, `/api/v1/billing`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/invoices` | Create professional invoice with multi-service line items (GST, ITR, TDS, Accounting, Consulting) | `BILLING_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/invoices` | List & filter invoices with pagination (status, client, date range) | `BILLING_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/invoices/{id}` | Get invoice details, line items, and payment receipts | `BILLING_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/invoices/{id}` | Update draft invoice (line items, pricing, due date) | `BILLING_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/invoices/{id}/issue` | Issue invoice to client and transition from DRAFT to ISSUED | `BILLING_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/invoices/{id}/cancel` | Cancel invoice | `BILLING_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/invoices/{id}/payments` | **Record Payment Receipt** (auto-updates balance due & status to `PAID` / `PARTIALLY_PAID`) | `BILLING_CREATE` / `BILLING_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/invoices/{id}/payments` | List payment receipts for invoice | `BILLING_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/billing/clients/{clientId}/history` | **Client Billing History & Outstanding Summary** (total billed, total paid, balance, invoices) | `BILLING_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/billing/dashboard/stats` | **Billing Executive Dashboard** (Total Billed, Total Collected, Total Outstanding, Service Breakdown) | `BILLING_VIEW` / `ORG_ADMIN` |

### Document Management Endpoints (`/api/documents` and `/api/v1/documents`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/documents/upload` | Multipart file upload with metadata (Client, GST, ITR, Task) | `DOCUMENT_UPLOAD` / `ORG_ADMIN` |
| `GET` | `/api/v1/documents/{id}/download` | Stream binary document file with MIME headers | `DOCUMENT_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/documents/{id}` | Get document metadata by ID | `DOCUMENT_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/documents` | List & filter documents with pagination | `DOCUMENT_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/documents/clients/{clientId}` | **Client Document Vault** (all active client files) | `DOCUMENT_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/documents/{id}` | Update document metadata, tags, and notes | `DOCUMENT_UPLOAD` / `ORG_ADMIN` |
| `DELETE` | `/api/v1/documents/{id}` | Delete document and purge from storage | `DOCUMENT_DELETE` / `ORG_ADMIN` |

### Compliance Calendar Endpoints (`/api/compliance` and `/api/v1/compliance`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `GET` | `/api/v1/compliance/calendar` | List & filter calendar compliance obligations with pagination | `TASK_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/upcoming` | List upcoming compliance obligations (next N days) | `TASK_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/overdue` | List overdue compliance obligations | `TASK_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/today` | List compliance obligations due today | `TASK_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/dashboard/stats` | **Compliance Executive Dashboard Metrics** (Due Today, Due This Week, Overdue, Completed, Type Breakdown) | `TASK_VIEW` / `ORG_ADMIN` |
| `POST` | `/api/v1/compliance/obligations` | Create custom compliance obligation | `TASK_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/obligations/{id}` | Get compliance obligation details | `TASK_VIEW` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/compliance/obligations/{id}/status` | Update obligation status (`COMPLETED`, `IN_PROGRESS`, etc.) | `TASK_UPDATE` / `ORG_ADMIN` |
| `PUT` | `/api/v1/compliance/obligations/{id}/assigned-employee` | Assign / Reassign practitioner to obligation | `TASK_ASSIGN` / `ORG_ADMIN` |
| `POST` | `/api/v1/compliance/obligations/{id}/create-task` | Convert/link compliance obligation to actionable Task | `TASK_CREATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/compliance/generate` | Batch generate compliance obligations from rules for period | `TASK_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/compliance/rules` | List active configurable compliance rules | `TASK_VIEW` / `ORG_ADMIN` |
| `POST` | `/api/v1/compliance/rules` | Create custom configurable compliance rule | `ORG_ADMIN` / `SUPER_ADMIN` |

### ITR Management Endpoints (`/api/itr` and `/api/v1/itr`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/itr/profiles` | Create ITR profile for client | `ITR_CREATE` / `ORG_ADMIN` |
| `PUT` | `/api/v1/itr/profiles/{id}` | Update ITR profile | `ITR_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/profiles/{id}` | Get ITR profile by ID | `ITR_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/profiles/clients/{clientId}` | Get ITR profile for client | `ITR_VIEW` / `ORG_ADMIN` |
| `POST` | `/api/v1/itr/returns` | Create ITR return filing record (AY, FY, ITR Form) | `ITR_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/returns` | List & search ITR returns with filters & pagination | `ITR_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/returns/{id}` | Get ITR return details by ID | `ITR_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/itr/returns/{id}` | Update ITR return record | `ITR_UPDATE` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/itr/returns/{id}/status` | Update workflow status (`DOCUMENTS_PENDING` -> `DATA_ENTRY` -> `UNDER_REVIEW` -> `READY_TO_FILE` -> `FILED` -> `VERIFICATION_PENDING` -> `COMPLETED`) | `ITR_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/itr/returns/{id}/filing-details` | Record e-filing submission date & ACK/ITR-V number | `ITR_UPDATE` / `ORG_ADMIN` |
| `PUT` | `/api/v1/itr/returns/{id}/assigned-employee` | Assign / Reassign practitioner employee | `ITR_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/returns/upcoming` | List upcoming ITR returns approaching due date | `ITR_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/returns/overdue` | List overdue ITR returns past due date | `ITR_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/clients/{clientId}/history` | Client ITR filing history across assessment years | `ITR_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/itr/dashboard/workload` | **ITR Practice & Employee Workload Dashboard** | `ITR_VIEW` / `ORG_ADMIN` |

### GST Management Endpoints (`/api/gst` and `/api/v1/gst`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/gst/profiles` | Register GST profile / GSTIN for client | `GST_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/profiles` | List & search GST profiles with filters | `GST_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/profiles/{id}` | Get GST profile details | `GST_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/gst/profiles/{id}` | Update GST profile & assigned practitioner | `GST_UPDATE` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/gst/profiles/{id}/status` | Update GST profile status (`ACTIVE`, `SUSPENDED`, etc.) | `GST_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/gst/filings` | Create / schedule return filing (GSTR-1, GSTR-3B, GSTR-9, CMP-08) | `GST_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/filings` | List & filter filings by return type, period, and status | `GST_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/filings/{id}` | Get filing details by ID | `GST_VIEW` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/gst/filings/{id}/status` | Update filing status (`FILED`, etc.) & record ARN | `GST_UPDATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/gst/filings/batch-generate` | Batch auto-generate return filings for all active practice clients | `GST_CREATE` / `ORG_ADMIN` |
| `POST` | `/api/v1/gst/summaries` | Save monthly sales, purchase, ITC, and liability computation | `GST_CREATE` / `GST_UPDATE` |
| `GET` | `/api/v1/gst/summaries` | Get monthly computation summary | `GST_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/dashboard/workload` | **GST Practice & Employee Workload Dashboard** | `GST_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/gst/clients/{clientId}/history` | Client GST return filing history | `GST_VIEW` / `ORG_ADMIN` |

### Client Management 360 Endpoints (`/api/clients` and `/api/v1/clients`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/clients` | Create client across 9 constitution types | `CLIENT_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/clients` | List & search clients with filters & pagination | `CLIENT_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/clients/{id}` | Get client details by ID | `CLIENT_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/clients/{id}` | Update client profile & tax numbers | `CLIENT_UPDATE` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/clients/{id}/status` | Update client status (`ACTIVE`, `INACTIVE`, `PROSPECT`, `ARCHIVED`) | `CLIENT_UPDATE` / `ORG_ADMIN` |
| `PUT` | `/api/v1/clients/{id}/assigned-employee` | Assign / Reassign practitioner employee | `CLIENT_UPDATE` / `ORG_ADMIN` |
| `DELETE` | `/api/v1/clients/{id}` | Archive client | `CLIENT_DELETE` / `ORG_ADMIN` |
| `GET` | `/api/v1/clients/{id}/overview` | **Client 360-Degree Unified Overview Dashboard** | `CLIENT_VIEW` / `ORG_ADMIN` |
| `POST` | `/api/v1/clients/{id}/notes` | Add communication note (Call, Meeting, Follow-up) | `CLIENT_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/clients/{id}/notes` | List communication history & notes | `CLIENT_VIEW` / `ORG_ADMIN` |

### Employee Endpoints (`/api/employees` and `/api/v1/employees`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/v1/employees` | Create employee record | `EMPLOYEE_CREATE` / `ORG_ADMIN` |
| `GET` | `/api/v1/employees` | List & search employees with filters & pagination | `EMPLOYEE_VIEW` / `ORG_ADMIN` |
| `GET` | `/api/v1/employees/{id}` | Get employee details by ID | `EMPLOYEE_VIEW` / `ORG_ADMIN` |
| `PUT` | `/api/v1/employees/{id}` | Update employee profile & manager | `EMPLOYEE_UPDATE` / `ORG_ADMIN` |
| `PATCH` | `/api/v1/employees/{id}/status` | Update employment status (`ACTIVE`, `INACTIVE`, etc.) | `EMPLOYEE_UPDATE` / `ORG_ADMIN` |
| `DELETE` | `/api/v1/employees/{id}` | Deactivate / Terminate employee | `EMPLOYEE_UPDATE` / `ORG_ADMIN` |
| `GET` | `/api/employees/{id}/workload` | Get employee task workload analytics | `EMPLOYEE_VIEW` / `TASK_VIEW` |

### Roles & RBAC Endpoints (`/api/v1/roles`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `GET` | `/api/v1/roles` | List all available roles (System + Custom) | `ROLE_READ` / `ORG_ADMIN` |
| `GET` | `/api/v1/roles/{roleId}` | Get role details by ID | `ROLE_READ` / `ORG_ADMIN` |
| `GET` | `/api/v1/roles/permissions` | List all standard system permissions | `ROLE_READ` / `ORG_ADMIN` |
| `POST` | `/api/v1/roles` | Create custom tenant role | `ROLE_WRITE` / `ORG_ADMIN` |
| `PUT` | `/api/v1/roles/{roleId}` | Update custom tenant role (System roles protected) | `ROLE_WRITE` / `ORG_ADMIN` |
| `DELETE` | `/api/v1/roles/{roleId}` | Delete custom tenant role (System roles protected) | `ROLE_WRITE` / `ORG_ADMIN` |
| `GET` | `/api/v1/roles/users/{userId}` | Get assigned roles and effective permissions | `USER_VIEW` / `ROLE_READ` |
| `PUT` | `/api/v1/roles/users/{userId}` | Assign roles to a user | `USER_UPDATE` / `ROLE_WRITE` |
| `DELETE` | `/api/v1/roles/users/{userId}/{roleId}` | Remove a role from a user | `USER_UPDATE` / `ROLE_WRITE` |

### Authentication Endpoints (`/api/auth`)
| HTTP Method | Endpoint | Description | Security / Role |
|---|---|---|---|
| `POST` | `/api/auth/register-organization` | Onboard new organization + primary admin | Public |
| `POST` | `/api/auth/login` | User login (returns access & refresh tokens) | Public |
| `POST` | `/api/auth/refresh` | Refresh access token | Public |
| `GET` | `/api/auth/me` | Current authenticated user profile | Authenticated |
| `POST` | `/api/auth/register-user` | Register team member in admin's organization | `USER_WRITE` / `ORG_ADMIN` |
| `POST` | `/api/auth/logout` | Invalidate active access & refresh tokens | Authenticated |

---

## 📊 Database Migrations (Flyway)

- **`V1__initial_schema.sql`**: Core multi-tenant architecture (`organizations`, `users`, `roles`, `permissions`, `employees`, `clients`, `tasks`, `audit_logs`).
- **`V2__organization_module_enhancement.sql`**: Organization address fields and `organization_settings` table.
- **`V3__rbac_roles_permissions_enhancement.sql`**: Granular permissions catalog and 7 default system roles with permission mappings.
- **`V4__employee_module_enhancement.sql`**: Employee contact details, names, manager hierarchy constraint, and indexes.
- **`V5__client_module_enhancement.sql`**: Client 360 profile fields, tax numbers (PAN, GSTIN, TAN, CIN), assigned employee relationship, indexes, and `client_notes` table for communication history.
- **`V6__gst_module.sql`**: GST Profiles & Registrations, GST Return Filings (GSTR-1, GSTR-3B, GSTR-9, CMP-08), and Monthly Summaries (Sales, Purchase, ITC, Tax Liability).
- **`V7__itr_module.sql`**: ITR Profiles (PAN, Taxpayer Type, Default ITR Form) and ITR Return Filings (Assessment Year, Due Date, Status Workflow, ACK Number).
- **`V8__compliance_calendar_module.sql`**: Configurable Compliance Rule Engine (due day, offsets, frequencies, applicability) and Compliance Obligations with automated task creation and dashboard analytics.
- **`V9__document_management_module.sql`**: Multi-tenant Document Vault (`documents` table) linked to Clients, GST filings, ITR returns, and Tasks with storage abstraction (Local/S3).
- **`V10__client_portal_module.sql`**: Client Portal authentication (`users.client_id`), `CLIENT_ADMIN` / `CLIENT_USER` roles, `client_notifications`, and `client_document_requests` pending checklists.
- **`V11__billing_module.sql`**: Client Invoicing & Billing (`invoices`, `invoice_items`, `invoice_payments`) with multi-service pricing, partial payment receipts, and balance due tracking.

---

## 🧪 Verification & Testing Suite

Taxoryn includes **131 automated tests** covering unit, integration, and security layers:
- **Client Billing & Payment Receipts**: Multi-service professional line items (GST, ITR, TDS, Accounting, Consulting), auto-calculated 18% GST tax, subtotal & total, invoice issuing (`DRAFT` -> `ISSUED`), partial & full payment receipts, automatic balance due settlement (`PARTIALLY_PAID` / `PAID`), client billing history ledger, practice billing dashboard KPIs, and cross-tenant billing isolation.
- **Client Portal & Security Isolation**: Scoped client login, JWT `clientId` claims, client compliance dashboard, GST/ITR tracking, pending document checklists, and strict security isolation preventing client users from accessing foreign clients, employees, org admin, internal notes, or internal tasks (403 Forbidden).
- **Document Management & Storage Abstraction**: Pluggable storage architecture (`DocumentStorageService`, `LocalDocumentStorageService`, `S3DocumentStorageService`), multipart file upload, SHA-256 integrity hashing, stream downloads, metadata tagging, client vault, and cross-tenant file isolation.
- **Compliance Calendar & Rule Engine**: Dynamic due date evaluation, custom & system rule configuration, batch obligation generator, scheduled background overdue processing, actionable task creation, and Executive Dashboard metrics (Due Today, Due This Week, Overdue, Completed, Type Breakdown).
- **ITR Compliance & Workload**: ITR client profile registration, return creation across AYs, 8-stage status workflow (`DOCUMENTS_PENDING` -> `COMPLETED`), e-filing ACK number tracking, upcoming/overdue queries, and Practice Workload Dashboard.
- **GST Compliance & Workload**: GST profile registration, GSTR-1 & GSTR-3B lifecycle, ARN tracking, ITC/liability calculation, batch period generator, and workload dashboard.
- **Client 360 & Constitution Types**: All 9 client types, statutory numbers, assigned practitioner, and contact hierarchy.
- **Client 360-Degree Overview**: Aggregated single-screen overview covering tasks, compliance, documents, billing, and notes.
- **Communication History**: Call, meeting, email, and follow-up interaction note logging and retrieval.
- **Employee CRUD & Workload Analytics**: Workload counts for assigned, pending, overdue, and completed tasks.
- **Tenant Isolation**: Cross-tenant entity lookups strictly return 404 NOT FOUND.
- **System Role Immutability**: System default roles cannot be altered or deleted.

```bash
mvn clean test
# Results: Tests run: 131, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS)
```

---

## 🚀 Quick Start

### 1. Provision Database & Seed Demo Data
```powershell
.\scripts\setup-db.ps1 -SeedData
```

### 2. Run Application
```bash
mvn spring-boot:run
```

### 3. Swagger UI & Documentation
- **Interactive UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **JSON Schema**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
