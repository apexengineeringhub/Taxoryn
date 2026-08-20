import React, { useState, useEffect } from 'react';
import { CreditCard, Check, Sparkles, ShieldCheck, Zap } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { subscriptionApi } from '../api/endpoints';
import { SubscriptionPlan, SubscriptionInfo } from '../types';
import clsx from 'clsx';

export const SubscriptionsPage: React.FC = () => {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionInfo | null>(null);
  const [interval, setInterval] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [isLoading, setIsLoading] = useState(true);

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
      setPlans(plansRes || []);
      setSubscription(currentRes || null);
    } catch (err) {
      console.error('Failed to load subscription info', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePlanChange = async (planCode: string) => {
    try {
      await subscriptionApi.changePlan({ plan: planCode, interval });
      alert(`Plan successfully upgraded to ${planCode}`);
      loadData();
    } catch (err) {
      alert('Plan upgrade failed');
    }
  };

  return (
    <div className="space-y-8 max-w-5xl mx-auto animate-fade-in">
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
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          { code: 'STARTER', name: 'Starter Practice', price: 999, clients: 50, users: 3, storage: 5 },
          { code: 'PROFESSIONAL', name: 'Professional CA Firm', price: 2499, clients: 250, users: 10, storage: 25, popular: true },
          { code: 'ENTERPRISE', name: 'Enterprise Corporate', price: 5999, clients: 1000, users: 50, storage: 100 },
        ].map((plan) => {
          const isCurrent = subscription?.plan === plan.code;
          const displayPrice = interval === 'ANNUAL' ? Math.round(plan.price * 0.8) : plan.price;

          return (
            <div
              key={plan.code}
              className={clsx(
                'bg-white rounded-2xl border p-6 flex flex-col justify-between shadow-card relative transition-all',
                plan.popular ? 'border-brand-500 ring-2 ring-brand-500/20' : 'border-slate-200/90',
                isCurrent && 'bg-slate-50/50'
              )}
            >
              {plan.popular && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-brand-600 text-white text-[10px] font-bold uppercase tracking-wider px-3 py-0.5 rounded-full shadow-sm">
                  Most Popular
                </span>
              )}

              <div>
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-extrabold text-base text-slate-900">{plan.name}</h3>
                    <p className="text-xs text-slate-500 mt-0.5">Ideal for growing tax practices</p>
                  </div>
                </div>

                <div className="mt-6 mb-6">
                  <span className="text-3xl font-black text-slate-900">₹{displayPrice}</span>
                  <span className="text-xs text-slate-400 font-medium"> / month</span>
                </div>

                <ul className="space-y-2.5 text-xs text-slate-600 border-t border-slate-100 pt-5">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Up to <strong>{plan.clients} Active Clients</strong></span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Up to <strong>{plan.users} Staff Users & CAs</strong></span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span><strong>{plan.storage} GB Encrypted Vault Storage</strong></span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>GST (GSTR-1, 3B) & ITR Hubs</span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Client Portal & Document Requests</span>
                  </li>
                </ul>
              </div>

              <div className="mt-8">
                <Button
                  variant={isCurrent ? 'outline' : plan.popular ? 'primary' : 'secondary'}
                  className="w-full"
                  disabled={isCurrent}
                  onClick={() => handlePlanChange(plan.code)}
                >
                  {isCurrent ? 'Current Active Plan' : 'Upgrade Plan'}
                </Button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
