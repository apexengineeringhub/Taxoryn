import React, { useEffect, useState } from 'react';
import {
  CreditCard,
  TrendingUp,
  CheckCircle2,
  Sparkles,
  ShieldCheck,
  RefreshCw,
  Zap,
  Building2,
  ArrowRight,
  Layers,
  Clock,
} from 'lucide-react';
import { platformDashboardApi, adminPracticeApi } from '../api/endpoints';
import { PlatformDashboardSummary, Organization } from '../types';
import { Button } from '../components/common/Button';
import { WorkspacePageHeader } from '../components/layout/WorkspacePageHeader';
import clsx from 'clsx';

export const PlatformSubscriptionsPage: React.FC = () => {
  const [data, setData] = useState<PlatformDashboardSummary | null>(null);
  const [practices, setPractices] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadSubscriptions();
  }, []);

  const loadSubscriptions = async () => {
    try {
      setIsLoading(true);
      const [summary, practiceRes] = await Promise.all([
        platformDashboardApi.getOverview(),
        adminPracticeApi.getPractices({ size: 100 }),
      ]);
      setData(summary);
      setPractices(practiceRes?.content || []);
    } catch (err) {
      console.error('Failed to load platform subscriptions', err);
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

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Header */}
      <WorkspacePageHeader
        sectionBadge="SaaS Billing Operations"
        sectionBadgeStyle="bg-emerald-100 text-emerald-800 border-emerald-200"
        title="Platform Subscriptions & SaaS Revenue"
        titleIcon={CreditCard}
        titleIconColor="text-emerald-600"
        description="Manage practice subscription tiers, monitor MRR/ARR, and track platform recurring revenue."
      >
        <Button variant="secondary" onClick={loadSubscriptions} disabled={isLoading} className="text-xs gap-1.5 font-bold shadow-2xs">
          <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
        </Button>
      </WorkspacePageHeader>

      {/* Revenue Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-gradient-to-br from-indigo-900 to-indigo-950 text-white rounded-xl p-5 shadow-md">
          <span className="text-xs font-bold text-indigo-200 uppercase tracking-wider block">Monthly Recurring Revenue (MRR)</span>
          <p className="text-3xl font-black mt-2">
            {formatCurrency(data?.subscriptionMetrics?.estimatedMrr)}
          </p>
          <span className="text-xs text-indigo-300 mt-2 block">
            Across {data?.subscriptionMetrics?.activeTiers ?? 0} active practice tenants
          </span>
        </div>

        <div className="bg-gradient-to-br from-purple-900 to-purple-950 text-white rounded-xl p-5 shadow-md">
          <span className="text-xs font-bold text-purple-200 uppercase tracking-wider block">Annual Recurring Revenue (ARR)</span>
          <p className="text-3xl font-black mt-2">
            {formatCurrency(data?.subscriptionMetrics?.estimatedArr)}
          </p>
          <span className="text-xs text-purple-300 mt-2 block">
            Run-rate based on active SaaS plans
          </span>
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block">Subscribed Practices</span>
          <p className="text-3xl font-black text-slate-900 mt-2">
            {data?.subscriptionMetrics?.totalSubscriptions ?? 0}
          </p>
          <div className="flex items-center gap-2 mt-2 text-xs font-semibold text-emerald-600">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>{data?.subscriptionMetrics?.activeTiers ?? 0} active subscriptions</span>
          </div>
        </div>
      </div>

      {/* Platform Subscription Tier Catalog */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs">
        <h3 className="text-base font-bold text-slate-900 mb-1">Taxoryn SaaS Plan Catalog</h3>
        <p className="text-xs text-slate-500 mb-6">Tier quotas, features, and active adoption across tax practices</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Starter */}
          <div className="border border-slate-200 rounded-xl p-4 bg-slate-50/50 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-slate-600">STARTER</span>
                <span className="text-xs font-bold bg-slate-200 text-slate-800 px-2 py-0.5 rounded-full">
                  {data?.subscriptionMetrics?.starterTiers ?? 0} practices
                </span>
              </div>
              <div className="mt-3">
                <span className="text-2xl font-black text-slate-900">₹999</span>
                <span className="text-xs text-slate-500"> / month</span>
              </div>
              <ul className="mt-4 space-y-2 text-xs text-slate-600">
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> Up to 3 Team Members</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> Up to 50 Clients</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> GST, ITR, TDS Workflows</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> 5 GB Cloud Vault</li>
              </ul>
            </div>
          </div>

          {/* Professional */}
          <div className="border border-blue-200 rounded-xl p-4 bg-blue-50/40 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-blue-800">PROFESSIONAL</span>
                <span className="text-xs font-bold bg-blue-200 text-blue-900 px-2 py-0.5 rounded-full">
                  {data?.subscriptionMetrics?.professionalTiers ?? 0} practices
                </span>
              </div>
              <div className="mt-3">
                <span className="text-2xl font-black text-blue-950">₹2,999</span>
                <span className="text-xs text-blue-600"> / month</span>
              </div>
              <ul className="mt-4 space-y-2 text-xs text-slate-700">
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-blue-600 shrink-0" /> Up to 10 Team Members</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-blue-600 shrink-0" /> Up to 250 Clients</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-blue-600 shrink-0" /> Marketplace Verified Badge</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-blue-600 shrink-0" /> 25 GB Cloud Vault</li>
              </ul>
            </div>
          </div>

          {/* Business */}
          <div className="border border-purple-200 rounded-xl p-4 bg-purple-50/40 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-purple-800">BUSINESS</span>
                <span className="text-xs font-bold bg-purple-200 text-purple-900 px-2 py-0.5 rounded-full">
                  {data?.subscriptionMetrics?.businessTiers ?? 0} practices
                </span>
              </div>
              <div className="mt-3">
                <span className="text-2xl font-black text-purple-950">₹5,999</span>
                <span className="text-xs text-purple-600"> / month</span>
              </div>
              <ul className="mt-4 space-y-2 text-xs text-slate-700">
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-600 shrink-0" /> Up to 30 Team Members</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-600 shrink-0" /> Up to 1,000 Clients</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-600 shrink-0" /> Custom Practice Branding</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-600 shrink-0" /> 100 GB Cloud Vault</li>
              </ul>
            </div>
          </div>

          {/* Enterprise */}
          <div className="border border-indigo-300 rounded-xl p-4 bg-indigo-50/60 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-indigo-900 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-600" /> ENTERPRISE
                </span>
                <span className="text-xs font-bold bg-indigo-200 text-indigo-950 px-2 py-0.5 rounded-full">
                  {data?.subscriptionMetrics?.enterpriseTiers ?? 0} practices
                </span>
              </div>
              <div className="mt-3">
                <span className="text-2xl font-black text-indigo-950">₹14,999</span>
                <span className="text-xs text-indigo-600"> / month</span>
              </div>
              <ul className="mt-4 space-y-2 text-xs text-slate-800">
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-indigo-600 shrink-0" /> Unlimited Team & Clients</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-indigo-600 shrink-0" /> Dedicated Account Manager</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-indigo-600 shrink-0" /> Priority Marketplace Showcase</li>
                <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-indigo-600 shrink-0" /> 1 TB High-Speed Storage</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Tenant Subscriptions Table */}
      <div className="bg-white border border-slate-200 rounded-xl shadow-xs overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-900">Tenant Practice Subscription Registry</h3>
          <span className="text-xs text-slate-500">{practices.length} Practices Enrolled</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3">Practice Name</th>
                <th className="px-4 py-3">Active Tier</th>
                <th className="px-4 py-3">Billing Cycle</th>
                <th className="px-4 py-3">Account Status</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {practices.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50/60 transition-colors">
                  <td className="px-5 py-3.5 font-bold text-slate-900">{p.name}</td>
                  <td className="px-4 py-3.5">
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 uppercase">
                      {p.subscriptionPlan || 'STARTER'}
                    </span>
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">Monthly Auto-Renewal</td>
                  <td className="px-4 py-3.5">
                    <span className={clsx(
                      'px-2.5 py-0.5 rounded-full text-[10px] font-bold border',
                      p.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-rose-50 text-rose-700 border-rose-200'
                    )}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-4 py-3.5 text-slate-500">
                    {p.createdAt ? new Date(p.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' }) : 'N/A'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
