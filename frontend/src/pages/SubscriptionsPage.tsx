import React, { useState, useEffect } from 'react';
import {
  CreditCard,
  Check,
  Sparkles,
  ShieldCheck,
  Zap,
  Users,
  UserCheck,
  HardDrive,
  MapPin,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  Building2,
  Mail,
  Phone,
  ArrowRight,
  X,
  HelpCircle,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { subscriptionApi } from '../api/endpoints';
import { SubscriptionPlan, SubscriptionInfo, SubscriptionEntitlementsResponse, EntitlementResult, EnterpriseInquiryRequest } from '../types';
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
    maxUsers: 1,
    formattedStorage: '5 GB',
    features: [
      'Up to 1 team member',
      'Up to 25 active clients',
      '5 GB document vault storage',
      '1 Primary office location',
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
      'Up to 3 branch locations',
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
      'Up to 10 branch locations',
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
      'Unlimited branch locations',
      'Unlimited bulk document processing',
      'Multi-branch organizational governance',
      'Custom API & ERP integrations',
      '99.9% uptime SLA & 24/7 phone support',
    ],
    isPopular: false,
  },
];

const PLAN_RANKS: Record<string, number> = {
  STARTER: 1,
  PROFESSIONAL: 2,
  BUSINESS: 3,
  ENTERPRISE: 4,
};

export const SubscriptionsPage: React.FC = () => {
  const [plans, setPlans] = useState<SubscriptionPlan[]>(DEFAULT_PLAN_CATALOG);
  const [subscription, setSubscription] = useState<SubscriptionInfo | null>(null);
  const [entitlements, setEntitlements] = useState<SubscriptionEntitlementsResponse | null>(null);
  const [interval, setInterval] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  
  // Confirmation Modal state
  const [targetPlanToChange, setTargetPlanToChange] = useState<SubscriptionPlan | null>(null);
  const [downgradeError, setDowngradeError] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Enterprise Inquiry Modal state
  const [isEnterpriseModalOpen, setIsEnterpriseModalOpen] = useState(false);
  const [enterpriseForm, setEnterpriseForm] = useState<EnterpriseInquiryRequest>({
    contactName: '',
    contactEmail: '',
    contactPhone: '',
    firmName: '',
    estimatedTeamSize: 50,
    estimatedClientCount: 500,
    requirements: '',
  });
  const [isSubmittingEnterprise, setIsSubmittingEnterprise] = useState(false);

  const { user, organization, updateSubscriptionPlan, refreshOrganization } = useAuth();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [plansRes, currentRes, entitlementsRes] = await Promise.all([
        subscriptionApi.getPlans().catch(() => DEFAULT_PLAN_CATALOG),
        subscriptionApi.getCurrent().catch(() => null),
        subscriptionApi.getEntitlements().catch(() => null),
      ]);
      if (plansRes && plansRes.length > 0) {
        setPlans(plansRes);
      }
      setSubscription(currentRes || null);
      setEntitlements(entitlementsRes || null);
    } catch (err) {
      console.error('Failed to load subscription info', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenPlanModal = (plan: SubscriptionPlan) => {
    const planCode = (plan.plan || plan.code || 'STARTER').toUpperCase();
    if (planCode === 'ENTERPRISE') {
      const contactFullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
      setEnterpriseForm({
        contactName: contactFullName || '',
        contactEmail: user?.email || '',
        contactPhone: user?.phone || '',
        firmName: organization?.name || '',
        estimatedTeamSize: 50,
        estimatedClientCount: 500,
        requirements: '',
      });
      setIsEnterpriseModalOpen(true);
      return;
    }

    setDowngradeError(null);
    setTargetPlanToChange(plan);
  };

  const handleConfirmPlanChange = async () => {
    if (!targetPlanToChange || isUpdating) return;
    const planCode = (targetPlanToChange.plan || targetPlanToChange.code || 'STARTER').toUpperCase();
    setIsUpdating(true);
    setDowngradeError(null);

    try {
      const billingInterval = interval === 'ANNUAL' ? 'YEARLY' : 'MONTHLY';
      const updated = await subscriptionApi.changePlan({ plan: planCode, interval: billingInterval });

      updateSubscriptionPlan(planCode);
      if (updated) {
        setSubscription(updated);
      }
      setSuccessBanner(`Plan successfully updated to ${formatPlanDisplayName(planCode)}!`);
      setTimeout(() => setSuccessBanner(null), 6000);
      setTargetPlanToChange(null);
      await Promise.all([loadData(), refreshOrganization()]);
    } catch (err: any) {
      const errorMsg = err?.response?.data?.message || err?.message || 'Plan update could not be completed.';
      setDowngradeError(errorMsg);
    } finally {
      setIsUpdating(false);
    }
  };

  const handleSubmitEnterpriseInquiry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!enterpriseForm.contactName.trim() || !enterpriseForm.contactEmail.trim()) {
      return;
    }

    setIsSubmittingEnterprise(true);
    try {
      await subscriptionApi.submitEnterpriseInquiry(enterpriseForm);
      setIsEnterpriseModalOpen(false);
      setSuccessBanner('Enterprise inquiry submitted successfully! Our solutions team will contact you within 1 business day.');
      setTimeout(() => setSuccessBanner(null), 7000);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to submit enterprise inquiry. Please try again.');
    } finally {
      setIsSubmittingEnterprise(false);
    }
  };

  const displayPlans = plans.length > 0 ? plans : DEFAULT_PLAN_CATALOG;
  const currentPlanCode = (subscription?.plan || entitlements?.plan || 'STARTER').toUpperCase();
  const currentRank = PLAN_RANKS[currentPlanCode] || 1;

  const getResourceIcon = (type: string) => {
    switch (type) {
      case 'TEAM_MEMBER':
        return Users;
      case 'CLIENT':
        return UserCheck;
      case 'STORAGE':
        return HardDrive;
      case 'LOCATION':
        return MapPin;
      default:
        return Zap;
    }
  };

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in pb-12">
      {/* Header */}
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-black tracking-tight text-slate-900">SaaS Practice Subscriptions & Limits</h1>
        <p className="text-xs text-slate-500 max-w-xl mx-auto">
          Scale your tax practice with transparent client quotas, multi-user CA staff accounts, and regional branch management.
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

      {/* Success Notification Banner */}
      {successBanner && (
        <div className="p-4 rounded-xl text-xs font-bold bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-center gap-2.5 shadow-xs animate-fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{successBanner}</span>
        </div>
      )}

      {/* Live Entitlements & Usage Quotas Dashboard */}
      {entitlements && entitlements.entitlements && (
        <div className="bg-white border border-slate-200/90 rounded-2xl p-6 shadow-card space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-slate-900">Current Plan Usage & Quotas</h2>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-extrabold bg-brand-50 text-brand-700 border border-brand-200">
                  {formatPlanDisplayName(entitlements.plan)}
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 uppercase">
                  {entitlements.status || 'ACTIVE'}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Real-time resource consumption against your subscribed tier limits
              </p>
            </div>

            {entitlements.anyLimitReached ? (
              <div className="flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>Limit Reached — Upgrade to add more resources</span>
              </div>
            ) : entitlements.anyWarning ? (
              <div className="flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 bg-amber-50 border border-amber-200 text-amber-700 rounded-xl">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Approaching Limit (80%+)</span>
              </div>
            ) : null}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {entitlements.entitlements.map((res) => {
              const Icon = getResourceIcon(res.resourceType);
              const isBlocked = !res.allowed;
              const isWarn = res.warning && !isBlocked;

              return (
                <div
                  key={res.resourceType}
                  className={clsx(
                    'rounded-xl border p-4 transition-all flex flex-col justify-between space-y-3',
                    isBlocked
                      ? 'bg-rose-50/40 border-rose-200 ring-1 ring-rose-300/40'
                      : isWarn
                      ? 'bg-amber-50/30 border-amber-200 ring-1 ring-amber-300/40'
                      : 'bg-slate-50/50 border-slate-200/80'
                  )}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className={clsx(
                        'p-2 rounded-lg',
                        isBlocked ? 'bg-rose-100 text-rose-700' : isWarn ? 'bg-amber-100 text-amber-700' : 'bg-slate-200/80 text-slate-700'
                      )}>
                        <Icon className="w-4 h-4" />
                      </div>
                      <span className="text-xs font-bold text-slate-900">
                        {res.resourceType === 'TEAM_MEMBER' ? 'Team Members' :
                         res.resourceType === 'CLIENT' ? 'Active Clients' :
                         res.resourceType === 'STORAGE' ? 'Document Vault' : 'Branch Locations'}
                      </span>
                    </div>

                    <span className={clsx(
                      'text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase',
                      isBlocked ? 'bg-rose-100 text-rose-800' : isWarn ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                    )}>
                      {isBlocked ? '100% Full' : `${Math.round(res.percentageUsed)}%`}
                    </span>
                  </div>

                  <div className="space-y-1.5">
                    <div className="flex justify-between items-baseline text-xs">
                      <span className="font-extrabold text-slate-900">{res.formattedUsage}</span>
                      <span className="text-[11px] text-slate-400">
                        {res.unlimited ? 'Unlimited' : `${res.limit} allowed`}
                      </span>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                      <div
                        className={clsx(
                          'h-full rounded-full transition-all duration-500',
                          isBlocked ? 'bg-rose-600' : isWarn ? 'bg-amber-500' : 'bg-emerald-500'
                        )}
                        style={{ width: `${Math.min(100, Math.max(0, res.percentageUsed))}%` }}
                      />
                    </div>
                  </div>

                  <p className={clsx(
                    'text-[11px] font-medium leading-tight',
                    isBlocked ? 'text-rose-700 font-semibold' : isWarn ? 'text-amber-700 font-semibold' : 'text-slate-500'
                  )}>
                    {res.message}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Plan Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {displayPlans.map((plan) => {
          const planCode = (plan.plan || plan.code || 'STARTER').toUpperCase();
          const targetRank = PLAN_RANKS[planCode] || 1;
          const isCurrent = currentPlanCode === planCode;
          const isUpgrade = targetRank > currentRank;
          const isDowngrade = targetRank < currentRank;
          const isEnterprise = planCode === 'ENTERPRISE';

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
                  variant={isCurrent ? 'outline' : isEnterprise ? 'primary' : isUpgrade ? 'primary' : 'secondary'}
                  className="w-full"
                  disabled={isCurrent || isUpdating}
                  onClick={() => handleOpenPlanModal(plan)}
                >
                  {isCurrent
                    ? 'Current Active Plan'
                    : isEnterprise
                    ? 'Contact Enterprise Sales'
                    : isUpgrade
                    ? 'Upgrade Plan'
                    : 'Downgrade Plan'}
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Plan Upgrade / Downgrade Confirmation Modal */}
      {targetPlanToChange && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 border border-slate-100 space-y-5 animate-scale-up">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className={clsx(
                  'p-2 rounded-xl',
                  (PLAN_RANKS[targetPlanToChange.code || ''] || 1) > currentRank
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-amber-50 text-amber-700'
                )}>
                  <CreditCard className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-extrabold text-slate-900 text-sm">
                    {(PLAN_RANKS[targetPlanToChange.code || ''] || 1) > currentRank ? 'Confirm Plan Upgrade' : 'Confirm Plan Downgrade'}
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Switching to {targetPlanToChange.name} ({interval === 'ANNUAL' ? 'Annual' : 'Monthly'})
                  </p>
                </div>
              </div>

              <button
                onClick={() => setTargetPlanToChange(null)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Downgrade Limit Error / Protection Alert */}
            {downgradeError && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 space-y-1.5 animate-fade-in">
                <div className="flex items-center gap-1.5 font-bold text-rose-900">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>Downgrade Blocked (Usage Exceeds Quota)</span>
                </div>
                <p className="leading-relaxed">{downgradeError}</p>
                <p className="text-[11px] text-rose-600 font-semibold pt-1 border-t border-rose-200/60">
                  Taxoryn does not delete existing practice data. Please reduce active resources before switching.
                </p>
              </div>
            )}

            <div className="bg-slate-50 rounded-xl p-3.5 space-y-2 text-xs text-slate-600 border border-slate-100">
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Target Plan:</span>
                <span className="font-bold text-slate-900">{targetPlanToChange.name}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Billing Cadence:</span>
                <span className="font-semibold text-slate-800">{interval === 'ANNUAL' ? 'Annual (20% Off)' : 'Monthly'}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Staff Limit:</span>
                <span className="font-semibold text-slate-800">{targetPlanToChange.maxUsers} Users</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Client Limit:</span>
                <span className="font-semibold text-slate-800">{targetPlanToChange.maxClients} Active Clients</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Vault Storage:</span>
                <span className="font-semibold text-slate-800">{targetPlanToChange.formattedStorage || '25 GB'}</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setTargetPlanToChange(null)}
                disabled={isUpdating}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleConfirmPlanChange}
                isLoading={isUpdating}
              >
                Confirm Plan Change
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Enterprise Consultation Inquiry Modal */}
      {isEnterpriseModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-100 animate-scale-up">
            <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-brand-50 text-brand-700 rounded-xl">
                  <Sparkles className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-extrabold text-slate-900 text-sm">Enterprise Consultation & Quota Scale</h3>
                  <p className="text-[11px] text-slate-500">
                    Custom deployment, ERP integrations, and high-volume compliance pipelines
                  </p>
                </div>
              </div>

              <button
                onClick={() => setIsEnterpriseModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitEnterpriseInquiry} className="p-5 space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Contact Name <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. CA Rajesh Sharma"
                    value={enterpriseForm.contactName}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, contactName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Work Email <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="e.g. rajesh@sharmaca.com"
                    value={enterpriseForm.contactEmail}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
                  <input
                    type="tel"
                    placeholder="e.g. +91 98765 43210"
                    value={enterpriseForm.contactPhone || ''}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, contactPhone: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Firm / CA Practice Name</label>
                  <input
                    type="text"
                    placeholder="e.g. Sharma & Associates LLP"
                    value={enterpriseForm.firmName || ''}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, firmName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Estimated Team Size</label>
                  <input
                    type="number"
                    min={1}
                    placeholder="e.g. 50"
                    value={enterpriseForm.estimatedTeamSize || ''}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, estimatedTeamSize: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Estimated Active Clients</label>
                  <input
                    type="number"
                    min={1}
                    placeholder="e.g. 1000"
                    value={enterpriseForm.estimatedClientCount || ''}
                    onChange={(e) => setEnterpriseForm({ ...enterpriseForm, estimatedClientCount: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Custom Requirements / Workflows</label>
                <textarea
                  rows={3}
                  placeholder="e.g. Need Tally/SAP ERP integration, multi-branch audit workflows, custom role permissions, SSO."
                  value={enterpriseForm.requirements || ''}
                  onChange={(e) => setEnterpriseForm({ ...enterpriseForm, requirements: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsEnterpriseModalOpen(false)}
                  disabled={isSubmittingEnterprise}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" isLoading={isSubmittingEnterprise}>
                  Submit Enterprise Request
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
