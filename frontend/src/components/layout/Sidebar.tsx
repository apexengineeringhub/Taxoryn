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
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import clsx from 'clsx';

interface SidebarProps {
  collapsed?: boolean;
}

export const Sidebar: React.FC<SidebarProps> = () => {
  const { user, logout, practiceName, practiceInitials, subscriptionPlan } = useAuth();
  const { currentTheme, practiceLogo, getEmployeeAvatar } = useBranding();

  const userAvatar = getEmployeeAvatar(user?.email || user?.id);
  const isLight = currentTheme.mode === 'light';

  const navItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Clients 360°', path: '/clients', icon: Users },
    { label: 'Tasks & Workflow', path: '/tasks', icon: CheckSquare },
    { label: 'GST Compliance', path: '/gst', icon: Building2 },
    { label: 'ITR Compliance', path: '/itr', icon: FileSpreadsheet },
    { label: 'Tax Calendar', path: '/calendar', icon: Calendar },
    { label: 'Document Vault', path: '/documents', icon: FolderLock },
    { label: 'Billing & Invoices', path: '/billing', icon: Receipt },
    { label: 'Client Portal', path: '/portal', icon: Globe },
    { label: 'Team & RBAC', path: '/team', icon: UserCheck },
    { label: 'Audit Trails', path: '/audit-logs', icon: ShieldAlert },
    { label: 'Branding & Themes', path: '/settings/branding', icon: Palette },
    { label: 'Subscription', path: '/settings/subscription', icon: CreditCard },
  ];

  return (
    <aside
      className={clsx(
        'w-64 border-r flex flex-col h-screen select-none shrink-0 transition-colors duration-300',
        isLight ? 'text-slate-700 shadow-xs' : 'text-slate-300'
      )}
      style={{
        backgroundColor: currentTheme.sidebarBg,
        borderColor: currentTheme.sidebarBorder,
      }}
    >
      {/* Dynamic Practice Brand Header */}
      <div
        className="h-16 px-4 flex items-center justify-between border-b transition-colors"
        style={{
          backgroundColor: currentTheme.sidebarHeaderBg,
          borderColor: currentTheme.sidebarBorder,
        }}
      >
        <div className="flex items-center gap-2.5 min-w-0">
          {practiceLogo ? (
            <img
              src={practiceLogo}
              alt="Practice Logo"
              className={clsx(
                'w-9 h-9 object-contain rounded-lg p-0.5 shrink-0 border',
                isLight ? 'bg-white border-slate-200 shadow-2xs' : 'bg-white/10 border-white/10'
              )}
            />
          ) : (
            <div
              className="w-9 h-9 rounded-lg flex items-center justify-center text-white shadow-md font-black text-sm shrink-0 tracking-wider transition-colors"
              style={{ backgroundColor: currentTheme.primaryColor }}
            >
              {practiceInitials}
            </div>
          )}
          <div className="min-w-0">
            <span
              className={clsx('font-black text-xs tracking-tight block truncate', isLight ? 'text-slate-900' : 'text-white')}
              title={practiceName}
            >
              {practiceName}
            </span>
            <span
              className={clsx('text-[9px] font-bold tracking-wider uppercase block truncate', isLight ? 'text-slate-400' : 'text-slate-400')}
            >
              Tax Practice Platform
            </span>
          </div>
        </div>
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
              isLight ? 'text-emerald-700' : 'text-emerald-400'
            )}
          >
            <Sparkles className="w-3 h-3" /> {subscriptionPlan} Plan
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
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
          <div className="flex items-center gap-2.5 truncate">
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
              <p className={clsx('text-xs font-bold truncate', isLight ? 'text-slate-900' : 'text-white')}>
                {user?.firstName} {user?.lastName || ''}
              </p>
              <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
            </div>
          </div>
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
