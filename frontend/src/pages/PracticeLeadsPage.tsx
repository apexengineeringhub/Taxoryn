import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, CalendarClock, ChevronLeft, ChevronRight, CirclePlus, Edit2, ExternalLink, MessageSquareText, RefreshCw, Search, UserRound, X } from 'lucide-react';
import { practiceLeadApi, employeeApi, servicesApi } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';
import { Employee, PracticeLead, PracticeLeadActivity, PracticeLeadPriority, PracticeLeadRequest, PracticeLeadSource, PracticeLeadStatus } from '../types';

const STATUSES: PracticeLeadStatus[] = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL_SENT', 'FOLLOW_UP', 'CONVERTED', 'LOST'];
const PRIORITIES: PracticeLeadPriority[] = ['CRITICAL', 'URGENT', 'HIGH', 'MEDIUM', 'LOW'];
const SOURCES: PracticeLeadSource[] = ['WEBSITE', 'PHONE', 'EMAIL', 'WALK_IN', 'REFERRAL', 'MARKETPLACE', 'SOCIAL_MEDIA', 'CAMPAIGN', 'OTHER'];
const label = (value?: string) => value?.replace(/_/g, ' ') || '—';
const followUpState = (value?: string) => {
  if (!value) return '';
  const target = new Date(value).toDateString();
  const today = new Date().toDateString();
  if (new Date(value).getTime() < new Date(new Date().toDateString()).getTime()) return 'Overdue';
  return target === today ? 'Today' : 'Upcoming';
};
const toInput = (value?: string) => {
  if (!value) return '';
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
const fromInput = (value: string) => value ? new Date(value).toISOString() : undefined;
const badgeTone: Record<string, string> = { NEW: 'bg-blue-50 text-blue-700', CONTACTED: 'bg-indigo-50 text-indigo-700', QUALIFIED: 'bg-emerald-50 text-emerald-700', PROPOSAL_SENT: 'bg-violet-50 text-violet-700', FOLLOW_UP: 'bg-amber-50 text-amber-700', CONVERTED: 'bg-green-100 text-green-800', LOST: 'bg-slate-100 text-slate-600', CRITICAL: 'bg-rose-100 text-rose-800', URGENT: 'bg-orange-100 text-orange-800', HIGH: 'bg-amber-50 text-amber-800', MEDIUM: 'bg-sky-50 text-sky-700', LOW: 'bg-slate-100 text-slate-600' };

const blankLead = (): PracticeLeadRequest => ({ leadType: 'INDIVIDUAL', name: '', businessName: '', email: '', phone: '', source: 'OTHER', status: 'NEW', priority: 'MEDIUM', interestedServiceCode: '', description: '', assignedEmployeeId: '', nextFollowUpAt: '' });

export const PracticeLeadsPage: React.FC = () => {
  const { leadId } = useParams();
  return leadId ? <LeadDetail leadId={leadId} /> : <LeadList />;
};

const LeadList: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [rows, setRows] = useState<PracticeLead[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [services, setServices] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchText, setSearchText] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [source, setSource] = useState('');
  const [serviceCode, setServiceCode] = useState('');
  const [assigned, setAssigned] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState<PracticeLead | null>(null);
  const [form, setForm] = useState<PracticeLeadRequest>(blankLead());

  const permissions = user?.permissions || [];
  const roleCodes = (user?.roles || []).map((r: any) => typeof r === 'string' ? r : r.code);
  const canCreate = permissions.includes('LEAD_CREATE') || roleCodes.some((r: string) => ['ORG_ADMIN','PRACTICE_ADMIN','PRACTICE_OWNER','PARTNER','MANAGER','PRACTITIONER','TAX_PROFESSIONAL','STAFF','ACCOUNTANT','ARTICLE_ASSISTANT'].includes(r));
  const canUpdate = permissions.includes('LEAD_UPDATE') || canCreate;
  const canConvert = permissions.includes('LEAD_CONVERT') || ['ORG_ADMIN','PRACTICE_ADMIN','PRACTICE_OWNER','PARTNER','MANAGER','PRACTITIONER','TAX_PROFESSIONAL'].some(r => roleCodes.includes(r));

  useEffect(() => { employeeApi.getAll({ page: 0, size: 100 }).then((res) => setEmployees(res?.content || [])).catch(() => {}); servicesApi.getAll().then(setServices).catch(() => {}); }, []);
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const result = await practiceLeadApi.list({ page, size: 20, search: search || undefined, status: status || undefined, priority: priority || undefined, source: source || undefined, interestedServiceCode: serviceCode || undefined, assignedEmployeeId: assigned || undefined });
      setRows(result?.content || []); setTotalPages(result?.totalPages || 0); setTotalElements(result?.totalElements || 0);
    } catch (e: any) { setError(e?.response?.data?.message || 'Unable to load leads. Please try again.'); }
    finally { setLoading(false); }
  }, [page, search, status, priority, source, serviceCode, assigned]);
  useEffect(() => { void load(); }, [load]);

  const openNew = () => { setEditing(null); setForm(blankLead()); setFormOpen(true); };
  const openEdit = (lead: PracticeLead) => {
    setEditing(lead); setForm({ leadType: lead.leadType, name: lead.name, businessName: lead.businessName || '', email: lead.email || '', phone: lead.phone || '', source: lead.source, status: lead.status, priority: lead.priority, interestedServiceCode: lead.interestedServiceCode || '', description: lead.description || '', assignedEmployeeId: lead.assignedEmployeeId || '', nextFollowUpAt: toInput(lead.nextFollowUpAt), lostReason: lead.lostReason || '' }); setFormOpen(true);
  };
  const save = async (event: React.FormEvent) => {
    event.preventDefault(); setSaving(true); setError('');
    try {
      const payload = { ...form, businessName: form.businessName || undefined, email: form.email || undefined, phone: form.phone || undefined, description: form.description || undefined, assignedEmployeeId: form.assignedEmployeeId || undefined, interestedServiceCode: form.interestedServiceCode || undefined, nextFollowUpAt: fromInput(form.nextFollowUpAt || '') };
      if (editing) await practiceLeadApi.update(editing.id, payload); else await practiceLeadApi.create(payload);
      setFormOpen(false); setPage(0); if (page === 0) await load();
    } catch (e: any) { setError(e?.response?.data?.message || 'Could not save lead.'); }
    finally { setSaving(false); }
  };
  const assign = async (lead: PracticeLead, employeeId: string) => {
    if (!employeeId) return;
    try { await practiceLeadApi.assign(lead.id, employeeId); await load(); }
    catch (e: any) { setError(e?.response?.data?.message || 'Could not assign lead.'); }
  };
  const markLost = async (lead: PracticeLead) => {
    const reason = window.prompt('Why was this lead lost? (optional)');
    if (reason === null) return;
    try { await practiceLeadApi.markLost(lead.id, reason || undefined); await load(); }
    catch (e: any) { setError(e?.response?.data?.message || 'Could not mark lead lost.'); }
  };
  const submitSearch = (event: React.FormEvent) => { event.preventDefault(); setPage(0); setSearch(searchText.trim()); };

  return <div className="mx-auto max-w-7xl space-y-5 p-6">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div><h1 className="text-2xl font-bold text-slate-900">Leads / Enquiries</h1><p className="mt-1 text-sm text-slate-500">Capture prospective clients and track their journey to onboarding.</p></div>
      {canCreate && <button onClick={openNew} className="inline-flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white"><CirclePlus className="h-4 w-4" />Add Lead</button>}
    </div>
    {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>}
    <div className="flex flex-wrap gap-2 rounded-xl border border-slate-200 bg-white p-3">
      <form onSubmit={submitSearch} className="flex min-w-56 flex-1 gap-2"><div className="relative flex-1"><Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" /><input value={searchText} onChange={e => setSearchText(e.target.value)} placeholder="Search name, business, email, phone" className="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-3 text-sm" /></div><button className="rounded-lg border border-slate-200 px-3 text-sm">Search</button></form>
      <select aria-label="Status filter" value={status} onChange={e => { setStatus(e.target.value); setPage(0); }} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All statuses</option>{STATUSES.map(s => <option key={s} value={s}>{label(s)}</option>)}</select>
      <select aria-label="Priority filter" value={priority} onChange={e => { setPriority(e.target.value); setPage(0); }} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All priorities</option>{PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}</select>
      <select aria-label="Source filter" value={source} onChange={e => { setSource(e.target.value); setPage(0); }} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All sources</option>{SOURCES.map(s => <option key={s} value={s}>{label(s)}</option>)}</select>
      <select aria-label="Service filter" value={serviceCode} onChange={e => { setServiceCode(e.target.value); setPage(0); }} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All services</option>{services.map((s: any) => <option key={s.serviceCode || s.id} value={s.serviceCode}>{s.serviceName || s.displayName}</option>)}</select>
      <select aria-label="Assigned employee filter" value={assigned} onChange={e => { setAssigned(e.target.value); setPage(0); }} className="rounded-lg border border-slate-200 px-3 py-2 text-sm"><option value="">All assignees</option>{employees.map(emp => <option key={emp.id} value={emp.id}>{emp.fullName || `${emp.firstName} ${emp.lastName || ''}`}</option>)}</select>
    </div>
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      {loading ? (
        <div role="status" className="p-12 text-center text-sm text-slate-500">Loading leads…</div>
      ) : error ? (
        <div role="alert" className="p-14 text-center">
          <AlertCircle className="mx-auto mb-3 h-10 w-10 text-rose-500" />
          <p className="text-base font-semibold text-slate-900">Unable to load leads</p>
          <p className="mt-1 text-sm text-slate-500">We couldn't retrieve your leads right now.</p>
          {error !== 'Unable to load leads. Please try again.' && (
            <p className="mt-1 text-xs text-rose-600">{error}</p>
          )}
          <button
            type="button"
            onClick={() => void load()}
            className="mt-4 inline-flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700"
          >
            <RefreshCw className="h-4 w-4" />
            Try Again
          </button>
        </div>
      ) : rows.length === 0 ? (
        <div className="p-14 text-center">
          <UserRound className="mx-auto mb-3 h-9 w-9 text-slate-300" />
          <p className="font-semibold text-slate-700">No leads yet</p>
          <p className="mt-1 text-sm text-slate-500">Capture your first enquiry and track it through conversion.</p>
          {canCreate && (
            <button onClick={openNew} className="mt-4 rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white">
              + Add Lead
            </button>
          )}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>{['Lead','Contact','Interested Service','Source','Status','Priority','Assigned To','Next Follow-up','Actions'].map(x => <th key={x} className="px-4 py-3">{x}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map(lead => (
                <tr key={lead.id} className="hover:bg-slate-50/70">
                  <td className="px-4 py-3">
                    <Link to={`/leads/${lead.id}`} className="font-semibold text-slate-900 hover:text-brand-700">{lead.name}</Link>
                    {lead.businessName && <div className="mt-0.5 text-xs text-slate-500">{lead.businessName}</div>}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-600">
                    {lead.phone || '—'}
                    <div>{lead.email || ''}</div>
                  </td>
                  <td className="px-4 py-3">{lead.interestedServiceName || lead.interestedServiceCode || '—'}</td>
                  <td className="px-4 py-3">{label(lead.source)}</td>
                  <td className="px-4 py-3"><span className={`rounded-full px-2 py-1 text-[10px] font-bold ${badgeTone[lead.status]}`}>{label(lead.status)}</span></td>
                  <td className="px-4 py-3"><span className={`rounded-full px-2 py-1 text-[10px] font-bold ${badgeTone[lead.priority]}`}>{lead.priority}</span></td>
                  <td className="px-4 py-3">{lead.assignedEmployeeName || 'Unassigned'}</td>
                  <td className="px-4 py-3 text-xs">
                    {lead.nextFollowUpAt ? (
                      <span className={new Date(lead.nextFollowUpAt) < new Date() ? 'font-semibold text-rose-700' : 'text-slate-600'}>
                        {new Date(lead.nextFollowUpAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}
                        {new Date(lead.nextFollowUpAt) < new Date() && <span className="block">Overdue</span>}
                      </span>
                    ) : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2 whitespace-nowrap">
                      <button title="View" onClick={() => navigate(`/leads/${lead.id}`)} className="text-xs font-semibold text-brand-700">View</button>
                      {canUpdate && <button title="Edit" onClick={() => openEdit(lead)} className="text-slate-500 hover:text-brand-700"><Edit2 className="h-4 w-4" /></button>}
                      <details className="relative">
                        <summary className="cursor-pointer list-none rounded px-2 text-lg leading-none text-slate-500">⋮</summary>
                        <div className="absolute right-0 z-10 mt-1 w-48 rounded-lg border border-slate-200 bg-white p-1 shadow-lg">
                          {permissions.includes('LEAD_ASSIGN') && (
                            <label className="block px-2 py-1 text-[10px] text-slate-400">
                              Assign to
                              <select defaultValue="" onChange={e => void assign(lead, e.target.value)} className="mt-1 w-full rounded border p-1.5 text-xs">
                                <option value="">Choose employee…</option>
                                {employees.map(emp => <option key={emp.id} value={emp.id}>{emp.fullName || emp.firstName}</option>)}
                              </select>
                            </label>
                          )}
                          <button onClick={() => navigate(`/leads/${lead.id}#activity`)} className="block w-full rounded px-3 py-2 text-left text-xs hover:bg-slate-50">Add Communication</button>
                          {canConvert && ['QUALIFIED','PROPOSAL_SENT','FOLLOW_UP'].includes(lead.status) && (
                            <button onClick={() => navigate(`/leads/${lead.id}#convert`)} className="block w-full rounded px-3 py-2 text-left text-xs hover:bg-slate-50">Convert to Client</button>
                          )}
                          {canUpdate && !['LOST','CONVERTED'].includes(lead.status) && (
                            <button onClick={() => void markLost(lead)} className="block w-full rounded px-3 py-2 text-left text-xs hover:bg-slate-50">Mark Lost</button>
                          )}
                        </div>
                      </details>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {!loading && !error && rows.length > 0 && (
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-xs text-slate-500">
          <span>{totalElements} leads</span>
          <div className="flex items-center gap-2">
            <button disabled={page <= 0} onClick={() => setPage(p => p - 1)} className="rounded border px-2 py-1 disabled:opacity-40"><ChevronLeft className="h-4 w-4" /></button>
            <span>Page {page + 1} of {Math.max(1, totalPages)}</span>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)} className="rounded border px-2 py-1 disabled:opacity-40"><ChevronRight className="h-4 w-4" /></button>
          </div>
        </div>
      )}
    </div>
    {formOpen && <LeadFormModal form={form} setForm={setForm} employees={employees} services={services} saving={saving} editing={!!editing} canAssign={permissions.includes('LEAD_ASSIGN')} onClose={() => setFormOpen(false)} onSubmit={save} />}
  </div>;
};

const LeadFormModal: React.FC<{ form: PracticeLeadRequest; setForm: React.Dispatch<React.SetStateAction<PracticeLeadRequest>>; employees: Employee[]; services: any[]; saving: boolean; editing: boolean; canAssign: boolean; onClose: () => void; onSubmit: (e: React.FormEvent) => void }> = ({ form, setForm, employees, services, saving, editing, canAssign, onClose, onSubmit }) => {
  const set = (key: keyof PracticeLeadRequest, value: any) => setForm(current => ({ ...current, [key]: value }));
  return <div className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 p-4"><form onSubmit={onSubmit} className="max-h-[92vh] w-full max-w-2xl space-y-4 overflow-y-auto rounded-2xl bg-white p-6 shadow-xl"><div className="flex items-center justify-between"><h2 className="text-lg font-bold">{editing ? 'Edit Lead' : 'Add Lead'}</h2><button type="button" onClick={onClose}><X className="h-5 w-5" /></button></div><div className="grid gap-3 sm:grid-cols-2">
    <label className="text-xs font-semibold">Lead Type<select value={form.leadType} onChange={e => set('leadType', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm"><option value="INDIVIDUAL">Individual</option><option value="BUSINESS">Business</option></select></label>
    <label className="text-xs font-semibold">Name *<input required maxLength={255} value={form.name} onChange={e => set('name', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
    <label className="text-xs font-semibold">Business Name<input value={form.businessName} onChange={e => set('businessName', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
    <label className="text-xs font-semibold">Phone<input value={form.phone} onChange={e => set('phone', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
    <label className="text-xs font-semibold">Email<input type="email" value={form.email} onChange={e => set('email', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
    <label className="text-xs font-semibold">Source<select value={form.source} onChange={e => set('source', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm">{SOURCES.map(s => <option key={s} value={s}>{label(s)}</option>)}</select></label>
    <label className="text-xs font-semibold">Interested Service<select value={form.interestedServiceCode} onChange={e => set('interestedServiceCode', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm"><option value="">Not specified</option>{services.map((s: any) => <option key={s.serviceCode || s.id} value={s.serviceCode}>{s.serviceName || s.displayName}</option>)}</select></label>
    <label className="text-xs font-semibold">Priority<select value={form.priority} onChange={e => set('priority', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm">{PRIORITIES.map(p => <option key={p}>{p}</option>)}</select></label>
    <label className="text-xs font-semibold">Assigned Employee<select disabled={!canAssign} value={form.assignedEmployeeId} onChange={e => set('assignedEmployeeId', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm disabled:bg-slate-100"><option value="">Auto assign to me / unassigned</option>{employees.map(emp => <option key={emp.id} value={emp.id}>{emp.fullName || emp.firstName}</option>)}</select></label>
    <label className="text-xs font-semibold">Next Follow-up<input type="datetime-local" value={form.nextFollowUpAt} onChange={e => set('nextFollowUpAt', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
    {editing && <label className="text-xs font-semibold">Status<select value={form.status} onChange={e => set('status', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm">{STATUSES.filter(s => s !== 'CONVERTED' && s !== 'LOST').map(s => <option key={s} value={s}>{label(s)}</option>)}</select></label>}
    <label className="text-xs font-semibold sm:col-span-2">Description<textarea rows={3} value={form.description} onChange={e => set('description', e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label>
  </div><div className="flex justify-end gap-2 border-t pt-4"><button type="button" onClick={onClose} className="rounded-lg border px-4 py-2 text-sm">Cancel</button><button disabled={saving} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white">{saving ? 'Saving…' : 'Save Lead'}</button></div></form></div>;
};

const LeadDetail: React.FC<{ leadId: string }> = ({ leadId }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [lead, setLead] = useState<PracticeLead | null>(null);
  const [activities, setActivities] = useState<PracticeLeadActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activityType, setActivityType] = useState('PHONE_CALL');
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [saving, setSaving] = useState(false);
  const [convertOpen, setConvertOpen] = useState(false);
  const [clientType, setClientType] = useState('INDIVIDUAL');
  const [displayName, setDisplayName] = useState('');
  const [legalName, setLegalName] = useState('');
  const [assignedEmployeeId, setAssignedEmployeeId] = useState('');
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState<PracticeLeadRequest>(blankLead());
  const roleCodes = (user?.roles || []).map((r: any) => typeof r === 'string' ? r : r.code);
  const canUpdate = user?.permissions?.includes('LEAD_UPDATE') || ['ORG_ADMIN','PRACTICE_ADMIN','PRACTICE_OWNER','PARTNER','MANAGER','TAX_MANAGER','PRACTITIONER','TAX_PROFESSIONAL','STAFF','ACCOUNTANT','ARTICLE_ASSISTANT'].some(r => roleCodes.includes(r));
  const canConvert = user?.permissions?.includes('LEAD_CONVERT') || ['ORG_ADMIN','PRACTICE_ADMIN','PRACTICE_OWNER','PARTNER','MANAGER','PRACTITIONER','TAX_PROFESSIONAL'].some(r => roleCodes.includes(r));
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { const [result, history] = await Promise.all([practiceLeadApi.get(leadId), practiceLeadApi.activities(leadId)]); setLead(result); setActivities(history || []); setDisplayName(result.name); setLegalName(result.businessName || ''); setClientType(result.leadType === 'BUSINESS' ? 'OTHER' : 'INDIVIDUAL'); }
    catch (e: any) { setError(e?.response?.data?.message || 'Could not load lead.'); }
    finally { setLoading(false); }
  }, [leadId]);
  useEffect(() => { void load(); employeeApi.getAll({ page: 0, size: 100 }).then(r => setEmployees(r?.content || [])).catch(() => {}); }, [load]);
  useEffect(() => {
    if (location.hash === '#convert' && lead && ['QUALIFIED','PROPOSAL_SENT','FOLLOW_UP'].includes(lead.status)) setConvertOpen(true);
    if (location.hash === '#followup' && lead && canUpdate && !['CONVERTED','LOST'].includes(lead.status)) {
      setEditForm({ leadType: lead.leadType, name: lead.name, businessName: lead.businessName || '', email: lead.email || '', phone: lead.phone || '', source: lead.source, status: lead.status, priority: lead.priority, interestedServiceCode: lead.interestedServiceCode || '', description: lead.description || '', assignedEmployeeId: lead.assignedEmployeeId || '', nextFollowUpAt: toInput(lead.nextFollowUpAt), lostReason: lead.lostReason || '' });
      setEditOpen(true);
    }
  }, [location.hash, lead, canUpdate]);
  const addActivity = async (event: React.FormEvent) => { event.preventDefault(); setSaving(true); try { await practiceLeadApi.addActivity(leadId, { activityType, subject: subject || undefined, content, occurredAt: new Date().toISOString() }); setContent(''); setSubject(''); await load(); } catch (e: any) { setError(e?.response?.data?.message || 'Could not save activity.'); } finally { setSaving(false); } };
  const openEdit = () => {
    if (!lead) return;
    setEditForm({ leadType: lead.leadType, name: lead.name, businessName: lead.businessName || '', email: lead.email || '', phone: lead.phone || '', source: lead.source, status: lead.status, priority: lead.priority, interestedServiceCode: lead.interestedServiceCode || '', description: lead.description || '', assignedEmployeeId: lead.assignedEmployeeId || '', nextFollowUpAt: toInput(lead.nextFollowUpAt), lostReason: lead.lostReason || '' });
    setEditOpen(true);
  };
  const saveEdit = async (event: React.FormEvent) => {
    event.preventDefault(); setSaving(true);
    try {
      await practiceLeadApi.update(leadId, { ...editForm, businessName: editForm.businessName || undefined, email: editForm.email || undefined, phone: editForm.phone || undefined, description: editForm.description || undefined, assignedEmployeeId: editForm.assignedEmployeeId || undefined, interestedServiceCode: editForm.interestedServiceCode || undefined, nextFollowUpAt: fromInput(editForm.nextFollowUpAt || '') });
      setEditOpen(false); await load();
    } catch (e: any) { setError(e?.response?.data?.message || 'Could not update lead.'); }
    finally { setSaving(false); }
  };
  const convert = async (event: React.FormEvent) => { event.preventDefault(); if (!lead) return; setSaving(true); try { const result = await practiceLeadApi.convert(lead.id, { clientType, displayName, legalName: legalName || undefined, email: lead.email, phone: lead.phone, assignedEmployeeId: assignedEmployeeId || undefined }); if (result.convertedClientId) navigate(`/clients/${result.convertedClientId}`); } catch (e: any) { setError(e?.response?.data?.message || 'Could not convert lead.'); } finally { setSaving(false); } };
  if (loading) return <div role="status" className="p-12 text-center text-sm text-slate-500">Loading lead…</div>;
  if (!lead) return <div className="mx-auto max-w-3xl p-6"><div role="alert" className="rounded-lg bg-rose-50 p-4 text-rose-700">{error || 'Lead not found.'}</div><button onClick={() => navigate('/leads')} className="mt-4 text-sm text-brand-700">Back to Leads</button></div>;
  const overdue = !!lead.nextFollowUpAt && new Date(lead.nextFollowUpAt) < new Date();
  return <div className="mx-auto max-w-5xl space-y-5 p-6">
    <Link to="/leads" className="inline-flex items-center gap-2 text-sm text-slate-500 hover:text-brand-700"><ArrowLeft className="h-4 w-4" />Back to Leads</Link>
    {error && <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}
    <section className="rounded-2xl border border-slate-200 bg-white p-6"><div className="flex flex-wrap items-start justify-between gap-4"><div><div className="flex flex-wrap items-center gap-2"><h1 className="text-2xl font-bold text-slate-900">{lead.name}</h1><span className={`rounded-full px-2 py-1 text-xs font-bold ${badgeTone[lead.status]}`}>{label(lead.status)}</span><span className={`rounded-full px-2 py-1 text-xs font-bold ${badgeTone[lead.priority]}`}>{lead.priority}</span></div>{lead.businessName && <p className="mt-1 text-sm text-slate-500">{lead.businessName}</p>}</div><div className="flex gap-2">{canUpdate && !['CONVERTED','LOST'].includes(lead.status) && <button onClick={openEdit} className="inline-flex items-center gap-1 rounded-lg border px-3 py-2 text-sm"><Edit2 className="h-4 w-4" />Edit</button>}{canConvert && lead.status !== 'CONVERTED' && lead.status !== 'LOST' && <button onClick={() => setConvertOpen(true)} disabled={!['QUALIFIED','PROPOSAL_SENT','FOLLOW_UP'].includes(lead.status)} className="rounded-lg bg-brand-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50">Convert to Client</button>}</div></div>
      <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{[['Email',lead.email],['Phone',lead.phone],['Source',label(lead.source)],['Interested Service',lead.interestedServiceName || lead.interestedServiceCode],['Assigned To',lead.assignedEmployeeName],['Next Follow-up',lead.nextFollowUpAt ? new Date(lead.nextFollowUpAt).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'}) : '—'],['Created',lead.createdAt ? new Date(lead.createdAt).toLocaleDateString('en-IN') : '—']].map(([key,value]) => <div key={key} className="rounded-lg bg-slate-50 p-3"><p className="text-[10px] font-bold uppercase text-slate-400">{key}</p><p className={`mt-1 text-sm ${key === 'Next Follow-up' && overdue ? 'font-semibold text-rose-700' : 'text-slate-700'}`}>{value || '—'}{key === 'Next Follow-up' && overdue ? ' · Overdue' : ''}</p></div>)}</div>{lead.description && <p className="mt-4 whitespace-pre-wrap border-t pt-4 text-sm text-slate-600">{lead.description}</p>}{lead.convertedClientId && <Link to={`/clients/${lead.convertedClientId}`} className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-brand-700">View converted client <ExternalLink className="h-4 w-4" /></Link>}{lead.status === 'LOST' && lead.lostReason && <p className="mt-4 text-sm text-slate-500">Lost reason: {lead.lostReason}</p>}</section>
    <div className="flex items-center gap-3">{lead.nextFollowUpAt && <span className={`text-sm font-semibold ${overdue ? 'text-rose-700' : 'text-amber-700'}`}>Follow-up {followUpState(lead.nextFollowUpAt)}</span>}{canUpdate && !['CONVERTED','LOST'].includes(lead.status) && <button onClick={openEdit} className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800"><CalendarClock className="mr-2 inline h-4 w-4" />Create Follow-up / Update Date</button>}</div>
    <section id="activity" className="grid gap-5 lg:grid-cols-[1fr_360px]"><div className="rounded-2xl border border-slate-200 bg-white p-5"><h2 className="mb-4 text-base font-bold">Lead Activity</h2>{activities.length === 0 ? <p className="rounded-lg border border-dashed p-8 text-center text-sm text-slate-400">No activity yet.</p> : <div className="space-y-3">{activities.map(a => <article key={a.id} className="rounded-lg border border-slate-100 p-4"><div className="flex items-center gap-2"><span className="rounded-full bg-brand-50 px-2 py-1 text-[10px] font-bold text-brand-700">{label(a.activityType)}</span><time className="ml-auto text-[10px] text-slate-400">{new Date(a.occurredAt).toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'})}</time></div>{a.subject && <h3 className="mt-2 text-sm font-semibold">{a.subject}</h3>}<p className="mt-1 whitespace-pre-wrap text-sm text-slate-600">{a.content}</p><p className="mt-2 text-[10px] text-slate-400">{a.authorName || 'Practice user'}</p></article>)}</div>}</div>
    <form onSubmit={addActivity} className="h-fit space-y-3 rounded-2xl border border-slate-200 bg-white p-5"><h2 className="text-base font-bold"><MessageSquareText className="mr-2 inline h-4 w-4" />Add Communication</h2><select value={activityType} onChange={e => setActivityType(e.target.value)} className="w-full rounded-lg border p-2 text-sm"><option value="PHONE_CALL">Phone Call</option><option value="NOTE">Note</option><option value="EMAIL">Email</option><option value="MEETING">Meeting</option><option value="WHATSAPP">WhatsApp</option></select><input value={subject} onChange={e => setSubject(e.target.value)} placeholder="Subject (optional)" className="w-full rounded-lg border p-2 text-sm" /><textarea required rows={4} value={content} onChange={e => setContent(e.target.value)} placeholder="What happened?" className="w-full rounded-lg border p-2 text-sm" /><button disabled={saving} className="w-full rounded-lg bg-brand-600 px-3 py-2 text-sm font-semibold text-white">{saving ? 'Saving…' : 'Add to Timeline'}</button></form></section>
    {convertOpen && <div className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 p-4"><form onSubmit={convert} className="w-full max-w-lg space-y-4 rounded-2xl bg-white p-6"><div className="flex justify-between"><h2 className="text-lg font-bold">Review Client Details</h2><button type="button" onClick={() => setConvertOpen(false)}><X className="h-5 w-5" /></button></div><p className="text-sm text-slate-500">Confirm the details before creating a client. Assignment is optional and determined separately from the lead.</p><label className="block text-xs font-semibold">Client Type<select value={clientType} onChange={e => setClientType(e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm">{['INDIVIDUAL','PROPRIETOR','PARTNERSHIP','LLP','PRIVATE_LIMITED','PUBLIC_LIMITED','OTHER'].map(x => <option key={x}>{x}</option>)}</select></label><label className="block text-xs font-semibold">Display Name<input required value={displayName} onChange={e => setDisplayName(e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label><label className="block text-xs font-semibold">Legal / Business Name<input value={legalName} onChange={e => setLegalName(e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm" /></label><label className="block text-xs font-semibold">Assigned Employee<select value={assignedEmployeeId} onChange={e => setAssignedEmployeeId(e.target.value)} className="mt-1 w-full rounded-lg border p-2 text-sm"><option value="">Leave unassigned</option>{employees.map(emp => <option key={emp.id} value={emp.id}>{emp.fullName || emp.firstName}</option>)}</select></label><div className="flex justify-end gap-2"><button type="button" onClick={() => setConvertOpen(false)} className="rounded-lg border px-4 py-2 text-sm">Cancel</button><button disabled={saving} className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white">{saving ? 'Converting…' : 'Create Client'}</button></div></form></div>}
    {editOpen && <LeadFormModal form={editForm} setForm={setEditForm} employees={employees} services={[]} saving={saving} editing canAssign={!!user?.permissions?.includes('LEAD_ASSIGN')} onClose={() => setEditOpen(false)} onSubmit={saveEdit} />}
  </div>;
};
