import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  CheckSquare,
  Building2,
  FileSpreadsheet,
  Calendar,
  FolderLock,
  Receipt,
  Globe,
  UserCheck,
  ShieldAlert,
  CreditCard,
  Palette,
  LogOut,
  Sparkles,
  Percent,
  Store,
  Search,
  ShieldCheck,
  MessageSquare,
  MessageSquarePlus,
  Server,
  Lock,
  Bell,
  BarChart3,
  X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import { resolveRoleWorkspace } from '../../config/roleWorkspaceConfig';
import { TaxorynLogo } from '../common/TaxorynLogo';
import clsx from 'clsx';

interface SidebarProps {
  collapsed?: boolean;
  /** Mobile/tablet only: whether the slide-in drawer is open. Ignored at lg+ where the sidebar is always visible. */
  isOpen?: boolean;
  /** Mobile/tablet only: called when a nav link is tapped or the backdrop is clicked. */
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen = false, onClose }) => {
  const { user, logout, practiceName, practiceInitials, subscriptionPlan } = useAuth();
  const { currentTheme, practiceLogo, getEmployeeAvatar } = useBranding();

  const userAvatar = getEmployeeAvatar(user?.email || user?.id);
  const isLight = currentTheme.mode === 'light';

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isTaxorynSuperAdmin = userRoleCodes.includes('TAXORYN_SUPERADMIN') || userRoleCodes.includes('SUPER_ADMIN');
  const isTaxorynOpsAdmin = userRoleCodes.includes('TAXORYN_OPERATIONS_ADMIN');
  const isTaxorynSupportAdmin = userRoleCodes.includes('TAXORYN_SUPPORT_ADMIN');
  const isTaxorynFinanceAdmin = userRoleCodes.includes('TAXORYN_FINANCE_ADMIN');
  const isTaxorynMarketplaceAdmin = userRoleCodes.includes('TAXORYN_MARKETPLACE_ADMIN');
  const isTaxorynContentAdmin = userRoleCodes.includes('TAXORYN_CONTENT_ADMIN');
  const isTaxorynSecurityAdmin = userRoleCodes.includes('TAXORYN_SECURITY_ADMIN');
  const isTaxorynEngineeringAdmin = userRoleCodes.includes('TAXORYN_ENGINEERING_ADMIN');
  const isSuperAdmin = isTaxorynSuperAdmin || isTaxorynOpsAdmin || isTaxorynSupportAdmin || isTaxorynFinanceAdmin || isTaxorynMarketplaceAdmin || isTaxorynContentAdmin || isTaxorynSecurityAdmin || isTaxorynEngineeringAdmin;

  const isFirmAdmin = !isSuperAdmin && userRoleCodes.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'].includes(r));
  const isStaff = !isSuperAdmin && !isFirmAdmin && userRoleCodes.some((r: string) => ['PRACTICE_EMPLOYEE', 'ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE', 'ACCOUNTANT'].includes(r));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'PRACTICE_CLIENT', 'CLIENT_ADMIN', 'MARKETPLACE_CUSTOMER'].includes(r));
  const userPermissions = user?.permissions || [];
  const hasBillingAccess = isFirmAdmin || userPermissions.includes('BILLING_VIEW') || userPermissions.includes('BILLING_READ');

  // Dynamic Workspace Definition
  const platformWorkspace = resolveRoleWorkspace(userRoleCodes);

  const getPlatformSubtitle = () => {
    if (platformWorkspace?.platformSubtitle) return platformWorkspace.platformSubtitle;
    if (isTaxorynSupportAdmin) return 'PLATFORM SUPPORT';
    if (isTaxorynFinanceAdmin) return 'PLATFORM FINANCE';
    if (isTaxorynMarketplaceAdmin) return 'MARKETPLACE OPERATIONS';
    if (isTaxorynSecurityAdmin) return 'PLATFORM SECURITY';
    if (isTaxorynOpsAdmin) return 'PLATFORM OPERATIONS';
    if (isTaxorynContentAdmin) return 'PLATFORM CONTENT';
    if (isTaxorynEngineeringAdmin) return 'PLATFORM ENGINEERING';
    return 'PLATFORM SUPERADMIN';
  };

  // 1. Platform SuperAdmin & Platform Role Nav Items (Strictly role-resolved)
  const platformNavItems = platformWorkspace?.navigation?.map(item => ({
    ...item,
    visible: true,
  })) || [
    { label: 'Platform Overview', path: '/admin/overview', icon: LayoutDashboard, visible: true },
  ];

  // 2. Client / Taxpayer Customer Portal Nav Items
  const clientNavItems = [
    { label: 'Portal Dashboard', path: '/portal', icon: LayoutDashboard, visible: true },
    { label: 'GST Returns', path: '/portal?tab=gst', icon: Building2, visible: true },
    { label: 'ITR Returns', path: '/portal?tab=itr', icon: FileSpreadsheet, visible: true },
    { label: 'TDS Statements', path: '/portal?tab=tds', icon: Percent, visible: true },
    { label: 'Invoices & Due Bills', path: '/portal?tab=invoices', icon: Receipt, visible: true },
    { label: 'Document Vault', path: '/portal?tab=documents', icon: FolderLock, visible: true },
    { label: 'Notification Center', path: '/notifications', icon: Bell, visible: true },
    { label: 'Find CA / CS / Advocates', path: '/marketplace', icon: Search, visible: true },
    { label: 'Security & Password', path: '/settings/security', icon: Lock, visible: true },
    { label: 'Give Feedback', path: '/feedback', icon: MessageSquarePlus, visible: true },
  ];

  // 3. Practice Operations Suite (Tenant Admin & Practice Staff)
  const practiceNavItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard, visible: true },
    { label: isStaff ? 'My Assigned Clients' : 'Clients 360°', path: '/clients', icon: Users, visible: true },
    { label: isStaff ? 'My Assigned Tasks' : 'Tasks & Workflow', path: '/tasks', icon: CheckSquare, visible: true },
    { label: 'GST Compliance', path: '/gst', icon: Building2, visible: true },
    { label: 'ITR Compliance', path: '/itr', icon: FileSpreadsheet, visible: true },
    { label: 'TDS Compliance', path: '/tds', icon: Percent, visible: true },
    { label: 'Tax Calendar', path: '/calendar', icon: Calendar, visible: true },
    { label: 'Document Vault', path: '/documents', icon: FolderLock, visible: true },
    { label: 'Billing & Invoices', path: '/billing', icon: Receipt, visible: hasBillingAccess },
    { label: 'Reports', path: '/reports', icon: BarChart3, visible: true },
    { label: 'Notification Center', path: '/notifications', icon: Bell, visible: true },
    { label: 'Inbound Leads (CRM)', path: '/marketplace/leads', icon: Store, visible: isFirmAdmin },
    { label: 'Client Onboarding', path: '/marketplace/onboarding', icon: UserCheck, visible: isFirmAdmin },
    { label: 'Client Portal Hub', path: '/portal', icon: Globe, visible: isFirmAdmin },
    { label: isStaff ? 'Department Team' : 'Team & RBAC', path: '/team', icon: UserCheck, visible: true },
    { label: 'Audit Trails', path: '/audit-logs', icon: ShieldAlert, visible: isFirmAdmin },
    { label: 'Feedback Ops', path: '/admin/feedback', icon: ShieldCheck, visible: isFirmAdmin },
    { label: 'Branding & Themes', path: '/settings/branding', icon: Palette, visible: isFirmAdmin },
    { label: 'Marketplace', path: '/settings/marketplace', icon: Sparkles, visible: isFirmAdmin },
    { label: 'WhatsApp Alerts', path: '/settings/whatsapp', icon: MessageSquare, visible: isFirmAdmin },
    { label: 'Subscription', path: '/settings/subscription', icon: CreditCard, visible: isFirmAdmin },
    { label: 'Security & Password', path: '/settings/security', icon: Lock, visible: true },
    { label: 'Give Feedback', path: '/feedback', icon: MessageSquarePlus, visible: true },
  ];

  const navItems = (
    isSuperAdmin ? platformNavItems : isClientUser ? clientNavItems : practiceNavItems
  ).filter((item) => item.visible);

  return (
    <aside
      className={clsx(
        'w-64 border-r flex flex-col h-dvh select-none shrink-0 transition-transform duration-300',
        // Below lg: fixed slide-in drawer, off-screen unless isOpen. At lg+: always visible, static.
        'fixed inset-y-0 left-0 z-50 lg:static lg:translate-x-0',
        isOpen ? 'translate-x-0' : '-translate-x-full',
        isLight ? 'text-slate-700 shadow-xs' : 'text-slate-300'
      )}
      style={{
        backgroundColor: currentTheme.sidebarBg,
        borderColor: currentTheme.sidebarBorder,
      }}
    >
      {/* Brand Header */}
      <div
        className="h-16 px-4 flex items-center justify-between border-b transition-colors"
        style={{
          backgroundColor: currentTheme.sidebarHeaderBg,
          borderColor: currentTheme.sidebarBorder,
        }}
      >
        <div className="flex items-center gap-2.5 min-w-0">
          {isSuperAdmin ? (
            <div className="w-9 h-9 rounded-xl bg-[#082E5B] flex items-center justify-center p-1 shadow-xs shrink-0 border border-white/10">
              <TaxorynLogo variant="symbol" theme="dark" size="xs" />
            </div>
          ) : practiceLogo ? (
            <img
              src={practiceLogo}
              alt="Practice Logo"
              className={clsx(
                'w-9 h-9 object-contain rounded-lg p-0.5 shrink-0 border',
                isLight ? 'bg-white border-slate-200 shadow-2xs' : 'bg-white/10 border-white/10'
              )}
            />
          ) : (
            <div className="w-9 h-9 rounded-xl bg-[#082E5B] flex items-center justify-center p-1 shadow-xs shrink-0 border border-white/10">
              <TaxorynLogo variant="symbol" theme="dark" size="xs" />
            </div>
          )}
          <div className="min-w-0">
            <span
              className={clsx('font-black text-xs tracking-tight block truncate', isLight ? 'text-slate-900' : 'text-white')}
              title={isSuperAdmin ? 'Taxoryn Platform Operations' : practiceName}
            >
              {isSuperAdmin ? 'Taxoryn Platform' : practiceName}
            </span>
            <span
              className={clsx('text-[9px] font-bold tracking-wider uppercase block truncate', isLight ? 'text-slate-400' : 'text-slate-400')}
            >
              {isSuperAdmin ? getPlatformSubtitle() : 'Tax Practice Platform'}
            </span>
          </div>
        </div>
        {/* Mobile-only close button for the drawer */}
        <button
          onClick={onClose}
          className={clsx(
            'lg:hidden p-2 -mr-1 rounded-md shrink-0 transition-colors',
            isLight ? 'text-slate-400 hover:text-slate-700 hover:bg-slate-100' : 'text-slate-400 hover:text-white hover:bg-white/10'
          )}
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Tenant Indicator */}
      <div
        className={clsx('px-4 py-2 border-b flex items-center justify-between', isLight ? 'bg-slate-50/60' : 'bg-black/10')}
        style={{ borderColor: currentTheme.sidebarBorder }}
      >
        <div className="truncate">
          <span
            className={clsx(
              'inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider',
              isSuperAdmin ? 'text-purple-700' : isLight ? 'text-emerald-700' : 'text-emerald-400'
            )}
          >
            {isSuperAdmin ? (
              <>
                <Server className="w-3 h-3 text-purple-600" /> Platform Multi-Tenant
              </>
            ) : (
              <>
                <Sparkles className="w-3 h-3" /> {subscriptionPlan} Plan
              </>
            )}
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            onClick={onClose}
            style={({ isActive }) =>
              isActive ? { backgroundColor: currentTheme.primaryColor, color: '#FFFFFF' } : {}
            }
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all group',
                isActive
                  ? 'text-white shadow-sm font-bold'
                  : isLight
                  ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/90'
                  : 'text-slate-400 hover:text-white hover:bg-white/10'
              )
            }
          >
            <item.icon className="w-4 h-4 shrink-0 transition-colors" />
            <span className="truncate">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User Footer */}
      <div
        className={clsx('p-3 border-t', isLight ? 'bg-slate-50/80' : 'bg-black/20')}
        style={{ borderColor: currentTheme.sidebarBorder }}
      >
        <div
          className={clsx('flex items-center justify-between p-2 rounded-lg border', isLight ? 'bg-white border-slate-200/80 shadow-2xs' : 'bg-white/5 border-white/5')}
        >
          <NavLink
            to="/settings/security"
            title="Account Security & Password Settings"
            className="flex items-center gap-2.5 truncate hover:opacity-90 transition-opacity flex-1 min-w-0"
          >
            {userAvatar ? (
              <img
                src={userAvatar}
                alt={user?.firstName}
                className="w-7 h-7 rounded-full object-cover border border-slate-200 shrink-0 shadow-2xs"
              />
            ) : (
              <div
                className="w-7 h-7 rounded-full text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-2xs"
                style={{ backgroundColor: currentTheme.primaryColor }}
              >
                {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
              </div>
            )}
            <div className="truncate">
              <p className={clsx('text-xs font-bold truncate flex items-center gap-1.5', isLight ? 'text-slate-900' : 'text-white')}>
                <span>{user?.firstName} {user?.lastName || ''}</span>
                <span className={clsx(
                  'text-[9px] px-1.5 py-0.2 rounded font-semibold uppercase tracking-wider',
                  isSuperAdmin
                    ? 'bg-purple-100 text-purple-800 border border-purple-200'
                    : isClientUser
                    ? 'bg-sky-100 text-sky-700 border border-sky-200/60'
                    : isFirmAdmin
                    ? 'bg-blue-100 text-blue-700 border border-blue-200/60'
                    : isStaff
                    ? 'bg-amber-100 text-amber-700 border border-amber-200/60'
                    : 'bg-emerald-100 text-emerald-700 border border-emerald-200/60'
                )}>
                  {isTaxorynSuperAdmin
                    ? 'SuperAdmin'
                    : isTaxorynOpsAdmin
                    ? 'Ops Admin'
                    : isTaxorynSupportAdmin
                    ? 'Support'
                    : isTaxorynFinanceAdmin
                    ? 'Finance'
                    : isTaxorynMarketplaceAdmin
                    ? 'Marketplace'
                    : isTaxorynContentAdmin
                    ? 'Content'
                    : isTaxorynSecurityAdmin
                    ? 'Security'
                    : isTaxorynEngineeringAdmin
                    ? 'Engineering'
                    : isClientUser
                    ? userRoleCodes.includes('CLIENT_ADMIN')
                      ? 'Client Admin'
                      : userRoleCodes.includes('MARKETPLACE_CUSTOMER')
                      ? 'Customer'
                      : 'Client'
                    : isFirmAdmin
                    ? 'Practice Admin'
                    : isStaff
                    ? 'Practice Staff'
                    : 'Tax Consultant'}
                </span>
              </p>
              <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
            </div>
          </NavLink>
          <button
            onClick={logout}
            title="Sign Out"
            className={clsx(
              'p-1.5 rounded-md transition-colors',
              isLight ? 'text-slate-400 hover:text-rose-600 hover:bg-slate-100' : 'text-slate-400 hover:text-rose-400 hover:bg-white/10'
            )}
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
