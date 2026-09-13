import React from 'react';
import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { MetricType, MetricNavigationContext, buildMetricUrl, getMetricNavigationConfig } from '../../config/metricRoutes';

export interface ActionableMetricProps {
  metric?: MetricType;
  to?: string;
  context?: MetricNavigationContext;
  value?: React.ReactNode | number | string;
  children?: React.ReactNode;
  label?: string;
  sublabel?: string;
  className?: string;
  variant?: 'display' | 'inline' | 'kpi' | 'pill' | 'table-cell' | 'custom';
  color?: 'default' | 'brand' | 'emerald' | 'amber' | 'rose' | 'purple' | 'indigo' | 'blue';
  ariaLabel?: string;
  disabled?: boolean;
  onClick?: (e: React.MouseEvent<HTMLElement>) => void;
  title?: string;
}

export const ActionableMetric: React.FC<ActionableMetricProps> = ({
  metric,
  to,
  context,
  value,
  children,
  label,
  sublabel,
  className,
  variant = 'inline',
  color = 'default',
  ariaLabel,
  disabled = false,
  onClick,
  title,
}) => {
  const destinationUrl = React.useMemo(() => {
    if (disabled) return null;
    if (to) return to;
    if (metric) {
      try {
        return buildMetricUrl(metric, context);
      } catch (err) {
        console.warn(`[ActionableMetric] Failed to build URL for metric ${metric}`, err);
        return null;
      }
    }
    return null;
  }, [metric, to, context, disabled]);

  const resolvedLabel = React.useMemo(() => {
    if (label) return label;
    if (metric) {
      try {
        return getMetricNavigationConfig(metric, context).label;
      } catch {
        return undefined;
      }
    }
    return undefined;
  }, [label, metric, context]);

  const accessibilityLabel = ariaLabel || (resolvedLabel ? (value !== undefined ? `View ${value} ${resolvedLabel}` : `View ${resolvedLabel}`) : (value !== undefined ? `View details for count ${value}` : 'View details'));

  const colorStyles = {
    default: 'text-slate-900 hover:text-slate-700',
    brand: 'text-brand-600 hover:text-brand-700',
    emerald: 'text-emerald-600 hover:text-emerald-700',
    amber: 'text-amber-700 hover:text-amber-800',
    rose: 'text-rose-600 hover:text-rose-700 font-bold',
    purple: 'text-purple-600 hover:text-purple-700',
    indigo: 'text-indigo-600 hover:text-indigo-700',
    blue: 'text-blue-600 hover:text-blue-700',
  };

  const pillColorStyles = {
    default: 'bg-slate-100 text-slate-800 border-slate-200 hover:bg-slate-200',
    brand: 'bg-brand-50 text-brand-700 border-brand-200 hover:bg-brand-100',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100',
    amber: 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100',
    rose: 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100 font-semibold',
    purple: 'bg-purple-50 text-purple-700 border-purple-200 hover:bg-purple-100',
    indigo: 'bg-indigo-50 text-indigo-700 border-indigo-200 hover:bg-indigo-100',
    blue: 'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100',
  };

  const content = children ? (
    <>{children}</>
  ) : (
    <>
      {variant === 'inline' && (
        <span className="inline-flex items-baseline gap-1">
          {label && <span className="opacity-90">{label}:</span>}
          <span className="font-semibold">{value}</span>
        </span>
      )}
      {variant === 'display' && (
        <span className="text-3xl font-black tracking-tight">{value}</span>
      )}
      {variant === 'kpi' && (
        <span className="text-2xl font-black">{value}</span>
      )}
      {variant === 'pill' && (
        <span className="px-2 py-0.5 rounded-full text-xs font-semibold border inline-flex items-center">
          {value}
        </span>
      )}
      {variant === 'table-cell' && (
        <span className="font-bold">{value}</span>
      )}
      {variant === 'custom' && (
        <span>{value}</span>
      )}
      {sublabel && <span className="text-[10px] block opacity-75">{sublabel}</span>}
    </>
  );

  const baseInteractiveClasses = 'transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-1 rounded cursor-pointer hover:underline';

  if (!destinationUrl || disabled) {
    return (
      <span
        title={title || resolvedLabel}
        className={clsx(
          variant === 'pill' ? pillColorStyles[color] : colorStyles[color],
          className
        )}
      >
        {content}
      </span>
    );
  }

  return (
    <Link
      to={destinationUrl}
      aria-label={accessibilityLabel}
      title={title || `Click to view ${resolvedLabel || 'details'}`}
      onClick={onClick}
      className={clsx(
        baseInteractiveClasses,
        variant === 'pill' ? pillColorStyles[color] : colorStyles[color],
        className
      )}
    >
      {content}
    </Link>
  );
};
