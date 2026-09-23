# Phase 10: Client 360 / Client Profile Foundation — Walkthrough

## 1. Architecture Overview

Phase 10 establishes the **Client** as the central operational entity for Taxoryn CA practice management. Rather than duplicating data into a giant `Client360Entity` or creating complex separate tables, Phase 10 implements a **multi-domain read aggregation layer** directly on top of existing normalized domain entities (GST, ITR, TDS, Tasks, Documents, DocRequests, Invoices, Notices, Notes, and Audit Logs).

```
                      ┌──────────────────────────────────────────────┐
                      │              Client 360 Hub                  │
                      │       GET /api/v1/clients/{id}/360           │
                      └──────────────────────┬───────────────────────┘
                                             │
             ┌───────────────────────────────┼───────────────────────────────┐
             │                               │                               │
             ▼                               ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐             ┌─────────────────┐
    │  Client Master  │             │   Compliance    │             │ Work & Notices  │
    │  • Identity     │             │   • GST Profiles│             │ • Tasks         │
    │  • Statutory    │             │   • ITR Returns │             │ • Tax Notices   │
    │  • Assignment   │             │   • TDS Returns │             │ • Hearings      │
    └─────────────────┘             └─────────────────┘             └─────────────────┘
             │                               │                               │
             ▼                               ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐             ┌─────────────────┐
    │   Vault & Docs  │             │ Financial Scope │             │   Audit Trail   │
    │  • Documents    │             │ • Invoices      │             │ • Client Notes  │
    │  • Doc Requests │             │ • Zero-Trust    │             │ • Activity Log  │
    │  • Uploads/Reqs │             │   Redaction     │             │   Timeline      │
    └─────────────────┘             └─────────────────┘             └─────────────────┘
```

---

## 2. API Contract

### Endpoints
- `GET /api/v1/clients/{clientId}/overview`
- `GET /api/v1/clients/{clientId}/360` (canonical alias)
- `POST /api/v1/clients/{clientId}/notes`

### Response Payload Structure (`ClientOverviewDto`)
```json
{
  "success": true,
  "data": {
    "client": {
      "id": "uuid",
      "displayName": "Acme Technologies Pvt Ltd",
      "legalName": "Acme Technologies Private Limited",
      "clientType": "PRIVATE_LIMITED",
      "pan": "AAACA1234A",
      "gstin": "27AAACA1234A1Z5",
      "tan": "PNEP12345C",
      "assignedEmployeeName": "Vikram Sharma"
    },
    "statutory": {
      "pan": "AAACA1234A",
      "gstin": "27AAACA1234A1Z5",
      "tan": "PNEP12345C",
      "isPanValid": true,
      "isGstActive": true
    },
    "services": [
      { "serviceCode": "GST", "serviceName": "GST Compliance & Filing", "status": "ACTIVE" },
      { "serviceCode": "ITR", "serviceName": "Income Tax Returns", "status": "ACTIVE" },
      { "serviceCode": "TDS", "serviceName": "TDS & TCS Returns", "status": "ACTIVE" }
    ],
    "taskSummary": {
      "totalTasks": 5,
      "pendingTasks": 3,
      "inProgressTasks": 2,
      "completedTasks": 2,
      "recentTasks": []
    },
    "complianceSummary": {
      "gstStatus": "ACTIVE",
      "itrStatus": "ACTIVE",
      "tdsStatus": "ACTIVE",
      "gstDetails": { ... },
      "itrDetails": { ... },
      "tdsDetails": { ... }
    },
    "documentsSummary": { ... },
    "docRequestsSummary": { ... },
    "billingSummary": { ... }, // Redacted/omitted for non-billing staff
    "noticeSummary": { ... },
    "recentNotes": [ ... ],
    "activityTimeline": [ ... ]
  }
}
```

---

## 3. Security & Zero-Trust Governance

1. **Multi-Tenant Boundary**: Strictly enforces `organizationId` from authenticated JWT (`SecurityUtils.getCurrentOrganizationId()`). Cross-tenant access returns HTTP 404/403.
2. **Client Portfolio Scoping**: Validated via `securityScopeEvaluator.evaluateCurrentScope()`. Non-admin staff cannot access clients outside their assigned portfolio or department.
3. **Financial Redaction**: `billingSummary` is completely stripped if the caller lacks `ROLE_ORG_ADMIN`, `BILLING_VIEW`, or financial permissions.
4. **Task Scoping**: Staff deliverables are scoped to individual assigned tasks, while firm admins see comprehensive practice deliverables.

---

## 4. Frontend Architecture

- **Page**: `frontend/src/pages/Client360Page.tsx`
- **Route**: Protected `/clients/:clientId` wrapped with `<ModuleRouteGuard moduleCode="CLIENTS">`
- **Navigation**: Directly linked from the client data table in `ClientsPage.tsx`
- **Tabs (9 Functional Views)**:
  1. `Overview`: Hero KPI metrics, upcoming deliverables, statutory profile, and quick actions.
  2. `Services`: Active statutory tax service modules (GST, ITR, TDS, Notices, Documents, Billing).
  3. `Compliance`: Multi-domain Indian compliance status across GST returns, ITR filings, and TDS forms.
  4. `Documents`: Client document vault records and upload status.
  5. `Document Requests`: Status of checklists, pending requests, and received items.
  6. `Tasks`: Task list, priority badges, due dates, and quick status.
  7. `Notices`: Tax notices, demand amounts, hearing dates, and draft responses.
  8. `Billing`: Invoice history, payment status, and outstanding balances.
  9. `Activity & Notes`: Chronological activity timeline with interactive "Add Note" modal.

---

## 5. Verification Results

| Test Category | Suite / Command | Result |
| :--- | :--- | :--- |
| **Backend Compilation** | `mvn test-compile` | **PASS** (0 errors) |
| **Phase 10 Backend Tests** | `Client360AggregationIntegrationTest`, `ClientServiceTest`, `ClientManagementIntegrationTest`, `ClientAccessScopeSecurityIntegrationTest` | **PASS (41/41)** |
| **Frontend Unit Tests** | `node --test --experimental-strip-types src/tests/**/*.test.ts` | **PASS (137/137)** |
| **Frontend Production Build** | `npm run build` | **PASS** (12.10s, 0 errors) |

---

## 6. Milestone-Based Regression Strategy

> [!IMPORTANT]
> In accordance with the project development strategy, full regression (`mvn clean test`) is deferred to **Milestone 1 (after Phase 22)**. Focused Phase 10 verification is 100% complete and verified.
