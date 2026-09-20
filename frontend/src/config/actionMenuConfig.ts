import {
  Plus,
  UserPlus,
  CheckSquare,
  FileText,
  Building2,
  FileSpreadsheet,
  Percent,
  UserCheck,
  Receipt,
  ShieldCheck,
  MessageSquare,
  Store,
  BookOpen,
  Bell,
  Headphones,
} from 'lucide-react';
import { CLIENT_ROLES } from '../utils/permissionUtils.ts';

export type ActionCategory =
  | 'CLIENT'
  | 'WORK'
  | 'DOCUMENTS'
  | 'COMPLIANCE'
  | 'COMMUNICATION'
  | 'TEAM'
  | 'BILLING'
  | 'TENANTS'
  | 'GOVERNANCE'
  | 'MARKETPLACE'
  | 'CONTENT'
  | 'SUPPORT';

export interface ActionDefinition {
  id: string;
  category: ActionCategory;
  categoryLabel: string;
  label: string;
  description: string;
  icon: any;
  requiredPermissions: string[];
  allowedRoles: string[];
  targetPath: string;
  supportsClientContext?: boolean;
}

export interface ComplianceSubmenuAction {
  id: string;
  label: string;
  description: string;
  icon: any;
  requiredPermissions: string[];
  allowedRoles: string[];
  targetPath: string;
  supportsClientContext?: boolean;
}

// Compliance submenu definitions for practice users
export const COMPLIANCE_SUBMENU_ACTIONS: ComplianceSubmenuAction[] = [
  {
    id: 'gst-return',
    label: 'GST Return',
    description: 'Start GST compliance & return filing',
    icon: Building2,
    requiredPermissions: ['GST_CREATE', 'GST_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'STAFF', 'ARTICLE_ASSISTANT'],
    targetPath: '/gst?action=new',
    supportsClientContext: true,
  },
  {
    id: 'itr-return',
    label: 'ITR Return',
    description: 'Start income-tax return computation & filing',
    icon: FileSpreadsheet,
    requiredPermissions: ['ITR_CREATE', 'ITR_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'STAFF', 'ARTICLE_ASSISTANT'],
    targetPath: '/itr?action=new',
    supportsClientContext: true,
  },
  {
    id: 'tds-work',
    label: 'TDS Work',
    description: 'Start quarterly TDS return & challan work',
    icon: Percent,
    requiredPermissions: ['TDS_CREATE', 'TDS_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'STAFF', 'ARTICLE_ASSISTANT'],
    targetPath: '/tds?action=new',
    supportsClientContext: true,
  },
];

// Platform SuperAdmin & Platform Operations Actions
export const PLATFORM_ACTION_DEFINITIONS: ActionDefinition[] = [
  {
    id: 'platform-onboard-practice',
    category: 'TENANTS',
    categoryLabel: 'TENANTS',
    label: 'Onboard Practice Firm',
    description: 'Register & provision a new CA firm workspace',
    icon: Building2,
    requiredPermissions: ['PRACTICE_CREATE', 'PRACTICE_WRITE', 'PRACTICE_VIEW'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN'],
    targetPath: '/admin/practices',
    supportsClientContext: false,
  },
  {
    id: 'platform-create-user',
    category: 'GOVERNANCE',
    categoryLabel: 'GOVERNANCE',
    label: 'Create Platform User',
    description: 'Provision an internal Taxoryn operations or support user',
    icon: UserPlus,
    requiredPermissions: ['USER_CREATE', 'PLATFORM_USER_CREATE', 'USER_VIEW'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN'],
    targetPath: '/admin/users',
    supportsClientContext: false,
  },
  {
    id: 'platform-add-marketplace-service',
    category: 'MARKETPLACE',
    categoryLabel: 'MARKETPLACE',
    label: 'Add Marketplace Service',
    description: 'Create a new tax or statutory service in the master catalog',
    icon: Store,
    requiredPermissions: ['MARKETPLACE_MANAGE', 'MARKETPLACE_VIEW'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_MARKETPLACE_ADMIN', 'TAXORYN_OPERATIONS_ADMIN'],
    targetPath: '/admin/marketplace',
    supportsClientContext: false,
  },
  {
    id: 'platform-publish-content',
    category: 'CONTENT',
    categoryLabel: 'CONTENT',
    label: 'Publish Knowledge Content',
    description: 'Publish a tax guide, masterclass video, or statutory update',
    icon: BookOpen,
    requiredPermissions: ['CONTENT_MANAGE', 'CONTENT_VIEW'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_CONTENT_ADMIN', 'TAXORYN_OPERATIONS_ADMIN'],
    targetPath: '/admin/content',
    supportsClientContext: false,
  },
  {
    id: 'platform-broadcast-announcement',
    category: 'COMMUNICATION',
    categoryLabel: 'COMMUNICATION',
    label: 'Platform Announcement',
    description: 'Broadcast a platform-wide compliance alert or notice',
    icon: Bell,
    requiredPermissions: ['NOTIFICATION_MANAGE', 'NOTIFICATION_CREATE', 'NOTIFICATION_VIEW'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN'],
    targetPath: '/notifications',
    supportsClientContext: false,
  },
  {
    id: 'platform-triage-feedback',
    category: 'SUPPORT',
    categoryLabel: 'SUPPORT',
    label: 'Triage Support Feedback',
    description: 'Review user feedback, bug reports, and support tickets',
    icon: Headphones,
    requiredPermissions: ['FEEDBACK_VIEW', 'FEEDBACK_MANAGE'],
    allowedRoles: ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_SUPPORT_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_ENGINEERING_ADMIN'],
    targetPath: '/admin/feedback',
    supportsClientContext: false,
  },
];

// Practice / CA Firm Action Definitions
export const PRACTICE_ACTION_DEFINITIONS: ActionDefinition[] = [
  {
    id: 'new-client',
    category: 'CLIENT',
    categoryLabel: 'CLIENT',
    label: 'New Client',
    description: 'Create a new client and start onboarding',
    icon: UserPlus,
    requiredPermissions: ['CLIENT_CREATE', 'CLIENT_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER'],
    targetPath: '/clients?action=new',
    supportsClientContext: false,
  },
  {
    id: 'new-task',
    category: 'WORK',
    categoryLabel: 'WORK',
    label: 'New Task',
    description: 'Create and assign a task',
    icon: CheckSquare,
    requiredPermissions: ['TASK_CREATE', 'TASK_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'STAFF', 'ARTICLE_ASSISTANT'],
    targetPath: '/tasks?action=new',
    supportsClientContext: true,
  },
  {
    id: 'request-documents',
    category: 'DOCUMENTS',
    categoryLabel: 'DOCUMENTS',
    label: 'Request Documents',
    description: 'Request documents from a client',
    icon: FileText,
    requiredPermissions: ['DOC_REQUEST_CREATE', 'DOCUMENT_WRITE', 'CLIENT_UPDATE', 'CLIENT_CREATE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'STAFF', 'PRACTITIONER'],
    targetPath: '/documents?action=request',
    supportsClientContext: true,
  },
  {
    id: 'start-compliance',
    category: 'COMPLIANCE',
    categoryLabel: 'COMPLIANCE',
    label: 'Start Compliance Work',
    description: 'GST, Income Tax (ITR), and TDS return workflows',
    icon: ShieldCheck,
    requiredPermissions: [], // Managed dynamically via child compliance actions
    allowedRoles: [],
    targetPath: '', // Triggers submenu
    supportsClientContext: true,
  },
  {
    id: 'send-client-message',
    category: 'COMMUNICATION',
    categoryLabel: 'COMMUNICATION',
    label: 'Send Client Message',
    description: 'Message a client through the portal',
    icon: MessageSquare,
    requiredPermissions: ['CLIENT_VIEW', 'CLIENT_UPDATE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'STAFF'],
    targetPath: '/portal?tab=messages',
    supportsClientContext: true,
  },
  {
    id: 'add-employee',
    category: 'TEAM',
    categoryLabel: 'TEAM',
    label: 'Add Employee',
    description: 'Invite a team member or practitioner to your practice',
    icon: UserCheck,
    requiredPermissions: ['EMPLOYEE_CREATE', 'EMPLOYEE_WRITE', 'USER_CREATE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'],
    targetPath: '/team?action=add',
    supportsClientContext: false,
  },
  {
    id: 'create-invoice',
    category: 'BILLING',
    categoryLabel: 'BILLING',
    label: 'Create Invoice',
    description: 'Issue a tax invoice or retainer bill for a client',
    icon: Receipt,
    requiredPermissions: ['BILLING_CREATE', 'BILLING_WRITE'],
    allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER'],
    targetPath: '/billing?action=new',
    supportsClientContext: true,
  },
];

// Client Portal & Taxpayer Customer Action Definitions
export const CLIENT_ACTION_DEFINITIONS: ActionDefinition[] = [
  {
    id: 'client-upload-doc',
    category: 'DOCUMENTS',
    categoryLabel: 'DOCUMENTS',
    label: 'Upload Tax Documents',
    description: 'Upload statutory notices, bank statements, or invoices',
    icon: FileText,
    requiredPermissions: [],
    allowedRoles: CLIENT_ROLES,
    targetPath: '/portal?tab=documents',
    supportsClientContext: false,
  },
  {
    id: 'client-message-ca',
    category: 'COMMUNICATION',
    categoryLabel: 'COMMUNICATION',
    label: 'Message Your Tax Professional',
    description: 'Chat directly with your assigned CA or practitioner',
    icon: MessageSquare,
    requiredPermissions: [],
    allowedRoles: CLIENT_ROLES,
    targetPath: '/portal?tab=messages',
    supportsClientContext: false,
  },
  {
    id: 'client-post-requirement',
    category: 'MARKETPLACE',
    categoryLabel: 'MARKETPLACE',
    label: 'Request Tax Service',
    description: 'Post a new requirement for CA consultations or filings',
    icon: Store,
    requiredPermissions: [],
    allowedRoles: CLIENT_ROLES,
    targetPath: '/marketplace/customer/requirements/new',
    supportsClientContext: false,
  },
  {
    id: 'client-feedback',
    category: 'SUPPORT',
    categoryLabel: 'SUPPORT',
    label: 'Give Feedback',
    description: 'Share suggestions or report an issue with the platform',
    icon: ShieldCheck,
    requiredPermissions: [],
    allowedRoles: CLIENT_ROLES,
    targetPath: '/marketplace/customer/feedback',
    supportsClientContext: false,
  },
];

// Alias for backwards compatibility
export const ACTION_DEFINITIONS = PRACTICE_ACTION_DEFINITIONS;
