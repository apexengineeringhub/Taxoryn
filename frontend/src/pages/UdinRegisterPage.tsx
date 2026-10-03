import React, { useEffect, useState, useMemo } from 'react';
import {
  FileText,
  Plus,
  Search,
  RefreshCw,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
  Building2,
  User,
  Filter,
  Calendar,
  ShieldCheck,
  Ban,
  Edit2,
  Trash2,
  Copy,
  ExternalLink,
  Info,
  Layers,
  Award,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { udinApi, clientApi } from '../api/endpoints';
import {
  UdinDto,
  UdinStatusType,
  UdinVerificationStatusType,
  UdinDocumentType,
  UdinSummaryDto,
  CreateUdinRequest,
  UpdateUdinRequest,
  UpdateUdinVerificationRequest,
  Client,
} from '../types';
import { useAuth } from '../context/AuthContext';

const DOCUMENT_TYPES: { value: UdinDocumentType; label: string; group: string }[] = [
  { value: 'TAX_AUDIT_REPORT_3CA_3CD', label: 'Tax Audit Report (Form 3CA / 3CD)', group: 'Direct Tax' },
  { value: 'TAX_AUDIT_REPORT_3CB_3CD', label: 'Tax Audit Report (Form 3CB / 3CD)', group: 'Direct Tax' },
  { value: 'FORM_15CB_CERTIFICATION', label: 'Form 15CB Foreign Remittance Cert', group: 'Direct Tax' },
  { value: 'TRANSFER_PRICING_REPORT', label: 'Transfer Pricing Report (Form 3CEB)', group: 'Direct Tax' },
  { value: 'GST_AUDIT_CERTIFICATE', label: 'GST Audit / Annual Return Cert (GSTR-9C)', group: 'Indirect Tax' },
  { value: 'TURNOVER_CERTIFICATE', label: 'Turnover Certificate', group: 'Certificates' },
  { value: 'NET_WORTH_CERTIFICATE', label: 'Net Worth Certificate', group: 'Certificates' },
  { value: 'STATUTORY_AUDIT_REPORT', label: 'Statutory Company Audit Report', group: 'Corporate Audit' },
  { value: 'INTERNAL_AUDIT_REPORT', label: 'Internal Audit & Management Report', group: 'Corporate Audit' },
  { value: 'OTHER_CERTIFICATION', label: 'Other Professional Certification', group: 'Other' },
  { value: 'OTHER', label: 'General / Other Document', group: 'Other' },
];

export const UdinRegisterPage: React.FC = () => {
  const { user } = useAuth();
  const [udinList, setUdinList] = useState<UdinDto[]>([]);
  const [summary, setSummary] = useState<UdinSummaryDto | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [verificationFilter, setVerificationFilter] = useState<string>('ALL');
  const [docTypeFilter, setDocTypeFilter] = useState<string>('ALL');
  const [clientFilter, setClientFilter] = useState<string>('ALL');

  // Record / Edit Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUdin, setEditingUdin] = useState<UdinDto | null>(null);
  const [udinNumber, setUdinNumber] = useState('');
  const [selectedClientId, setSelectedClientId] = useState<string>('');
  const [documentType, setDocumentType] = useState<UdinDocumentType>('TAX_AUDIT_REPORT_3CA_3CD');
  const [documentTitle, setDocumentTitle] = useState('');
  const [documentDescription, setDocumentDescription] = useState('');
  const [signatoryName, setSignatoryName] = useState('');
  const [signatoryMembershipNo, setSignatoryMembershipNo] = useState('');
  const [generationDate, setGenerationDate] = useState('');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  // Quick Verification Modal
  const [isVerifyModalOpen, setIsVerifyModalOpen] = useState(false);
  const [verifyingUdin, setVerifyingUdin] = useState<UdinDto | null>(null);
  const [verifyStatus, setVerifyStatus] = useState<UdinVerificationStatusType>('VERIFIED');
  const [verifySource, setVerifySource] = useState('ICAI_PORTAL_MANUAL');
  const [verifyRemarks, setVerifyRemarks] = useState('');
  const [savingVerify, setSavingVerify] = useState(false);

  // Cancel Modal
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancellingUdin, setCancellingUdin] = useState<UdinDto | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [processingCancel, setProcessingCancel] = useState(false);

  // Details Modal
  const [detailUdin, setDetailUdin] = useState<UdinDto | null>(null);

  // Delete State
  const [udinToDelete, setUdinToDelete] = useState<UdinDto | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [udinRes, summaryRes, clientRes] = await Promise.allSettled([
        udinApi.getUdins({ size: 100 }),
        udinApi.getUdinSummary(),
        clientApi.getAll({ page: 0, size: 500 }),
      ]);

      if (udinRes.status === 'fulfilled') {
        setUdinList(udinRes.value.content || []);
      } else {
        setError('Failed to fetch UDIN records.');
      }

      if (summaryRes.status === 'fulfilled') {
        setSummary(summaryRes.value);
      }

      if (clientRes.status === 'fulfilled') {
        setClients(clientRes.value.content || []);
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Error loading UDIN data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateModal = () => {
    setEditingUdin(null);
    setUdinNumber('');
    setSelectedClientId('');
    setDocumentType('TAX_AUDIT_REPORT_3CA_3CD');
    setDocumentTitle('');
    setDocumentDescription('');
    setSignatoryName(user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() : '');
    setSignatoryMembershipNo('');
    setGenerationDate(new Date().toISOString().split('T')[0]);
    setNotes('');
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (udin: UdinDto) => {
    setEditingUdin(udin);
    setUdinNumber(udin.udin);
    setSelectedClientId(udin.clientId || '');
    setDocumentType(udin.documentType);
    setDocumentTitle(udin.documentTitle);
    setDocumentDescription(udin.documentDescription || '');
    setSignatoryName(udin.signatoryName);
    setSignatoryMembershipNo(udin.signatoryMembershipNo || '');
    setGenerationDate(udin.generationDate);
    setNotes(udin.notes || '');
    setFormError('');
    setIsModalOpen(true);
  };

  const openVerifyModal = (udin: UdinDto) => {
    setVerifyingUdin(udin);
    setVerifyStatus(udin.verificationStatus === 'NOT_VERIFIED' ? 'VERIFIED' : udin.verificationStatus);
    setVerifySource(udin.verificationSource || 'ICAI_PORTAL_MANUAL');
    setVerifyRemarks(udin.verificationRemarks || '');
    setIsVerifyModalOpen(true);
  };

  const openCancelModal = (udin: UdinDto) => {
    setCancellingUdin(udin);
    setCancelReason('');
    setIsCancelModalOpen(true);
  };

  const handleSaveUdin = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');

    const cleanUdin = udinNumber.trim().toUpperCase();
    if (!editingUdin) {
      if (!cleanUdin || cleanUdin.length !== 18) {
        setFormError('UDIN must be exactly 18 alphanumeric characters.');
        return;
      }
      if (!/^[A-Z0-9]{18}$/.test(cleanUdin)) {
        setFormError('UDIN must contain only alphanumeric characters without spaces or symbols.');
        return;
      }
    }

    if (!documentTitle.trim()) {
      setFormError('Document title is required.');
      return;
    }

    if (!signatoryName.trim()) {
      setFormError('Signatory name is required.');
      return;
    }

    if (!generationDate) {
      setFormError('Generation date is required.');
      return;
    }

    setSaving(true);
    try {
      if (editingUdin) {
        const payload: UpdateUdinRequest = {
          clientId: selectedClientId || null,
          documentType,
          documentTitle: documentTitle.trim(),
          documentDescription: documentDescription.trim() || null,
          signatoryName: signatoryName.trim(),
          signatoryMembershipNo: signatoryMembershipNo.trim() || null,
          generationDate,
          notes: notes.trim() || null,
        };
        await udinApi.updateUdin(editingUdin.id, payload);
        setSuccessMessage('UDIN details updated successfully.');
      } else {
        const payload: CreateUdinRequest = {
          udin: cleanUdin,
          clientId: selectedClientId || null,
          documentType,
          documentTitle: documentTitle.trim(),
          documentDescription: documentDescription.trim() || null,
          signatoryName: signatoryName.trim(),
          signatoryMembershipNo: signatoryMembershipNo.trim() || null,
          generationDate,
          notes: notes.trim() || null,
        };
        await udinApi.createUdin(payload);
        setSuccessMessage('UDIN record registered successfully.');
      }
      setIsModalOpen(false);
      loadData();
    } catch (err: any) {
      setFormError(err?.response?.data?.message || 'Failed to save UDIN record.');
    } finally {
      setSaving(false);
    }
  };

  const handleSaveVerification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verifyingUdin) return;
    setSavingVerify(true);
    try {
      const payload: UpdateUdinVerificationRequest = {
        verificationStatus: verifyStatus,
        verificationSource: verifySource.trim() || 'MANUAL',
        verificationRemarks: verifyRemarks.trim() || undefined,
      };
      await udinApi.updateVerification(verifyingUdin.id, payload);
      setSuccessMessage(`UDIN verification status updated to ${verifyStatus}.`);
      setIsVerifyModalOpen(false);
      loadData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to update verification status.');
    } finally {
      setSavingVerify(false);
    }
  };

  const handleCancelUdin = async () => {
    if (!cancellingUdin || !cancelReason.trim()) return;
    setProcessingCancel(true);
    try {
      await udinApi.cancelUdin(cancellingUdin.id, { reason: cancelReason.trim() });
      setSuccessMessage(`UDIN ${cancellingUdin.udin} marked as cancelled.`);
      setIsCancelModalOpen(false);
      loadData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to cancel UDIN.');
    } finally {
      setProcessingCancel(false);
    }
  };

  const handleDeleteUdin = async () => {
    if (!udinToDelete) return;
    setDeleting(true);
    try {
      await udinApi.deleteUdin(udinToDelete.id);
      setSuccessMessage('UDIN record deleted successfully.');
      setUdinToDelete(null);
      loadData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to delete UDIN.');
    } finally {
      setDeleting(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setSuccessMessage(`Copied "${text}" to clipboard.`);
    setTimeout(() => setSuccessMessage(''), 3000);
  };

  // Filtered List
  const filteredUdins = useMemo(() => {
    return udinList.filter((item) => {
      const q = search.toLowerCase().trim();
      const matchSearch =
        !q ||
        item.udin.toLowerCase().includes(q) ||
        item.documentTitle.toLowerCase().includes(q) ||
        item.signatoryName.toLowerCase().includes(q) ||
        (item.clientName && item.clientName.toLowerCase().includes(q)) ||
        (item.signatoryMembershipNo && item.signatoryMembershipNo.toLowerCase().includes(q));

      const matchStatus = statusFilter === 'ALL' || item.status === statusFilter;
      const matchVerification = verificationFilter === 'ALL' || item.verificationStatus === verificationFilter;
      const matchDocType = docTypeFilter === 'ALL' || item.documentType === docTypeFilter;
      const matchClient = clientFilter === 'ALL' || item.clientId === clientFilter;

      return matchSearch && matchStatus && matchVerification && matchDocType && matchClient;
    });
  }, [udinList, search, statusFilter, verificationFilter, docTypeFilter, clientFilter]);

  const renderVerificationBadge = (vStatus: UdinVerificationStatusType) => {
    switch (vStatus) {
      case 'VERIFIED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">
            <CheckCircle className="w-3.5 h-3.5" />
            Verified
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300 border border-red-300 dark:border-red-700">
            <XCircle className="w-3.5 h-3.5" />
            Failed
          </span>
        );
      case 'NOT_APPLICABLE':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 border border-gray-300 dark:border-gray-700">
            N/A
          </span>
        );
      case 'NOT_VERIFIED':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300 border border-amber-300 dark:border-amber-700">
            <Clock className="w-3.5 h-3.5" />
            Not Verified
          </span>
        );
    }
  };

  const renderStatusBadge = (status: UdinStatusType) => {
    switch (status) {
      case 'ACTIVE':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300">
            ACTIVE
          </span>
        );
      case 'CANCELLED':
      case 'REVOKED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300">
            {status}
          </span>
        );
      case 'ARCHIVED':
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400">
            {status}
          </span>
        );
    }
  };

  const getDocTypeLabel = (code: string) => {
    const found = DOCUMENT_TYPES.find((d) => d.value === code);
    return found ? found.label : code.replace(/_/g, ' ');
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-500/10 text-blue-600 dark:text-blue-400 rounded-xl border border-blue-500/20">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
                UDIN Register & Verification
                <span className="text-xs font-normal px-2.5 py-0.5 rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                  ICAI Compliance
                </span>
              </h1>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Maintain, track, and verify Unique Document Identification Numbers (UDIN) across all practice client engagements
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-2"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>

          <Button
            onClick={openCreateModal}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Record UDIN
          </Button>
        </div>
      </div>

      {/* Compliance Disclaimer Notice */}
      <div className="rounded-xl border border-blue-200 dark:border-blue-800/60 bg-gradient-to-r from-blue-50/70 via-indigo-50/40 to-blue-50/70 dark:from-blue-950/30 dark:via-indigo-950/20 dark:to-blue-950/30 p-4">
        <div className="flex items-start gap-3">
          <ShieldCheck className="w-5 h-5 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
          <div className="text-xs sm:text-sm text-blue-950 dark:text-blue-200 space-y-1">
            <p className="font-semibold text-blue-900 dark:text-blue-300">
              Unique Document Identification Number (UDIN) Governance Notice
            </p>
            <p className="text-blue-800/90 dark:text-blue-300/80 leading-relaxed">
              Taxoryn acts as a practice-level register and internal verification management vault. Official UDIN generation and authentication are governed by the Institute of Chartered Accountants of India (ICAI). Ensure all 18-digit UDINs recorded match official certificates and audit signoffs.
            </p>
          </div>
        </div>
      </div>

      {/* Notifications / Alerts */}
      {successMessage && (
        <div className="rounded-lg bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 p-4 text-sm text-emerald-800 dark:text-emerald-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage('')}
            className="text-emerald-600 hover:text-emerald-800 dark:hover:text-emerald-200"
          >
            &times;
          </button>
        </div>
      )}

      {error && (
        <div className="rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 p-4 text-sm text-red-800 dark:text-red-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 dark:text-red-400" />
            <span>{error}</span>
          </div>
          <button
            onClick={() => setError('')}
            className="text-red-600 hover:text-red-800 dark:hover:text-red-200"
          >
            &times;
          </button>
        </div>
      )}

      {/* Metric KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Total Registered</p>
            <Layers className="w-4 h-4 text-gray-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-gray-900 dark:text-white">
            {summary?.totalCount ?? udinList.length}
          </p>
          <span className="text-xs text-gray-400">All recorded UDINs</span>
        </div>

        <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Active Records</p>
            <FileText className="w-4 h-4 text-blue-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-blue-600 dark:text-blue-400">
            {summary?.activeCount ?? udinList.filter((u) => u.status === 'ACTIVE').length}
          </p>
          <span className="text-xs text-blue-500/80">Valid signoffs</span>
        </div>

        <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Verified</p>
            <CheckCircle className="w-4 h-4 text-emerald-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {summary?.verifiedCount ?? udinList.filter((u) => u.verificationStatus === 'VERIFIED').length}
          </p>
          <span className="text-xs text-emerald-500/80">Portal authenticated</span>
        </div>

        <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Pending Verification</p>
            <Clock className="w-4 h-4 text-amber-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-amber-600 dark:text-amber-400">
            {summary?.unverifiedCount ?? udinList.filter((u) => u.verificationStatus === 'NOT_VERIFIED').length}
          </p>
          <span className="text-xs text-amber-500/80">Awaiting crosscheck</span>
        </div>

        <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm col-span-2 md:col-span-1">
          <div className="flex items-center justify-between">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Cancelled / Failed</p>
            <Ban className="w-4 h-4 text-rose-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-rose-600 dark:text-rose-400">
            {(summary?.cancelledCount ?? 0) + (summary?.failedVerificationCount ?? 0)}
          </p>
          <span className="text-xs text-rose-500/80">Revoked or disputed</span>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white dark:bg-gray-800 p-4 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Search */}
          <div className="relative sm:col-span-2">
            <Search className="w-4 h-4 absolute left-3 top-3 text-gray-400" />
            <input
              type="text"
              placeholder="Search 18-digit UDIN, title, signatory..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
            />
          </div>

          {/* Verification Filter */}
          <div>
            <select
              value={verificationFilter}
              onChange={(e) => setVerificationFilter(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            >
              <option value="ALL">All Verification (Any)</option>
              <option value="NOT_VERIFIED">Pending Verification</option>
              <option value="VERIFIED">Verified</option>
              <option value="FAILED">Verification Failed</option>
              <option value="NOT_APPLICABLE">Not Applicable</option>
            </select>
          </div>

          {/* Document Type Filter */}
          <div>
            <select
              value={docTypeFilter}
              onChange={(e) => setDocTypeFilter(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            >
              <option value="ALL">All Document Types</option>
              {DOCUMENT_TYPES.map((d) => (
                <option key={d.value} value={d.value}>
                  {d.label}
                </option>
              ))}
            </select>
          </div>

          {/* Client Filter */}
          <div>
            <select
              value={clientFilter}
              onChange={(e) => setClientFilter(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            >
              <option value="ALL">All Clients ({clients.length})</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.displayName} {c.pan ? `(${c.pan})` : ''}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Active Filter Indicators */}
        {(search || statusFilter !== 'ALL' || verificationFilter !== 'ALL' || docTypeFilter !== 'ALL' || clientFilter !== 'ALL') && (
          <div className="flex items-center gap-2 pt-2 border-t border-gray-100 dark:border-gray-700 text-xs text-gray-500">
            <span>Showing {filteredUdins.length} of {udinList.length} records</span>
            <button
              onClick={() => {
                setSearch('');
                setStatusFilter('ALL');
                setVerificationFilter('ALL');
                setDocTypeFilter('ALL');
                setClientFilter('ALL');
              }}
              className="text-blue-600 dark:text-blue-400 hover:underline font-medium ml-auto"
            >
              Clear All Filters
            </button>
          </div>
        )}
      </div>

      {/* Main UDIN Register Table */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-gray-500 dark:text-gray-400">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto text-blue-500 mb-3" />
            <p>Loading UDIN register records...</p>
          </div>
        ) : filteredUdins.length === 0 ? (
          <div className="p-12 text-center">
            <Award className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
            <h3 className="text-base font-medium text-gray-900 dark:text-white">No UDIN records found</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1 max-w-sm mx-auto">
              {udinList.length === 0
                ? 'Your practice register is empty. Record your first UDIN to track audit and certificate numbers.'
                : 'No records match the active filter criteria.'}
            </p>
            {udinList.length === 0 && (
              <Button onClick={openCreateModal} className="mt-4 bg-blue-600 text-white">
                <Plus className="w-4 h-4 mr-1.5" />
                Record First UDIN
              </Button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  <th className="py-3.5 px-4">UDIN Number</th>
                  <th className="py-3.5 px-4">Document / Certification</th>
                  <th className="py-3.5 px-4">Client</th>
                  <th className="py-3.5 px-4">Signatory CA</th>
                  <th className="py-3.5 px-4">Generation Date</th>
                  <th className="py-3.5 px-4">Verification</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {filteredUdins.map((item) => (
                  <tr
                    key={item.id}
                    className="hover:bg-gray-50/80 dark:hover:bg-gray-700/40 transition group cursor-pointer"
                    onClick={() => setDetailUdin(item)}
                  >
                    {/* UDIN Number with Copy */}
                    <td className="py-3.5 px-4 font-mono font-bold text-gray-900 dark:text-white">
                      <div className="flex items-center gap-1.5">
                        <span className="bg-gray-100 dark:bg-gray-900 px-2 py-0.5 rounded border border-gray-300 dark:border-gray-700 text-xs tracking-wider text-blue-700 dark:text-blue-300">
                          {item.udin}
                        </span>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            copyToClipboard(item.udin);
                          }}
                          className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 rounded text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
                          title="Copy UDIN"
                        >
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>

                    {/* Document Title & Type */}
                    <td className="py-3.5 px-4">
                      <p className="font-medium text-gray-900 dark:text-white line-clamp-1">{item.documentTitle}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1 mt-0.5">
                        <FileText className="w-3 h-3 text-gray-400" />
                        {getDocTypeLabel(item.documentType)}
                      </p>
                    </td>

                    {/* Client Name & PAN */}
                    <td className="py-3.5 px-4">
                      {item.clientName ? (
                        <div>
                          <p className="font-medium text-gray-900 dark:text-gray-100 flex items-center gap-1.5">
                            <Building2 className="w-3.5 h-3.5 text-gray-400 flex-shrink-0" />
                            <span className="line-clamp-1">{item.clientName}</span>
                          </p>
                          {item.clientPan && (
                            <span className="text-xs font-mono text-gray-500 dark:text-gray-400 uppercase">
                              PAN: {item.clientPan}
                            </span>
                          )}
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400 italic">Not Assigned</span>
                      )}
                    </td>

                    {/* Signatory CA */}
                    <td className="py-3.5 px-4">
                      <p className="text-gray-900 dark:text-gray-200 flex items-center gap-1.5">
                        <User className="w-3.5 h-3.5 text-gray-400" />
                        {item.signatoryName}
                      </p>
                      {item.signatoryMembershipNo && (
                        <p className="text-xs font-mono text-gray-500 dark:text-gray-400">
                          M.No: {item.signatoryMembershipNo}
                        </p>
                      )}
                    </td>

                    {/* Generation Date */}
                    <td className="py-3.5 px-4 text-xs font-medium text-gray-600 dark:text-gray-300 whitespace-nowrap">
                      {item.generationDate}
                    </td>

                    {/* Verification Status */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {renderVerificationBadge(item.verificationStatus)}
                    </td>

                    {/* Lifecycle Status */}
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {renderStatusBadge(item.status)}
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Quick Verify */}
                        <button
                          type="button"
                          onClick={() => openVerifyModal(item)}
                          className="p-1.5 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 rounded-lg transition"
                          title="Update Verification Status"
                        >
                          <CheckCircle className="w-4 h-4" />
                        </button>

                        {/* Edit */}
                        <button
                          type="button"
                          onClick={() => openEditModal(item)}
                          className="p-1.5 hover:bg-blue-50 dark:hover:bg-blue-950/40 text-blue-600 dark:text-blue-400 rounded-lg transition"
                          title="Edit Details"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>

                        {/* Cancel if Active */}
                        {item.status === 'ACTIVE' && (
                          <button
                            type="button"
                            onClick={() => openCancelModal(item)}
                            className="p-1.5 hover:bg-amber-50 dark:hover:bg-amber-950/40 text-amber-600 dark:text-amber-400 rounded-lg transition"
                            title="Cancel / Revoke UDIN"
                          >
                            <Ban className="w-4 h-4" />
                          </button>
                        )}

                        {/* Delete */}
                        <button
                          type="button"
                          onClick={() => setUdinToDelete(item)}
                          className="p-1.5 hover:bg-rose-50 dark:hover:bg-rose-950/40 text-rose-600 dark:text-rose-400 rounded-lg transition"
                          title="Delete UDIN Record"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Record / Edit UDIN Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700 shadow-2xl">
            <form onSubmit={handleSaveUdin} className="p-6 space-y-5">
              <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-xl">
                    <Award className="w-5 h-5" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-gray-900 dark:text-white">
                      {editingUdin ? 'Edit UDIN Record' : 'Record New UDIN'}
                    </h2>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {editingUdin ? 'Update metadata for recorded UDIN' : 'Register 18-digit ICAI UDIN for practice client audits & certifications'}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 text-xl font-bold"
                >
                  &times;
                </button>
              </div>

              {formError && (
                <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-xl text-xs text-red-700 dark:text-red-300 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* UDIN Number (Immutable in edit) */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    UDIN (18 Characters) *
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={18}
                    disabled={!!editingUdin}
                    placeholder="e.g. 24123456AAAAAA1234"
                    value={udinNumber}
                    onChange={(e) => setUdinNumber(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
                    className="w-full px-3 py-2 text-sm font-mono tracking-wider uppercase rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white disabled:bg-gray-100 dark:disabled:bg-gray-800 outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <p className="text-xs text-gray-400 mt-1">
                    Format: Exactly 18 alphanumeric characters issued by ICAI. (Current length: {udinNumber.length}/18)
                  </p>
                </div>

                {/* Associated Client */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Associated Client
                  </label>
                  <select
                    value={selectedClientId}
                    onChange={(e) => setSelectedClientId(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">-- None / General Advisory --</option>
                    {clients.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.displayName} {c.pan ? `(${c.pan})` : ''}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Document Type */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Document / Certificate Type *
                  </label>
                  <select
                    value={documentType}
                    onChange={(e) => setDocumentType(e.target.value as UdinDocumentType)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    {DOCUMENT_TYPES.map((d) => (
                      <option key={d.value} value={d.value}>
                        [{d.group}] {d.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Document Title */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Document Title / Subject *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Tax Audit Report under Section 44AB for FY 2023-24"
                    value={documentTitle}
                    onChange={(e) => setDocumentTitle(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Signatory CA Name */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Signatory Partner / CA Name *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. CA Anand Singhal"
                    value={signatoryName}
                    onChange={(e) => setSignatoryName(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* ICAI Membership Number */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    ICAI Membership No.
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. 123456"
                    value={signatoryMembershipNo}
                    onChange={(e) => setSignatoryMembershipNo(e.target.value)}
                    className="w-full px-3 py-2 text-sm font-mono rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Generation Date */}
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    UDIN Generation Date *
                  </label>
                  <input
                    type="date"
                    required
                    value={generationDate}
                    onChange={(e) => setGenerationDate(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Document Description */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Description & Key Particulars
                  </label>
                  <textarea
                    rows={2}
                    placeholder="Key figures, financial figures, audit opinion summary, or specific remarks..."
                    value={documentDescription}
                    onChange={(e) => setDocumentDescription(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                {/* Internal Practice Notes */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                    Internal Practice Notes
                  </label>
                  <textarea
                    rows={2}
                    placeholder="Internal reference numbers, signed physical file location..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIsModalOpen(false)}
                  disabled={saving}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={saving}
                  className="bg-blue-600 text-white min-w-[120px]"
                >
                  {saving ? 'Saving...' : editingUdin ? 'Update UDIN' : 'Record UDIN'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Quick Verification Update Modal */}
      {isVerifyModalOpen && verifyingUdin && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-2xl max-w-lg w-full border border-gray-200 dark:border-gray-700 shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 rounded-xl">
                  <CheckCircle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 dark:text-white">Update Verification Status</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400 font-mono">UDIN: {verifyingUdin.udin}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsVerifyModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 text-xl font-bold"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSaveVerification} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                  Verification Status *
                </label>
                <select
                  value={verifyStatus}
                  onChange={(e) => setVerifyStatus(e.target.value as UdinVerificationStatusType)}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="VERIFIED">VERIFIED (Authenticated on ICAI Portal)</option>
                  <option value="NOT_VERIFIED">NOT_VERIFIED (Pending Crosscheck)</option>
                  <option value="FAILED">FAILED (Discrepancy / Mismatch on Portal)</option>
                  <option value="NOT_APPLICABLE">NOT_APPLICABLE</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                  Verification Source / Method
                </label>
                <input
                  type="text"
                  placeholder="e.g. ICAI Portal Check, QR Code Scan, Client Signoff"
                  value={verifySource}
                  onChange={(e) => setVerifySource(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                  Verification Remarks / Findings
                </label>
                <textarea
                  rows={3}
                  placeholder="Enter notes on verification results, date of check, or any discrepancy notes..."
                  value={verifyRemarks}
                  onChange={(e) => setVerifyRemarks(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIsVerifyModalOpen(false)}
                  disabled={savingVerify}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={savingVerify}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  {savingVerify ? 'Updating...' : 'Save Verification'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel UDIN Modal */}
      {isCancelModalOpen && cancellingUdin && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-2xl max-w-md w-full border border-gray-200 dark:border-gray-700 shadow-2xl p-6 space-y-4">
            <div className="flex items-center gap-3 text-amber-600">
              <Ban className="w-6 h-6 flex-shrink-0" />
              <div>
                <h3 className="font-bold text-gray-900 dark:text-white">Cancel / Revoke UDIN Record</h3>
                <p className="text-xs text-gray-500 dark:text-gray-400 font-mono">UDIN: {cancellingUdin.udin}</p>
              </div>
            </div>

            <p className="text-xs text-gray-600 dark:text-gray-300">
              This will mark the UDIN as <strong>CANCELLED</strong> in Taxoryn. Please provide the reason for cancellation or revocation.
            </p>

            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase mb-1">
                Cancellation Reason *
              </label>
              <textarea
                required
                rows={3}
                placeholder="Reason for cancellation (e.g. Document amended, Typo in financial figure on ICAI portal)..."
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-200 dark:border-gray-700">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setIsCancelModalOpen(false)}
                disabled={processingCancel}
              >
                Keep Active
              </Button>
              <Button
                type="button"
                disabled={processingCancel || !cancelReason.trim()}
                onClick={handleCancelUdin}
                className="bg-amber-600 hover:bg-amber-700 text-white"
              >
                {processingCancel ? 'Cancelling...' : 'Confirm Cancellation'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {udinToDelete && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-2xl max-w-md w-full border border-gray-200 dark:border-gray-700 shadow-2xl p-6 space-y-4">
            <div className="flex items-center gap-3 text-rose-600">
              <Trash2 className="w-6 h-6 flex-shrink-0" />
              <div>
                <h3 className="font-bold text-gray-900 dark:text-white">Delete UDIN Record?</h3>
                <p className="text-xs text-gray-500 dark:text-gray-400 font-mono">UDIN: {udinToDelete.udin}</p>
              </div>
            </div>

            <p className="text-xs text-gray-600 dark:text-gray-300 leading-relaxed">
              Are you sure you want to permanently delete this UDIN register entry? This action will also be recorded in the enterprise audit log.
            </p>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-200 dark:border-gray-700">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setUdinToDelete(null)}
                disabled={deleting}
              >
                Cancel
              </Button>
              <Button
                type="button"
                disabled={deleting}
                onClick={handleDeleteUdin}
                className="bg-rose-600 hover:bg-rose-700 text-white"
              >
                {deleting ? 'Deleting...' : 'Delete Record'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Full Detail Modal */}
      {detailUdin && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-200 dark:border-gray-700 shadow-2xl p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-blue-100 dark:bg-blue-900/40 text-blue-600 rounded-xl">
                  <Award className="w-6 h-6" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-gray-900 dark:text-white font-mono tracking-wider">
                    {detailUdin.udin}
                  </h2>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {getDocTypeLabel(detailUdin.documentType)}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setDetailUdin(null)}
                className="text-gray-400 hover:text-gray-600 text-xl font-bold"
              >
                &times;
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Document Title</span>
                <p className="text-sm font-semibold text-gray-900 dark:text-white mt-0.5">{detailUdin.documentTitle}</p>
              </div>

              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Client</span>
                <p className="text-sm font-semibold text-gray-900 dark:text-white mt-0.5">
                  {detailUdin.clientName || 'General / Unassigned'}
                  {detailUdin.clientPan && <span className="ml-1 font-mono text-xs font-normal">({detailUdin.clientPan})</span>}
                </p>
              </div>

              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Signatory CA</span>
                <p className="text-sm font-semibold text-gray-900 dark:text-white mt-0.5">{detailUdin.signatoryName}</p>
                {detailUdin.signatoryMembershipNo && (
                  <p className="text-gray-500 font-mono mt-0.5">ICAI M.No: {detailUdin.signatoryMembershipNo}</p>
                )}
              </div>

              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Generation Date</span>
                <p className="text-sm font-semibold text-gray-900 dark:text-white mt-0.5">{detailUdin.generationDate}</p>
              </div>

              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Verification Status</span>
                <div className="mt-1">{renderVerificationBadge(detailUdin.verificationStatus)}</div>
                {detailUdin.verificationSource && (
                  <p className="text-gray-500 mt-1">Source: {detailUdin.verificationSource}</p>
                )}
                {detailUdin.verifiedAt && (
                  <p className="text-gray-500 mt-0.5">Checked at: {new Date(detailUdin.verifiedAt).toLocaleString()}</p>
                )}
              </div>

              <div className="bg-gray-50 dark:bg-gray-900/60 p-3 rounded-xl">
                <span className="text-gray-500 font-medium">Lifecycle Status</span>
                <div className="mt-1">{renderStatusBadge(detailUdin.status)}</div>
              </div>
            </div>

            {detailUdin.verificationRemarks && (
              <div className="bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800 p-3 rounded-xl text-xs">
                <span className="font-semibold text-emerald-800 dark:text-emerald-300">Verification Remarks</span>
                <p className="text-gray-700 dark:text-gray-300 mt-0.5 whitespace-pre-wrap">{detailUdin.verificationRemarks}</p>
              </div>
            )}

            {detailUdin.documentDescription && (
              <div className="bg-gray-50 dark:bg-gray-900/40 p-3 rounded-xl text-xs">
                <span className="font-semibold text-gray-700 dark:text-gray-300">Particulars & Description</span>
                <p className="text-gray-600 dark:text-gray-400 mt-0.5 whitespace-pre-wrap">{detailUdin.documentDescription}</p>
              </div>
            )}

            {detailUdin.notes && (
              <div className="bg-gray-50 dark:bg-gray-900/40 p-3 rounded-xl text-xs">
                <span className="font-semibold text-gray-700 dark:text-gray-300">Notes & File References</span>
                <p className="text-gray-600 dark:text-gray-400 mt-0.5 whitespace-pre-wrap">{detailUdin.notes}</p>
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-400">
              <span>Created: {detailUdin.createdAt ? new Date(detailUdin.createdAt).toLocaleDateString() : 'N/A'}</span>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    const u = detailUdin;
                    setDetailUdin(null);
                    openVerifyModal(u);
                  }}
                >
                  Verify Status
                </Button>
                <Button
                  size="sm"
                  onClick={() => setDetailUdin(null)}
                >
                  Close
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
