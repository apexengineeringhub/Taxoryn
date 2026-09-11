import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Plus,
  UserPlus,
  CheckSquare,
  FileText,
  ChevronRight,
  ChevronLeft,
  Building2,
  FileSpreadsheet,
  Percent,
  UserCheck,
  Receipt,
  ShieldCheck,
  MessageSquare,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import { hasPermission } from '../../utils/permissionUtils';
import clsx from 'clsx';

export interface ActionDefinition {
  id: string;
  category: 'CLIENT' | 'WORK' | 'DOCUMENTS' | 'COMPLIANCE' | 'COMMUNICATION' | 'TEAM' | 'BILLING';
  categoryLabel: string;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  requiredPermissions: string[];
  allowedRoles: string[];
  targetPath: string;
  supportsClientContext?: boolean;
}

export interface ComplianceSubmenuAction {
  id: string;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  requiredPermissions: string[];
  allowedRoles: string[];
  targetPath: string;
  supportsClientContext?: boolean;
}

// Compliance submenu definitions
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

// Top-level action definitions
export const ACTION_DEFINITIONS: ActionDefinition[] = [
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

type MenuView = 'MAIN' | 'COMPLIANCE';

export const NewActionMenu: React.FC = () => {
  const { user } = useAuth();
  const { currentTheme } = useBranding();
  const navigate = useNavigate();
  const location = useLocation();

  const [isOpen, setIsOpen] = useState(false);
  const [activeView, setActiveView] = useState<MenuView>('MAIN');
  const [highlightedIndex, setHighlightedIndex] = useState<number>(-1);
  const menuRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Extract current client context if present in current URL
  const currentClientId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('clientId') || '';
  }, [location.search]);

  // Authorized compliance children
  const authorizedComplianceActions = useMemo(() => {
    return COMPLIANCE_SUBMENU_ACTIONS.filter((subAction) =>
      hasPermission(user, subAction.requiredPermissions, subAction.allowedRoles)
    );
  }, [user]);

  // Authorized top-level actions
  const authorizedMainActions = useMemo(() => {
    return ACTION_DEFINITIONS.filter((action) => {
      if (action.id === 'start-compliance') {
        // Parent appears if and only if at least 1 child is authorized
        return authorizedComplianceActions.length > 0;
      }
      return hasPermission(user, action.requiredPermissions, action.allowedRoles);
    });
  }, [user, authorizedComplianceActions]);

  // Current items based on active view
  const currentItems = useMemo(() => {
    return activeView === 'MAIN' ? authorizedMainActions : authorizedComplianceActions;
  }, [activeView, authorizedMainActions, authorizedComplianceActions]);

  // Reset view & highlighted index when menu opens or closes
  useEffect(() => {
    if (isOpen) {
      setActiveView('MAIN');
      setHighlightedIndex(0);
    } else {
      setActiveView('MAIN');
      setHighlightedIndex(-1);
    }
  }, [isOpen]);

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        menuRef.current &&
        !menuRef.current.contains(e.target as Node) &&
        buttonRef.current &&
        !buttonRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const executeAction = (action: ActionDefinition | ComplianceSubmenuAction) => {
    if (action.id === 'start-compliance') {
      setActiveView('COMPLIANCE');
      setHighlightedIndex(0);
      return;
    }

    setIsOpen(false);
    let target = action.targetPath;

    // Propagate active client context if supported and available
    if (action.supportsClientContext && currentClientId) {
      const sep = target.includes('?') ? '&' : '?';
      target = `${target}${sep}clientId=${encodeURIComponent(currentClientId)}`;
    }

    navigate(target);
  };

  // Keyboard navigation & accessibility
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
        e.preventDefault();
        setIsOpen(true);
      }
      return;
    }

    switch (e.key) {
      case 'Escape':
        e.preventDefault();
        if (activeView === 'COMPLIANCE') {
          setActiveView('MAIN');
          const complianceIdx = authorizedMainActions.findIndex((a) => a.id === 'start-compliance');
          setHighlightedIndex(complianceIdx >= 0 ? complianceIdx : 0);
        } else {
          setIsOpen(false);
          buttonRef.current?.focus();
        }
        break;

      case 'ArrowLeft':
        if (activeView === 'COMPLIANCE') {
          e.preventDefault();
          setActiveView('MAIN');
          const complianceIdx = authorizedMainActions.findIndex((a) => a.id === 'start-compliance');
          setHighlightedIndex(complianceIdx >= 0 ? complianceIdx : 0);
        }
        break;

      case 'ArrowRight':
        if (activeView === 'MAIN' && highlightedIndex >= 0 && highlightedIndex < authorizedMainActions.length) {
          const current = authorizedMainActions[highlightedIndex];
          if (current.id === 'start-compliance') {
            e.preventDefault();
            setActiveView('COMPLIANCE');
            setHighlightedIndex(0);
          }
        }
        break;

      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev < currentItems.length - 1 ? prev + 1 : 0
        );
        break;

      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev > 0 ? prev - 1 : currentItems.length - 1
        );
        break;

      case 'Enter':
      case ' ':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < currentItems.length) {
          executeAction(currentItems[highlightedIndex]);
        }
        break;

      case 'Tab':
        setIsOpen(false);
        break;

      default:
        break;
    }
  };

  // If no action is authorized across all categories, hide the launcher
  if (authorizedMainActions.length === 0) {
    return null;
  }

  return (
    <div className="relative inline-block text-left" onKeyDown={handleKeyDown}>
      {/* Global New Action Button */}
      <button
        ref={buttonRef}
        type="button"
        onClick={() => {
          setIsOpen((prev) => !prev);
        }}
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label="New Action Launcher"
        style={{ backgroundColor: currentTheme.primaryColor }}
        className="hidden sm:inline-flex items-center gap-1.5 text-white text-xs font-semibold px-3 py-1.5 rounded-lg shadow-sm hover:opacity-90 active:scale-95 transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500 cursor-pointer"
      >
        <Plus className="w-4 h-4 shrink-0" />
        <span>New Action</span>
      </button>

      {/* Popover Command Menu */}
      {isOpen && (
        <div
          ref={menuRef}
          role="menu"
          aria-orientation="vertical"
          aria-labelledby="new-action-menu"
          className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white shadow-2xl border border-slate-200/90 py-2.5 z-50 animate-in fade-in slide-in-from-top-2 duration-150 focus:outline-none"
        >
          {/* Header context */}
          <div className="px-4 py-2 border-b border-slate-100 flex items-center justify-between">
            {activeView === 'COMPLIANCE' ? (
              <button
                type="button"
                onClick={() => {
                  setActiveView('MAIN');
                  const compIdx = authorizedMainActions.findIndex((a) => a.id === 'start-compliance');
                  setHighlightedIndex(compIdx >= 0 ? compIdx : 0);
                }}
                className="inline-flex items-center gap-1 text-[11px] font-bold text-brand-600 hover:text-brand-700 cursor-pointer"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Back to Actions</span>
              </button>
            ) : (
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                What would you like to do?
              </span>
            )}

            {currentClientId && (
              <span className="text-[10px] font-bold text-brand-600 bg-brand-50 px-2 py-0.5 rounded-md border border-brand-200/60 truncate max-w-[140px]">
                Client Context Active
              </span>
            )}
          </div>

          {/* MAIN VIEW */}
          {activeView === 'MAIN' && (
            <div className="divide-y divide-slate-100/80">
              {authorizedMainActions.map((action, index) => {
                const Icon = action.icon;
                const isHighlighted = highlightedIndex === index;
                const isSubmenuTrigger = action.id === 'start-compliance';

                return (
                  <div key={action.id} className="py-1">
                    <div className="px-4 pt-1.5 pb-0.5">
                      <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400 font-mono">
                        {action.categoryLabel}
                      </span>
                    </div>

                    <button
                      type="button"
                      role="menuitem"
                      tabIndex={0}
                      onClick={() => executeAction(action)}
                      onMouseEnter={() => setHighlightedIndex(index)}
                      className={clsx(
                        'w-full text-left px-4 py-2.5 flex items-center justify-between gap-3 transition-colors cursor-pointer group',
                        isHighlighted ? 'bg-slate-50 text-slate-900' : 'text-slate-700 hover:bg-slate-50'
                      )}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div
                          className={clsx(
                            'w-8 h-8 rounded-xl flex items-center justify-center shrink-0 transition-colors',
                            isHighlighted
                              ? 'bg-brand-600 text-white shadow-xs'
                              : 'bg-slate-100 text-slate-600 group-hover:bg-brand-50 group-hover:text-brand-600'
                          )}
                        >
                          <Icon className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <span className="text-xs font-bold text-slate-900 block group-hover:text-brand-700 transition-colors">
                            {action.label}
                          </span>
                          <span className="text-[11px] text-slate-500 block truncate">
                            {action.description}
                          </span>
                        </div>
                      </div>

                      <ChevronRight
                        className={clsx(
                          'w-4 h-4 shrink-0 transition-transform group-hover:translate-x-0.5',
                          isSubmenuTrigger
                            ? 'text-brand-600 font-bold'
                            : 'text-slate-300 group-hover:text-brand-600'
                        )}
                      />
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {/* COMPLIANCE SUBMENU VIEW */}
          {activeView === 'COMPLIANCE' && (
            <div className="py-1">
              <div className="px-4 pt-1.5 pb-1">
                <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400 font-mono">
                  SELECT COMPLIANCE WORKFLOW
                </span>
              </div>

              <div className="divide-y divide-slate-100/60">
                {authorizedComplianceActions.map((subAction, index) => {
                  const Icon = subAction.icon;
                  const isHighlighted = highlightedIndex === index;

                  return (
                    <button
                      key={subAction.id}
                      type="button"
                      role="menuitem"
                      tabIndex={0}
                      onClick={() => executeAction(subAction)}
                      onMouseEnter={() => setHighlightedIndex(index)}
                      className={clsx(
                        'w-full text-left px-4 py-2.5 flex items-center justify-between gap-3 transition-colors cursor-pointer group',
                        isHighlighted ? 'bg-slate-50 text-slate-900' : 'text-slate-700 hover:bg-slate-50'
                      )}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div
                          className={clsx(
                            'w-8 h-8 rounded-xl flex items-center justify-center shrink-0 transition-colors',
                            isHighlighted
                              ? 'bg-brand-600 text-white shadow-xs'
                              : 'bg-slate-100 text-slate-600 group-hover:bg-brand-50 group-hover:text-brand-600'
                          )}
                        >
                          <Icon className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <span className="text-xs font-bold text-slate-900 block group-hover:text-brand-700 transition-colors">
                            {subAction.label}
                          </span>
                          <span className="text-[11px] text-slate-500 block truncate">
                            {subAction.description}
                          </span>
                        </div>
                      </div>

                      <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-brand-600 shrink-0 transition-transform group-hover:translate-x-0.5" />
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
