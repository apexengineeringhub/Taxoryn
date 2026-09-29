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
  ChevronDown,
  ChevronUp,
  UserCheck,
  Receipt,
  FolderLock,
  History,
  Plus,
  Play,
  RotateCw,
  RefreshCw,
  Sparkles,
  CheckSquare,
  Filter,
  Trash2,
} from 'lucide-react';
import { engagementsApi, servicesApi, clientApi, workTemplatesApi, engagementWorkApi, workInstancesApi } from '../api/endpoints';
import {
  EngagementDto,
  EngagementStatusType,
  EngagementPriorityType,
  ServiceDto,
  UpdateEngagementPayload,
  UpdateEngagementAssignmentPayload,
  WorkTemplateDto,
  EngagementWorkTemplateDto,
  WorkInstanceDto,
  WorkInstanceStatusType,
  RecurrenceType,
  EnableEngagementTemplatePayload,
  GenerateWorkInstancePayload,
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

  // Work Items & Templates State
  const [linkedTemplates, setLinkedTemplates] = useState<EngagementWorkTemplateDto[]>([]);
  const [workInstances, setWorkInstances] = useState<WorkInstanceDto[]>([]);
  const [availablePracticeTemplates, setAvailablePracticeTemplates] = useState<WorkTemplateDto[]>([]);
  const [isLoadingWorkData, setIsLoadingWorkData] = useState<boolean>(false);
  const [expandedWorkInstanceIds, setExpandedWorkInstanceIds] = useState<Set<string>>(new Set());

  // Enable Template Modal State
  const [isEnableTemplateModalOpen, setIsEnableTemplateModalOpen] = useState<boolean>(false);
  const [enableTemplateForm, setEnableTemplateForm] = useState<{
    templateId: string;
    recurrenceType: RecurrenceType;
    recurrenceInterval: number;
    dayOfMonth?: number;
  }>({
    templateId: '',
    recurrenceType: 'MONTHLY',
    recurrenceInterval: 1,
    dayOfMonth: 20,
  });
  const [isSubmittingEnableTemplate, setIsSubmittingEnableTemplate] = useState<boolean>(false);

  // Generate Work Modal State
  const [isGenerateWorkModalOpen, setIsGenerateWorkModalOpen] = useState<boolean>(false);
  const [generateWorkForm, setGenerateWorkForm] = useState<{
    templateId: string;
    periodStart: string;
    periodEnd: string;
    customTitle: string;
    targetDueDate: string;
    assignedUserId: string;
  }>({
    templateId: '',
    periodStart: '',
    periodEnd: '',
    customTitle: '',
    targetDueDate: '',
    assignedUserId: '',
  });
  const [isSubmittingGenerateWork, setIsSubmittingGenerateWork] = useState<boolean>(false);

  // Work Instance Status Update Modal State
  const [statusModalInstance, setStatusModalInstance] = useState<WorkInstanceDto | null>(null);
  const [newInstanceStatus, setNewInstanceStatus] = useState<WorkInstanceStatusType>('IN_PROGRESS');
  const [instanceStatusNotes, setInstanceStatusNotes] = useState<string>('');
  const [isUpdatingInstanceStatus, setIsUpdatingInstanceStatus] = useState<boolean>(false);

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

  // Load Work Templates & Instances
  const loadWorkData = useCallback(async () => {
    if (!id) return;
    setIsLoadingWorkData(true);
    try {
      const [enabledTpls, instsPage, availTpls] = await Promise.all([
        engagementWorkApi.getEnabledTemplates(id),
        engagementWorkApi.getWorkInstances(id),
        engagementWorkApi.getAvailableTemplates(id),
      ]);
      setLinkedTemplates(enabledTpls || []);
      setWorkInstances(instsPage?.content || []);
      setAvailablePracticeTemplates(availTpls || []);
    } catch (err: any) {
      console.error('Failed to load engagement work data', err);
    } finally {
      setIsLoadingWorkData(false);
    }
  }, [id]);

  useEffect(() => {
    loadEngagement();
    loadWorkData();
    servicesApi.getAll().then(setServices).catch(() => {});
  }, [loadEngagement, loadWorkData]);

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

  const handleEnableTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !enableTemplateForm.templateId) return;
    try {
      setIsSubmittingEnableTemplate(true);
      await engagementWorkApi.enableTemplate(id, enableTemplateForm.templateId, {
        recurrenceType: enableTemplateForm.recurrenceType,
        recurrenceInterval: enableTemplateForm.recurrenceInterval,
        dayOfMonth: enableTemplateForm.dayOfMonth,
      });
      await loadWorkData();
      setIsEnableTemplateModalOpen(false);
      setEnableTemplateForm({
        templateId: '',
        recurrenceType: 'MONTHLY',
        recurrenceInterval: 1,
        dayOfMonth: 20,
      });
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to link work template to engagement');
    } finally {
      setIsSubmittingEnableTemplate(false);
    }
  };

  const handleDisableTemplate = async (templateId: string) => {
    if (!id || !confirm('Are you sure you want to unlink this template from the engagement?')) return;
    try {
      await engagementWorkApi.disableTemplate(id, templateId);
      await loadWorkData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to disable template');
    }
  };

  const handleGenerateWork = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !generateWorkForm.templateId || !generateWorkForm.periodStart || !generateWorkForm.periodEnd) return;
    try {
      setIsSubmittingGenerateWork(true);
      await engagementWorkApi.generateWork(id, {
        templateId: generateWorkForm.templateId,
        periodStart: generateWorkForm.periodStart,
        periodEnd: generateWorkForm.periodEnd,
        title: generateWorkForm.customTitle || undefined,
        dueDate: generateWorkForm.targetDueDate || undefined,
        assignedUserId: generateWorkForm.assignedUserId || engagement?.assignedUserId || undefined,
      });
      await loadWorkData();
      setIsGenerateWorkModalOpen(false);
      setGenerateWorkForm({
        templateId: '',
        periodStart: '',
        periodEnd: '',
        customTitle: '',
        targetDueDate: '',
        assignedUserId: '',
      });
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to generate work instance');
    } finally {
      setIsSubmittingGenerateWork(false);
    }
  };

  const handleUpdateInstanceStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!statusModalInstance) return;
    try {
      setIsUpdatingInstanceStatus(true);
      await workInstancesApi.updateStatus(statusModalInstance.id, {
        status: newInstanceStatus,
        notes: instanceStatusNotes.trim() || undefined,
      });
      await loadWorkData();
      setStatusModalInstance(null);
      setInstanceStatusNotes('');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update work instance status');
    } finally {
      setIsUpdatingInstanceStatus(false);
    }
  };

  const toggleExpandInstance = (instanceId: string) => {
    setExpandedWorkInstanceIds((prev) => {
      const next = new Set(prev);
      if (next.has(instanceId)) next.delete(instanceId);
      else next.add(instanceId);
      return next;
    });
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

      {/* Tab: Work Items (P0.2 Recurring Compliance & Work Templates) */}
      {activeTab === 'work_items' && (
        <div className="space-y-6">
          {/* Header Action Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs">
            <div>
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Layers className="w-4 h-4 text-brand-600" />
                <span>Recurring Work Engine & Compliance Pipeline</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Standardized work templates and concrete compliance periods for this engagement mandate.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsEnableTemplateModalOpen(true)}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold shadow-2xs transition-colors"
              >
                <Plus className="w-3.5 h-3.5 text-brand-600" />
                <span>Link Template</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  if (linkedTemplates.length === 0) {
                    alert('Please link at least one work template first before generating a work period.');
                    return;
                  }
                  setGenerateWorkForm({
                    templateId: linkedTemplates[0].templateId,
                    periodStart: new Date().toISOString().split('T')[0],
                    periodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                    customTitle: '',
                    targetDueDate: '',
                    assignedUserId: engagement.assignedUserId || '',
                  });
                  setIsGenerateWorkModalOpen(true);
                }}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
              >
                <Play className="w-3.5 h-3.5" />
                <span>Generate Period Work</span>
              </button>
            </div>
          </div>

          {/* Section 1: Linked Templates */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h4 className="text-xs font-black uppercase tracking-wider text-slate-500 flex items-center gap-2">
                <Sparkles className="w-3.5 h-3.5 text-brand-600" />
                <span>Active Mandate Templates ({linkedTemplates.length})</span>
              </h4>
              <Link
                to="/work-templates"
                className="text-xs font-bold text-brand-600 hover:text-brand-700 inline-flex items-center gap-1"
              >
                <span>Manage Practice Templates</span>
                <ChevronRight className="w-3 h-3" />
              </Link>
            </div>

            {linkedTemplates.length === 0 ? (
              <div className="py-6 text-center border border-dashed border-slate-200 rounded-xl bg-slate-50/50 space-y-2">
                <Layers className="w-8 h-8 text-slate-300 mx-auto" />
                <p className="text-xs font-bold text-slate-700">No Work Templates Linked Yet</p>
                <p className="text-[11px] text-slate-500 max-w-sm mx-auto">
                  Link a practice compliance template (e.g., GST Monthly Returns, TDS Quarterly) to auto-generate checklists and track recurring deadlines.
                </p>
                <button
                  type="button"
                  onClick={() => setIsEnableTemplateModalOpen(true)}
                  className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold rounded-lg transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Link Template Now</span>
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {linkedTemplates.map((tpl) => (
                  <div
                    key={tpl.id}
                    className="p-3.5 border border-slate-200 rounded-xl bg-slate-50/60 hover:bg-slate-50 transition-colors flex flex-col justify-between space-y-3"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-mono text-[10px] font-bold text-brand-700 bg-brand-50 border border-brand-200 px-2 py-0.5 rounded">
                          {tpl.templateCode}
                        </span>
                        <div className="flex items-center gap-1.5">
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200 uppercase">
                            {tpl.recurrenceType}
                          </span>
                          {tpl.active && (
                            <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                              Active
                            </span>
                          )}
                        </div>
                      </div>
                      <h5 className="font-bold text-xs text-slate-900 mt-2">{tpl.templateName}</h5>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        {tpl.taskCount} standard tasks included
                      </p>
                    </div>

                    <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between text-[11px]">
                      <button
                        type="button"
                        onClick={() => {
                          setGenerateWorkForm({
                            templateId: tpl.templateId,
                            periodStart: new Date().toISOString().split('T')[0],
                            periodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                            customTitle: '',
                            targetDueDate: '',
                            assignedUserId: engagement.assignedUserId || '',
                          });
                          setIsGenerateWorkModalOpen(true);
                        }}
                        className="inline-flex items-center gap-1 font-bold text-brand-600 hover:text-brand-700"
                      >
                        <Play className="w-3 h-3" />
                        <span>Generate Instance</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDisableTemplate(tpl.templateId)}
                        className="text-slate-400 hover:text-rose-600 transition-colors"
                        title="Unlink template"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Section 2: Concrete Generated Work Instances Pipeline */}
          <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h4 className="text-xs font-black uppercase tracking-wider text-slate-500 flex items-center gap-2">
                <Briefcase className="w-3.5 h-3.5 text-brand-600" />
                <span>Generated Compliance Periods & Work ({workInstances.length})</span>
              </h4>
              <button
                type="button"
                onClick={loadWorkData}
                disabled={isLoadingWorkData}
                className="text-slate-400 hover:text-slate-700 transition-colors p-1"
                title="Refresh Work Pipeline"
              >
                <RefreshCw className={clsx('w-3.5 h-3.5', isLoadingWorkData && 'animate-spin')} />
              </button>
            </div>

            {workInstances.length === 0 ? (
              <div className="py-8 text-center border border-dashed border-slate-200 rounded-xl bg-slate-50/50 space-y-2">
                <Clock className="w-8 h-8 text-slate-300 mx-auto" />
                <p className="text-xs font-bold text-slate-700">No Compliance Periods Generated</p>
                <p className="text-[11px] text-slate-500 max-w-sm mx-auto">
                  Work instances represent discrete compliance periods (e.g. "October 2026 GSTR-3B") and their unified tasks.
                </p>
                <button
                  type="button"
                  disabled={linkedTemplates.length === 0}
                  onClick={() => {
                    if (linkedTemplates.length === 0) return;
                    setGenerateWorkForm({
                      templateId: linkedTemplates[0].templateId,
                      periodStart: new Date().toISOString().split('T')[0],
                      periodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                      customTitle: '',
                      targetDueDate: '',
                      assignedUserId: engagement.assignedUserId || '',
                    });
                    setIsGenerateWorkModalOpen(true);
                  }}
                  className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-50"
                >
                  <Play className="w-3.5 h-3.5" />
                  <span>Generate Next Work Period</span>
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {workInstances.map((inst) => {
                  const isExpanded = expandedWorkInstanceIds.has(inst.id);
                  const progress = inst.totalTasks > 0 ? Math.round((inst.completedTasks / inst.totalTasks) * 100) : 0;

                  return (
                    <div
                      key={inst.id}
                      className="border border-slate-200 rounded-2xl p-4 bg-white hover:border-slate-300 transition-all shadow-2xs space-y-3"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                        <div className="space-y-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-mono text-xs font-bold text-slate-900">
                              {inst.title}
                            </span>
                            <StatusBadge status={inst.status} />
                          </div>
                          <div className="flex items-center gap-3 text-xs text-slate-500 flex-wrap">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3.5 h-3.5 text-slate-400" />
                              <span>Period: {inst.periodStart} → {inst.periodEnd}</span>
                            </span>
                            <span>•</span>
                            <span className="flex items-center gap-1">
                              <Clock className="w-3.5 h-3.5 text-slate-400" />
                              <span>Due: {inst.dueDate || 'No Due Date'}</span>
                            </span>
                            {inst.assignedUserName && (
                              <>
                                <span>•</span>
                                <span className="flex items-center gap-1">
                                  <User className="w-3.5 h-3.5 text-slate-400" />
                                  <span>{inst.assignedUserName}</span>
                                </span>
                              </>
                            )}
                          </div>
                        </div>

                        {/* Progress & Actions */}
                        <div className="flex items-center gap-3 shrink-0">
                          <div className="w-32 text-right">
                            <div className="flex items-center justify-between text-[11px] font-bold mb-1">
                              <span className="text-slate-500">Tasks</span>
                              <span className="text-slate-800">{inst.completedTasks} / {inst.totalTasks}</span>
                            </div>
                            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-brand-500 transition-all duration-300"
                                style={{ width: `${progress}%` }}
                              />
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => {
                              setStatusModalInstance(inst);
                              setNewInstanceStatus(inst.status);
                              setInstanceStatusNotes('');
                            }}
                            className="px-2.5 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-xs font-bold text-slate-700 transition-colors"
                          >
                            Update Status
                          </button>

                          <button
                            type="button"
                            onClick={() => toggleExpandInstance(inst.id)}
                            className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors"
                            title={isExpanded ? 'Hide tasks' : 'Show tasks'}
                          >
                            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                          </button>
                        </div>
                      </div>

                      {/* Expanded Task Breakdown */}
                      {isExpanded && (
                        <div className="pt-3 border-t border-slate-100 space-y-2">
                          <div className="flex items-center justify-between text-xs font-bold text-slate-600">
                            <span>Instantiated Task Checklist ({inst.tasks?.length || 0})</span>
                            <Link
                              to="/tasks"
                              className="text-brand-600 hover:underline text-[11px]"
                            >
                              Open in Task Kanban →
                            </Link>
                          </div>

                          {(!inst.tasks || inst.tasks.length === 0) ? (
                            <p className="text-xs text-slate-400 italic py-2">No individual tasks registered for this instance.</p>
                          ) : (
                            <div className="space-y-1.5">
                              {inst.tasks.map((task) => (
                                <div
                                  key={task.id}
                                  className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs hover:bg-white hover:border-slate-200 transition-colors"
                                >
                                  <div className="flex items-center gap-2.5 min-w-0">
                                    <CheckSquare className={clsx('w-4 h-4 shrink-0', task.status === 'COMPLETED' ? 'text-emerald-600' : 'text-slate-400')} />
                                    <span className={clsx('font-medium text-slate-800 truncate', task.status === 'COMPLETED' && 'line-through text-slate-400')}>
                                      {task.title}
                                    </span>
                                  </div>
                                  <div className="flex items-center gap-2 shrink-0 text-[11px]">
                                    <span className="font-bold px-1.5 py-0.5 rounded bg-slate-200/70 text-slate-700 text-[10px]">
                                      {task.priority}
                                    </span>
                                    <StatusBadge status={task.status} />
                                    <span className="text-slate-400">{task.dueDate || 'No Due'}</span>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
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

      {/* Enable Template Modal */}
      {isEnableTemplateModalOpen && (
        <Modal
          isOpen={isEnableTemplateModalOpen}
          onClose={() => setIsEnableTemplateModalOpen(false)}
          title="Link Work Template to Engagement"
          maxWidth="lg"
        >
          <form onSubmit={handleEnableTemplate} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Select Practice Template <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={enableTemplateForm.templateId}
                onChange={(e) => {
                  const tplId = e.target.value;
                  const tpl = availablePracticeTemplates.find((t) => t.id === tplId);
                  setEnableTemplateForm((prev) => ({
                    ...prev,
                    templateId: tplId,
                    recurrenceType: tpl?.recurrenceType || prev.recurrenceType,
                    recurrenceInterval: tpl?.recurrenceInterval || prev.recurrenceInterval,
                    dayOfMonth: tpl?.dayOfMonth ?? prev.dayOfMonth,
                  }));
                }}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
              >
                <option value="">Select a template...</option>
                {availablePracticeTemplates.map((t) => {
                  const isMatchingService = engagement.serviceId && t.serviceId === engagement.serviceId;
                  return (
                    <option key={t.id} value={t.id}>
                      [{t.templateCode}] {t.name} {isMatchingService ? '★ (Matches Engagement Service)' : ''}
                    </option>
                  );
                })}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Recurrence Schedule</label>
                <select
                  value={enableTemplateForm.recurrenceType}
                  onChange={(e) =>
                    setEnableTemplateForm({
                      ...enableTemplateForm,
                      recurrenceType: e.target.value as RecurrenceType,
                    })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="ANNUALLY">Annually</option>
                  <option value="ONE_OFF">One-Off</option>
                  <option value="CUSTOM">Custom Interval</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Recurrence Interval</label>
                <input
                  type="number"
                  min={1}
                  value={enableTemplateForm.recurrenceInterval}
                  onChange={(e) =>
                    setEnableTemplateForm({
                      ...enableTemplateForm,
                      recurrenceInterval: parseInt(e.target.value) || 1,
                    })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Day of Month (Default Due Day)
              </label>
              <input
                type="number"
                min={1}
                max={31}
                value={enableTemplateForm.dayOfMonth || 20}
                onChange={(e) =>
                  setEnableTemplateForm({
                    ...enableTemplateForm,
                    dayOfMonth: parseInt(e.target.value) || 20,
                  })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
              />
              <span className="text-[11px] text-slate-400 mt-0.5 block">
                e.g. 20 = GSTR-3B due on 20th of the following month
              </span>
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsEnableTemplateModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmittingEnableTemplate || !enableTemplateForm.templateId}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isSubmittingEnableTemplate ? 'Linking...' : 'Link Template'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Generate Work Modal */}
      {isGenerateWorkModalOpen && (
        <Modal
          isOpen={isGenerateWorkModalOpen}
          onClose={() => setIsGenerateWorkModalOpen(false)}
          title="Generate Concrete Work Period"
          maxWidth="lg"
        >
          <form onSubmit={handleGenerateWork} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Work Template <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={generateWorkForm.templateId}
                onChange={(e) => setGenerateWorkForm({ ...generateWorkForm, templateId: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
              >
                <option value="">Select linked template...</option>
                {linkedTemplates.map((t) => (
                  <option key={t.templateId} value={t.templateId}>
                    [{t.templateCode}] {t.templateName}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Period Start Date <span className="text-rose-500">*</span>
                </label>
                <input
                  type="date"
                  required
                  value={generateWorkForm.periodStart}
                  onChange={(e) => setGenerateWorkForm({ ...generateWorkForm, periodStart: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Period End Date <span className="text-rose-500">*</span>
                </label>
                <input
                  type="date"
                  required
                  value={generateWorkForm.periodEnd}
                  onChange={(e) => setGenerateWorkForm({ ...generateWorkForm, periodEnd: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Custom Title (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. GST Monthly Compliance — Oct 2026"
                  value={generateWorkForm.customTitle}
                  onChange={(e) => setGenerateWorkForm({ ...generateWorkForm, customTitle: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Target Due Date (Optional)
                </label>
                <input
                  type="date"
                  value={generateWorkForm.targetDueDate}
                  onChange={(e) => setGenerateWorkForm({ ...generateWorkForm, targetDueDate: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsGenerateWorkModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmittingGenerateWork || !generateWorkForm.templateId || !generateWorkForm.periodStart || !generateWorkForm.periodEnd}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isSubmittingGenerateWork ? 'Instantiating Work...' : 'Generate Work & Tasks'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Update Work Instance Status Modal */}
      {statusModalInstance && (
        <Modal
          isOpen={!!statusModalInstance}
          onClose={() => setStatusModalInstance(null)}
          title={`Update Status: ${statusModalInstance.title}`}
          maxWidth="md"
        >
          <form onSubmit={handleUpdateInstanceStatus} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Target Status</label>
              <select
                value={newInstanceStatus}
                onChange={(e) => setNewInstanceStatus(e.target.value as WorkInstanceStatusType)}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium"
              >
                <option value="PLANNED">PLANNED</option>
                <option value="IN_PROGRESS">IN PROGRESS</option>
                <option value="BLOCKED">BLOCKED</option>
                <option value="UNDER_REVIEW">UNDER REVIEW</option>
                <option value="FILED">FILED</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Audit Notes / Comments</label>
              <textarea
                rows={3}
                placeholder="e.g. Return challan generated and verified / Signed off by partner"
                value={instanceStatusNotes}
                onChange={(e) => setInstanceStatusNotes(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setStatusModalInstance(null)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isUpdatingInstanceStatus}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isUpdatingInstanceStatus ? 'Saving...' : 'Update Status'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default EngagementOverviewPage;
