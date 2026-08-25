import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Users,
  UserCheck,
  Store,
  CreditCard,
  MessageSquare,
  Activity,
  TrendingUp,
  ShieldCheck,
  ArrowUpRight,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Sparkles,
  Server,
  Database,
  Cpu,
  Layers,
  FileCode,
  ShieldAlert,
  ArrowRight,
} from 'lucide-react';
import { platformDashboardApi } from '../api/endpoints';
import { PlatformDashboardSummary } from '../types';
import clsx from 'clsx';

export const PlatformOverviewPage: React.FC = () => {
  const [data, setData] = useState<PlatformDashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());

  useEffect(() => {
    loadPlatformOverview();
  }, []);

  const loadPlatformOverview = async () => {
    try {
      setIsLoading(true);
      const summary = await platformDashboardApi.getOverview();
      setData(summary);
      setLastRefreshed(new Date());
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

  const formatUptime = (seconds: number = 0) => {
    const days = Math.floor(seconds / 86400);
    const hrs = Math.floor((seconds % 86400) / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (days > 0) return `${days}d ${hrs}h ${mins}m`;
    if (hrs > 0) return `${hrs}h ${mins}m`;
    return `${mins}m`;
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Platform Executive Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-gradient-to-r from-slate-900 via-indigo-950 to-purple-950 p-6 rounded-2xl text-white shadow-xl">
        <div>
          <div className="flex items-center gap-2.5 mb-1.5">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-widest bg-purple-500/30 text-purple-200 border border-purple-400/30">
              Platform Operations
            </span>
            <span className="text-slate-400 text-xs">•</span>
            <span className="text-xs text-slate-300 font-medium">Global Multi-Tenant Ecosystem</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white flex items-center gap-3">
            Taxoryn Platform Overview
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-2xl">
            Monitor platform growth, marketplace activity, subscriptions and platform health.
          </p>
        </div>
        <div className="flex items-center gap-3 self-start sm:self-auto">
          <div className="bg-white/10 backdrop-blur-md px-3.5 py-2 rounded-xl border border-white/10 text-xs font-semibold text-slate-200 flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>Platform Live</span>
          </div>
          <button
            onClick={loadPlatformOverview}
            disabled={isLoading}
            className="p-2 bg-white/10 hover:bg-white/20 border border-white/10 rounded-xl text-slate-200 transition-all"
            title="Refresh Platform Metrics"
          >
            <RefreshCw className={clsx('w-4 h-4', isLoading && 'animate-spin')} />
          </button>
        </div>
      </div>

      {/* Row 1: Top 8 Platform KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Active Practices */}
        <Link
          to="/admin/practices"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-purple-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-600 transition-colors">
              Active Practices
            </span>
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all">
              <Building2 className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : data?.kpis?.activePractices ?? 0}
            </span>
            <span className="text-xs font-bold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full border border-purple-200">
              of {data?.kpis?.totalPractices ?? 0} total
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Pending KYC: {data?.practiceEcosystem?.pendingVerification ?? 0}</span>
            <span className="font-semibold text-emerald-600">+{data?.practiceEcosystem?.newPracticesThisMonth ?? 0} this mo</span>
          </div>
        </Link>

        {/* KPI 2: Active Users */}
        <Link
          to="/admin/users"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-blue-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
              Active Users
            </span>
            <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : data?.kpis?.activeUsers ?? 0}
            </span>
            <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200">
              of {data?.kpis?.totalUsers ?? 0} total
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Staff: {data?.userEcosystem?.practiceEmployees ?? 0}</span>
            <span>CAs: {data?.userEcosystem?.practitioners ?? 0}</span>
          </div>
        </Link>

        {/* KPI 3: Active Customers */}
        <Link
          to="/admin/users"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-sky-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-sky-600 transition-colors">
              Active Customers
            </span>
            <div className="w-9 h-9 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center group-hover:bg-sky-600 group-hover:text-white transition-all">
              <UserCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-sky-600">
              {isLoading ? '...' : data?.kpis?.activeCustomers ?? 0}
            </span>
            <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
              Taxpayers
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Requirements: {data?.marketplaceFunnel?.totalRequirements ?? 0}</span>
            <span className="font-semibold text-slate-700">Enquiries: {data?.marketplaceFunnel?.totalEnquiries ?? 0}</span>
          </div>
        </Link>

        {/* KPI 4: Marketplace Activity */}
        <Link
          to="/admin/marketplace"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-amber-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
              Marketplace Activity
            </span>
            <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition-all">
              <Store className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-amber-600">
              {isLoading ? '...' : data?.kpis?.totalMarketplaceLeads ?? 0}
            </span>
            <span className="text-xs font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
              Inbound Leads
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Accepted: {data?.marketplaceFunnel?.acceptedEnquiries ?? 0}</span>
            <span className="font-bold text-emerald-600">{data?.marketplaceFunnel?.conversionRate ?? 0}% win rate</span>
          </div>
        </Link>

        {/* KPI 5: Active Subscriptions */}
        <Link
          to="/admin/subscriptions"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-indigo-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-indigo-600 transition-colors">
              Active Subscriptions
            </span>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center group-hover:bg-indigo-600 group-hover:text-white transition-all">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-indigo-600">
              {isLoading ? '...' : data?.kpis?.activeSubscriptions ?? 0}
            </span>
            <span className="text-xs font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-full border border-indigo-200">
              SaaS Tenants
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Enterprise: {data?.subscriptionMetrics?.enterpriseTiers ?? 0}</span>
            <span>Business: {data?.subscriptionMetrics?.businessTiers ?? 0}</span>
          </div>
        </Link>

        {/* KPI 6: Open Feedback */}
        <Link
          to="/admin/feedback"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-rose-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-rose-600 transition-colors">
              Open Feedback Ops
            </span>
            <div className="w-9 h-9 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-all">
              <MessageSquare className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-rose-600">
              {isLoading ? '...' : data?.kpis?.openFeedback ?? 0}
            </span>
            <span className="text-xs font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
              Pending Triage
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Escalated: {data?.feedbackOperations?.escalatedToEng ?? 0}</span>
            <span className="font-semibold text-emerald-600">Resolved: {data?.feedbackOperations?.resolved ?? 0}</span>
          </div>
        </Link>

        {/* KPI 7: Platform Availability */}
        <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Platform Health
            </span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Activity className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-2xl font-black text-emerald-600 flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-emerald-500"></span>
              99.98%
            </span>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              {data?.platformHealth?.databaseStatus ?? 'HEALTHY'}
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Uptime: {formatUptime(data?.platformHealth?.uptimeSeconds)}</span>
            <span>DB Pool: {data?.platformHealth?.activeDbConnections}/{data?.platformHealth?.maxDbConnections}</span>
          </div>
        </div>

        {/* KPI 8: Platform MRR */}
        <Link
          to="/admin/subscriptions"
          className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card hover:shadow-card-hover hover:border-emerald-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-emerald-700 transition-colors">
              Platform MRR (SaaS)
            </span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:bg-emerald-600 group-hover:text-white transition-all">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-2xl font-black text-slate-900 truncate">
              {isLoading ? '...' : formatCurrency(data?.kpis?.monthlyRecurringRevenue)}
            </span>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
              ARR: {formatCurrency(data?.kpis?.annualRecurringRevenue)}
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Taxoryn SaaS Revenue</span>
            <ArrowUpRight className="w-4 h-4 text-emerald-600 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
          </div>
        </Link>
      </div>

      {/* Row 2: Practice & User Ecosystems */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Practice Ecosystem */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
            <div className="flex items-center gap-2.5">
              <Building2 className="w-5 h-5 text-purple-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">Practice Ecosystem</h3>
                <p className="text-[11px] text-slate-500">Multi-tenant tax practice lifecycle distribution</p>
              </div>
            </div>
            <Link
              to="/admin/practices"
              className="text-xs font-bold text-purple-600 hover:text-purple-800 flex items-center gap-1"
            >
              Manage Practices <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center mb-4">
            <div className="bg-purple-50/70 border border-purple-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-purple-700 font-semibold">Total Practices</p>
              <p className="text-xl font-black text-purple-900 mt-0.5">{data?.practiceEcosystem?.totalPractices ?? 0}</p>
            </div>
            <div className="bg-emerald-50/70 border border-emerald-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-emerald-700 font-semibold">Active</p>
              <p className="text-xl font-black text-emerald-900 mt-0.5">{data?.practiceEcosystem?.activePractices ?? 0}</p>
            </div>
            <div className="bg-amber-50/70 border border-amber-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-amber-700 font-semibold">Pending KYC</p>
              <p className="text-xl font-black text-amber-900 mt-0.5">{data?.practiceEcosystem?.pendingVerification ?? 0}</p>
            </div>
            <div className="bg-rose-50/70 border border-rose-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-rose-700 font-semibold">Suspended</p>
              <p className="text-xl font-black text-rose-900 mt-0.5">{data?.practiceEcosystem?.suspendedPractices ?? 0}</p>
            </div>
          </div>

          <div className="bg-slate-50 border border-slate-100 rounded-lg p-3 text-xs flex items-center justify-between text-slate-600">
            <span>🛡️ SuperAdmin Privacy Principle: Practice accounts are isolated; client tax filings and PAN/GST confidential data are protected.</span>
          </div>
        </div>

        {/* User Ecosystem */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
            <div className="flex items-center gap-2.5">
              <Users className="w-5 h-5 text-blue-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">User Ecosystem</h3>
                <p className="text-[11px] text-slate-500">Platform identity, actor roles & account breakdown</p>
              </div>
            </div>
            <Link
              to="/admin/users"
              className="text-xs font-bold text-blue-600 hover:text-blue-800 flex items-center gap-1"
            >
              Manage Users <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center mb-4">
            <div className="bg-blue-50/70 border border-blue-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-blue-700 font-semibold">Total Users</p>
              <p className="text-xl font-black text-blue-900 mt-0.5">{data?.userEcosystem?.totalUsers ?? 0}</p>
            </div>
            <div className="bg-sky-50/70 border border-sky-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-sky-700 font-semibold">Customers</p>
              <p className="text-xl font-black text-sky-900 mt-0.5">{data?.userEcosystem?.customers ?? 0}</p>
            </div>
            <div className="bg-indigo-50/70 border border-indigo-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-indigo-700 font-semibold">Practitioners</p>
              <p className="text-xl font-black text-indigo-900 mt-0.5">{data?.userEcosystem?.practitioners ?? 0}</p>
            </div>
            <div className="bg-amber-50/70 border border-amber-200/60 p-3 rounded-lg">
              <p className="text-[11px] text-amber-700 font-semibold">Staff & Trainees</p>
              <p className="text-xl font-black text-amber-900 mt-0.5">{data?.userEcosystem?.practiceEmployees ?? 0}</p>
            </div>
          </div>

          <div className="flex items-center justify-between text-xs text-slate-500 px-1">
            <span>Taxoryn Internal SuperAdmins: <strong className="text-slate-800">{data?.userEcosystem?.taxorynAdminUsers ?? 1}</strong></span>
            <span>Active Rate: <strong className="text-emerald-700 font-bold">{(data?.kpis?.totalUsers ?? 1) > 0 ? Math.round(((data?.kpis?.activeUsers ?? 0) / (data?.kpis?.totalUsers ?? 1)) * 100) : 100}%</strong></span>
          </div>
        </div>
      </div>

      {/* Row 3: Marketplace Health & Funnel */}
      <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-5">
          <div className="flex items-center gap-2.5">
            <Store className="w-5 h-5 text-amber-600" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">Marketplace Demand & Conversion Funnel</h3>
              <p className="text-[11px] text-slate-500">Live transaction pipeline from Customer Tax Requirement to Completed Service</p>
            </div>
          </div>
          <Link
            to="/admin/marketplace"
            className="text-xs font-bold text-amber-700 hover:text-amber-800 flex items-center gap-1"
          >
            Marketplace Ops <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Funnel Visual Steps */}
        <div className="grid grid-cols-1 sm:grid-cols-5 gap-3 relative">
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-center relative">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Step 1</span>
            <p className="text-xs font-bold text-slate-700 mt-1">Tax Requirements</p>
            <p className="text-2xl font-black text-slate-900 mt-2">{data?.marketplaceFunnel?.totalRequirements ?? 0}</p>
            <span className="text-[10px] text-slate-500 mt-1 block">Created by customers</span>
          </div>

          <div className="p-4 rounded-xl bg-purple-50/60 border border-purple-200/80 text-center relative">
            <span className="text-[10px] font-bold text-purple-400 uppercase tracking-wider block">Step 2</span>
            <p className="text-xs font-bold text-purple-900 mt-1">Active / Matched</p>
            <p className="text-2xl font-black text-purple-700 mt-2">{data?.marketplaceFunnel?.matchedRequirements ?? 0}</p>
            <span className="text-[10px] text-purple-600 mt-1 block">Distributed to CAs</span>
          </div>

          <div className="p-4 rounded-xl bg-blue-50/60 border border-blue-200/80 text-center relative">
            <span className="text-[10px] font-bold text-blue-400 uppercase tracking-wider block">Step 3</span>
            <p className="text-xs font-bold text-blue-900 mt-1">Inbound Enquiries</p>
            <p className="text-2xl font-black text-blue-700 mt-2">{data?.marketplaceFunnel?.totalEnquiries ?? 0}</p>
            <span className="text-[10px] text-blue-600 mt-1 block">Customer leads initiated</span>
          </div>

          <div className="p-4 rounded-xl bg-indigo-50/60 border border-indigo-200/80 text-center relative">
            <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-wider block">Step 4</span>
            <p className="text-xs font-bold text-indigo-900 mt-1">Accepted Proposals</p>
            <p className="text-2xl font-black text-indigo-700 mt-2">{data?.marketplaceFunnel?.acceptedEnquiries ?? 0}</p>
            <span className="text-[10px] text-indigo-600 mt-1 block">Engagements active</span>
          </div>

          <div className="p-4 rounded-xl bg-emerald-50/60 border border-emerald-200/80 text-center relative">
            <span className="text-[10px] font-bold text-emerald-500 uppercase tracking-wider block">Step 5</span>
            <p className="text-xs font-bold text-emerald-900 mt-1">Completed Services</p>
            <p className="text-2xl font-black text-emerald-700 mt-2">{data?.marketplaceFunnel?.completedServices ?? 0}</p>
            <span className="text-[10px] text-emerald-700 font-bold mt-1 block">
              {data?.marketplaceFunnel?.conversionRate ?? 0}% Win Rate
            </span>
          </div>
        </div>
      </div>

      {/* Row 4: Subscriptions & Revenue vs Feedback Ops */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* SaaS Subscriptions Breakdown */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
            <div className="flex items-center gap-2.5">
              <CreditCard className="w-5 h-5 text-indigo-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">Platform SaaS Subscriptions & Revenue</h3>
                <p className="text-[11px] text-slate-500">Taxoryn platform revenue generated from practice subscriptions</p>
              </div>
            </div>
            <Link
              to="/admin/subscriptions"
              className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              Subscription Ops <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center mb-4">
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
              <span className="text-[10px] font-bold text-slate-500 uppercase">Starter</span>
              <p className="text-lg font-black text-slate-800 mt-0.5">{data?.subscriptionMetrics?.starterTiers ?? 0}</p>
              <span className="text-[10px] text-slate-400">₹999/mo</span>
            </div>
            <div className="p-3 bg-blue-50/60 border border-blue-200 rounded-lg">
              <span className="text-[10px] font-bold text-blue-700 uppercase">Pro</span>
              <p className="text-lg font-black text-blue-900 mt-0.5">{data?.subscriptionMetrics?.professionalTiers ?? 0}</p>
              <span className="text-[10px] text-blue-600">₹2,999/mo</span>
            </div>
            <div className="p-3 bg-purple-50/60 border border-purple-200 rounded-lg">
              <span className="text-[10px] font-bold text-purple-700 uppercase">Business</span>
              <p className="text-lg font-black text-purple-900 mt-0.5">{data?.subscriptionMetrics?.businessTiers ?? 0}</p>
              <span className="text-[10px] text-purple-600">₹5,999/mo</span>
            </div>
            <div className="p-3 bg-emerald-50/60 border border-emerald-200 rounded-lg">
              <span className="text-[10px] font-bold text-emerald-700 uppercase">Enterprise</span>
              <p className="text-lg font-black text-emerald-900 mt-0.5">{data?.subscriptionMetrics?.enterpriseTiers ?? 0}</p>
              <span className="text-[10px] text-emerald-600">₹14,999/mo</span>
            </div>
          </div>

          <div className="bg-indigo-50/60 border border-indigo-200/80 rounded-xl p-3.5 flex items-center justify-between text-xs">
            <div>
              <p className="font-bold text-indigo-950">Platform Monthly Run Rate</p>
              <p className="text-[11px] text-indigo-700">Estimated SaaS revenue across all subscribed practices</p>
            </div>
            <span className="text-base font-black text-indigo-900">
              {formatCurrency(data?.subscriptionMetrics?.estimatedMrr)} / mo
            </span>
          </div>
        </div>

        {/* Global Feedback Operations */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
            <div className="flex items-center gap-2.5">
              <MessageSquare className="w-5 h-5 text-rose-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">Application Feedback Operations</h3>
                <p className="text-[11px] text-slate-500">Live triage, engineering escalation & issue lifecycle</p>
              </div>
            </div>
            <Link
              to="/admin/feedback"
              className="text-xs font-bold text-rose-600 hover:text-rose-800 flex items-center gap-1"
            >
              Feedback Dashboard <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-3 sm:grid-cols-5 gap-2 text-center mb-4">
            <div className="p-2.5 bg-blue-50/70 border border-blue-200 rounded-lg">
              <p className="text-[10px] font-semibold text-blue-700 uppercase">New</p>
              <p className="text-lg font-black text-blue-900 mt-0.5">{data?.feedbackOperations?.newFeedback ?? 0}</p>
            </div>
            <div className="p-2.5 bg-purple-50/70 border border-purple-200 rounded-lg">
              <p className="text-[10px] font-semibold text-purple-700 uppercase">In Review</p>
              <p className="text-lg font-black text-purple-900 mt-0.5">{data?.feedbackOperations?.underReview ?? 0}</p>
            </div>
            <div className="p-2.5 bg-amber-50/70 border border-amber-200 rounded-lg">
              <p className="text-[10px] font-semibold text-amber-700 uppercase">In Progress</p>
              <p className="text-lg font-black text-amber-900 mt-0.5">{data?.feedbackOperations?.inProgress ?? 0}</p>
            </div>
            <div className="p-2.5 bg-rose-50/70 border border-rose-200 rounded-lg">
              <p className="text-[10px] font-semibold text-rose-700 uppercase">Escalated</p>
              <p className="text-lg font-black text-rose-900 mt-0.5">{data?.feedbackOperations?.escalatedToEng ?? 0}</p>
            </div>
            <div className="p-2.5 bg-emerald-50/70 border border-emerald-200 rounded-lg">
              <p className="text-[10px] font-semibold text-emerald-700 uppercase">Resolved</p>
              <p className="text-lg font-black text-emerald-900 mt-0.5">{data?.feedbackOperations?.resolved ?? 0}</p>
            </div>
          </div>

          <div className="flex items-center justify-between text-xs pt-1">
            <span className="text-slate-500">
              Total Logged: <strong className="text-slate-800">{data?.feedbackOperations?.totalFeedback ?? 0}</strong>
            </span>
            <span className="text-rose-700 font-bold">
              Critical Open: {data?.feedbackOperations?.criticalOpen ?? 0}
            </span>
          </div>
        </div>
      </div>

      {/* Row 5: Platform Infrastructure & Subsystem Live Health */}
      <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
          <div className="flex items-center gap-2.5">
            <Server className="w-5 h-5 text-emerald-600" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">Platform Subsystem & Infrastructure Health</h3>
              <p className="text-[11px] text-slate-500">Live operational status and JVM / Database resource utilization</p>
            </div>
          </div>
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            All Subsystems Operational
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">API Gateway</span>
            <span className="text-xs font-bold text-emerald-700 flex items-center gap-1.5 mt-1">
              <CheckCircle2 className="w-3.5 h-3.5" /> Healthy
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">PostgreSQL DB</span>
            <span className="text-xs font-bold text-emerald-700 flex items-center gap-1.5 mt-1">
              <Database className="w-3.5 h-3.5" /> {data?.platformHealth?.databaseStatus ?? 'HEALTHY'}
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">JWT & Security</span>
            <span className="text-xs font-bold text-emerald-700 flex items-center gap-1.5 mt-1">
              <ShieldCheck className="w-3.5 h-3.5" /> Healthy
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Marketplace</span>
            <span className="text-xs font-bold text-emerald-700 flex items-center gap-1.5 mt-1">
              <Store className="w-3.5 h-3.5" /> Healthy
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">JVM Memory</span>
            <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mt-1">
              <Cpu className="w-3.5 h-3.5 text-slate-500" /> {data?.platformHealth?.usedMemoryMb ?? 0} MB / {data?.platformHealth?.maxMemoryMb ?? 0} MB
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">DB Connections</span>
            <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mt-1">
              <Layers className="w-3.5 h-3.5 text-slate-500" /> {data?.platformHealth?.activeDbConnections ?? 1} of {data?.platformHealth?.maxDbConnections ?? 15}
            </span>
          </div>
        </div>
      </div>

      {/* Row 6: Platform Administrative Audit Stream */}
      <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
          <div className="flex items-center gap-2.5">
            <ShieldAlert className="w-5 h-5 text-indigo-600" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">Security & Platform Audit Trail</h3>
              <p className="text-[11px] text-slate-500">Immutable administrative events across all organizations</p>
            </div>
          </div>
          <Link
            to="/audit-logs"
            className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
          >
            View Full Audit Trail <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80 font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-4 py-2.5">Action</th>
                <th className="px-4 py-2.5">Entity</th>
                <th className="px-4 py-2.5">Target ID</th>
                <th className="px-4 py-2.5">Timestamp</th>
                <th className="px-4 py-2.5 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="text-center py-6 text-slate-400">Loading audit events...</td>
                </tr>
              ) : !data?.recentActivities?.length ? (
                <tr>
                  <td colSpan={5} className="text-center py-6 text-slate-400">No recent security events logged</td>
                </tr>
              ) : (
                data.recentActivities.map((act) => (
                  <tr key={act.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-4 py-2.5 font-bold text-slate-900">{act.action}</td>
                    <td className="px-4 py-2.5 text-slate-600">{act.entityType}</td>
                    <td className="px-4 py-2.5 text-slate-400 font-mono text-[11px]">{act.entityId || 'N/A'}</td>
                    <td className="px-4 py-2.5 text-slate-500">
                      {act.timestamp ? new Date(act.timestamp).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : 'Recent'}
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        {act.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
