import React, { useState } from 'react';
import {
  X,
  FileText,
  Calendar,
  CheckCircle2,
  XCircle,
  Clock,
  Download,
  Upload,
  Send,
  AlertTriangle,
  FileCheck,
  Ban,
  MessageSquare,
} from 'lucide-react';
import { DocumentRequest, DocumentRequestItem } from '../../types';
import { documentRequestApi, documentApi } from '../../api/endpoints';

interface DocumentRequestReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  request: DocumentRequest;
  onUpdate: () => void;
}

export const DocumentRequestReviewModal: React.FC<DocumentRequestReviewModalProps> = ({
  isOpen,
  onClose,
  request: initialRequest,
  onUpdate,
}) => {
  const [request, setRequest] = useState<DocumentRequest>(initialRequest);
  const [rejectingItemId, setRejectingItemId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [loadingAction, setLoadingAction] = useState<string | null>(null);
  const [uploadingItemId, setUploadingItemId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  if (!isOpen) return null;

  const handleAcceptItem = async (itemId: string) => {
    try {
      setLoadingAction(`accept-${itemId}`);
      setFeedback(null);
      const updated = await documentRequestApi.acceptItem(itemId);
      setRequest(updated);
      onUpdate();
      setFeedback({ type: 'success', message: 'Document item accepted successfully.' });
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to accept document item.' });
    } finally {
      setLoadingAction(null);
    }
  };

  const handleOpenRejectModal = (itemId: string) => {
    setRejectingItemId(itemId);
    setRejectionReason('');
  };

  const handleConfirmReject = async () => {
    if (!rejectingItemId || !rejectionReason.trim()) return;

    try {
      setLoadingAction(`reject-${rejectingItemId}`);
      setFeedback(null);
      const updated = await documentRequestApi.rejectItem(rejectingItemId, rejectionReason.trim());
      setRequest(updated);
      setRejectingItemId(null);
      setRejectionReason('');
      onUpdate();
      setFeedback({ type: 'success', message: 'Document item rejected. Correction notice sent to client.' });
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to reject document item.' });
    } finally {
      setLoadingAction(null);
    }
  };

  const handleFileUpload = async (itemId: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploadingItemId(itemId);
      setFeedback(null);
      const updated = await documentRequestApi.uploadItem(itemId, file);
      setRequest(updated);
      onUpdate();
      setFeedback({ type: 'success', message: 'Document uploaded on behalf of client successfully.' });
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to upload document.' });
    } finally {
      setUploadingItemId(null);
    }
  };

  const handleSendReminder = async () => {
    try {
      setLoadingAction('reminder');
      setFeedback(null);
      await documentRequestApi.sendReminder(request.id);
      setFeedback({ type: 'success', message: 'Document reminder sent to client via In-App & Email.' });
    } catch (err: any) {
      setFeedback({ type: 'error', message: err?.response?.data?.message || 'Failed to send reminder.' });
    } finally {
      setLoadingAction(null);
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

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="px-2.5 py-1 text-xs font-bold bg-emerald-100 text-emerald-800 rounded-full">Completed</span>;
      case 'PARTIALLY_COMPLETED':
        return <span className="px-2.5 py-1 text-xs font-bold bg-blue-100 text-blue-800 rounded-full">Partially Completed</span>;
      case 'SENT':
        return <span className="px-2.5 py-1 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">Pending Client Upload</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 text-xs font-bold bg-slate-100 text-slate-700 rounded-full">Cancelled</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-bold bg-slate-100 text-slate-700 rounded-full">{status}</span>;
    }
  };

  const getItemStatusBadge = (status: string) => {
    switch (status) {
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
            <Clock className="w-3.5 h-3.5 text-blue-600" /> Uploaded (Needs Review)
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-rose-100 text-rose-800 rounded-full">
            <XCircle className="w-3.5 h-3.5 text-rose-600" /> Needs Correction
          </span>
        );
      case 'PENDING':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">
            <Clock className="w-3.5 h-3.5 text-amber-600" /> Pending Upload
          </span>
        );
    }
  };

  const progressPercent = Math.round(
    (request.totalItems > 0 ? (request.acceptedItems / request.totalItems) * 100 : 0)
  );

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full overflow-hidden border border-slate-200 flex flex-col max-h-[92vh] animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="px-6 py-5 bg-gradient-to-r from-slate-900 via-slate-800 to-[#082e5b] text-white flex items-center justify-between">
          <div className="flex items-center space-x-3.5">
            <div className="p-2.5 bg-emerald-500/20 text-emerald-400 rounded-xl border border-emerald-500/30">
              <FileCheck className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-mono font-bold bg-slate-800 px-2 py-0.5 rounded border border-slate-700 text-emerald-400">
                  {request.requestNumber}
                </span>
                {getStatusBadge(request.status)}
                {request.isOverdue && (
                  <span className="px-2 py-0.5 text-[10px] font-bold bg-rose-500/30 text-rose-300 border border-rose-400/30 rounded-full">
                    Overdue
                  </span>
                )}
              </div>
              <h2 className="text-base font-bold text-white mt-1">{request.purpose}</h2>
              <p className="text-xs text-slate-300">
                Client: <span className="font-semibold text-white">{request.clientName}</span>
                {request.dueDate && (
                  <span className="ml-3">
                    Due Date: <span className="font-semibold text-white">{request.dueDate}</span>
                  </span>
                )}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Feedback Alert */}
        {feedback && (
          <div
            className={`px-6 py-3 text-xs font-semibold flex items-center justify-between ${
              feedback.type === 'success' ? 'bg-emerald-50 text-emerald-800 border-b border-emerald-100' : 'bg-rose-50 text-rose-800 border-b border-rose-100'
            }`}
          >
            <span>{feedback.message}</span>
            <button onClick={() => setFeedback(null)} className="p-0.5 hover:opacity-75">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Overview & Progress Bar */}
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-slate-700">
                Verification Progress: {request.acceptedItems} of {request.totalItems} Items Accepted
              </span>
              <span className="text-xs font-mono font-bold text-emerald-700">{progressPercent}%</span>
            </div>
            <div className="w-full h-2.5 bg-slate-200 rounded-full overflow-hidden">
              <div
                className="h-full bg-emerald-500 rounded-full transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              />
            </div>

            {request.message && (
              <div className="mt-3 p-3 bg-white rounded-xl border border-slate-200 text-xs text-slate-600 flex items-start gap-2">
                <MessageSquare className="w-4 h-4 text-slate-400 flex-shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold text-slate-700">Practitioner Note: </span>
                  <span>{request.message}</span>
                </div>
              </div>
            )}
          </div>

          {/* Checklist Items */}
          <div className="space-y-3.5">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Requested Documents ({request.items.length})
            </h3>

            {request.items.map((item) => (
              <div
                key={item.id}
                className="p-4 bg-white border border-slate-200 rounded-2xl shadow-sm hover:border-slate-300 transition-all space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className="p-2 bg-slate-100 text-slate-600 rounded-xl mt-0.5">
                      <FileText className="w-5 h-5 text-emerald-600" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="text-sm font-bold text-slate-900">{item.title}</h4>
                        {item.required ? (
                          <span className="text-[10px] font-bold px-2 py-0.5 bg-rose-50 text-rose-700 rounded border border-rose-200">
                            Required
                          </span>
                        ) : (
                          <span className="text-[10px] font-medium px-2 py-0.5 bg-slate-100 text-slate-600 rounded">
                            Optional
                          </span>
                        )}
                      </div>
                      {item.description && (
                        <p className="text-xs text-slate-500 mt-0.5">{item.description}</p>
                      )}
                    </div>
                  </div>

                  <div>{getItemStatusBadge(item.status)}</div>
                </div>

                {/* Uploaded File Info */}
                {item.uploadedDocumentName && (
                  <div className="p-3 bg-emerald-50/50 border border-emerald-100 rounded-xl flex items-center justify-between text-xs">
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      <span className="font-semibold text-emerald-950 truncate">
                        {item.uploadedDocumentName}
                      </span>
                      {item.uploadedDocumentSize && (
                        <span className="text-slate-500 text-[11px]">
                          ({Math.round(item.uploadedDocumentSize / 1024)} KB)
                        </span>
                      )}
                    </div>
                    <button
                      onClick={() => handleDownloadDoc(item.uploadedDocumentId, item.uploadedDocumentName)}
                      className="inline-flex items-center space-x-1 font-bold text-emerald-700 hover:text-emerald-900 bg-white px-2.5 py-1 rounded-lg border border-emerald-200 shadow-2xs hover:bg-emerald-50 transition-colors"
                    >
                      <Download className="w-3.5 h-3.5" />
                      <span>Download</span>
                    </button>
                  </div>
                )}

                {/* Rejection Notice Box */}
                {item.status === 'REJECTED' && item.rejectionReason && (
                  <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs space-y-1">
                    <div className="font-bold text-rose-800 flex items-center gap-1.5">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                      Rejection Reason (Sent to Client):
                    </div>
                    <p className="text-rose-900 font-medium pl-5">{item.rejectionReason}</p>
                  </div>
                )}

                {/* Action Buttons for this item */}
                <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                  <div className="text-[11px] text-slate-400">
                    {item.uploadedAt ? `Uploaded ${new Date(item.uploadedAt).toLocaleDateString()}` : 'Awaiting client submission'}
                  </div>

                  <div className="flex items-center space-x-2">
                    {/* Upload on behalf */}
                    <label className="cursor-pointer inline-flex items-center space-x-1 text-xs font-semibold text-slate-600 hover:text-slate-900 px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors">
                      <Upload className="w-3.5 h-3.5 text-slate-500" />
                      <span>{uploadingItemId === item.id ? 'Uploading...' : 'Upload Doc'}</span>
                      <input
                        type="file"
                        onChange={(e) => handleFileUpload(item.id, e)}
                        disabled={uploadingItemId === item.id}
                        className="hidden"
                      />
                    </label>

                    {item.status !== 'ACCEPTED' && item.uploadedDocumentId && (
                      <>
                        <button
                          onClick={() => handleAcceptItem(item.id)}
                          disabled={loadingAction === `accept-${item.id}`}
                          className="inline-flex items-center space-x-1 text-xs font-bold text-emerald-700 bg-emerald-100/80 hover:bg-emerald-200 px-3 py-1 rounded-lg transition-colors"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Accept</span>
                        </button>
                        <button
                          onClick={() => handleOpenRejectModal(item.id)}
                          disabled={loadingAction === `reject-${item.id}`}
                          className="inline-flex items-center space-x-1 text-xs font-bold text-rose-700 bg-rose-100/80 hover:bg-rose-200 px-3 py-1 rounded-lg transition-colors"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          <span>Reject</span>
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {request.status !== 'COMPLETED' && request.status !== 'CANCELLED' && (
              <button
                onClick={handleSendReminder}
                disabled={loadingAction === 'reminder'}
                className="inline-flex items-center space-x-1.5 text-xs font-bold text-slate-700 bg-white border border-slate-300 hover:bg-slate-100 px-3.5 py-2 rounded-xl transition-colors shadow-2xs"
              >
                <Send className="w-3.5 h-3.5 text-slate-500" />
                <span>{loadingAction === 'reminder' ? 'Sending Reminder...' : 'Send Reminder to Client'}</span>
              </button>
            )}
          </div>

          <button
            onClick={onClose}
            className="px-5 py-2 text-xs font-bold text-slate-700 bg-white border border-slate-300 hover:bg-slate-100 rounded-xl transition-colors"
          >
            Close
          </button>
        </div>
      </div>

      {/* Reject Reason Sub-Modal */}
      {rejectingItemId && (
        <div className="fixed inset-0 z-60 bg-slate-900/70 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150 border border-slate-200">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-rose-900 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600" />
                Reject Document & Request Correction
              </h3>
              <button onClick={() => setRejectingItemId(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Please explain why this document cannot be accepted. This reason will be shown to the client in the portal and emailed to them.
            </p>

            <textarea
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="e.g. Bank statement is incomplete or password-protected. Please upload unencrypted statements for all 12 months."
              rows={3}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-rose-500 focus:border-rose-500"
              autoFocus
            />

            <div className="flex items-center justify-end space-x-2 pt-2">
              <button
                onClick={() => setRejectingItemId(null)}
                className="px-3.5 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmReject}
                disabled={!rejectionReason.trim()}
                className="px-4 py-1.5 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg shadow-sm disabled:opacity-50"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};