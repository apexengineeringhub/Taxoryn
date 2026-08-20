import React from 'react';
import { Globe, Users, ShieldCheck, Mail, CheckCircle2 } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';

export const ClientPortalManagementPage: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Client Portal Hub</h1>
          <p className="text-xs text-slate-500 mt-1">
            Configure client-facing portal access, upload permissions, and acknowledgement downloads.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title="Portal Feature Overview" subtitle="Client self-service features">
          <div className="space-y-3 text-xs">
            <div className="flex items-center gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Direct document uploads (Bank statements, 26AS, Form 16)</span>
            </div>
            <div className="flex items-center gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Real-time filing status & signed computation downloads</span>
            </div>
            <div className="flex items-center gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>Online fee invoice viewing and instant receipt downloads</span>
            </div>
          </div>
        </Card>

        <Card title="Security & Isolation" subtitle="Strict multi-tenant boundaries" className="md:col-span-2">
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2 text-xs">
            <div className="flex items-center gap-2 font-bold text-slate-900">
              <ShieldCheck className="w-4 h-4 text-brand-600" />
              <span>End-to-End Client Isolation</span>
            </div>
            <p className="text-slate-600">
              Client users are isolated to their specific clientId with role <code>CLIENT_USER</code>. They cannot access internal practice notes, other client records, or internal CA staff task allocations.
            </p>
          </div>
        </Card>
      </div>
    </div>
  );
};
