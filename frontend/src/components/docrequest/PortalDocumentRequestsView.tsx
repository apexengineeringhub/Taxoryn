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
} from 'lucide-react';
import { DocumentRequest, DocumentRequestItem } from '../../types';
import { documentRequestApi, documentApi } from '../../api/endpoints';

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
  const [expandedRequestId, setExpandedRequestId] = useState<string | null>(null);
  const [uploadingItemId, setUploadingItemId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      let list: DocumentRequest[] = [];
      if (isPracticeUser && clientId) {
        list = await documentRequestApi.getByClient(clientId);
      } else {
        list = await documentRequestApi.getPortalRequests();
      }
      setRequests(list);
      if (list.length > 0 && !expandedRequestId) {
        setExpandedRequestId(list[0].id);
      }
    } catch (err) {
      console.error('Failed to load portal document requests', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [clientId, isPracticeUser]);

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

      await fetchRequests();
      setFeedback({ type: 'success', message: 'Document uploaded successfully! Your practitioner has been notified.' });
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err?.response?.data?.message || 'Failed to upload document. Please try again.',
      });
    } finally {
      setUploadingItemId(null);
    }
  };

  const handleDownloadDoc = async (docId?: string, fileName?: string) => {
    if (!docId) return;
    try {
      const blob = await documentApi.download(docId);
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

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12 bg-white rounded-2xl border border-slate-200">
        <div className="w-6 h-6 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (requests.length === 0) {
    return (
      <div className="text-center p-10 bg-white rounded-2xl border border-slate-200">
        <div className="w-12 h-12 bg-emerald-100/60 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
          <CheckCircle2 className="w-6 h-6" />
        </div>
        <h4 className="text-sm font-bold text-slate-800">All Document Requests Completed</h4>
        <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
          You currently have no pending document requests from your tax consultant.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
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

      {requests.map((req) => {
        const isExpanded = expandedRequestId === req.id;
        const percent = req.totalItems > 0 ? Math.round((req.acceptedItems / req.totalItems) * 100) : 0;

        return (
          <div
            key={req.id}
            className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden transition-all"
          >
            {/* Request Summary Card Header */}
            <div
              onClick={() => setExpandedRequestId(isExpanded ? null : req.id)}
              className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 cursor-pointer hover:bg-slate-50/70 transition-colors"
            >
              <div className="space-y-1.5 flex-1">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-mono font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                    {req.requestNumber}
                  </span>
                  {req.status === 'COMPLETED' ? (
                    <span className="px-2.5 py-0.5 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">
                      All Items Completed
                    </span>
                  ) : (
                    <span className="px-2.5 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">
                      {req.pendingItems + req.rejectedItems} Pending Action
                    </span>
                  )}
                  {req.isOverdue && (
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200 rounded-full">
                      Due Date Passed
                    </span>
                  )}
                </div>

                <h3 className="text-base font-bold text-slate-900">{req.purpose}</h3>

                <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500">
                  {req.dueDate && (
                    <span className="flex items-center gap-1 font-semibold text-slate-700">
                      <Calendar className="w-3.5 h-3.5 text-emerald-600" />
                      Submission Due: {req.dueDate}
                    </span>
                  )}
                  <span>
                    Consultant: <strong className="text-slate-700">{req.requestedByName || 'Your Tax Consultant'}</strong>
                  </span>
                </div>
              </div>

              {/* Progress & Toggle */}
              <div className="flex items-center space-x-4">
                <div className="text-right">
                  <span className="text-xs font-bold text-slate-800">
                    {req.acceptedItems + req.uploadedItems} / {req.totalItems} Uploaded
                  </span>
                  <div className="w-24 h-1.5 bg-slate-100 rounded-full mt-1 overflow-hidden">
                    <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${percent}%` }} />
                  </div>
                </div>

                <div className="p-1.5 text-slate-400 rounded-lg hover:bg-slate-200 transition-colors">
                  {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                </div>
              </div>
            </div>

            {/* Expanded Item Checklist */}
            {isExpanded && (
              <div className="border-t border-slate-100 p-5 bg-slate-50/50 space-y-4">
                {req.message && (
                  <div className="p-3.5 bg-emerald-50/60 border border-emerald-200/80 rounded-xl text-xs text-emerald-950 flex items-start gap-2.5">
                    <MessageSquare className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold">Message from Consultant: </span>
                      <span>{req.message}</span>
                    </div>
                  </div>
                )}

                <div className="space-y-3">
                  {req.items.map((item) => (
                    <div
                      key={item.id}
                      className="p-4 bg-white rounded-xl border border-slate-200 shadow-2xs space-y-3"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-start space-x-3">
                          <div className="p-2 bg-slate-50 border border-slate-200 text-emerald-600 rounded-xl mt-0.5">
                            <FileText className="w-4 h-4" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <h4 className="text-xs font-bold text-slate-900">{item.title}</h4>
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

                      {/* Rejection Notice Banner */}
                      {item.status === 'REJECTED' && item.rejectionReason && (
                        <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs space-y-1">
                          <div className="font-bold text-rose-800 flex items-center gap-1.5">
                            <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                            Correction Requested by Consultant:
                          </div>
                          <p className="text-rose-900 font-medium pl-5">{item.rejectionReason}</p>
                        </div>
                      )}

                      {/* Uploaded File Details */}
                      {item.uploadedDocumentName && (
                        <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg flex items-center justify-between text-xs">
                          <div className="flex items-center space-x-2 truncate">
                            <FileText className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                            <span className="font-medium text-slate-800 truncate">{item.uploadedDocumentName}</span>
                          </div>
                          <button
                            onClick={() => handleDownloadDoc(item.uploadedDocumentId, item.uploadedDocumentName)}
                            className="text-emerald-700 hover:text-emerald-900 font-bold inline-flex items-center gap-1 px-2 py-0.5 rounded hover:bg-emerald-50 transition-colors"
                          >
                            <Download className="w-3 h-3" /> Download
                          </button>
                        </div>
                      )}

                      {/* Upload Action */}
                      <div className="pt-2 border-t border-slate-100 flex items-center justify-end">
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
                            onChange={(e) => handleFileUpload(item.id, e)}
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
      })}
    </div>
  );
};