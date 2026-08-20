import { apiClient } from './client';
import {
  ApiResponse,
  PagedResponse,
  OrganizationDashboard,
  Client,
  Task,
  GstProfile,
  GstReturnFiling,
  ItrProfile,
  ItrReturn,
  CalendarEvent,
  DocumentItem,
  Invoice,
  SubscriptionPlan,
  SubscriptionInfo,
  Employee,
  Role,
  AuditLog,
  AuthTokens,
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
};

// --- 4. Tasks ---
export const taskApi = {
  getAll: async (params?: { page?: number; size?: number; status?: string; clientId?: string; assignedTo?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Task>>>('/v1/tasks', { params });
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
  updateStatus: async (id: string, status: string) => {
    const res = await apiClient.patch<ApiResponse<Task>>(`/v1/tasks/${id}/status`, { status });
    return res.data.data;
  },
};

// --- 5. GST Compliance ---
export const gstApi = {
  getProfiles: async (params?: { clientId?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<GstProfile>>>('/v1/gst/profiles', { params });
    return res.data.data;
  },
  createProfile: async (payload: Partial<GstProfile>) => {
    const res = await apiClient.post<ApiResponse<GstProfile>>('/v1/gst/profiles', payload);
    return res.data.data;
  },
  getFilings: async (params?: { returnPeriod?: string; returnType?: string; filingStatus?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<GstReturnFiling>>>('/v1/gst/filings', { params });
    return res.data.data;
  },
  recordFiling: async (id: string, payload: { filingDate: string; acknowledgementNumber: string; filingStatus?: string }) => {
    const res = await apiClient.post<ApiResponse<GstReturnFiling>>(`/v1/gst/filings/${id}/file`, payload);
    return res.data.data;
  },
};

// --- 6. ITR Compliance ---
export const itrApi = {
  getProfiles: async (params?: { clientId?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ItrProfile>>>('/v1/itr/profiles', { params });
    return res.data.data;
  },
  getReturns: async (params?: { assessmentYear?: string; status?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<ItrReturn>>>('/v1/itr/returns', { params });
    return res.data.data;
  },
  updateReturnStatus: async (id: string, payload: { status: string; acknowledgementNumber?: string; verificationDate?: string }) => {
    const res = await apiClient.patch<ApiResponse<ItrReturn>>(`/v1/itr/returns/${id}/status`, payload);
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
  getInvoices: async (params?: { clientId?: string; status?: string }) => {
    const res = await apiClient.get<ApiResponse<PagedResponse<Invoice>>>('/v1/billing/invoices', { params });
    return res.data.data;
  },
  createInvoice: async (payload: Partial<Invoice>) => {
    const res = await apiClient.post<ApiResponse<Invoice>>('/v1/billing/invoices', payload);
    return res.data.data;
  },
  recordPayment: async (invoiceId: string, payload: { amount: number; paymentMode: string; referenceNumber?: string; paymentDate: string }) => {
    const res = await apiClient.post<ApiResponse<any>>(`/v1/billing/invoices/${invoiceId}/payments`, payload);
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
  getRoles: async () => {
    const res = await apiClient.get<ApiResponse<Role[]>>('/v1/roles');
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
