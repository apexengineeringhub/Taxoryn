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
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-card">
      <div>
        <div className="flex items-center gap-2 mb-1">
          <span className={clsx(
            'px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider',
            sectionBadgeStyle || workspace?.badgeStyle || 'bg-purple-100 text-purple-800 border border-purple-200'
          )}>
            {sectionBadge}
          </span>
          <span className="text-xs text-slate-400">•</span>
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
            {workspaceLabel}
          </span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 flex items-center gap-2.5">
          {TitleIcon && <TitleIcon className={clsx('w-7 h-7 sm:w-8 sm:h-8', titleIconColor)} />}
          {title}
        </h1>
        {description && (
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            {description}
          </p>
        )}
      </div>
      {children && <div className="flex items-center gap-3">{children}</div>}
    </div>
  );
};
