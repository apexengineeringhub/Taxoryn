import React, { useState, useRef, useMemo } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  UploadCloud,
  FileText,
  Receipt,
  XCircle,
  ExternalLink,
  ChevronRight,
  Sparkles,
  ShieldAlert,
  ArrowRight,
  Filter,
  CheckCheck,
  RefreshCw,
  FolderLock,
  MessageSquare,
} from 'lucide-react';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import {
  ClientPortalDashboard,
  DocumentRequest,
  DocumentRequestItem,
  ClientDocumentRequest,
  Invoice,
  MessageContextType,
} from '../../types';
import { documentRequestApi, portalApi } from '../../api/endpoints';
import { formatCurrency } from '../../utils/currencyUtils';
import clsx from 'clsx';

export interface ClientActionCenterProps {
  dashboard: ClientPortalDashboard | null;
  activeRequests: DocumentRequest[];
  pendingDocRequests: ClientDocumentRequest[];
  invoices: Invoice[];
  isLoading: boolean;
  onRefresh: () => void;
  onNavigateTab: (tab: string) => void;
  onOpenUploadModal?: (request?: DocumentRequest | ClientDocumentRequest, item?: DocumentRequestItem) => void;
  onInitiateContextualMessage?: (context: {
    contextType: MessageContextType;
    contextId?: string;
    contextTitle: string;
  }) => void;
  isPracticeUser?: boolean;
}

type ActionFilter = 'all' | 'action_required' | 'under_review' | 'completed' | 'bills';

export const ClientActionCenter: React.FC<ClientActionCenterProps> = ({
  dashboard,
  activeRequests = [],
  pendingDocRequests = [],
  invoices = [],
  isLoading,
  onRefresh,
  onNavigateTab,
  onOpenUploadModal,
  onInitiateContextualMessage,
  isPracticeUser = false,
}) => {
  const [filter, setFilter] = useState<ActionFilter>('all');
  const [uploadingItemId, setUploadingItemId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const activeTargetItemRef = useRef<{ itemId: string; requestId?: string; isLegacy?: boolean } | null>(null);

  // 1. Extract and normalize all canonical items
  const canonicalItems = useMemo(() => {
    const items: Array<{
      item: DocumentRequestItem;
      request: DocumentRequest;
    }> = [];
    activeRequests.forEach((req) => {
      if (req.items && req.items.length > 0) {
        req.items.forEach((item) => {
          items.push({ item, request: req });
        });
      }
    });
    return items;
  }, [activeRequests]);

  // 2. Identify Rejected Items Needing Correction
  const rejectedItems = useMemo(() => {
    const list: Array<{
      id: string;
      title: string;
      documentType: string;
      description?: string;
      rejectionReason?: string;
      dueDate?: string;
      isOverdue?: boolean;
      requestNumber?: string;
      purpose?: string;
      requestId?: string;
      isLegacy?: boolean;
    }> = [];

    // From canonical items
    canonicalItems.forEach(({ item, request }) => {
      if (item.status === 'REJECTED') {
        const isOverdue = request.dueDate
          ? new Date(request.dueDate) < new Date() && request.status !== 'COMPLETED'
          : false;
        list.push({
          id: item.id,
          title: item.title,
          documentType: item.documentType,
          description: item.description,
          rejectionReason: item.rejectionReason || 'Please review and upload an updated document.',
          dueDate: request.dueDate,
          isOverdue,
          requestNumber: request.requestNumber,
          purpose: request.purpose,
          requestId: request.id,
          isLegacy: false,
        });
      }
    });

    // From legacy pending requests if marked REJECTED
    pendingDocRequests.forEach((req) => {
      if (req.status === 'REJECTED' && !list.some((i) => i.id === req.id)) {
        const isOverdue = req.dueDate ? new Date(req.dueDate) < new Date() : false;
        list.push({
          id: req.id,
          title: req.title,
          documentType: req.documentType,
          description: req.description,
          rejectionReason: req.rejectionReason || 'Document rejected. Please upload a corrected copy.',
          dueDate: req.dueDate,
          isOverdue,
          requestNumber: req.requestNumber,
          purpose: req.title,
          requestId: req.requestId,
          isLegacy: true,
        });
      }
    });

    return list;
  }, [canonicalItems, pendingDocRequests]);

  // 3. Identify Pending Items To Upload
  const pendingItems = useMemo(() => {
    const list: Array<{
      id: string;
      title: string;
      documentType: string;
      description?: string;
      dueDate?: string;
      isOverdue?: boolean;
      requestNumber?: string;
      purpose?: string;
      requestId?: string;
      isLegacy?: boolean;
    }> = [];

    // From canonical items
    canonicalItems.forEach(({ item, request }) => {
      if (item.status === 'PENDING') {
        const isOverdue = request.dueDate
          ? new Date(request.dueDate) < new Date() && request.status !== 'COMPLETED'
          : false;
        list.push({
          id: item.id,
          title: item.title,
          documentType: item.documentType,
          description: item.description,
          dueDate: request.dueDate,
          isOverdue,
          requestNumber: request.requestNumber,
          purpose: request.purpose,
          requestId: request.id,
          isLegacy: false,
        });
      }
    });

    // From legacy pending requests
    pendingDocRequests.forEach((req) => {
      if (req.status === 'PENDING' && !list.some((i) => i.id === req.id)) {
        const isOverdue = req.dueDate ? new Date(req.dueDate) < new Date() : false;
        list.push({
          id: req.id,
          title: req.title,
          documentType: req.documentType,
          description: req.description,
          dueDate: req.dueDate,
          isOverdue,
          requestNumber: req.requestNumber,
          purpose: req.title,
          requestId: req.requestId,
          isLegacy: true,
        });
      }
    });

    return list;
  }, [canonicalItems, pendingDocRequests]);

  // 4. Identify In-Progress / Under Review Items
  const underReviewItems = useMemo(() => {
    const list: Array<{
      id: string;
      title: string;
      documentType: string;
      uploadedDocumentName?: string;
      uploadedAt?: string;
      requestNumber?: string;
      purpose?: string;
      requestId?: string;
    }> = [];

    canonicalItems.forEach(({ item, request }) => {
      if (item.status === 'UPLOADED' || item.status === 'UNDER_REVIEW') {
        list.push({
          id: item.id,
          title: item.title,
          documentType: item.documentType,
          uploadedDocumentName: item.uploadedDocumentName,
          uploadedAt: item.uploadedAt,
          requestNumber: request.requestNumber,
          purpose: request.purpose,
          requestId: request.id,
        });
      }
    });

    return list;
  }, [canonicalItems]);

  // 5. Identify Completed / Accepted Items & Requests
  const completedItems = useMemo(() => {
    const list: Array<{
      id: string;
      title: string;
      documentType: string;
      requestNumber?: string;
      purpose?: string;
      reviewedByName?: string;
      reviewedAt?: string;
    }> = [];

    canonicalItems.forEach(({ item, request }) => {
      if (item.status === 'ACCEPTED') {
        list.push({
          id: item.id,
          title: item.title,
          documentType: item.documentType,
          requestNumber: request.requestNumber,
          purpose: request.purpose,
          reviewedByName: item.reviewedByName,
          reviewedAt: item.reviewedAt,
        });
      }
    });

    return list;
  }, [canonicalItems]);

  // 6. Outstanding Unpaid Invoices
  const unpaidInvoices = useMemo(() => {
    return invoices.filter((inv) => inv.status !== 'PAID' && (inv.balanceDue || 0) > 0);
  }, [invoices]);

  // Counts
  const totalActionRequiredCount = rejectedItems.length + pendingItems.length + unpaidInvoices.length;

  // Handle direct file upload click
  const triggerItemUpload = (itemId: string, requestId?: string, isLegacy?: boolean) => {
    activeTargetItemRef.current = { itemId, requestId, isLegacy };
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
      fileInputRef.current.click();
    }
  };

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const target = activeTargetItemRef.current;
    if (!file || !target) return;

    try {
      setUploadingItemId(target.itemId);
      setFeedback(null);

      if (target.isLegacy) {
        // Legacy upload path
        await portalApi.uploadDocument(
          file,
          {
            title: file.name,
            category: 'CLIENT_UPLOAD',
            documentType: 'OTHER',
            description: `Fulfilling request ${target.itemId}`,
            clientId: dashboard?.clientId,
          },
          target.itemId
        );
      } else {
        // Canonical multi-item upload
        if (isPracticeUser) {
          await documentRequestApi.uploadItem(target.itemId, file);
        } else {
          await documentRequestApi.uploadPortalItem(target.itemId, file);
        }
      }

      setFeedback({
        type: 'success',
        message: `"${file.name}" uploaded successfully! Your tax consultant has been notified.`,
      });
      onRefresh();
    } catch (err: any) {
      console.error('Failed to upload document in Action Center', err);
      const msg = err?.response?.data?.message || 'Failed to upload document. Please check file format and size.';
      setFeedback({ type: 'error', message: msg });
    } finally {
      setUploadingItemId(null);
      activeTargetItemRef.current = null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Hidden File Input for Direct Inline Uploads */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileSelected}
        className="hidden"
        aria-label="Upload document file"
      />

      {/* Top Banner / Hero Card */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white p-5 sm:p-6 rounded-2xl shadow-sm border border-slate-700/50">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-brand-500/20 text-brand-300 border border-brand-400/30">
                Action Center
              </span>
              {totalActionRequiredCount > 0 ? (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-400/30 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3 text-amber-400" />
                  {totalActionRequiredCount} Items Requiring Your Attention
                </span>
              ) : (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-400/30 flex items-center gap-1">
                  <CheckCheck className="w-3 h-3 text-emerald-400" />
                  All Caught Up
                </span>
              )}
            </div>
            <h1 className="text-xl sm:text-2xl font-black text-white tracking-tight">
              {isPracticeUser
                ? `Collaboration Overview: ${dashboard?.displayName || 'Client'}`
                : `Welcome, ${dashboard?.displayName || 'Valued Client'}`}
            </h1>
            <p className="text-xs text-slate-300 max-w-2xl">
              {isPracticeUser
                ? 'Review client document submissions, outstanding checklists, and required follow-ups.'
                : 'Track requested tax proofs, view correction notes, and ensure your compliance filings proceed smoothly.'}
            </p>
          </div>

          <div className="flex items-center gap-2.5 self-start sm:self-center">
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={isLoading}
              leftIcon={<RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} />}
              className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs font-bold"
            >
              Refresh Status
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => onNavigateTab('documents')}
              leftIcon={<FolderLock className="w-3.5 h-3.5" />}
              className="bg-brand-600 hover:bg-brand-500 text-white text-xs font-bold shadow-xs"
            >
              Document Vault
            </Button>
          </div>
        </div>

        {/* Action Summary Metric Tiles */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6 pt-5 border-t border-slate-700/60">
          <div
            onClick={() => setFilter('action_required')}
            className={clsx(
              'p-3 rounded-xl border transition-all cursor-pointer',
              filter === 'action_required'
                ? 'bg-amber-500/20 border-amber-400/50'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            )}
          >
            <span className="text-[11px] font-semibold text-amber-300 uppercase tracking-wider block">
              Action Required
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-2xl font-black text-white">
                {rejectedItems.length + pendingItems.length}
              </span>
              {rejectedItems.length > 0 && (
                <span className="text-[11px] font-bold text-rose-400">
                  ({rejectedItems.length} {rejectedItems.length === 1 ? 'correction' : 'corrections'})
                </span>
              )}
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">Pending & rejected files</span>
          </div>

          <div
            onClick={() => setFilter('under_review')}
            className={clsx(
              'p-3 rounded-xl border transition-all cursor-pointer',
              filter === 'under_review'
                ? 'bg-blue-500/20 border-blue-400/50'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            )}
          >
            <span className="text-[11px] font-semibold text-blue-300 uppercase tracking-wider block">
              Under Review
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-2xl font-black text-white">{underReviewItems.length}</span>
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">Submitted, awaiting review</span>
          </div>

          <div
            onClick={() => setFilter('completed')}
            className={clsx(
              'p-3 rounded-xl border transition-all cursor-pointer',
              filter === 'completed'
                ? 'bg-emerald-500/20 border-emerald-400/50'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            )}
          >
            <span className="text-[11px] font-semibold text-emerald-300 uppercase tracking-wider block">
              Completed
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-2xl font-black text-white">{completedItems.length}</span>
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">Accepted & verified files</span>
          </div>

          <div
            onClick={() => setFilter('bills')}
            className={clsx(
              'p-3 rounded-xl border transition-all cursor-pointer',
              filter === 'bills'
                ? 'bg-rose-500/20 border-rose-400/50'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            )}
          >
            <span className="text-[11px] font-semibold text-rose-300 uppercase tracking-wider block">
              Unpaid Invoices
            </span>
            <div className="flex items-baseline gap-2 mt-1">
              <span className="text-2xl font-black text-white">{unpaidInvoices.length}</span>
              {(dashboard?.outstandingBalance || 0) > 0 && (
                <span className="text-[11px] font-bold text-rose-300">
                  {formatCurrency(dashboard?.outstandingBalance || 0)}
                </span>
              )}
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">Payment due</span>
          </div>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={clsx(
            'p-4 rounded-xl text-xs font-semibold flex items-center justify-between border transition-all',
            feedback.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-rose-50 text-rose-800 border-rose-200'
          )}
        >
          <div className="flex items-center gap-2">
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-slate-400 hover:text-slate-700 text-sm font-bold px-2 py-0.5"
            aria-label="Dismiss feedback"
          >
            &times;
          </button>
        </div>
      )}

      {/* Category Filter Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
        <span className="text-xs font-bold text-slate-500 flex items-center gap-1.5 shrink-0 mr-1">
          <Filter className="w-3.5 h-3.5" />
          Filter:
        </span>
        {[
          { id: 'all', label: `All Action Items (${totalActionRequiredCount + underReviewItems.length})` },
          { id: 'action_required', label: `Action Required (${rejectedItems.length + pendingItems.length})` },
          { id: 'under_review', label: `Under Review (${underReviewItems.length})` },
          { id: 'completed', label: `Completed (${completedItems.length})` },
          { id: 'bills', label: `Bills Due (${unpaidInvoices.length})` },
        ].map((btn) => (
          <button
            key={btn.id}
            onClick={() => setFilter(btn.id as ActionFilter)}
            className={clsx(
              'px-3 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 cursor-pointer',
              filter === btn.id
                ? 'bg-brand-600 text-white shadow-2xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-900'
            )}
          >
            {btn.label}
          </button>
        ))}
      </div>

      {/* CATEGORY 1: 🚨 Rejected Items Needing Immediate Correction */}
      {(filter === 'all' || filter === 'action_required') && rejectedItems.length > 0 && (
        <Card
          title="🚨 Action Required: Corrections Needed"
          subtitle="The following documents require re-upload or correction based on consultant review"
          className="border-rose-200 bg-rose-50/20 shadow-2xs"
        >
          <div className="space-y-3">
            {rejectedItems.map((item) => {
              const isUploadingThis = uploadingItemId === item.id;
              return (
                <div
                  key={item.id}
                  className="bg-white p-4 sm:p-5 rounded-2xl border border-rose-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all hover:border-rose-300"
                >
                  <div className="space-y-2 max-w-2xl">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                        <XCircle className="w-3.5 h-3.5 text-rose-600" />
                        Correction Required
                      </span>
                      {item.requestNumber && (
                        <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                          {item.requestNumber}
                        </span>
                      )}
                      <span className="font-mono text-[10px] font-bold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded">
                        {item.documentType}
                      </span>
                      {item.isOverdue && (
                        <span className="text-[10px] font-bold bg-rose-600 text-white px-2 py-0.5 rounded uppercase">
                          Overdue
                        </span>
                      )}
                    </div>

                    <div>
                      <h3 className="text-sm font-bold text-slate-900">{item.title}</h3>
                      {item.description && (
                        <p className="text-xs text-slate-500 mt-0.5">{item.description}</p>
                      )}
                    </div>

                    {/* Prominent Rejection Reason Callout */}
                    <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 text-xs text-rose-900 flex items-start gap-2.5">
                      <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold text-rose-950 block">Consultant Correction Note:</span>
                        <p className="text-rose-800 mt-0.5 font-medium">{item.rejectionReason}</p>
                      </div>
                    </div>

                    {item.dueDate && (
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        Deadline: <strong className="text-slate-800">{item.dueDate}</strong>
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 self-start md:self-center shrink-0">
                    {onInitiateContextualMessage && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          onInitiateContextualMessage({
                            contextType: 'DOCUMENT_ITEM',
                            contextId: item.id,
                            contextTitle: `${item.requestNumber ? item.requestNumber + ' - ' : ''}${item.title}`,
                          })
                        }
                        leftIcon={<MessageSquare className="w-3.5 h-3.5 text-rose-600" />}
                        className="text-xs font-semibold text-rose-700 border-rose-200 hover:bg-rose-50"
                      >
                        Ask Question
                      </Button>
                    )}
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => triggerItemUpload(item.id, item.requestId, item.isLegacy)}
                      disabled={isUploadingThis}
                      leftIcon={
                        isUploadingThis ? (
                          <RefreshCw className="w-4 h-4 animate-spin" />
                        ) : (
                          <UploadCloud className="w-4 h-4" />
                        )
                      }
                      className="bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-xs"
                    >
                      {isUploadingThis ? 'Uploading...' : 'Re-Upload File'}
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {/* CATEGORY 2: 📥 Pending Document Requests & Checklists */}
      {(filter === 'all' || filter === 'action_required') && pendingItems.length > 0 && (
        <Card
          title="📥 Pending Document Requests"
          subtitle="Required tax proofs and statements awaiting your submission"
          className="border-amber-200 bg-amber-50/20 shadow-2xs"
        >
          <div className="space-y-3">
            {pendingItems.map((item) => {
              const isUploadingThis = uploadingItemId === item.id;
              return (
                <div
                  key={item.id}
                  className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all hover:border-brand-300"
                >
                  <div className="space-y-1.5 max-w-2xl">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                        <Clock className="w-3.5 h-3.5 text-amber-600" />
                        Upload Pending
                      </span>
                      {item.requestNumber && (
                        <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                          {item.requestNumber}
                        </span>
                      )}
                      <span className="font-mono text-[10px] font-bold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded">
                        {item.documentType}
                      </span>
                      {item.isOverdue && (
                        <span className="text-[10px] font-bold bg-rose-600 text-white px-2 py-0.5 rounded uppercase">
                          Overdue
                        </span>
                      )}
                    </div>

                    <div>
                      <h3 className="text-sm font-bold text-slate-900">{item.title}</h3>
                      {item.description && (
                        <p className="text-xs text-slate-500 mt-0.5">{item.description}</p>
                      )}
                    </div>

                    {item.dueDate && (
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-slate-400" />
                        Due Date: <strong className="text-slate-800">{item.dueDate}</strong>
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 self-start md:self-center shrink-0">
                    {onInitiateContextualMessage && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          onInitiateContextualMessage({
                            contextType: 'DOCUMENT_ITEM',
                            contextId: item.id,
                            contextTitle: `${item.requestNumber ? item.requestNumber + ' - ' : ''}${item.title}`,
                          })
                        }
                        leftIcon={<MessageSquare className="w-3.5 h-3.5 text-slate-500" />}
                        className="text-xs text-slate-600 hover:text-brand-700"
                      >
                        Ask Query
                      </Button>
                    )}
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => triggerItemUpload(item.id, item.requestId, item.isLegacy)}
                      disabled={isUploadingThis}
                      leftIcon={
                        isUploadingThis ? (
                          <RefreshCw className="w-4 h-4 animate-spin" />
                        ) : (
                          <UploadCloud className="w-4 h-4" />
                        )
                      }
                      className="text-xs font-bold"
                    >
                      {isUploadingThis ? 'Uploading...' : 'Upload Document'}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onNavigateTab('documents')}
                      leftIcon={<ExternalLink className="w-3.5 h-3.5" />}
                      className="text-xs font-semibold"
                    >
                      View Checklist
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {/* CATEGORY 3: ⏳ Under Review Submissions */}
      {(filter === 'all' || filter === 'under_review') && underReviewItems.length > 0 && (
        <Card
          title="⏳ In-Progress: Consultant Review Underway"
          subtitle="Documents you have uploaded that are currently being verified by your tax practitioner"
          className="border-blue-200 bg-blue-50/10 shadow-2xs"
        >
          <div className="space-y-3">
            {underReviewItems.map((item) => (
              <div
                key={item.id}
                className="bg-white p-4 rounded-xl border border-blue-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900 text-sm">{item.title}</span>
                    {item.requestNumber && (
                      <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                        {item.requestNumber}
                      </span>
                    )}
                    <span className="font-mono text-[10px] font-bold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded">
                      {item.documentType}
                    </span>
                  </div>
                  {item.uploadedDocumentName && (
                    <span className="text-slate-600 font-medium block">
                      Uploaded File: <strong className="text-slate-900">{item.uploadedDocumentName}</strong>
                    </span>
                  )}
                  {item.uploadedAt && (
                    <span className="text-[10px] text-slate-400 block">
                      Submitted on: {new Date(item.uploadedAt).toLocaleString()}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
                    <Clock className="w-3.5 h-3.5 text-blue-600" />
                    Verification in Progress
                  </span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* CATEGORY 4: ✅ Completed / Accepted Documents */}
      {(filter === 'all' || filter === 'completed') && completedItems.length > 0 && (
        <Card
          title="✅ Completed & Verified Documents"
          subtitle="Documents submitted by you that have been verified and accepted by your practitioner"
          className="border-emerald-200 bg-emerald-50/10 shadow-2xs"
        >
          <div className="space-y-2.5">
            {completedItems.slice(0, 6).map((item) => (
              <div
                key={item.id}
                className="bg-white p-3.5 rounded-xl border border-slate-200 flex items-center justify-between gap-3 text-xs"
              >
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-900">{item.title}</span>
                    <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-600 px-1.5 py-0.2 rounded">
                      {item.documentType}
                    </span>
                  </div>
                  {item.purpose && (
                    <span className="text-[11px] text-slate-500 block">For: {item.purpose}</span>
                  )}
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                    Verified & Accepted
                  </span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* CATEGORY 5: 💳 Unpaid Bills / Invoices */}
      {(filter === 'all' || filter === 'bills') && unpaidInvoices.length > 0 && (
        <Card
          title="💳 Outstanding Professional Invoices"
          subtitle="Tax consultancy fee invoices with balance due"
          action={
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onNavigateTab('invoices')}
              rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
              className="text-xs font-bold text-brand-600"
            >
              View Invoices Tab
            </Button>
          }
          className="border-slate-200 shadow-2xs"
        >
          <div className="space-y-3">
            {unpaidInvoices.map((inv) => (
              <div
                key={inv.id}
                className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-brand-600 text-sm">{inv.invoiceNumber}</span>
                    <span className="px-2 py-0.5 rounded-full font-bold text-[10px] bg-rose-50 text-rose-700 border border-rose-200">
                      Payment Due
                    </span>
                  </div>
                  <span className="text-slate-500 block mt-0.5">
                    Invoice Date: {inv.invoiceDate} • Due Date: {inv.dueDate}
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <span className="text-[10px] text-slate-400 block uppercase font-bold">Balance Due</span>
                    <span className="text-base font-black text-rose-600 block">
                      {formatCurrency(inv.balanceDue || inv.totalAmount || inv.total)}
                    </span>
                  </div>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => onNavigateTab('invoices')}
                    className="text-xs font-bold shrink-0"
                  >
                    View Invoice
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* EMPTY STATE: All Caught Up! */}
      {totalActionRequiredCount === 0 && (
        <div className="bg-gradient-to-b from-white to-emerald-50/30 p-8 sm:p-10 rounded-2xl border border-emerald-200 text-center space-y-4 shadow-2xs">
          <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 mx-auto shadow-xs">
            <CheckCheck className="w-8 h-8" />
          </div>
          <div className="space-y-1.5 max-w-md mx-auto">
            <h3 className="text-lg font-black text-slate-900 tracking-tight">
              ✨ You Are All Caught Up!
            </h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              There are no pending document requests or required corrections for your account. All
              necessary compliance proofs have been provided to your tax consultant.
            </p>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onNavigateTab('documents')}
              leftIcon={<FolderLock className="w-4 h-4 text-slate-600" />}
              className="text-xs font-bold"
            >
              Browse Document Vault
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                if (onInitiateContextualMessage) {
                  onInitiateContextualMessage({
                    contextType: 'GENERAL',
                    contextTitle: 'General Consultation',
                  });
                } else {
                  onNavigateTab('messages');
                }
              }}
              leftIcon={<MessageSquare className="w-4 h-4 text-brand-600" />}
              className="text-xs font-bold"
            >
              Message Consultant
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
