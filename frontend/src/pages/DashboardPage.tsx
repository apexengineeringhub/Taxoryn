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

  const periodOptions: { label: string; value: PeriodType }[] = [
    { label: 'Today', value: 'TODAY' },
    { label: 'This Week', value: 'THIS_WEEK' },
    { label: 'This Month', value: 'THIS_MONTH' },
    { label: 'Last Month', value: 'LAST_MONTH' },
    { label: 'This Quarter', value: 'THIS_QUARTER' },
    { label: 'All Time', value: 'ALL_TIME' },
  ];

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* 1. Header & Operational Control Bar */}
      <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">
              {isStaff ? 'Operations Cockpit' : 'Practice Operational Dashboard'}
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/60">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Sync
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            {organization?.name ? `${organization.name} • ` : ''}
            {isStaff
              ? 'Personal deliverables, active compliance obligations, and task assignments.'
              : 'Holistic real-time oversight of clients, engagements, compliance, DSC/UDIN registers, and billing.'}
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

      {/* Error state */}
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

      {/* 2. Quick Actions Bar */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 rounded-2xl p-4 text-white shadow-md">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-amber-400" />
            <span className="text-xs font-bold uppercase tracking-wider text-slate-300">Quick Actions</span>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs">
            {isModuleAvailable('CLIENTS') && (
              <Link
                to="/clients"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
              >
                <PlusCircle className="w-3.5 h-3.5 text-blue-400" />
                Add Client
              </Link>
            )}
            {isModuleAvailable('TASKS') && (
              <Link
                to="/tasks"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
              >
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                Create Task
              </Link>
            )}
            {isModuleAvailable('CLIENTS') && (
              <>
                <Link
                  to="/dsc"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
                >
                  <ShieldCheck className="w-3.5 h-3.5 text-amber-400" />
                  Record DSC
                </Link>
                <Link
                  to="/udin"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
                >
                  <Award className="w-3.5 h-3.5 text-purple-400" />
                  Record UDIN
                </Link>
              </>
            )}
            {hasBillingAccess && isModuleAvailable('BILLING') && (
              <Link
                to="/billing"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
              >
                <Receipt className="w-3.5 h-3.5 text-emerald-400" />
                Create Invoice
              </Link>
            )}
            {isModuleAvailable('REMINDERS') && (
              <Link
                to="/reminders"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white/10 hover:bg-white/20 border border-white/15 rounded-lg font-medium text-white transition-all backdrop-blur-xs"
              >
                <Bell className="w-3.5 h-3.5 text-rose-400" />
                Set Reminder
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* 3. Top Row: Core Practice KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-6 gap-4">
        {/* KPI 1: Active Clients */}
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

        {/* KPI 2: Active Engagements */}
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

        {/* KPI 3: Open Deliverables / Tasks */}
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

        {/* KPI 4: Overdue Tasks */}
        {isModuleAvailable('TASKS') && (
          <Link
            to="/tasks"
            className={clsx(
              'border rounded-2xl p-4 shadow-card hover:shadow-md transition-all group block',
              (dashboard?.overdueTasks ?? 0) > 0
                ? 'bg-rose-50/60 border-rose-200/90 hover:border-rose-400'
                : 'bg-white border-slate-200/90 hover:border-slate-300'
            )}
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-rose-600 transition-colors">
                Overdue Tasks
              </span>
              <div className="w-8 h-8 rounded-lg bg-rose-100 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-all">
                <AlertTriangle className="w-4 h-4" />
              </div>
            </div>
            <div className="mt-3">
              <p className={clsx('text-2xl font-black tracking-tight', (dashboard?.overdueTasks ?? 0) > 0 ? 'text-rose-700' : 'text-slate-900')}>
                {isLoading ? '...' : dashboard?.overdueTasks ?? 0}
              </p>
              <p className="text-[11px] text-rose-600 font-medium mt-0.5">
                Requires Immediate Action
              </p>
            </div>
          </Link>
        )}

        {/* KPI 5: Pending Documents */}
        {isModuleAvailable('DOCUMENTS') && (
          <Link
            to="/documents"
            className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-amber-400 hover:shadow-md transition-all group block"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
                Pending Docs
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
                Awaiting Client Upload
              </p>
            </div>
          </Link>
        )}

        {/* KPI 6: Outstanding Receivables / Invoiced */}
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

      {/* 4. Middle Section: Compliance, Work & Health Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Compliance Overview */}
        <Card
          title="Compliance & Filing Overview"
          subtitle="Real-time statutory obligation health across active clients"
          className="lg:col-span-2"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 bg-slate-50 border border-slate-100 rounded-xl text-center">
                <p className="text-xs text-slate-500 font-medium">Total Obligations</p>
                <p className="text-xl font-black text-slate-900 mt-1">
                  {dashboard?.openComplianceObligations ?? 0}
                </p>
              </div>
              <div className="p-3 bg-amber-50/70 border border-amber-200/60 rounded-xl text-center">
                <p className="text-xs text-amber-800 font-medium">Pending</p>
                <p className="text-xl font-black text-amber-900 mt-1">
                  {dashboard?.compliance?.pending ?? dashboard?.openComplianceObligations ?? 0}
                </p>
              </div>
              <div className="p-3 bg-rose-50/70 border border-rose-200/60 rounded-xl text-center">
                <p className="text-xs text-rose-800 font-medium">Overdue</p>
                <p className="text-xl font-black text-rose-900 mt-1">
                  {dashboard?.overdueComplianceObligations ?? 0}
                </p>
              </div>
              <div className="p-3 bg-emerald-50/70 border border-emerald-200/60 rounded-xl text-center">
                <p className="text-xs text-emerald-800 font-medium">Completed</p>
                <p className="text-xl font-black text-emerald-900 mt-1">
                  {dashboard?.compliance?.completed ?? 0}
                </p>
              </div>
            </div>

            {/* Breakdown by Discipline */}
            <div className="pt-3 border-t border-slate-100 space-y-3">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider">Compliance By Service Domain</p>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                {isModuleAvailable('GST') && (
                  <Link to="/gst" className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-emerald-50/40 hover:border-emerald-200 transition-all flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Building2 className="w-4 h-4 text-emerald-600" />
                      <span className="font-semibold text-slate-800">GST Filing</span>
                    </div>
                    <span className="font-bold text-slate-900">{dashboard?.compliance?.gst?.total ?? 0} Total</span>
                  </Link>
                )}
                {isModuleAvailable('ITR') && (
                  <Link to="/itr" className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-purple-50/40 hover:border-purple-200 transition-all flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <FileSpreadsheet className="w-4 h-4 text-purple-600" />
                      <span className="font-semibold text-slate-800">ITR Returns</span>
                    </div>
                    <span className="font-bold text-slate-900">{dashboard?.compliance?.itr?.total ?? 0} Total</span>
                  </Link>
                )}
                {isModuleAvailable('TDS') && (
                  <Link to="/tds" className="p-3 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-indigo-50/40 hover:border-indigo-200 transition-all flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Percent className="w-4 h-4 text-indigo-600" />
                      <span className="font-semibold text-slate-800">TDS / TCS</span>
                    </div>
                    <span className="font-bold text-slate-900">{dashboard?.compliance?.tds?.total ?? 0} Total</span>
                  </Link>
                )}
              </div>
            </div>
          </div>
        </Card>

        {/* Reminders & Action Alerts */}
        <Card
          title="Reminders & Notifications"
          subtitle="Scheduled operational follow-ups"
          className="lg:col-span-1"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-2 text-center">
              <Link to="/reminders" className="p-3 bg-amber-50/70 border border-amber-200/60 rounded-xl hover:border-amber-400 transition-all block">
                <p className="text-[11px] text-amber-800 font-medium">Pending</p>
                <p className="text-lg font-black text-amber-900 mt-0.5">{dashboard?.reminders?.pending ?? 0}</p>
              </Link>
              <Link to="/reminders" className="p-3 bg-rose-50/70 border border-rose-200/60 rounded-xl hover:border-rose-400 transition-all block">
                <p className="text-[11px] text-rose-800 font-medium">Overdue</p>
                <p className="text-lg font-black text-rose-900 mt-0.5">{dashboard?.reminders?.overdue ?? 0}</p>
              </Link>
              <Link to="/reminders" className="p-3 bg-blue-50/70 border border-blue-200/60 rounded-xl hover:border-blue-400 transition-all block">
                <p className="text-[11px] text-blue-800 font-medium">Upcoming</p>
                <p className="text-lg font-black text-blue-900 mt-0.5">{dashboard?.reminders?.upcoming ?? 0}</p>
              </Link>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <Bell className="w-4 h-4 text-blue-600" />
                <span className="font-medium text-slate-700">Automation Rule Reminders</span>
              </div>
              <Link to="/reminders" className="text-blue-600 font-semibold hover:underline flex items-center gap-1">
                View All <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          </div>
        </Card>
      </div>

      {/* 5. Compliance Registers Row: DSC & UDIN Registers */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* DSC Register Widget */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">DSC Health</h3>
            </div>
            <Link to="/dsc" className="text-xs text-blue-600 font-semibold hover:underline">Manage</Link>
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
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">UDIN Status</h3>
            </div>
            <Link to="/udin" className="text-xs text-blue-600 font-semibold hover:underline">Manage</Link>
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
                  <AlertCircle className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Tax Notices</h3>
              </div>
              <Link to="/notices" className="text-xs text-blue-600 font-semibold hover:underline">View</Link>
            </div>
            <div className="flex items-center justify-between p-2 rounded-lg bg-rose-50/50 border border-rose-100 text-xs">
              <span className="text-slate-600 font-medium">Active Notices:</span>
              <span className="font-bold text-rose-700">{dashboard?.openTaxNotices ?? 0} Open</span>
            </div>
          </div>
        )}

        {/* Document Requests Widget */}
        {isModuleAvailable('DOCUMENTS') && (
          <div className="bg-white border border-slate-200/90 rounded-2xl p-4 shadow-card hover:border-slate-300 transition-all">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                  <FileCheck2 className="w-4 h-4" />
                </div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Document Desk</h3>
              </div>
              <Link to="/documents" className="text-xs text-blue-600 font-semibold hover:underline">Desk</Link>
            </div>
            <div className="flex items-center justify-between p-2 rounded-lg bg-blue-50/50 border border-blue-100 text-xs">
              <span className="text-slate-600 font-medium">Pending Requests:</span>
              <span className="font-bold text-blue-700">{dashboard?.pendingDocumentRequests ?? 0} Pending</span>
            </div>
          </div>
        )}
      </div>

      {/* 6. Lower Grid: Team Workload Allocation & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Team Workload Table (Hidden for SOLO practices) */}
        {!isSolo && isModuleAvailable('TASKS') && (
          <Card
            title={isStaff ? 'My Practice Workload & Tasks' : 'CA Team & Staff Workload Allocation'}
            subtitle={isStaff ? 'Breakdown of your current deliverable queue' : 'Assigned vs pending tasks per practice staff member'}
            className="lg:col-span-2"
            noPadding
          >
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/80 font-semibold text-slate-500 uppercase tracking-wider">
                    <th className="px-5 py-3">Employee</th>
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

            {/* Mobile card list */}
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

        {/* Recent Practice Activity Feed */}
        <Card
          title="Recent Practice Activity"
          subtitle="Audit log of operations across team"
          className={!isSolo && isModuleAvailable('TASKS') ? 'lg:col-span-1' : 'lg:col-span-3'}
        >
          <div className="space-y-3">
            {isLoading ? (
              <p className="text-xs text-slate-400 text-center py-6">Loading recent activity...</p>
            ) : !dashboard?.recentActivity?.length ? (
              <p className="text-xs text-slate-400 text-center py-6">No recent practice operations recorded</p>
            ) : (
              <div className="space-y-2.5">
                {dashboard.recentActivity.map((act) => (
                  <div key={act.id} className="p-2.5 rounded-xl bg-slate-50 border border-slate-100 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-800">{act.action}</span>
                      <span className="text-[10px] text-slate-400">
                        {act.createdAt ? new Date(act.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-600 mt-1">{act.description}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};
