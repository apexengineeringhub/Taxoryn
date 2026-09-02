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
  Eye,
  FileSpreadsheet,
  UploadCloud,
  Check,
  RotateCcw,
  ExternalLink,
  Layers,
  Inbox,
  AlertCircle,
  HelpCircle
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { gstApi, clientApi, documentApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { GstReturnFiling, GstProfile, Client } from '../types';
import clsx from 'clsx';

export const GstCompliancePage: React.FC = () => {
  const [filings, setFilings] = useState<GstReturnFiling[]>([]);
  const [profiles, setProfiles] = useState<GstProfile[]>([]);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Modals & Drawers
  const [selectedFiling, setSelectedFiling] = useState<GstReturnFiling | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [detailTab, setDetailTab] = useState<'OVERVIEW' | 'WORKFLOW' | 'TASK' | 'DOCUMENTS' | 'DOC_REQUEST'>('WORKFLOW');
  const [isFilingModalOpen, setIsFilingModalOpen] = useState(false);
  const [isBatchModalOpen, setIsBatchModalOpen] = useState(false);
  const [isNewFilingModalOpen, setIsNewFilingModalOpen] = useState(false);
  const [isDocReqModalOpen, setIsDocReqModalOpen] = useState(false);

  // Record Filing Form
  const [arnNumber, setArnNumber] = useState('');
  const [filingDate, setFilingDate] = useState(new Date().toISOString().split('T')[0]);
  const [reviewComments, setReviewComments] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Filing Attached Documents
  const [filingDocs, setFilingDocs] = useState<any[]>([]);
  const [isLoadingDocs, setIsLoadingDocs] = useState(false);

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
  const [newCreateTask, setNewCreateTask] = useState(true);
  const [newTaxableValue, setNewTaxableValue] = useState('');
  const [newTaxLiability, setNewTaxLiability] = useState('');
  const [newItc, setNewItc] = useState('');

  // Doc Request Form inside GST
  const [docReqPurpose, setDocReqPurpose] = useState('');
  const [includeSales, setIncludeSales] = useState(true);
  const [includePurchase, setIncludePurchase] = useState(true);
  const [include2b, setInclude2b] = useState(true);
  const [includeBank, setIncludeBank] = useState(false);

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

  const loadFilingDetail = async (filingId: string) => {
    try {
      const filing = await gstApi.getFilingById(filingId);
      setSelectedFiling(filing);
      loadFilingDocuments(filingId);
    } catch (err) {
      console.error('Failed to load filing details', err);
    }
  };

  const loadFilingDocuments = async (filingId: string) => {
    try {
      setIsLoadingDocs(true);
      const docs = await gstApi.getFilingDocuments(filingId);
      setFilingDocs(docs || []);
    } catch (err) {
      console.error('Failed to load filing documents', err);
      setFilingDocs([]);
    } finally {
      setIsLoadingDocs(false);
    }
  };

  // 1. Quick Status Update (PREPARED, UNDER_REVIEW, etc.)
  const handleUpdateStatus = async (status: string, comments?: string) => {
    if (!selectedFiling) return;
    try {
      setIsSubmitting(true);
      const updated = await gstApi.updateFilingStatus(selectedFiling.id, {
        filingStatus: status,
        notes: comments || selectedFiling.notes,
      });
      setSelectedFiling(updated);
      setReviewComments('');
      loadFilings();
    } catch (err: any) {
      alert(`Failed to update status: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 2. Record Filing / Mark as Filed with ARN
  const handleRecordFiling = async () => {
    if (!selectedFiling) return;
    if (!arnNumber.trim()) {
      alert('Please enter the GST Portal ARN / Acknowledgement reference number.');
      return;
    }

    try {
      setIsSubmitting(true);
      const updated = await gstApi.recordFiling(selectedFiling.id, {
        acknowledgementNumber: arnNumber.trim().toUpperCase(),
        filingDate: filingDate,
        filingStatus: 'FILED',
      });
      setSelectedFiling(updated);
      setIsFilingModalOpen(false);
      setArnNumber('');
      loadFilings();
    } catch (err: any) {
      alert(`Failed to record filing: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 3. Create Linked Task
  const handleCreateTaskForFiling = async (filingId: string) => {
    try {
      setIsSubmitting(true);
      const updated = await gstApi.createTaskForFiling(filingId);
      if (selectedFiling && selectedFiling.id === filingId) {
        setSelectedFiling(updated);
      }
      loadFilings();
      alert('Task created and assigned successfully!');
    } catch (err: any) {
      alert(`Failed to generate task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 4. Create Document Request for Filing
  const handleCreateDocRequest = async () => {
    if (!selectedFiling) return;
    try {
      setIsSubmitting(true);
      const items: any[] = [];
      if (includeSales) {
        items.push({ title: `Sales Register / Tax Invoices (${selectedFiling.returnPeriod})`, documentType: 'INVOICE', required: true });
      }
      if (includePurchase) {
        items.push({ title: `Purchase Register / Bills (${selectedFiling.returnPeriod})`, documentType: 'INVOICE', required: true });
      }
      if (include2b) {
        items.push({ title: `GSTR-2B Statement / Excel`, documentType: 'OTHER', required: false });
      }
      if (includeBank) {
        items.push({ title: `Bank Statement for ${selectedFiling.returnPeriod}`, documentType: 'BANK_STATEMENT', required: false });
      }

      await gstApi.createDocumentRequestForFiling(selectedFiling.id, {
        purpose: docReqPurpose || `GST ${selectedFiling.returnType} ${selectedFiling.returnPeriod} Supporting Documents`,
        items,
      });

      setIsDocReqModalOpen(false);
      loadFilingDetail(selectedFiling.id);
      loadFilings();
      alert('Document request sent to client portal successfully!');
    } catch (err: any) {
      alert(`Failed to request documents: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 5. Batch Generation
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

  // 6. Create Individual Filing
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
        createTask: newCreateTask,
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

  // Summary Metrics
  const totalCount = filings.length;
  const pendingCount = filings.filter((f) => f.filingStatus === 'PENDING').length;
  const preparedCount = filings.filter((f) => f.filingStatus === 'PREPARED' || f.filingStatus === 'UNDER_REVIEW').length;
  const filedCount = filings.filter((f) => f.filingStatus === 'FILED').length;
  const overdueCount = filings.filter((f) => f.filingStatus !== 'FILED' && new Date(f.dueDate) < new Date()).length;

  const columns: Column<GstReturnFiling>[] = [
    {
      header: 'Client & GSTIN',
      accessor: (row) => (
        <div>
          <button
            onClick={() => {
              setSelectedFiling(row);
              setDetailTab('OVERVIEW');
              setDetailModalOpen(true);
              loadFilingDetail(row.id);
            }}
            className="font-bold text-slate-900 hover:text-brand-600 block text-left transition-colors"
          >
            {row.clientName || 'Practice Client'}
          </button>
          <span className="font-mono text-[11px] font-bold text-brand-600 block">{row.gstin || '27AAACB1111A1Z5'}</span>
        </div>
      ),
    },
    {
      header: 'Return & Period',
      accessor: (row) => (
        <div>
          <span className="font-bold text-xs bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200 inline-block">
            {row.returnType}
          </span>
          <span className="font-mono text-xs font-semibold text-slate-600 block mt-0.5">{row.returnPeriod}</span>
        </div>
      ),
    },
    {
      header: 'Due Date',
      accessor: (row) => {
        const isOverdue = new Date(row.dueDate) < new Date() && row.filingStatus !== 'FILED';
        return (
          <div>
            <span className={clsx('font-mono text-xs font-semibold', isOverdue ? 'text-rose-600 font-bold' : 'text-slate-700')}>
              {row.dueDate}
            </span>
            {isOverdue && <span className="text-[10px] font-bold text-rose-600 block uppercase">Overdue</span>}
          </div>
        );
      },
    },
    {
      header: 'Workflow Links',
      accessor: (row) => (
        <div className="flex flex-col gap-1 text-[11px]">
          {/* Linked Task */}
          {row.taskId ? (
            <Link
              to="/tasks"
              className="inline-flex items-center gap-1 text-slate-700 hover:text-brand-600 font-medium"
            >
              <CheckSquare className="w-3 h-3 text-brand-600" />
              <span>Task:</span>
              <span className={clsx(
                'px-1.5 py-0.2 rounded text-[10px] font-bold',
                row.taskStatus === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700' :
                row.taskStatus === 'UNDER_REVIEW' ? 'bg-amber-50 text-amber-700' :
                'bg-blue-50 text-blue-700'
              )}>
                {row.taskStatus || 'LINKED'}
              </span>
            </Link>
          ) : (
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleCreateTaskForFiling(row.id);
              }}
              className="inline-flex items-center gap-1 text-[10px] font-semibold text-brand-600 hover:underline"
            >
              <Plus className="w-2.5 h-2.5" /> Generate Task
            </button>
          )}

          {/* Linked Document Request */}
          {row.documentRequestId ? (
            <div className="inline-flex items-center gap-1 text-slate-600">
              <Inbox className="w-3 h-3 text-indigo-500" />
              <span>Docs:</span>
              <span className="font-semibold text-slate-800">
                {row.documentRequestReceivedCount ?? 0}/{row.documentRequestItemsCount ?? 0}
              </span>
            </div>
          ) : (
            <button
              onClick={() => {
                setSelectedFiling(row);
                setIsDocReqModalOpen(true);
              }}
              className="inline-flex items-center gap-1 text-[10px] font-semibold text-indigo-600 hover:underline"
            >
              <Inbox className="w-2.5 h-2.5" /> Request Docs
            </button>
          )}
        </div>
      ),
    },
    {
      header: 'Tax Summary',
      accessor: (row) => (
        <div className="text-right text-xs">
          <div className="font-mono font-medium text-slate-800">L: {formatCurrency(row.totalTaxLiability)}</div>
          <div className="font-mono text-emerald-700 text-[11px]">ITC: {formatCurrency(row.totalItcClaimed)}</div>
        </div>
      ),
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
          <button
            onClick={() => {
              setSelectedFiling(row);
              setDetailTab('WORKFLOW');
              setDetailModalOpen(true);
              loadFilingDetail(row.id);
            }}
            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
          >
            <Eye className="w-3.5 h-3.5" /> Detail
          </button>

          {row.filingStatus !== 'FILED' ? (
            <button
              onClick={() => {
                setSelectedFiling(row);
                setArnNumber(row.acknowledgementNumber || '');
                setIsFilingModalOpen(true);
              }}
              className="px-2.5 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-2xs"
            >
              <Send className="w-3 h-3" /> Record ARN
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
            End-to-end GST return preparation, document checklists, review workflows, and portal filing sync.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
          <Link to="/gst/migration" className="w-full sm:w-auto">
            <Button variant="outline" className="w-full justify-center text-xs" leftIcon={<Sparkles className="w-4 h-4 text-brand-600" />}>
              📥 Bulk GST Migration
            </Button>
          </Link>
          <Button
            variant="outline"
            onClick={() => setIsBatchModalOpen(true)}
            className="w-full sm:w-auto justify-center text-xs"
            leftIcon={<Clock className="w-4 h-4 text-indigo-600" />}
          >
            ⚡ Batch Schedule Filings
          </Button>
          <Button onClick={() => setIsNewFilingModalOpen(true)} className="w-full sm:w-auto justify-center text-xs" leftIcon={<Plus className="w-4 h-4" />}>
            New Return Filing
          </Button>
        </div>
      </div>

      {/* Metrics Bar */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-2xs">
          <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Total Filings</div>
          <div className="text-xl font-black text-slate-900 mt-1">{totalCount}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-2xs">
          <div className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">Pending Preparation</div>
          <div className="text-xl font-black text-amber-700 mt-1">{pendingCount}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-2xs">
          <div className="text-[11px] font-bold text-blue-600 uppercase tracking-wider">Prepared / Under Review</div>
          <div className="text-xl font-black text-blue-700 mt-1">{preparedCount}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-2xs">
          <div className="text-[11px] font-bold text-emerald-600 uppercase tracking-wider">Filed & ARN Recorded</div>
          <div className="text-xl font-black text-emerald-700 mt-1">{filedCount}</div>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-2xs">
          <div className="text-[11px] font-bold text-rose-600 uppercase tracking-wider">Overdue Returns</div>
          <div className="text-xl font-black text-rose-700 mt-1">{overdueCount}</div>
        </div>
      </div>

      {/* Return Type Tab Filter */}
      <div className="border-b border-slate-200 flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
        {['ALL', 'GSTR1', 'GSTR3B', 'CMP08', 'GSTR9'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={clsx(
              'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 whitespace-nowrap',
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

      {/* ========================================================================= */}
      {/* 1. GST FILING DETAIL MODAL (5 TABS)                                       */}
      {/* ========================================================================= */}
      <Modal
        isOpen={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        title={
          selectedFiling
            ? `${selectedFiling.returnType} (${selectedFiling.returnPeriod}) — ${selectedFiling.clientName || 'GST Return'}`
            : 'GST Return Filing Details'
        }
        subtitle={
          selectedFiling
            ? `GSTIN: ${selectedFiling.gstin || 'N/A'} | Statutory Due Date: ${selectedFiling.dueDate}`
            : ''
        }
      >
        {selectedFiling && (
          <div className="space-y-4 text-xs">
            {/* Modal Tabs */}
            <div className="flex border-b border-slate-200 gap-2 overflow-x-auto no-scrollbar pb-1">
              {[
                { id: 'WORKFLOW', label: 'Workflow & Actions' },
                { id: 'OVERVIEW', label: 'Tax Computation' },
                { id: 'TASK', label: 'Operational Task' },
                { id: 'DOC_REQUEST', label: 'Document Checklist' },
                { id: 'DOCUMENTS', label: 'Document Vault' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setDetailTab(tab.id as any)}
                  className={clsx(
                    'px-3 py-2 text-xs font-bold border-b-2 transition-all shrink-0 whitespace-nowrap',
                    detailTab === tab.id
                      ? 'border-brand-600 text-brand-600'
                      : 'border-transparent text-slate-500 hover:text-slate-700'
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {/* TAB 1: WORKFLOW & ACTIONS */}
            {detailTab === 'WORKFLOW' && (
              <div className="space-y-4">
                {/* Stepper */}
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                  <div className="text-[11px] font-bold text-slate-600 mb-2 uppercase tracking-wider">Filing Lifecycle Stage</div>
                  <div className="flex items-center justify-between relative overflow-x-auto no-scrollbar pb-1 gap-2">
                    <div className="flex items-center gap-2">
                      <span className={clsx(
                        'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
                        selectedFiling.filingStatus === 'PENDING' ? 'bg-amber-500 text-white' : 'bg-emerald-600 text-white'
                      )}>
                        {selectedFiling.filingStatus === 'PENDING' ? '1' : <Check className="w-3.5 h-3.5" />}
                      </span>
                      <span className="font-bold text-slate-800">Pending</span>
                    </div>

                    <ArrowRight className="w-4 h-4 text-slate-400" />

                    <div className="flex items-center gap-2">
                      <span className={clsx(
                        'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
                        selectedFiling.filingStatus === 'PREPARED' ? 'bg-blue-500 text-white' :
                        ['UNDER_REVIEW', 'FILED'].includes(selectedFiling.filingStatus) ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-600'
                      )}>
                        {['UNDER_REVIEW', 'FILED'].includes(selectedFiling.filingStatus) ? <Check className="w-3.5 h-3.5" /> : '2'}
                      </span>
                      <span className="font-bold text-slate-800">Prepared</span>
                    </div>

                    <ArrowRight className="w-4 h-4 text-slate-400" />

                    <div className="flex items-center gap-2">
                      <span className={clsx(
                        'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
                        selectedFiling.filingStatus === 'UNDER_REVIEW' ? 'bg-purple-500 text-white' :
                        selectedFiling.filingStatus === 'FILED' ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-600'
                      )}>
                        {selectedFiling.filingStatus === 'FILED' ? <Check className="w-3.5 h-3.5" /> : '3'}
                      </span>
                      <span className="font-bold text-slate-800">Under Review</span>
                    </div>

                    <ArrowRight className="w-4 h-4 text-slate-400" />

                    <div className="flex items-center gap-2">
                      <span className={clsx(
                        'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
                        selectedFiling.filingStatus === 'FILED' ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-600'
                      )}>
                        4
                      </span>
                      <span className="font-bold text-slate-800">Filed</span>
                    </div>
                  </div>
                </div>

                {/* Workflow Actions Based on Current State */}
                <div className="bg-white p-3.5 rounded-lg border border-slate-200 space-y-3">
                  <div className="font-bold text-slate-900">Available Lifecycle Actions</div>

                  <div className="flex flex-wrap gap-2">
                    {selectedFiling.filingStatus === 'PENDING' && (
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => handleUpdateStatus('PREPARED')}
                        isLoading={isSubmitting}
                      >
                        ✓ Mark as Prepared
                      </Button>
                    )}

                    {selectedFiling.filingStatus === 'PREPARED' && (
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => handleUpdateStatus('UNDER_REVIEW')}
                        isLoading={isSubmitting}
                      >
                        🚀 Submit for Partner Review
                      </Button>
                    )}

                    {selectedFiling.filingStatus === 'UNDER_REVIEW' && (
                      <>
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => {
                            setArnNumber(selectedFiling.acknowledgementNumber || '');
                            setIsFilingModalOpen(true);
                          }}
                        >
                          🏛️ Ready to File — Record ARN
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            const reason = prompt('Enter rework comments for the associate:');
                            if (reason) handleUpdateStatus('PREPARED', reason);
                          }}
                        >
                          🔄 Request Rework
                        </Button>
                      </>
                    )}

                    {selectedFiling.filingStatus !== 'FILED' && selectedFiling.filingStatus !== 'UNDER_REVIEW' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setArnNumber(selectedFiling.acknowledgementNumber || '');
                          setIsFilingModalOpen(true);
                        }}
                      >
                        <Send className="w-3.5 h-3.5 mr-1" /> Direct Record ARN
                      </Button>
                    )}
                  </div>
                </div>

                {/* Filed Details */}
                {selectedFiling.filingStatus === 'FILED' && (
                  <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-3.5 space-y-2">
                    <div className="flex items-center gap-2 text-emerald-800 font-bold">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      <span>Filing Completed & Synchronized</span>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-xs">
                      <div>
                        <span className="text-slate-500 block">ARN / Ack Number:</span>
                        <span className="font-mono font-bold text-slate-900">{selectedFiling.acknowledgementNumber || 'N/A'}</span>
                      </div>
                      <div>
                        <span className="text-slate-500 block">Filing Date:</span>
                        <span className="font-mono font-bold text-slate-900">{selectedFiling.filingDate || 'N/A'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* TAB 2: OVERVIEW & TAX COMPUTATION */}
            {detailTab === 'OVERVIEW' && (
              <div className="space-y-3">
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">Taxable Turnover</div>
                    <div className="font-mono font-bold text-slate-900 text-sm mt-0.5">{formatCurrency(selectedFiling.totalTaxableValue)}</div>
                  </div>
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">Output Tax Liability</div>
                    <div className="font-mono font-bold text-slate-900 text-sm mt-0.5">{formatCurrency(selectedFiling.totalTaxLiability)}</div>
                  </div>
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">ITC Claimed</div>
                    <div className="font-mono font-bold text-emerald-700 text-sm mt-0.5">{formatCurrency(selectedFiling.totalItcClaimed)}</div>
                  </div>
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">Tax Paid in Cash</div>
                    <div className="font-mono font-bold text-slate-900 text-sm mt-0.5">{formatCurrency(selectedFiling.taxPaidCash)}</div>
                  </div>
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">Tax Paid via ITC</div>
                    <div className="font-mono font-bold text-slate-900 text-sm mt-0.5">{formatCurrency(selectedFiling.taxPaidItc)}</div>
                  </div>
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold">Assigned Associate</div>
                    <div className="font-bold text-slate-900 text-xs mt-0.5">{selectedFiling.assignedEmployeeName || 'Unassigned'}</div>
                  </div>
                </div>

                {selectedFiling.notes && (
                  <div className="bg-slate-50 p-2.5 rounded border border-slate-200">
                    <div className="text-slate-500 text-[10px] uppercase font-bold mb-1">Practitioner Notes</div>
                    <p className="text-slate-700">{selectedFiling.notes}</p>
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: OPERATIONAL TASK */}
            {detailTab === 'TASK' && (
              <div className="space-y-3">
                {selectedFiling.taskId ? (
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="font-bold text-slate-900 text-xs">Linked Preparation Task</div>
                      <span className="px-2 py-0.5 bg-brand-100 text-brand-800 text-[10px] font-bold rounded">
                        {selectedFiling.taskStatus || 'IN_PROGRESS'}
                      </span>
                    </div>
                    <div className="text-slate-700 text-xs">{selectedFiling.taskTitle || 'GST Return Preparation Deliverable'}</div>
                    <div className="flex items-center justify-between pt-2 border-t border-slate-200 text-[11px]">
                      <span className="text-slate-500">Task ID: {selectedFiling.taskId}</span>
                      <Link to="/tasks" className="text-brand-600 font-bold hover:underline inline-flex items-center gap-1">
                        Open in Task Manager <ExternalLink className="w-3 h-3" />
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="bg-slate-50 p-4 rounded-lg border border-dashed border-slate-300 text-center space-y-2">
                    <CheckSquare className="w-8 h-8 text-slate-400 mx-auto" />
                    <div className="text-xs font-semibold text-slate-700">No operational task linked yet</div>
                    <p className="text-[11px] text-slate-500">Auto-create a task to assign this filing deliverable to your team.</p>
                    <Button
                      size="sm"
                      onClick={() => handleCreateTaskForFiling(selectedFiling.id)}
                      isLoading={isSubmitting}
                    >
                      + Generate & Link Task
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* TAB 4: DOCUMENT CHECKLIST */}
            {detailTab === 'DOC_REQUEST' && (
              <div className="space-y-3">
                {selectedFiling.documentRequestId ? (
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="font-bold text-slate-900 text-xs">Document Request: {selectedFiling.documentRequestNumber || 'Active'}</div>
                      <span className="px-2 py-0.5 bg-indigo-100 text-indigo-800 text-[10px] font-bold rounded">
                        {selectedFiling.documentRequestStatus || 'SENT'}
                      </span>
                    </div>
                    <div className="text-xs text-slate-600">
                      Checklist Progress: <span className="font-bold text-slate-900">{selectedFiling.documentRequestReceivedCount ?? 0}</span> of <span className="font-bold text-slate-900">{selectedFiling.documentRequestItemsCount ?? 0}</span> documents uploaded.
                    </div>
                    <div className="pt-2 border-t border-slate-200">
                      <Link to="/document-requests" className="text-brand-600 font-bold hover:underline inline-flex items-center gap-1 text-[11px]">
                        Manage Checklist in Document Requests Hub <ExternalLink className="w-3 h-3" />
                      </Link>
                    </div>
                  </div>
                ) : (
                  <div className="bg-slate-50 p-4 rounded-lg border border-dashed border-slate-300 text-center space-y-2">
                    <Inbox className="w-8 h-8 text-slate-400 mx-auto" />
                    <div className="text-xs font-semibold text-slate-700">No document request checklist active</div>
                    <p className="text-[11px] text-slate-500">Request Sales Registers, Purchase Bills, and 2B from the client portal.</p>
                    <Button
                      size="sm"
                      onClick={() => setIsDocReqModalOpen(true)}
                    >
                      + Request Supporting Documents
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* TAB 5: DOCUMENT VAULT */}
            {detailTab === 'DOCUMENTS' && (
              <div className="space-y-3">
                {isLoadingDocs ? (
                  <div className="text-center py-4 text-slate-500">Loading documents...</div>
                ) : filingDocs.length > 0 ? (
                  <div className="space-y-1.5">
                    {filingDocs.map((doc: any) => (
                      <div key={doc.id} className="flex items-center justify-between p-2 bg-slate-50 rounded border border-slate-200">
                        <div className="flex items-center gap-2">
                          <FileText className="w-4 h-4 text-brand-600" />
                          <span className="font-medium text-slate-800">{doc.fileName || doc.documentName}</span>
                        </div>
                        <span className="text-[10px] text-slate-500 font-mono">{(doc.fileSize / 1024).toFixed(1)} KB</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="bg-slate-50 p-4 rounded-lg border border-dashed border-slate-300 text-center space-y-2">
                    <FileText className="w-8 h-8 text-slate-400 mx-auto" />
                    <div className="text-xs font-semibold text-slate-700">No documents stored in vault yet</div>
                    <p className="text-[11px] text-slate-500">Documents uploaded by clients or associates will appear here.</p>
                  </div>
                )}
              </div>
            )}

            <div className="flex justify-end pt-3 border-t border-slate-200">
              <Button variant="outline" size="sm" onClick={() => setDetailModalOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* ========================================================================= */}
      {/* 2. RECORD FILING / ARN MODAL                                              */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isFilingModalOpen}
        onClose={() => setIsFilingModalOpen(false)}
        title="Record GST Portal Filing (ARN)"
        subtitle={`Filing ${selectedFiling?.returnType} for period ${selectedFiling?.returnPeriod} (${selectedFiling?.gstin})`}
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              ARN / Acknowledgement Reference Number <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. AA2708260012345"
              value={arnNumber}
              onChange={(e) => setArnNumber(e.target.value)}
              className="w-full font-mono uppercase px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            />
            <p className="text-[10px] text-slate-500 mt-1">
              Obtained from the official Government GST Portal (services.gst.gov.in) upon successful submission.
            </p>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Actual Filing Date</label>
            <input
              type="date"
              value={filingDate}
              onChange={(e) => setFilingDate(e.target.value)}
              className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            />
          </div>

          <div className="p-3 bg-brand-50 border border-brand-200 rounded-lg text-brand-800 text-[11px] flex items-start gap-2">
            <ShieldCheck className="w-4 h-4 text-brand-600 shrink-0 mt-0.5" />
            <div>
              <strong>Automatic Multi-Module Synchronization:</strong> Recording the ARN marks this GST return as{' '}
              <span className="font-bold">FILED</span>, automatically marks the statutory Compliance Obligation as{' '}
              <span className="font-bold">COMPLETED</span>, and resolves any linked operational Task as{' '}
              <span className="font-bold">COMPLETED</span>.
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsFilingModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleRecordFiling} isLoading={isSubmitting} leftIcon={<Send className="w-3.5 h-3.5" />}>
              Save & Mark Filed
            </Button>
          </div>
        </div>
      </Modal>

      {/* ========================================================================= */}
      {/* 3. REQUEST DOCUMENTS MODAL                                                */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isDocReqModalOpen}
        onClose={() => setIsDocReqModalOpen(false)}
        title="Request Supporting Documents from Client"
        subtitle={`Checklist for ${selectedFiling?.returnType} (${selectedFiling?.returnPeriod})`}
      >
        <div className="space-y-3 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Request Purpose / Notes</label>
            <input
              type="text"
              placeholder={`GST ${selectedFiling?.returnType} ${selectedFiling?.returnPeriod} Supporting Documents`}
              value={docReqPurpose}
              onChange={(e) => setDocReqPurpose(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            />
          </div>

          <div className="space-y-2">
            <label className="block font-semibold text-slate-700">Checklist Items to Request:</label>
            <label className="flex items-center gap-2 p-2 bg-slate-50 rounded border border-slate-200 cursor-pointer">
              <input type="checkbox" checked={includeSales} onChange={(e) => setIncludeSales(e.target.checked)} className="rounded text-brand-600" />
              <span className="font-medium text-slate-800">Sales Register / Outward Invoices</span>
            </label>
            <label className="flex items-center gap-2 p-2 bg-slate-50 rounded border border-slate-200 cursor-pointer">
              <input type="checkbox" checked={includePurchase} onChange={(e) => setIncludePurchase(e.target.checked)} className="rounded text-brand-600" />
              <span className="font-medium text-slate-800">Purchase Register / Inward Bills</span>
            </label>
            <label className="flex items-center gap-2 p-2 bg-slate-50 rounded border border-slate-200 cursor-pointer">
              <input type="checkbox" checked={include2b} onChange={(e) => setInclude2b(e.target.checked)} className="rounded text-brand-600" />
              <span className="font-medium text-slate-800">GSTR-2B Statement / Excel Export</span>
            </label>
            <label className="flex items-center gap-2 p-2 bg-slate-50 rounded border border-slate-200 cursor-pointer">
              <input type="checkbox" checked={includeBank} onChange={(e) => setIncludeBank(e.target.checked)} className="rounded text-brand-600" />
              <span className="font-medium text-slate-800">Bank Statement for Period</span>
            </label>
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsDocReqModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateDocRequest} isLoading={isSubmitting} leftIcon={<Inbox className="w-3.5 h-3.5" />}>
              Send Request to Client Portal
            </Button>
          </div>
        </div>
      </Modal>

      {/* ========================================================================= */}
      {/* 4. BATCH GENERATION MODAL                                                 */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isBatchModalOpen}
        onClose={() => setIsBatchModalOpen(false)}
        title="Batch Schedule GST Return Filings"
        subtitle="Auto-generate return filings and compliance obligations for all active clients"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Target Return Type</label>
            <select
              value={batchReturnType}
              onChange={(e: any) => setBatchReturnType(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            >
              <option value="GSTR3B">GSTR-3B (Monthly Summary Return)</option>
              <option value="GSTR1">GSTR-1 (Outward Supplies Return)</option>
              <option value="CMP08">CMP-08 (Composition Scheme Statement)</option>
              <option value="GSTR9">GSTR-9 (Annual Return)</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Period (YYYY-MM)</label>
              <input
                type="text"
                placeholder="2026-07"
                value={batchPeriod}
                onChange={(e) => setBatchPeriod(e.target.value)}
                className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Financial Year</label>
              <input
                type="text"
                placeholder="2026-27"
                value={batchFy}
                onChange={(e) => setBatchFy(e.target.value)}
                className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-700 mb-1">Statutory Due Date</label>
            <input
              type="date"
              value={batchDueDate}
              onChange={(e) => setBatchDueDate(e.target.value)}
              className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsBatchModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleExecuteBatchGenerate} isLoading={isSubmitting} leftIcon={<Clock className="w-3.5 h-3.5" />}>
              Schedule for {profiles.length} Active Clients
            </Button>
          </div>
        </div>
      </Modal>

      {/* ========================================================================= */}
      {/* 5. INDIVIDUAL NEW FILING MODAL                                            */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isNewFilingModalOpen}
        onClose={() => setIsNewFilingModalOpen(false)}
        title="Schedule Individual GST Filing"
        subtitle="Create a new return filing record for a specific client"
      >
        <div className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Select Client & GSTIN <span className="text-rose-500">*</span>
            </label>
            <select
              value={newProfileId}
              onChange={(e) => setNewProfileId(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
            >
              <option value="">-- Choose Client Profile --</option>
              {profiles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.clientName || p.tradeName || p.legalName} ({p.gstin})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Return Type</label>
              <select
                value={newReturnType}
                onChange={(e: any) => setNewReturnType(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
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
                placeholder="2026-07"
                value={newPeriod}
                onChange={(e) => setNewPeriod(e.target.value)}
                className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Financial Year</label>
              <input
                type="text"
                placeholder="2026-27"
                value={newFy}
                onChange={(e) => setNewFy(e.target.value)}
                className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={newDueDate}
                onChange={(e) => setNewDueDate(e.target.value)}
                className="w-full font-mono px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>

          <label className="flex items-center gap-2 p-2 bg-brand-50 border border-brand-200 rounded-lg cursor-pointer">
            <input
              type="checkbox"
              checked={newCreateTask}
              onChange={(e) => setNewCreateTask(e.target.checked)}
              className="rounded text-brand-600"
            />
            <span className="font-semibold text-brand-900 text-xs">
              Automatically create linked preparation task in Task Management
            </span>
          </label>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsNewFilingModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateIndividualFiling} isLoading={isSubmitting} leftIcon={<Plus className="w-3.5 h-3.5" />}>
              Create Filing
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
