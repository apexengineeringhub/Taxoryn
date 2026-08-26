import {
  LayoutDashboard,
  Building2,
  Users,
  Store,
  CreditCard,
  ShieldCheck,
  ShieldAlert,
  Search,
  FileSpreadsheet,
  Percent,
  Receipt,
  FolderLock,
  MessageSquarePlus,
  CheckSquare,
  Calendar,
  UserCheck,
  Globe,
  Palette,
  Sparkles,
  Server,
  Activity,
  FileText,
  Video,
  BookOpen,
  Headphones,
  LifeBuoy,
  BadgeAlert,
  Layers,
} from 'lucide-react';

export interface NavItemConfig {
  label: string;
  path: string;
  icon: any;
  requiredPermission?: string;
  badge?: string;
}

export interface RoleWorkspaceDefinition {
  roleCode: string;
  roleTitle: string;
  platformSubtitle: string;
  dashboardType: 'superadmin' | 'support' | 'marketplace' | 'finance' | 'content' | 'security' | 'engineering' | 'practice' | 'staff' | 'client';
  defaultRoute: string;
  badgeStyle: string;
  navigation: NavItemConfig[];
}

export const ROLE_WORKSPACE_CONFIGS: Record<string, RoleWorkspaceDefinition> = {
  // 1. Taxoryn Platform SuperAdmin (Platform Governance)
  TAXORYN_SUPERADMIN: {
    roleCode: 'TAXORYN_SUPERADMIN',
    roleTitle: 'Taxoryn SuperAdmin',
    platformSubtitle: 'PLATFORM SUPERADMIN',
    dashboardType: 'superadmin',
    defaultRoute: '/admin/overview',
    badgeStyle: 'bg-purple-100 text-purple-800 border-purple-200',
    navigation: [
      { label: 'Platform Overview', path: '/admin/overview', icon: LayoutDashboard },
      { label: 'Practice Tenants', path: '/admin/practices', icon: Building2 },
      { label: 'Platform Users', path: '/admin/users', icon: Users },
      { label: 'Marketplace Ops', path: '/admin/marketplace', icon: Store },
      { label: 'Subscriptions & MRR', path: '/admin/subscriptions', icon: CreditCard },
      { label: 'Feedback Ops', path: '/admin/feedback', icon: ShieldCheck },
      { label: 'Security & Audit', path: '/audit-logs', icon: ShieldAlert },
    ],
  },
  SUPER_ADMIN: {
    roleCode: 'SUPER_ADMIN',
    roleTitle: 'Taxoryn SuperAdmin',
    platformSubtitle: 'PLATFORM SUPERADMIN',
    dashboardType: 'superadmin',
    defaultRoute: '/admin/overview',
    badgeStyle: 'bg-purple-100 text-purple-800 border-purple-200',
    navigation: [
      { label: 'Platform Overview', path: '/admin/overview', icon: LayoutDashboard },
      { label: 'Practice Tenants', path: '/admin/practices', icon: Building2 },
      { label: 'Platform Users', path: '/admin/users', icon: Users },
      { label: 'Marketplace Ops', path: '/admin/marketplace', icon: Store },
      { label: 'Subscriptions & MRR', path: '/admin/subscriptions', icon: CreditCard },
      { label: 'Feedback Ops', path: '/admin/feedback', icon: ShieldCheck },
      { label: 'Security & Audit', path: '/audit-logs', icon: ShieldAlert },
    ],
  },

  // 2. Taxoryn Platform Support Admin (Support & Feedback Triage)
  TAXORYN_SUPPORT_ADMIN: {
    roleCode: 'TAXORYN_SUPPORT_ADMIN',
    roleTitle: 'Platform Support',
    platformSubtitle: 'PLATFORM SUPPORT',
    dashboardType: 'support',
    defaultRoute: '/admin/overview',
    badgeStyle: 'bg-blue-100 text-blue-800 border-blue-200',
    navigation: [
      { label: 'Support Workspace', path: '/admin/overview', icon: Headphones },
      { label: 'Customer & Practice Lookup', path: '/admin/practices', icon: Search },
      { label: 'Feedback Ops', path: '/admin/feedback', icon: ShieldCheck },
      { label: 'Support Cases', path: '/admin/feedback', icon: LifeBuoy },
      { label: 'Knowledge Base', path: '/admin/overview', icon: BookOpen },
    ],
  },

  // 3. Taxoryn Operations Admin (Day-to-day operations & Verification)
  TAXORYN_OPERATIONS_ADMIN: {
    roleCode: 'TAXORYN_OPERATIONS_ADMIN',
    roleTitle: 'Platform Operations',
    platformSubtitle: 'PLATFORM OPERATIONS',
    dashboardType: 'superadmin',
    defaultRoute: '/admin/overview',
    badgeStyle: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    navigation: [
      { label: 'Platform Overview', path: '/admin/overview', icon: LayoutDashboard },
      { label: 'Practice Tenants', path: '/admin/practices', icon: Building2 },
      { label: 'Platform Users', path: '/admin/users', icon: Users },
      { label: 'Feedback Ops', path: '/admin/feedback', icon: ShieldCheck },
    ],
  },

  // 4. Taxoryn Marketplace Admin (Lead Matching & Disputes)
  TAXORYN_MARKETPLACE_ADMIN: {
    roleCode: 'TAXORYN_MARKETPLACE_ADMIN',
    roleTitle: 'Marketplace Operations',
    platformSubtitle: 'MARKETPLACE OPERATIONS',
    dashboardType: 'marketplace',
    defaultRoute: '/admin/marketplace',
    badgeStyle: 'bg-amber-100 text-amber-800 border-amber-200',
    navigation: [
      { label: 'Marketplace Overview', path: '/admin/marketplace', icon: Store },
      { label: 'Requirements', path: '/admin/marketplace', icon: FileText },
      { label: 'Enquiries', path: '/admin/marketplace', icon: Search },
      { label: 'Matches', path: '/admin/marketplace', icon: Sparkles },
      { label: 'Consultations', path: '/admin/marketplace', icon: Users },
      { label: 'Disputes', path: '/admin/marketplace', icon: BadgeAlert },
      { label: 'Marketplace Practices', path: '/admin/practices', icon: Building2 },
    ],
  },

  // 5. Taxoryn Finance Admin (SaaS Subscriptions & MRR)
  TAXORYN_FINANCE_ADMIN: {
    roleCode: 'TAXORYN_FINANCE_ADMIN',
    roleTitle: 'Platform Finance',
    platformSubtitle: 'PLATFORM FINANCE',
    dashboardType: 'finance',
    defaultRoute: '/admin/subscriptions',
    badgeStyle: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    navigation: [
      { label: 'Finance Overview', path: '/admin/subscriptions', icon: CreditCard },
      { label: 'Subscriptions & MRR', path: '/admin/subscriptions', icon: Layers },
      { label: 'Payments', path: '/admin/subscriptions', icon: Receipt },
      { label: 'Finance Reports', path: '/admin/subscriptions', icon: FileSpreadsheet },
    ],
  },

  // 6. Taxoryn Content Admin (Knowledge Base & Articles)
  TAXORYN_CONTENT_ADMIN: {
    roleCode: 'TAXORYN_CONTENT_ADMIN',
    roleTitle: 'Platform Content',
    platformSubtitle: 'PLATFORM CONTENT',
    dashboardType: 'content',
    defaultRoute: '/admin/overview',
    badgeStyle: 'bg-teal-100 text-teal-800 border-teal-200',
    navigation: [
      { label: 'Content Overview', path: '/admin/overview', icon: LayoutDashboard },
      { label: 'Articles', path: '/admin/overview', icon: FileText },
      { label: 'Videos', path: '/admin/overview', icon: Video },
      { label: 'Content Library', path: '/admin/overview', icon: BookOpen },
      { label: 'Content Analytics', path: '/admin/overview', icon: Activity },
    ],
  },

  // 7. Taxoryn Security Admin (Audit Trails & Governance)
  TAXORYN_SECURITY_ADMIN: {
    roleCode: 'TAXORYN_SECURITY_ADMIN',
    roleTitle: 'Platform Security',
    platformSubtitle: 'PLATFORM SECURITY',
    dashboardType: 'security',
    defaultRoute: '/audit-logs',
    badgeStyle: 'bg-rose-100 text-rose-800 border-rose-200',
    navigation: [
      { label: 'Security Overview', path: '/audit-logs', icon: ShieldAlert },
      { label: 'Security & Audit', path: '/audit-logs', icon: ShieldCheck },
      { label: 'Security Alerts', path: '/audit-logs', icon: BadgeAlert },
      { label: 'Access Reviews', path: '/audit-logs', icon: Users },
    ],
  },

  // 8. Taxoryn Engineering Admin (Platform Telemetry & Incidents)
  TAXORYN_ENGINEERING_ADMIN: {
    roleCode: 'TAXORYN_ENGINEERING_ADMIN',
    roleTitle: 'Platform Engineering',
    platformSubtitle: 'PLATFORM ENGINEERING',
    dashboardType: 'engineering',
    defaultRoute: '/admin/feedback',
    badgeStyle: 'bg-cyan-100 text-cyan-800 border-cyan-200',
    navigation: [
      { label: 'Engineering Overview', path: '/admin/feedback', icon: Server },
      { label: 'Platform Health', path: '/admin/overview', icon: Activity },
      { label: 'Integrations', path: '/admin/overview', icon: Layers },
      { label: 'Technical Incidents', path: '/admin/feedback', icon: ShieldAlert },
    ],
  },
};

export const resolveRoleWorkspace = (userRoles: string[] = []): RoleWorkspaceDefinition | null => {
  // Normalize roles to string array
  const roleStrings = userRoles.map((r: any) => (typeof r === 'string' ? r : r.code || ''));

  // Check exact matches in priority order
  for (const role of roleStrings) {
    if (ROLE_WORKSPACE_CONFIGS[role]) {
      return ROLE_WORKSPACE_CONFIGS[role];
    }
  }
  return null;
};

export const getWorkspaceDisplayName = (userRoles: string[] = []): string => {
  const ws = resolveRoleWorkspace(userRoles);
  return ws?.roleTitle || 'Taxoryn Platform';
};

export const getWorkspaceShortName = (userRoles: string[] = []): string => {
  const ws = resolveRoleWorkspace(userRoles);
  return ws?.platformSubtitle || 'PLATFORM SUPERADMIN';
};

export const getWorkspaceBadgeStyle = (userRoles: string[] = []): string => {
  const ws = resolveRoleWorkspace(userRoles);
  return ws?.badgeStyle || 'bg-purple-100 text-purple-800 border-purple-200';
};

