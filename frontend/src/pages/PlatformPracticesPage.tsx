import React, { useEffect, useState } from 'react';
import {
  Building2,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  ShieldCheck,
  CreditCard,
  Mail,
  Phone,
  Calendar,
  ExternalLink,
  Edit2,
} from 'lucide-react';
import { adminPracticeApi } from '../api/endpoints';
import { Organization } from '../types';
import { Button } from '../components/common/Button';
import clsx from 'clsx';

export const PlatformPracticesPage: React.FC = () => {
  const [practices, setPractices] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedPractice, setSelectedPractice] = useState<Organization | null>(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<string>('ACTIVE');
  const [statusReason, setStatusReason] = useState<string>('');
  const [isUpdating, setIsUpdating] = useState(false);

  useEffect(() => {
    loadPractices();
  }, []);

  const loadPractices = async () => {
    try {
      setIsLoading(true);
      const res = await adminPracticeApi.getPractices({ size: 100 });
      setPractices(res?.content || []);
    } catch (err) {
      console.error('Failed to load practices', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!selectedPractice) return;
    try {
      setIsUpdating(true);
      await adminPracticeApi.updateStatus(selectedPractice.id, {
        status: targetStatus,
        reason: statusReason,
      });
      setIsStatusModalOpen(false);
      setSelectedPractice(null);
      setStatusReason('');
      await loadPractices();
    } catch (err) {
      console.error('Failed to update practice status', err);
    } finally {
      setIsUpdating(false);
    }
  };

  const filteredPractices = practices.filter((p) => {
    if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      const matchName = p.name?.toLowerCase().includes(q);
      const matchEmail = p.email?.toLowerCase().includes(q);
      const matchCity = p.city?.toLowerCase().includes(q);
      if (!matchName && !matchEmail && !matchCity) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/90 shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-widest bg-purple-100 text-purple-800 border border-purple-200">
              Tenant Management
            </span>
            <span className="text-xs text-slate-500">• Platform SuperAdmin</span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2.5">
            <Building2 className="w-7 h-7 text-purple-600" />
            Practice Tenant Governance
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Manage tax practice organizations, lifecycle statuses, and subscription tiers across Taxoryn.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={loadPractices} disabled={isLoading} className="text-xs gap-1.5">
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} /> Refresh
          </Button>
        </div>
      </div>

      {/* Privacy Notice Banner */}
      <div className="bg-purple-50/70 border border-purple-200/80 rounded-xl p-4 flex items-start gap-3 text-xs text-purple-950">
        <ShieldCheck className="w-5 h-5 text-purple-700 shrink-0 mt-0.5" />
        <div>
          <p className="font-bold">Tenant Isolation & Privacy Policy Guardrails</p>
          <p className="text-purple-800 mt-0.5 leading-relaxed">
            As a Platform SuperAdmin, you manage practice registration, verification, and tenant status. Sensitive tax calculations, PAN/Aadhaar documents, and individual client billing records remain isolated to the respective practice.
          </p>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search practice name, email, city..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto">
          {['ALL', 'ACTIVE', 'PENDING_VERIFICATION', 'SUSPENDED', 'INACTIVE'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold whitespace-nowrap transition-colors',
                statusFilter === st
                  ? 'bg-purple-600 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              )}
            >
              {st.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Practices Table */}
      <div className="bg-white border border-slate-200 rounded-xl shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-5 py-3">Practice Name</th>
                <th className="px-4 py-3">Subscription Tier</th>
                <th className="px-4 py-3">Contact</th>
                <th className="px-4 py-3">Location</th>
                <th className="px-4 py-3">Created</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center py-10 text-slate-400">Loading practices...</td>
                </tr>
              ) : filteredPractices.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-10 text-slate-400">No practices found matching criteria</td>
                </tr>
              ) : (
                filteredPractices.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-bold text-slate-900 text-sm">{p.name}</div>
                      <div className="text-[11px] text-slate-400 font-mono mt-0.5">ID: {p.id.substring(0, 8)}...</div>
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={clsx(
                        'px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wide border',
                        p.subscriptionPlan === 'ENTERPRISE' ? 'bg-purple-50 text-purple-700 border-purple-200' :
                        p.subscriptionPlan === 'BUSINESS' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' :
                        p.subscriptionPlan === 'PROFESSIONAL' ? 'bg-blue-50 text-blue-700 border-blue-200' :
                        'bg-slate-100 text-slate-700 border-slate-200'
                      )}>
                        {p.subscriptionPlan || 'STARTER'}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600">
                      <div className="flex items-center gap-1.5">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        <span>{p.email || 'N/A'}</span>
                      </div>
                      {p.phone && (
                        <div className="flex items-center gap-1.5 text-slate-400 mt-0.5">
                          <Phone className="w-3 h-3" />
                          <span>{p.phone}</span>
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3.5 text-slate-600">
                      {p.city ? `${p.city}, ${p.state || 'IN'}` : 'Not Specified'}
                    </td>
                    <td className="px-4 py-3.5 text-slate-500">
                      {p.createdAt ? new Date(p.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' }) : 'N/A'}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className={clsx(
                        'px-2.5 py-0.5 rounded-full text-[10px] font-bold border',
                        p.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                        p.status === 'SUSPENDED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                        'bg-amber-50 text-amber-700 border-amber-200'
                      )}>
                        {p.status}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          setSelectedPractice(p);
                          setTargetStatus(p.status || 'ACTIVE');
                          setIsStatusModalOpen(true);
                        }}
                        className="text-xs font-semibold"
                      >
                        Status
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Status Update Modal */}
      {isStatusModalOpen && selectedPractice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-scale-up">
            <h3 className="text-lg font-bold text-slate-900 mb-1">
              Update Practice Lifecycle Status
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Organization: <strong className="text-slate-800">{selectedPractice.name}</strong>
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Target Status</label>
                <select
                  value={targetStatus}
                  onChange={(e) => setTargetStatus(e.target.value)}
                  className="w-full p-2 text-xs rounded-lg border border-slate-300 focus:ring-2 focus:ring-purple-500"
                >
                  <option value="ACTIVE">ACTIVE (Fully Operational)</option>
                  <option value="SUSPENDED">SUSPENDED (Temporarily Blocked)</option>
                  <option value="INACTIVE">INACTIVE (Deactivated)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Reason / Note for Audit Trail</label>
                <textarea
                  rows={3}
                  value={statusReason}
                  onChange={(e) => setStatusReason(e.target.value)}
                  placeholder="Provide reason for status transition..."
                  className="w-full p-2 text-xs rounded-lg border border-slate-300 focus:ring-2 focus:ring-purple-500"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 mt-6 pt-4 border-t border-slate-100">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  setIsStatusModalOpen(false);
                  setSelectedPractice(null);
                }}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleUpdateStatus}
                disabled={isUpdating}
              >
                {isUpdating ? 'Updating...' : 'Save Status'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
