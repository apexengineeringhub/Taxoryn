import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Users,
  UserCheck,
  CreditCard,
  Store,
  MessageSquare,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  ArrowRight,
  Sparkles,
  Server,
  Zap,
  Activity,
  Check,
} from 'lucide-react';
import { platformDashboardApi } from '../api/endpoints';
import { PlatformDashboardSummary, RecentPlatformActivity } from '../types';
import { useAuth } from '../context/AuthContext';
import { SupportOverviewPage } from './SupportOverviewPage';
import clsx from 'clsx';

export const PlatformOverviewPage: React.FC = () => {
  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isSupportAdmin = userRoleCodes.includes('TAXORYN_SUPPORT_ADMIN');

  if (isSupportAdmin) {
    return <SupportOverviewPage />;
  }

  const [data, setData] = useState<PlatformDashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadPlatformOverview();
  }, []);

  const loadPlatformOverview = async () => {
    try {
      setIsLoading(true);
      const summary = await platformDashboardApi.getOverview();
      setData(summary);
    } catch (err) {
      console.error('Failed to load platform overview dashboard', err);
    } finally {
      setIsLoading(false);
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  const formatTimeAgo = (timestamp?: string) => {
    if (!timestamp) return 'Recent';
    const now = new Date();
    const date = new Date(timestamp);
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}h ago`;
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays === 1) return 'Yesterday';
    if (diffInDays < 30) return `${diffInDays}d ago`;
    return date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
  };

  // Reusable mapper for human-readable activity labels
  const formatActivityItem = (act: any): RecentPlatformActivity => {
    if (act.displayTitle) {
      return act as RecentPlatformActivity;
    }

    const action = act.action || 'SYSTEM_EVENT';
    let title = 'Platform event recorded';
    let target = act.entityType || 'Platform System';
    let nav = '/audit-logs';
    let severity: 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS' = 'INFO';

    if (action.includes('PRACTICE') || action.includes('ORGANIZATION')) {
      nav = '/admin/practices';
      target = 'Tax Practice';
      if (action.includes('CREATE') || action.includes('REGISTER')) title = 'New practice registered';
      else if (action.includes('VERIF')) { title = 'Practice verified'; severity = 'SUCCESS'; }
      else if (action.includes('SUSPEND')) { title = 'Practice suspended'; severity = 'WARNING'; }
      else title = 'Practice updated';
    } else if (action.includes('FEEDBACK')) {
      nav = '/admin/feedback';
      target = 'Feedback Ops';
      if (action.includes('CREATE')) title = 'New feedback received';
      else if (action.includes('RESOLV')) { title = 'Feedback resolved'; severity = 'SUCCESS'; }
      else if (action.includes('ESCALAT')) { title = 'Feedback escalated'; severity = 'WARNING'; }
      else title = 'Feedback triaged';
    } else if (action.includes('MARKETPLACE') || action.includes('LEAD') || action.includes('REQUIREMENT')) {
      nav = '/admin/marketplace';
      target = 'Marketplace Services';
      if (action.includes('CONSULTATION')) title = 'Marketplace consultation booked';
      else if (action.includes('LEAD') || action.includes('ENQUIRY')) title = 'New marketplace enquiry';
      else title = 'New marketplace requirement';
    } else if (action.includes('SUBSCRIPTION') || action.includes('PAYMENT')) {
      nav = '/admin/subscriptions';
      target = 'SaaS Subscription';
      if (action.includes('UPGRADE')) title = 'Subscription upgraded';
      else title = 'Subscription updated';
    } else if (action.includes('USER') || action.includes('CUSTOMER')) {
      nav = '/admin/users';
      target = 'Platform User';
      if (action.includes('CUSTOMER')) title = 'New marketplace customer';
      else if (action.includes('ROLE')) title = 'Admin role changed';
      else title = 'New user registered';
    } else if (action.includes('SECURITY') || action.includes('TOKEN')) {
      nav = '/audit-logs';
      target = 'Platform Security';
      title = 'Security event detected';
      severity = 'WARNING';
    }

    return {
      id: act.id || Math.random().toString(),
      displayTitle: title,
      description: target,
      targetDisplayName: target,
      timestamp: act.timestamp || new Date().toISOString(),
      severity,
      status: 'SUCCESS',
      navigationTarget: nav,
    };
  };

  // Values extracted with clean fallbacks
  const activePractices = data?.summary?.activePractices ?? data?.kpis?.activePractices ?? 0;
  const totalPractices = data?.summary?.totalPractices ?? data?.kpis?.totalPractices ?? 0;
  const platformUsers = data?.summary?.platformUsers ?? data?.kpis?.activeUsers ?? 0;
  const totalUsers = data?.kpis?.totalUsers ?? platformUsers;
  const marketplaceCustomers = data?.summary?.marketplaceCustomers ?? data?.kpis?.activeCustomers ?? 0;
  const activeSubscriptions = data?.summary?.activeSubscriptions ?? data?.kpis?.activeSubscriptions ?? 0;
  const estimatedMrr = data?.subscriptionMetrics?.estimatedMrr ?? data?.kpis?.monthlyRecurringRevenue ?? 0;

  // Marketplace Metrics
  const newRequirements = data?.marketplace?.newRequirements ?? data?.marketplaceFunnel?.activeRequirements ?? 0;
  const activeEnquiries = data?.marketplace?.activeEnquiries ?? data?.marketplaceFunnel?.totalEnquiries ?? 0;
  const matchesCompleted = data?.marketplace?.matchesCompleted ?? data?.marketplaceFunnel?.matchedRequirements ?? 0;
  const consultationsBooked = data?.marketplace?.consultationsBooked ?? data?.marketplaceFunnel?.acceptedEnquiries ?? 0;

  // Attention Items
  const pendingVerification = data?.attention?.pendingPracticeVerification ?? data?.practiceEcosystem?.pendingVerification ?? 0;
  const openFeedback = data?.attention?.openFeedback ?? data?.kpis?.openFeedback ?? 0;
  const securityAlerts = data?.attention?.securityAlerts ?? 0;
  const paymentIssues = data?.attention?.paymentIssues ?? data?.practiceEcosystem?.suspendedPractices ?? 0;
  const marketplaceIssues = data?.attention?.marketplaceIssues ?? 0;

  const totalAttentionCount = pendingVerification + openFeedback + securityAlerts + paymentIssues + marketplaceIssues;

  // Subsystems Health
  const healthApi = data?.health?.api ?? 'HEALTHY';
  const healthDb = data?.health?.database ?? data?.platformHealth?.databaseStatus ?? 'HEALTHY';
  const healthJobs = data?.health?.backgroundJobs ?? 'HEALTHY';
  const healthMarketplace = data?.health?.marketplace ?? 'HEALTHY';
  const healthNotifications = data?.health?.notifications ?? 'HEALTHY';

  // Activities list (up to 6 events)
  const activities: RecentPlatformActivity[] = (
    data?.recentActivity?.length
      ? data.recentActivity
      : (data?.recentActivities || []).map(formatActivityItem)
  ).slice(0, 6);

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* ========================================================================= */}
      {/* A. PAGE HEADER                                                            */}
      {/* ========================================================================= */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white border border-slate-200/90 p-6 rounded-2xl shadow-card">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-purple-100 text-purple-800 border border-purple-200">
              Taxoryn Platform
            </span>
            <span className="text-slate-300 text-xs">•</span>
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              PLATFORM SUPERADMIN
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-900">
            Platform Overview
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Monitor the health, growth and important activity of the Taxoryn platform.
          </p>
        </div>

        <div className="flex items-center gap-3 self-start sm:self-auto">
          <div className="bg-emerald-50 border border-emerald-200/80 px-3.5 py-1.5 rounded-xl text-xs font-bold text-emerald-800 flex items-center gap-2 shadow-2xs">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Platform Live</span>
          </div>
          <button
            onClick={loadPlatformOverview}
            disabled={isLoading}
            className="p-2 bg-slate-100 hover:bg-slate-200 border border-slate-200 rounded-xl text-slate-600 transition-all shadow-2xs"
            title="Refresh Platform Metrics"
          >
            <RefreshCw className={clsx('w-4 h-4', isLoading && 'animate-spin')} />
          </button>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* B. 4 KPI SUMMARY CARDS                                                    */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Active Practices */}
        <Link
          to="/admin/practices"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-purple-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-700 transition-colors">
              Active Practices
            </span>
            <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all shadow-2xs">
              <Building2 className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : activePractices.toLocaleString('en-IN')}
            </span>
            <span className="text-xs font-bold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full border border-purple-200">
              of {totalPractices.toLocaleString('en-IN')} total
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Tenants & CAs</span>
            <span className="font-semibold text-purple-700 flex items-center gap-0.5">
              Manage <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>
        </Link>

        {/* KPI 2: Platform Users */}
        <Link
          to="/admin/users"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-blue-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-700 transition-colors">
              Platform Users
            </span>
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all shadow-2xs">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : platformUsers.toLocaleString('en-IN')}
            </span>
            <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200">
              of {totalUsers.toLocaleString('en-IN')} total
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Staff, CAs & Admins</span>
            <span className="font-semibold text-blue-700 flex items-center gap-0.5">
              Manage <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>
        </Link>

        {/* KPI 3: Marketplace Customers */}
        <Link
          to="/admin/users"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-sky-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-sky-700 transition-colors">
              Marketplace Customers
            </span>
            <div className="w-9 h-9 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center group-hover:bg-sky-600 group-hover:text-white transition-all shadow-2xs">
              <UserCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : marketplaceCustomers.toLocaleString('en-IN')}
            </span>
            <span className="text-xs font-bold text-sky-700 bg-sky-50 px-2 py-0.5 rounded-full border border-sky-200">
              Active Taxpayers
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Registered Clients</span>
            <span className="font-semibold text-sky-700 flex items-center gap-0.5">
              View <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>
        </Link>

        {/* KPI 4: Active Subscriptions */}
        <Link
          to="/admin/subscriptions"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-emerald-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-emerald-700 transition-colors">
              Active Subscriptions
            </span>
            <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-all shadow-2xs">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : activeSubscriptions.toLocaleString('en-IN')}
            </span>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              {estimatedMrr > 0 ? formatCurrency(estimatedMrr) + '/mo' : 'SaaS Tiers'}
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>SaaS Billing Plans</span>
            <span className="font-semibold text-emerald-700 flex items-center gap-0.5">
              Plans <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>
        </Link>
      </div>

      {/* ========================================================================= */}
      {/* C & D: MARKETPLACE OVERVIEW + ATTENTION REQUIRED (2-Column)               */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* C. Marketplace Overview */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-card flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-5">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shadow-2xs">
                  <Store className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Marketplace</h3>
                  <p className="text-xs text-slate-500">Tax demand, matching & consultations</p>
                </div>
              </div>
              <Link
                to="/admin/marketplace"
                className="text-xs font-bold text-amber-700 hover:text-amber-800 inline-flex items-center gap-1 group"
              >
                <span>View Marketplace</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </div>

            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="p-3.5 rounded-xl bg-slate-50/80 border border-slate-200/80">
                <p className="text-xs font-semibold text-slate-500">New Requirements</p>
                <p className="text-2xl font-black text-slate-900 mt-1">
                  {isLoading ? '...' : newRequirements.toLocaleString('en-IN')}
                </p>
                <span className="text-[11px] text-slate-400 mt-0.5 block">Customer filings posted</span>
              </div>

              <div className="p-3.5 rounded-xl bg-blue-50/60 border border-blue-200/70">
                <p className="text-xs font-semibold text-blue-700">Active Enquiries</p>
                <p className="text-2xl font-black text-blue-900 mt-1">
                  {isLoading ? '...' : activeEnquiries.toLocaleString('en-IN')}
                </p>
                <span className="text-[11px] text-blue-600 mt-0.5 block">Inbound consultation leads</span>
              </div>

              <div className="p-3.5 rounded-xl bg-purple-50/60 border border-purple-200/70">
                <p className="text-xs font-semibold text-purple-700">Matches Completed</p>
                <p className="text-2xl font-black text-purple-900 mt-1">
                  {isLoading ? '...' : matchesCompleted.toLocaleString('en-IN')}
                </p>
                <span className="text-[11px] text-purple-600 mt-0.5 block">Client-CA connections</span>
              </div>

              <div className="p-3.5 rounded-xl bg-emerald-50/60 border border-emerald-200/70">
                <p className="text-xs font-semibold text-emerald-700">Consultations Booked</p>
                <p className="text-2xl font-black text-emerald-900 mt-1">
                  {isLoading ? '...' : consultationsBooked.toLocaleString('en-IN')}
                </p>
                <span className="text-[11px] text-emerald-600 mt-0.5 block">Active engagements</span>
              </div>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Multi-tenant matching engine active</span>
            <Link
              to="/admin/marketplace"
              className="font-bold text-amber-700 hover:text-amber-800"
            >
              Marketplace Ops →
            </Link>
          </div>
        </div>

        {/* D. Attention Required */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-card flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
              <div className="flex items-center gap-3">
                <div className={clsx(
                  'w-9 h-9 rounded-xl flex items-center justify-center shadow-2xs',
                  totalAttentionCount > 0 ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'
                )}>
                  {totalAttentionCount > 0 ? <AlertTriangle className="w-5 h-5" /> : <CheckCircle2 className="w-5 h-5" />}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base font-bold text-slate-900">Attention Required</h3>
                    {totalAttentionCount > 0 && (
                      <span className="px-2 py-0.5 text-[10px] font-black rounded-full bg-rose-100 text-rose-800 border border-rose-200">
                        {totalAttentionCount} Action{totalAttentionCount > 1 ? 's' : ''}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500">Actionable items requiring platform administrator review</p>
                </div>
              </div>
            </div>

            {totalAttentionCount === 0 && !isLoading ? (
              <div className="py-8 text-center flex flex-col items-center justify-center">
                <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mb-2.5">
                  <Check className="w-6 h-6 stroke-[2.5]" />
                </div>
                <p className="text-sm font-bold text-slate-800">All systems look good</p>
                <p className="text-xs text-slate-500 mt-0.5">No urgent platform actions requiring attention</p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {/* 1. Pending Practice Verification */}
                {pendingVerification > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-amber-50/70 border border-amber-200/80">
                    <div className="flex items-center gap-2.5">
                      <span className="w-2 h-2 rounded-full bg-amber-500 shrink-0"></span>
                      <div>
                        <p className="text-xs font-bold text-amber-950">Pending Practice Verification</p>
                        <p className="text-[11px] text-amber-700">{pendingVerification} practice{pendingVerification > 1 ? 's' : ''} awaiting KYC approval</p>
                      </div>
                    </div>
                    <Link
                      to="/admin/practices"
                      className="px-3 py-1 bg-white hover:bg-amber-100 border border-amber-200 text-amber-900 text-xs font-bold rounded-lg shadow-2xs transition-colors"
                    >
                      Review →
                    </Link>
                  </div>
                )}

                {/* 2. Open Feedback */}
                {openFeedback > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-purple-50/70 border border-purple-200/80">
                    <div className="flex items-center gap-2.5">
                      <span className="w-2 h-2 rounded-full bg-purple-500 shrink-0"></span>
                      <div>
                        <p className="text-xs font-bold text-purple-950">Open Feedback</p>
                        <p className="text-[11px] text-purple-700">{openFeedback} item{openFeedback > 1 ? 's' : ''} pending triage</p>
                      </div>
                    </div>
                    <Link
                      to="/admin/feedback"
                      className="px-3 py-1 bg-white hover:bg-purple-100 border border-purple-200 text-purple-900 text-xs font-bold rounded-lg shadow-2xs transition-colors"
                    >
                      Review →
                    </Link>
                  </div>
                )}

                {/* 3. Security Alerts */}
                {securityAlerts > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-rose-50/70 border border-rose-200/80">
                    <div className="flex items-center gap-2.5">
                      <span className="w-2 h-2 rounded-full bg-rose-500 shrink-0"></span>
                      <div>
                        <p className="text-xs font-bold text-rose-950">Security Alerts</p>
                        <p className="text-[11px] text-rose-700">{securityAlerts} high-priority event{securityAlerts > 1 ? 's' : ''}</p>
                      </div>
                    </div>
                    <Link
                      to="/audit-logs"
                      className="px-3 py-1 bg-white hover:bg-rose-100 border border-rose-200 text-rose-900 text-xs font-bold rounded-lg shadow-2xs transition-colors"
                    >
                      Review →
                    </Link>
                  </div>
                )}

                {/* 4. Payment / Subscription Issues */}
                {paymentIssues > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-100/90 border border-slate-200">
                    <div className="flex items-center gap-2.5">
                      <span className="w-2 h-2 rounded-full bg-slate-500 shrink-0"></span>
                      <div>
                        <p className="text-xs font-bold text-slate-900">Payment & Subscription Issues</p>
                        <p className="text-[11px] text-slate-600">{paymentIssues} suspended or past-due tenant{paymentIssues > 1 ? 's' : ''}</p>
                      </div>
                    </div>
                    <Link
                      to="/admin/subscriptions"
                      className="px-3 py-1 bg-white hover:bg-slate-200 border border-slate-200 text-slate-800 text-xs font-bold rounded-lg shadow-2xs transition-colors"
                    >
                      Review →
                    </Link>
                  </div>
                )}

                {/* 5. Marketplace Issues */}
                {marketplaceIssues > 0 && (
                  <div className="flex items-center justify-between p-3 rounded-xl bg-amber-50/70 border border-amber-200/80">
                    <div className="flex items-center gap-2.5">
                      <span className="w-2 h-2 rounded-full bg-amber-500 shrink-0"></span>
                      <div>
                        <p className="text-xs font-bold text-amber-950">Marketplace Issues</p>
                        <p className="text-[11px] text-amber-700">{marketplaceIssues} unfulfilled request{marketplaceIssues > 1 ? 's' : ''}</p>
                      </div>
                    </div>
                    <Link
                      to="/admin/marketplace"
                      className="px-3 py-1 bg-white hover:bg-amber-100 border border-amber-200 text-amber-900 text-xs font-bold rounded-lg shadow-2xs transition-colors"
                    >
                      Review →
                    </Link>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Prioritized by operational severity</span>
            {totalAttentionCount > 0 && (
              <span className="font-semibold text-slate-700">Action required</span>
            )}
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* E & F: PLATFORM HEALTH + RECENT IMPORTANT ACTIVITY (2-Column)             */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* E. Platform Health */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-card flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-5">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shadow-2xs">
                  <Activity className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Platform Health</h3>
                  <p className="text-xs text-slate-500">High-level subsystem operational status</p>
                </div>
              </div>
              <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full flex items-center gap-1.5 shadow-2xs">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span>Operational</span>
              </span>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-200/70">
                <span className="text-xs font-bold text-slate-700">API Gateway</span>
                <span className={clsx(
                  'text-xs font-bold flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border',
                  healthApi === 'HEALTHY'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}>
                  <span className={clsx('w-2 h-2 rounded-full', healthApi === 'HEALTHY' ? 'bg-emerald-500' : 'bg-rose-500')}></span>
                  {healthApi === 'HEALTHY' ? 'Healthy' : 'Degraded'}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-200/70">
                <span className="text-xs font-bold text-slate-700">Database</span>
                <span className={clsx(
                  'text-xs font-bold flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border',
                  healthDb === 'HEALTHY'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}>
                  <span className={clsx('w-2 h-2 rounded-full', healthDb === 'HEALTHY' ? 'bg-emerald-500' : 'bg-rose-500')}></span>
                  {healthDb === 'HEALTHY' ? 'Healthy' : 'Degraded'}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-200/70">
                <span className="text-xs font-bold text-slate-700">Background Jobs</span>
                <span className={clsx(
                  'text-xs font-bold flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border',
                  healthJobs === 'HEALTHY'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}>
                  <span className={clsx('w-2 h-2 rounded-full', healthJobs === 'HEALTHY' ? 'bg-emerald-500' : 'bg-rose-500')}></span>
                  {healthJobs === 'HEALTHY' ? 'Healthy' : 'Degraded'}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-200/70">
                <span className="text-xs font-bold text-slate-700">Marketplace Engine</span>
                <span className={clsx(
                  'text-xs font-bold flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border',
                  healthMarketplace === 'HEALTHY'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}>
                  <span className={clsx('w-2 h-2 rounded-full', healthMarketplace === 'HEALTHY' ? 'bg-emerald-500' : 'bg-rose-500')}></span>
                  {healthMarketplace === 'HEALTHY' ? 'Healthy' : 'Degraded'}
                </span>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-200/70">
                <span className="text-xs font-bold text-slate-700">Notifications Subsystem</span>
                <span className={clsx(
                  'text-xs font-bold flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border',
                  healthNotifications === 'HEALTHY'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}>
                  <span className={clsx('w-2 h-2 rounded-full', healthNotifications === 'HEALTHY' ? 'bg-emerald-500' : 'bg-rose-500')}></span>
                  {healthNotifications === 'HEALTHY' ? 'Healthy' : 'Degraded'}
                </span>
              </div>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Zero open platform outages</span>
            <span className="font-semibold text-emerald-700">99.98% Uptime</span>
          </div>
        </div>

        {/* F. Recent Important Activity */}
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-card flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center shadow-2xs">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Recent Important Activity</h3>
                  <p className="text-xs text-slate-500">Platform registrations, updates & business events</p>
                </div>
              </div>
              <Link
                to="/audit-logs"
                className="text-xs font-bold text-purple-700 hover:text-purple-800 flex items-center gap-1 group"
              >
                <span>View Full Audit</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
              </Link>
            </div>

            {isLoading ? (
              <div className="py-8 text-center text-xs text-slate-400">Loading recent activity...</div>
            ) : !activities.length ? (
              <div className="py-8 text-center text-xs text-slate-400">No important activity yet</div>
            ) : (
              <div className="divide-y divide-slate-100">
                {activities.map((act) => (
                  <div key={act.id} className="py-2.5 flex items-center justify-between hover:bg-slate-50/60 rounded-lg px-2 -mx-2 transition-colors">
                    <div className="flex items-start gap-2.5 truncate">
                      <span className={clsx(
                        'w-2 h-2 rounded-full mt-1.5 shrink-0',
                        act.severity === 'WARNING'
                          ? 'bg-amber-500'
                          : act.severity === 'CRITICAL'
                          ? 'bg-rose-500'
                          : act.severity === 'SUCCESS'
                          ? 'bg-emerald-500'
                          : 'bg-purple-500'
                      )}></span>
                      <div className="truncate">
                        <p className="text-xs font-bold text-slate-900 truncate">{act.displayTitle}</p>
                        <p className="text-[11px] text-slate-500 truncate">{act.targetDisplayName || act.description}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0 ml-3">
                      <span className="text-[11px] text-slate-400 font-medium">
                        {formatTimeAgo(act.timestamp)}
                      </span>
                      {act.navigationTarget && (
                        <Link
                          to={act.navigationTarget}
                          className="text-purple-600 hover:text-purple-800 p-1 hover:bg-purple-50 rounded"
                          title="Open Module"
                        >
                          <ArrowRight className="w-3.5 h-3.5" />
                        </Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Detailed forensic logs in Security & Audit</span>
            <Link
              to="/audit-logs"
              className="font-bold text-purple-700 hover:text-purple-800"
            >
              View Full Audit →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
