import React, { useState, useEffect } from 'react';
import {
  FileText,
  Plus,
  Calendar,
  CheckCircle2,
  Clock,
  AlertCircle,
  ChevronRight,
  Send,
  Sparkles,
} from 'lucide-react';
import { DocumentRequest } from '../../types';
import { documentRequestApi } from '../../api/endpoints';
import { RequestDocumentsModal } from './RequestDocumentsModal';
import { DocumentRequestReviewModal } from './DocumentRequestReviewModal';

interface ClientDocumentRequestsTabProps {
  clientId: string;
  clientName: string;
}

export const ClientDocumentRequestsTab: React.FC<ClientDocumentRequestsTabProps> = ({
  clientId,
  clientName,
}) => {
  const [requests, setRequests] = useState<DocumentRequest[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [isRequestModalOpen, setIsRequestModalOpen] = useState<boolean>(false);
  const [selectedReviewRequest, setSelectedReviewRequest] = useState<DocumentRequest | null>(null);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const list = await documentRequestApi.getByClient(clientId);
      setRequests(list);
    } catch (err) {
      console.error('Failed to load document requests for client', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (clientId) {
      fetchRequests();
    }
  }, [clientId]);

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

  return (
    <div className="space-y-6">
      {/* Header & Quick Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
        <div>
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <FileText className="w-5 h-5 text-emerald-600" />
            Client Document Requests
            <span className="text-xs font-semibold bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full border border-emerald-200">
              {requests.length} Requests
            </span>
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            Request statutory tax documents, audit proofs, and return filing papers directly from {clientName}.
          </p>
        </div>

        <button
          onClick={() => setIsRequestModalOpen(true)}
          className="inline-flex items-center space-x-2 px-4 py-2 text-xs font-bold text-slate-900 bg-[#00d1a3] hover:bg-[#00b388] rounded-xl shadow-sm transition-all flex-shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Request Documents</span>
        </button>
      </div>

      {/* Requests List */}
      {loading ? (
        <div className="flex items-center justify-center p-12 bg-white rounded-2xl border border-slate-200">
          <div className="w-6 h-6 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : requests.length === 0 ? (
        <div className="text-center p-10 bg-slate-50 rounded-2xl border border-dashed border-slate-300">
          <div className="w-12 h-12 bg-emerald-100/60 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <FileText className="w-6 h-6" />
          </div>
          <h4 className="text-sm font-bold text-slate-800">No Document Requests Yet</h4>
          <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1 mb-4">
            Create your first document request checklist for ITR, GST, TDS, or Custom Tax Audit filings.
          </p>
          <button
            onClick={() => setIsRequestModalOpen(true)}
            className="inline-flex items-center space-x-1.5 px-4 py-2 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-xl transition-all shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create Document Request</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3.5">
          {requests.map((req) => {
            const percent = req.totalItems > 0 ? Math.round((req.acceptedItems / req.totalItems) * 100) : 0;

            return (
              <div
                key={req.id}
                onClick={() => setSelectedReviewRequest(req)}
                className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs hover:border-emerald-300 hover:shadow-md transition-all cursor-pointer group"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1.5">
                    <div className="flex items-center space-x-2">
                      <span className="text-xs font-mono font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                        {req.requestNumber}
                      </span>
                      {getStatusBadge(req.status)}
                      {req.isOverdue && (
                        <span className="px-2 py-0.5 text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200 rounded-full">
                          Overdue
                        </span>
                      )}
                    </div>
                    <h4 className="text-sm font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">
                      {req.purpose}
                    </h4>
                    <div className="flex items-center space-x-4 text-xs text-slate-500">
                      {req.dueDate && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          Due: {req.dueDate}
                        </span>
                      )}
                      <span>
                        Requested by: <strong className="text-slate-700">{req.requestedByName || 'Practitioner'}</strong>
                      </span>
                      <span>
                        Created: {new Date(req.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>

                  {/* Progress Stats & Arrow */}
                  <div className="flex items-center space-x-5">
                    <div className="text-right">
                      <div className="text-xs font-bold text-slate-800">
                        {req.acceptedItems} / {req.totalItems} Verified
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {req.pendingItems} Pending • {req.rejectedItems} Rejected
                      </div>
                      <div className="w-28 h-1.5 bg-slate-100 rounded-full mt-1.5 overflow-hidden">
                        <div
                          className="h-full bg-emerald-500 rounded-full"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                    </div>

                    <div className="p-2 bg-slate-50 group-hover:bg-emerald-50 text-slate-400 group-hover:text-emerald-600 rounded-xl transition-colors">
                      <ChevronRight className="w-5 h-5" />
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modals */}
      <RequestDocumentsModal
        isOpen={isRequestModalOpen}
        onClose={() => setIsRequestModalOpen(false)}
        clientId={clientId}
        clientName={clientName}
        onSuccess={fetchRequests}
      />

      {selectedReviewRequest && (
        <DocumentRequestReviewModal
          isOpen={!!selectedReviewRequest}
          onClose={() => setSelectedReviewRequest(null)}
          request={selectedReviewRequest}
          onUpdate={fetchRequests}
        />
      )}
    </div>
  );
};