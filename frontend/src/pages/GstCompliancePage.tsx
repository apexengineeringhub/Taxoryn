import React, { useState, useEffect } from 'react';
import { Building2, CheckCircle2, Clock, AlertTriangle, FileText, Send } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { gstApi } from '../api/endpoints';
import { GstReturnFiling } from '../types';
import clsx from 'clsx';

export const GstCompliancePage: React.FC = () => {
  const [filings, setFilings] = useState<GstReturnFiling[]>([]);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFiling, setSelectedFiling] = useState<GstReturnFiling | null>(null);
  const [isFilingModalOpen, setIsFilingModalOpen] = useState(false);
  const [arnNumber, setArnNumber] = useState('');
  const [filingDate, setFilingDate] = useState(new Date().toISOString().split('T')[0]);

  useEffect(() => {
    loadFilings();
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

  const handleRecordFiling = async () => {
    if (!selectedFiling || !arnNumber) return;
    try {
      await gstApi.recordFiling(selectedFiling.id, {
        acknowledgementNumber: arnNumber,
        filingDate: filingDate,
        filingStatus: 'FILED',
      });
      setIsFilingModalOpen(false);
      setArnNumber('');
      loadFilings();
    } catch (err) {
      alert('Failed to record filing');
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
          <span className="font-mono text-[10px] text-slate-400 block">{row.gstin || '27AAACB1111A1Z5'}</span>
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
      accessor: (row) => <span className="font-mono text-xs text-slate-700">{row.returnPeriod}</span>,
    },
    {
      header: 'Due Date',
      accessor: (row) => <span className="font-mono text-xs text-slate-700">{row.dueDate}</span>,
    },
    {
      header: 'Taxable Turnover',
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
        <div className="flex items-center justify-end gap-2">
          {row.filingStatus !== 'FILED' && (
            <button
              onClick={() => {
                setSelectedFiling(row);
                setIsFilingModalOpen(true);
              }}
              className="px-2.5 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
            >
              <Send className="w-3 h-3" /> Record Filing
            </button>
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
        searchPlaceholder="Search by GSTIN or client name..."
      />

      {/* Record Filing Modal */}
      <Modal
        isOpen={isFilingModalOpen}
        onClose={() => setIsFilingModalOpen(false)}
        title="Record GST Return Filing"
        subtitle={`Filing ${selectedFiling?.returnType} for ${selectedFiling?.returnPeriod}`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">ARN / Acknowledgement Number *</label>
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
            <label className="block text-xs font-semibold text-slate-700 mb-1">Filing Date</label>
            <input
              type="date"
              value={filingDate}
              onChange={(e) => setFilingDate(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>
          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsFilingModalOpen(false)}>Cancel</Button>
            <Button onClick={handleRecordFiling}>Mark as Filed</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
