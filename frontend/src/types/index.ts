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
  city?: string;
  state?: string;
  subscriptionPlan?: 'STARTER' | 'PROFESSIONAL' | 'BUSINESS' | 'ENTERPRISE';
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  createdAt?: string;
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
  roles: (string | { id?: string; code: string; name: string })[];
  permissions: string[];
  isClientUser?: boolean;
  createdAt?: string;
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
  tds?: {
    totalTdsClients: number;
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
  assigneeEmail?: string;
  title: string;
  description?: string;
  category?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'TODO' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'COMPLETED' | 'CANCELLED';
  dueDate: string;
  completedDate?: string;
  estimatedHours?: number;
  actualHours?: number;
  unassign?: boolean;
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

export interface BulkGstImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalSkipped: number;
  totalFailed: number;
  importedItems: string[];
  errors: string[];
}

export interface BulkItrImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalSkipped: number;
  totalFailed: number;
  importedItems: string[];
  errors: string[];
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
  pan?: string;
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

export interface DocumentItem {
  id: string;
  clientId?: string;
  clientName?: string;
  filename: string;
  title?: string;
  fileType?: string;
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
  clientGstin?: string;
  clientPan?: string;
  invoiceDate: string;
  dueDate: string;
  subtotal: number;
  tax: number;
  total: number;
  totalAmount?: number;
  paidAmount: number;
  balanceDue: number;
  status: 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED' | 'OVERDUE';
  notes?: string;
  terms?: string;
  items?: InvoiceLineItem[];
  payments?: InvoicePaymentRecord[];
  createdAt?: string;
}

export interface InvoiceLineItem {
  id?: string;
  service: 'GST_FILING' | 'ITR_FILING' | 'TDS' | 'ACCOUNTING' | 'CONSULTING' | 'AUDIT' | 'ROC_COMPLIANCE' | 'OTHER';
  description: string;
  hsnSacCode?: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  tax?: number;
  amount?: number;
}

export interface InvoicePaymentRecord {
  id: string;
  invoiceId: string;
  amount: number;
  paymentDate: string;
  paymentMode: 'BANK_TRANSFER' | 'UPI' | 'CHEQUE' | 'CASH' | 'OTHER';
  referenceNumber?: string;
  notes?: string;
}

export interface BulkCreateInvoicesRequest {
  clientIds?: string[];
  invoiceDate: string;
  dueDate: string;
  items: Array<{
    service: 'GST_FILING' | 'ITR_FILING' | 'TDS' | 'ACCOUNTING' | 'CONSULTING' | 'AUDIT' | 'ROC_COMPLIANCE' | 'OTHER';
    description?: string;
    quantity: number;
    unitPrice: number;
    taxRate: number;
  }>;
  autoIssue?: boolean;
  notes?: string;
  terms?: string;
}

export interface BulkInvoiceResult {
  totalProcessed: number;
  totalCreated: number;
  totalSkipped: number;
  totalFailed: number;
  totalBilledAmount: number;
  createdInvoices: Invoice[];
  errors: string[];
}

export interface BillingDashboardStats {
  totalBilled: number;
  totalCollected: number;
  totalOutstanding: number;
  totalDraft: number;
  totalInvoices: number;
  draftInvoices: number;
  issuedInvoices: number;
  partiallyPaidInvoices: number;
  paidInvoices: number;
  overdueInvoices: number;
  cancelledInvoices: number;
  revenueByService?: Record<string, number>;
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
  userId?: string;
  employeeCode: string;
  firstName: string;
  lastName?: string;
  fullName?: string;
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
  organizationId?: string;
  organizationName?: string;
  practiceName?: string;
  userId?: string;
  actor?: string;
  actorName?: string;
  actorEmail?: string;
  actorRole?: string;
  userName?: string;
  userEmail?: string;
  action: string;
  displayAction?: string;
  entityType: string;
  displayEntityType?: string;
  entityName?: string;
  entityId?: string;
  targetDisplayName?: string;
  status?: string;
  severity?: 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS';
  description?: string;
  timestamp: string;
  createdAt?: string;
  ipAddress?: string;
  requestId?: string;
  userAgent?: string;
  oldValue?: string;
  newValue?: string;
}

// 13. Client Portal
export interface ClientGstStatus {
  id: string;
  returnPeriod: string;
  financialYear?: string;
  returnType: string;
  status: string;
  dueDate: string;
  filedDate?: string;
  arn?: string;
  totalTaxPayable?: number;
  itcClaimed?: number;
}

export interface ClientItrStatus {
  id: string;
  assessmentYear: string;
  financialYear?: string;
  itrType: string;
  taxpayerType?: string;
  status: string;
  dueDate: string;
  filingDate?: string;
  acknowledgementNumber?: string;
}

export interface ClientDocumentRequest {
  id: string;
  title: string;
  description?: string;
  documentType: string;
  status: 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED';
  dueDate?: string;
  requestedAt: string;
  uploadedDocumentId?: string;
  uploadedDocumentName?: string;
}

export interface ClientPortalUser {
  userId: string;
  clientId: string;
  clientName: string;
  email: string;
  firstName: string;
  lastName?: string;
  fullName?: string;
  phone?: string;
  roles: string[];
}

export interface RegisterClientPortalUserRequest {
  clientId: string;
  email: string;
  password: string;
  firstName: string;
  lastName?: string;
  phone?: string;
  role: 'CLIENT_ADMIN' | 'CLIENT_USER';
}

export interface ClientPortalProfile {
  clientId: string;
  displayName: string;
  legalName?: string;
  pan: string;
  gstin?: string;
  tan?: string;
  email?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
  assignedPractitionerName?: string;
  assignedPractitionerEmail?: string;
  assignedPractitionerPhone?: string;
}

export interface ClientPortalDashboard {
  clientId: string;
  displayName: string;
  legalName?: string;
  clientType: string;
  pan: string;
  gstin?: string;
  tan?: string;
  assignedPractitionerName?: string;
  assignedPractitionerEmail?: string;
  assignedPractitionerPhone?: string;
  pendingDocumentsCount: number;
  pendingTasksCount: number;
  activeGstReturnsCount: number;
  activeItrReturnsCount: number;
  unpaidInvoicesCount: number;
  outstandingBalance: number;
  latestGstFilings: ClientGstStatus[];
  latestItrReturns: ClientItrStatus[];
  pendingDocumentRequests: ClientDocumentRequest[];
  pendingTasks: any[];
  recentNotifications: any[];
  latestInvoices: Invoice[];
}

// 12. TDS / TCS Practice Management Interfaces
export interface TdsProfile {
  id: string;
  clientId: string;
  clientName?: string;
  tan: string;
  deductorType: 'COMPANY' | 'INDIVIDUAL_HUF' | 'FIRM' | 'LLP' | 'BRANCH_DIVISION' | 'GOVERNMENT_CENTRAL' | 'GOVERNMENT_STATE' | 'STATUTORY_BODY' | 'AUTONOMOUS_BODY' | 'OTHER';
  branchDivisionName?: string;
  paCode?: string;
  ddoCode?: string;
  ministryName?: string;
  responsiblePersonName?: string;
  responsiblePersonPan?: string;
  responsiblePersonDesignation?: string;
  responsiblePersonFatherName?: string;
  responsiblePersonEmail?: string;
  responsiblePersonMobile?: string;
  responsiblePersonAddress?: string;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SURRENDERED';
  tracesUsername?: string;
  tracesStatus: 'NOT_REGISTERED' | 'REGISTERED_ACTIVE' | 'PASSWORD_EXPIRED' | 'SUSPENDED';
  createdAt?: string;
  updatedAt?: string;
}

export interface TdsReturn {
  id: string;
  clientId: string;
  clientName?: string;
  tdsProfileId: string;
  tan?: string;
  formType: 'FORM_24Q' | 'FORM_26Q' | 'FORM_27Q' | 'FORM_27EQ' | 'FORM_26QB' | 'FORM_26QC' | 'FORM_26QD' | 'FORM_26QE';
  quarter: 'Q1' | 'Q2' | 'Q3' | 'Q4';
  financialYear: string; // e.g. "2026-27"
  assessmentYear: string; // e.g. "2027-28"
  dueDate?: string;
  filingStatus: 'PENDING' | 'DRAFT' | 'CHALLANS_ATTACHED' | 'UNDER_REVIEW' | 'READY_TO_FILE' | 'FILED' | 'OVERDUE' | 'CANCELLED';
  filingDate?: string;
  tokenNumber?: string;
  receiptNumber?: string;
  totalAmountPaid: number;
  totalTaxDeducted: number;
  totalTaxDeposited: number;
  totalInterest?: number;
  totalLateFee?: number;
  totalPenalty?: number;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  fvuValidationStatus: 'NOT_VALIDATED' | 'VALIDATED' | 'FAILED';
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TdsChallan {
  id: string;
  tdsProfileId: string;
  tan?: string;
  clientName?: string;
  tdsReturnId?: string;
  bsrCode: string;
  challanDate: string;
  challanSerialNo: string;
  cin?: string;
  majorHead: 'HEAD_0020_COMPANY' | 'HEAD_0021_NON_COMPANY';
  minorHead: 'HEAD_200_PAYABLE_BY_TAXPAYER' | 'HEAD_400_REGULAR_ASSESSMENT';
  sectionCode: string;
  tdsAmount: number;
  surchargeAmount?: number;
  cessAmount?: number;
  interestAmount?: number;
  feeAmount?: number;
  penaltyAmount?: number;
  totalAmount: number;
  utilizedAmount: number;
  balanceAmount: number;
  challanStatus: 'UNUTILIZED' | 'PARTIALLY_UTILIZED' | 'FULLY_UTILIZED' | 'OVERUTILIZED';
  quarter: 'Q1' | 'Q2' | 'Q3' | 'Q4';
  financialYear: string;
  paymentMode: 'NET_BANKING' | 'DEBIT_CARD' | 'OVER_THE_COUNTER' | 'NEFT_RTGS';
  bankName?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TdsDeducteeEntry {
  id: string;
  tdsProfileId: string;
  tdsReturnId?: string;
  challanId?: string;
  deducteePan: string;
  deducteeName: string;
  deducteeType: 'COMPANY' | 'NON_COMPANY';
  sectionCode: string;
  paymentCreditDate: string;
  invoiceRefNumber?: string;
  amountPaidCredited: number;
  tdsRate: number;
  tdsAmount: number;
  surchargeAmount?: number;
  cessAmount?: number;
  totalTaxDeducted: number;
  deductionDate: string;
  certificateNumber197?: string;
  reasonCode: 'STANDARD' | 'LOWER_RATE_197' | 'NIL_RATE_197' | 'FORM_15G_15H' | 'TRANSPORTER_194C' | 'THRESHOLD_EXEMPTION' | 'HIGHER_RATE_206AA' | 'HIGHER_RATE_206AB';
  quarter: 'Q1' | 'Q2' | 'Q3' | 'Q4';
  financialYear: string;
  status: 'ACTIVE' | 'REVERSED';
  createdAt?: string;
  updatedAt?: string;
}

export interface TdsCertificate {
  id: string;
  tdsProfileId: string;
  tan?: string;
  clientName?: string;
  tdsReturnId?: string;
  certificateType: 'FORM_16_PART_A' | 'FORM_16_PART_B' | 'FORM_16A' | 'FORM_27D';
  financialYear: string;
  quarter?: 'Q1' | 'Q2' | 'Q3' | 'Q4';
  deducteePan: string;
  deducteeName: string;
  tracesRequestNumber?: string;
  certificateNumber?: string;
  generationDate?: string;
  dispatchStatus: 'PENDING' | 'REQUESTED_FROM_TRACES' | 'DOWNLOADED' | 'DIGITALLY_SIGNED' | 'SENT_TO_CLIENT' | 'SENT_TO_DEDUCTEE';
  dispatchedAt?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TdsSectionRate {
  sectionCode: string;
  title: string;
  returnForm: string;
  rateIndividual: number;
  rateOthers: number;
  thresholdLimit: number;
  singleTransactionLimit: number;
  nonPanRate: number;
  statutoryNotes: string;
}

export interface TdsComputationRequest {
  sectionCode: string;
  amount: number;
  deducteeType?: 'COMPANY' | 'NON_COMPANY';
  validPanProvided?: boolean;
  specifiedNonFiler206AB?: boolean;
  lowerDeductionRate?: number;
  cumulativePaidInYear?: number;
  paymentCreditDate?: string;
  deductionDate?: string;
  depositDate?: string;
  filingDueDate?: string;
  actualFilingDate?: string;
}

export interface TdsComputationResult {
  sectionCode: string;
  sectionTitle: string;
  grossAmount: number;
  thresholdExemptionApplicable: boolean;
  effectiveRate: number;
  baseTdsAmount: number;
  surchargeRate: number;
  surchargeAmount: number;
  cessAmount: number;
  totalTaxDeducted: number;
  netPayableToDeductee: number;
  delayInDeductionInterest: number;
  delayInDepositInterest: number;
  totalInterest: number;
  delayDays: number;
  lateFee234E: number;
  totalPayableWithPenalties: number;
  remarks: string;
}

export interface TdsWorkloadDashboard {
  quarter: string;
  financialYear: string;
  totalTanClients: number;
  activeTanProfiles: number;
  totalScheduledReturns: number;
  filedReturns: number;
  pendingReturns: number;
  underReviewReturns: number;
  overdueReturns: number;
  totalPracticeTdsDeducted: number;
  totalPracticeChallansPaid: number;
  unutilizedChallanBalance: number;
  pendingCertificatesCount: number;
  returnCards: TdsReturn[];
}

export interface BulkTdsProfileImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalSkipped: number;
  totalFailed: number;
  importedProfiles: TdsProfile[];
  errorMessages: string[];
}

export interface BulkTdsReturnImportResult {
  totalProcessed: number;
  totalCreated: number;
  totalSkipped: number;
  totalFailed: number;
  importedReturns: TdsReturn[];
  errorMessages: string[];
}

// 13. Customer Marketplace & Discovery Interfaces
export type ProfessionalType = 'CHARTERED_ACCOUNTANT' | 'COMPANY_SECRETARY' | 'COST_ACCOUNTANT' | 'TAX_ADVOCATE' | 'TAX_CONSULTANT';
export type VisibilityStatus = 'PRIVATE' | 'PUBLIC' | 'SUSPENDED';
export type VerificationStatus = 'NOT_SUBMITTED' | 'PENDING' | 'VERIFIED' | 'REJECTED';
export type LeadStatus = 'NEW' | 'CONTACTED' | 'PROPOSAL_SENT' | 'CONVERTED' | 'ARCHIVED';
export type LeadUrgency = 'LOW' | 'STANDARD' | 'URGENT';
export type ConsultationMode = 'VIDEO' | 'PHONE' | 'IN_PERSON';
export type ConsultationStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface MarketplaceService {
  id?: string;
  organizationId?: string;
  marketplaceProfileId?: string;
  title: string;
  category: string;
  description?: string;
  price: number;
  pricingType: 'FIXED' | 'MONTHLY_RETAINER' | 'HOURLY' | 'CUSTOM';
  deliveryDays: number;
  deliverables?: string;
  isActive: boolean;
}

export interface MarketplaceReview {
  id: string;
  marketplaceProfileId: string;
  reviewerName: string;
  reviewerDesignation?: string;
  reviewerCompany?: string;
  rating: number;
  reviewTitle?: string;
  reviewComment: string;
  serviceTaken?: string;
  isVerifiedClient?: boolean;
  createdAt: string;
}

export interface MarketplaceProfile {
  id: string;
  organizationId: string;
  slug: string;
  displayName: string;
  publicSlug?: string;
  description?: string;
  website?: string;
  headline?: string;
  bio?: string;
  professionalType: ProfessionalType;
  experienceYears: number;
  city?: string;
  state?: string;
  pincode?: string;
  address?: string;
  phone?: string;
  email?: string;
  websiteUrl?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  specializations: string[];
  languagesSpoken?: string;
  startingFee: number;
  hourlyRate: number;
  averageRating: number;
  totalReviews: number;
  totalClientsServed: number;
  verificationStatus: VerificationStatus;
  visibilityStatus?: VisibilityStatus;
  isPublished: boolean;
  isFeatured: boolean;
  consultationEnabled: boolean;
  consultationFee: number;
  consultationDurationMinutes: number;
  services?: MarketplaceService[];
  offeredServices?: PublicTaxService[];
  recentReviews?: MarketplaceReview[];
  locations?: PublicPracticeLocation[];
  primaryLocation?: PublicPracticeLocation;
  distanceKm?: number;
  nearestLocation?: PublicPracticeLocation;
  completeness?: ProfileCompleteness;
  profileCompleteness?: ProfileCompleteness;
  completenessScore?: number;
  missingCompletenessFields?: string[];
}

// 6. Controlled Tax Service Master Interfaces
export interface TaxServiceCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  sortOrder: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaxServiceAlias {
  id: string;
  taxServiceId: string;
  alias: string;
  normalizedAlias: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TaxService {
  id: string;
  categoryId: string;
  categoryCode?: string;
  categoryName?: string;
  code: string;
  name: string;
  description?: string;
  sortOrder: number;
  isActive: boolean;
  aliases?: TaxServiceAlias[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PublicTaxService {
  id: string;
  code: string;
  name: string;
  description?: string;
  category: string;
  categoryName?: string;
  sortOrder?: number;
}

export interface PublicTaxServiceCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  sortOrder: number;
  services: PublicTaxService[];
}

export interface PracticeService {
  id: string;
  marketplaceProfileId: string;
  taxServiceId: string;
  taxServiceCode: string;
  taxServiceName: string;
  categoryCode: string;
  categoryName: string;
  description?: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateTaxServiceCategoryRequest {
  code: string;
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface UpdateTaxServiceCategoryRequest {
  name?: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface CreateTaxServiceRequest {
  categoryId: string;
  code: string;
  name: string;
  description?: string;
  sortOrder?: number;
  isActive?: boolean;
  aliases?: string[];
}

export interface UpdateTaxServiceRequest {
  categoryId?: string;
  name?: string;
  description?: string;
  sortOrder?: number;
  isActive?: boolean;
}

export interface CreateTaxServiceAliasRequest {
  alias: string;
  isActive?: boolean;
}

export interface UpdatePracticeServicesRequest {
  taxServiceIds: string[];
}

export interface PracticeLocation {
  id: string;
  marketplaceProfileId: string;
  organizationId: string;
  locationName: string;
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  district?: string;
  state: string;
  stateCode?: string;
  country: string;
  countryCode: string;
  pincode: string;
  latitude?: number;
  longitude?: number;
  isPrimary: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PublicPracticeLocation {
  id: string;
  locationName: string;
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  district?: string;
  state: string;
  stateCode?: string;
  country: string;
  countryCode: string;
  pincode: string;
  latitude?: number;
  longitude?: number;
  isPrimary: boolean;
}

export interface CreatePracticeLocationRequest {
  locationName: string;
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  district?: string;
  state: string;
  stateCode?: string;
  country?: string;
  countryCode?: string;
  pincode: string;
  latitude?: number;
  longitude?: number;
  isPrimary?: boolean;
}

export interface UpdatePracticeLocationRequest {
  locationName: string;
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  district?: string;
  state: string;
  stateCode?: string;
  country?: string;
  countryCode?: string;
  pincode: string;
  latitude?: number;
  longitude?: number;
  isPrimary?: boolean;
  isActive?: boolean;
}

export interface ProfileCompleteness {
  percentage: number;
  completedItems: string[];
  missingItems: string[];
  isEligibleForPublishing?: boolean;
}

export interface MarketplaceLead {
  id: string;
  organizationId: string;
  marketplaceProfileId: string;
  serviceId?: string;
  serviceTitle?: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  city?: string;
  pan?: string;
  gstin?: string;
  serviceCategory?: string;
  requirementDescription: string;
  budgetRange?: string;
  urgency: LeadUrgency;
  leadStatus: LeadStatus;
  convertedClientId?: string;
  convertedClientName?: string;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  practitionerNotes?: string;
  createdAt: string;
}

export interface MarketplaceConsultation {
  id: string;
  organizationId: string;
  marketplaceProfileId: string;
  practiceDisplayName?: string;
  leadId?: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  topic: string;
  consultationMode: ConsultationMode;
  meetingLink?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  feeAmount: number;
  paymentStatus: 'PENDING' | 'PAID' | 'WAIVED' | 'REFUNDED';
  consultationStatus: ConsultationStatus;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  notes?: string;
}

export interface MarketplaceVerification {
  id: string;
  organizationId: string;
  organizationName?: string;
  marketplaceProfileId: string;
  professionalBody: string;
  membershipNumber: string;
  copNumber?: string;
  firmRegistrationNumber?: string;
  documentUrl?: string;
  verificationStatus: VerificationStatus;
  rejectionReason?: string;
  verifiedAt?: string;
  verifiedBy?: string;
  createdAt: string;
}

export interface MarketplaceStats {
  totalListedPractitioners: number;
  totalVerifiedPractitioners: number;
  totalPendingVerifications: number;
  totalInboundLeads: number;
  totalConvertedClients: number;
  leadConversionRate: number;
  totalConsultationsBooked: number;
  estimatedMarketplacePipelineValue: number;
}

// 14. Marketplace Proposal & Onboarding Pipeline Types
export type ProposalStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';
export type OnboardingStatus = 'INITIATED' | 'DOCUMENTS_PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export type OnboardingDocType =
  | 'PAN_CARD'
  | 'AADHAAR_CARD'
  | 'CERTIFICATE_OF_INCORPORATION'
  | 'GST_CERTIFICATE'
  | 'ADDRESS_PROOF'
  | 'BOARD_RESOLUTION'
  | 'CANCELLED_CHEQUE'
  | 'OTHER';
export type DocVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface MarketplaceProposal {
  id: string;
  organizationId: string;
  practiceDisplayName?: string;
  marketplaceProfileId: string;
  leadId: string;
  clientName?: string;
  clientEmail?: string;
  clientPhone?: string;
  serviceId?: string;
  serviceTitle?: string;
  proposalTitle: string;
  scopeOfWork: string;
  deliverables?: string;
  feeAmount: number;
  pricingType: 'FIXED' | 'MONTHLY_RETAINER' | 'HOURLY';
  estimatedTimelineDays: number;
  proposalStatus: ProposalStatus;
  accessToken: string;
  validUntil?: string;
  rejectionReason?: string;
  acceptedAt?: string;
  createdAt: string;
}

export interface OnboardingDocument {
  id: string;
  onboardingId: string;
  documentType: OnboardingDocType;
  documentName: string;
  filePath: string;
  fileSizeBytes: number;
  contentType?: string;
  isRequired: boolean;
  verificationStatus: DocVerificationStatus;
  rejectionReason?: string;
  verifiedAt?: string;
  verifiedBy?: string;
  createdAt: string;
}

export interface MarketplaceOnboarding {
  id: string;
  organizationId: string;
  practiceDisplayName?: string;
  marketplaceProfileId: string;
  leadId: string;
  proposalId?: string;
  proposalTitle?: string;
  accessToken: string;
  clientName: string;
  legalName?: string;
  clientEmail: string;
  clientPhone: string;
  entityType: 'INDIVIDUAL' | 'COMPANY' | 'LLP' | 'FIRM' | 'HUF' | 'TRUST';
  pan?: string;
  gstin?: string;
  tan?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
  onboardingStatus: OnboardingStatus;
  engagementLetterSigned: boolean;
  engagementSignedAt?: string;
  engagementLetterUrl?: string;
  feeAgreementAgreed: boolean;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  promotedClientId?: string;
  portalUserId?: string;
  reviewerNotes?: string;
  rejectionReason?: string;
  completedAt?: string;
  createdAt: string;
  documents?: OnboardingDocument[];
}

export interface CreateProposalRequest {
  leadId: string;
  serviceId?: string;
  proposalTitle: string;
  scopeOfWork: string;
  deliverables?: string;
  feeAmount: number;
  pricingType?: 'FIXED' | 'MONTHLY_RETAINER' | 'HOURLY';
  estimatedTimelineDays?: number;
  validUntil?: string;
}

export interface AcceptProposalRequest {
  isAccepted: boolean;
  rejectionReason?: string;
  clientNotes?: string;
}

export interface InitiateOnboardingRequest {
  leadId: string;
  proposalId?: string;
  entityType?: 'INDIVIDUAL' | 'COMPANY' | 'LLP' | 'FIRM' | 'HUF' | 'TRUST';
  assignedEmployeeId?: string;
}

export interface UpdateOnboardingDetailsRequest {
  clientName: string;
  legalName?: string;
  entityType?: 'INDIVIDUAL' | 'COMPANY' | 'LLP' | 'FIRM' | 'HUF' | 'TRUST';
  pan?: string;
  gstin?: string;
  tan?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  pincode?: string;
}

export interface SignEngagementLetterRequest {
  signedConsent: boolean;
  agreedToFees: boolean;
  signatureName?: string;
  signatureIpAddress?: string;
}

export interface VerifyOnboardingDocumentRequest {
  verificationStatus: DocVerificationStatus;
  rejectionReason?: string;
}

export interface ApproveAndPromoteClientRequest {
  assignedEmployeeId?: string;
  createOnboardingTask?: boolean;
  provisionClientPortalUser?: boolean;
  initialPortalPassword?: string;
  reviewerNotes?: string;
}

// 8. Marketplace Customer Account & Profile
export interface CustomerProfileCompleteness {
  percentage: number;
  completedItems: string[];
  missingItems: string[];
}

export interface CustomerProfile {
  id: string;
  userId: string;
  customerType: 'INDIVIDUAL' | 'BUSINESS';
  firstName: string;
  lastName?: string;
  displayName: string;
  email: string;
  phone?: string;
  profilePhotoUrl?: string;
  city?: string;
  state?: string;
  pincode?: string;
  preferredLanguage?: string;
  businessName?: string;
  status: 'ACTIVE' | 'BLOCKED' | 'DEACTIVATED';
  profileCompleteness?: CustomerProfileCompleteness;
  createdAt: string;
  updatedAt: string;
}

export interface RegisterCustomerRequest {
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  password: string;
  customerType?: 'INDIVIDUAL' | 'BUSINESS';
  businessName?: string;
  city?: string;
  state?: string;
  pincode?: string;
  preferredLanguage?: string;
}

export interface UpdateCustomerProfileRequest {
  firstName?: string;
  lastName?: string;
  displayName?: string;
  phone?: string;
  profilePhotoUrl?: string;
  city?: string;
  state?: string;
  pincode?: string;
  preferredLanguage?: string;
  customerType?: 'INDIVIDUAL' | 'BUSINESS';
  businessName?: string;
}

export interface CustomerDashboard {
  profile: CustomerProfile;
  totalRequirements?: number;
  totalRequests: number;
  totalConsultations: number;
  totalProposals: number;
  totalReviews: number;
  recentTaxRequirements?: CustomerTaxRequirementSummary[];
  recentLeads: MarketplaceLead[];
  recentConsultations: MarketplaceConsultation[];
  recentProposals: MarketplaceProposal[];
  recentReviews: MarketplaceReview[];
}

export interface CustomerAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  customer: CustomerProfile;
}

// 18. Customer Tax Requirement Interfaces (Feature #6)
export type CustomerTaxpayerType =
  | 'SALARIED'
  | 'SELF_EMPLOYED'
  | 'BUSINESS_OWNER'
  | 'FREELANCER'
  | 'INVESTOR'
  | 'OTHER';

export type TaxRequirementStatus = 'DRAFT' | 'SUBMITTED' | 'CANCELLED' | 'CLOSED';

export interface CustomerTaxRequirement {
  id: string;
  customerId: string;
  service: PublicTaxService;
  status: TaxRequirementStatus;
  statusDisplayName: string;
  customerType?: CustomerTaxpayerType;
  customerTypeDisplayName?: string;
  financialYear?: string;
  financialYearDisplay?: string;
  description?: string;
  city?: string;
  state?: string;
  pincode?: string;
  searchRadiusKm?: number;
  editable: boolean;
  cancellable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerTaxRequirementSummary {
  id: string;
  taxServiceId: string;
  taxServiceCode: string;
  taxServiceName: string;
  categoryName?: string;
  status: TaxRequirementStatus;
  statusDisplayName: string;
  customerType?: CustomerTaxpayerType;
  customerTypeDisplayName?: string;
  financialYear?: string;
  financialYearDisplay?: string;
  city?: string;
  state?: string;
  editable: boolean;
  cancellable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaxRequirementRequest {
  taxServiceId?: string;
  taxServiceCode?: string;
  customerType?: CustomerTaxpayerType;
  financialYear?: string;
  description?: string;
  city?: string;
  state?: string;
  pincode?: string;
  searchRadiusKm?: number;
}

export interface UpdateTaxRequirementRequest {
  taxServiceId?: string;
  taxServiceCode?: string;
  customerType?: CustomerTaxpayerType;
  financialYear?: string;
  description?: string;
  city?: string;
  state?: string;
  pincode?: string;
  searchRadiusKm?: number;
}

export interface FinancialYearOption {
  code: string;
  label: string;
  isCurrent: boolean;
}

export type PrivacyDataLevel = 'LEVEL_1_PUBLIC' | 'LEVEL_2_EARLY_ENQUIRY' | 'LEVEL_3_PRIVATE_CUSTOMER' | 'LEVEL_4_SENSITIVE_DOCUMENTS';

export interface EarlyEnquiryView {
  id: string;
  organizationId: string;
  marketplaceProfileId: string;
  service?: PublicTaxService;
  serviceCategory?: string;
  financialYear?: string;
  financialYearDisplay?: string;
  customerType?: CustomerTaxpayerType;
  customerTypeDisplayName?: string;
  requirementSummary: string;
  clientName: string;
  maskedEmail?: string;
  maskedPhone?: string;
  city?: string;
  state?: string;
  budgetRange?: string;
  urgency: LeadUrgency;
  leadStatus: LeadStatus;
  assignedEmployeeId?: string;
  assignedEmployeeName?: string;
  practitionerNotes?: string;
  privacyLevel: PrivacyDataLevel;
  createdAt: string;
}

export interface CreateMarketplaceLeadRequest {
  marketplaceProfileId: string;
  taxRequirementId?: string;
  taxServiceId?: string;
  taxServiceCode?: string;
  financialYear?: string;
  customerType?: CustomerTaxpayerType;
  earlyEnquiryMessage?: string;
  serviceId?: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  city?: string;
  pan?: string;
  gstin?: string;
  serviceCategory?: string;
  requirementDescription?: string;
  budgetRange?: string;
  urgency?: LeadUrgency | string;
}

// Application Feedback is deliberately separate from a MarketplaceReview.
export type ApplicationFeedbackType = 'SUGGESTION' | 'PROBLEM' | 'GENERAL' | 'EXPERIENCE';
export type ApplicationFeedbackCategory =
  | 'APPLICATION_EXPERIENCE'
  | 'PRACTICE_SEARCH'
  | 'PRACTICE_PROFILE'
  | 'CUSTOMER_PROFILE'
  | 'TAX_SERVICE'
  | 'REQUIREMENT'
  | 'MATCHING'
  | 'ENQUIRY'
  | 'REVIEWS'
  | 'ACCOUNT'
  | 'PERFORMANCE'
  | 'OTHER'
  | 'PRACTICE_LOCATIONS'
  | 'EMPLOYEE_MANAGEMENT'
  | 'CUSTOMER_MANAGEMENT'
  | 'ENQUIRIES'
  | 'TAX_SERVICES'
  | 'MARKETPLACE'
  | 'CUSTOMER_MATCHING'
  | 'NOTIFICATIONS'
  | 'DOCUMENTS'
  | 'REPORTS'
  | 'BILLING'
  | 'CUSTOMER_REQUIREMENTS'
  | 'TASKS'
  | 'PRACTICE_OPERATIONS'
  | 'SEARCH';

export type ApplicationFeedbackActorType = 'CUSTOMER' | 'PRACTITIONER' | 'PRACTICE_EMPLOYEE';
export type ApplicationFeedbackContextType = 'CUSTOMER_PORTAL' | 'PRACTICE_PORTAL';
export type ApplicationFeedbackStatus = 'NEW' | 'UNDER_REVIEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'ESCALATED' | 'RESOLVED' | 'CLOSED' | 'REJECTED' | 'DUPLICATE';
export type ApplicationFeedbackPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type FeedbackTeam = 'CUSTOMER_SUPPORT' | 'OPERATIONS' | 'MARKETPLACE' | 'PRODUCT' | 'ENGINEERING' | 'MARKETING' | 'FINANCE';
export type FeedbackNoteVisibility = 'INTERNAL' | 'CUSTOMER_VISIBLE';
export type EngineeringIssueStatus = 'OPEN' | 'IN_PROGRESS' | 'FIXED' | 'CLOSED';

export interface CreateApplicationFeedbackRequest {
  type: ApplicationFeedbackType;
  category: ApplicationFeedbackCategory;
  rating?: number;
  title: string;
  description: string;
}

export interface ApplicationFeedback extends CreateApplicationFeedbackRequest {
  id: string;
  actorType?: ApplicationFeedbackActorType;
  contextType?: ApplicationFeedbackContextType;
  practiceId?: string;
  page?: string;
  feature?: string;
  source?: string;
  status?: ApplicationFeedbackStatus;
  priority?: ApplicationFeedbackPriority;
  createdAt: string;
}

// Admin Feedback Management Interfaces
export interface AdminApplicationFeedbackSummary {
  id: string;
  feedbackCode: string;
  actorType: ApplicationFeedbackActorType;
  contextType: ApplicationFeedbackContextType;
  type: ApplicationFeedbackType;
  category: ApplicationFeedbackCategory;
  rating?: number;
  title: string;
  descriptionExcerpt: string;
  page?: string;
  feature?: string;
  status: ApplicationFeedbackStatus;
  priority: ApplicationFeedbackPriority;
  assignedTeam?: FeedbackTeam;
  assignedUserId?: string;
  assignedUserName?: string;
  practiceId?: string;
  practiceName?: string;
  reporterName?: string;
  reporterEmail?: string;
  hasEngineeringIssue: boolean;
  engineeringIssueCode?: string;
  hasDuplicateOf: boolean;
  duplicateOfId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface FeedbackAssignment {
  id: string;
  feedbackId: string;
  team: FeedbackTeam;
  assignedUserId?: string;
  assignedUserName?: string;
  assignedUserEmail?: string;
  assignedBy?: string;
  assignedByName?: string;
  reason?: string;
  assignedAt: string;
  unassignedAt?: string;
  active: boolean;
}

export interface FeedbackNote {
  id: string;
  feedbackId: string;
  authorId?: string;
  authorName?: string;
  note: string;
  visibility: FeedbackNoteVisibility;
  createdAt: string;
}

export interface FeedbackStatusHistory {
  id: string;
  feedbackId: string;
  oldStatus?: ApplicationFeedbackStatus;
  newStatus: ApplicationFeedbackStatus;
  changedBy?: string;
  changedByName?: string;
  reason?: string;
  createdAt: string;
}

export interface EngineeringIssue {
  id: string;
  feedbackId: string;
  issueCode: string;
  title: string;
  description: string;
  priority: ApplicationFeedbackPriority;
  status: EngineeringIssueStatus;
  assignedTeam: string;
  createdBy?: string;
  createdByName?: string;
  externalSystem?: string;
  externalIssueId?: string;
  externalIssueUrl?: string;
  externalStatus?: string;
  lastSyncedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface AdminApplicationFeedbackDetail {
  id: string;
  feedbackCode: string;
  userId: string;
  reporterName: string;
  reporterEmail?: string;
  reporterPhone?: string;
  actorType: ApplicationFeedbackActorType;
  practiceId?: string;
  practiceName?: string;
  practiceEmail?: string;
  practiceSubscriptionPlan?: string;
  contextType: ApplicationFeedbackContextType;
  type: ApplicationFeedbackType;
  category: ApplicationFeedbackCategory;
  rating?: number;
  title: string;
  description: string;
  page?: string;
  feature?: string;
  source: string;
  status: ApplicationFeedbackStatus;
  priority: ApplicationFeedbackPriority;
  assignedTeam?: FeedbackTeam;
  assignedUserId?: string;
  assignedUserName?: string;
  activeAssignment?: FeedbackAssignment;
  assignmentHistory: FeedbackAssignment[];
  duplicateOfId?: string;
  duplicateOfTitle?: string;
  resolutionNote?: string;
  resolvedBy?: string;
  resolvedByName?: string;
  resolvedAt?: string;
  closedBy?: string;
  closedByName?: string;
  closedAt?: string;
  engineeringIssue?: EngineeringIssue;
  notes: FeedbackNote[];
  timeline: FeedbackStatusHistory[];
  createdAt: string;
  updatedAt?: string;
}

export interface AdminAssignee {
  userId: string;
  name: string;
  email: string;
  role: string;
}

export interface AdminFeedbackStats {
  totalCount: number;
  newCount: number;
  underReviewCount: number;
  assignedCount: number;
  inProgressCount: number;
  escalatedCount: number;
  resolvedCount: number;
  closedCount: number;
  rejectedCount: number;
  duplicateCount: number;
  criticalCount: number;
  highCount: number;
}

export interface AssignFeedbackRequest {
  team: FeedbackTeam;
  assignedUserId?: string;
  reason?: string;
}

export interface CreateFeedbackNoteRequest {
  note: string;
  visibility?: FeedbackNoteVisibility;
}

export interface UpdateFeedbackPriorityRequest {
  priority: ApplicationFeedbackPriority;
  reason?: string;
}

export interface ResolveFeedbackRequest {
  resolutionNote: string;
}

export interface CloseFeedbackRequest {
  reason?: string;
}

export interface RejectFeedbackRequest {
  reason: string;
}

export interface MarkDuplicateFeedbackRequest {
  duplicateOfId: string;
  reason?: string;
}

export interface EscalateToEngineeringRequest {
  title: string;
  description: string;
  priority: ApplicationFeedbackPriority;
  internalNotes?: string;
}

// 18. Platform SuperAdmin Operations & Dashboard
export interface PlatformKpis {
  activePractices: number;
  totalPractices: number;
  activeUsers: number;
  totalUsers: number;
  activeCustomers: number;
  totalMarketplaceLeads: number;
  activeSubscriptions: number;
  openFeedback: number;
  platformStatus: string;
  monthlyRecurringRevenue: number;
  annualRecurringRevenue: number;
}

export interface PracticeEcosystem {
  totalPractices: number;
  activePractices: number;
  pendingVerification: number;
  inactivePractices: number;
  suspendedPractices: number;
  newPracticesThisMonth: number;
}

export interface UserEcosystem {
  totalUsers: number;
  activeUsers: number;
  customers: number;
  practitioners: number;
  practiceEmployees: number;
  taxorynAdminUsers: number;
}

export interface MarketplaceFunnel {
  totalRequirements: number;
  activeRequirements: number;
  matchedRequirements: number;
  totalEnquiries: number;
  acceptedEnquiries: number;
  completedServices: number;
  conversionRate: number;
}

export interface SubscriptionMetrics {
  totalSubscriptions: number;
  starterTiers: number;
  professionalTiers: number;
  businessTiers: number;
  enterpriseTiers: number;
  activeTiers: number;
  trialOrFreeTiers: number;
  estimatedMrr: number;
  estimatedArr: number;
}

export interface FeedbackOperations {
  totalFeedback: number;
  newFeedback: number;
  underReview: number;
  assigned: number;
  inProgress: number;
  escalatedToEng: number;
  resolved: number;
  criticalOpen: number;
  topCategories: Record<string, number>;
}

export interface PlatformSummary {
  activePractices: number;
  totalPractices: number;
  platformUsers: number;
  marketplaceCustomers: number;
  activeSubscriptions: number;
}

export interface PlatformMarketplace {
  newRequirements: number;
  activeEnquiries: number;
  matchesCompleted: number;
  consultationsBooked: number;
}

export interface PlatformAttention {
  pendingPracticeVerification: number;
  openFeedback: number;
  securityAlerts: number;
  paymentIssues: number;
  marketplaceIssues: number;
}

export interface RecentPlatformActivity {
  id: string;
  displayTitle: string;
  description: string;
  targetDisplayName: string;
  timestamp: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS';
  status: 'SUCCESS' | 'PENDING' | 'ALERT';
  navigationTarget: string;
}

export interface PlatformHealth {
  api?: string;
  database?: string;
  backgroundJobs?: string;
  marketplace?: string;
  notifications?: string;
  apiGatewayStatus: string;
  databaseStatus: string;
  authServiceStatus: string;
  marketplaceStatus: string;
  feedbackSubsystemStatus: string;
  backgroundJobsStatus: string;
  activeDbConnections: number;
  maxDbConnections: number;
  systemCpuLoad: number;
  usedMemoryMb: number;
  maxMemoryMb: number;
  uptimeSeconds: number;
}

export interface RecentAdminActivity {
  id: string;
  action: string;
  entityType: string;
  entityId?: string;
  userEmail: string;
  description: string;
  timestamp: string;
  status: string;
}

export interface PlatformDashboardSummary {
  summary?: PlatformSummary;
  marketplace?: PlatformMarketplace;
  attention?: PlatformAttention;
  health?: PlatformHealth;
  recentActivity?: RecentPlatformActivity[];
  kpis: PlatformKpis;
  practiceEcosystem: PracticeEcosystem;
  userEcosystem: UserEcosystem;
  marketplaceFunnel: MarketplaceFunnel;
  subscriptionMetrics: SubscriptionMetrics;
  feedbackOperations: FeedbackOperations;
  platformHealth: PlatformHealth;
  recentActivities: RecentAdminActivity[];
}

export interface SupportKpis {
  openCases: number;
  waitingForCustomer: number;
  highPriority: number;
  unresolvedFeedback: number;
  resolvedThisMonth: number;
}

export interface SupportAttentionItem {
  id: string;
  title: string;
  description: string;
  priority: string;
  status: string;
  actionTarget: string;
  actionLabel: string;
}

export interface RecentSupportActivity {
  id: string;
  title: string;
  description: string;
  actor: string;
  target: string;
  timestamp: string;
  status: string;
  severity: string;
  navigationTarget: string;
}

export interface SupportDashboardSummary {
  kpis: SupportKpis;
  supportAttention: SupportAttentionItem[];
  recentActivity: RecentSupportActivity[];
}

