import React from 'react';
import { Search, Bell, Plus, ShieldCheck, Server, LogOut, Menu } from 'lucide-react';
import clsx from 'clsx';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import { resolveRoleWorkspace } from '../../config/roleWorkspaceConfig';
import { NotificationBellDropdown } from '../notification/NotificationBellDropdown';

interface HeaderProps {
  /** Shows a hamburger button (mobile/tablet only) that opens the sidebar drawer. */
  onMenuClick?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  const { user, logout } = useAuth();
  const { currentTheme, getEmployeeAvatar } = useBranding();

  const userAvatar = getEmployeeAvatar(user?.email || user?.id);

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));

  const platformWorkspace = resolveRoleWorkspace(userRoleCodes);
  const isPlatformUser = !!platformWorkspace || userRoleCodes.some((r: string) => r.startsWith('TAXORYN_') || r === 'SUPER_ADMIN');

  // Practice / Organization Roles
  const isPracticeAdmin = !isPlatformUser && userRoleCodes.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'].includes(r));
  const isManager = !isPlatformUser && userRoleCodes.includes('MANAGER');
  const isPractitioner = !isPlatformUser && (userRoleCodes.includes('PRACTITIONER') || userRoleCodes.includes('TAX_PROFESSIONAL'));
  const isStaff = !isPlatformUser && !isPracticeAdmin && userRoleCodes.some((r: string) => ['PRACTICE_EMPLOYEE', 'ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE', 'ACCOUNTANT'].includes(r));

  // Customer Roles
  const isMarketplaceCustomer = userRoleCodes.includes('MARKETPLACE_CUSTOMER');
  const isClientAdmin = userRoleCodes.includes('CLIENT_ADMIN');
  const isClientUser = userRoleCodes.includes('CLIENT_USER') || userRoleCodes.includes('PRACTICE_CLIENT') || isClientAdmin || isMarketplaceCustomer;

  const getHeaderRoleLabel = () => {
    if (platformWorkspace?.roleTitle) return platformWorkspace.roleTitle;
    if (isPracticeAdmin) return 'Practice Admin';
    if (isManager) return 'Practice Manager';
    if (isPractitioner) return 'Tax Consultant';
    if (isStaff) return 'Practice Staff';
    if (isMarketplaceCustomer) return 'Marketplace Customer';
    if (isClientAdmin) return 'Client Admin';
    if (isClientUser) return 'Client';
    return 'User';
  };

  return (
    <header className="h-16 px-3 sm:px-6 glass-header flex items-center justify-between gap-2 sm:gap-4 sticky top-0 z-30 select-none">
      {/* Search Input (Global Search) */}
      <div className="flex items-center gap-3 min-w-0 flex-1">
        {/* Hamburger — mobile/tablet only, opens the sidebar drawer */}
        <button
          onClick={onMenuClick}
          className="lg:hidden shrink-0 p-2 -ml-1 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors"
          aria-label="Open navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="relative w-full max-w-[11rem] sm:max-w-none sm:w-64 md:w-80 min-w-0">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder={
              isPlatformUser
                ? "Search practices, platform users, leads, subscriptions... (Ctrl+K)"
                : isClientUser
                ? "Search filings, invoices, documents..."
                : "Quick search clients, GSTIN, PAN... (Ctrl+K)"
            }
            className="w-full pl-9 pr-8 py-1.5 text-xs bg-slate-100/70 border border-slate-200/80 rounded-lg focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all placeholder:text-slate-400"
          />
          <kbd className="hidden sm:inline-block absolute right-2.5 top-1/2 -translate-y-1/2 px-1.5 py-0.5 text-[10px] font-semibold text-slate-400 bg-white border border-slate-200 rounded shadow-2xs">
            ⌘K
          </kbd>
        </div>
      </div>

      {/* Actions & Alerts */}
      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        {/* Quick Action Button (Practice Staff Only) */}
        {!isClientUser && !isPlatformUser && (
          <button
            style={{ backgroundColor: currentTheme.primaryColor }}
            className="hidden sm:inline-flex items-center gap-1.5 text-white text-xs font-semibold px-3 py-1.5 rounded-lg shadow-sm hover:opacity-90 transition-opacity"
          >
            <Plus className="w-4 h-4" />
            <span>New Action</span>
          </button>
        )}

        {/* Notifications Bell Dropdown */}
        <NotificationBellDropdown />

        {/* Vertical Divider */}
        <div className="h-6 w-px bg-slate-200" />

        {/* User Avatar & Role Pill */}
        <div className="flex items-center gap-2">
          {userAvatar ? (
            <img
              src={userAvatar}
              alt={user?.firstName}
              className="w-8 h-8 rounded-full object-cover border border-slate-200 shadow-2xs"
            />
          ) : (
            <div
              className="w-8 h-8 rounded-full text-white font-bold text-xs flex items-center justify-center shadow-2xs"
              style={{ backgroundColor: isPlatformUser ? '#7C3AED' : currentTheme.primaryColor }}
            >
              {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
            </div>
          )}

          <div className={clsx(
            'hidden md:flex items-center gap-1.5 border rounded-full px-3 py-1 text-xs font-semibold',
            isPlatformUser
              ? 'bg-purple-50 text-purple-700 border-purple-200 shadow-2xs'
              : isClientUser
              ? 'bg-sky-50 text-sky-700 border-sky-200'
              : isPracticeAdmin
              ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
              : isPractitioner
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
              : isStaff
              ? 'bg-amber-50 text-amber-700 border-amber-200'
              : 'bg-slate-100 text-slate-700 border-slate-200'
          )}>
            {isPlatformUser ? (
              <Server className="w-3.5 h-3.5 text-purple-600" />
            ) : (
              <ShieldCheck className={clsx(
                'w-3.5 h-3.5',
                isClientUser
                  ? 'text-sky-600'
                  : isPracticeAdmin
                  ? 'text-indigo-600'
                  : isPractitioner
                  ? 'text-emerald-600'
                  : isStaff
                  ? 'text-amber-600'
                  : 'text-slate-600'
              )} />
            )}
            <span>{getHeaderRoleLabel()}</span>
          </div>

          {/* Quick Sign Out Button */}
          <button
            onClick={() => logout()}
            title="Sign Out"
            className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors ml-1"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
