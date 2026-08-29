import { apiClient } from './client';
import {
  ApiResponse,
  PagedResponse,
  OrganizationDashboard,
  Client,
  Task,
  GstProfile,
  GstReturnFiling,
  BulkItrImportResult,
  ItrProfile,
  ItrReturn,
  CalendarEvent,
  DocumentItem,
  Invoice,
  BulkCreateInvoicesRequest,
  BulkInvoiceResult,
  BillingDashboardStats,
  SubscriptionPlan,
  SubscriptionInfo,
  Employee,
  Role,
  AuditLog,
  AuthTokens,
  ClientPortalDashboard,
  ClientPortalProfile,
  ClientGstStatus,
  ClientItrStatus,
  ClientDocumentRequest,
  DocumentRequest,
  DocumentRequestItem,
  CreateDocumentRequest,
  CreateDocumentRequestItem,
  DocumentRequestSummary,
  ClientPortalUser,
  RegisterClientPortalUserRequest,
  TdsProfile,
  TdsReturn,
  TdsChallan,
  TdsDeducteeEntry,
  TdsCertificate,
  TdsSectionRate,
  TdsComputationRequest,
  TdsComputationResult,
  TdsWorkloadDashboard,
  BulkTdsProfileImportResult,
  BulkTdsReturnImportResult,
  MarketplaceProfile,
  ProfileCompleteness,
  MarketplaceService,
  MarketplaceLead,
  MarketplaceConsultation,
  MarketplaceReview,
  MarketplaceVerification,
  MarketplaceStats,
  MarketplaceProposal,
  MarketplaceOnboarding,
  OnboardingDocument,
  CreateProposalRequest,
  AcceptProposalRequest,
  InitiateOnboardingRequest,
  UpdateOnboardingDetailsRequest,
  SignEngagementLetterRequest,
  VerifyOnboardingDocumentRequest,
  ApproveAndPromoteClientRequest,
  RegisterCustomerRequest,
  CustomerAuthResponse,
  CustomerProfile,
  UpdateCustomerProfileRequest,
  CustomerDashboard,
  PracticeLocation,
  PublicPracticeLocation,
  CreatePracticeLocationRequest,
  UpdatePracticeLocationRequest,
  TaxServiceCategory,
  CreateTaxServiceCategoryRequest,
  UpdateTaxServiceCategoryRequest,
  TaxService,
  CreateTaxServiceRequest,
  UpdateTaxServiceRequest,
  TaxServiceAlias,
  CreateTaxServiceAliasRequest,
  PublicTaxService,
  PublicTaxServiceCategory,
  PracticeService,
  UpdatePracticeServicesRequest,
  CustomerTaxpayerType,
  TaxRequirementStatus,
  CustomerTaxRequirement,
  CustomerTaxRequirementSummary,
  CreateTaxRequirementRequest,
  UpdateTaxRequirementRequest,
  FinancialYearOption,
  EarlyEnquiryView,
  EnquiryDetail,
  AcceptEnquiryRequest,
  RejectEnquiryRequest,
  AssignEnquiryRequest,
  CancelEnquiryRequest,
  SubmitEnquiryReviewRequest,
  EnquiryMessage,
  SendEnquiryMessageRequest,
  EnquiryMessageThread,
  CreateMarketplaceLeadRequest,
  ApplicationFeedback,
  CreateApplicationFeedbackRequest,
  AdminApplicationFeedbackSummary,
  AdminApplicationFeedbackDetail,
  FeedbackAssignment,
  FeedbackNote,
  FeedbackStatusHistory,
  EngineeringIssue,
  AdminAssignee,
  AdminFeedbackStats,
  AssignFeedbackRequest,
  CreateFeedbackNoteRequest,
  UpdateFeedbackPriorityRequest,
  ResolveFeedbackRequest,
  CloseFeedbackRequest,
  RejectFeedbackRequest,
  MarkDuplicateFeedbackRequest,
  EscalateToEngineeringRequest,
  FeedbackTeam,
  Organization,
  User,
  PlatformDashboardSummary,
  SupportDashboardSummary,
  LearnContentSummary,
  LearnContentDetail,
  LearnPublicCategory,
  ContentDashboardStats,
  ContentVersion,
  MediaAsset,
  WhatsAppMessageRecord,
  WhatsAppIntegrationStatus,
  NotificationItem,
  NotificationFilterParams,
  UnreadCountResponse,
  WorklistSummary,
  TaskWorklistParams,
  ComplianceObligation,
  ComplianceDashboardStats,
} from '../types';

// --- 1. Authentication ---
export const authApi = {
  login: async (credentials: { email: string; passwordHash?: string; password?: string }) => {
    const res = await apiClient.post<ApiResponse<AuthTokens>>('/v1/auth/login', credentials);
    return res.data.data;
  },
  registerOrg: async (payload: any) => {
    const res = await apiClient.post<ApiResponse<AuthTokens>>('/v1/auth/register-organization', payload);
    return res.data.data;
  },
  logout: async (refreshToken?: string | null) => {
    const res = await apiClient.post<ApiResponse<void>>('/v1/auth/logout', {
      refreshToken: refreshToken || undefined,
    });
    return res.data;
  },
  changePassword: async (payload: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
    const res = await apiClient.post<ApiResponse<void>>('/v1/auth/change-password', payload);
    return res.data;
  },
  forgotPassword: async (email: string) => {
    const res = await apiClient.post<ApiResponse<void>>('/v1/auth/forgot-password', { email });
    return res.data;
  },
  resetPassword: async (payload: { token: string; newPassword: string }) => {
    const res = await apiClient.post<ApiResponse<void>>('/v1/auth/reset-password', payload);
    return res.data;
  },
};

// --- 2. Organization Dashboard ---
export const dashboardApi = {
  getOrganizationDashboard: async () => {
    const res = await apiClient.get<ApiResponse<OrganizationDashboard>>('/v1/dashboard');
    return res.data.data;
  },
};

// --- 3. Clients ---
export const clientApi = {
  getAll: async (params?: { page?: number; size?: number; status?: string; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Client>>>('/v1/clients', { params });
    return res.data.data;
  },
  getById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<Client>>(`/v1/clients/${id}`);
    return res.data.data;
  },
  create: async (payload: Partial<Client>) => {
    const res = await apiClient.post<ApiResponse<Client>>('/v1/clients', payload);
    return res.data.data;
  },
  update: async (id: string, payload: Partial<Client>) => {
    const res = await apiClient.put<ApiResponse<Client>>(`/v1/clients/${id}`, payload);
    return res.data.data;
  },
  delete: async (id: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/clients/${id}`);
    return res.data;
  },
  updateStatus: async (id: string, status: string) => {
    const res = await apiClient.patch<ApiResponse<Client>>(`/v1/clients/${id}/status`, { status });
    return res.data.data;
  },
  bulkImport: async (clients: Partial<Client>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/clients/bulk', clients);
    return res.data.data;
  },
};

// --- 4. Tasks ---
export const taskApi = {
  getAll: async (params?: {
    page?: number;
    size?: number;
    status?: string;
    clientId?: string;
    assignedTo?: string;
    myTasksOnly?: boolean;
    search?: string;
    taskCategory?: string;
    priority?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Task>>>('/v1/tasks', { params: { size: 100, ...params } });
    return res.data.data;
  },
  getById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<Task>>(`/v1/tasks/${id}`);
    return res.data.data;
  },
  create: async (payload: Partial<Task>) => {
    const res = await apiClient.post<ApiResponse<Task>>('/v1/tasks', payload);
    return res.data.data;
  },
  update: async (id: string, payload: Partial<Task>) => {
    const res = await apiClient.put<ApiResponse<Task>>(`/v1/tasks/${id}`, payload);
    return res.data.data;
  },
  updateStatus: async (id: string, status: string) => {
    const res = await apiClient.put<ApiResponse<Task>>(`/v1/tasks/${id}`, { status });
    return res.data.data;
  },
  delete: async (id: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/tasks/${id}`);
    return res.data.data;
  },
  generateBulk: async (payload: {
    clientIds: string[];
    assignedTo?: string;
    title: string;
    description?: string;
    taskCategory: string;
    priority: string;
    dueDate?: string;
  }) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/tasks/bulk-generator', payload);
    return res.data.data;
  },
  bulkImport: async (tasks: Partial<Task>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/tasks/bulk', tasks);
    return res.data.data;
  },
  getWorklist: async (params?: TaskWorklistParams) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Task>>>('/v1/tasks/worklist', { params: { size: 100, ...params } });
    return res.data.data;
  },
  getWorklistSummary: async () => {
    const res = await apiClient.get<ApiResponse<WorklistSummary>>('/v1/tasks/worklist/summary');
    return res.data.data;
  },
};

// --- 5. GST Compliance ---
export const gstApi = {
  getProfiles: async (params?: { clientId?: string; search?: string; status?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<GstProfile>>>('/v1/gst/profiles', { params });
    return res.data.data;
  },
  createProfile: async (payload: Partial<GstProfile>) => {
    const res = await apiClient.post<ApiResponse<GstProfile>>('/v1/gst/profiles', payload);
    return res.data.data;
  },
  bulkImportProfiles: async (profiles: Partial<GstProfile>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/gst/profiles/bulk', profiles);
    return res.data.data;
  },
  getFilings: async (params?: { returnPeriod?: string; returnType?: string; filingStatus?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<GstReturnFiling>>>('/v1/gst/filings', { params });
    return res.data.data;
  },
  createFiling: async (payload: Partial<GstReturnFiling>) => {
    const res = await apiClient.post<ApiResponse<GstReturnFiling>>('/v1/gst/filings', payload);
    return res.data.data;
  },
  updateFilingStatus: async (id: string, payload: { filingStatus: string; filingDate?: string; acknowledgementNumber?: string; totalTaxableValue?: number; totalTaxLiability?: number; totalItcClaimed?: number; notes?: string }) => {
    const res = await apiClient.patch<ApiResponse<GstReturnFiling>>(`/v1/gst/filings/${id}/status`, payload);
    return res.data.data;
  },
  recordFiling: async (id: string, payload: { filingDate: string; acknowledgementNumber: string; filingStatus?: string }) => {
    try {
      const res = await apiClient.patch<ApiResponse<GstReturnFiling>>(`/v1/gst/filings/${id}/status`, {
        filingStatus: payload.filingStatus || 'FILED',
        filingDate: payload.filingDate,
        acknowledgementNumber: payload.acknowledgementNumber,
      });
      return res.data.data;
    } catch {
      const res = await apiClient.post<ApiResponse<GstReturnFiling>>(`/v1/gst/filings/${id}/file`, {
        filingStatus: payload.filingStatus || 'FILED',
        filingDate: payload.filingDate,
        acknowledgementNumber: payload.acknowledgementNumber,
      });
      return res.data.data;
    }
  },
  batchGenerateFilings: async (payload: { returnPeriod: string; returnType?: string; returnTypes?: string[]; financialYear: string; dueDate?: string; gstr1DueDate?: string; gstr3bDueDate?: string; cmp08DueDate?: string }) => {
    const res = await apiClient.post<ApiResponse<GstReturnFiling[]>>('/v1/gst/filings/batch-generate', payload);
    return res.data.data;
  },
  bulkImportFilings: async (filings: Partial<GstReturnFiling>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/gst/filings/bulk', filings);
    return res.data.data;
  },
  getWorkloadDashboard: async (period: string, assignedEmployeeId?: string) => {
    const res = await apiClient.get<ApiResponse<any>>('/v1/gst/dashboard/workload', {
      params: { period, assignedEmployeeId },
    });
    return res.data.data;
  },
};

// --- 6. ITR Compliance ---
export const itrApi = {
  getProfiles: async (params?: { clientId?: string; page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ItrProfile>>>('/v1/itr/profiles', { params });
    return res.data.data;
  },
  createProfile: async (payload: Partial<ItrProfile>) => {
    const res = await apiClient.post<ApiResponse<ItrProfile>>('/v1/itr/profiles', payload);
    return res.data.data;
  },
  bulkImportProfiles: async (profiles: Partial<ItrProfile>[]) => {
    try {
      const res = await apiClient.post<ApiResponse<BulkItrImportResult>>('/v1/itr/profiles/bulk', profiles);
      return res.data.data;
    } catch {
      // Resilient Sequential Fallback to POST /v1/itr/profiles
      const result: BulkItrImportResult = {
        totalProcessed: profiles.length,
        totalCreated: 0,
        totalSkipped: 0,
        totalFailed: 0,
        importedItems: [],
        errors: [],
      };
      for (const p of profiles) {
        try {
          const created = await itrApi.createProfile(p);
          result.totalCreated++;
          result.importedItems.push(created.clientName || created.pan);
        } catch (err: any) {
          const msg = err.response?.data?.message || err.message;
          if (msg?.toLowerCase().includes('already exists') || msg?.toLowerCase().includes('duplicate')) {
            result.totalSkipped++;
          } else {
            result.totalFailed++;
            result.errors.push(`${p.pan}: ${msg}`);
          }
        }
      }
      return result;
    }
  },
  getReturns: async (params?: { assessmentYear?: string; status?: string; itrType?: string; clientId?: string; page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ItrReturn>>>('/v1/itr/returns', { params });
    return res.data.data;
  },
  createReturn: async (payload: Partial<ItrReturn>) => {
    const res = await apiClient.post<ApiResponse<ItrReturn>>('/v1/itr/returns', payload);
    return res.data.data;
  },
  bulkImportReturns: async (returns: Partial<ItrReturn>[]) => {
    try {
      const res = await apiClient.post<ApiResponse<BulkItrImportResult>>('/v1/itr/returns/bulk', returns);
      return res.data.data;
    } catch {
      // Resilient Sequential Fallback to POST /v1/itr/returns
      const result: BulkItrImportResult = {
        totalProcessed: returns.length,
        totalCreated: 0,
        totalSkipped: 0,
        totalFailed: 0,
        importedItems: [],
        errors: [],
      };
      for (const r of returns) {
        try {
          const created = await itrApi.createReturn(r);
          result.totalCreated++;
          result.importedItems.push(`${created.clientName} (AY ${created.assessmentYear} - ${created.itrType})`);
        } catch (err: any) {
          const msg = err.response?.data?.message || err.message;
          if (msg?.toLowerCase().includes('already exists') || msg?.toLowerCase().includes('duplicate')) {
            result.totalSkipped++;
          } else {
            result.totalFailed++;
            result.errors.push(`${r.pan}: ${msg}`);
          }
        }
      }
      return result;
    }
  },
  batchGenerateReturns: async (payload: { assessmentYear: string; financialYear: string; itrTypes?: string[]; nonAuditDueDate?: string; auditDueDate?: string }) => {
    try {
      const res = await apiClient.post<ApiResponse<ItrReturn[]>>('/v1/itr/returns/batch-generate', payload);
      return res.data.data;
    } catch {
      // Resilient Sequential Fallback
      const profRes = await itrApi.getProfiles({ size: 500 }).catch(() => ({ content: [] }));
      const profiles = profRes.content || [];
      const createdList: ItrReturn[] = [];
      for (const p of profiles) {
        try {
          const isAudit = p.taxpayerType === 'COMPANY' || p.taxpayerType === 'LLP' || p.defaultItrType === 'ITR_6';
          const dueDate = isAudit ? (payload.auditDueDate || '2026-10-31') : (payload.nonAuditDueDate || '2026-07-31');
          const ret = await itrApi.createReturn({
            clientId: p.clientId,
            pan: p.pan,
            assessmentYear: payload.assessmentYear,
            financialYear: payload.financialYear,
            itrType: p.defaultItrType || 'ITR_1',
            taxpayerType: p.taxpayerType || 'INDIVIDUAL',
            dueDate: dueDate,
            status: 'DOCUMENTS_PENDING',
          });
          createdList.push(ret);
        } catch {
          // ignore duplicate
        }
      }
      return createdList;
    }
  },
  updateReturnStatus: async (id: string, payload: { status: string; acknowledgementNumber?: string; verificationDate?: string; notes?: string }) => {
    const res = await apiClient.patch<ApiResponse<ItrReturn>>(`/v1/itr/returns/${id}/status`, payload);
    return res.data.data;
  },
  recordFilingDetails: async (id: string, payload: { filingDate: string; acknowledgementNumber: string; verificationDate?: string; notes?: string }) => {
    try {
      const res = await apiClient.post<ApiResponse<ItrReturn>>(`/v1/itr/returns/${id}/filing-details`, payload);
      return res.data.data;
    } catch {
      const res = await apiClient.patch<ApiResponse<ItrReturn>>(`/v1/itr/returns/${id}/status`, {
        status: payload.verificationDate ? 'COMPLETED' : 'FILED',
        acknowledgementNumber: payload.acknowledgementNumber,
        verificationDate: payload.verificationDate,
        notes: payload.notes,
      });
      return res.data.data;
    }
  },
  seedDemo: async () => {
    const res = await apiClient.post<ApiResponse<ItrReturn[]>>('/v1/itr/seed-demo');
    return res.data.data;
  },
  getWorkloadDashboard: async (assessmentYear?: string, assignedEmployeeId?: string) => {
    const res = await apiClient.get<ApiResponse<any>>('/v1/itr/dashboard/workload', {
      params: { assessmentYear, assignedEmployeeId },
    });
    return res.data.data;
  },
};

// --- 6B. TDS & TCS Practice Management ---
export const tdsApi = {
  // Profiles (TAN Master)
  getProfiles: async (params?: { deductorType?: string; status?: string; clientId?: string; page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<TdsProfile>>>('/v1/tds/profiles', { params });
    return res.data.data;
  },
  getProfileById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<TdsProfile>>(`/v1/tds/profiles/${id}`);
    return res.data.data;
  },
  getProfileByClientId: async (clientId: string) => {
    const res = await apiClient.get<ApiResponse<TdsProfile>>(`/v1/tds/profiles/clients/${clientId}`);
    return res.data.data;
  },
  createProfile: async (payload: Partial<TdsProfile>) => {
    const res = await apiClient.post<ApiResponse<TdsProfile>>('/v1/tds/profiles', payload);
    return res.data.data;
  },
  updateProfile: async (id: string, payload: Partial<TdsProfile>) => {
    const res = await apiClient.put<ApiResponse<TdsProfile>>(`/v1/tds/profiles/${id}`, payload);
    return res.data.data;
  },
  bulkImportProfiles: async (profiles: Partial<TdsProfile>[]) => {
    try {
      const res = await apiClient.post<ApiResponse<BulkTdsProfileImportResult>>('/v1/tds/profiles/bulk', profiles);
      return res.data.data;
    } catch {
      const result: BulkTdsProfileImportResult = {
        totalProcessed: profiles.length,
        totalCreated: 0,
        totalSkipped: 0,
        totalFailed: 0,
        importedProfiles: [],
        errorMessages: [],
      };
      for (const p of profiles) {
        try {
          const created = await tdsApi.createProfile(p);
          result.totalCreated++;
          result.importedProfiles.push(created);
        } catch (err: any) {
          const msg = err.response?.data?.message || err.message;
          if (msg?.toLowerCase().includes('already exists') || msg?.toLowerCase().includes('duplicate')) {
            result.totalSkipped++;
          } else {
            result.totalFailed++;
            result.errorMessages.push(`${p.tan}: ${msg}`);
          }
        }
      }
      return result;
    }
  },

  // Returns Lifecycle
  getReturns: async (params?: { formType?: string; quarter?: string; financialYear?: string; filingStatus?: string; clientId?: string; tdsProfileId?: string; assignedEmployeeId?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<TdsReturn>>>('/v1/tds/returns', { params });
    return res.data.data;
  },
  getReturnById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<TdsReturn>>(`/v1/tds/returns/${id}`);
    return res.data.data;
  },
  createReturn: async (payload: Partial<TdsReturn>) => {
    const res = await apiClient.post<ApiResponse<TdsReturn>>('/v1/tds/returns', payload);
    return res.data.data;
  },
  updateReturn: async (id: string, payload: Partial<TdsReturn>) => {
    const res = await apiClient.put<ApiResponse<TdsReturn>>(`/v1/tds/returns/${id}`, payload);
    return res.data.data;
  },
  updateReturnStatus: async (id: string, payload: { filingStatus: string; filingDate?: string; tokenNumber?: string; receiptNumber?: string; notes?: string }) => {
    const res = await apiClient.patch<ApiResponse<TdsReturn>>(`/v1/tds/returns/${id}/status`, payload);
    return res.data.data;
  },
  recordFiling: async (id: string, payload: { filingDate: string; tokenNumber: string; receiptNumber?: string; notes?: string }) => {
    const res = await apiClient.post<ApiResponse<TdsReturn>>(`/v1/tds/returns/${id}/file`, payload);
    return res.data.data;
  },
  assignEmployee: async (id: string, payload: { employeeId: string }) => {
    const res = await apiClient.put<ApiResponse<TdsReturn>>(`/v1/tds/returns/${id}/assigned-employee`, payload);
    return res.data.data;
  },
  batchGenerateReturns: async (payload: { quarter: string; financialYear: string; formTypes?: string[]; assessmentYear?: string; dueDate?: string }) => {
    const res = await apiClient.post<ApiResponse<TdsReturn[]>>('/v1/tds/returns/batch-generate', payload);
    return res.data.data;
  },
  bulkImportReturns: async (returns: Partial<TdsReturn>[]) => {
    try {
      const res = await apiClient.post<ApiResponse<BulkTdsReturnImportResult>>('/v1/tds/returns/bulk', returns);
      return res.data.data;
    } catch {
      const result: BulkTdsReturnImportResult = {
        totalProcessed: returns.length,
        totalCreated: 0,
        totalSkipped: 0,
        totalFailed: 0,
        importedReturns: [],
        errorMessages: [],
      };
      for (const r of returns) {
        try {
          const created = await tdsApi.createReturn(r);
          result.totalCreated++;
          result.importedReturns.push(created);
        } catch (err: any) {
          const msg = err.response?.data?.message || err.message;
          if (msg?.toLowerCase().includes('already exists') || msg?.toLowerCase().includes('duplicate')) {
            result.totalSkipped++;
          } else {
            result.totalFailed++;
            result.errorMessages.push(`${r.formType} ${r.quarter}: ${msg}`);
          }
        }
      }
      return result;
    }
  },
  getUpcomingReturns: async (daysAhead: number = 30) => {
    const res = await apiClient.get<ApiResponse<TdsReturn[]>>('/v1/tds/returns/upcoming', { params: { daysAhead } });
    return res.data.data;
  },
  getOverdueReturns: async () => {
    const res = await apiClient.get<ApiResponse<TdsReturn[]>>('/v1/tds/returns/overdue');
    return res.data.data;
  },
  getClientReturnHistory: async (clientId: string) => {
    const res = await apiClient.get<ApiResponse<TdsReturn[]>>(`/v1/tds/clients/${clientId}/history`);
    return res.data.data;
  },
  seedDemo: async () => {
    const res = await apiClient.post<ApiResponse<TdsReturn[]>>('/v1/tds/seed-demo');
    return res.data.data;
  },

  // Challans ITNS 281
  getChallans: async (params?: { tdsProfileId?: string; quarter?: string; financialYear?: string; challanStatus?: string; sectionCode?: string; page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<TdsChallan>>>('/v1/tds/challans', { params });
    return res.data.data;
  },
  getChallanById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<TdsChallan>>(`/v1/tds/challans/${id}`);
    return res.data.data;
  },
  createChallan: async (payload: Partial<TdsChallan>) => {
    const res = await apiClient.post<ApiResponse<TdsChallan>>('/v1/tds/challans', payload);
    return res.data.data;
  },
  updateChallan: async (id: string, payload: Partial<TdsChallan>) => {
    const res = await apiClient.put<ApiResponse<TdsChallan>>(`/v1/tds/challans/${id}`, payload);
    return res.data.data;
  },

  // Deductees
  createDeducteeEntry: async (payload: Partial<TdsDeducteeEntry>) => {
    const res = await apiClient.post<ApiResponse<TdsDeducteeEntry>>('/v1/tds/deductees', payload);
    return res.data.data;
  },
  getDeducteesByProfile: async (profileId: string) => {
    const res = await apiClient.get<ApiResponse<TdsDeducteeEntry[]>>(`/v1/tds/profiles/${profileId}/deductees`);
    return res.data.data;
  },
  getDeducteesByReturn: async (returnId: string) => {
    const res = await apiClient.get<ApiResponse<TdsDeducteeEntry[]>>(`/v1/tds/returns/${returnId}/deductees`);
    return res.data.data;
  },

  // Form 16 / 16A Certificates
  createCertificate: async (payload: Partial<TdsCertificate>) => {
    const res = await apiClient.post<ApiResponse<TdsCertificate>>('/v1/tds/certificates', payload);
    return res.data.data;
  },
  getCertificatesByProfile: async (profileId: string) => {
    const res = await apiClient.get<ApiResponse<TdsCertificate[]>>(`/v1/tds/profiles/${profileId}/certificates`);
    return res.data.data;
  },
  updateCertificateStatus: async (id: string, payload: { dispatchStatus: string; certificateNumber?: string; notes?: string }) => {
    const res = await apiClient.patch<ApiResponse<TdsCertificate>>(`/v1/tds/certificates/${id}/status`, payload);
    return res.data.data;
  },

  // Dashboard & Rate Engine
  getWorkloadDashboard: async (quarter: string = 'Q1', financialYear: string = '2026-27', assignedEmployeeId?: string) => {
    const res = await apiClient.get<ApiResponse<TdsWorkloadDashboard>>('/v1/tds/dashboard/workload', {
      params: { quarter, financialYear, assignedEmployeeId },
    });
    return res.data.data;
  },
  computeTds: async (payload: TdsComputationRequest) => {
    const res = await apiClient.post<ApiResponse<TdsComputationResult>>('/v1/tds/calculator/compute', payload);
    return res.data.data;
  },
  getSectionRates: async () => {
    const res = await apiClient.get<ApiResponse<TdsSectionRate[]>>('/v1/tds/calculator/rates');
    return res.data.data;
  },
};

// --- 7. Compliance Calendar ---
export const calendarApi = {
  getEvents: async (params?: { fromDate?: string; toDate?: string; complianceType?: string }) => {
    const res = await apiClient.get<ApiResponse<CalendarEvent[]>>('/v1/compliance-calendar/events', { params });
    return res.data.data;
  },
};

export const complianceApi = {
  getCalendar: async (params?: {
    fromDate?: string;
    toDate?: string;
    period?: string;
    complianceType?: string;
    status?: string;
    clientId?: string;
    assignedEmployeeId?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ComplianceObligation>>>('/v1/compliance/calendar', { params: { size: 100, ...params } });
    return res.data.data;
  },
  getUpcoming: async (daysAhead: number = 30) => {
    const res = await apiClient.get<ApiResponse<ComplianceObligation[]>>('/v1/compliance/upcoming', { params: { daysAhead } });
    return res.data.data;
  },
  getOverdue: async () => {
    const res = await apiClient.get<ApiResponse<ComplianceObligation[]>>('/v1/compliance/overdue');
    return res.data.data;
  },
  getDueToday: async () => {
    const res = await apiClient.get<ApiResponse<ComplianceObligation[]>>('/v1/compliance/today');
    return res.data.data;
  },
  getDashboardStats: async () => {
    const res = await apiClient.get<ApiResponse<ComplianceDashboardStats>>('/v1/compliance/dashboard/stats');
    return res.data.data;
  },
  getObligationById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<ComplianceObligation>>(`/v1/compliance/obligations/${id}`);
    return res.data.data;
  },
  createObligation: async (payload: Partial<ComplianceObligation>) => {
    const res = await apiClient.post<ApiResponse<ComplianceObligation>>('/v1/compliance/obligations', payload);
    return res.data.data;
  },
  updateStatus: async (id: string, payload: { status: string; completionNotes?: string }) => {
    const res = await apiClient.patch<ApiResponse<ComplianceObligation>>(`/v1/compliance/obligations/${id}/status`, payload);
    return res.data.data;
  },
  assignEmployee: async (id: string, payload: { employeeId: string; remarks?: string }) => {
    const res = await apiClient.put<ApiResponse<ComplianceObligation>>(`/v1/compliance/obligations/${id}/assigned-employee`, payload);
    return res.data.data;
  },
  createTaskForObligation: async (id: string) => {
    const res = await apiClient.post<ApiResponse<ComplianceObligation>>(`/v1/compliance/obligations/${id}/create-task`);
    return res.data.data;
  },
};

// --- 8. Documents ---
export const documentApi = {
  getAll: async (params?: { clientId?: string; category?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<DocumentItem>>>('/v1/documents', { params });
    return res.data.data;
  },
  upload: async (formData: FormData) => {
    const res = await apiClient.post<ApiResponse<DocumentItem>>('/v1/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
  downloadUrl: (id: string) => `/api/v1/documents/${id}/download`,
  download: async (id: string) => {
    const res = await apiClient.get(`/v1/documents/${id}/download`, { responseType: 'blob' });
    return res.data as Blob;
  },
};

// --- 9. Billing & Invoices ---
export const billingApi = {
  getInvoices: async (params?: { clientId?: string; status?: string; page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Invoice>>>('/v1/invoices', { params: { size: 100, ...params } });
    return res.data.data;
  },
  getInvoiceById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<Invoice>>(`/v1/invoices/${id}`);
    return res.data.data;
  },
  createInvoice: async (payload: any) => {
    const res = await apiClient.post<ApiResponse<Invoice>>('/v1/invoices', payload);
    return res.data.data;
  },
  bulkCreateInvoices: async (payload: BulkCreateInvoicesRequest) => {
    try {
      const res = await apiClient.post<ApiResponse<BulkInvoiceResult>>('/v1/invoices/bulk', payload);
      return res.data.data;
    } catch {
      // Sequential fallback
      const result: BulkInvoiceResult = {
        totalProcessed: payload.clientIds?.length || 0,
        totalCreated: 0,
        totalSkipped: 0,
        totalFailed: 0,
        totalBilledAmount: 0,
        createdInvoices: [],
        errors: [],
      };
      if (payload.clientIds && payload.clientIds.length > 0) {
        for (const cid of payload.clientIds) {
          try {
            const created = await billingApi.createInvoice({
              clientId: cid,
              invoiceDate: payload.invoiceDate,
              dueDate: payload.dueDate,
              items: payload.items as any,
              notes: payload.notes,
              terms: payload.terms,
            });
            if (payload.autoIssue && created.id) {
              try { await billingApi.issueInvoice(created.id); } catch {}
            }
            result.totalCreated++;
            result.totalBilledAmount += Number(created.total || 0);
            result.createdInvoices.push(created);
          } catch (err: any) {
            result.totalFailed++;
            result.errors.push(`Client ${cid}: ${err.message}`);
          }
        }
      }
      return result;
    }
  },
  seedDemoInvoices: async () => {
    const res = await apiClient.post<ApiResponse<Invoice[]>>('/v1/invoices/seed-demo');
    return res.data.data;
  },
  issueInvoice: async (id: string) => {
    const res = await apiClient.post<ApiResponse<Invoice>>(`/v1/invoices/${id}/issue`);
    return res.data.data;
  },
  cancelInvoice: async (id: string) => {
    const res = await apiClient.post<ApiResponse<Invoice>>(`/v1/invoices/${id}/cancel`);
    return res.data.data;
  },
  recordPayment: async (invoiceId: string, payload: { amount: number; paymentMode: string; referenceNumber?: string; paymentDate: string; notes?: string }) => {
    const res = await apiClient.post<ApiResponse<any>>(`/v1/invoices/${invoiceId}/payments`, payload);
    return res.data.data;
  },
  sendReminder: async (id: string) => {
    const res = await apiClient.post<ApiResponse<void>>(`/v1/invoices/${id}/reminder`);
    return res.data;
  },
  getDashboardStats: async () => {
    const res = await apiClient.get<ApiResponse<BillingDashboardStats>>('/v1/invoices/dashboard/stats');
    return res.data.data;
  },
};

// --- 10. Subscriptions ---
export const subscriptionApi = {
  getPlans: async () => {
    const res = await apiClient.get<ApiResponse<SubscriptionPlan[]>>('/v1/subscriptions/plans');
    return res.data.data;
  },
  getCurrent: async () => {
    const res = await apiClient.get<ApiResponse<SubscriptionInfo>>('/v1/subscriptions/current');
    return res.data.data;
  },
  changePlan: async (payload: { plan: string; interval: string }) => {
    const res = await apiClient.post<ApiResponse<SubscriptionInfo>>('/v1/subscriptions/change-plan', payload);
    return res.data.data;
  },
};

// --- 11. Team & Roles ---
export const teamApi = {
  getEmployees: async (params?: { status?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Employee>>>('/v1/employees', { params });
    return res.data.data;
  },
  createEmployee: async (payload: Partial<Employee>) => {
    const res = await apiClient.post<ApiResponse<Employee>>('/v1/employees', payload);
    return res.data.data;
  },
  bulkImportEmployees: async (employees: Partial<Employee>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/employees/bulk', employees);
    return res.data.data;
  },
  getRoles: async () => {
    const res = await apiClient.get<ApiResponse<Role[]>>('/v1/roles');
    return res.data.data;
  },
};

export const employeeApi = {
  getAll: async (params?: { page?: number; size?: number; status?: string; department?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Employee>>>('/v1/employees', { params });
    return res.data.data;
  },
  create: async (payload: Partial<Employee>) => {
    const res = await apiClient.post<ApiResponse<Employee>>('/v1/employees', payload);
    return res.data.data;
  },
  bulkImport: async (employees: Partial<Employee>[]) => {
    const res = await apiClient.post<ApiResponse<any>>('/v1/employees/bulk', employees);
    return res.data.data;
  },
};

// --- 12. Audit Logs ---
export const auditApi = {
  getLogs: async (params?: {
    page?: number;
    size?: number;
    entityType?: string;
    action?: string;
    search?: string;
    status?: string;
    startDate?: string;
    endDate?: string;
    organizationId?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<AuditLog>>>('/v1/audit-logs', { params });
    return res.data.data;
  },
};

// --- 13. Client Portal ---
export const portalApi = {
  getDashboard: async () => {
    const res = await apiClient.get<ApiResponse<ClientPortalDashboard>>('/v1/portal/dashboard');
    return res.data.data;
  },
  getDashboardPreview: async (clientId: string) => {
    const res = await apiClient.get<ApiResponse<ClientPortalDashboard>>(`/v1/portal/preview/${clientId}`);
    return res.data.data;
  },
  getProfile: async () => {
    const res = await apiClient.get<ApiResponse<ClientPortalProfile>>('/v1/portal/profile');
    return res.data.data;
  },
  updateProfile: async (payload: Partial<ClientPortalProfile>) => {
    const res = await apiClient.put<ApiResponse<ClientPortalProfile>>('/v1/portal/profile', payload);
    return res.data.data;
  },
  getGstStatus: async () => {
    const res = await apiClient.get<ApiResponse<ClientGstStatus[]>>('/v1/portal/gst-status');
    return res.data.data;
  },
  getItrStatus: async () => {
    const res = await apiClient.get<ApiResponse<ClientItrStatus[]>>('/v1/portal/itr-status');
    return res.data.data;
  },
  getClientInvoices: async () => {
    const res = await apiClient.get<ApiResponse<Invoice[]>>('/v1/portal/invoices');
    return res.data.data;
  },
  getClientInvoiceById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<Invoice>>(`/v1/portal/invoices/${id}`);
    return res.data.data;
  },
  getClientDocuments: async () => {
    const res = await apiClient.get<ApiResponse<DocumentItem[]>>('/v1/portal/documents');
    return res.data.data;
  },
  getPendingDocuments: async () => {
    const res = await apiClient.get<ApiResponse<ClientDocumentRequest[]>>('/v1/portal/pending-documents');
    return res.data.data;
  },
  uploadDocument: async (file: File, metadata: { title: string; category: string; description?: string; clientId?: string }, documentRequestId?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );
    const res = await apiClient.post<ApiResponse<DocumentItem>>('/v1/portal/documents/upload', formData, {
      params: documentRequestId ? { documentRequestId } : undefined,
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
  getClientTasks: async () => {
    const res = await apiClient.get<ApiResponse<Task[]>>('/v1/portal/tasks');
    return res.data.data;
  },
  getClientNotifications: async () => {
    const res = await apiClient.get<ApiResponse<any[]>>('/v1/portal/notifications');
    return res.data.data;
  },
  markNotificationRead: async (id: string) => {
    const res = await apiClient.patch<ApiResponse<void>>(`/v1/portal/notifications/${id}/read`);
    return res.data.data;
  },
  registerUser: async (payload: RegisterClientPortalUserRequest) => {
    const res = await apiClient.post<ApiResponse<ClientPortalUser>>('/v1/portal/users', payload);
    return res.data.data;
  },
  getClientPortalUsers: async (clientId: string) => {
    const res = await apiClient.get<ApiResponse<ClientPortalUser[]>>(`/v1/portal/clients/${clientId}/users`);
    return res.data.data;
  },
  requestDocument: async (payload: { clientId: string; title: string; description?: string; documentType: string; dueDate?: string }) => {
    const res = await apiClient.post<ApiResponse<ClientDocumentRequest>>('/v1/portal/document-requests', payload);
    return res.data.data;
  },
};

// --- 13. Multi-Item Document Requests V1 ---
export const documentRequestApi = {
  getAll: async (params?: { page?: number; size?: number; clientId?: string; status?: string; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<DocumentRequest>>>('/v1/document-requests', { params });
    return res.data.data;
  },
  getById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<DocumentRequest>>(`/v1/document-requests/${id}`);
    return res.data.data;
  },
  getByClient: async (clientId: string) => {
    const res = await apiClient.get<ApiResponse<DocumentRequest[]>>(`/v1/document-requests/clients/${clientId}`);
    return res.data.data;
  },
  create: async (payload: CreateDocumentRequest) => {
    const res = await apiClient.post<ApiResponse<DocumentRequest>>('/v1/document-requests', payload);
    return res.data.data;
  },
  acceptItem: async (itemId: string) => {
    const res = await apiClient.post<ApiResponse<DocumentRequest>>(`/v1/document-requests/items/${itemId}/accept`);
    return res.data.data;
  },
  rejectItem: async (itemId: string, rejectionReason: string) => {
    const res = await apiClient.post<ApiResponse<DocumentRequest>>(`/v1/document-requests/items/${itemId}/reject`, {
      rejectionReason,
    });
    return res.data.data;
  },
  uploadItem: async (itemId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post<ApiResponse<DocumentRequest>>(`/v1/document-requests/items/${itemId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
  sendReminder: async (id: string) => {
    const res = await apiClient.post<ApiResponse<void>>(`/v1/document-requests/${id}/remind`);
    return res.data;
  },
  cancel: async (id: string) => {
    const res = await apiClient.post<ApiResponse<DocumentRequest>>(`/v1/document-requests/${id}/cancel`);
    return res.data.data;
  },
  getSummaryStats: async () => {
    const res = await apiClient.get<ApiResponse<DocumentRequestSummary>>('/v1/document-requests/summary/stats');
    return res.data.data;
  },

  // Client Portal Endpoints
  getPortalRequests: async () => {
    const res = await apiClient.get<ApiResponse<DocumentRequest[]>>('/v1/portal/document-requests/v1');
    return res.data.data;
  },
  getPortalRequestById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<DocumentRequest>>(`/v1/portal/document-requests/v1/${id}`);
    return res.data.data;
  },
  uploadPortalItem: async (itemId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post<ApiResponse<DocumentRequest>>(`/v1/portal/document-requests/v1/items/${itemId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
};

// --- 13. Customer Marketplace & Discovery (Public) ---
export const marketplacePublicApi = {
  search: async (params?: {
    city?: string;
    state?: string;
    pincode?: string;
    professionalType?: string;
    specialization?: string;
    service?: string;
    verifiedOnly?: boolean;
    verified?: boolean;
    minRating?: number;
    search?: string;
    q?: string;
    latitude?: number;
    longitude?: number;
    radiusKm?: number;
    page?: number;
    size?: number;
    sortBy?: string;
    sort?: string;
    sortDirection?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceProfile>>>('/v1/marketplace/search', { params });
    return res.data.data;
  },
  getFeatured: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceProfile[]>>('/v1/marketplace/featured');
    return res.data.data;
  },
  getById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceProfile>>(`/v1/marketplace/profiles/${id}`);
    return res.data.data;
  },
  getBySlug: async (slug: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceProfile>>(`/v1/marketplace/profiles/slug/${slug}`);
    return res.data.data;
  },
  getServices: async (profileId: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceService[]>>(`/v1/marketplace/profiles/${profileId}/services`);
    return res.data.data;
  },
  getReviews: async (profileId: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceReview[]>>(`/v1/marketplace/profiles/${profileId}/reviews`);
    return res.data.data;
  },
  submitLead: async (payload: CreateMarketplaceLeadRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceLead>>('/v1/marketplace/leads', payload);
    return res.data.data;
  },
  bookConsultation: async (payload: { marketplaceProfileId: string; clientName: string; clientEmail: string; clientPhone: string; topic: string; consultationMode?: string; bookingDate: string; startTime: string; endTime: string; notes?: string }) => {
    const res = await apiClient.post<ApiResponse<MarketplaceConsultation>>('/v1/marketplace/consultations', payload);
    return res.data.data;
  },
  submitReview: async (payload: { marketplaceProfileId: string; reviewerName: string; reviewerDesignation?: string; reviewerCompany?: string; rating: number; reviewTitle?: string; reviewComment: string; serviceTaken?: string }) => {
    const res = await apiClient.post<ApiResponse<MarketplaceReview>>('/v1/marketplace/reviews', payload);
    return res.data.data;
  },
  seedDemo: async () => {
    const res = await apiClient.post<ApiResponse<MarketplaceProfile[]>>('/v1/marketplace/seed-demo');
    return res.data.data;
  },
  // Controlled Tax Service Master Catalog
  getTaxServices: async () => {
    const res = await apiClient.get<ApiResponse<PublicTaxService[]>>('/v1/marketplace/tax-services');
    return res.data.data;
  },
  getTaxServiceCategories: async () => {
    const res = await apiClient.get<ApiResponse<PublicTaxServiceCategory[]>>('/v1/marketplace/tax-services/categories');
    return res.data.data;
  },
  resolveTaxService: async (query: string) => {
    const res = await apiClient.get<ApiResponse<PublicTaxService>>('/v1/marketplace/tax-services/resolve', { params: { query } });
    return res.data.data;
  },
};

// --- Controlled Tax Services Public Catalog API ---
export const taxServicePublicApi = {
  getServices: marketplacePublicApi.getTaxServices,
  getCategories: marketplacePublicApi.getTaxServiceCategories,
  resolve: marketplacePublicApi.resolveTaxService,
};

// --- 14. Practice Marketplace Management ---
export const marketplacePracticeApi = {
  getMyProfile: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceProfile>>('/v1/practice/marketplace/profile');
    return res.data.data;
  },
  createProfile: async (payload: Partial<MarketplaceProfile>) => {
    const res = await apiClient.post<ApiResponse<MarketplaceProfile>>('/v1/marketplace/practice-profile', payload);
    return res.data.data;
  },
  updateMyProfile: async (payload: Partial<MarketplaceProfile>) => {
    const res = await apiClient.put<ApiResponse<MarketplaceProfile>>('/v1/marketplace/practice-profile', payload);
    return res.data.data;
  },
  updateVisibility: async (visibility: string) => {
    const res = await apiClient.patch<ApiResponse<MarketplaceProfile>>('/v1/marketplace/practice-profile/visibility', { visibility });
    return res.data.data;
  },
  generateSlug: async (params?: { baseName?: string; city?: string }) => {
    const res = await apiClient.get<ApiResponse<string>>('/v1/practice/marketplace/profile/slug/generate', { params });
    return res.data.data;
  },
  getProfileCompleteness: async () => {
    const res = await apiClient.get<ApiResponse<ProfileCompleteness>>('/v1/practice/marketplace/profile/completeness');
    return res.data.data;
  },
  // Locations Management
  getLocations: async () => {
    const res = await apiClient.get<ApiResponse<PracticeLocation[]>>('/v1/marketplace/practice-profile/locations');
    return res.data.data;
  },
  getLocationById: async (locationId: string) => {
    const res = await apiClient.get<ApiResponse<PracticeLocation>>(`/v1/marketplace/practice-profile/locations/${locationId}`);
    return res.data.data;
  },
  createLocation: async (payload: CreatePracticeLocationRequest) => {
    const res = await apiClient.post<ApiResponse<PracticeLocation>>('/v1/marketplace/practice-profile/locations', payload);
    return res.data.data;
  },
  updateLocation: async (locationId: string, payload: UpdatePracticeLocationRequest) => {
    const res = await apiClient.put<ApiResponse<PracticeLocation>>(`/v1/marketplace/practice-profile/locations/${locationId}`, payload);
    return res.data.data;
  },
  setPrimaryLocation: async (locationId: string) => {
    const res = await apiClient.patch<ApiResponse<PracticeLocation>>(`/v1/marketplace/practice-profile/locations/${locationId}/primary`);
    return res.data.data;
  },
  activateLocation: async (locationId: string) => {
    const res = await apiClient.patch<ApiResponse<PracticeLocation>>(`/v1/marketplace/practice-profile/locations/${locationId}/activate`);
    return res.data.data;
  },
  deactivateLocation: async (locationId: string) => {
    const res = await apiClient.delete<ApiResponse<PracticeLocation>>(`/v1/marketplace/practice-profile/locations/${locationId}`);
    return res.data.data;
  },
  getMyServices: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceService[]>>('/v1/practice/marketplace/services');
    return res.data.data;
  },
  createService: async (payload: Partial<MarketplaceService>) => {
    const res = await apiClient.post<ApiResponse<MarketplaceService>>('/v1/practice/marketplace/services', payload);
    return res.data.data;
  },
  updateService: async (id: string, payload: Partial<MarketplaceService>) => {
    const res = await apiClient.put<ApiResponse<MarketplaceService>>(`/v1/practice/marketplace/services/${id}`, payload);
    return res.data.data;
  },
  deleteService: async (id: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/practice/marketplace/services/${id}`);
    return res.data;
  },
  getMyLeads: async (params?: { status?: string; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceLead>>>('/v1/practice/marketplace/leads', { params });
    return res.data.data;
  },
  getMyEarlyEnquiries: async (params?: { status?: string; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<EarlyEnquiryView>>>('/v1/practice/marketplace/enquiries', { params });
    return res.data.data;
  },
  getEarlyEnquiryById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<EarlyEnquiryView>>(`/v1/practice/marketplace/enquiries/${id}`);
    return res.data.data;
  },
  // Operational Lifecycle Enquiries
  getPracticeEnquiries: async (params?: { status?: string; assignedEmployeeId?: string; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<EnquiryDetail>>>('/v1/marketplace/practice-profile/lifecycle-enquiries', { params });
    return res.data.data;
  },
  getPracticeEnquiryDetail: async (id: string) => {
    const res = await apiClient.get<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}`);
    return res.data.data;
  },
  acceptEnquiry: async (id: string, payload?: AcceptEnquiryRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/accept`, payload || {});
    return res.data.data;
  },
  rejectEnquiry: async (id: string, payload: RejectEnquiryRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/reject`, payload);
    return res.data.data;
  },
  assignEnquiry: async (id: string, payload: AssignEnquiryRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/assign`, payload);
    return res.data.data;
  },
  startEnquiry: async (id: string) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/start`);
    return res.data.data;
  },
  completeEnquiry: async (id: string) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/complete`);
    return res.data.data;
  },
  cancelPracticeEnquiry: async (id: string, payload?: CancelEnquiryRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/cancel`, payload || {});
    return res.data.data;
  },
  getEnquiryMessages: async (id: string) => {
    const res = await apiClient.get<ApiResponse<EnquiryMessageThread>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/messages`);
    return res.data.data;
  },
  sendEnquiryMessage: async (id: string, payload: SendEnquiryMessageRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryMessage>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/messages`, payload);
    return res.data.data;
  },
  markMessagesRead: async (id: string) => {
    const res = await apiClient.post<ApiResponse<void>>(`/v1/marketplace/practice-profile/lifecycle-enquiries/${id}/messages/read`);
    return res.data;
  },
  updateLeadStatus: async (id: string, params: { status?: string; notes?: string; assignedEmployeeId?: string }) => {
    const res = await apiClient.patch<ApiResponse<MarketplaceLead>>(`/v1/practice/marketplace/leads/${id}/status`, null, { params });
    return res.data.data;
  },
  convertLeadToClient: async (id: string, payload: { clientType?: string; assignedEmployeeId?: string; createOnboardingTask?: boolean; notes?: string }) => {
    const res = await apiClient.post<ApiResponse<MarketplaceLead>>(`/v1/practice/marketplace/leads/${id}/convert-to-client`, payload);
    return res.data.data;
  },
  getMyConsultations: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceConsultation>>>('/v1/practice/marketplace/consultations', { params });
    return res.data.data;
  },
  updateConsultationStatus: async (id: string, params: { status?: string; meetingLink?: string; notes?: string }) => {
    const res = await apiClient.patch<ApiResponse<MarketplaceConsultation>>(`/v1/practice/marketplace/consultations/${id}/status`, null, { params });
    return res.data.data;
  },
  submitVerification: async (payload: { professionalBody: string; membershipNumber: string; copNumber?: string; firmRegistrationNumber?: string; documentUrl?: string }) => {
    const res = await apiClient.post<ApiResponse<MarketplaceVerification>>('/v1/practice/marketplace/verification', payload);
    return res.data.data;
  },
  getVerificationStatus: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceVerification>>('/v1/practice/marketplace/verification');
    return res.data.data;
  },
  getStats: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceStats>>('/v1/practice/marketplace/stats');
    return res.data.data;
  },
  // Controlled Tax Services Selection
  getControlledTaxServices: async () => {
    const res = await apiClient.get<ApiResponse<PracticeService[]>>('/v1/marketplace/practice-profile/tax-services');
    return res.data.data;
  },
  updateControlledTaxServices: async (taxServiceIds: string[]) => {
    const res = await apiClient.put<ApiResponse<PracticeService[]>>('/v1/marketplace/practice-profile/tax-services', { taxServiceIds });
    return res.data.data;
  },
  addControlledTaxService: async (taxServiceId: string) => {
    const res = await apiClient.post<ApiResponse<PracticeService>>(`/v1/marketplace/practice-profile/tax-services/${taxServiceId}`);
    return res.data.data;
  },
  removeControlledTaxService: async (taxServiceId: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/marketplace/practice-profile/tax-services/${taxServiceId}`);
    return res.data;
  },
};

// --- 15. Platform Admin Marketplace Governance ---
export const marketplaceAdminApi = {
  getPendingVerifications: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceVerification>>>('/v1/admin/marketplace/verifications/pending', { params });
    return res.data.data;
  },
  processVerification: async (id: string, payload: { verificationStatus: string; rejectionReason?: string }) => {
    const res = await apiClient.post<ApiResponse<MarketplaceVerification>>(`/v1/admin/marketplace/verifications/${id}/process`, payload);
    return res.data.data;
  },
  toggleFeatured: async (id: string, isFeatured: boolean) => {
    const res = await apiClient.patch<ApiResponse<MarketplaceProfile>>(`/v1/admin/marketplace/profiles/${id}/featured`, null, { params: { isFeatured } });
    return res.data.data;
  },
  togglePublish: async (id: string, isPublished: boolean) => {
    const res = await apiClient.patch<ApiResponse<MarketplaceProfile>>(`/v1/admin/marketplace/profiles/${id}/publish`, null, { params: { isPublished } });
    return res.data.data;
  },
  getPlatformStats: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceStats>>('/v1/admin/marketplace/stats');
    return res.data.data;
  },
};

// --- 16. Platform Admin Tax Service Master Governance ---
export const taxServiceAdminApi = {
  // Categories
  getCategories: async () => {
    const res = await apiClient.get<ApiResponse<TaxServiceCategory[]>>('/v1/admin/tax-services/categories');
    return res.data.data;
  },
  getCategoryById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<TaxServiceCategory>>(`/v1/admin/tax-services/categories/${id}`);
    return res.data.data;
  },
  createCategory: async (payload: CreateTaxServiceCategoryRequest) => {
    const res = await apiClient.post<ApiResponse<TaxServiceCategory>>('/v1/admin/tax-services/categories', payload);
    return res.data.data;
  },
  updateCategory: async (id: string, payload: UpdateTaxServiceCategoryRequest) => {
    const res = await apiClient.put<ApiResponse<TaxServiceCategory>>(`/v1/admin/tax-services/categories/${id}`, payload);
    return res.data.data;
  },
  toggleCategoryStatus: async (id: string, isActive: boolean) => {
    const res = await apiClient.patch<ApiResponse<TaxServiceCategory>>(`/v1/admin/tax-services/categories/${id}/status`, null, { params: { isActive } });
    return res.data.data;
  },
  // Tax Services Master
  getTaxServices: async (params?: { categoryId?: string; isActive?: boolean; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<TaxService>>>('/v1/admin/tax-services', { params });
    return res.data.data;
  },
  getTaxServiceById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<TaxService>>(`/v1/admin/tax-services/${id}`);
    return res.data.data;
  },
  createTaxService: async (payload: CreateTaxServiceRequest) => {
    const res = await apiClient.post<ApiResponse<TaxService>>('/v1/admin/tax-services', payload);
    return res.data.data;
  },
  updateTaxService: async (id: string, payload: UpdateTaxServiceRequest) => {
    const res = await apiClient.put<ApiResponse<TaxService>>(`/v1/admin/tax-services/${id}`, payload);
    return res.data.data;
  },
  toggleTaxServiceStatus: async (id: string, isActive: boolean) => {
    const res = await apiClient.patch<ApiResponse<TaxService>>(`/v1/admin/tax-services/${id}/status`, null, { params: { isActive } });
    return res.data.data;
  },
  // Aliases
  getAliases: async (taxServiceId: string) => {
    const res = await apiClient.get<ApiResponse<TaxServiceAlias[]>>(`/v1/admin/tax-services/${taxServiceId}/aliases`);
    return res.data.data;
  },
  addAlias: async (taxServiceId: string, payload: CreateTaxServiceAliasRequest) => {
    const res = await apiClient.post<ApiResponse<TaxServiceAlias>>(`/v1/admin/tax-services/${taxServiceId}/aliases`, payload);
    return res.data.data;
  },
  deleteAlias: async (taxServiceId: string, aliasId: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/admin/tax-services/${taxServiceId}/aliases/${aliasId}`);
    return res.data;
  },
};

// --- 16. Practice Marketplace Onboarding Hub ---
export const marketplaceOnboardingPracticeApi = {
  sendProposal: async (payload: CreateProposalRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceProposal>>('/v1/practice/marketplace/onboarding/proposals', payload);
    return res.data.data;
  },
  getProposals: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceProposal>>>('/v1/practice/marketplace/onboarding/proposals', { params });
    return res.data.data;
  },
  initiateOnboarding: async (payload: InitiateOnboardingRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceOnboarding>>('/v1/practice/marketplace/onboarding/initiate', payload);
    return res.data.data;
  },
  getOnboardings: async (params?: { status?: string; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MarketplaceOnboarding>>>('/v1/practice/marketplace/onboarding', { params });
    return res.data.data;
  },
  getOnboardingById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceOnboarding>>(`/v1/practice/marketplace/onboarding/${id}`);
    return res.data.data;
  },
  verifyDocument: async (onboardingId: string, documentId: string, payload: VerifyOnboardingDocumentRequest) => {
    const res = await apiClient.put<ApiResponse<OnboardingDocument>>(
      `/v1/practice/marketplace/onboarding/${onboardingId}/documents/${documentId}/verify`,
      payload
    );
    return res.data.data;
  },
  promoteToClient: async (onboardingId: string, payload?: ApproveAndPromoteClientRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceOnboarding>>(
      `/v1/practice/marketplace/onboarding/${onboardingId}/promote-to-client`,
      payload || {}
    );
    return res.data.data;
  },
};

// --- 17. Public Customer Self-Serve Onboarding Portal ---
export const marketplaceOnboardingPublicApi = {
  getProposalByToken: async (token: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceProposal>>(`/v1/marketplace/onboarding/proposal/${token}`);
    return res.data.data;
  },
  respondToProposal: async (token: string, payload: AcceptProposalRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceProposal>>(`/v1/marketplace/onboarding/proposal/${token}/respond`, payload);
    return res.data.data;
  },
  getOnboardingByToken: async (token: string) => {
    const res = await apiClient.get<ApiResponse<MarketplaceOnboarding>>(`/v1/marketplace/onboarding/session/${token}`);
    return res.data.data;
  },
  updateDetails: async (token: string, payload: UpdateOnboardingDetailsRequest) => {
    const res = await apiClient.put<ApiResponse<MarketplaceOnboarding>>(`/v1/marketplace/onboarding/session/${token}/details`, payload);
    return res.data.data;
  },
  signEngagement: async (token: string, payload: SignEngagementLetterRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceOnboarding>>(`/v1/marketplace/onboarding/session/${token}/sign-engagement`, payload);
    return res.data.data;
  },
  uploadDocument: async (
    token: string,
    params: { documentType: string; documentName: string; filePath: string; fileSizeBytes?: number; contentType?: string }
  ) => {
    const res = await apiClient.post<ApiResponse<OnboardingDocument>>(
      `/v1/marketplace/onboarding/session/${token}/upload-document`,
      null,
      { params }
    );
    return res.data.data;
  },
};

// --- 18. Marketplace Customer Account & Self-Service Portal ---
export const marketplaceCustomerApi = {
  register: async (payload: RegisterCustomerRequest) => {
    const res = await apiClient.post<ApiResponse<CustomerAuthResponse>>('/v1/marketplace/customer/register', payload);
    return res.data.data;
  },
  getProfile: async () => {
    const res = await apiClient.get<ApiResponse<CustomerProfile>>('/v1/marketplace/customer/profile');
    return res.data.data;
  },
  updateProfile: async (payload: UpdateCustomerProfileRequest) => {
    const res = await apiClient.put<ApiResponse<CustomerProfile>>('/v1/marketplace/customer/profile', payload);
    return res.data.data;
  },
  getDashboard: async () => {
    const res = await apiClient.get<ApiResponse<CustomerDashboard>>('/v1/marketplace/customer/dashboard');
    return res.data.data;
  },
  getLeads: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceLead[]>>('/v1/marketplace/customer/leads');
    return res.data.data;
  },
  getConsultations: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceConsultation[]>>('/v1/marketplace/customer/consultations');
    return res.data.data;
  },
  getProposals: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceProposal[]>>('/v1/marketplace/customer/proposals');
    return res.data.data;
  },
  getReviews: async () => {
    const res = await apiClient.get<ApiResponse<MarketplaceReview[]>>('/v1/marketplace/customer/reviews');
    return res.data.data;
  },
  // Operational Enquiry Tracking & Verified Reviews
  getEnquiries: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<EnquiryDetail>>>('/v1/marketplace/customer/enquiries', { params });
    return res.data.data;
  },
  getEnquiryDetail: async (id: string) => {
    const res = await apiClient.get<ApiResponse<EnquiryDetail>>(`/v1/marketplace/customer/enquiries/${id}`);
    return res.data.data;
  },
  cancelEnquiry: async (id: string, payload?: CancelEnquiryRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryDetail>>(`/v1/marketplace/customer/enquiries/${id}/cancel`, payload || {});
    return res.data.data;
  },
  submitVerifiedReview: async (id: string, payload: SubmitEnquiryReviewRequest) => {
    const res = await apiClient.post<ApiResponse<MarketplaceReview>>(`/v1/marketplace/customer/enquiries/${id}/review`, payload);
    return res.data.data;
  },
  // Secure Messages
  getEnquiryMessages: async (id: string) => {
    const res = await apiClient.get<ApiResponse<EnquiryMessageThread>>(`/v1/marketplace/customer/enquiries/${id}/messages`);
    return res.data.data;
  },
  sendEnquiryMessage: async (id: string, payload: SendEnquiryMessageRequest) => {
    const res = await apiClient.post<ApiResponse<EnquiryMessage>>(`/v1/marketplace/customer/enquiries/${id}/messages`, payload);
    return res.data.data;
  },
  markMessagesRead: async (id: string) => {
    const res = await apiClient.post<ApiResponse<void>>(`/v1/marketplace/customer/enquiries/${id}/messages/read`);
    return res.data;
  },
};

// --- 20. Application Feedback (kept separate from Marketplace Reviews) ---
export const applicationFeedbackApi = {
  create: async (payload: CreateApplicationFeedbackRequest, context?: { page?: string; feature?: string }) => {
    const headers: Record<string, string> = {};
    if (context?.page) headers['X-Feedback-Page'] = context.page;
    if (context?.feature) headers['X-Feedback-Feature'] = context.feature;
    const res = await apiClient.post<ApiResponse<ApplicationFeedback>>('/v1/customer/feedback', payload, { headers });
    return res.data.data;
  },
  listMine: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ApplicationFeedback>>>('/v1/customer/feedback', { params });
    return res.data.data;
  },
};

// --- 19. Customer Tax Requirements API (Feature #6) ---
export const customerTaxRequirementApi = {
  create: async (payload: CreateTaxRequirementRequest) => {
    const res = await apiClient.post<ApiResponse<CustomerTaxRequirement>>('/v1/customer/tax-requirements', payload);
    return res.data.data;
  },
  list: async (params?: { status?: TaxRequirementStatus; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<CustomerTaxRequirementSummary>>>('/v1/customer/tax-requirements', {
      params,
    });
    return res.data.data;
  },
  getById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<CustomerTaxRequirement>>(`/v1/customer/tax-requirements/${id}`);
    return res.data.data;
  },
  update: async (id: string, payload: UpdateTaxRequirementRequest) => {
    const res = await apiClient.put<ApiResponse<CustomerTaxRequirement>>(`/v1/customer/tax-requirements/${id}`, payload);
    return res.data.data;
  },
  submit: async (id: string) => {
    const res = await apiClient.post<ApiResponse<CustomerTaxRequirement>>(`/v1/customer/tax-requirements/${id}/submit`);
    return res.data.data;
  },
  cancel: async (id: string) => {
    const res = await apiClient.post<ApiResponse<CustomerTaxRequirement>>(`/v1/customer/tax-requirements/${id}/cancel`);
    return res.data.data;
  },
  getFinancialYears: async () => {
    const res = await apiClient.get<ApiResponse<FinancialYearOption[]>>('/v1/customer/tax-requirements/financial-years');
    return res.data.data;
  },
};

// --- 20. Platform Admin Feedback Management API ---
export const adminFeedbackApi = {
  getFeedbackList: async (params?: {
    search?: string;
    actorType?: string;
    type?: string;
    category?: string;
    status?: string;
    priority?: string;
    practiceId?: string;
    assignedTeam?: string;
    assignedUserId?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<AdminApplicationFeedbackSummary>>>('/v1/admin/feedback', { params });
    return res.data.data;
  },
  getFeedbackDetail: async (id: string) => {
    const res = await apiClient.get<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}`);
    return res.data.data;
  },
  startReview: async (id: string) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/start-review`);
    return res.data.data;
  },
  assignFeedback: async (id: string, payload: AssignFeedbackRequest) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/assign`, payload);
    return res.data.data;
  },
  addNote: async (id: string, payload: CreateFeedbackNoteRequest) => {
    const res = await apiClient.post<ApiResponse<FeedbackNote>>(`/v1/admin/feedback/${id}/notes`, payload);
    return res.data.data;
  },
  updatePriority: async (id: string, payload: UpdateFeedbackPriorityRequest) => {
    const res = await apiClient.patch<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/priority`, payload);
    return res.data.data;
  },
  resolveFeedback: async (id: string, payload: ResolveFeedbackRequest) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/resolve`, payload);
    return res.data.data;
  },
  closeFeedback: async (id: string, payload?: CloseFeedbackRequest) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/close`, payload || {});
    return res.data.data;
  },
  rejectFeedback: async (id: string, payload: RejectFeedbackRequest) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/reject`, payload);
    return res.data.data;
  },
  markDuplicate: async (id: string, payload: MarkDuplicateFeedbackRequest) => {
    const res = await apiClient.post<ApiResponse<AdminApplicationFeedbackDetail>>(`/v1/admin/feedback/${id}/duplicate`, payload);
    return res.data.data;
  },
  escalateToEngineering: async (id: string, payload: EscalateToEngineeringRequest) => {
    const res = await apiClient.post<ApiResponse<EngineeringIssue>>(`/v1/admin/feedback/${id}/escalate`, payload);
    return res.data.data;
  },
  getStats: async () => {
    const res = await apiClient.get<ApiResponse<AdminFeedbackStats>>('/v1/admin/feedback/stats');
    return res.data.data;
  },
  getAssignees: async () => {
    const res = await apiClient.get<ApiResponse<AdminAssignee[]>>('/v1/admin/feedback/assignees');
    return res.data.data;
  },
  getTeams: async () => {
    const res = await apiClient.get<ApiResponse<FeedbackTeam[]>>('/v1/admin/feedback/teams');
    return res.data.data;
  },
};

// --- 21. Platform Overview Dashboard API (SuperAdmin Only) ---
export const platformDashboardApi = {
  getOverview: async () => {
    const res = await apiClient.get<ApiResponse<PlatformDashboardSummary>>('/v1/admin/platform/dashboard');
    return res.data.data;
  },
};

// --- 21b. Platform Support Dashboard API (Support Admin) ---
export const supportDashboardApi = {
  getOverview: async () => {
    const res = await apiClient.get<ApiResponse<SupportDashboardSummary>>('/v1/admin/support/overview');
    return res.data.data;
  },
};

// --- 22. Platform Admin Practice Governance API ---
export const adminPracticeApi = {
  getPractices: async (params?: { page?: number; size?: number; search?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Organization>>>('/v1/organizations', { params });
    return res.data.data;
  },
  updateStatus: async (organizationId: string, payload: { status: string; reason?: string }) => {
    const res = await apiClient.patch<ApiResponse<Organization>>(`/v1/organizations/${organizationId}/status`, payload);
    return res.data.data;
  },
  getPracticeById: async (organizationId: string) => {
    const res = await apiClient.get<ApiResponse<Organization>>(`/v1/organizations/${organizationId}`);
    return res.data.data;
  },
};

// --- 23. Platform Admin User Governance API ---
export const adminUserApi = {
  getUsers: async (params?: { role?: string; status?: string; search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<User>>>('/v1/admin/users', { params });
    return res.data.data;
  },
  createUser: async (payload: {
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    roleCode: string;
    status?: string;
    temporaryPassword?: string;
  }) => {
    const res = await apiClient.post<ApiResponse<User>>('/v1/admin/users', payload);
    return res.data.data;
  },
  updateRole: async (userId: string, roleCode: string) => {
    const res = await apiClient.put<ApiResponse<User>>(`/v1/admin/users/${userId}/role`, { roleCode });
    return res.data.data;
  },
  updateStatus: async (userId: string, status: string) => {
    const res = await apiClient.patch<ApiResponse<User>>(`/v1/admin/users/${userId}/status`, null, { params: { status } });
    return res.data.data;
  },
};

// --- 24. Taxoryn Learn Public Knowledge API ---
export const publicLearnApi = {
  getContentList: async (params?: {
    contentType?: string;
    categoryId?: string;
    taxServiceId?: string;
    tag?: string;
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<LearnContentSummary>>>('/v1/public/content', { params });
    return res.data.data;
  },
  getContentBySlug: async (slug: string) => {
    const res = await apiClient.get<ApiResponse<LearnContentDetail>>(`/v1/public/content/${slug}`);
    return res.data.data;
  },
  getRelatedContent: async (slug: string, limit: number = 4) => {
    const res = await apiClient.get<ApiResponse<LearnContentSummary[]>>(`/v1/public/content/${slug}/related`, { params: { limit } });
    return res.data.data;
  },
  getCategories: async () => {
    const res = await apiClient.get<ApiResponse<LearnPublicCategory[]>>('/v1/public/content/categories');
    return res.data.data;
  },
};

// --- 25. Taxoryn Learn Admin Content Studio & Governance API ---
export const adminLearnApi = {
  getDashboardStats: async () => {
    const res = await apiClient.get<ApiResponse<ContentDashboardStats>>('/v1/admin/content/dashboard-stats');
    return res.data.data;
  },
  getReviewQueue: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<LearnContentSummary>>>('/v1/admin/content/review-queue', { params });
    return res.data.data;
  },
  getContentList: async (params?: {
    contentType?: string;
    status?: string;
    categoryId?: string;
    taxServiceId?: string;
    tag?: string;
    search?: string;
    page?: number;
    size?: number;
  }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<LearnContentSummary>>>('/v1/admin/content', { params });
    return res.data.data;
  },
  getContentById: async (id: string) => {
    const res = await apiClient.get<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}`);
    return res.data.data;
  },
  previewContent: async (id: string) => {
    const res = await apiClient.get<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/preview`);
    return res.data.data;
  },
  createContent: async (payload: {
    contentType: string;
    title: string;
    slug?: string;
    summary?: string;
    body: string;
    thumbnailUrl?: string;
    featuredImageUrl?: string;
    altText?: string;
    youtubeUrl?: string;
    videoDurationSeconds?: number;
    categoryId?: string;
    taxServiceId?: string;
    taxServiceIds?: string[];
    tags?: string[];
  }) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>('/v1/admin/content', payload);
    return res.data.data;
  },
  updateContent: async (id: string, payload: {
    contentType?: string;
    title?: string;
    slug?: string;
    summary?: string;
    body?: string;
    thumbnailUrl?: string;
    featuredImageUrl?: string;
    altText?: string;
    youtubeUrl?: string;
    videoDurationSeconds?: number;
    categoryId?: string;
    taxServiceId?: string;
    taxServiceIds?: string[];
    tags?: string[];
  }) => {
    const res = await apiClient.put<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}`, payload);
    return res.data.data;
  },
  submitForReview: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/submit-review`);
    return res.data.data;
  },
  startReview: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/start-review`);
    return res.data.data;
  },
  approveContent: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/approve`);
    return res.data.data;
  },
  rejectContent: async (id: string, reason: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/reject`, { reason });
    return res.data.data;
  },
  scheduleContent: async (id: string, scheduledPublishAt: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/schedule`, { scheduledPublishAt });
    return res.data.data;
  },
  publishContent: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/publish`);
    return res.data.data;
  },
  archiveContent: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/archive`);
    return res.data.data;
  },
  restoreContent: async (id: string) => {
    const res = await apiClient.post<ApiResponse<LearnContentDetail>>(`/v1/admin/content/${id}/restore`);
    return res.data.data;
  },
  getVersionHistory: async (id: string) => {
    const res = await apiClient.get<ApiResponse<ContentVersion[]>>(`/v1/admin/content/${id}/versions`);
    return res.data.data;
  },
  getControlledTaxServices: async () => {
    const res = await apiClient.get<ApiResponse<PublicTaxService[]>>('/v1/admin/content/tax-services');
    return res.data.data;
  },
};

// --- 26. Content Studio Media Library API ---
export const adminMediaApi = {
  getMediaAssets: async (params?: { search?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<MediaAsset>>>('/v1/admin/content/media', { params });
    return res.data.data;
  },
  uploadMedia: async (file: File, altText?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (altText) {
      formData.append('altText', altText);
    }
    const res = await apiClient.post<ApiResponse<MediaAsset>>('/v1/admin/content/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data.data;
  },
  updateMedia: async (id: string, payload: { altText?: string }) => {
    const res = await apiClient.put<ApiResponse<MediaAsset>>(`/v1/admin/content/media/${id}`, payload);
    return res.data.data;
  },
  deleteMedia: async (id: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/admin/content/media/${id}`);
    return res.data.data;
  },
};

// --- 27. WhatsApp Integration API ---
export const whatsappApi = {
  getStatus: async () => {
    const res = await apiClient.get<ApiResponse<WhatsAppIntegrationStatus>>('/v1/notifications/whatsapp/status');
    return res.data.data;
  },
  getMessages: async (params?: { page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<WhatsAppMessageRecord>>>('/v1/notifications/whatsapp/messages', { params });
    return res.data.data;
  },
  resendMessage: async (id: string) => {
    const res = await apiClient.post<ApiResponse<WhatsAppMessageRecord>>(`/v1/notifications/whatsapp/messages/${id}/resend`);
    return res.data.data;
  },
};

// --- 28. Notification Center API ---
export const notificationApi = {
  getAll: async (params?: NotificationFilterParams) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<NotificationItem>>>('/v1/notifications', { params });
    return res.data.data;
  },
  getUnreadCount: async () => {
    const res = await apiClient.get<ApiResponse<UnreadCountResponse>>('/v1/notifications/unread-count');
    return res.data.data;
  },
  markAsRead: async (id: string) => {
    const res = await apiClient.patch<ApiResponse<NotificationItem>>(`/v1/notifications/${id}/read`);
    return res.data.data;
  },
  markAsUnread: async (id: string) => {
    const res = await apiClient.patch<ApiResponse<NotificationItem>>(`/v1/notifications/${id}/unread`);
    return res.data.data;
  },
  markAllAsRead: async () => {
    const res = await apiClient.post<ApiResponse<{ updated: number }>>('/v1/notifications/mark-all-read');
    return res.data.data;
  },
  dismiss: async (id: string) => {
    const res = await apiClient.delete<ApiResponse<void>>(`/v1/notifications/${id}`);
    return res.data.data;
  },
  send: async (payload: Partial<NotificationItem>) => {
    const res = await apiClient.post<ApiResponse<NotificationItem>>('/v1/notifications/send', payload);
    return res.data.data;
  },
};



