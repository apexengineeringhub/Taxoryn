import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Headphones,
  LifeBuoy,
  AlertTriangle,
  Clock,
  CheckCircle2,
  RefreshCw,
  Search,
  ArrowUpRight,
  MessageSquare,
  ShieldAlert,
  Users,
  Building2,
  ExternalLink,
  HelpCircle,
  TrendingUp,
} from 'lucide-react';
import { supportDashboardApi } from '../api/endpoints';
import { SupportDashboardSummary } from '../types';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import clsx from 'clsx';

export const SupportOverviewPage: React.FC = () => {
  const [data, setData] = useState<SupportDashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadDashboard = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const res = await supportDashboardApi.getOverview();
      setData(res);
    } catch (err: any) {
      console.error('Failed to load support dashboard', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to retrieve support metrics.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const kpis = data?.kpis;
  const attentionItems = data?.supportAttention || [];
  const recentActivity = data?.recentActivity || [];

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* ========================================================================= */}
      {/* 1. Header                                                                 */}
      {/* ========================================================================= */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-card">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-blue-100 text-blue-800 border border-blue-200">
              Taxoryn Support
            </span>
            <span className="text-xs text-slate-400">•</span>
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              PLATFORM SUPPORT WORKSPACE
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 flex items-center gap-2.5">
            <Headphones className="w-8 h-8 text-blue-600" />
            Support Overview & Ticket Triage
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Monitor open platform feedback, customer service tickets, SLA resolution rates, and escalated technical inquiries.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={loadDashboard}
            disabled={isLoading}
            className="text-xs gap-1.5 shadow-2xs font-bold"
          >
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
          </Button>
          <Link to="/admin/feedback">
            <Button
              variant="primary"
              className="text-xs gap-1.5 bg-blue-600 hover:bg-blue-700 text-white shadow-xs font-bold"
            >
              <LifeBuoy className="w-4 h-4" /> Feedback Operations
            </Button>
          </Link>
        </div>
      </div>

      {/* Error Alert */}
      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs font-semibold flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 2. Core Support KPI Cards (4 Cards)                                       */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Open Cases */}
        <Link
          to="/admin/feedback"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-blue-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-blue-600 transition-colors">
              Open Support Cases
            </span>
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-all">
              <LifeBuoy className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-slate-900">
              {isLoading ? '...' : kpis?.openCases ?? 0}
            </span>
            <span className="text-xs font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200">
              Active Queue
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Resolved This Month:</span>
            <span className="font-bold text-emerald-600">{kpis?.resolvedThisMonth ?? 0} closed</span>
          </div>
        </Link>

        {/* Card 2: Waiting for Customer */}
        <Link
          to="/admin/feedback"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-amber-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-amber-600 transition-colors">
              Waiting for Customer
            </span>
            <div className="w-9 h-9 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:bg-amber-600 group-hover:text-white transition-all">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-amber-600">
              {isLoading ? '...' : kpis?.waitingForCustomer ?? 0}
            </span>
            <span className="text-xs font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
              Pending Reply
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Awaiting input:</span>
            <span className="font-semibold text-slate-700">Follow-up needed</span>
          </div>
        </Link>

        {/* Card 3: High Priority */}
        <Link
          to="/admin/feedback"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-rose-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-rose-600 transition-colors">
              High Priority
            </span>
            <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center group-hover:bg-rose-600 group-hover:text-white transition-all">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-rose-600">
              {isLoading ? '...' : kpis?.highPriority ?? 0}
            </span>
            <span className="text-xs font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
              Critical SLA
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Requires rapid action:</span>
            <span className="font-bold text-rose-600">&lt; 4h SLA Target</span>
          </div>
        </Link>

        {/* Card 4: Unresolved Feedback */}
        <Link
          to="/admin/feedback"
          className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card hover:shadow-card-hover hover:border-purple-300 transition-all group block"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider group-hover:text-purple-600 transition-colors">
              Unresolved Feedback
            </span>
            <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-all">
              <MessageSquare className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <span className="text-3xl font-black text-purple-600">
              {isLoading ? '...' : kpis?.unresolvedFeedback ?? 0}
            </span>
            <span className="text-xs font-bold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-full border border-purple-200">
              Triage Queue
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>Product improvements:</span>
            <span className="font-semibold text-slate-700">Open for triage</span>
          </div>
        </Link>
      </div>

      {/* ========================================================================= */}
      {/* 3. Support Attention Section & Quick Actions                              */}
      {/* ========================================================================= */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Support Attention */}
        <Card
          title="Support Attention"
          subtitle="Actionable items requiring priority investigation, customer follow-up, or engineering escalation."
          className="lg:col-span-2 border-slate-200/90"
        >
          <div className="space-y-3">
            {isLoading ? (
              <div className="text-center py-8 text-slate-400">Loading support queue items...</div>
            ) : attentionItems.length === 0 ? (
              <div className="text-center py-8 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
                <p className="text-xs font-bold text-slate-700">All Support Queues Clear</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  No high-priority blockers or SLA-breaching cases waiting for triage.
                </p>
              </div>
            ) : (
              attentionItems.map((item, idx) => (
                <div
                  key={item.id || idx}
                  className="flex items-center justify-between p-3.5 rounded-xl border transition-all hover:bg-slate-50/80 bg-white border-slate-200"
                >
                  <div className="flex items-start gap-3">
                    <div className={clsx(
                      'w-8 h-8 rounded-lg flex items-center justify-center shrink-0 mt-0.5 text-xs font-bold',
                      item.priority === 'CRITICAL' || item.priority === 'HIGH'
                        ? 'bg-rose-100 text-rose-700'
                        : 'bg-amber-100 text-amber-700'
                    )}>
                      <AlertTriangle className="w-4 h-4" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-900">{item.title}</p>
                      <p className="text-[11px] text-slate-500 mt-0.5">{item.description}</p>
                    </div>
                  </div>
                  <Link
                    to={item.actionTarget || '/admin/feedback'}
                    className="px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 font-bold text-xs inline-flex items-center gap-1 shrink-0 ml-3 transition-colors"
                  >
                    <span>{item.actionLabel || 'Review →'}</span>
                  </Link>
                </div>
              ))
            )}
          </div>
        </Card>

        {/* Quick Support Lookup Hub */}
        <Card
          title="Support Directory & Lookups"
          subtitle="Non-tax metadata lookups for customer and practice inquiries."
          className="lg:col-span-1 border-slate-200/90"
        >
          <div className="space-y-3">
            <Link
              to="/admin/practices"
              className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-slate-50/60 hover:bg-white hover:border-blue-300 transition-all group"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center group-hover:bg-blue-600 group-hover:text-white transition-colors">
                  <Building2 className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">Practice Directory</p>
                  <p className="text-[10px] text-slate-500">Lookup firm phone, email & status</p>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600" />
            </Link>

            <Link
              to="/admin/feedback"
              className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-slate-50/60 hover:bg-white hover:border-purple-300 transition-all group"
            >
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center group-hover:bg-purple-600 group-hover:text-white transition-colors">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">Feedback Response Hub</p>
                  <p className="text-[10px] text-slate-500">Post resolution notes & updates</p>
                </div>
              </div>
              <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:text-purple-600" />
            </Link>

            <div className="p-3 bg-blue-50/60 border border-blue-200 rounded-xl">
              <div className="flex items-center gap-2 mb-1">
                <HelpCircle className="w-4 h-4 text-blue-600 shrink-0" />
                <span className="text-xs font-bold text-blue-900">Support Privacy Policy</span>
              </div>
              <p className="text-[11px] text-blue-700 leading-relaxed">
                Platform Support administrators have metadata-only access. Direct customer PAN, Aadhaar, ITR returns, GST computations, and bank accounts remain encrypted and inaccessible.
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* ========================================================================= */}
      {/* 4. Recent Support Activity                                                */}
      {/* ========================================================================= */}
      <Card
        title="Recent Support Activity"
        subtitle="Chronological log of customer inquiries, ticket status changes, and communication events."
        noPadding
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/80 font-bold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3.5">Activity & Event</th>
                <th className="px-4 py-3.5">Category / Feature</th>
                <th className="px-4 py-3.5">Actor</th>
                <th className="px-4 py-3.5">Status</th>
                <th className="px-4 py-3.5">Timestamp</th>
                <th className="px-5 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-400 font-medium">
                    Loading recent support activity...
                  </td>
                </tr>
              ) : recentActivity.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-400 font-medium">
                    No recent support activities recorded.
                  </td>
                </tr>
              ) : (
                recentActivity.map((act, idx) => (
                  <tr key={act.id || idx} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-5 py-3.5">
                      <p className="font-bold text-slate-900">{act.title}</p>
                      <p className="text-[11px] text-slate-500 truncate max-w-xs">{act.description}</p>
                    </td>
                    <td className="px-4 py-3.5 font-medium text-slate-700">{act.target}</td>
                    <td className="px-4 py-3.5 text-slate-600 font-mono text-[11px]">{act.actor}</td>
                    <td className="px-4 py-3.5">
                      <span className={clsx(
                        'px-2.5 py-0.5 rounded-full text-[10px] font-bold border inline-flex items-center gap-1',
                        act.status === 'RESOLVED' || act.status === 'CLOSED'
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : act.status === 'UNDER_REVIEW' || act.status === 'IN_PROGRESS'
                          ? 'bg-amber-50 text-amber-700 border-amber-200'
                          : 'bg-blue-50 text-blue-700 border-blue-200'
                      )}>
                        {act.status}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-slate-500 font-medium whitespace-nowrap">
                      {act.timestamp ? new Date(act.timestamp).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : 'Recent'}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Link
                        to={act.navigationTarget || '/admin/feedback'}
                        className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-bold inline-flex items-center gap-1 transition-colors"
                      >
                        Details <ExternalLink className="w-3 h-3 text-slate-400" />
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};
