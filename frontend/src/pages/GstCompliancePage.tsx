import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  CheckCircle2,
  Clock,
  AlertTriangle,
  FileText,
  Send,
  Sparkles,
  Plus,
  ArrowRight,
  ShieldCheck,
  CheckSquare,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { gstApi, clientApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { GstReturnFiling, GstProfile, Client } from '../types';
import clsx from 'clsx';

export const GstCompliancePage: React.FC = () => {
  const [filings, setFilings] = useState<GstReturnFiling[]>([]);
  const [profiles, setProfiles] = useState<GstProfile[]>([]);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Modals
  const [selectedFiling, setSelectedFiling] = useState<GstReturnFiling | null>(null);
  const [isFilingModalOpen, setIsFilingModalOpen] = useState(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isNewFilingModalOpen, setIsNewFilingModalOpen] = useState(false);

  // Record Filing Form
  const [arnNumber, setArnNumber] = useState('');
  const [filingDate, setFilingDate] = useState(new Date().toISOString().split('T')[0]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Batch Generation Form
  const [batchPeriod, setBatchPeriod] = useState(() => {
    const d = new Date();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    return `${d.getFullYear()}-${m}`;
  });
  const [batchReturnType, setBatchReturnType] = useState<'GSTR1' | 'GSTR3B' | 'CMP08' | 'GSTR9'>('GSTR3B');
  const [batchFy, setBatchFy] = useState('2026-27');
  const [batchDueDate, setBatchDueDate] = useState(() => {
    const d = new Date();
    d.setDate(20);
    return d.toISOString().split('T')[0];
  });

  // Individual New Filing Form
  const [newProfileId, setNewProfileId] = useState('');
  const [newReturnType, setNewReturnType] = useState<'GSTR1' | 'GSTR3B' | 'CMP08' | 'GSTR9'>('GSTR3B');
  const [newPeriod, setNewPeriod] = useState('2026-07');
  const [newFy, setNewFy] = useState('2026-27');
  const [newDueDate, setNewDueDate] = useState('2026-08-20');
  const [newTaxableValue, setNewTaxableValue] = useState('');
  const [newTaxLiability, setNewTaxLiability] = useState('');
  const [newItc, setNewItc] = useState('');

  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();

  useEffect(() => {
    loadFilings();
    loadProfiles();
  }, [activeTab]);

  const loadFilings = async () => {
    try {
      setIsLoading(true);
      const params: any = {};
      if (activeTab !== 'ALL') params.returnType = activeTab;
      const res = await gstApi.getFilings(params);
      setFilings(res.content || []);
    } catch (err) {
      console.error('Failed to load GST filings', err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadProfiles = async () => {
    try {
      const res = await gstApi.getProfiles();
      setProfiles(res.content || []);
    } catch (err) {
      console.error('Failed to load GST profiles', err);
    }
  };

  // Record Filing / Mark as Filed
  const handleRecordFiling = async () => {
    if (!selectedFiling) return;
    if (!arnNumber.trim()) {
      alert('Please enter the GST Portal ARN / Acknowledgement reference number.');
      return;
    }

    try {
      setIsSubmitting(true);
      await gstApi.recordFiling(selectedFiling.id, {
        acknowledgementNumber: arnNumber.trim().toUpperCase(),
        filingDate: filingDate,
        filingStatus: 'FILED',
      });
      setIsFilingModalOpen(false);
      setArnNumber('');
      loadFilings();
    } catch (err: any) {
      alert(`Failed to record filing: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Quick Status Update
  const handleQuickStatusUpdate = async (filingId: string, status: string) => {
    try {
      await gstApi.updateFilingStatus(filingId, { filingStatus: status });
      loadFilings();
    } catch (err: any) {
      alert(`Failed to update status: ${err.response?.data?.message || err.message}`);
    }
  };

  // Execute Batch Filing Generator
  const handleExecuteBatchGenerate = async () => {
    try {
      setIsSubmitting(true);
      const res = await gstApi.batchGenerateFilings({
        returnPeriod: batchPeriod,
        returnType: batchReturnType,
        returnTypes: [batchReturnType],
        financialYear: batchFy,
        dueDate: batchDueDate,
        gstr1DueDate: batchDueDate,
        gstr3bDueDate: batchDueDate,
        cmp08DueDate: batchDueDate,
      });
      alert(`Successfully scheduled ${res?.length || 0} filings for ${batchReturnType} (${batchPeriod})!`);
      setIsBatchModalOpen(false);
      loadFilings();
    } catch (err: any) {
      alert(`Batch generation notice: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Create Individual Filing
  const handleCreateIndividualFiling = async () => {
    if (!newProfileId) {
      alert('Please select a GST registration profile.');
      return;
    }

    try {
      setIsSubmitting(true);
      await gstApi.createFiling({
        gstProfileId: newProfileId,
        returnType: newReturnType,
        returnPeriod: newPeriod,
        financialYear: newFy,
        dueDate: newDueDate,
        filingStatus: 'PENDING',
        totalTaxableValue: parseFloat(newTaxableValue) || 0,
        totalTaxLiability: parseFloat(newTaxLiability) || 0,
        totalItcClaimed: parseFloat(newItc) || 0,
      });
      setIsNewFilingModalOpen(false);
      setNewTaxableValue('');
      setNewTaxLiability('');
      setNewItc('');
      loadFilings();
    } catch (err: any) {
      alert(`Failed to schedule filing: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(val);
  };

  const columns: Column<GstReturnFiling>[] = [
    {
      header: 'Client & GSTIN',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.clientName || 'Practice Client'}</span>
          <span className="font-mono text-[11px] font-bold text-brand-600 block">{row.gstin || '27AAACB1111A1Z5'}</span>
        </div>
      ),
    },
    {
      header: 'Return Type',
      accessor: (row) => (
        <span className="font-bold text-xs bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200">
          {row.returnType}
        </span>
      ),
    },
    {
      header: 'Return Period',
      accessor: (row) => <span className="font-mono text-xs font-semibold text-slate-700">{row.returnPeriod}</span>,
    },
    {
      header: 'Due Date',
      accessor: (row) => {
        const isOverdue = new Date(row.dueDate) < new Date() && row.filingStatus !== 'FILED';
        return (
          <span className={clsx('font-mono text-xs font-semibold', isOverdue ? 'text-rose-600 font-bold' : 'text-slate-700')}>
            {row.dueDate}
          </span>
        );
      },
    },
    {
      header: 'Taxable Value',
      accessor: (row) => <span className="font-mono font-medium text-slate-800">{formatCurrency(row.totalTaxableValue)}</span>,
      align: 'right',
    },
    {
      header: 'Tax Liability',
      accessor: (row) => <span className="font-mono font-medium text-slate-800">{formatCurrency(row.totalTaxLiability)}</span>,
      align: 'right',
    },
    {
      header: 'ITC Claimed',
      accessor: (row) => <span className="font-mono font-medium text-emerald-700">{formatCurrency(row.totalItcClaimed)}</span>,
      align: 'right',
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.filingStatus} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          {row.filingStatus === 'PENDING' && (
            <button
              onClick={() => handleQuickStatusUpdate(row.id, 'PREPARED')}
              className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-semibold transition-colors"
            >
              Mark Prepared
            </button>
          )}

          {row.filingStatus !== 'FILED' ? (
            <button
              onClick={() => {
                setSelectedFiling(row);
                setArnNumber(row.acknowledgementNumber || '');
                setIsFilingModalOpen(true);
              }}
              className="px-2.5 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
            >
              <Send className="w-3 h-3" /> Record Filing
            </button>
          ) : (
            <div className="text-right">
              <span className="text-[10px] font-mono font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 block">
                {row.acknowledgementNumber || 'FILED'}
              </span>
            </div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">GST Compliance Hub</h1>
          <p className="text-xs text-slate-500 mt-1">
            GSTR-1, GSTR-3B, CMP-08, and Annual Return tracking with statutory deadlines.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link to="/gst/migration">
            <Button variant="outline" leftIcon={<Sparkles className="w-4 h-4 text-brand-600" />}>
              📥 Bulk GST Migration Hub
            </Button>
          </Link>
          <Button
            variant="outline"
            onClick={() => setIsBatchModalOpen(true)}
            leftIcon={<Clock className="w-4 h-4 text-indigo-600" />}
          >
            ⚡ Batch Schedule Filings
          </Button>
          <Button onClick={() => setIsNewFilingModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            New Return Filing
          </Button>
        </div>
      </div>

      {/* Return Type Tab Filter */}
      <div className="border-b border-slate-200 flex items-center gap-2">
        {['ALL', 'GSTR1', 'GSTR3B', 'CMP08', 'GSTR9'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={clsx(
              'px-4 py-2.5 text-xs font-bold border-b-2 transition-all',
              activeTab === tab
                ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            {tab === 'ALL' ? 'All GST Returns' : tab}
          </button>
        ))}
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={filings}
        isLoading={isLoading}
        searchPlaceholder="Search by GSTIN, Client name, or ARN..."
      />

      {/* 1. Record Filing / ARN Modal */}
      <Modal
        isOpen={isFilingModalOpen}
        onClose={() => setIsFilingModalOpen(false)}
        title="Record GST Return Filing & ARN"
        subtitle={`Filing ${selectedFiling?.returnType} for period ${selectedFiling?.returnPeriod} (${selectedFiling?.gstin})`}
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              ARN / Acknowledgement Reference Number <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. AA2707261234567"
              value={arnNumber}
              onChange={(e) => setArnNumber(e.target.value.toUpperCase())}
              className="w-full font-mono text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Filing Date</label>
            <input
              type="date"
              value={filingDate}
              onChange={(e) => setFilingDate(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsFilingModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleRecordFiling}
              isLoading={isSubmitting}
              style={{ backgroundColor: currentTheme.primaryColor }}
              leftIcon={<CheckCircle2 className="w-4 h-4" />}
            >
              Confirm & Mark as Filed
            </Button>
          </div>
        </div>
      </Modal>

      {/* 2. Batch Schedule Filings Modal */}
      <Modal
        isOpen={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        title="Batch Schedule GST Filings"
        subtitle={`Auto-schedules compliance filings for all active GST registrations in ${practiceName}`}
      >
        <div className="space-y-4 text-xs">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Type</label>
              <select
                value={batchReturnType}
                onChange={(e) => setBatchReturnType(e.target.value as any)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="GSTR3B">GSTR-3B (Monthly Summary)</option>
                <option value="GSTR1">GSTR-1 (Outward Supplies)</option>
                <option value="CMP08">CMP-08 (Composition Statement)</option>
                <option value="GSTR9">GSTR-9 (Annual Return)</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Period</label>
              <input
                type="text"
                value={batchPeriod}
                onChange={(e) => setBatchPeriod(e.target.value)}
                placeholder="2026-07"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Financial Year</label>
              <input
                type="text"
                value={batchFy}
                onChange={(e) => setBatchFy(e.target.value)}
                placeholder="2026-27"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Statutory Due Date</label>
              <input
                type="date"
                value={batchDueDate}
                onChange={(e) => setBatchDueDate(e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg"
              />
            </div>
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsBatchModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleExecuteBatchGenerate}
              isLoading={isSubmitting}
              style={{ backgroundColor: currentTheme.primaryColor }}
              leftIcon={<Sparkles className="w-4 h-4" />}
            >
              Generate Filings Across Firm
            </Button>
          </div>
        </div>
      </Modal>

      {/* 3. New Individual Return Filing Modal */}
      <Modal
        isOpen={isNewFilingModalOpen}
        onClose={() => setIsNewFilingModalOpen(false)}
        title="Schedule Individual GST Filing"
        subtitle="Create an individual return filing record for a specific client"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Select Client GSTIN <span className="text-rose-500">*</span>
            </label>
            <select
              value={newProfileId}
              onChange={(e) => setNewProfileId(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-white"
            >
              <option value="">-- Choose Client GST Profile --</option>
              {profiles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.gstin} - {p.tradeName || p.legalName}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Type</label>
              <select
                value={newReturnType}
                onChange={(e) => setNewReturnType(e.target.value as any)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="GSTR3B">GSTR-3B</option>
                <option value="GSTR1">GSTR-1</option>
                <option value="CMP08">CMP-08</option>
                <option value="GSTR9">GSTR-9</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Period</label>
              <input
                type="text"
                value={newPeriod}
                onChange={(e) => setNewPeriod(e.target.value)}
                placeholder="2026-07"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Taxable Value (₹)</label>
              <input
                type="number"
                placeholder="0"
                value={newTaxableValue}
                onChange={(e) => setNewTaxableValue(e.target.value)}
                className="w-full px-2.5 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Tax Liability (₹)</label>
              <input
                type="number"
                placeholder="0"
                value={newTaxLiability}
                onChange={(e) => setNewTaxLiability(e.target.value)}
                className="w-full px-2.5 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">ITC Claimed (₹)</label>
              <input
                type="number"
                placeholder="0"
                value={newItc}
                onChange={(e) => setNewItc(e.target.value)}
                className="w-full px-2.5 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewFilingModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreateIndividualFiling}
              isLoading={isSubmitting}
              style={{ backgroundColor: currentTheme.primaryColor }}
            >
              Schedule Filing
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
