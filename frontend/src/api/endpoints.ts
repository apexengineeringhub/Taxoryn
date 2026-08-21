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
  ClientPortalUser,
  RegisterClientPortalUserRequest,
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

// --- 7. Compliance Calendar ---
export const calendarApi = {
  getEvents: async (params?: { fromDate?: string; toDate?: string; complianceType?: string }) => {
    const res = await apiClient.get<ApiResponse<CalendarEvent[]>>('/v1/compliance-calendar/events', { params });
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
  getLogs: async (params?: { page?: number; size?: number; entityType?: string; action?: string; fromDate?: string; toDate?: string }) => {
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

