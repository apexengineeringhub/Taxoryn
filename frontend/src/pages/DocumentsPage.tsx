import React, { useState, useEffect } from 'react';
import {
  FolderLock,
  Upload,
  Download,
  FileText,
  File,
  HardDrive,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Calendar,
  Eye,
  Plus,
  Send,
  Sparkles,
  Filter,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { documentApi, documentRequestApi, clientApi } from '../api/endpoints';
import { DocumentItem, DocumentRequest, DocumentRequestSummary, Client } from '../types';
import { DocumentRequestReviewModal } from '../components/docrequest/DocumentRequestReviewModal';
import { RequestDocumentsModal } from '../components/docrequest/RequestDocumentsModal';
import clsx from 'clsx';

export const DocumentsPage: React.FC = () => {
  const [activeMainTab, setActiveMainTab] = useState<'requests' | 'vault'>('requests');

  // Vault state
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [category, setCategory] = useState('ALL');
  const [isLoadingVault, setIsLoadingVault] = useState(true);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadCategory, setUploadCategory] = useState('GST');
  const [isUploading, setIsUploading] = useState(false);

  // Document Requests Workflow state
  const [requests, setRequests] = useState<DocumentRequest[]>([]);
  const [stats, setStats] = useState<DocumentRequestSummary | null>(null);
  const [isLoadingRequests, setIsLoadingRequests] = useState(true);
  const [selectedReviewRequest, setSelectedReviewRequest] = useState<DocumentRequest | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Create Request Modal state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isBuilderModalOpen, setIsBuilderModalOpen] = useState(false);
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClientIdForCreate, setSelectedClientIdForCreate] = useState<string>('');

  useEffect(() => {
    if (activeMainTab === 'vault') {
      loadDocuments();
    } else {
      loadRequests();
      loadStats();
    }
  }, [activeMainTab, category, statusFilter]);

  useEffect(() => {
    loadClientsList();
  }, []);

  const loadClientsList = async () => {
    try {
      const res = await clientApi.getAll({ size: 100 });
      setClients(res.content || []);
      if (res.content && res.content.length > 0) {
        setSelectedClientIdForCreate(res.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load clients', err);
    }
  };

  const loadDocuments = async () => {
    try {
      setIsLoadingVault(true);
      const params: any = {};
      if (category !== 'ALL') params.category = category;
      const res = await documentApi.getAll(params);
      setDocuments(res.content || []);
    } catch (err) {
      console.error('Failed to load documents', err);
    } finally {
      setIsLoadingVault(false);
    }
  };

  const loadRequests = async () => {
    try {
      setIsLoadingRequests(true);
      const params: any = { size: 50 };
      if (statusFilter !== 'ALL') params.status = statusFilter;
      const res = await documentRequestApi.getAll(params);
      setRequests(res.content || []);
    } catch (err) {
      console.error('Failed to load document requests', err);
    } finally {
      setIsLoadingRequests(false);
    }
  };

  const loadStats = async () => {
    try {
      const data = await documentRequestApi.getSummaryStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to load summary stats', err);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      await documentApi.upload(selectedFile, {
        category: uploadCategory,
      });
      setIsUploadModalOpen(false);
      setSelectedFile(null);
      loadDocuments();
    } catch (err: any) {
      alert(`Upload failed: ${err?.response?.data?.message || err?.message || 'Upload failed'}`);
    } finally {
      setIsUploading(false);
    }
  };

  const formatFileSize = (bytes: number = 0) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="px-2.5 py-0.5 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">Completed</span>;
      case 'PARTIALLY_COMPLETED':
        return <span className="px-2.5 py-0.5 text-xs font-bold bg-blue-100 text-blue-800 rounded-full">Partially Uploaded</span>;
      case 'SENT':
        return <span className="px-2.5 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">Pending Upload</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 text-xs font-bold bg-slate-100 text-slate-700 rounded-full">Cancelled</span>;
      default:
        return <span className="px-2.5 py-0.5 text-xs font-bold bg-slate-100 text-slate-700 rounded-full">{status}</span>;
    }
  };

  const handleSendReminder = async (requestId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await documentRequestApi.sendReminder(requestId);
      alert('Follow-up reminder sent to client successfully!');
    } catch (err: any) {
      alert(`Failed to send reminder: ${err.response?.data?.message || err.message}`);
    }
  };

  const getDueBadge = (dueDate?: string, isOverdue?: boolean) => {
    if (!dueDate) return <span className="text-slate-400 text-xs">—</span>;
    const due = new Date(dueDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    due.setHours(0, 0, 0, 0);
    const diffDays = Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays < 0 || isOverdue) {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-black bg-rose-50 text-rose-700 border border-rose-200 rounded-full mt-0.5">
          <AlertTriangle className="w-2.5 h-2.5" />
          Overdue ({Math.abs(diffDays)}d)
        </span>
      );
    }
    if (diffDays === 0) {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-black bg-amber-50 text-amber-700 border border-amber-200 rounded-full mt-0.5">
          <Clock className="w-2.5 h-2.5" />
          Due Today
        </span>
      );
    }
    if (diffDays <= 3) {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-black bg-blue-50 text-blue-700 border border-blue-200 rounded-full mt-0.5">
          <Clock className="w-2.5 h-2.5" />
          Due in {diffDays}d
        </span>
      );
    }
    return (
      <span className="text-slate-600 text-xs font-semibold block">
        {dueDate}
      </span>
    );
  };

  const requestColumns: Column<DocumentRequest>[] = [
    {
      header: 'Request Reference',
      accessor: (row) => (
        <div>
          <span className="font-mono font-bold text-xs bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200 block w-fit">
            {row.requestNumber}
          </span>
          <span className="text-[11px] text-slate-400 mt-0.5 block">
            {new Date(row.createdAt).toLocaleDateString()}
          </span>
        </div>
      ),
    },
    {
      header: 'Client & Purpose',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.clientName}</span>
          <span className="text-xs text-slate-600 font-medium">{row.purpose}</span>
          {row.financialYear && (
            <span className="text-[10px] text-slate-400 block font-mono">FY {row.financialYear}</span>
          )}
        </div>
      ),
    },
    {
      header: 'Due Date & Timing',
      accessor: (row) => (
        <div className="space-y-0.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            <span>{row.dueDate || 'No Due Date'}</span>
          </div>
          {getDueBadge(row.dueDate, row.isOverdue)}
        </div>
      ),
    },
    {
      header: 'Verification Progress',
      accessor: (row) => {
        const percent = row.totalItems > 0 ? Math.round((row.acceptedItems / row.totalItems) * 100) : 0;
        return (
          <div className="w-40 space-y-1">
            <div className="flex justify-between text-[11px] font-bold text-slate-700">
              <span>{row.acceptedItems} / {row.totalItems} Verified</span>
              <span className="font-mono text-emerald-700">{percent}%</span>
            </div>
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${percent}%` }} />
            </div>
            <div className="text-[10px] text-slate-400">
              {row.uploadedItems} In-Review • {row.pendingItems} Pending
            </div>
          </div>
        );
      },
    },
    {
      header: 'Status',
      accessor: (row) => getStatusBadge(row.status),
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-2">
          {(row.status === 'SENT' || row.status === 'PARTIALLY_COMPLETED') && (
            <button
              onClick={(e) => handleSendReminder(row.id, e)}
              title="Send Follow-up Reminder"
              className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-bold text-brand-700 bg-brand-50 border border-brand-200 hover:bg-brand-100 rounded-xl transition-colors shadow-2xs"
            >
              <Send className="w-3 h-3 text-brand-600" />
              <span>Remind</span>
            </button>
          )}
          <button
            onClick={() => setSelectedReviewRequest(row)}
            className="inline-flex items-center space-x-1 px-3 py-1.5 text-xs font-bold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-xl transition-colors shadow-2xs"
          >
            <Eye className="w-3.5 h-3.5 text-slate-500" />
            <span>Review</span>
          </button>
        </div>
      ),
    },
  ];

  const handleDownloadVaultDoc = async (id: string, fileName?: string) => {
    try {
      const blob = await documentApi.download(id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'document.pdf';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: any) {
      alert(`Failed to download document: ${err?.response?.data?.message || err?.message || 'Download failed'}`);
    }
  };

  const vaultColumns: Column<DocumentItem>[] = [
    {
      header: 'Document Name',
      accessor: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs shrink-0">
            <FileText className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <span className="font-bold text-slate-900 block break-all">{row.originalFilename || (row as any).fileName}</span>
            <span className="text-[10px] text-slate-400 block truncate">{row.clientName || 'General Document'}</span>
          </div>
        </div>
      ),
    },
    {
      header: 'Category',
      accessor: (row) => (
        <span className="font-bold text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded border border-slate-200 uppercase">
          {row.category || (row as any).documentType}
        </span>
      ),
    },
    {
      header: 'Size',
      accessor: (row) => <span className="font-mono text-xs text-slate-600">{formatFileSize(row.fileSize)}</span>,
    },
    {
      header: 'Uploaded On',
      accessor: (row) => <span className="text-xs text-slate-500">{new Date(row.createdAt).toLocaleDateString()}</span>,
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <button
          onClick={() => handleDownloadVaultDoc(row.id, row.originalFilename || (row as any).fileName)}
          className="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded-md inline-flex items-center gap-1 text-xs font-semibold cursor-pointer"
          title="Download Document"
        >
          <Download className="w-4 h-4" />
        </button>
      ),
    },
  ];

  const selectedClientObj = clients.find((c) => c.id === selectedClientIdForCreate);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 flex items-center gap-2.5">
            <span>Documents & Client Requests</span>
            <span className="text-xs bg-emerald-100 text-emerald-800 font-extrabold px-2.5 py-0.5 rounded-full border border-emerald-300">
              V1 Suite
            </span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Request, track, verify, and store statutory tax documents with end-to-end client workflow.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          {activeMainTab === 'requests' ? (
            <Button
              onClick={() => setIsCreateModalOpen(true)}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Request Documents
            </Button>
          ) : (
            <Button
              onClick={() => setIsUploadModalOpen(true)}
              leftIcon={<Upload className="w-4 h-4" />}
            >
              Upload Document
            </Button>
          )}
        </div>
      </div>

      {/* Main Switcher Tabs */}
      <div className="flex border-b border-slate-200 overflow-x-auto no-scrollbar pb-1">
        <button
          onClick={() => setActiveMainTab('requests')}
          className={clsx(
            'px-5 py-3 text-xs font-bold border-b-2 transition-all flex items-center gap-2 shrink-0 whitespace-nowrap',
            activeMainTab === 'requests'
              ? 'border-emerald-600 text-emerald-800 bg-emerald-50/50 rounded-t-xl'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          )}
        >
          <FileText className="w-4 h-4 text-emerald-600" />
          <span>Client Document Requests (Workflow)</span>
          {stats && (
            <span className="bg-emerald-100 text-emerald-800 text-[11px] font-extrabold px-2 py-0.2 rounded-full">
              {stats.totalRequests}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveMainTab('vault')}
          className={clsx(
            'px-5 py-3 text-xs font-bold border-b-2 transition-all flex items-center gap-2 shrink-0 whitespace-nowrap',
            activeMainTab === 'vault'
              ? 'border-emerald-600 text-emerald-800 bg-emerald-50/50 rounded-t-xl'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          )}
        >
          <FolderLock className="w-4 h-4 text-slate-500" />
          <span>Document Vault (Stored Files)</span>
        </button>
      </div>

      {/* VIEW 1: Document Requests Workflow */}
      {activeMainTab === 'requests' && (
        <div className="space-y-6">
          {/* Summary Metric Cards */}
          {stats && (
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3.5">
              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-[11px] font-bold text-slate-500 uppercase">Total Requests</span>
                <p className="text-2xl font-black text-slate-900 mt-1">{stats.totalRequests}</p>
                <span className="text-[10px] text-slate-400">All client checklists</span>
              </div>

              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-[11px] font-bold text-amber-600 uppercase">Pending Upload</span>
                <p className="text-2xl font-black text-amber-700 mt-1">{stats.pendingRequests}</p>
                <span className="text-[10px] text-slate-400">Awaiting client action</span>
              </div>

              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-[11px] font-bold text-blue-600 uppercase">Under Review</span>
                <p className="text-2xl font-black text-blue-700 mt-1">{stats.partiallyCompletedRequests}</p>
                <span className="text-[10px] text-slate-400">Needs practitioner review</span>
              </div>

              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-[11px] font-bold text-emerald-600 uppercase">Completed</span>
                <p className="text-2xl font-black text-emerald-700 mt-1">{stats.completedRequests}</p>
                <span className="text-[10px] text-slate-400">All required accepted</span>
              </div>

              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
                <span className="text-[11px] font-bold text-rose-600 uppercase">Overdue</span>
                <p className="text-2xl font-black text-rose-700 mt-1">{stats.overdueRequests}</p>
                <span className="text-[10px] text-slate-400">Passed due date</span>
              </div>
            </div>
          )}

          {/* Status Filter Chips */}
          <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
            <span className="text-xs font-bold text-slate-500 flex items-center gap-1 shrink-0">
              <Filter className="w-3.5 h-3.5" /> Filter:
            </span>
            {['ALL', 'SENT', 'PARTIALLY_COMPLETED', 'COMPLETED', 'CANCELLED'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={clsx(
                  'px-3 py-1 text-xs font-bold rounded-lg transition-all shrink-0 whitespace-nowrap',
                  statusFilter === st
                    ? 'bg-slate-900 text-white shadow-2xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                {st === 'ALL' ? 'All Requests' : st.replace('_', ' ')}
              </button>
            ))}
          </div>

          {/* Requests Table */}
          <DataTable
            columns={requestColumns}
            data={requests}
            isLoading={isLoadingRequests}
            searchPlaceholder="Search requests by purpose, client, or reference..."
          />
        </div>
      )}

      {/* VIEW 2: Document Vault */}
      {activeMainTab === 'vault' && (
        <div className="space-y-6">
          {/* Category Filter */}
          <div className="border-b border-slate-200 flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
            {['ALL', 'GST', 'ITR', 'FINANCIAL_STATEMENTS', 'KYC'].map((cat) => (
              <button
                key={cat}
                onClick={() => setCategory(cat)}
                className={clsx(
                  'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 whitespace-nowrap',
                  category === cat
                    ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
                    : 'border-transparent text-slate-500 hover:text-slate-700'
                )}
              >
                {cat.replace('_', ' ')}
              </button>
            ))}
          </div>

          <DataTable
            columns={vaultColumns}
            data={documents}
            isLoading={isLoadingVault}
            searchPlaceholder="Search files by name..."
          />
        </div>
      )}

      {/* Create Request Target Client Picker Modal */}
      {isCreateModalOpen && (
        <Modal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          title="Select Client to Request Documents"
          subtitle="Choose the client for whom you wish to create a document checklist."
        >
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Select Client *</label>
              <select
                value={selectedClientIdForCreate}
                onChange={(e) => setSelectedClientIdForCreate(e.target.value)}
                className="w-full px-3.5 py-2.5 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 bg-white font-medium"
              >
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan}) — {c.clientType}
                  </option>
                ))}
              </select>
            </div>

            <div className="pt-4 flex justify-end gap-2">
              <Button variant="outline" type="button" onClick={() => setIsCreateModalOpen(false)}>
                Cancel
              </Button>
              <Button
                type="button"
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setIsBuilderModalOpen(true);
                }}
              >
                Proceed to Checklist Builder →
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Full Request Builder Modal */}
      {selectedClientIdForCreate && isBuilderModalOpen && (
        <RequestDocumentsModal
          isOpen={isBuilderModalOpen}
          onClose={() => setIsBuilderModalOpen(false)}
          clientId={selectedClientIdForCreate}
          clientName={selectedClientObj?.displayName || 'Client'}
          onSuccess={() => {
            loadRequests();
            loadStats();
          }}
        />
      )}

      {/* Review Modal */}
      {selectedReviewRequest && (
        <DocumentRequestReviewModal
          isOpen={!!selectedReviewRequest}
          onClose={() => setSelectedReviewRequest(null)}
          request={selectedReviewRequest}
          onUpdate={() => {
            loadRequests();
            loadStats();
          }}
        />
      )}

      {/* Vault Upload Modal */}
      <Modal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        title="Upload Practice Document"
        subtitle="Supported formats: PDF, XLSX, ZIP, PNG (Max 25MB)"
      >
        <form onSubmit={handleUpload} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Select File *</label>
            <input
              type="file"
              required
              onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              className="w-full text-xs file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
            <select
              value={uploadCategory}
              onChange={(e) => setUploadCategory(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            >
              <option value="GST">GST Filings & Registers</option>
              <option value="ITR">ITR Computations & Form 16</option>
              <option value="FINANCIAL_STATEMENTS">Balance Sheet & P&L</option>
              <option value="KYC">KYC & Registration Certificates</option>
            </select>
          </div>

          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" type="button" onClick={() => setIsUploadModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isUploading}>
              Upload
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};