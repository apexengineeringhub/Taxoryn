// ==============================================================================
// Taxoryn Frontend - Core TypeScript Interfaces
// ==============================================================================

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
  traceId?: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

// 1. Auth & User
export interface Organization {
  id: string;
  name: string;
  legalName?: string;
  tradeName?: string;
  email?: string;
  phone?: string;
  pan?: string;
  gstin?: string;
  subscriptionPlan?: 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export interface User {
  id: string;
  organizationId: string;
  organizationName?: string;
  email: string;
  firstName: string;
  lastName?: string;
  phone?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  roles: string[] | { id?: string; code: string; name: string }[];
  permissions: string[];
  isClientUser?: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
  organization?: Organization;
}

// 2. Organization Dashboard
export interface OrganizationDashboard {
  clients: {
    total: number;
    active: number;
    inactive: number;
  };
  employees: {
    total: number;
    active: number;
  };
  tasks: {
    total: number;
    pending: number;
    overdue: number;
    completed: number;
  };
  gst: {
    totalGstClients: number;
    returnsDue: number;
    returnsOverdue: number;
    returnsFiled: number;
  };
  itr: {
    totalItrClients: number;
    pending: number;
    filed: number;
    overdue: number;
  };
  billing: {
    totalInvoiceAmount: number;
    paidAmount: number;
    outstandingAmount: number;
  };
  employeeWorkload: EmployeeWorkloadItem[];
}

export interface EmployeeWorkloadItem {
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  email: string;
  department: string;
  designation: string;
  assignedTasks: number;
  pendingTasks: number;
  overdueTasks: number;
}

// 3. Client 360
export interface Client {
  id: string;
  organizationId: string;
  displayName: string;
  legalName?: string;
  tradeName?: string;
  pan: string;
  gstin?: string;
  tan?: string;
  cin?: string;
  clientType: 'INDIVIDUAL' | 'PROPRIETORSHIP' | 'PARTNERSHIP' | 'LLP' | 'PRIVATE_LIMITED' | 'PUBLIC_LIMITED' | 'TRUST' | 'HUF' | 'OTHER';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PROSPECT' | 'ARCHIVED';
  email?: string;
  phone?: string;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  portalEnabled?: boolean;
  createdAt?: string;
}

export interface ClientContact {
  id?: string;
  name: string;
  designation?: string;
  email?: string;
  phone?: string;
  isPrimary: boolean;
}

export interface BulkImportResult {
  totalProcessed: number;
  totalSuccess: number;
  totalFailed: number;
  totalSkipped: number;
  importedClients: Client[];
  errors: BulkImportError[];
}

export interface BulkImportError {
  rowNumber: number;
  clientName: string;
  pan: string;
  reason: string;
}

// 4. Task Management
export interface Task {
  id: string;
  organizationId: string;
  clientId: string;
  clientName?: string;
  assignedTo?: string;
  assigneeName?: string;
  title: string;
  description?: string;
  category?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'TODO' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'COMPLETED' | 'CANCELLED';
  dueDate: string;
  completedDate?: string;
  estimatedHours?: number;
  actualHours?: number;
}

export interface BulkTaskCreateRequest {
  clientIds: string[];
  assignedTo?: string;
  title: string;
  description?: string;
  taskCategory: 'GST' | 'ITR' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  dueDate?: string;
}

export interface BulkTaskImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalFailed: number;
  createdTasks: Task[];
  errors: string[];
}

// 5. GST Compliance
export interface GstProfile {
  id: string;
  clientId: string;
  clientName?: string;
  gstin: string;
  legalName: string;
  tradeName?: string;
  stateCode?: string;
  filingFrequency?: 'MONTHLY' | 'QUARTERLY';
  taxpayerType?: string;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface GstReturnFiling {
  id: string;
  gstProfileId: string;
  clientId: string;
  clientName?: string;
  gstin?: string;
  returnType: 'GSTR1' | 'GSTR3B' | 'GSTR9' | 'GSTR9C' | 'CMP08' | 'GSTR4' | 'GSTR7' | 'GSTR8';
  returnPeriod: string; // e.g. "2026-07"
  financialYear: string; // e.g. "2026-27"
  dueDate: string;
  filingStatus: 'PENDING' | 'PREPARED' | 'UNDER_REVIEW' | 'FILED' | 'OVERDUE' | 'FAILED';
  filingDate?: string;
  acknowledgementNumber?: string;
  totalTaxableValue: number;
  totalTaxLiability: number;
  totalItcClaimed: number;
  taxPaidCash: number;
  taxPaidItc: number;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  notes?: string;
}

// 6. ITR Compliance
export interface ItrProfile {
  id: string;
  clientId: string;
  clientName?: string;
  pan: string;
  taxpayerType: 'INDIVIDUAL' | 'HUF' | 'FIRM' | 'LLP' | 'COMPANY' | 'TRUST' | 'AOP_BOI';
  defaultItrType: 'ITR_1' | 'ITR_2' | 'ITR_3' | 'ITR_4' | 'ITR_5' | 'ITR_6' | 'ITR_7';
  residentialStatus: 'RESIDENT' | 'NON_RESIDENT' | 'RNOR';
  status: 'ACTIVE' | 'INACTIVE';
}

export interface ItrReturn {
  id: string;
  clientId: string;
  clientName?: string;
  itrProfileId?: string;
  assessmentYear: string; // e.g. "2026-27"
  financialYear: string; // e.g. "2025-26"
  itrType: 'ITR_1' | 'ITR_2' | 'ITR_3' | 'ITR_4' | 'ITR_5' | 'ITR_6' | 'ITR_7';
  taxpayerType: string;
  dueDate: string;
  filingDate?: string;
  acknowledgementNumber?: string;
  verificationDate?: string;
  status: 'DOCUMENTS_PENDING' | 'DATA_ENTRY' | 'UNDER_REVIEW' | 'READY_TO_FILE' | 'FILED' | 'VERIFICATION_PENDING' | 'COMPLETED' | 'CANCELLED';
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  notes?: string;
}

// 7. Compliance Calendar
export interface CalendarEvent {
  id: string;
  title: string;
  category: 'STATUTORY_DUE_DATE' | 'CLIENT_FILING_DUE' | 'TASK_DUE' | 'AUDIT_MILESTONE';
  complianceType: 'GST' | 'ITR' | 'TDS' | 'ROC' | 'ADVANCE_TAX' | 'GENERAL';
  dueDate: string;
  clientId?: string;
  clientName?: string;
  status: 'UPCOMING' | 'DUE_TODAY' | 'OVERDUE' | 'COMPLETED';
  urgency: 'NORMAL' | 'HIGH' | 'CRITICAL';
}

// 8. Documents
export interface DocumentItem {
  id: string;
  clientId?: string;
  clientName?: string;
  filename: string;
  originalFilename: string;
  category: string;
  fileSize: number;
  contentType: string;
  storageKey: string;
  uploadedByName?: string;
  createdAt: string;
  tags?: string[];
}

// 9. Billing & Invoices
export interface Invoice {
  id: string;
  invoiceNumber: string;
  clientId: string;
  clientName?: string;
  invoiceDate: string;
  dueDate: string;
  subtotal: number;
  tax: number;
  total: number;
  paidAmount: number;
  balanceDue: number;
  status: 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED' | 'OVERDUE';
  notes?: string;
  items?: InvoiceLineItem[];
}

export interface InvoiceLineItem {
  id?: string;
  description: string;
  hsnSacCode?: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  taxRate: number;
}

// 10. Subscriptions & Plans
export interface SubscriptionPlan {
  id: string;
  code: 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';
  name: string;
  monthlyPrice: number;
  annualPrice: number;
  maxClients: number;
  maxUsers: number;
  maxStorageGb: number;
  features: string[];
}

export interface SubscriptionInfo {
  id: string;
  organizationId: string;
  plan: 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';
  status: 'ACTIVE' | 'TRIALING' | 'PAST_DUE' | 'CANCELLED';
  billingInterval: 'MONTHLY' | 'ANNUAL';
  currentPeriodStart: string;
  currentPeriodEnd: string;
  usage: {
    clientsUsed: number;
    clientsMax: number;
    usersUsed: number;
    usersMax: number;
    storageUsedGb: number;
    storageMaxGb: number;
  };
}

// 11. Employee & RBAC
export interface Employee {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  department: string;
  designation: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE' | 'TERMINATED';
  roleName?: string;
}

export interface BulkEmployeeImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalFailed: number;
  totalSkipped: number;
  createdEmployees: Employee[];
  errors: string[];
}

export interface Role {
  id: string;
  code: string;
  name: string;
  description?: string;
  isSystemRole: boolean;
  permissions: { code: string; name: string; module: string }[];
}

// 12. Audit Logs
export interface AuditLog {
  id: string;
  userId?: string;
  userName?: string;
  userEmail?: string;
  action: string;
  entityType: string;
  entityId?: string;
  timestamp: string;
  ipAddress?: string;
  requestId?: string;
  oldValue?: string;
  newValue?: string;
}
