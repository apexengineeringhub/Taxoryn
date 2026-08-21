import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  FileSpreadsheet,
  CheckCircle2,
  Send,
  AlertCircle,
  Sparkles,
  Plus,
  ArrowRight,
  Clock,
  ShieldCheck,
  Calendar,
  Layers,
  FileCheck,
  Filter,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { itrApi, clientApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { ItrReturn, ItrProfile, Client } from '../types';
import clsx from 'clsx';

export const ItrCompliancePage: React.FC = () => {
  const [returns, setReturns] = useState<ItrReturn[]>([]);
  const [profiles, setProfiles] = useState<ItrProfile[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [assessmentYear, setAssessmentYear] = useState<string>('2026-27');
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Modals
  const [selectedReturn, setSelectedReturn] = useState<ItrReturn | null>(null);
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [isRecordFilingModalOpen, setIsRecordFilingModalOpen] = useState(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isNewReturnModalOpen, setIsNewReturnModalOpen] = useState(false);

  // Status & Record Filing Form States
  const [newStatus, setNewStatus] = useState('FILED');
  const [ackNo, setAckNo] = useState('');
  const [filingDate, setFilingDate] = useState(new Date().toISOString().split('T')[0]);
  const [verificationDate, setVerificationDate] = useState('');
  const [practitionerNotes, setPractitionerNotes] = useState('');

  // Batch Generation Form States
  const [batchAy, setBatchAy] = useState('2026-27');
  const [batchFy, setBatchFy] = useState('2025-26');
  const [batchNonAuditDueDate, setBatchNonAuditDueDate] = useState('2026-07-31');
  const [batchAuditDueDate, setBatchAuditDueDate] = useState('2026-10-31');

  // New Individual Return Form States
  const [newPan, setNewPan] = useState('');
  const [newClientId, setNewClientId] = useState('');
  const [newAy, setNewAy] = useState('2026-27');
  const [newFy, setNewFy] = useState('2025-26');
  const [newItrType, setNewItrType] = useState<any>('ITR_1');
  const [newDueDate, setNewDueDate] = useState('2026-07-31');

  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();

  useEffect(() => {
    loadReturns();
    loadPrerequisites();
  }, [assessmentYear, activeTab]);

  const loadPrerequisites = async () => {
    try {
      const [profRes, clientRes] = await Promise.all([
        itrApi.getProfiles().catch(() => ({ content: [] })),
        clientApi.getAll({ size: 300 }).catch(() => ({ content: [] })),
      ]);
      setProfiles(profRes.content || []);
      setClients(clientRes.content || []);
    } catch (err) {
      console.error('Failed to load ITR prerequisites', err);
    }
  };

  const loadReturns = async () => {
    try {
      setIsLoading(true);
      const params: any = { assessmentYear };
      if (activeTab !== 'ALL') {
        params.itrType = activeTab;
      }
      const res = await itrApi.getReturns(params);
      setReturns(res.content || []);
    } catch (err) {
      console.error('Failed to load ITR returns', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Quick Status Update
  const handleUpdateStatus = async () => {
    if (!selectedReturn) return;
    try {
      setIsSubmitting(true);
      await itrApi.updateReturnStatus(selectedReturn.id, {
        status: newStatus,
        acknowledgementNumber: ackNo || undefined,
        verificationDate: newStatus === 'COMPLETED' ? (verificationDate || new Date().toISOString().split('T')[0]) : undefined,
        notes: practitionerNotes || undefined,
      });
      setIsStatusModalOpen(false);
      setAckNo('');
      setPractitionerNotes('');
      loadReturns();
    } catch (err: any) {
      alert(`Failed to update return status: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Record e-Filing & Acknowledgement
  const handleRecordFiling = async () => {
    if (!selectedReturn) return;
    if (!ackNo.trim()) {
      alert('Please enter the e-Filing Acknowledgement Number / ITR-V Ack.');
      return;
    }

    try {
      setIsSubmitting(true);
      await itrApi.recordFilingDetails(selectedReturn.id, {
        acknowledgementNumber: ackNo.trim().toUpperCase(),
        filingDate: filingDate,
        verificationDate: verificationDate || undefined,
        notes: practitionerNotes || undefined,
      });
      setIsRecordFilingModalOpen(false);
      setAckNo('');
      setPractitionerNotes('');
      loadReturns();
    } catch (err: any) {
      alert(`Failed to record ITR filing: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Batch Generate Returns across practice (With Resilient Fallback)
  const handleBatchGenerate = async () => {
    try {
      setIsSubmitting(true);

      // 1. Try High-Speed Batch Endpoint First
      try {
        const res = await itrApi.batchGenerateReturns({
          assessmentYear: batchAy,
          financialYear: batchFy,
          nonAuditDueDate: batchNonAuditDueDate,
          auditDueDate: batchAuditDueDate,
        });
        alert(`Successfully scheduled ${res?.length || 0} ITR returns for AY ${batchAy}!`);
        setIsBatchModalOpen(false);
        loadReturns();
        return;
      } catch (batchErr: any) {
        console.warn('Batch generation endpoint fallback, executing client-level scheduling...', batchErr);
      }

      // 2. Sequential Fallback: Generate returns for each registered ITR profile or client
      let scheduledCount = 0;
      const targetProfiles = profiles.length > 0 ? profiles : clients.map((c) => ({
        id: '',
        clientId: c.id,
        pan: c.pan,
        taxpayerType: (c.clientType === 'PRIVATE_LIMITED' || c.clientType === 'PUBLIC_LIMITED' ? 'COMPANY' : (c.clientType === 'LLP' ? 'LLP' : (c.clientType === 'PARTNERSHIP' ? 'FIRM' : 'INDIVIDUAL'))) as any,
        defaultItrType: (c.clientType === 'PRIVATE_LIMITED' || c.clientType === 'PUBLIC_LIMITED' ? 'ITR_6' : (c.clientType === 'LLP' || c.clientType === 'PARTNERSHIP' ? 'ITR_5' : 'ITR_1')) as any,
        residentialStatus: 'RESIDENT' as any,
        status: 'ACTIVE' as any,
      }));

      for (const prof of targetProfiles) {
        try {
          const isAudit = prof.taxpayerType === 'COMPANY' || prof.taxpayerType === 'LLP' || prof.defaultItrType === 'ITR_6';
          const dueDate = isAudit ? batchAuditDueDate : batchNonAuditDueDate;
          await itrApi.createReturn({
            clientId: prof.clientId || undefined,
            pan: prof.pan,
            assessmentYear: batchAy,
            financialYear: batchFy,
            itrType: prof.defaultItrType || 'ITR_1',
            taxpayerType: prof.taxpayerType || 'INDIVIDUAL',
            dueDate: dueDate,
            status: 'DOCUMENTS_PENDING',
          });
          scheduledCount++;
        } catch (singleErr: any) {
          // Ignore if return already exists for client + AY
        }
      }

      alert(`Successfully scheduled ${scheduledCount} ITR returns for AY ${batchAy}!`);
      setIsBatchModalOpen(false);
      loadReturns();
    } catch (err: any) {
      alert(`Batch generation notice: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Schedule Individual Return
  const handleScheduleNewReturn = async () => {
    if (!newClientId && !newPan) {
      alert('Please select a client or enter a valid PAN.');
      return;
    }

    try {
      setIsSubmitting(true);
      await itrApi.createReturn({
        clientId: newClientId || undefined,
        pan: newPan ? newPan.toUpperCase().trim() : undefined,
        assessmentYear: newAy,
        financialYear: newFy,
        itrType: newItrType,
        dueDate: newDueDate,
        status: 'DOCUMENTS_PENDING',
      });
      setIsNewReturnModalOpen(false);
      setNewPan('');
      setNewClientId('');
      loadReturns();
    } catch (err: any) {
      alert(`Failed to schedule return: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns: Column<ItrReturn>[] = [
    {
      header: 'Client & PAN',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.clientName || 'Taxpayer'}</span>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="font-mono text-[11px] font-semibold text-slate-600">
              {row.pan || 'PAN Registered'}
            </span>
            <span className="text-[10px] text-purple-700 bg-purple-50 px-1.5 py-0.2 rounded font-bold">
              AY {row.assessmentYear}
            </span>
          </div>
        </div>
      ),
    },
    {
      header: 'Taxpayer Category',
      accessor: (row) => (
        <span className="text-xs text-slate-600 font-medium">{row.taxpayerType || 'INDIVIDUAL'}</span>
      ),
    },
    {
      header: 'ITR Form',
      accessor: (row) => (
        <span className="font-bold text-xs bg-purple-50 text-purple-700 px-2 py-0.5 rounded border border-purple-200">
          {row.itrType?.replace('_', ' ') || 'ITR-1'}
        </span>
      ),
    },
    {
      header: 'Statutory Due Date',
      accessor: (row) => (
        <div>
          <span className="font-mono text-xs text-slate-700 block">{row.dueDate || '2026-07-31'}</span>
          {row.dueDate && new Date(row.dueDate) < new Date() && !['FILED', 'COMPLETED'].includes(row.status) && (
            <span className="text-[10px] text-rose-600 font-bold">Overdue</span>
          )}
        </div>
      ),
    },
    {
      header: 'Ack / ITR-V Number',
      accessor: (row) =>
        row.acknowledgementNumber ? (
          <div>
            <span className="font-mono text-xs text-slate-800 font-bold block">{row.acknowledgementNumber}</span>
            {row.filingDate && (
              <span className="font-mono text-[10px] text-slate-400">Filed: {row.filingDate}</span>
            )}
          </div>
        ) : (
          <span className="text-slate-400 italic text-[11px]">Pending e-Filing</span>
        ),
    },
    {
      header: 'Filing Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          {/* Quick Record Filing / Ack button if unfiled */}
          {!['FILED', 'COMPLETED'].includes(row.status) ? (
            <button
              onClick={() => {
                setSelectedReturn(row);
                setAckNo(row.acknowledgementNumber || '');
                setFilingDate(new Date().toISOString().split('T')[0]);
                setIsRecordFilingModalOpen(true);
              }}
              className="px-2.5 py-1 bg-purple-600 hover:bg-purple-700 text-white rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
            >
              <CheckCircle2 className="w-3 h-3" /> Record Ack
            </button>
          ) : (
            <button
              onClick={() => {
                setSelectedReturn(row);
                setNewStatus(row.status);
                setAckNo(row.acknowledgementNumber || '');
                setIsStatusModalOpen(true);
              }}
              className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-xs font-semibold transition-colors"
            >
              Update
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">Income Tax (ITR) Compliance</h1>
            <span className="text-xs bg-purple-100 text-purple-800 font-bold px-2 py-0.5 rounded-full border border-purple-200">
              AY {assessmentYear}
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            ITR-1 to ITR-7 computation tracking, e-verification stages, batch return generation, and CPC acknowledgment records.
          </p>
        </div>

        {/* Global Action Toolbar */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* AY Switcher */}
          <div className="flex items-center gap-1.5 bg-white border border-slate-200 rounded-lg px-2.5 py-1 shadow-2xs">
            <span className="text-[11px] font-bold text-slate-500">AY:</span>
            <select
              value={assessmentYear}
              onChange={(e) => setAssessmentYear(e.target.value)}
              className="bg-transparent text-xs font-bold text-slate-800 focus:outline-none"
            >
              <option value="2026-27">AY 2026-27 (Current)</option>
              <option value="2025-26">AY 2025-26</option>
              <option value="2024-25">AY 2024-25</option>
            </select>
          </div>

          <Link to="/itr/migration">
            <Button
              variant="outline"
              leftIcon={<Layers className="w-4 h-4 text-purple-600" />}
              className="border-purple-200 text-purple-900 bg-purple-50/50 hover:bg-purple-100/50"
            >
              📥 Bulk ITR Migration Hub
            </Button>
          </Link>

          <Button
            variant="outline"
            onClick={() => setIsBatchModalOpen(true)}
            leftIcon={<Sparkles className="w-4 h-4 text-amber-500" />}
            className="border-amber-300 bg-amber-50/40 hover:bg-amber-100/50 text-amber-900"
          >
            ⚡ Batch Schedule Returns
          </Button>

          <Button
            onClick={() => setIsNewReturnModalOpen(true)}
            style={{ backgroundColor: currentTheme.primaryColor }}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Schedule Return
          </Button>
        </div>
      </div>

      {/* Return Type Tab Filters */}
      <div className="border-b border-slate-200 flex items-center gap-2 overflow-x-auto">
        {['ALL', 'ITR_1', 'ITR_2', 'ITR_3', 'ITR_4', 'ITR_5', 'ITR_6', 'ITR_7'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={clsx(
              'px-4 py-2.5 text-xs font-bold border-b-2 transition-all whitespace-nowrap',
              activeTab === tab
                ? 'border-purple-600 text-purple-700 bg-purple-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            {tab === 'ALL' ? 'All ITR Returns' : tab.replace('_', '-')}
          </button>
        ))}
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={returns}
        isLoading={isLoading}
        searchPlaceholder="Search by PAN, client name, or Ack number..."
      />

      {/* 1. Record e-Filing / Ack Number Modal */}
      <Modal
        isOpen={isRecordFilingModalOpen}
        onClose={() => setIsRecordFilingModalOpen(false)}
        title="Record ITR e-Filing & Acknowledgement"
        subtitle={`Recording CPC Submission for ${selectedReturn?.clientName || 'Taxpayer'} (AY ${selectedReturn?.assessmentYear})`}
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              ITR-V / e-Filing Acknowledgement Number <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              placeholder="e.g. 123456789012345"
              value={ackNo}
              onChange={(e) => setAckNo(e.target.value.toUpperCase())}
              className="w-full font-mono text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Date of Filing</label>
              <input
                type="date"
                value={filingDate}
                onChange={(e) => setFilingDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">e-Verification Date (Optional)</label>
              <input
                type="date"
                value={verificationDate}
                onChange={(e) => setVerificationDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Practitioner Remarks / Notes</label>
            <textarea
              rows={2}
              placeholder="e.g. Filed with DSC / Aadhaar OTP verified"
              value={practitionerNotes}
              onChange={(e) => setPractitionerNotes(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsRecordFilingModalOpen(false)}>
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

      {/* 2. Batch Schedule ITR Returns Modal */}
      <Modal
        isOpen={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        title="Batch Schedule Practice ITR Returns"
        subtitle={`Auto-schedules return filing records for all active ITR client profiles in ${practiceName}`}
      >
        <div className="space-y-4 text-xs">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Assessment Year</label>
              <input
                type="text"
                value={batchAy}
                onChange={(e) => setBatchAy(e.target.value)}
                placeholder="2026-27"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Financial Year</label>
              <input
                type="text"
                value={batchFy}
                onChange={(e) => setBatchFy(e.target.value)}
                placeholder="2025-26"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Non-Audit Due Date (Individuals/Firms)</label>
              <input
                type="date"
                value={batchNonAuditDueDate}
                onChange={(e) => setBatchNonAuditDueDate(e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Audit / Corporate Due Date (ITR-6)</label>
              <input
                type="date"
                value={batchAuditDueDate}
                onChange={(e) => setBatchAuditDueDate(e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg"
              />
            </div>
          </div>

          <div className="p-3 bg-purple-50 rounded-lg border border-purple-200 text-purple-900 text-[11px] leading-relaxed">
            ⚡ This will scan all <strong>{profiles.length} registered ITR client profiles</strong> in your firm and create pending filing records with their respective default forms (ITR-1 to ITR-7). Existing returns for AY {batchAy} will not be duplicated.
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsBatchModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleBatchGenerate}
              isLoading={isSubmitting}
              style={{ backgroundColor: currentTheme.primaryColor }}
              leftIcon={<Sparkles className="w-4 h-4" />}
            >
              Generate Returns Across Firm
            </Button>
          </div>
        </div>
      </Modal>

      {/* 3. Schedule Individual Return Modal */}
      <Modal
        isOpen={isNewReturnModalOpen}
        onClose={() => setIsNewReturnModalOpen(false)}
        title="Schedule Individual ITR Return"
        subtitle="Create an ITR filing record for a specific client"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Select Client from Master List
            </label>
            <select
              value={newClientId}
              onChange={(e) => {
                setNewClientId(e.target.value);
                const selected = clients.find((c) => c.id === e.target.value);
                if (selected?.pan) setNewPan(selected.pan);
              }}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-white"
            >
              <option value="">-- Choose Existing Client --</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName} ({c.pan || 'No PAN'})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Or Enter Client PAN directly
            </label>
            <input
              type="text"
              placeholder="e.g. ABCDE1234F"
              value={newPan}
              onChange={(e) => setNewPan(e.target.value.toUpperCase())}
              className="w-full font-mono text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">ITR Form</label>
              <select
                value={newItrType}
                onChange={(e) => setNewItrType(e.target.value as any)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg bg-white"
              >
                <option value="ITR_1">ITR-1 (Sahaj - Salary/Interest)</option>
                <option value="ITR_2">ITR-2 (Capital Gains/Multiple Houses)</option>
                <option value="ITR_3">ITR-3 (Proprietorship Business/Profession)</option>
                <option value="ITR_4">ITR-4 (Sugam - Presumptive 44AD/ADA)</option>
                <option value="ITR_5">ITR-5 (Partnership Firm / LLP / AOP)</option>
                <option value="ITR_6">ITR-6 (Corporate / Companies)</option>
                <option value="ITR_7">ITR-7 (Trust / Non-Profit)</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Assessment Year</label>
              <input
                type="text"
                value={newAy}
                onChange={(e) => setNewAy(e.target.value)}
                placeholder="2026-27"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Statutory Due Date</label>
            <input
              type="date"
              value={newDueDate}
              onChange={(e) => setNewDueDate(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsNewReturnModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleScheduleNewReturn}
              isLoading={isSubmitting}
              style={{ backgroundColor: currentTheme.primaryColor }}
            >
              Schedule Return
            </Button>
          </div>
        </div>
      </Modal>

      {/* 4. Update Status Modal */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Update ITR Filing Status"
        subtitle={`Assessment Year ${selectedReturn?.assessmentYear} (${selectedReturn?.clientName})`}
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Status Stage</label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg bg-white"
            >
              <option value="DOCUMENTS_PENDING">Documents Pending</option>
              <option value="DATA_ENTRY">Data Entry</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="READY_TO_FILE">Ready To File</option>
              <option value="FILED">Filed (Pending e-Verification)</option>
              <option value="VERIFICATION_PENDING">Verification Pending</option>
              <option value="COMPLETED">Completed / Verified</option>
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              ITR-V / Acknowledgement Number (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. 123456789012345"
              value={ackNo}
              onChange={(e) => setAckNo(e.target.value)}
              className="w-full font-mono text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2 border-t border-slate-100">
            <Button variant="outline" onClick={() => setIsStatusModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateStatus} isLoading={isSubmitting}>
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
