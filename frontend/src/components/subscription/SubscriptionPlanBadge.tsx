import React from 'react';
import clsx from 'clsx';
import { Sparkles } from 'lucide-react';
import { formatPlanDisplayName, formatPlanShortName } from '../../utils/planUtils';

interface SubscriptionPlanBadgeProps {
  plan?: string | null;
  variant?: 'badge' | 'sidebar' | 'pill';
  isLoading?: boolean;
  className?: string;
}

export const SubscriptionPlanBadge: React.FC<SubscriptionPlanBadgeProps> = ({
  plan,
  variant = 'badge',
  isLoading = false,
  className,
}) => {
  if (isLoading) {
    return <span className={clsx('text-slate-400 text-xs italic', className)}>Loading plan...</span>;
  }

  if (!plan) {
    return <span className={clsx('text-slate-400 text-xs italic', className)}>Plan unavailable</span>;
  }

  if (variant === 'sidebar') {
    return (
      <span className={clsx('inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider', className)}>
        <Sparkles className="w-3 h-3" /> {formatPlanShortName(plan)} Plan
      </span>
    );
  }

  const upper = plan.toUpperCase().trim();
  const colorStyles =
    upper === 'ENTERPRISE'
      ? 'bg-purple-50 text-purple-700 border-purple-200'
      : upper === 'BUSINESS'
      ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
      : upper === 'PROFESSIONAL'
      ? 'bg-blue-50 text-blue-700 border-blue-200'
      : 'bg-emerald-50 text-emerald-700 border-emerald-200';

  return (
    <span
      className={clsx(
        'px-2.5 py-0.5 rounded-full text-[10px] font-bold border uppercase tracking-wide inline-flex items-center gap-1',
        colorStyles,
        className
      )}
    >
      {formatPlanDisplayName(plan)}
    </span>
  );
};

export default SubscriptionPlanBadge;
