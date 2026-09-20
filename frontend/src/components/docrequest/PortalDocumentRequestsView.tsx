import React, { useState, useEffect } from 'react';
import {
  FileText,
  Upload,
  CheckCircle2,
  XCircle,
  Clock,
  AlertTriangle,
  Calendar,
  ChevronDown,
  ChevronUp,
  MessageSquare,
  Sparkles,
  Download,
  Send,
  Plus,
  FileCheck,
  ShieldCheck,
  Eye,
  FolderOpen,
} from 'lucide-react';
import { DocumentRequest, DocumentRequestItem } from '../../types';
import { documentRequestApi, documentApi, portalApi } from '../../api/endpoints';
import { RequestDocumentModal } from './RequestDocumentModal';
import { SendDocumentModal } from './SendDocumentModal';
import { DeclineRequestModal } from './DeclineRequestModal';

interface PortalDocumentRequestsViewProps {
  isPracticeUser?: boolean;
  clientId?: string;
}

export const PortalDocumentRequestsView: React.FC<PortalDocumentRequestsViewProps> = ({
  isPracticeUser = false,
  clientId,
}) => {
  const [requests, setRequests] = useState<DocumentRequest[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeSubTab, setActiveSubTab] = useState<'pending_uploads' | 'delivered' | 'my_requests'>('pending_uploads');
  const [expandedRequestId, setExpandedRequestId] = useState<string | null>(null);
  const [uploadingItemId, setUploadingItemId] = useState<string | null>(null);
  const [isRequestModalOpen, setIsRequestModalOpen] = useState<boolean>(false);
  const [isSendDocModalOpen, setIsSendDocModalOpen] = useState<boolean>(false);
  const [fulfillingRequest, setFulfillingRequest] = useState<DocumentRequest | null>(null);
  const [decliningRequest, setDecliningRequest] = useState<DocumentRequest | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const activeFetchIdRef = React.useRef<number>(0);

  const fetchRequests = async () => {
    const fetchId = ++activeFetchIdRef.current;
    try {
      setLoading(true);
      setRequests([]);
      setExpandedRequestId(null);
      setFeedback(null);

      let list: DocumentRequest[] = [];
      if (isPracticeUser) {
        if (!clientId) {
          setLoading(false);
          return;
        }
        list = await documentRequestApi.getByClient(clientId);
      } else {
        list = await documentRequestApi.getPortalRequests();
      }

      if (activeFetchIdRef.current !== fetchId) {
        return;
      }

      setRequests(list);
      if (list && list.length > 0) {
        setExpandedRequestId(list[0].id);
      }
    } catch (err: any) {
      if (activeFetchIdRef.current === fetchId) {
        console.error('Failed to load document requests', err);
        setFeedback({ type: 'error', message: 'Failed to load document requests.' });
        setRequests([]);
      }
    } finally {
      if (activeFetchIdRef.current === fetchId) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [clientId, isPracticeUser]);

  // Separate categorized lists
  const pendingChecklistRequests = requests.filter(
    r => (r.exchangeType === 'DOCUMENT_REQUEST' || !r.exchangeType) && r.direction !== 'CLIENT_TO_PRACTITIONER'
  );

  const deliveredDocuments = requests.filter(
    r => r.deliveredDocumentId || r.exchangeType === 'DOCUMENT_DELIVERY' || (r.direction === 'PRACTITIONER_TO_CLIENT' && r.status === 'SENT')
  );

  const clientInitiatedRequests = requests.filter(
    r => r.exchangeType === 'ACKNOWLEDGEMENT_REQUEST' || r.direction === 'CLIENT_TO_PRACTITIONER'
  );

  const handleFileUpload = async (itemId: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploadingItemId(itemId);
      setFeedback(null);

      if (isPracticeUser) {
        await documentRequestApi.uploadItem(itemId, file);
      } else {
        await documentRequestApi.uploadPortalItem(itemId, file);
      }

      setFeedback({ type: 'success', message: 'Document uploaded successfully.' });
      fetchRequests();
    } catch (err: any) {
      console.error('Upload failed', err);
      const msg = err?.response?.data?.message || 'Failed to upload document. Please verify file type and size.';
      setFeedback({ type: 'error', message: msg });
    } finally {
      setUploadingItemId(null);
      e.target.value = '';
    }
  };

  const handleDownloadDoc = async (docId?: string, fileName?: string, requestId?: string) => {
    if (!docId) return;
    try {
      if (requestId && !isPracticeUser) {
        documentRequestApi.recordDownloaded(requestId).catch(() => {});
      }
      let blob: Blob;
      if (!isPracticeUser) {
        try {
          blob = await portalApi.downloadDocument(docId);
        } catch {
          blob = await documentApi.download(docId);
        }
      } else {
        blob = await documentApi.download(docId);
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'document.pdf';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setFeedback({ type: 'error', message: 'Failed to download document.' });
    }
  };

  const handlePreviewDoc = async (docId?: string, requestId?: string) => {
    if (!docId) return;
    try {
      if (requestId && !isPracticeUser) {
        documentRequestApi.recordViewed(requestId).catch(() => {});
      }
      let blob: Blob;
      if (!isPracticeUser) {
        try {
          blob = await portalApi.previewDocument(docId);
        } catch {
          blob = await documentApi.download(docId);
        }
      } else {
        blob = await documentApi.download(docId);
      }
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => {
        window.URL.revokeObjectURL(url);
      }, 10000);
    } catch (err) {
      setFeedback({ type: 'error', message: 'Failed to preview document.' });
    }
  };

  const getItemBadge = (item: DocumentRequestItem) => {
    switch (item.status) {
      case 'ACCEPTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> Accepted
          </span>
        );
      case 'UPLOADED':
      case 'UNDER_REVIEW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-blue-100 text-blue-800 rounded-full">
            <Clock className="w-3.5 h-3.5 text-blue-600" /> Uploaded (Under Review)
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-rose-100 text-rose-800 rounded-full">
            <XCircle className="w-3.5 h-3.5 text-rose-600" /> Action Required
          </span>
        );
      case 'PENDING':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">
            <Clock className="w-3.5 h-3.5 text-amber-600" /> Upload Pending
          </span>
        );
    }
  };

  const getCategoryBadge = (cat?: string) => {
    switch (cat) {
      case 'ACKNOWLEDGEMENT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">Acknowledgement</span>;
      case 'RETURN_COPY':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800">Return Copy</span>;
      case 'TAX_DOCUMENT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800">Tax Document</span>;
      case 'CERTIFICATE':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">Certificate</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">{cat || 'Document'}</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12 bg-white rounded-2xl border border-slate-200">
        <div className="w-6 h-6 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Top Bar with Dynamic Persona CTA */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xs">
        <div>
          <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <FileText className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            {isPracticeUser ? 'Document Exchange & Deliverables' : 'Document Exchange & Acknowledgements'}
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            {isPracticeUser
              ? 'Deliver filed tax acknowledgements, track client uploads, and fulfill client requests.'
              : 'Submit required tax proofs, download filed acknowledgements, or request returns from your practitioner.'}
          </p>
        </div>

        {isPracticeUser ? (
          <button
            onClick={() => setIsSendDocModalOpen(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-sm transition-all shrink-0"
          >
            <Send className="w-4 h-4" />
            <span>Deliver Document to Client</span>
          </button>
        ) : (
          <button
            onClick={() => setIsRequestModalOpen(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-sm transition-all shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>Request a Document / Ack</span>
          </button>
        )}
      </div>

      {/* Subtab Navigation */}
      <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 pb-2 overflow-x-auto no-scrollbar">
        <button
          onClick={() => setActiveSubTab('pending_uploads')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 flex items-center gap-2 ${
            activeSubTab === 'pending_uploads'
              ? 'bg-slate-900 text-white shadow-2xs dark:bg-white dark:text-slate-900'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400'
          }`}
        >
          <span>{isPracticeUser ? 'Client Upload Checklists' : 'Required Uploads'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] font-extrabold bg-amber-500/20 text-amber-700 dark:text-amber-300">
            {pendingChecklistRequests.length}
          </span>
        </button>

        <button
          onClick={() => setActiveSubTab('delivered')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 flex items-center gap-2 ${
            activeSubTab === 'delivered'
              ? 'bg-slate-900 text-white shadow-2xs dark:bg-white dark:text-slate-900'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400'
          }`}
        >
          <span>{isPracticeUser ? 'Delivered to Client' : 'Delivered Acknowledgements'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] font-extrabold bg-emerald-500/20 text-emerald-700 dark:text-emerald-300">
            {deliveredDocuments.length}
          </span>
        </button>

        <button
          onClick={() => setActiveSubTab('my_requests')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 flex items-center gap-2 ${
            activeSubTab === 'my_requests'
              ? 'bg-slate-900 text-white shadow-2xs dark:bg-white dark:text-slate-900'
              : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400'
          }`}
        >
          <span>{isPracticeUser ? 'Client Requests (Acks / Returns)' : 'My Requests to Consultant'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] font-extrabold bg-blue-500/20 text-blue-700 dark:text-blue-300">
            {clientInitiatedRequests.length}
          </span>
        </button>
      </div>

      {feedback && (
        <div
          className={`p-4 rounded-xl text-xs font-semibold flex items-center justify-between ${
            feedback.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-rose-50 text-rose-800 border border-rose-200'
          }`}
        >
          <span>{feedback.message}</span>
          <button onClick={() => setFeedback(null)} className="p-1 hover:opacity-75">
            &times;
          </button>
        </div>
      )}

      {/* SUBTAB 1: Required Uploads / Checklists */}
      {activeSubTab === 'pending_uploads' && (
        <div className="space-y-4">
          {pendingChecklistRequests.length === 0 ? (
            <div className="text-center p-10 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800">
              <div className="w-12 h-12 bg-emerald-100/60 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h4 className="text-sm font-bold text-slate-800 dark:text-white">
                {isPracticeUser ? 'No Pending Checklist Requests' : 'All Checklist Uploads Completed'}
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                {isPracticeUser
                  ? 'There are no active document checklist requests pending for this client.'
                  : 'You currently have no pending document requests from your tax consultant.'}
              </p>
            </div>
          ) : (
            pendingChecklistRequests.map(req => {
              const isExpanded = expandedRequestId === req.id;
              const percent = req.totalItems > 0 ? Math.round((req.acceptedItems / req.totalItems) * 100) : 0;

              return (
                <div
                  key={req.id}
                  className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xs overflow-hidden transition-all"
                >
                  <div
                    onClick={() => setExpandedRequestId(isExpanded ? null : req.id)}
                    className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 cursor-pointer hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <div className="space-y-1.5 flex-1">
                      <div className="flex items-center space-x-2">
                        <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded">
                          {req.requestNumber}
                        </span>
                        {req.status === 'COMPLETED' ? (
                          <span className="px-2.5 py-0.5 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">
                            All Items Completed
                          </span>
                        ) : (
                          <span className="px-2.5 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">
                            {req.pendingItems + req.rejectedItems} {req.pendingItems + req.rejectedItems === 1 ? 'Pending Action' : 'Pending Actions'}
                          </span>
                        )}
                        {req.isOverdue && (
                          <span className="px-2 py-0.5 text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200 rounded-full">
                            Due Date Passed
                          </span>
                        )}
                      </div>

                      <h3 className="text-base font-bold text-slate-900 dark:text-white">{req.purpose}</h3>

                      <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
                        {req.dueDate && (
                          <span className="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
                            <Calendar className="w-3.5 h-3.5 text-emerald-600" />
                            Submission Due: {req.dueDate}
                          </span>
                        )}
                        {req.financialYear && <span>FY: {req.financialYear}</span>}
                        {req.assessmentYear && <span>AY: {req.assessmentYear}</span>}
                        <span>• {req.totalItems} Required {req.totalItems === 1 ? 'Proof' : 'Proofs'}</span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-4 shrink-0">
                      <div className="text-right hidden sm:block">
                        <div className="text-xs font-bold text-slate-800 dark:text-slate-200">{percent}% Complete</div>
                        <div className="w-24 bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden mt-1">
                          <div
                            className="bg-emerald-500 h-full transition-all duration-300"
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </div>

                      <button
                        className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
                        aria-label={isExpanded ? 'Collapse request details' : 'Expand request details'}
                      >
                        {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                      </button>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="border-t border-slate-100 dark:border-slate-800 p-5 bg-slate-50/50 dark:bg-slate-800/30 space-y-4">
                      {req.message && (
                        <div className="p-3.5 bg-emerald-50/60 dark:bg-emerald-950/30 border border-emerald-200/80 dark:border-emerald-900/40 rounded-xl text-xs text-emerald-950 dark:text-emerald-200 flex items-start gap-2.5">
                          <MessageSquare className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                          <div>
                            <span className="font-bold">Instructions: </span>
                            <span>{req.message}</span>
                          </div>
                        </div>
                      )}

                      <div className="space-y-3">
                        {req.items.map(item => (
                          <div
                            key={item.id}
                            className="p-4 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-2xs space-y-3"
                          >
                            <div className="flex items-start justify-between">
                              <div className="flex items-start space-x-3">
                                <div className="p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-emerald-600 rounded-xl mt-0.5">
                                  <FileText className="w-4 h-4" />
                                </div>
                                <div>
                                  <div className="flex items-center gap-2">
                                    <h4 className="text-xs font-bold text-slate-900 dark:text-white">{item.title}</h4>
                                    {item.required && (
                                      <span className="text-[10px] font-bold text-rose-600 bg-rose-50 px-1.5 py-0.2 rounded">
                                        Required
                                      </span>
                                    )}
                                  </div>
                                  {item.description && (
                                    <p className="text-[11px] text-slate-500 mt-0.5">{item.description}</p>
                                  )}
                                </div>
                              </div>

                              <div>{getItemBadge(item)}</div>
                            </div>

                            {item.status === 'REJECTED' && item.rejectionReason && (
                              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs space-y-1">
                                <div className="font-bold text-rose-800 flex items-center gap-1.5">
                                  <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                                  Correction Requested:
                                </div>
                                <p className="text-rose-900 font-medium pl-5">{item.rejectionReason}</p>
                              </div>
                            )}

                            {item.uploadedDocumentName && (
                              <div className="p-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg flex items-center justify-between text-xs">
                                <div className="flex items-center space-x-2 truncate">
                                  <FileText className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                                  <span className="font-medium text-slate-800 dark:text-slate-200 truncate">{item.uploadedDocumentName}</span>
                                </div>
                                <button
                                  onClick={() => handleDownloadDoc(item.uploadedDocumentId, item.uploadedDocumentName, req.id)}
                                  className="text-emerald-700 hover:text-emerald-900 font-bold inline-flex items-center gap-1 px-2 py-0.5 rounded hover:bg-emerald-50 transition-colors"
                                >
                                  <Download className="w-3 h-3" /> Download
                                </button>
                              </div>
                            )}

                            <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-end">
                              <label className="cursor-pointer inline-flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-bold text-slate-900 bg-[#00d1a3] hover:bg-[#00b388] rounded-lg shadow-2xs transition-all">
                                <Upload className="w-3.5 h-3.5" />
                                <span>
                                  {uploadingItemId === item.id
                                    ? 'Uploading...'
                                    : item.uploadedDocumentId
                                    ? 'Replace Document'
                                    : 'Upload Document'}
                                </span>
                                <input
                                  type="file"
                                  onChange={e => handleFileUpload(item.id, e)}
                                  disabled={uploadingItemId === item.id}
                                  className="hidden"
                                />
                              </label>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      )}

      {/* SUBTAB 2: Delivered Documents / Acknowledgements */}
      {activeSubTab === 'delivered' && (
        <div className="space-y-4">
          {deliveredDocuments.length === 0 ? (
            <div className="text-center p-10 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800">
              <div className="w-12 h-12 bg-blue-100/60 text-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
                <FolderOpen className="w-6 h-6" />
              </div>
              <h4 className="text-sm font-bold text-slate-800 dark:text-white">
                {isPracticeUser ? 'No Documents Delivered to Client Yet' : 'No Delivered Documents Yet'}
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                {isPracticeUser
                  ? "Use 'Deliver Document to Client' above to send ITR-V acknowledgements, tax audit reports, or filed return summaries."
                  : 'Filed tax returns, ITR acknowledgements, and computation sheets delivered by your consultant will appear here.'}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3.5">
              {deliveredDocuments.map(doc => (
                <div
                  key={doc.id}
                  className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xs hover:border-blue-300 dark:hover:border-blue-800 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded">
                        {doc.requestNumber}
                      </span>
                      {getCategoryBadge(doc.category)}
                      {doc.status === 'DOWNLOADED' ? (
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">
                          {isPracticeUser ? 'Client Downloaded' : 'Downloaded'}
                        </span>
                      ) : doc.status === 'VIEWED' ? (
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700">
                          {isPracticeUser ? 'Client Viewed' : 'Viewed'}
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                          {isPracticeUser ? 'Delivered' : 'New Deliverable'}
                        </span>
                      )}
                    </div>

                    <div>
                      <h4 className="text-sm font-bold text-slate-900 dark:text-white">
                        {doc.purpose}
                      </h4>
                      {doc.deliveredDocumentName && (
                        <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mt-0.5 flex items-center gap-1.5">
                          <FileText className="w-3.5 h-3.5 text-blue-500" />
                          <span>{doc.deliveredDocumentName}</span>
                          {doc.deliveredDocumentSize && (
                            <span>• {((doc.deliveredDocumentSize || 0) / 1024).toFixed(1)} KB</span>
                          )}
                        </p>
                      )}
                    </div>

                    {doc.message && (
                      <p className="text-xs text-slate-600 dark:text-slate-300 italic bg-slate-50 dark:bg-slate-800/50 p-2.5 rounded-lg">
                        "{doc.message}"
                      </p>
                    )}

                    <div className="flex items-center gap-3 text-[11px] text-slate-400">
                      {doc.deliveredAt && (
                        <span>Delivered: {new Date(doc.deliveredAt).toLocaleDateString()}</span>
                      )}
                      {doc.financialYear && <span>FY: {doc.financialYear}</span>}
                      {doc.assessmentYear && <span>AY: {doc.assessmentYear}</span>}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => handlePreviewDoc(doc.deliveredDocumentId, doc.id)}
                      className="px-3 py-2 text-xs font-bold text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 rounded-xl transition-colors inline-flex items-center gap-1.5"
                    >
                      <Eye className="w-3.5 h-3.5" /> Preview
                    </button>
                    <button
                      onClick={() => handleDownloadDoc(doc.deliveredDocumentId, doc.deliveredDocumentName, doc.id)}
                      className="px-4 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl transition-all shadow-md shadow-blue-500/20 inline-flex items-center gap-1.5"
                    >
                      <Download className="w-3.5 h-3.5" /> Download
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* SUBTAB 3: Client Requests / My Requests */}
      {activeSubTab === 'my_requests' && (
        <div className="space-y-4">
          {clientInitiatedRequests.length === 0 ? (
            <div className="text-center p-10 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800">
              <div className="w-12 h-12 bg-blue-100/60 text-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
                <Send className="w-6 h-6" />
              </div>
              <h4 className="text-sm font-bold text-slate-800 dark:text-white">
                {isPracticeUser ? 'No Client Requests Pending' : 'No Requests Submitted Yet'}
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1 mb-3">
                {isPracticeUser
                  ? 'There are currently no acknowledgement or return copy requests from this client.'
                  : 'Need your ITR acknowledgement, computation sheet, or tax certificate? Request it from your consultant.'}
              </p>
              {!isPracticeUser && (
                <button
                  onClick={() => setIsRequestModalOpen(true)}
                  className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-sm transition-all"
                >
                  <Plus className="w-4 h-4" /> Request a Document
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3.5">
              {clientInitiatedRequests.map(req => (
                <div
                  key={req.id}
                  className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xs space-y-3"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded">
                        {req.requestNumber}
                      </span>
                      {getCategoryBadge(req.category)}
                    </div>

                    <div>
                      {req.status === 'DECLINED' ? (
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
                          Declined
                        </span>
                      ) : req.status === 'COMPLETED' || req.deliveredDocumentId ? (
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                          Delivered / Fulfilled
                        </span>
                      ) : (
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
                          {isPracticeUser ? 'Action Required: Client Waiting for Response' : 'Pending Practitioner Response'}
                        </span>
                      )}
                    </div>
                  </div>

                  <div>
                    <h4 className="text-sm font-bold text-slate-900 dark:text-white">{req.purpose}</h4>
                    {req.message && (
                      <p className="text-xs text-slate-500 mt-0.5">
                        {isPracticeUser ? `Client's Note: "${req.message}"` : `Your Note: "${req.message}"`}
                      </p>
                    )}
                  </div>

                  {/* If declined, display decline notice */}
                  {req.status === 'DECLINED' && req.declineReason && (
                    <div className="p-3 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/40 rounded-xl text-xs space-y-1">
                      <div className="font-bold text-rose-800 dark:text-rose-300 flex items-center gap-1.5">
                        <XCircle className="w-3.5 h-3.5 text-rose-600" />
                        {isPracticeUser ? 'Decline Explanation Provided:' : 'Explanation from Consultant:'}
                      </div>
                      <p className="text-rose-900 dark:text-rose-200 pl-5">{req.declineReason}</p>
                    </div>
                  )}

                  {/* If fulfilled with document */}
                  {req.deliveredDocumentId && (
                    <div className="p-3 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-900/40 rounded-xl flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <FileCheck className="w-4 h-4 text-emerald-600" />
                        <span className="font-semibold text-emerald-900 dark:text-emerald-200">
                          {req.deliveredDocumentName || 'Delivered File'}
                        </span>
                      </div>
                      <button
                        onClick={() => handleDownloadDoc(req.deliveredDocumentId, req.deliveredDocumentName, req.id)}
                        className="px-3 py-1 bg-emerald-600 text-white font-bold rounded-lg hover:bg-emerald-700 transition-colors inline-flex items-center gap-1"
                      >
                        <Download className="w-3 h-3" /> Download Deliverable
                      </button>
                    </div>
                  )}

                  {/* Practitioner action buttons to fulfill or decline from the portal hub */}
                  {isPracticeUser && !req.deliveredDocumentId && req.status !== 'DECLINED' && req.status !== 'COMPLETED' && (
                    <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-end gap-2">
                      <button
                        onClick={() => setDecliningRequest(req)}
                        className="px-3 py-1.5 text-xs font-bold text-rose-700 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100 rounded-xl transition-colors inline-flex items-center gap-1.5"
                      >
                        <XCircle className="w-3.5 h-3.5" /> Decline Request
                      </button>
                      <button
                        onClick={() => setFulfillingRequest(req)}
                        className="px-3.5 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-sm transition-all inline-flex items-center gap-1.5"
                      >
                        <Send className="w-3.5 h-3.5" /> Fulfill & Deliver Document
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modal: Client Requests Document (only shown to clients) */}
      {!isPracticeUser && isRequestModalOpen && (
        <RequestDocumentModal
          isOpen={isRequestModalOpen}
          onClose={() => setIsRequestModalOpen(false)}
          onSuccess={() => {
            fetchRequests();
            setActiveSubTab('my_requests');
          }}
        />
      )}

      {/* Modal: Practitioner Delivers Document Proactively */}
      {isPracticeUser && isSendDocModalOpen && (
        <SendDocumentModal
          isOpen={isSendDocModalOpen}
          onClose={() => setIsSendDocModalOpen(false)}
          preselectedClientId={clientId}
          onSuccess={() => {
            setIsSendDocModalOpen(false);
            fetchRequests();
            setActiveSubTab('delivered');
            setFeedback({ type: 'success', message: 'Document delivered to client successfully.' });
          }}
        />
      )}

      {/* Modal: Practitioner Fulfills Client Request */}
      {isPracticeUser && fulfillingRequest && (
        <SendDocumentModal
          isOpen={!!fulfillingRequest}
          onClose={() => setFulfillingRequest(null)}
          fulfillingRequest={fulfillingRequest}
          preselectedClientId={clientId || fulfillingRequest.clientId}
          onSuccess={() => {
            setFulfillingRequest(null);
            fetchRequests();
            setActiveSubTab('delivered');
            setFeedback({ type: 'success', message: 'Document delivered to client successfully.' });
          }}
        />
      )}

      {/* Modal: Practitioner Declines Client Request */}
      {isPracticeUser && decliningRequest && (
        <DeclineRequestModal
          isOpen={!!decliningRequest}
          onClose={() => setDecliningRequest(null)}
          request={decliningRequest}
          onSuccess={() => {
            setDecliningRequest(null);
            fetchRequests();
            setFeedback({ type: 'success', message: 'Request declined.' });
          }}
        />
      )}
    </div>
  );
};