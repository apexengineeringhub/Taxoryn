import React, { useState, useEffect, useCallback } from 'react';
import {
  MessageSquare,
  Send,
  CheckCheck,
  Eye,
  AlertCircle,
  Clock,
  RotateCcw,
  RefreshCw,
  Search,
  Filter,
  CheckCircle2,
  X,
  Phone,
  Layers,
  Sparkles,
} from 'lucide-react';
import { whatsappApi } from '../api/endpoints';
import { WhatsAppMessageRecord, WhatsAppIntegrationStatus } from '../types';
import { Modal } from '../components/common/Modal';
import { Button } from '../components/common/Button';
import clsx from 'clsx';

export const WhatsAppMessagesPage: React.FC = () => {
  const [messages, setMessages] = useState<WhatsAppMessageRecord[]>([]);
  const [status, setStatus] = useState<WhatsAppIntegrationStatus | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMessage, setSelectedMessage] = useState<WhatsAppMessageRecord | null>(null);
  const [resendingId, setResendingId] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const [statusRes, messagesRes] = await Promise.all([
        whatsappApi.getStatus().catch(() => null),
        whatsappApi.getMessages({ page, size: pageSize }),
      ]);
      if (statusRes) {
        setStatus(statusRes);
      }
      setMessages(messagesRes?.content || []);
      setTotalElements(messagesRes?.totalElements || 0);
    } catch (err: any) {
      console.error('Failed to load WhatsApp messages', err);
      setErrorMessage(err?.response?.data?.message || 'Failed to load WhatsApp notification history');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleResend = async (id: string) => {
    try {
      setResendingId(id);
      setActionSuccess(null);
      setErrorMessage(null);
      await whatsappApi.resendMessage(id);
      setActionSuccess('Message queued for redelivery successfully!');
      loadData();
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || 'Failed to resend WhatsApp notification');
    } finally {
      setResendingId(null);
    }
  };

  const filteredMessages = messages.filter((m) => {
    const matchesStatus = statusFilter === 'ALL' || m.status === statusFilter;
    const matchesSearch =
      searchTerm.trim() === '' ||
      m.recipientPhone.includes(searchTerm.trim()) ||
      m.templateType.toLowerCase().includes(searchTerm.trim().toLowerCase()) ||
      (m.messageContent && m.messageContent.toLowerCase().includes(searchTerm.trim().toLowerCase()));
    return matchesStatus && matchesSearch;
  });

  const getStatusBadge = (msgStatus: string) => {
    switch (msgStatus) {
      case 'READ':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <Eye className="w-3 h-3" /> Read
          </span>
        );
      case 'DELIVERED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
            <CheckCheck className="w-3 h-3" /> Delivered
          </span>
        );
      case 'SENT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-sky-50 text-sky-700 border border-sky-200">
            <Send className="w-3 h-3" /> Sent
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertCircle className="w-3 h-3" /> Failed
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3 h-3" /> Pending
          </span>
        );
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <MessageSquare className="w-7 h-7 text-emerald-600" />
            WhatsApp Notification Center
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time delivery tracking, automated invoice & welcome alerts, and Meta Cloud API status
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            isLoading={isLoading}
            leftIcon={<RefreshCw className="w-4 h-4" />}
          >
            Refresh
          </Button>
        </div>
      </div>

      {/* Success / Error Alerts */}
      {actionSuccess && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-emerald-800 text-sm">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button onClick={() => setActionSuccess(null)} className="text-emerald-500 hover:text-emerald-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center justify-between text-rose-800 text-sm">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-rose-500 hover:text-rose-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Metrics Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500">Provider Status</span>
            <span
              className={clsx(
                'w-2.5 h-2.5 rounded-full',
                status?.enabled ? 'bg-emerald-500' : 'bg-amber-400'
              )}
            />
          </div>
          <div className="mt-2 text-xl font-bold text-slate-900">
            {status?.provider || 'LOG'}
          </div>
          <div className="text-xs text-slate-500 mt-1">
            {status?.enabled ? 'Active & Delivering' : 'Disabled / Test Mode'}
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500">Messages Sent</span>
            <Send className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="mt-2 text-2xl font-bold text-emerald-600">
            {status?.totalMessagesSent || 0}
          </div>
          <div className="text-xs text-slate-500 mt-1">Delivered or active</div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500">Failed</span>
            <AlertCircle className="w-4 h-4 text-rose-600" />
          </div>
          <div className="mt-2 text-2xl font-bold text-rose-600">
            {status?.totalMessagesFailed || 0}
          </div>
          <div className="text-xs text-slate-500 mt-1">Requires retry</div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase text-slate-500">Pending</span>
            <Clock className="w-4 h-4 text-amber-500" />
          </div>
          <div className="mt-2 text-2xl font-bold text-amber-600">
            {status?.totalMessagesPending || 0}
          </div>
          <div className="text-xs text-slate-500 mt-1">In transit queue</div>
        </div>
      </div>

      {/* Filters & Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by phone, template..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 bg-white"
          >
            <option value="ALL">All Statuses</option>
            <option value="SENT">Sent</option>
            <option value="DELIVERED">Delivered</option>
            <option value="READ">Read</option>
            <option value="FAILED">Failed</option>
            <option value="PENDING">Pending</option>
          </select>
        </div>
      </div>

      {/* Message Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Recipient</th>
                <th className="px-4 py-3">Type & Template</th>
                <th className="px-4 py-3">Message Content</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Sent / Delivered</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredMessages.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-slate-400">
                    <MessageSquare className="w-10 h-10 mx-auto mb-2 opacity-30" />
                    <p className="font-medium">No WhatsApp delivery records found</p>
                    <p className="text-xs text-slate-400 mt-1">Notifications dispatched will appear here automatically</p>
                  </td>
                </tr>
              ) : (
                filteredMessages.map((msg) => (
                  <tr key={msg.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-4 py-3 font-mono font-medium text-slate-800 flex items-center gap-1.5">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      {msg.recipientPhone}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-800 text-xs">{msg.templateType}</div>
                      <div className="text-[11px] text-slate-400 font-mono">{msg.templateName}</div>
                    </td>
                    <td className="px-4 py-3 max-w-xs">
                      <p className="truncate text-xs text-slate-600">{msg.messageContent || 'Template message'}</p>
                    </td>
                    <td className="px-4 py-3">{getStatusBadge(msg.status)}</td>
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      <div>{msg.sentAt ? new Date(msg.sentAt).toLocaleString() : new Date(msg.createdAt).toLocaleString()}</div>
                      {msg.readAt && (
                        <div className="text-[11px] text-emerald-600 font-medium flex items-center gap-1 mt-0.5">
                          <Eye className="w-3 h-3" /> Read: {new Date(msg.readAt).toLocaleTimeString()}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right whitespace-nowrap space-x-2">
                      <button
                        onClick={() => setSelectedMessage(msg)}
                        className="text-xs font-semibold text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 px-2.5 py-1.5 rounded-lg transition-colors inline-flex items-center gap-1"
                      >
                        <Eye className="w-3 h-3" /> View
                      </button>
                      {(msg.status === 'FAILED' || msg.status === 'PENDING') && (
                        <button
                          onClick={() => handleResend(msg.id)}
                          disabled={resendingId === msg.id}
                          className="text-xs font-semibold text-emerald-700 hover:text-emerald-800 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-1.5 rounded-lg transition-colors inline-flex items-center gap-1 disabled:opacity-50"
                        >
                          <RotateCcw className={clsx('w-3 h-3', resendingId === msg.id && 'animate-spin')} />
                          Resend
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalElements > pageSize && (
          <div className="p-4 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
            <span>
              Showing {page * pageSize + 1} - {Math.min((page + 1) * pageSize, totalElements)} of {totalElements}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={(page + 1) * pageSize >= totalElements}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Message Details Modal */}
      {selectedMessage && (
        <Modal
          isOpen={true}
          onClose={() => setSelectedMessage(null)}
          title="WhatsApp Message Details"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-xs bg-slate-50 p-3 rounded-lg border border-slate-200">
              <div>
                <span className="text-slate-400 block">Recipient:</span>
                <span className="font-semibold text-slate-800">{selectedMessage.recipientPhone}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Status:</span>
                <span className="font-semibold">{getStatusBadge(selectedMessage.status)}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Template:</span>
                <span className="font-mono text-slate-800">{selectedMessage.templateName}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Provider Message ID:</span>
                <span className="font-mono text-slate-800 truncate block">{selectedMessage.providerMessageId || 'N/A'}</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-500 uppercase mb-1">Message Content</label>
              <div className="p-3 bg-slate-900 text-slate-100 rounded-lg font-mono text-xs whitespace-pre-wrap max-h-48 overflow-y-auto">
                {selectedMessage.messageContent || 'N/A'}
              </div>
            </div>

            {selectedMessage.errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
                <span className="font-semibold block mb-1">Error Information:</span>
                {selectedMessage.errorMessage}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              {(selectedMessage.status === 'FAILED' || selectedMessage.status === 'PENDING') && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    handleResend(selectedMessage.id);
                    setSelectedMessage(null);
                  }}
                  isLoading={resendingId === selectedMessage.id}
                  leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
                >
                  Resend Message
                </Button>
              )}
              <Button variant="outline" size="sm" onClick={() => setSelectedMessage(null)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
