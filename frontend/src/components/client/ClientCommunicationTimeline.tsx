import React, { useCallback, useEffect, useState } from 'react';
import { CalendarClock, Edit2, Mail, MessageCircle, Phone, Plus, Trash2, Users, Video, FileText } from 'lucide-react';
import { clientApi } from '../../api/endpoints';
import { useAuth } from '../../context/AuthContext';
import { ClientCommunication, ClientCommunicationRequest, ClientCommunicationType, ClientCommunicationVisibility } from '../../types';

const COMMUNICATION_TYPES: { value: ClientCommunicationType; label: string }[] = [
  { value: 'NOTE', label: 'Note' },
  { value: 'EMAIL', label: 'Email' },
  { value: 'PHONE_CALL', label: 'Phone Call' },
  { value: 'MEETING', label: 'Meeting' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
  { value: 'CLIENT_PORTAL', label: 'Client Portal' },
  { value: 'DOCUMENT_REQUEST', label: 'Document Request' },
  { value: 'TASK', label: 'Task' },
  { value: 'COMPLIANCE', label: 'Compliance' },
  { value: 'NOTICE', label: 'Notice' },
  { value: 'BILLING', label: 'Billing / Invoice' },
];

const toLocalInput = (value?: string) => {
  const date = value ? new Date(value) : new Date();
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

const fromLocalInput = (value: string) => new Date(value).toISOString();

export const ClientCommunicationTimeline: React.FC<{ clientId: string }> = ({ clientId }) => {
  const { user } = useAuth();
  const [entries, setEntries] = useState<ClientCommunication[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [followUpFilter, setFollowUpFilter] = useState('');
  const [editing, setEditing] = useState<ClientCommunication | null>(null);
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState({
    communicationType: 'NOTE' as ClientCommunicationType,
    subject: '', content: '', occurredAt: toLocalInput(), visibility: 'INTERNAL' as ClientCommunicationVisibility,
    followUpRequired: false, followUpDate: '',
  });

  const roleCodes = (user?.roles || []).map((role: any) => typeof role === 'string' ? role : role.code || '');
  const permissions = user?.permissions || [];
  const canCreate = permissions.includes('CLIENT_COMMUNICATION_CREATE') || roleCodes.some((role: string) =>
    ['SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER', 'MANAGER', 'TAX_MANAGER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'SENIOR_TAX_ASSOCIATE', 'STAFF', 'ACCOUNTANT', 'TAX_ASSOCIATE', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'EMPLOYEE'].includes(role));
  const canUpdate = permissions.includes('CLIENT_COMMUNICATION_UPDATE') || roleCodes.some((role: string) =>
    ['SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER', 'MANAGER', 'TAX_MANAGER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'SENIOR_TAX_ASSOCIATE'].includes(role));
  const canDelete = permissions.includes('CLIENT_COMMUNICATION_DELETE') || roleCodes.some((role: string) =>
    ['SUPER_ADMIN', 'TAXORYN_SUPERADMIN', 'ORG_ADMIN', 'PRACTICE_ADMIN', 'PRACTICE_OWNER', 'PARTNER'].includes(role));

  const loadEntries = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await clientApi.getCommunications(clientId, {
        page, size: 20, type: typeFilter || undefined,
        dateFrom: fromDate ? fromLocalInput(`${fromDate}T00:00`) : undefined,
        dateTo: toDate ? fromLocalInput(`${toDate}T23:59:59`) : undefined,
        followUpRequired: followUpFilter === '' ? undefined : followUpFilter === 'true',
      });
      setEntries(data?.content || []);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements || 0);
    } catch (loadError: any) {
      setError(loadError?.response?.data?.message || 'Failed to load communication timeline.');
    } finally {
      setLoading(false);
    }
  }, [clientId, page, typeFilter, fromDate, toDate, followUpFilter]);

  useEffect(() => { void loadEntries(); }, [loadEntries]);

  const openCreate = () => {
    setEditing(null);
    setForm({ communicationType: 'NOTE', subject: '', content: '', occurredAt: toLocalInput(), visibility: 'INTERNAL', followUpRequired: false, followUpDate: '' });
    setFormError('');
    setFormOpen(true);
  };

  const [formOpen, setFormOpen] = useState(false);

  const openEdit = (entry: ClientCommunication) => {
    setEditing(entry);
    setForm({
      communicationType: entry.communicationType, subject: entry.subject || '', content: entry.content,
      occurredAt: toLocalInput(entry.occurredAt), visibility: entry.visibility || 'INTERNAL',
      followUpRequired: entry.followUpRequired, followUpDate: entry.followUpDate ? toLocalInput(entry.followUpDate) : '',
    });
    setFormError('');
    setFormOpen(true);
  };

  const handleSave = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError('');
    if (!form.content.trim()) { setFormError('Communication content is required.'); return; }
    if (!form.occurredAt) { setFormError('Occurred at is required.'); return; }
    if (form.followUpRequired && !form.followUpDate) { setFormError('Follow-up date is required.'); return; }
    if (form.followUpRequired && form.followUpDate < form.occurredAt) { setFormError('Follow-up date cannot be earlier than the communication date.'); return; }

    const payload: ClientCommunicationRequest = {
      communicationType: form.communicationType,
      subject: form.subject.trim() || undefined,
      content: form.content.trim(),
      occurredAt: fromLocalInput(form.occurredAt),
      visibility: form.visibility,
      followUpRequired: form.followUpRequired,
      followUpDate: form.followUpRequired ? fromLocalInput(form.followUpDate) : undefined,
    };

    try {
      setSaving(true);
      if (editing) await clientApi.updateCommunication(clientId, editing.id, payload);
      else await clientApi.createCommunication(clientId, payload);
      setFormOpen(false);
      setFeedback(editing ? 'Communication updated.' : 'Communication recorded.');
      setPage(0);
      if (page === 0) await loadEntries();
    } catch (saveError: any) {
      setFormError(saveError?.response?.data?.message || 'Failed to save communication.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (entry: ClientCommunication) => {
    if (!window.confirm(`Delete this ${entry.communicationType.toLowerCase().replace('_', ' ')} entry?`)) return;
    try {
      await clientApi.deleteCommunication(clientId, entry.id);
      setFeedback('Communication deleted.');
      await loadEntries();
    } catch (deleteError: any) {
      setError(deleteError?.response?.data?.message || 'Failed to delete communication.');
    }
  };

  const iconFor = (type: ClientCommunicationType) => {
    if (type === 'EMAIL') return Mail;
    if (type === 'PHONE_CALL' || type === 'CALL') return Phone;
    if (type === 'MEETING') return Users;
    if (type === 'WHATSAPP' || type === 'CLIENT_PORTAL') return MessageCircle;
    if (type === 'DOCUMENT_REQUEST') return FileText;
    return CalendarClock;
  };

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-slate-900">Communication Timeline</h2>
          <p className="mt-1 text-xs text-slate-500">Client interactions, follow-ups, and related practice activity.</p>
        </div>
        {canCreate && <button type="button" onClick={openCreate} className="inline-flex items-center gap-2 rounded-lg bg-brand-600 px-3 py-2 text-xs font-semibold text-white hover:bg-brand-700"><Plus className="h-4 w-4" />Add Communication</button>}
      </div>

      {feedback && <div role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-800">{feedback}</div>}
      {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">{error}</div>}

      <div className="flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-3">
        <select aria-label="Filter communication type" value={typeFilter} onChange={(event) => { setTypeFilter(event.target.value); setPage(0); }} className="rounded-md border border-slate-200 bg-white px-2.5 py-2 text-xs">
          <option value="">All types</option>{COMMUNICATION_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
        </select>
        <label className="flex items-center gap-1.5 text-xs text-slate-500">From<input aria-label="Date from" type="date" value={fromDate} onChange={(event) => { setFromDate(event.target.value); setPage(0); }} className="rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700" /></label>
        <label className="flex items-center gap-1.5 text-xs text-slate-500">To<input aria-label="Date to" type="date" value={toDate} onChange={(event) => { setToDate(event.target.value); setPage(0); }} className="rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700" /></label>
        <select aria-label="Filter follow-up" value={followUpFilter} onChange={(event) => { setFollowUpFilter(event.target.value); setPage(0); }} className="rounded-md border border-slate-200 bg-white px-2.5 py-2 text-xs">
          <option value="">All follow-ups</option><option value="true">Follow-up required</option><option value="false">No follow-up</option>
        </select>
      </div>

      {loading ? (
        <div role="status" className="rounded-xl border border-slate-200 bg-white py-12 text-center text-sm text-slate-500">Loading communication timeline…</div>
      ) : entries.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white py-12 text-center">
          <MessageCircle className="mx-auto mb-2 h-8 w-8 text-slate-300" />
          <p className="text-sm font-semibold text-slate-600">No communications yet</p>
          <p className="mt-1 text-xs text-slate-400">Add the first client interaction to start the timeline.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {entries.map((entry) => {
            const Icon = iconFor(entry.communicationType);
            const manual = entry.communicationType !== 'SYSTEM_EVENT';
            return (
              <article key={entry.id} className="relative rounded-xl border border-slate-200 bg-white p-4 shadow-2xs">
                <div className="flex items-start gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700"><Icon className="h-4 w-4" /></span>
                  <div className="min-w-0 flex-1 space-y-1.5">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-brand-50 px-2 py-0.5 text-[10px] font-bold text-brand-700">{entry.communicationType.replace(/_/g, ' ')}</span>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${entry.visibility === 'CLIENT_VISIBLE' ? 'bg-sky-50 text-sky-700' : 'bg-slate-100 text-slate-600'}`}>{entry.visibility === 'CLIENT_VISIBLE' ? 'Client visible' : 'Internal'}</span>
                      <time className="ml-auto text-[11px] text-slate-400">{new Date(entry.occurredAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</time>
                    </div>
                    {entry.subject && <h3 className="text-sm font-bold text-slate-900">{entry.subject}</h3>}
                    <p className="whitespace-pre-wrap text-xs leading-relaxed text-slate-600">{entry.content}</p>
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1 text-[10px] text-slate-400">
                      <span>{entry.createdByName || 'Practice user'}</span>
                      {entry.followUpRequired && <span className="inline-flex items-center gap-1 font-semibold text-amber-700"><CalendarClock className="h-3 w-3" />Follow-up: {entry.followUpDate ? new Date(entry.followUpDate).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : 'Required'}</span>}
                    </div>
                  </div>
                  {manual && (canUpdate || canDelete) && <div className="flex shrink-0 gap-1">
                    {canUpdate && <button aria-label="Edit communication" onClick={() => openEdit(entry)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-brand-700"><Edit2 className="h-4 w-4" /></button>}
                    {canDelete && <button aria-label="Delete communication" onClick={() => void handleDelete(entry)} className="rounded p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-700"><Trash2 className="h-4 w-4" /></button>}
                  </div>}
                </div>
              </article>
            );
          })}
          <div className="flex items-center justify-between border-t border-slate-200 pt-3 text-xs text-slate-500">
            <span>{totalElements} total entries</span>
            <div className="flex items-center gap-2">
              <button type="button" disabled={page <= 0} onClick={() => setPage((current) => current - 1)} className="rounded border border-slate-200 px-3 py-1.5 disabled:opacity-40">Previous</button>
              <span>Page {page + 1} of {Math.max(totalPages, 1)}</span>
              <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} className="rounded border border-slate-200 px-3 py-1.5 disabled:opacity-40">Next</button>
            </div>
          </div>
        </div>
      )}

      {formOpen && <div className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/40 p-4" role="presentation">
        <div role="dialog" aria-modal="true" aria-labelledby="communication-form-title" className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
          <div className="flex items-center justify-between border-b border-slate-200 p-5">
            <div><h3 id="communication-form-title" className="text-base font-bold text-slate-900">{editing ? 'Edit Communication' : 'Add Communication'}</h3><p className="mt-1 text-xs text-slate-500">Record an interaction in this client’s timeline.</p></div>
            <button type="button" onClick={() => setFormOpen(false)} aria-label="Close form" className="rounded-lg px-2 py-1 text-slate-400 hover:bg-slate-100">×</button>
          </div>
          <form onSubmit={handleSave} className="space-y-4 p-5">
            {formError && <p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{formError}</p>}
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-1 text-xs font-semibold text-slate-700">Communication Type
                <select value={form.communicationType} onChange={(event) => setForm({ ...form, communicationType: event.target.value as ClientCommunicationType })} className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-normal">
                  {COMMUNICATION_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
                </select>
              </label>
              <label className="space-y-1 text-xs font-semibold text-slate-700">Occurred At *
                <input required type="datetime-local" value={form.occurredAt} onChange={(event) => setForm({ ...form, occurredAt: event.target.value })} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs font-normal" />
              </label>
            </div>
            <label className="block space-y-1 text-xs font-semibold text-slate-700">Subject <span className="font-normal text-slate-400">(Optional)</span>
              <input maxLength={255} value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs font-normal" />
            </label>
            <label className="block space-y-1 text-xs font-semibold text-slate-700">Content *
              <textarea required rows={5} value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs font-normal" />
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-1 text-xs font-semibold text-slate-700">Visibility
                <select value={form.visibility} onChange={(event) => setForm({ ...form, visibility: event.target.value as ClientCommunicationVisibility })} className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-normal">
                  <option value="INTERNAL">Internal</option><option value="CLIENT_VISIBLE">Client visible</option>
                </select>
              </label>
              <div className="space-y-2">
                <label className="flex items-center gap-2 pt-1 text-xs font-semibold text-slate-700"><input type="checkbox" checked={form.followUpRequired} onChange={(event) => setForm({ ...form, followUpRequired: event.target.checked })} />Follow-up required</label>
                {form.followUpRequired && <label className="block space-y-1 text-xs font-semibold text-slate-700">Follow-up Date *<input required type="datetime-local" value={form.followUpDate} onChange={(event) => setForm({ ...form, followUpDate: event.target.value })} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs font-normal" /></label>}
              </div>
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
              <button type="button" disabled={saving} onClick={() => setFormOpen(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 disabled:opacity-50">Cancel</button>
              <button type="submit" disabled={saving} className="rounded-lg bg-brand-600 px-4 py-2 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-50">{saving ? 'Saving…' : editing ? 'Save Changes' : 'Save Communication'}</button>
            </div>
          </form>
        </div>
      </div>}
    </section>
  );
};
