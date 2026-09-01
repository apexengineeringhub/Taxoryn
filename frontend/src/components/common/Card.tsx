import React from 'react';
import clsx from 'clsx';

interface CardProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  bodyClassName?: string;
  noPadding?: boolean;
}

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  action,
  children,
  className,
  bodyClassName,
  noPadding = false,
}) => {
  return (
    <div className={clsx('bg-white border border-slate-200/80 rounded-xl shadow-card', className)}>
      {(title || action) && (
        <div className="px-4 py-3 sm:px-6 sm:py-4 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3 sm:gap-4">
          <div className="min-w-0">
            {title && <h3 className="text-base font-semibold text-slate-900 break-words">{title}</h3>}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5 break-words">{subtitle}</p>}
          </div>
          {action && <div className="flex items-center gap-2 flex-wrap shrink-0">{action}</div>}
        </div>
      )}
      <div className={clsx(!noPadding && 'p-4 sm:p-6', bodyClassName)}>{children}</div>
    </div>
  );
};
