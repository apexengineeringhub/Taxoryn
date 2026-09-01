import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { resolveRoleWorkspace } from '../../config/roleWorkspaceConfig';
import clsx from 'clsx';

interface WorkspacePageHeaderProps {
  sectionBadge: string;
  sectionBadgeStyle?: string;
  customWorkspaceLabel?: string;
  title: string;
  titleIcon?: any;
  titleIconColor?: string;
  description?: string;
  children?: React.ReactNode;
}

export const WorkspacePageHeader: React.FC<WorkspacePageHeaderProps> = ({
  sectionBadge,
  sectionBadgeStyle,
  customWorkspaceLabel,
  title,
  titleIcon: TitleIcon,
  titleIconColor = 'text-purple-600',
  description,
  children,
}) => {
  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const workspace = resolveRoleWorkspace(userRoleCodes);

  const workspaceLabel = customWorkspaceLabel || workspace?.roleTitle || 'Taxoryn Platform';

  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 sm:p-6 rounded-2xl border border-slate-200/90 shadow-card">
      <div className="min-w-0">
        <div className="flex items-center gap-2 mb-1 flex-wrap">
          <span className={clsx(
            'px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider',
            sectionBadgeStyle || workspace?.badgeStyle || 'bg-purple-100 text-purple-800 border border-purple-200'
          )}>
            {sectionBadge}
          </span>
          <span className="text-xs text-slate-400">•</span>
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider break-words">
            {workspaceLabel}
          </span>
        </div>
        <h1 className="text-xl sm:text-2xl md:text-3xl font-black text-slate-900 flex items-center gap-2.5 break-words">
          {TitleIcon && <TitleIcon className={clsx('w-6 h-6 sm:w-7 sm:h-7 md:w-8 md:h-8 shrink-0', titleIconColor)} />}
          {title}
        </h1>
        {description && (
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            {description}
          </p>
        )}
      </div>
      {children && (
        <div className="flex items-center gap-2 sm:gap-3 flex-wrap w-full sm:w-auto [&>*]:min-w-0">
          {children}
        </div>
      )}
    </div>
  );
};
