# Phase 11 — Client Service Master & Client-Service Engagement Foundation

## Summary of Accomplishments

Phase 11 establishes normalized client-service associations (`client_services`), service catalog (`ServiceCatalogItemDto`, `ClientServiceType`), operational practitioner assignment, lifecycle status transitions, tenant isolation, client portfolio scoping, module/subscription entitlement gating, and Client 360 integration.

---

## 1. Database Schema & Migration
- **Migration**: `src/main/resources/db/migration/V75__client_services_engagement.sql`
- **Table**: `client_services`
  - `id UUID PRIMARY KEY`
  - `organization_id UUID NOT NULL` (enforces tenant isolation)
  - `client_id UUID NOT NULL` (references `clients(id)`)
  - `service_type VARCHAR(64) NOT NULL` (maps to `ClientServiceType`)
  - `service_name VARCHAR(255) NOT NULL`
  - `status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'` (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `COMPLETED`)
  - `assigned_employee_id UUID` (operational assignee, decoupled from portfolio authorization)
  - `billing_cycle VARCHAR(32) DEFAULT 'MONTHLY'` (`MONTHLY`, `QUARTERLY`, `ANNUAL`, `ONE_TIME`)
  - `agreed_fee NUMERIC(15,2)`
  - `currency VARCHAR(10) DEFAULT 'INR'`
  - `start_date DATE`
  - `end_date DATE`
  - `engagement_notes TEXT`
  - `module_code VARCHAR(64)`
  - `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
  - `updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
  - Indexes on `(organization_id, client_id, status)` and `(organization_id, assigned_employee_id)`.

---

## 2. Backend Domain & Security Architecture
- **Catalog & Entitlement Enum**: [`ClientServiceType.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/entity/ClientServiceType.java)
  - Standard services: `GST_COMPLIANCE`, `INCOME_TAX_FILING`, `TDS_COMPLIANCE`, `TAX_NOTICE_MANAGEMENT`, `COMPLIANCE_CALENDAR`, `DOCUMENT_MANAGEMENT`, `CLIENT_BILLING`, `ACCOUNTING_BOOKKEEPING`, `STATUTORY_AUDIT`, `TAX_AUDIT`, `COMPANY_SECRETARIAL`, `PAYROLL_PROCESSING`, `ADVISORY_CONSULTING`, `OTHER`.
  - Maps to underlying `ProductModuleCode` and `ProductCapability`.
- **Entity**: [`ClientServiceEntity.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/entity/ClientServiceEntity.java)
- **Status Enum**: [`ClientServiceStatus.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/entity/ClientServiceStatus.java) (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `COMPLETED`)
- **DTOs**: `ServiceCatalogItemDto`, `ClientServiceDto`, `CreateClientServiceRequest`, `UpdateClientServiceRequest`
- **Repository**: [`ClientServiceRepository.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/repository/ClientServiceRepository.java)
- **Service**: [`ClientEngagementService.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/service/ClientEngagementService.java) & [`ClientEngagementServiceImpl.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/service/ClientEngagementServiceImpl.java)
  - Enforces tenant isolation via authenticated context `SecurityContextHelper.getRequiredTenantId()`.
  - Enforces portfolio scoping via `PracticeSecurityScopeEvaluator.checkClientAccess(clientId)`.
  - Prevents duplicate active engagements for the same service type.
  - Verifies module and subscription entitlement via `ModuleConfigurationService.verifyModuleAccess(...)`.
  - Performs audit logging for `CLIENT_SERVICE_CREATED`, `CLIENT_SERVICE_UPDATED`, `CLIENT_SERVICE_DEACTIVATED`.
- **Controller**: [`ClientEngagementController.java`](file:///d:/Projects/Taxoryn/src/main/java/com/taxoryn/module/client/controller/ClientEngagementController.java)
  - `GET /api/v1/client-services/catalog`
  - `GET /api/v1/clients/{clientId}/services`
  - `GET /api/v1/clients/{clientId}/services/{serviceId}`
  - `POST /api/v1/clients/{clientId}/services`
  - `PUT /api/v1/clients/{clientId}/services/{serviceId}`
  - `PATCH /api/v1/clients/{clientId}/services/{serviceId}/status`
  - `PATCH /api/v1/clients/{clientId}/services/{serviceId}/assignee`
  - `DELETE /api/v1/clients/{clientId}/services/{serviceId}`
- **Client 360 Aggregation Integration**:
  - `ClientServiceImpl.getClientOverview(...)` dynamically queries `ClientServiceRepository` to merge configured engagements with standard profile links.

---

## 3. Frontend Architecture
- **Types**: [`frontend/src/types/index.ts`](file:///d:/Projects/Taxoryn/frontend/src/types/index.ts)
  - Added `ClientServiceType`, `ClientServiceStatus`, `ServiceCatalogItem`, `ClientServiceDto`, `CreateClientServiceRequest`, `UpdateClientServiceRequest`, and updated `ClientServiceItem`.
- **API Endpoints**: [`frontend/src/api/endpoints.ts`](file:///d:/Projects/Taxoryn/frontend/src/api/endpoints.ts)
  - Added `clientServicesApi` with methods for catalog, client services listing, creation, updates, status transitions, assignee updates, and deactivation.
- **Client 360 View**: [`frontend/src/pages/Client360Page.tsx`](file:///d:/Projects/Taxoryn/frontend/src/pages/Client360Page.tsx)
  - Comprehensive "Services" Tab with filter pills (`ALL`, `ACTIVE`, `SUSPENDED`, `COMPLETED`, `INACTIVE`), engagement cards, practitioner assignments, fee display, direct status transition controls, module deep-links, and "Engage New Service" modal with real-time module entitlement warnings.

---

## 4. Verification Results
- **Focused Backend Test Suite**:
  - `ClientEngagementSecurityIntegrationTest` (8 tests): Passed (100%)
  - `Client360AggregationIntegrationTest` (4 tests): Passed (100%)
  - `ClientServiceTest` (17 tests): Passed (100%)
  - Total: 29 tests passed in 1m 10s.
- **Frontend Test Suite**:
  - `frontend/src/tests/clientServices.test.ts`: Passed (100%)
  - 142 total frontend unit tests passed.
- **Production Build**:
  - `npm run build` executed cleanly in 8.77s with 0 errors.
