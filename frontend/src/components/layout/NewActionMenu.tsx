import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Plus,
  UserPlus,
  CheckSquare,
  FileText,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useBranding } from '../../context/BrandingContext';
import { hasPermission } from '../../utils/permissionUtils';
import clsx from 'clsx';

export interface ActionDefinition {
  id: string;
  category: 'CLIENT' | 'WORK' | 'DOCUMENTS';
  categoryLabel: string;
  label: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  requiredPermissions: string[];
  allowedRoles: string[];
  targetPath: string;
  supportsClientContext?: boolean;
}

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
];

export const NewActionMenu: React.FC = () => {
  const { user } = useAuth();
  const { currentTheme } = useBranding();
  const navigate = useNavigate();
  const location = useLocation();

  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState<number>(-1);
  const menuRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Extract current client context if present in current URL
  const currentClientId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('clientId') || '';
  }, [location.search]);

  // Filter actions based on active user's permissions and roles
  const authorizedActions = useMemo(() => {
    return ACTION_DEFINITIONS.filter((action) =>
      hasPermission(user, action.requiredPermissions, action.allowedRoles)
    );
  }, [user]);

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

  // Keyboard navigation & accessibility
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
        e.preventDefault();
        setIsOpen(true);
        setHighlightedIndex(0);
      }
      return;
    }

    switch (e.key) {
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        buttonRef.current?.focus();
        break;
      case 'ArrowDown':
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev < authorizedActions.length - 1 ? prev + 1 : 0
        );
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev > 0 ? prev - 1 : authorizedActions.length - 1
        );
        break;
      case 'Enter':
      case ' ':
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < authorizedActions.length) {
          executeAction(authorizedActions[highlightedIndex]);
        }
        break;
      case 'Tab':
        setIsOpen(false);
        break;
      default:
        break;
    }
  };

  const executeAction = (action: ActionDefinition) => {
    setIsOpen(false);
    let target = action.targetPath;

    // Propagate active client context if supported and available
    if (action.supportsClientContext && currentClientId) {
      const sep = target.includes('?') ? '&' : '?';
      target = `${target}${sep}clientId=${encodeURIComponent(currentClientId)}`;
    }

    navigate(target);
  };

  // If no action is authorized (e.g. client user or unauthorized viewer), hide the launcher
  if (authorizedActions.length === 0) {
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
          setHighlightedIndex(0);
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
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
              What would you like to do?
            </span>
            {currentClientId && (
              <span className="text-[10px] font-bold text-brand-600 bg-brand-50 px-2 py-0.5 rounded-md border border-brand-200/60 truncate max-w-[140px]">
                Client Context Active
              </span>
            )}
          </div>

          <div className="divide-y divide-slate-100/80">
            {authorizedActions.map((action, index) => {
              const Icon = action.icon;
              const isHighlighted = highlightedIndex === index;

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

                    <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-brand-600 shrink-0 transition-transform group-hover:translate-x-0.5" />
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
