import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ArrowRight, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import { resolveRoleWorkspace } from '../../config/roleWorkspaceConfig';
import {
  filterNavigationSections,
  filterNavigationByPermissions,
  NavigationSection,
  NavigationItem,
  NOTIFICATION_PERMISSIONS,
  NOTIFICATION_ADMIN_ROLES,
} from '../../utils/permissionUtils';
import clsx from 'clsx';

interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isSuperAdmin = userRoleCodes.some((r: string) =>
    ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_SUPPORT_ADMIN', 'TAXORYN_FINANCE_ADMIN', 'TAXORYN_MARKETPLACE_ADMIN', 'TAXORYN_CONTENT_ADMIN', 'TAXORYN_SECURITY_ADMIN', 'TAXORYN_ENGINEERING_ADMIN'].includes(r)
  );
  const isClientUser = userRoleCodes.some((r: string) =>
    ['CLIENT_USER', 'PRACTICE_CLIENT', 'CLIENT_ADMIN', 'MARKETPLACE_CUSTOMER'].includes(r)
  );
  const isStaff = !isSuperAdmin && !userRoleCodes.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'].includes(r)) && userRoleCodes.some((r: string) => ['PRACTICE_EMPLOYEE', 'ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE', 'ACCOUNTANT'].includes(r));

  // Build searchable items based on persona
  const getSearchableSections = (): { section: string; item: NavigationItem }[] => {
    if (isSuperAdmin) {
      const platformWorkspace = resolveRoleWorkspace(userRoleCodes);
      const items = platformWorkspace?.navigation || [];
      const accessible = filterNavigationByPermissions(items, user);
      return accessible.map((item) => ({ section: 'PLATFORM', item }));
    }

    if (isClientUser) {
      const clientItems: NavigationItem[] = [
        { label: 'Portal Dashboard', path: '/portal' },
        { label: 'GST Returns', path: '/portal?tab=gst' },
        { label: 'ITR Returns', path: '/portal?tab=itr' },
        { label: 'TDS Statements', path: '/portal?tab=tds' },
        { label: 'Invoices & Due Bills', path: '/portal?tab=invoices' },
        { label: 'Document Vault', path: '/portal?tab=documents' },
        { label: 'Find a Tax Professional', path: '/marketplace/explore' },
        { label: 'Security & Password', path: '/settings/security' },
        { label: 'Give Feedback', path: '/feedback' },
      ];
      const accessible = filterNavigationByPermissions(clientItems, user);
      return accessible.map((item) => ({ section: 'PORTAL', item }));
    }

    // Practice Sections
    const practiceSections: NavigationSection[] = [
      {
        id: 'work',
        sectionTitle: 'WORK',
        items: [
          { label: 'Dashboard', path: '/dashboard' },
          { label: isStaff ? 'My Assigned Clients' : 'Clients 360°', path: '/clients', requiredPermissions: ['CLIENT_VIEW'] },
          { label: isStaff ? 'My Assigned Tasks' : 'Tasks & Workflow', path: '/tasks', requiredPermissions: ['TASK_VIEW'] },
        ],
      },
      {
        id: 'compliance',
        sectionTitle: 'COMPLIANCE',
        items: [
          { label: 'GST Compliance', path: '/gst', requiredPermissions: ['GST_VIEW'] },
          { label: 'ITR Compliance', path: '/itr', requiredPermissions: ['ITR_VIEW'] },
          { label: 'TDS Compliance', path: '/tds', requiredPermissions: ['ITR_VIEW', 'GST_VIEW', 'TASK_VIEW'] },
          { label: 'Notice Center', path: '/notices', requiredPermissions: ['NOTICE_VIEW'] },
          { label: 'Tax Calendar', path: '/calendar', requiredPermissions: ['TASK_VIEW', 'GST_VIEW', 'ITR_VIEW'] },
        ],
      },
      {
        id: 'documents',
        sectionTitle: 'DOCUMENTS',
        items: [
          { label: 'Document Vault', path: '/documents', requiredPermissions: ['DOCUMENT_VIEW'] },
        ],
      },
      {
        id: 'practice',
        sectionTitle: 'PRACTICE',
        items: [
          { label: 'Client Portal Hub', path: '/portal', requiredPermissions: ['CLIENT_VIEW', 'CLIENT_UPDATE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'Reports', path: '/reports', requiredPermissions: ['REPORT_VIEW', 'REPORTS_VIEW'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER'] },
          { label: 'Inbound Leads (CRM)', path: '/marketplace/leads', requiredPermissions: ['MARKETPLACE_LEAD_VIEW', 'MARKETPLACE_LEAD_MANAGE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'Client Onboarding', path: '/marketplace/onboarding', requiredPermissions: ['MARKETPLACE_ONBOARDING_MANAGE', 'CLIENT_CREATE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'Notification Center', path: '/notifications', requiredPermissions: NOTIFICATION_PERMISSIONS, allowedRoles: NOTIFICATION_ADMIN_ROLES },
        ],
      },
      {
        id: 'administration',
        sectionTitle: 'ADMINISTRATION',
        items: [
          { label: isStaff ? 'Department Team' : 'Team & RBAC', path: '/team', requiredPermissions: ['USER_VIEW', 'ROLE_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'Billing & Invoices', path: '/billing', requiredPermissions: ['BILLING_VIEW', 'BILLING_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT'] },
          { label: 'Activity & Audit', path: '/audit-logs', requiredPermissions: ['AUDIT_VIEW', 'AUDIT_READ'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER', 'TAX_PROFESSIONAL', 'PRACTITIONER', 'ACCOUNTANT'] },
          { label: 'Branding & Themes', path: '/settings/branding', requiredPermissions: ['ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'Subscription', path: '/settings/subscription', requiredPermissions: ['SUBSCRIPTION_VIEW', 'ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
          { label: 'WhatsApp Alerts', path: '/settings/whatsapp', requiredPermissions: ['COMMUNICATION_MANAGE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        ],
      },
      {
        id: 'growth',
        sectionTitle: 'GROWTH',
        items: [
          { label: 'Marketplace', path: '/settings/marketplace', requiredPermissions: ['MARKETPLACE_MANAGE', 'ORGANIZATION_UPDATE', 'ORG_WRITE'], allowedRoles: ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'] },
        ],
      },
      {
        id: 'settings',
        sectionTitle: 'SETTINGS',
        items: [
          { label: 'Security & Password', path: '/settings/security' },
          { label: 'Give Feedback', path: '/feedback' },
        ],
      },
    ];

    const filteredSections = filterNavigationSections(practiceSections, user);
    const flatResults: { section: string; item: NavigationItem }[] = [];
    filteredSections.forEach((sec) => {
      sec.items.forEach((item) => {
        flatResults.push({ section: sec.sectionTitle || 'MENU', item });
      });
    });
    return flatResults;
  };

  const allAccessibleItems = getSearchableSections();

  const filteredItems = allAccessibleItems.filter((entry) => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return true;
    return (
      entry.item.label.toLowerCase().includes(query) ||
      entry.section.toLowerCase().includes(query) ||
      entry.item.path.toLowerCase().includes(query)
    );
  });

  useEffect(() => {
    if (isOpen) {
      setSearchQuery('');
      setSelectedIndex(0);
      setTimeout(() => {
        inputRef.current?.focus();
      }, 50);
    }
  }, [isOpen]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [searchQuery]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen) return;

      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev < filteredItems.length - 1 ? prev + 1 : 0));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((prev) => (prev > 0 ? prev - 1 : filteredItems.length - 1));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filteredItems[selectedIndex]) {
          navigate(filteredItems[selectedIndex].item.path);
          onClose();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, selectedIndex, filteredItems, navigate, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-start justify-center p-4 sm:pt-20 animate-fade-in">
      {/* Backdrop click */}
      <div className="fixed inset-0" onClick={onClose} />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden z-10 animate-scale-up">
        {/* Search Header */}
        <div className="flex items-center px-4 py-3.5 border-b border-slate-100 gap-3">
          <Search className="w-5 h-5 text-slate-400 shrink-0" />
          <input
            ref={inputRef}
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search workspace navigation... (e.g. GST, Clients, Tasks)"
            className="w-full text-sm bg-transparent outline-none placeholder:text-slate-400 font-medium text-slate-800"
          />
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Search Results List */}
        <div className="max-h-80 overflow-y-auto p-2 divide-y divide-slate-50">
          {filteredItems.length === 0 ? (
            <div className="py-10 text-center text-slate-400">
              <Search className="w-8 h-8 text-slate-300 mx-auto mb-2" />
              <p className="text-xs font-semibold text-slate-500">No matching accessible navigation items</p>
              <p className="text-[11px] text-slate-400 mt-0.5">Try searching with a different term</p>
            </div>
          ) : (
            filteredItems.map((entry, idx) => {
              const isSelected = idx === selectedIndex;
              return (
                <div
                  key={`${entry.item.path}-${idx}`}
                  onClick={() => {
                    navigate(entry.item.path);
                    onClose();
                  }}
                  onMouseEnter={() => setSelectedIndex(idx)}
                  className={clsx(
                    'flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold cursor-pointer transition-all',
                    isSelected
                      ? 'bg-purple-50 text-purple-900 border border-purple-200 shadow-2xs'
                      : 'text-slate-700 hover:bg-slate-50 border border-transparent'
                  )}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <span
                      className={clsx(
                        'text-[9px] font-black uppercase px-2 py-0.5 rounded border shrink-0 tracking-wider',
                        isSelected
                          ? 'bg-purple-100 text-purple-800 border-purple-300'
                          : 'bg-slate-100 text-slate-500 border-slate-200'
                      )}
                    >
                      {entry.section}
                    </span>
                    <span className="truncate font-bold text-slate-900">{entry.item.label}</span>
                  </div>

                  <div className="flex items-center gap-1.5 text-[11px] text-slate-400 shrink-0">
                    <span className="font-mono text-[10px] text-slate-400">{entry.item.path}</span>
                    <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer shortcuts */}
        <div className="px-4 py-2 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 text-[10px] font-mono bg-white border border-slate-200 rounded">↑↓</kbd> Navigate
            </span>
            <span className="inline-flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 text-[10px] font-mono bg-white border border-slate-200 rounded">↵</kbd> Select
            </span>
            <span className="inline-flex items-center gap-1">
              <kbd className="px-1.5 py-0.5 text-[10px] font-mono bg-white border border-slate-200 rounded">Esc</kbd> Close
            </span>
          </div>
          <span className="font-medium text-slate-400">Taxoryn Permission-Aware Search</span>
        </div>
      </div>
    </div>
  );
};