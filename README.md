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

## 👔 Employee Management Module

The Employee module enables tax firms to manage their practitioners, hierarchy, and workload:

### 1. Employee Data Model
- `id` (UUID): Primary Key
- `organizationId` (UUID): Tenant ID (Strict multi-tenancy)
- `userId` (UUID): Optional link to User login account
- `employeeCode` (String): Unique employee identifier within tenant (e.g. `EMP-001`)
- `firstName` & `lastName` (String): Full practitioner name
- `email` (String): Official contact email (Unique per tenant)
- `phone` (String): Contact phone number
- `department` (String): Department (e.g. `Taxation`, `Audit`, `Accounting`)
- `designation` (String): Job title (e.g. `Partner`, `Senior Tax Manager`, `Audit Associate`)
- `joiningDate` (LocalDate): Employment start date
- `status` (Enum): `ACTIVE`, `INACTIVE`, `ON_LEAVE`, `RESIGNED`, `TERMINATED`
- `managerId` (UUID): Self-referential foreign key for reporting manager hierarchy within the tenant

### 2. Employee Workload Analytics (`GET /api/employees/{id}/workload`)
Computes real-time workload metrics across assigned workflow tasks:
- `totalAssignedTasks`: Total active tasks assigned to employee (excluding cancelled)
- `pendingTasks`: Tasks in `TODO`, `IN_PROGRESS`, or `UNDER_REVIEW`
- `overdueTasks`: Pending tasks where `dueDate < CURRENT_DATE`
- `completedTasks`: Tasks in `COMPLETED` status

---

## 📡 REST API Reference

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

---

## 🧪 Verification & Testing Suite

Taxoryn includes **66 automated tests** covering unit, integration, and security layers:
- **Employee CRUD & Lifecycle**: Creation, updating, status changes, and deactivation.
- **Search & Filtering**: Multi-field query search (name, email, code, phone) and filtering by department/status.
- **Workload Analytics**: Accurate counts for assigned, pending, overdue, and completed tasks.
- **Tenant Isolation**: Cross-tenant employee queries return 404 NOT FOUND.
- **System Role Immutability**: System roles cannot be updated or deleted by tenant admins (HTTP 403 `FORBIDDEN`).

```bash
mvn clean test
# Results: Tests run: 66, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS)
```

---

## 🚀 Quick Start

### 1. Run PostgreSQL with Docker Compose
```bash
docker compose up -d postgres
```

### 2. Run Application
```bash
mvn spring-boot:run
```

### 3. Swagger UI & Documentation
- **Interactive UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **JSON Schema**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
