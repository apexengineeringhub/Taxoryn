import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Briefcase,
  ArrowLeft,
  Calendar,
  User,
  ShieldCheck,
  Building2,
  CheckCircle2,
  PauseCircle,
  PlayCircle,
  XCircle,
  Clock,
  Edit3,
  FileText,
  AlertCircle,
  Check,
  ExternalLink,
  Layers,
  ChevronRight,
  UserCheck,
  Receipt,
  FolderLock,
  History,
} from 'lucide-react';
import { engagementsApi, servicesApi, clientApi } from '../api/endpoints';
import {
  EngagementDto,
  EngagementStatusType,
  EngagementPriorityType,
  ServiceDto,
  UpdateEngagementPayload,
  UpdateEngagementAssignmentPayload,
} from '../types';
import { StatusBadge } from '../components/common/StatusBadge';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';

export const EngagementOverviewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [engagement, setEngagement] = useState<EngagementDto | null>(null);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'work_items' | 'documents' | 'billing' | 'activity'>('overview');

  // Status Action Modal State
  const [statusModalAction, setStatusModalAction] = useState<EngagementStatusType | null>(null);
  const [statusReason, setStatusReason] = useState<string>('');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState<boolean>(false);

  // Edit Mandate Modal State
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);
  const [editFormData, setEditFormData] = useState<UpdateEngagementPayload>({});
  const [isSavingEdit, setIsSavingEdit] = useState<boolean>(false);

  // Load Engagement
  const loadEngagement = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await engagementsApi.getById(id);
      setEngagement(data);
      setEditFormData({
        name: data.name,
        description: data.description || '',
        serviceId: data.serviceId || '',
        locationId: data.locationId || '',
        startDate: data.startDate || '',
        endDate: data.endDate || '',
        priority: data.priority,
        notes: data.notes || '',
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Engagement not found');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadEngagement();
    servicesApi.getAll().then(setServices).catch(() => {});
  }, [loadEngagement]);

  const handleStatusTransition = async (newStatus: EngagementStatusType) => {
    if (!id) return;
    try {
      setIsUpdatingStatus(true);
      const updated = await engagementsApi.updateStatus(id, {
        status: newStatus,
        notes: statusReason.trim() || undefined,
      });
      setEngagement(updated);
      setStatusModalAction(null);
      setStatusReason('');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update engagement status');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    try {
      setIsSavingEdit(true);
      const updated = await engagementsApi.update(id, {
        ...editFormData,
        serviceId: editFormData.serviceId || undefined,
        startDate: editFormData.startDate || undefined,
        endDate: editFormData.endDate || undefined,
        description: editFormData.description?.trim() || undefined,
        notes: editFormData.notes?.trim() || undefined,
      });
      setEngagement(updated);
      setIsEditModalOpen(false);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update engagement');
    } finally {
      setIsSavingEdit(false);
    }
  };

  const getPriorityBadgeClass = (priority?: EngagementPriorityType) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'HIGH':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'MEDIUM':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center space-y-4">
        <div className="w-10 h-10 border-4 border-brand-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-xs font-bold text-slate-500 tracking-wide uppercase">
          Loading Engagement Details...
        </p>
      </div>
    );
  }

  if (error || !engagement) {
    return (
      <div className="bg-rose-50 border border-rose-200 rounded-2xl p-8 text-center space-y-4 max-w-lg mx-auto mt-12">
        <AlertCircle className="w-10 h-10 text-rose-500 mx-auto" />
        <h2 className="text-base font-bold text-rose-900">Engagement Not Found</h2>
        <p className="text-xs text-rose-700">{error}</p>
        <Link
          to="/engagements"
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-600 text-white text-xs font-bold shadow-xs hover:bg-brand-700 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Engagements</span>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Back Link */}
      <div>
        <Link
          to="/engagements"
          className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-brand-600 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Engagements Roster</span>
        </Link>
      </div>

      {/* Hero Header Card */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-2xs space-y-5">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-mono text-xs font-bold text-brand-700 bg-brand-50 border border-brand-200 px-2.5 py-0.5 rounded-lg">
                {engagement.engagementCode}
              </span>
              <span
                className={clsx(
                  'text-[10px] font-bold px-2 py-0.5 rounded-md border uppercase tracking-wider',
                  getPriorityBadgeClass(engagement.priority)
                )}
              >
                {engagement.priority} PRIORITY
              </span>
              <StatusBadge status={engagement.status} />
              {engagement.serviceCode && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 border border-purple-200">
                  {engagement.serviceCode}
                </span>
              )}
            </div>

            <h1 className="text-2xl font-black text-slate-900 tracking-tight">{engagement.name}</h1>

            <div className="flex items-center gap-3 text-xs text-slate-500 flex-wrap">
              <span className="flex items-center gap-1.5">
                <Building2 className="w-4 h-4 text-slate-400" />
                <span className="text-slate-400">Client:</span>
                <Link
                  to={`/clients/${engagement.clientId}`}
                  className="font-bold text-brand-600 hover:underline"
                >
                  {engagement.clientName || 'Client Profile'}
                </Link>
              </span>
              <span>•</span>
              <span className="flex items-center gap-1.5">
                <Briefcase className="w-4 h-4 text-slate-400" />
                <span className="text-slate-400">Service:</span>
                <span className="font-semibold text-slate-800">
                  {engagement.serviceName || 'Custom Practice Service'}
                </span>
              </span>
              {engagement.locationName && (
                <>
                  <span>•</span>
                  <span>Branch: {engagement.locationName}</span>
                </>
              )}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={() => setIsEditModalOpen(true)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold shadow-2xs transition-colors"
            >
              <Edit3 className="w-3.5 h-3.5 text-slate-500" />
              <span>Edit Mandate</span>
            </button>

            {/* Lifecycle Controls */}
            {engagement.status === 'DRAFT' && (
              <button
                onClick={() => handleStatusTransition('ACTIVE')}
                disabled={isUpdatingStatus}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-colors"
              >
                <PlayCircle className="w-3.5 h-3.5" />
                <span>Activate Engagement</span>
              </button>
            )}

            {engagement.status === 'ACTIVE' && (
              <>
                <button
                  onClick={() => setStatusModalAction('ON_HOLD')}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 text-xs font-bold transition-colors"
                >
                  <PauseCircle className="w-3.5 h-3.5" />
                  <span>Put On Hold</span>
                </button>
                <button
                  onClick={() => setStatusModalAction('COMPLETED')}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-colors"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Mark Complete</span>
                </button>
              </>
            )}

            {engagement.status === 'ON_HOLD' && (
              <button
                onClick={() => handleStatusTransition('ACTIVE')}
                disabled={isUpdatingStatus}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-colors"
              >
                <PlayCircle className="w-3.5 h-3.5" />
                <span>Resume Engagement</span>
              </button>
            )}

            {engagement.status !== 'CANCELLED' && engagement.status !== 'COMPLETED' && (
              <button
                onClick={() => setStatusModalAction('CANCELLED')}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-rose-600 hover:bg-rose-50 text-xs font-bold transition-colors"
              >
                <XCircle className="w-3.5 h-3.5" />
                <span>Cancel</span>
              </button>
            )}
          </div>
        </div>

        {/* Mandate Key Attributes Bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-slate-100 text-xs">
          <div>
            <span className="text-slate-400 block text-[11px]">Primary Preparer</span>
            <span className="font-bold text-slate-800 mt-0.5 block truncate">
              {engagement.assignedUserName || 'Unassigned'}
            </span>
          </div>
          <div>
            <span className="text-slate-400 block text-[11px]">Partner Reviewer</span>
            <span className="font-bold text-purple-700 mt-0.5 block truncate">
              {engagement.reviewerUserName || 'No Reviewer Assigned'}
            </span>
          </div>
          <div>
            <span className="text-slate-400 block text-[11px]">Mandate Period</span>
            <span className="font-bold text-slate-800 mt-0.5 block">
              {engagement.startDate || '—'} {engagement.endDate ? `to ${engagement.endDate}` : ''}
            </span>
          </div>
          <div>
            <span className="text-slate-400 block text-[11px]">Created Date</span>
            <span className="font-bold text-slate-800 mt-0.5 block">
              {engagement.createdAt ? engagement.createdAt.split('T')[0] : '—'}
            </span>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 overflow-x-auto">
        <button
          onClick={() => setActiveTab('overview')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 flex items-center gap-2',
            activeTab === 'overview'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          )}
        >
          <FileText className="w-4 h-4" />
          <span>Overview & Scope</span>
        </button>

        <button
          onClick={() => setActiveTab('work_items')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 flex items-center gap-2',
            activeTab === 'work_items'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          )}
        >
          <Layers className="w-4 h-4" />
          <span>Compliance Periods & Work Items</span>
        </button>

        <button
          onClick={() => setActiveTab('documents')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 flex items-center gap-2',
            activeTab === 'documents'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          )}
        >
          <FolderLock className="w-4 h-4" />
          <span>Documents Vault</span>
        </button>

        <button
          onClick={() => setActiveTab('billing')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 flex items-center gap-2',
            activeTab === 'billing'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          )}
        >
          <Receipt className="w-4 h-4" />
          <span>Billing & Invoices</span>
        </button>

        <button
          onClick={() => setActiveTab('activity')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold border-b-2 transition-all shrink-0 flex items-center gap-2',
            activeTab === 'activity'
              ? 'border-brand-600 text-brand-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          )}
        >
          <History className="w-4 h-4" />
          <span>Audit Activity</span>
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Column */}
          <div className="lg:col-span-2 space-y-6">
            {/* Mandate Scope */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-3">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <FileText className="w-4 h-4 text-brand-600" />
                <span>Scope of Engagement</span>
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-wrap">
                {engagement.description || 'No detailed scope description provided for this engagement.'}
              </p>
            </div>

            {/* Internal Instructions & Notes */}
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-3">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-brand-600" />
                <span>Internal Execution Notes</span>
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-wrap">
                {engagement.notes || 'No internal notes or fee arrangements documented.'}
              </p>
            </div>
          </div>

          {/* Right Column: Client Summary */}
          <div className="space-y-6">
            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Building2 className="w-4 h-4 text-brand-600" />
                <span>Client Entity</span>
              </h3>
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 space-y-2 text-xs">
                <p className="font-bold text-slate-900 text-sm">{engagement.clientName}</p>
                <Link
                  to={`/clients/${engagement.clientId}`}
                  className="inline-flex items-center gap-1 text-xs font-bold text-brand-600 hover:text-brand-700"
                >
                  <span>Open Client 360° Profile</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-3">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-brand-600" />
                <span>Practice Governance</span>
              </h3>
              <p className="text-xs text-slate-500">
                All filings and compliance work under this engagement are audited and require partner sign-off.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Tab: Work Items Placeholder (Anchor for P0.2) */}
      {activeTab === 'work_items' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center space-y-4 shadow-2xs">
          <div className="w-12 h-12 bg-purple-50 text-purple-600 rounded-2xl flex items-center justify-center mx-auto border border-purple-100">
            <Layers className="w-6 h-6" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="text-sm font-bold text-slate-900">Compliance Periods & Work Items</h3>
            <p className="text-xs text-slate-500">
              Recurring monthly/quarterly compliance periods, return filing checklists, and task workflows for{' '}
              <span className="font-semibold text-slate-800">{engagement.name}</span> will be managed here.
            </p>
          </div>
          <Link
            to="/compliance/workbench"
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold shadow-xs transition-colors"
          >
            <span>Open Compliance Workbench</span>
            <ChevronRight className="w-4 h-4" />
          </Link>
        </div>
      )}

      {/* Tab: Documents */}
      {activeTab === 'documents' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center space-y-4 shadow-2xs">
          <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mx-auto border border-blue-100">
            <FolderLock className="w-6 h-6" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="text-sm font-bold text-slate-900">Engagement Documents Vault</h3>
            <p className="text-xs text-slate-500">
              Client invoices, working papers, signed engagement letters, and return acknowledgements.
            </p>
          </div>
          <Link
            to="/documents"
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
          >
            <span>Open Document Vault</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </Link>
        </div>
      )}

      {/* Tab: Billing */}
      {activeTab === 'billing' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center space-y-4 shadow-2xs">
          <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto border border-emerald-100">
            <Receipt className="w-6 h-6" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="text-sm font-bold text-slate-900">Engagement Invoicing & Retainers</h3>
            <p className="text-xs text-slate-500">
              Generate invoices, track retainers, and record client fee payments for this engagement mandate.
            </p>
          </div>
          <Link
            to="/billing"
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition-colors"
          >
            <span>Open Practice Billing</span>
            <ChevronRight className="w-4 h-4" />
          </Link>
        </div>
      )}

      {/* Tab: Activity */}
      {activeTab === 'activity' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-4">
          <h3 className="text-sm font-bold text-slate-900">Engagement Lifecycle Timeline</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs">
              <div className="w-2 h-2 rounded-full bg-brand-600 mt-1.5 shrink-0" />
              <div className="space-y-0.5">
                <p className="font-bold text-slate-800">
                  Engagement created as {engagement.status}
                </p>
                <p className="text-[11px] text-slate-400">
                  Created code {engagement.engagementCode} • {engagement.createdAt ? engagement.createdAt.split('T')[0] : 'Recently'}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Status Reason Modal */}
      {statusModalAction && (
        <Modal
          isOpen={!!statusModalAction}
          onClose={() => setStatusModalAction(null)}
          title={`Update Engagement Status to ${statusModalAction}`}
          maxWidth="md"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-600">
              Please specify any notes or rationale for transitioning this engagement to{' '}
              <span className="font-bold text-slate-900">{statusModalAction}</span>.
            </p>
            <textarea
              rows={3}
              placeholder="e.g. Awaiting client documents / Final return successfully filed"
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setStatusModalAction(null)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isUpdatingStatus}
                onClick={() => handleStatusTransition(statusModalAction)}
                className="px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isUpdatingStatus ? 'Updating...' : 'Confirm Status Change'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Edit Mandate Modal */}
      {isEditModalOpen && (
        <Modal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          title="Edit Engagement Mandate"
          maxWidth="xl"
        >
          <form onSubmit={handleSaveEdit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Engagement Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={editFormData.name || ''}
                onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Scope & Description
              </label>
              <textarea
                rows={2}
                value={editFormData.description || ''}
                onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Priority</label>
                <select
                  value={editFormData.priority || 'MEDIUM'}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      priority: e.target.value as EngagementPriorityType,
                    })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium"
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="URGENT">URGENT</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Start Date</label>
                <input
                  type="date"
                  value={editFormData.startDate || ''}
                  onChange={(e) => setEditFormData({ ...editFormData, startDate: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">End Date</label>
                <input
                  type="date"
                  value={editFormData.endDate || ''}
                  onChange={(e) => setEditFormData({ ...editFormData, endDate: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Internal Notes</label>
              <input
                type="text"
                value={editFormData.notes || ''}
                onChange={(e) => setEditFormData({ ...editFormData, notes: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSavingEdit}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isSavingEdit ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default EngagementOverviewPage;
