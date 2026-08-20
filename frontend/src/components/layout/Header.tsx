import React from 'react';
import { Search, Bell, Plus, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Header: React.FC = () => {
  const { user, practiceName } = useAuth();

  return (
    <header className="h-16 px-6 glass-header flex items-center justify-between gap-4 sticky top-0 z-30 select-none">
      {/* Search Input (Global Search) */}
      <div className="flex items-center gap-3">
        <div className="relative w-64 md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Quick search clients, GSTIN, PAN... (Ctrl+K)"
            className="w-full pl-9 pr-8 py-1.5 text-xs bg-slate-100/70 border border-slate-200/80 rounded-lg focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all placeholder:text-slate-400"
          />
          <kbd className="hidden sm:inline-block absolute right-2.5 top-1/2 -translate-y-1/2 px-1.5 py-0.5 text-[10px] font-semibold text-slate-400 bg-white border border-slate-200 rounded shadow-2xs">
            ⌘K
          </kbd>
        </div>
      </div>

      {/* Actions & Alerts */}
      <div className="flex items-center gap-3">
        {/* Quick Action Button */}
        <button className="hidden sm:inline-flex items-center gap-1.5 bg-brand-600 hover:bg-brand-700 text-white text-xs font-semibold px-3 py-1.5 rounded-lg shadow-sm transition-colors">
          <Plus className="w-4 h-4" />
          <span>New Action</span>
        </button>

        {/* Notifications Bell */}
        <button
          title="Notifications"
          className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white"></span>
        </button>

        {/* Vertical Divider */}
        <div className="h-6 w-px bg-slate-200" />

        {/* Role Pill */}
        <div className="hidden md:flex items-center gap-1.5 bg-slate-100 border border-slate-200/80 rounded-full px-3 py-1 text-xs text-slate-700 font-medium">
          <ShieldCheck className="w-3.5 h-3.5 text-brand-600" />
          <span>
            {(() => {
              if (!user?.roles || (Array.isArray(user.roles) && user.roles.length === 0)) return 'CA Admin';
              const r = Array.isArray(user.roles) ? user.roles[0] : user.roles;
              if (typeof r === 'string') return r;
              if (typeof r === 'object' && r !== null) return (r as any).name || (r as any).code || 'CA Admin';
              return 'CA Admin';
            })()}
          </span>
        </div>
      </div>
    </header>
  );
};
