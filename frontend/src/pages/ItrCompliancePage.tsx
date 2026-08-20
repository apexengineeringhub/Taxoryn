import React, { useState, useEffect } from 'react';
import { FileSpreadsheet, CheckCircle2, Send, AlertCircle } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { itrApi } from '../api/endpoints';
import { ItrReturn } from '../types';

export const ItrCompliancePage: React.FC = () => {
  const [returns, setReturns] = useState<ItrReturn[]>([]);
  const [assessmentYear, setAssessmentYear] = useState<string>('2026-27');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedReturn, setSelectedReturn] = useState<ItrReturn | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newStatus, setNewStatus] = useState('FILED');
  const [ackNo, setAckNo] = useState('');

  useEffect(() => {
    loadReturns();
  }, [assessmentYear]);

  const loadReturns = async () => {
    try {
      setIsLoading(true);
      const res = await itrApi.getReturns({ assessmentYear });
      setReturns(res.content || []);
    } catch (err) {
      console.error('Failed to load ITR returns', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async () => {
    if (!selectedReturn) return;
    try {
      await itrApi.updateReturnStatus(selectedReturn.id, {
        status: newStatus,
        acknowledgementNumber: ackNo || undefined,
        verificationDate: newStatus === 'COMPLETED' ? new Date().toISOString().split('T')[0] : undefined,
      });
      setIsModalOpen(false);
      setAckNo('');
      loadReturns();
    } catch (err) {
      alert('Failed to update return status');
    }
  };

  const columns: Column<ItrReturn>[] = [
    {
      header: 'Client & PAN',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.clientName || 'Individual Taxpayer'}</span>
          <span className="font-mono text-[10px] text-slate-400 block">AY {row.assessmentYear}</span>
        </div>
      ),
    },
    {
      header: 'Taxpayer Category',
      accessor: (row) => (
        <span className="text-xs text-slate-600 font-medium">{row.taxpayerType || 'COMPANY'}</span>
      ),
    },
    {
      header: 'ITR Form',
      accessor: (row) => (
        <span className="font-bold text-xs bg-purple-50 text-purple-700 px-2 py-0.5 rounded border border-purple-200">
          {row.itrType?.replace('_', ' ') || 'ITR-6'}
        </span>
      ),
    },
    {
      header: 'Due Date',
      accessor: (row) => <span className="font-mono text-xs text-slate-700">{row.dueDate}</span>,
    },
    {
      header: 'Ack / ITR-V Number',
      accessor: (row) => row.acknowledgementNumber ? (
        <span className="font-mono text-xs text-slate-700 font-semibold">{row.acknowledgementNumber}</span>
      ) : (
        <span className="text-slate-400 italic">Pending Filing</span>
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
        <button
          onClick={() => {
            setSelectedReturn(row);
            setNewStatus(row.status);
            setAckNo(row.acknowledgementNumber || '');
            setIsModalOpen(true);
          }}
          className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
        >
          Update Status
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Income Tax (ITR) Compliance</h1>
          <p className="text-xs text-slate-500 mt-1">
            ITR-1 to ITR-7 computation tracking, e-verification stages, and CPC acknowledgment records.
          </p>
        </div>

        {/* AY Switcher */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-500">Assessment Year:</span>
          <select
            value={assessmentYear}
            onChange={(e) => setAssessmentYear(e.target.value)}
            className="bg-white border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-bold text-slate-800 shadow-2xs focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            <option value="2026-27">AY 2026-27 (Current)</option>
            <option value="2025-26">AY 2025-26</option>
            <option value="2024-25">AY 2024-25</option>
          </select>
        </div>
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={returns}
        isLoading={isLoading}
        searchPlaceholder="Search by PAN, client name..."
      />

      {/* Update Status Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Update ITR Filing Status"
        subtitle={`Assessment Year ${selectedReturn?.assessmentYear}`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Status Stage</label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
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
            <label className="block text-xs font-semibold text-slate-700 mb-1">ITR-V / Acknowledgement Number (Optional)</label>
            <input
              type="text"
              placeholder="e.g. 123456789012345"
              value={ackNo}
              onChange={(e) => setAckNo(e.target.value)}
              className="w-full font-mono text-xs px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button onClick={handleUpdateStatus}>Save Changes</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
