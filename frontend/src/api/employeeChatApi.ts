import { apiClient } from './client';

export interface ApiResponse<T> {
  success: boolean;
  status: number;
  message: string;
  data: T;
  timestamp: string;
  traceId?: string;
}

export interface EmployeeChatContact {
  employeeId: string;
  userId?: string;
  employeeCode: string;
  name: string;
  email?: string;
  phone?: string;
  avatarUrl?: string;
  designation?: string;
  department?: string;
  role?: string;
  status: string;
  lastMessagePreview?: string;
  lastMessageTimestamp?: string;
  unreadCount: number;
  isManager?: boolean;
  isSelf?: boolean;
}

export interface EmployeeChatChannel {
  id: string;
  organizationId: string;
  name: string;
  displayName: string;
  description?: string;
  channelType: 'GENERAL' | 'DEPARTMENT';
  department?: string;
  isDefault: boolean;
  isArchived: boolean;
  createdAt: string;
  lastMessagePreview?: string;
  lastMessageTimestamp?: string;
  unreadCount: number;
}

export interface EmployeeChatMessage {
  id: string;
  organizationId: string;
  channelId?: string;
  senderEmployeeId: string;
  senderName: string;
  senderEmail?: string;
  senderAvatarUrl?: string;
  senderDesignation?: string;
  senderDepartment?: string;
  recipientEmployeeId?: string;
  recipientName?: string;
  messageBody: string;
  attachmentsJson?: string;
  isRead?: boolean;
  readAt?: string;
  createdAt: string;
  isOwnMessage?: boolean;
}

export interface SendEmployeeChatMessagePayload {
  messageBody: string;
  attachmentsJson?: string;
  recipientEmployeeId?: string;
  channelId?: string;
}

export const employeeChatApi = {
  getEligibleContacts: async (): Promise<EmployeeChatContact[]> => {
    const res = await apiClient.get<ApiResponse<EmployeeChatContact[]>>('/v1/employee/chat/contacts');
    return res.data.data;
  },

  getAccessibleChannels: async (): Promise<EmployeeChatChannel[]> => {
    const res = await apiClient.get<ApiResponse<EmployeeChatChannel[]>>('/v1/employee/chat/channels');
    return res.data.data;
  },

  getDirectMessages: async (employeeId: string): Promise<EmployeeChatMessage[]> => {
    const res = await apiClient.get<ApiResponse<EmployeeChatMessage[]>>(`/v1/employee/chat/direct/${employeeId}/messages`);
    return res.data.data;
  },

  sendDirectMessage: async (employeeId: string, payload: SendEmployeeChatMessagePayload): Promise<EmployeeChatMessage> => {
    const res = await apiClient.post<ApiResponse<EmployeeChatMessage>>(`/v1/employee/chat/direct/${employeeId}/messages`, payload);
    return res.data.data;
  },

  markDirectMessagesRead: async (employeeId: string): Promise<void> => {
    await apiClient.post<ApiResponse<void>>(`/v1/employee/chat/direct/${employeeId}/read`);
  },

  getChannelMessages: async (channelId: string): Promise<EmployeeChatMessage[]> => {
    const res = await apiClient.get<ApiResponse<EmployeeChatMessage[]>>(`/v1/employee/chat/channels/${channelId}/messages`);
    return res.data.data;
  },

  sendChannelMessage: async (channelId: string, payload: SendEmployeeChatMessagePayload): Promise<EmployeeChatMessage> => {
    const res = await apiClient.post<ApiResponse<EmployeeChatMessage>>(`/v1/employee/chat/channels/${channelId}/messages`, payload);
    return res.data.data;
  },

  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const res = await apiClient.get<ApiResponse<{ unreadCount: number }>>('/v1/employee/chat/unread-count');
    return res.data.data;
  },
};
