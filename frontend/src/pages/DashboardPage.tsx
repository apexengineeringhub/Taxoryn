import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Receipt,
  FileSpreadsheet,
  Building2,
  TrendingUp,
  ArrowUpRight,
  Sparkles,
  Percent,
  ShieldCheck,
  Award,
  Bell,
  FileText,
  Calendar,
  PlusCircle,
  Briefcase,
  Layers,
  RefreshCw,
  Activity,
  ArrowRight,
  AlertCircle,
  FileCheck2,
  UserCheck,
  Check,
  Scale,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { StatusBadge } from '../components/common/StatusBadge';
import { ActionableMetric } from '../components/common/ActionableMetric';
import { dashboardApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { useModuleEntitlement } from '../context/ModuleEntitlementContext';
import { PracticeDashboardOverview, DashboardFilterParams } from '../types';
import { ClientPortalManagementPage } from './ClientPortalManagementPage';
import { PlatformOverviewPage } from './PlatformOverviewPage';
import { SupportOverviewPage } from './SupportOverviewPage';
import clsx from 'clsx';

type PeriodType = 'TODAY' | 'THIS_WEEK' | 'THIS_MONTH' | 'LAST_MONTH' | 'THIS_QUARTER' | 'ALL_TIME';

export const DashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<PracticeDashboardOverview | null>(null);
  const [selectedPeriod, setSelectedPeriod] = useState<PeriodType>('THIS_MONTH');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { user, organization } = useAuth();
  const { isModuleAvailable } = useModuleEntitlement();

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isTaxorynSuperAdmin = userRoleCodes.includes('TAXORYN_SUPERADMIN') || userRoleCodes.includes('SUPER_ADMIN');
  const isSupportAdmin = userRoleCodes.includes('TAXORYN_SUPPORT_ADMIN');
  const isPlatformUser = isTaxorynSuperAdmin || isSupportAdmin || userRoleCodes.some((r: string) => r.startsWith('TAXORYN_'));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'PRACTICE_CLIENT', 'CLIENT_ADMIN', 'MARKETPLACE_CUSTOMER'].includes(r));
  const isFirmAdmin = !isPlatformUser && userRoleCodes.some((r: string) => ['PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER'].includes(r));
  const isStaff = !isPlatformUser && !isFirmAdmin && userRoleCodes.some((r: string) => ['PRACTICE_EMPLOYEE', 'ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE', 'ACCOUNTANT'].includes(r));
  const isSolo = organization?.organizationType === 'SOLO' || organization?.organizationType === 'SOLO_PRACTITIONER';
  const userPermissions = user?.permissions || [];
  const hasBillingAccess = isFirmAdmin || userPermissions.includes('BILLING_VIEW') || userPermissions.includes('BILLING_READ');

  const loadDashboard = useCallback(async (period: PeriodType) => {
    try {
      setIsLoading(true);
      setError(null);
      const params: DashboardFilterParams = {
        period: period,
      };
      const data = await dashboardApi.getOverview(params);
      setDashboard(data);
    } catch (err: any) {
      console.error('Failed to fetch dashboard data', err);
      setError(err?.response?.data?.message || 'Failed to load practice dashboard metrics. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isClientUser && !isPlatformUser) {
      loadDashboard(selectedPeriod);
    }
  }, [isClientUser, isPlatformUser, selectedPeriod, loadDashboard]);

  if (isSupportAdmin) {
    return <SupportOverviewPage />;
  }

  if (isPlatformUser) {
    return <PlatformOverviewPage />;
  }

  if (isClientUser) {
    return <ClientPortalManagementPage />;
  }

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  };

  const firstName = user?.firstName || 'Practitioner';

  const periodOptions: { label: string; value: PeriodType }[] = [
    { label: 'Today', value: 'TODAY' },
    { label: 'This Week', value: 'THIS_WEEK' },
    { label: 'This Month', value: 'THIS_MONTH' },
    { label: 'Last Month', value: 'LAST_MONTH' },
    { label: 'This Quarter', value: 'THIS_QUARTER' },
    { label: 'All Time', value: 'ALL_TIME' },
  ];

  // Attention alert indicators
  const overdueTasksCount = dashboard?.overdueTasks ?? 0;
  const pendingDocsCount = dashboard?.pendingDocumentRequests ?? 0;
  const overdueComplianceCount = dashboard?.overdueComplianceObligations ?? 0;
  const expiringDscCount = dashboard?.dsc?.expiringSoon ?? 0;
  const overdueRemindersCount = dashboard?.reminders?.overdue ?? 0;
  const openNoticesCount = dashboard?.openTaxNotices ?? 0;
  const unverifiedUdinCount = dashboard?.udin?.unverifiedCount ?? 0;

  const totalAttentionCount =
    overdueTasksCount +
    pendingDocsCount +
    overdueComplianceCount +
    expiringDscCount +
    overdueRemindersCount +
    openNoticesCount;

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* 1. Practitioner Header & Context */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">
              {getGreeting()}, {firstName} 👋
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/60">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Practice Sync
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {organization?.name ? `${organization.name} • ` : ''}
            Here's what needs your attention today across clients, compliance, and deliverables.
          </p>
        </div>

        {/* Date Filter & Refresh */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex p-1 bg-slate-100 rounded-xl border border-slate-200/80 text-xs font-semibold">
            {periodOptions.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setSelectedPeriod(opt.value)}
                className={clsx(
                  'px-3 py-1.5 rounded-lg transition-all',
                  selectedPeriod === opt.value
                    ? 'bg-white text-slate-900 shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                )}
              >
                {opt.label}
              </button>
            ))}
          </div>

          <button
            onClick={() => loadDashboard(selectedPeriod)}
            disabled={isLoading}
            className="p-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 hover:text-slate-900 transition-colors"
            title="Refresh Dashboard"
            aria-label="Refresh Dashboard"
          >
            <RefreshCw className={clsx('w-4 h-4', isLoading && 'animate-spin text-blue-600')} />
          </button>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center justify-between text-rose-800 text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600" />
            <span>{error}</span>
          </div>
          <button
            onClick={() => loadDashboard(selectedPeriod)}
            className="px-3 py-1 bg-rose-600 text-white rounded-lg font-semibold hover:bg-rose-700 transition-colors"
          >
            Retry
          </button>
        </div>
      )}

      {/* 2. Needs Your Attention (Actionable Alerts Bar) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-amber-500 animate-pulse" />
            <h2 className="text-xs font-black uppercase tracking-wider text-slate-600">
              Needs Your Attention
            </h2>
            {totalAttentionCount > 0 && (
              <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 font-extrabold text-[11px] border border-rose-200">
                {totalAttentionCount} action items
              </span>
            )}
          </div>
          <span className="text-[11px] text-slate-400 font-medium">Prioritized for action</span>
        </div>

        {totalAttentionCount === 0 && !isLoading ? (
          <div className="p-4 bg-emerald-50/70 border border-emerald-200/70 rounded-2xl flex items-center gap-3 text-xs text-emerald-800">
            <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
              <Check className="w-4 h-4" />
            </div>
            <div>
              <p className="font-bold text-slate-900">All caught up!</p>
              <p className="text-slate-600 text-[11px]">No overdue tasks, expiring DSCs, or urgent compliance items require immediate attention.</p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3">
            {/* Alert: Overdue Tasks */}
            {isModuleAvailable('TASKS') && (
              <Link
                to="/tasks"
                className={clsx(
                  'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                  overdueTasksCount > 0
                    ? 'bg-rose-50/80 border-rose-200/90 hover:border-rose-400 hover:shadow-xs'
                    : 'bg-white border-slate-200/80 hover:border-slate-300'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Overdue Tasks</span>
                  <AlertTriangle className={clsx('w-4 h-4', overdueTasksCount > 0 ? 'text-rose-600' : 'text-slate-400')} />
                </div>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className={clsx('text-xl font-black', overdueTasksCount > 0 ? 'text-rose-700' : 'text-slate-700')}>
                    {isLoading ? '...' : overdueTasksCount}
                  </span>
                  <span className="text-[10px] font-semibold text-rose-600 group-hover:underline flex items-center gap-0.5">
                    Resolve <ArrowRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </Link>
            )}

            {/* Alert: Pending Documents */}
            {isModuleAvailable('DOCUMENTS') && (
              <Link
                to="/documents"
                className={clsx(
                  'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                  pendingDocsCount > 0
                    ? 'bg-amber-50/80 border-amber-200/90 hover:border-amber-400 hover:shadow-xs'
                    : 'bg-white border-slate-200/80 hover:border-slate-300'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Pending Docs</span>
                  <FileText className={clsx('w-4 h-4', pendingDocsCount > 0 ? 'text-amber-600' : 'text-slate-400')} />
                </div>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className={clsx('text-xl font-black', pendingDocsCount > 0 ? 'text-amber-700' : 'text-slate-700')}>
                    {isLoading ? '...' : pendingDocsCount}
                  </span>
                  <span className="text-[10px] font-semibold text-amber-600 group-hover:underline flex items-center gap-0.5">
                    Review <ArrowRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </Link>
            )}

            {/* Alert: Overdue Compliance */}
            <Link
              to="/compliance/workbench"
              className={clsx(
                'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                overdueComplianceCount > 0
                  ? 'bg-rose-50/80 border-rose-200/90 hover:border-rose-400 hover:shadow-xs'
                  : 'bg-white border-slate-200/80 hover:border-slate-300'
              )}
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Compliance Due</span>
                <Clock className={clsx('w-4 h-4', overdueComplianceCount > 0 ? 'text-rose-600' : 'text-slate-400')} />
              </div>
              <div className="mt-2 flex items-baseline justify-between">
                <span className={clsx('text-xl font-black', overdueComplianceCount > 0 ? 'text-rose-700' : 'text-slate-700')}>
                  {isLoading ? '...' : overdueComplianceCount}
                </span>
                <span className="text-[10px] font-semibold text-rose-600 group-hover:underline flex items-center gap-0.5">
                  Workbench <ArrowRight className="w-2.5 h-2.5" />
                </span>
              </div>
            </Link>

            {/* Alert: Expiring DSCs */}
            {isModuleAvailable('CLIENTS') && (
              <Link
                to="/dsc-register"
                className={clsx(
                  'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                  expiringDscCount > 0
                    ? 'bg-amber-50/80 border-amber-200/90 hover:border-amber-400 hover:shadow-xs'
                    : 'bg-white border-slate-200/80 hover:border-slate-300'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Expiring DSCs</span>
                  <ShieldCheck className={clsx('w-4 h-4', expiringDscCount > 0 ? 'text-amber-600' : 'text-slate-400')} />
                </div>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className={clsx('text-xl font-black', expiringDscCount > 0 ? 'text-amber-700' : 'text-slate-700')}>
                    {isLoading ? '...' : expiringDscCount}
                  </span>
                  <span className="text-[10px] font-semibold text-amber-600 group-hover:underline flex items-center gap-0.5">
                    Renew <ArrowRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </Link>
            )}

            {/* Alert: Tax Notices */}
            {isModuleAvailable('TAX_NOTICES') && (
              <Link
                to="/tax-notices"
                className={clsx(
                  'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                  openNoticesCount > 0
                    ? 'bg-purple-50/80 border-purple-200/90 hover:border-purple-400 hover:shadow-xs'
                    : 'bg-white border-slate-200/80 hover:border-slate-300'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Open Notices</span>
                  <Scale className={clsx('w-4 h-4', openNoticesCount > 0 ? 'text-purple-600' : 'text-slate-400')} />
                </div>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className={clsx('text-xl font-black', openNoticesCount > 0 ? 'text-purple-700' : 'text-slate-700')}>
                    {isLoading ? '...' : openNoticesCount}
                  </span>
                  <span className="text-[10px] font-semibold text-purple-600 group-hover:underline flex items-center gap-0.5">
                    Hearings <ArrowRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </Link>
            )}

            {/* Alert: Overdue Reminders */}
            {isModuleAvailable('REMINDERS') && (
              <Link
                to="/reminders"
                className={clsx(
                  'p-3.5 rounded-2xl border transition-all flex flex-col justify-between group',
                  overdueRemindersCount > 0
                    ? 'bg-rose-50/80 border-rose-200/90 hover:border-rose-400 hover:shadow-xs'
                    : 'bg-white border-slate-200/80 hover:border-slate-300'
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Overdue Reminders</span>
                  <Bell className={clsx('w-4 h-4', overdueRemindersCount > 0 ? 'text-rose-600' : 'text-slate-400')} />
                </div>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className={clsx('text-xl font-black', overdueRemindersCount > 0 ? 'text-rose-700' : 'text-slate-700')}>
                    {isLoading ? '...' : overdueRemindersCount}
                  </span>
                  <span className="text-[10px] font-semibold text-rose-600 group-hover:underline flex items-center gap-0.5">
                    Follow Up <ArrowRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </Link>
            )}
          </div>
        )}
      </div>

      {/* 3. Today's Work & Fast Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Fast Action Launcher */}
        <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 rounded-2xl p-5 text-white shadow-md lg:col-span-1 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              <span className="text-xs font-bold uppercase tracking-wider text-slate-300">Fast Actions</span>
            </div>
            <p className="text-xs text-slate-300">
              Launch routine workflows, client onboarding, or statutory registrations in one click.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2 mt-4 text-xs">
            {isModuleAvailable('CLIENTS') && (
              <Link
                to="/clients?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <PlusCircle className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                <span className="truncate">Add Client</span>
              </Link>
            )}
            {isModuleAvailable('TASKS') && (
              <Link
                to="/tasks?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span className="truncate">New Task</span>
              </Link>
            )}
            {isModuleAvailable('CLIENTS') && (
              <Link
                to="/dsc-register?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <ShieldCheck className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                <span className="truncate">Record DSC</span>
              </Link>
            )}
            {isModuleAvailable('CLIENTS') && (
              <Link
                to="/udin-register?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <Award className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                <span className="truncate">Record UDIN</span>
              </Link>
            )}
            {hasBillingAccess && isModuleAvailable('BILLING') && (
              <Link
                to="/billing?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <Receipt className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span className="truncate">New Invoice</span>
              </Link>
            )}
            {isModuleAvailable('REMINDERS') && (
              <Link
                to="/reminders?action=new"
                className="p-2.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-xl font-medium text-white transition-all flex items-center gap-2"
              >
                <Bell className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                <span className="truncate">Set Reminder</span>
              </Link>
            )}
          </div>
        </div>

        {/* Today's Work Summary Panel */}
        <Card
          title="Today's Work & Obligations"
          subtitle="Real-time delivery progress across tasks and statutory returns"
          className="lg:col-span-2"
        >
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-center">
            <Link
              to="/tasks"
              className="p-3 bg-slate-50 border border-slate-100 rounded-xl hover:border-slate-300 transition-all block"
            >
              <p className="text-xs text-slate-500 font-semibold">Open Deliverables</p>
              <p className="text-2xl font-black text-slate-900 mt-1">
                {isLoading ? '...' : dashboard?.pendingTasks ?? 0}
              </p>
              <p className="text-[11px] text-emerald-600 font-medium mt-1">
                {dashboard?.completedTasks ?? 0} finished in period
              </p>
            </Link>

            <Link
              to="/compliance/workbench"
              className="p-3 bg-slate-50 border border-slate-100 rounded-xl hover:border-slate-300 transition-all block"
            >
              <p className="text-xs text-slate-500 font-semibold">Compliance Due</p>
              <p className="text-2xl font-black text-slate-900 mt-1">
                {isLoading ? '...' : dashboard?.openComplianceObligations ?? 0}
              </p>
              <p className="text-[11px] text-indigo-600 font-medium mt-1">
                GST / ITR / TDS Filings
              </p>
            </Link>

            <Link
              to="/calendar"
              className="p-3 bg-slate-50 border border-slate-100 rounded-xl hover:border-slate-300 transition-all block"
            >
              <p className="text-xs text-slate-500 font-semibold">Tax Calendar</p>
              <p className="text-2xl font-black text-slate-900 mt-1">
                {isLoading ? '...' : (dashboard?.compliance?.pending ?? 0)}
              </p>
              <p className="text-[11px] text-blue-600 font-medium mt-1">
                View Schedule →
              </p>
            </Link>
          </div>

          <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-slate-600">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              <span>Deliverables update automatically as team members complete items.</span>
            </div>
            <Link to="/tasks" className="text-blue-600 font-bold hover:underline flex items-center gap-1">
              View All Tasks <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </Card>
      </div>

      {/* 4. Practice Snapshot (Compact Horizontal Metric Strip) */}
      <div className="space-y-3">
        <h2 className="text-xs font-black uppercase tracking-wider text-slate-500">Practice Snapshot</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
          {/* Active Clients */}
          {isModuleAvailable('CLIENTS') && (
            <Link
              to="/clients"
              className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-blue-400 hover:shadow-md transition-all group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
                  {isStaff ? 'Assigned Clients' : 'Active Clients'}
                </span>
                <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
                  <Users className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <p className="text-2xl font-black text-slate-900 tracking-tight">
                  {isLoading ? '...' : dashboard?.activeClients ?? 0}
                </p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  of {dashboard?.totalClients ?? 0} total registered
                </p>
              </div>
            </Link>
          )}

          {/* Active Engagements */}
          {isModuleAvailable('CLIENTS') && (
            <Link
              to="/engagements"
              className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-indigo-400 hover:shadow-md transition-all group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-indigo-600 transition-colors">
                  Engagements
                </span>
                <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center group-hover:bg-indigo-600 group-hover:text-white transition-all">
                  <Briefcase className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <p className="text-2xl font-black text-slate-900 tracking-tight">
                  {isLoading ? '...' : dashboard?.activeEngagements ?? 0}
                </p>
                <p className="text-[11px] text-indigo-600 font-medium mt-0.5">
                  Active Client Retainers
                </p>
              </div>
            </Link>
          )}

          {/* Open Deliverables */}
          {isModuleAvailable('TASKS') && (
            <Link
              to="/tasks"
              className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-emerald-400 hover:shadow-md transition-all group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-emerald-600 transition-colors">
                  Open Tasks
                </span>
                <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-all">
                  <CheckCircle2 className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <p className="text-2xl font-black text-slate-900 tracking-tight">
                  {isLoading ? '...' : dashboard?.pendingTasks ?? 0}
                </p>
                <p className="text-[11px] text-emerald-600 font-medium mt-0.5">
                  {dashboard?.completedTasks ?? 0} completed in period
                </p>
              </div>
            </Link>
          )}

          {/* Pending Documents */}
          {isModuleAvailable('DOCUMENTS') && (
            <Link
              to="/documents"
              className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-amber-400 hover:shadow-md transition-all group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
                  Document Vault
                </span>
                <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition-all">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <p className="text-2xl font-black text-slate-900 tracking-tight">
                  {isLoading ? '...' : dashboard?.pendingDocumentRequests ?? 0}
                </p>
                <p className="text-[11px] text-amber-600 font-medium mt-0.5">
                  Pending Client Requests
                </p>
              </div>
            </Link>
          )}

          {/* Outstanding Receivables */}
          {hasBillingAccess && isModuleAvailable('BILLING') && (
            <Link
              to="/billing"
              className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-purple-400 hover:shadow-md transition-all group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-600 transition-colors">
                  Receivables
                </span>
                <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all">
                  <Receipt className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <p className="text-xl font-black text-slate-900 tracking-tight truncate">
                  {isLoading ? '...' : formatCurrency(dashboard?.outstandingBillingAmount)}
                </p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Collected: {formatCurrency(dashboard?.periodCollectedAmount)}
                </p>
              </div>
            </Link>
          )}
        </div>
      </div>

      {/* 5. Compliance Health & Registers */}
      <div className="space-y-3">
        <h2 className="text-xs font-black uppercase tracking-wider text-slate-500">Compliance Health & Registers</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* DSC Register Widget */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">DSC Register</h3>
              </div>
              <Link to="/dsc-register" className="text-xs text-blue-600 font-semibold hover:underline">Manage</Link>
            </div>
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              <div className="p-2 rounded-lg bg-emerald-50/80 border border-emerald-100">
                <p className="text-[10px] text-emerald-700 font-medium">Active</p>
                <p className="font-black text-emerald-900 text-sm mt-0.5">{dashboard?.dsc?.active ?? 0}</p>
              </div>
              <div className="p-2 rounded-lg bg-amber-50/80 border border-amber-100">
                <p className="text-[10px] text-amber-700 font-medium">Expiring</p>
                <p className="font-black text-amber-900 text-sm mt-0.5">{dashboard?.dsc?.expiringSoon ?? 0}</p>
              </div>
              <div className="p-2 rounded-lg bg-rose-50/80 border border-rose-100">
                <p className="text-[10px] text-rose-700 font-medium">Expired</p>
                <p className="font-black text-rose-900 text-sm mt-0.5">{dashboard?.dsc?.expired ?? 0}</p>
              </div>
            </div>
          </div>

          {/* UDIN Register Widget */}
          <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                  <Award className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">UDIN Register</h3>
              </div>
              <Link to="/udin-register" className="text-xs text-blue-600 font-semibold hover:underline">Manage</Link>
            </div>
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              <div className="p-2 rounded-lg bg-slate-50 border border-slate-100">
                <p className="text-[10px] text-slate-600 font-medium">Total</p>
                <p className="font-black text-slate-900 text-sm mt-0.5">{dashboard?.udin?.totalCount ?? 0}</p>
              </div>
              <div className="p-2 rounded-lg bg-emerald-50/80 border border-emerald-100">
                <p className="text-[10px] text-emerald-700 font-medium">Verified</p>
                <p className="font-black text-emerald-900 text-sm mt-0.5">{dashboard?.udin?.verifiedCount ?? 0}</p>
              </div>
              <div className="p-2 rounded-lg bg-amber-50/80 border border-amber-100">
                <p className="text-[10px] text-amber-700 font-medium">Pending</p>
                <p className="font-black text-amber-900 text-sm mt-0.5">{dashboard?.udin?.unverifiedCount ?? 0}</p>
              </div>
            </div>
          </div>

          {/* Tax Notices Widget */}
          {isModuleAvailable('TAX_NOTICES') && (
            <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
                    <Scale className="w-4 h-4" />
                  </div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Tax Notices</h3>
                </div>
                <Link to="/tax-notices" className="text-xs text-blue-600 font-semibold hover:underline">View</Link>
              </div>
              <div className="flex items-center justify-between p-2 rounded-lg bg-rose-50/50 border border-rose-100 text-xs">
                <span className="text-slate-600 font-medium">Active Notices:</span>
                <span className="font-bold text-rose-700">{dashboard?.openTaxNotices ?? 0} Open</span>
              </div>
            </div>
          )}

          {/* Document Vault Widget */}
          {isModuleAvailable('DOCUMENTS') && (
            <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                    <FileCheck2 className="w-4 h-4" />
                  </div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Document Vault</h3>
                </div>
                <Link to="/documents" className="text-xs text-blue-600 font-semibold hover:underline">Vault</Link>
              </div>
              <div className="flex items-center justify-between p-2 rounded-lg bg-blue-50/50 border border-blue-100 text-xs">
                <span className="text-slate-600 font-medium">Pending Requests:</span>
                <span className="font-bold text-blue-700">{dashboard?.pendingDocumentRequests ?? 0} Pending</span>
              </div>
            </div>
          )}
        </div>

        {/* Compliance by Service Domain */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs pt-1">
          {isModuleAvailable('GST') && (
            <Link to="/gst" className="p-3.5 rounded-2xl border border-slate-200/90 bg-white hover:bg-emerald-50/40 hover:border-emerald-200 transition-all flex items-center justify-between shadow-card">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                  <Building2 className="w-4 h-4" />
                </div>
                <div>
                  <p className="font-bold text-slate-900">GST Compliance</p>
                  <p className="text-[11px] text-slate-500">GSTR-1, 3B, 9 Filings</p>
                </div>
              </div>
              <span className="font-black text-slate-900 text-sm">{dashboard?.compliance?.gst?.total ?? 0} Total</span>
            </Link>
          )}
          {isModuleAvailable('ITR') && (
            <Link to="/itr" className="p-3.5 rounded-2xl border border-slate-200/90 bg-white hover:bg-purple-50/40 hover:border-purple-200 transition-all flex items-center justify-between shadow-card">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                  <FileSpreadsheet className="w-4 h-4" />
                </div>
                <div>
                  <p className="font-bold text-slate-900">ITR Compliance</p>
                  <p className="text-[11px] text-slate-500">Income Tax Computations</p>
                </div>
              </div>
              <span className="font-black text-slate-900 text-sm">{dashboard?.compliance?.itr?.total ?? 0} Total</span>
            </Link>
          )}
          {isModuleAvailable('TDS') && (
            <Link to="/tds" className="p-3.5 rounded-2xl border border-slate-200/90 bg-white hover:bg-indigo-50/40 hover:border-indigo-200 transition-all flex items-center justify-between shadow-card">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                  <Percent className="w-4 h-4" />
                </div>
                <div>
                  <p className="font-bold text-slate-900">TDS / TCS</p>
                  <p className="text-[11px] text-slate-500">Quarterly Returns & Challans</p>
                </div>
              </div>
              <span className="font-black text-slate-900 text-sm">{dashboard?.compliance?.tds?.total ?? 0} Total</span>
            </Link>
          )}
        </div>
      </div>

      {/* 6. Team Workload (Visible for Multi-User Practices) */}
      {!isSolo && isModuleAvailable('TASKS') && (
        <Card
          title={isStaff ? 'My Workload & Assignments' : 'Team Workload'}
          subtitle={isStaff ? 'Breakdown of your current deliverable queue' : 'Assigned vs pending tasks per practice team member'}
          noPadding
        >
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/80 font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="px-5 py-3">Team Member</th>
                  <th className="px-4 py-3">Department</th>
                  <th className="px-4 py-3 text-center">Assigned</th>
                  <th className="px-4 py-3 text-center">Pending</th>
                  <th className="px-4 py-3 text-center">Overdue</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-8 text-slate-400">Loading workload metrics...</td>
                  </tr>
                ) : !dashboard?.employeeWorkload?.length ? (
                  <tr>
                    <td colSpan={5} className="text-center py-8 text-slate-400">No active team workload recorded</td>
                  </tr>
                ) : (
                  dashboard.employeeWorkload.map((emp) => (
                    <tr key={emp.employeeId} className="table-row-hover">
                      <td className="px-5 py-3 font-semibold text-slate-900">
                        {emp.employeeName}
                        <span className="block text-[10px] font-normal text-slate-400">{emp.employeeCode} • {emp.designation}</span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{emp.department || 'General Tax'}</td>
                      <td className="px-4 py-3 text-center font-bold text-slate-800">{emp.assignedTasks}</td>
                      <td className="px-4 py-3 text-center">
                        <span className={clsx('px-2 py-0.5 rounded-full font-bold text-[11px]', emp.pendingTasks > 0 ? 'bg-amber-50 text-amber-700' : 'text-slate-400')}>
                          {emp.pendingTasks}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={clsx('px-2 py-0.5 rounded-full font-bold text-[11px]', emp.overdueTasks > 0 ? 'bg-rose-50 text-rose-700' : 'text-slate-400')}>
                          {emp.overdueTasks}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile Card List */}
          <div className="md:hidden">
            {isLoading ? (
              <div className="text-center py-8 text-slate-400 text-xs">Loading workload metrics...</div>
            ) : !dashboard?.employeeWorkload?.length ? (
              <div className="text-center py-8 text-slate-400 text-xs">No active team workload recorded</div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {dashboard.employeeWorkload.map((emp) => (
                  <li key={emp.employeeId} className="px-4 py-3 text-xs space-y-2">
                    <div>
                      <p className="font-semibold text-slate-900">{emp.employeeName}</p>
                      <p className="text-[10px] text-slate-400">{emp.employeeCode} • {emp.designation} • {emp.department || 'General Tax'}</p>
                    </div>
                    <div className="grid grid-cols-3 gap-2 text-center">
                      <div className="p-2 rounded-lg bg-slate-50 border border-slate-100">
                        <p className="text-[10px] text-slate-500">Assigned</p>
                        <p className="font-bold text-slate-800">{emp.assignedTasks}</p>
                      </div>
                      <div className="p-2 rounded-lg bg-amber-50/70 border border-amber-200/50">
                        <p className="text-[10px] text-amber-700">Pending</p>
                        <p className="font-bold text-amber-800">{emp.pendingTasks}</p>
                      </div>
                      <div className="p-2 rounded-lg bg-rose-50/70 border border-rose-200/50">
                        <p className="text-[10px] text-rose-700">Overdue</p>
                        <p className="font-bold text-rose-800">{emp.overdueTasks}</p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </Card>
      )}
    </div>
  );
};

export default DashboardPage;
