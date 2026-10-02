import React, { useState, useEffect, useCallback } from 'react';
import { Bell, Clock, CheckCircle, XCircle, Plus, AlertTriangle, Filter, Users, ChevronDown } from 'lucide-react';
import { reminderApi } from '../api/endpoints';

type ReminderStatus = 'PENDING' | 'TRIGGERED' | 'COMPLETED' | 'CANCELLED';
type ReminderPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

interface Reminder {
  id: string;
  title: string;
  description?: string;
  reminderType: string;
  status: ReminderStatus;
  priority: ReminderPriority;
  targetUserName?: string;
  clientName?: string;
  taskTitle?: string;
  scheduledAt: string;
  overdue: boolean;
  triggeredAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  notes?: string;
  createdAt: string;
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  TRIGGERED: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-100 text-gray-500',
};

const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'bg-gray-100 text-gray-600',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
};

const TYPE_LABELS: Record<string, string> = {
  TASK_DUE: 'Task Due',
  TASK_OVERDUE: 'Task Overdue',
  FOLLOW_UP: 'Follow-up',
  DOCUMENT_COLLECTION: 'Document Collection',
  GENERAL: 'General',
};

export function RemindersPage() {
  const [tab, setTab] = useState<'my' | 'team'>('my');
  const [reminders, setReminders] = useState<Reminder[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [overdueCount, setOverdueCount] = useState(0);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const fetchReminders = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, any> = { page, size: 20 };
      if (statusFilter) params.status = statusFilter;
      const data = tab === 'my'
        ? await reminderApi.getMyReminders(params)
        : await reminderApi.getTeamReminders(params);
      setReminders(data?.content || []);
      setTotalElements(data?.totalElements || 0);
    } catch {
      setFeedback({ type: 'error', message: 'Failed to load reminders' });
    } finally {
      setLoading(false);
    }
  }, [tab, statusFilter, page]);

  const fetchOverdueCount = useCallback(async () => {
    try {
      const count = await reminderApi.getOverdueCount();
      setOverdueCount(count || 0);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchReminders(); }, [fetchReminders]);
  useEffect(() => { fetchOverdueCount(); }, [fetchOverdueCount]);

  const handleComplete = async (id: string) => {
    try {
      await reminderApi.complete(id);
      setFeedback({ type: 'success', message: 'Reminder completed' });
      fetchReminders();
      fetchOverdueCount();
    } catch {
      setFeedback({ type: 'error', message: 'Failed to complete reminder' });
    }
  };

  const handleCancel = async (id: string) => {
    try {
      await reminderApi.cancel(id);
      setFeedback({ type: 'success', message: 'Reminder cancelled' });
      fetchReminders();
      fetchOverdueCount();
    } catch {
      setFeedback({ type: 'error', message: 'Failed to cancel reminder' });
    }
  };

  const handleCreate = async (data: any) => {
    try {
      await reminderApi.create(data);
      setFeedback({ type: 'success', message: 'Reminder created' });
      setShowCreateModal(false);
      fetchReminders();
    } catch {
      setFeedback({ type: 'error', message: 'Failed to create reminder' });
    }
  };

  const formatDate = (iso: string) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Bell className="w-7 h-7 text-indigo-600" />
          <h1 className="text-2xl font-bold text-gray-900">Reminders</h1>
          {overdueCount > 0 && (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700">
              <AlertTriangle className="w-3.5 h-3.5" />
              {overdueCount} overdue
            </span>
          )}
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition"
        >
          <Plus className="w-4 h-4" />
          New Reminder
        </button>
      </div>

      {feedback && (
        <div role={feedback.type === 'error' ? 'alert' : 'status'} className={`mb-4 rounded-lg border px-4 py-3 text-sm ${feedback.type === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>
          <div className="flex items-center justify-between gap-3">
            <span>{feedback.message}</span>
            <button type="button" onClick={() => setFeedback(null)} className="font-semibold" aria-label="Dismiss notification">×</button>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 mb-4 border-b border-gray-200">
        <button
          onClick={() => { setTab('my'); setPage(0); }}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition ${tab === 'my' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
        >
          <Bell className="w-4 h-4 inline mr-1.5" />
          My Reminders
        </button>
        <button
          onClick={() => { setTab('team'); setPage(0); }}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition ${tab === 'team' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
        >
          <Users className="w-4 h-4 inline mr-1.5" />
          Team Reminders
        </button>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-4">
        <Filter className="w-4 h-4 text-gray-400" />
        <select
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="TRIGGERED">Triggered</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <span className="text-sm text-gray-500">{totalElements} reminder{totalElements !== 1 ? 's' : ''}</span>
      </div>

      {/* Reminder List */}
      {loading ? (
        <div className="text-center py-16 text-gray-500">Loading reminders...</div>
      ) : reminders.length === 0 ? (
        <div className="text-center py-16">
          <Bell className="w-12 h-12 mx-auto text-gray-300 mb-3" />
          <p className="text-gray-500">No reminders found</p>
          <button onClick={() => setShowCreateModal(true)} className="mt-3 text-indigo-600 text-sm font-medium hover:underline">
            Create your first reminder
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {reminders.map((r) => (
            <div
              key={r.id}
              className={`bg-white rounded-xl border p-4 hover:shadow-sm transition ${r.overdue ? 'border-red-300 bg-red-50/30' : 'border-gray-200'}`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <h3 className={`font-semibold text-sm ${r.overdue ? 'text-red-800' : 'text-gray-900'}`}>{r.title}</h3>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[r.status]}`}>{r.status}</span>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${PRIORITY_COLORS[r.priority]}`}>{r.priority}</span>
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-700">{TYPE_LABELS[r.reminderType] || r.reminderType}</span>
                    {r.overdue && (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700">
                        <AlertTriangle className="w-3 h-3" /> Overdue
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-4 text-xs text-gray-500 mt-1">
                    <span className="inline-flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {formatDate(r.scheduledAt)}
                    </span>
                    {r.targetUserName && <span>→ {r.targetUserName}</span>}
                    {r.taskTitle && <span className="text-indigo-600">📋 {r.taskTitle}</span>}
                    {r.clientName && <span>👤 {r.clientName}</span>}
                  </div>
                  {r.description && <p className="text-xs text-gray-500 mt-1 line-clamp-1">{r.description}</p>}
                </div>
                {(r.status === 'PENDING' || r.status === 'TRIGGERED') && (
                  <div className="flex items-center gap-1.5 flex-shrink-0">
                    <button
                      onClick={() => handleComplete(r.id)}
                      className="p-1.5 rounded-lg text-green-600 hover:bg-green-50 transition"
                      title="Complete"
                    >
                      <CheckCircle className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleCancel(r.id)}
                      className="p-1.5 rounded-lg text-red-500 hover:bg-red-50 transition"
                      title="Cancel"
                    >
                      <XCircle className="w-5 h-5" />
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalElements > 20 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40"
          >
            Previous
          </button>
          <span className="px-3 py-1.5 text-sm text-gray-500">Page {page + 1}</span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={reminders.length < 20}
            className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <CreateReminderModal
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreate}
        />
      )}
    </div>
  );
}

function CreateReminderModal({ onClose, onCreate }: { onClose: () => void; onCreate: (data: any) => void }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [reminderType, setReminderType] = useState('GENERAL');
  const [priority, setPriority] = useState('MEDIUM');
  const [notes, setNotes] = useState('');
  const [validationError, setValidationError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !scheduledAt) {
      setValidationError('Title and scheduled time are required');
      return;
    }
    setValidationError('');
    onCreate({
      title: title.trim(),
      description: description.trim() || undefined,
      scheduledAt: new Date(scheduledAt).toISOString(),
      reminderType,
      priority,
      notes: notes.trim() || undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b">
          <h2 className="text-lg font-semibold text-gray-900">Create Reminder</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100">
            <XCircle className="w-5 h-5 text-gray-400" />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {validationError && <p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{validationError}</p>}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Title *</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="e.g. Call ABC regarding missing bank statement"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Scheduled At *</label>
              <input
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select
                value={reminderType}
                onChange={(e) => setReminderType(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              >
                <option value="GENERAL">General</option>
                <option value="FOLLOW_UP">Follow-up</option>
                <option value="DOCUMENT_COLLECTION">Document Collection</option>
                <option value="TASK_DUE">Task Due</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              placeholder="Internal notes..."
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200">
              Cancel
            </button>
            <button type="submit" className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700">
              Create Reminder
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
