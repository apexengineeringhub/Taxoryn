import React, { useState, useEffect } from 'react';
import { CreditCard, Check, Sparkles, ShieldCheck, Zap } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { subscriptionApi } from '../api/endpoints';
import { SubscriptionPlan, SubscriptionInfo } from '../types';
import { useAuth } from '../context/AuthContext';
import { formatPlanDisplayName, formatPlanShortName } from '../utils/planUtils';
import clsx from 'clsx';

const DEFAULT_PLAN_CATALOG: SubscriptionPlan[] = [
  {
    plan: 'STARTER',
    code: 'STARTER',
    name: 'Starter Practice',
    description: 'Essential compliance and client management for solo practitioners and boutique tax consultants',
    monthlyPrice: 999,
    yearlyPrice: 9990,
    maxClients: 25,
    maxUsers: 5,
    formattedStorage: '5 GB',
    features: [
      'Up to 5 team members',
      'Up to 25 active clients',
      '5 GB document vault storage',
      'GST & ITR return tracking',
      'Compliance calendar & reminders',
      'Client portal self-service',
    ],
    isPopular: false,
  },
  {
    plan: 'PROFESSIONAL',
    code: 'PROFESSIONAL',
    name: 'Professional Practice',
    description: 'Comprehensive tax practice management with multi-service invoicing and staff workload balancing',
    monthlyPrice: 2499,
    yearlyPrice: 24990,
    maxClients: 100,
    maxUsers: 15,
    formattedStorage: '25 GB',
    features: [
      'Up to 15 team members',
      'Up to 100 active clients',
      '25 GB document vault storage',
      'Full GST (GSTR-1, 3B, 9) & ITR lifecycle',
      'Client billing & payment receipts',
      'Granular RBAC role delegation',
      'Priority email & chat support',
    ],
    isPopular: true,
  },
  {
    plan: 'BUSINESS',
    code: 'BUSINESS',
    name: 'Business Firm',
    description: 'High-volume operations for growing multi-partner CA firms and corporate compliance departments',
    monthlyPrice: 4999,
    yearlyPrice: 49990,
    maxClients: 500,
    maxUsers: 50,
    formattedStorage: '100 GB',
    features: [
      'Up to 50 team members',
      'Up to 500 active clients',
      '100 GB document vault storage',
      'Executive analytics & partner dashboards',
      'Automated batch compliance generator',
      'Custom role creation & audit logging',
      'Dedicated practice success manager',
    ],
    isPopular: false,
  },
  {
    plan: 'ENTERPRISE',
    code: 'ENTERPRISE',
    name: 'Enterprise Organization',
    description: 'Full enterprise platform with custom integrations, priority SLAs, and maximum quotas',
    monthlyPrice: 9999,
    yearlyPrice: 99990,
    maxClients: 2500,
    maxUsers: 250,
    formattedStorage: '500 GB',
    features: [
      'Up to 250 team members',
      'Up to 2,500 active clients',
      '500 GB document vault storage',
      'Unlimited bulk document processing',
      'Multi-branch organizational governance',
      'Custom API & ERP integrations',
      '99.9% uptime SLA & 24/7 phone support',
    ],
    isPopular: false,
  },
];

export const SubscriptionsPage: React.FC = () => {
  const [plans, setPlans] = useState<SubscriptionPlan[]>(DEFAULT_PLAN_CATALOG);
  const [subscription, setSubscription] = useState<SubscriptionInfo | null>(null);
  const [interval, setInterval] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const { updateSubscriptionPlan, refreshOrganization } = useAuth();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [plansRes, currentRes] = await Promise.all([
        subscriptionApi.getPlans(),
        subscriptionApi.getCurrent(),
      ]);
      if (plansRes && plansRes.length > 0) {
        setPlans(plansRes);
      }
      setSubscription(currentRes || null);
    } catch (err) {
      console.error('Failed to load subscription info', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePlanChange = async (planCode: string) => {
    if (!planCode || isUpdating) return;
    setIsUpdating(true);
    try {
      const billingInterval = interval === 'ANNUAL' ? 'YEARLY' : 'MONTHLY';
      const updated = await subscriptionApi.changePlan({ plan: planCode, interval: billingInterval });
      
      // Immediately synchronize global AuthContext and Sidebar state
      updateSubscriptionPlan(planCode);
      if (updated) {
        setSubscription(updated);
      }
      alert(`Subscription successfully changed to ${formatPlanDisplayName(planCode)}!`);
      await Promise.all([loadData(), refreshOrganization()]);
    } catch (err: any) {
      alert(`Plan upgrade failed: ${err?.response?.data?.message || err.message || 'Please try again'}`);
    } finally {
      setIsUpdating(false);
    }
  };

  const displayPlans = plans.length > 0 ? plans : DEFAULT_PLAN_CATALOG;

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in pb-12">
      {/* Header */}
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-black tracking-tight text-slate-900">SaaS Practice Subscriptions & Limits</h1>
        <p className="text-xs text-slate-500 max-w-xl mx-auto">
          Scale your tax practice with transparent client quotas, multi-user CA staff accounts, and unlimited filing workflows.
        </p>

        {/* Interval Switcher */}
        <div className="pt-4 inline-flex items-center gap-2 p-1 bg-slate-100 border border-slate-200 rounded-xl">
          <button
            onClick={() => setInterval('MONTHLY')}
            className={clsx(
              'px-4 py-1.5 text-xs font-bold rounded-lg transition-all',
              interval === 'MONTHLY' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            Monthly Billing
          </button>
          <button
            onClick={() => setInterval('ANNUAL')}
            className={clsx(
              'px-4 py-1.5 text-xs font-bold rounded-lg transition-all flex items-center gap-1.5',
              interval === 'ANNUAL' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <span>Annual Billing</span>
            <span className="text-[10px] font-bold px-1.5 py-0.5 bg-emerald-100 text-emerald-700 rounded-full">Save 20%</span>
          </button>
        </div>
      </div>

      {/* Plan Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {displayPlans.map((plan) => {
          const planCode = (plan.plan || plan.code || 'STARTER') as string;
          const isCurrent = (subscription?.plan || '').toUpperCase() === planCode.toUpperCase();
          const baseMonthly = plan.monthlyPrice || 999;
          const baseYearly = plan.yearlyPrice || baseMonthly * 10;
          const displayPrice = interval === 'ANNUAL' ? Math.round(baseYearly / 12) : baseMonthly;
          const isPopular = plan.isPopular ?? plan.popular ?? (planCode === 'PROFESSIONAL');

          return (
            <div
              key={planCode}
              className={clsx(
                'bg-white rounded-2xl border p-5 flex flex-col justify-between shadow-card relative transition-all',
                isPopular ? 'border-brand-500 ring-2 ring-brand-500/20' : 'border-slate-200/90',
                isCurrent && 'bg-slate-50/70 border-emerald-300 ring-2 ring-emerald-500/20'
              )}
            >
              {isCurrent ? (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-emerald-600 text-white text-[10px] font-bold uppercase tracking-wider px-3 py-0.5 rounded-full shadow-sm">
                  Active Plan
                </span>
              ) : isPopular ? (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-brand-600 text-white text-[10px] font-bold uppercase tracking-wider px-3 py-0.5 rounded-full shadow-sm">
                  Most Popular
                </span>
              ) : null}

              <div>
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-extrabold text-base text-slate-900">{plan.name || formatPlanDisplayName(planCode)}</h3>
                    <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2">{plan.description || 'Practice management tier'}</p>
                  </div>
                </div>

                <div className="mt-4 mb-5">
                  <span className="text-2xl sm:text-3xl font-black text-slate-900">₹{displayPrice.toLocaleString('en-IN')}</span>
                  <span className="text-xs text-slate-400 font-medium"> / month</span>
                  {interval === 'ANNUAL' && (
                    <span className="block text-[10px] text-emerald-600 font-semibold mt-0.5">Billed ₹{baseYearly.toLocaleString('en-IN')} annually</span>
                  )}
                </div>

                <ul className="space-y-2 text-xs text-slate-600 border-t border-slate-100 pt-4">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Up to <strong>{plan.maxClients} Active Clients</strong></span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Up to <strong>{plan.maxUsers} Staff Users & CAs</strong></span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span><strong>{plan.formattedStorage || `${plan.maxStorageGb || 5} GB`} Vault Storage</strong></span>
                  </li>
                  {plan.features?.slice(3).map((f, idx) => (
                    <li key={idx} className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                      <span>{f}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="mt-6">
                <Button
                  variant={isCurrent ? 'outline' : isPopular ? 'primary' : 'secondary'}
                  className="w-full"
                  disabled={isCurrent || isUpdating}
                  isLoading={isUpdating && !isCurrent}
                  onClick={() => handlePlanChange(planCode)}
                >
                  {isCurrent ? 'Current Active Plan' : 'Switch to Plan'}
                </Button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
