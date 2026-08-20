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
  LogOut,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import clsx from 'clsx';

interface SidebarProps {
  collapsed?: boolean;
}

export const Sidebar: React.FC<SidebarProps> = () => {
  const { user, logout } = useAuth();

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
    { label: 'Subscription', path: '/settings/subscription', icon: CreditCard },
  ];

  return (
    <aside className="w-64 bg-obsidian-900 border-r border-obsidian-800 text-slate-300 flex flex-col h-screen select-none shrink-0 transition-all">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center justify-between border-b border-obsidian-800 bg-obsidian-950/40">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center text-white shadow-lg shadow-brand-500/20 font-black text-lg">
            TX
          </div>
          <div>
            <span className="font-extrabold text-base tracking-tight text-white block">Taxoryn</span>
            <span className="text-[10px] text-slate-400 font-semibold tracking-wider uppercase block">Practice SaaS</span>
          </div>
        </div>
      </div>

      {/* Tenant Indicator */}
      <div className="px-4 py-3 border-b border-obsidian-800/80 bg-obsidian-800/20 flex items-center justify-between">
        <div className="truncate">
          <p className="text-xs font-semibold text-white truncate">{user?.organizationName || 'Tax Practice Hub'}</p>
          <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-medium">
            <Sparkles className="w-3 h-3" /> Professional Plan
          </span>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-all group',
                isActive
                  ? 'bg-brand-600 text-white shadow-sm font-semibold'
                  : 'text-slate-400 hover:text-white hover:bg-obsidian-800/80'
              )
            }
          >
            <item.icon className="w-4 h-4 shrink-0 transition-colors" />
            <span className="truncate">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User Footer */}
      <div className="p-3 border-t border-obsidian-800 bg-obsidian-950/30">
        <div className="flex items-center justify-between p-2 rounded-lg bg-obsidian-800/40">
          <div className="flex items-center gap-2.5 truncate">
            <div className="w-7 h-7 rounded-full bg-brand-500/20 border border-brand-500/40 text-brand-300 font-bold text-xs flex items-center justify-center">
              {user?.firstName ? user.firstName.charAt(0) : 'U'}
            </div>
            <div className="truncate">
              <p className="text-xs font-medium text-white truncate">{user?.firstName} {user?.lastName || ''}</p>
              <p className="text-[10px] text-slate-400 truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            title="Sign Out"
            className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-obsidian-700 rounded-md transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
