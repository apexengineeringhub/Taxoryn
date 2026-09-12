import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  Scale,
  ArrowLeft,
  Calendar,
  Clock,
  AlertTriangle,
  CheckCircle2,
  FileText,
  User,
  Building,
  Gavel,
  CheckCheck,
  Send,
  Upload,
  Plus,
  MessageSquare,
  History,
  FolderLock,
  ExternalLink,
  Flame,
  IndianRupee,
  Edit3,
  CheckSquare,
  Sparkles,
  Paperclip,
  Check,
  X,
  RefreshCw,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { noticesApi, taskApi, documentApi } from '../api/endpoints';
import {
  TaxNotice,
  NoticeResponse,
  NoticeHearing,
  NoticeActivity,
  NoticeStatus,
  NoticePriority,
  CreateNoticeResponseRequest,
  ReviewNoticeResponseRequest,
  ScheduleHearingRequest,
  RecordHearingOutcomeRequest,
  SubmitNoticeRequest,
  CloseNoticeRequest,
  Task,
  DocumentItem,
  DocumentRequest,
} from '../types';
import clsx from 'clsx';

export const NoticeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [notice, setNotice] = useState<TaxNotice | null>(null);
  const [responses, setResponses] = useState<NoticeResponse[]>([]);
  const [hearings, setHearings] = useState<NoticeHearing[]>([]);
  const [activities, setActivities] = useState<NoticeActivity[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [docRequests, setDocRequests] = useState<DocumentRequest[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'responses' | 'hearings' | 'filing' | 'tasks' | 'documents' | 'timeline'>('overview');

  // Modal States
  const [isDraftModalOpen, setIsDraftModalOpen] = useState(false);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [selectedResponseForReview, setSelectedResponseForReview] = useState<NoticeResponse | null>(null);
  const [reviewAction, setReviewAction] = useState<'APPROVE_REVIEW' | 'REQUEST_REVISION' | 'APPROVE_PARTNER'>('APPROVE_REVIEW');
  const [reviewComments, setReviewComments] = useState('');

  const [isHearingModalOpen, setIsHearingModalOpen] = useState(false);
  const [isOutcomeModalOpen, setIsOutcomeModalOpen] = useState(false);
  const [selectedHearing, setSelectedHearing] = useState<NoticeHearing | null>(null);

  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
  const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);
  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false);
  const [noteText, setNoteText] = useState('');

  // Draft Response Form
  const [draftForm, setDraftForm] = useState<CreateNoticeResponseRequest>({
    responseTitle: '',
    responseSummary: '',
    legalGrounds: '',
    factsOfCase: '',
    submitForReview: false,
  });

  // Schedule Hearing Form
  const [hearingForm, setHearingForm] = useState<ScheduleHearingRequest>({
    hearingDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    hearingTime: '11:00 AM',
    hearingMode: 'VIRTUAL_VC',
    hearingLink: '',
    authorityName: '',
    officerName: '',
    proceedingsSummary: '',
  });

  // Outcome Form
  const [outcomeForm, setOutcomeForm] = useState<RecordHearingOutcomeRequest>({
    status: 'COMPLETED',
    proceedingsSummary: '',
    outcomeSummary: '',
    nextAction: '',
    nextHearingDate: '',
  });

  // Submit Notice Form
  const [submitForm, setSubmitForm] = useState<SubmitNoticeRequest>({
    submissionMode: 'INCOME_TAX_PORTAL',
    portalAcknowledgementNumber: '',
    remarks: '',
  });

  // Close Notice Form
  const [closeForm, setCloseForm] = useState<CloseNoticeRequest>({
    closureStatus: 'RESOLVED',
    closureDate: new Date().toISOString().split('T')[0],
    closureRemarks: '',
  });

  const [isActionLoading, setIsActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadNoticeData(id);
    }
  }, [id]);

  const loadNoticeData = async (noticeId: string) => {
    try {
      setIsLoading(true);
      const [nRes, rRes, hRes, aRes] = await Promise.all([
        noticesApi.getNoticeById(noticeId),
        noticesApi.getResponses(noticeId),
        noticesApi.getHearings(noticeId),
        noticesApi.getActivities(noticeId),
      ]);
      setNotice(nRes);
      setResponses(rRes);
      setHearings(hRes);
      setActivities(aRes);

      // Initialize forms with notice context
      setHearingForm((prev) => ({
        ...prev,
        authorityName: nRes.issuingAuthority || '',
        officerName: nRes.issuingOfficerName || '',
      }));

      // Load linked tasks and documents
      loadLinkedResources(noticeId, nRes.clientId);
    } catch (err) {
      console.error('Failed to load notice case details', err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadLinkedResources = async (noticeId: string, clientId?: string) => {
    try {
      const [tasksRes, docsRes] = await Promise.allSettled([
        taskApi.getAll({ size: 50 }),
        documentApi.getAll({ clientId }),
      ]);
      if (tasksRes.status === 'fulfilled' && tasksRes.value?.content) {
        setTasks(tasksRes.value.content.filter((t: any) => t.noticeId === noticeId));
      }
      if (docsRes.status === 'fulfilled' && docsRes.value?.content) {
        setDocuments(docsRes.value.content.filter((d: any) => d.noticeId === noticeId));
      }
    } catch (err) {
      console.error('Failed to load linked tasks/documents', err);
    }
  };

  // --- Handlers ---

  const handleCreateDraft = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !draftForm.responseTitle.trim()) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.createResponse(id, draftForm);
      setIsDraftModalOpen(false);
      setDraftForm({
        responseTitle: '',
        responseSummary: '',
        legalGrounds: '',
        factsOfCase: '',
        submitForReview: false,
      });
      await loadNoticeData(id);
      setActiveTab('responses');
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to create response draft');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleReviewAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !selectedResponseForReview) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.reviewResponse(id, selectedResponseForReview.id, {
        action: reviewAction,
        comments: reviewComments,
      });
      setIsReviewModalOpen(false);
      setReviewComments('');
      await loadNoticeData(id);
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to record review');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleScheduleHearing = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.scheduleHearing(id, hearingForm);
      setIsHearingModalOpen(false);
      await loadNoticeData(id);
      setActiveTab('hearings');
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to schedule hearing');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleRecordOutcome = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !selectedHearing) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.recordHearingOutcome(id, selectedHearing.id, outcomeForm);
      setIsOutcomeModalOpen(false);
      await loadNoticeData(id);
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to record hearing outcome');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleSubmitNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.submitNotice(id, submitForm);
      setIsSubmitModalOpen(false);
      await loadNoticeData(id);
      setActiveTab('filing');
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to record portal filing');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleCloseNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.closeNotice(id, closeForm);
      setIsCloseModalOpen(false);
      await loadNoticeData(id);
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to close notice case');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !noteText.trim()) return;
    try {
      setIsActionLoading(true);
      setActionError(null);
      await noticesApi.addInternalNote(id, noteText.trim());
      setIsNoteModalOpen(false);
      setNoteText('');
      await loadNoticeData(id);
    } catch (err: any) {
      setActionError(err.response?.data?.message || err.message || 'Failed to add internal note');
    } finally {
      setIsActionLoading(false);
    }
  };

  const formatCurrency = (amount?: number) => {
    if (!amount && amount !== 0) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (isLoading || !notice) {
    return (
      <div className="py-24 text-center text-gray-500">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mb-3" />
        <p className="text-sm">Loading tax notice case details...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-16">
      {/* Top Breadcrumb & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <button
          onClick={() => navigate('/notices')}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-gray-600 hover:text-indigo-600 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Notice Center
        </button>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            size="sm"
            onClick={() => {
              setDraftForm({
                responseTitle: `Reply to Notice ${notice.noticeNumber} u/s ${notice.section || ''}`,
                responseSummary: '',
                legalGrounds: '',
                factsOfCase: '',
                submitForReview: false,
              });
              setIsDraftModalOpen(true);
            }}
            className="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-white"
          >
            <Edit3 className="w-4 h-4" /> Draft Written Response
          </Button>

          <Button
            size="sm"
            variant="outline"
            onClick={() => setIsHearingModalOpen(true)}
            className="flex items-center gap-1.5 text-purple-700 border-purple-200 hover:bg-purple-50"
          >
            <Gavel className="w-4 h-4 text-purple-600" /> Schedule Hearing
          </Button>

          <Button
            size="sm"
            variant="outline"
            onClick={() => setIsSubmitModalOpen(true)}
            className="flex items-center gap-1.5 text-emerald-700 border-emerald-200 hover:bg-emerald-50"
          >
            <Send className="w-4 h-4 text-emerald-600" /> Record Filing
          </Button>

          <Button
            size="sm"
            variant="outline"
            onClick={() => setIsCloseModalOpen(true)}
            className="flex items-center gap-1.5 text-gray-700 hover:bg-gray-50"
          >
            <CheckCircle2 className="w-4 h-4 text-gray-500" /> Resolve / Close
          </Button>
        </div>
      </div>

      {/* Case Header Card */}
      <Card className="p-6 bg-white border border-gray-200 shadow-sm space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
          <div className="space-y-2 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-2.5 py-1 text-xs font-bold rounded bg-indigo-100 text-indigo-800">
                {notice.department}
              </span>
              <h1 className="text-2xl font-bold text-gray-900">{notice.noticeNumber}</h1>
              {notice.section && (
                <span className="px-2.5 py-1 text-xs font-semibold rounded bg-gray-100 text-gray-700 border border-gray-300">
                  Section {notice.section}
                </span>
              )}
              {notice.isOverdue ? (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 text-xs font-bold rounded bg-red-600 text-white animate-pulse">
                  <Flame className="w-3.5 h-3.5" /> OVERDUE
                </span>
              ) : (
                <span className={clsx('px-2.5 py-0.5 text-xs font-bold rounded', {
                  'bg-rose-100 text-rose-800': notice.priority === 'CRITICAL',
                  'bg-amber-100 text-amber-800': notice.priority === 'HIGH',
                  'bg-blue-100 text-blue-800': notice.priority === 'MEDIUM',
                  'bg-gray-100 text-gray-700': notice.priority === 'LOW',
                })}>
                  {notice.priority}
                </span>
              )}
            </div>

            <div className="text-sm font-medium text-gray-800">{notice.subject}</div>

            {notice.dinNumber && (
              <div className="text-xs text-gray-500 font-mono">
                DIN: <span className="font-semibold text-gray-700">{notice.dinNumber}</span>
              </div>
            )}
          </div>

          {/* Quick Metrics Badge Group */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-right min-w-[130px]">
              <div className="text-xs text-gray-500">Demand Disputed</div>
              <div className="text-base font-bold text-gray-900">{formatCurrency(notice.demandAmount)}</div>
            </div>

            <div className={clsx('border rounded-lg p-3 text-right min-w-[140px]', notice.isOverdue ? 'bg-rose-50 border-rose-200 text-rose-800' : 'bg-gray-50 border-gray-200 text-gray-900')}>
              <div className="text-xs text-gray-500">Response Due</div>
              <div className="text-base font-bold">{notice.responseDueDate}</div>
              <div className="text-[11px] font-medium">
                {notice.daysRemaining !== undefined && (
                  notice.daysRemaining < 0 ? (
                    <span className="text-rose-600 font-bold">{Math.abs(notice.daysRemaining)} days overdue</span>
                  ) : notice.daysRemaining === 0 ? (
                    <span className="text-rose-600 font-bold">Due Today</span>
                  ) : (
                    <span className="text-gray-600">{notice.daysRemaining} days left</span>
                  )
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Client & Assigned Staff Ribbon */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 pt-4 border-t border-gray-100 text-xs">
          <div>
            <span className="text-gray-400 block">Client</span>
            <Link to={`/clients`} className="font-semibold text-indigo-600 hover:underline flex items-center gap-1 mt-0.5">
              <Building className="w-3.5 h-3.5" />
              {notice.clientName || 'Client'}
              {notice.clientPan && <span className="text-gray-500">({notice.clientPan})</span>}
            </Link>
          </div>

          <div>
            <span className="text-gray-400 block">Assigned Maker</span>
            <span className="font-medium text-gray-800 mt-0.5 block">
              {notice.assignedEmployeeName || <span className="text-gray-400 italic">Unassigned</span>}
            </span>
          </div>

          <div>
            <span className="text-gray-400 block">Reviewer (Checker)</span>
            <span className="font-medium text-gray-800 mt-0.5 block">
              {notice.reviewerEmployeeName || <span className="text-gray-400 italic">Unassigned</span>}
            </span>
          </div>

          <div>
            <span className="text-gray-400 block">Signing Partner</span>
            <span className="font-medium text-gray-800 mt-0.5 block">
              {notice.partnerEmployeeName || <span className="text-gray-400 italic">Unassigned</span>}
            </span>
          </div>
        </div>
      </Card>

      {/* Tabs Bar */}
      <div className="border-b border-gray-200">
        <nav className="flex space-x-6 overflow-x-auto text-sm font-medium">
          <button
            onClick={() => setActiveTab('overview')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'overview'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <FileText className="w-4 h-4" /> Case Overview
          </button>

          <button
            onClick={() => setActiveTab('responses')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'responses'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <Edit3 className="w-4 h-4" /> Responses & Reviews ({responses.length})
          </button>

          <button
            onClick={() => setActiveTab('hearings')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'hearings'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <Gavel className="w-4 h-4" /> Hearings ({hearings.length})
          </button>

          <button
            onClick={() => setActiveTab('filing')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'filing'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <Send className="w-4 h-4" /> Filing & Closure
          </button>

          <button
            onClick={() => setActiveTab('tasks')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'tasks'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <CheckSquare className="w-4 h-4" /> Tasks ({tasks.length})
          </button>

          <button
            onClick={() => setActiveTab('documents')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'documents'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <FolderLock className="w-4 h-4" /> Documents ({documents.length})
          </button>

          <button
            onClick={() => setActiveTab('timeline')}
            className={clsx(
              'py-3 border-b-2 font-semibold transition-colors flex items-center gap-1.5 whitespace-nowrap',
              activeTab === 'timeline'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            )}
          >
            <History className="w-4 h-4" /> Timeline ({activities.length})
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            {/* Statutory Details */}
            <Card className="p-6 bg-white border border-gray-200 space-y-4">
              <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider">Notice Case Summary</h3>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs">
                <div>
                  <span className="text-gray-400 block">Assessment Year</span>
                  <span className="font-semibold text-gray-800">{notice.assessmentYear || '—'}</span>
                </div>
                <div>
                  <span className="text-gray-400 block">Financial Year</span>
                  <span className="font-semibold text-gray-800">{notice.financialYear || '—'}</span>
                </div>
                <div>
                  <span className="text-gray-400 block">Notice Date</span>
                  <span className="font-semibold text-gray-800">{notice.noticeDate || '—'}</span>
                </div>
                <div>
                  <span className="text-gray-400 block">Received Date</span>
                  <span className="font-semibold text-gray-800">{notice.receivedDate}</span>
                </div>
                <div>
                  <span className="text-gray-400 block">Issuing Authority</span>
                  <span className="font-semibold text-gray-800">{notice.issuingAuthority || '—'}</span>
                </div>
                <div>
                  <span className="text-gray-400 block">Issuing Officer</span>
                  <span className="font-semibold text-gray-800">{notice.issuingOfficerName || '—'}</span>
                </div>
              </div>

              {notice.description && (
                <div className="pt-3 border-t border-gray-100">
                  <span className="text-xs font-semibold text-gray-600 block mb-1">Matter Description & Grounds:</span>
                  <p className="text-xs text-gray-700 bg-gray-50 p-3 rounded-lg leading-relaxed whitespace-pre-wrap">
                    {notice.description}
                  </p>
                </div>
              )}
            </Card>

            {/* Internal Confidential Notes */}
            <Card className="p-6 bg-white border border-gray-200 space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider flex items-center gap-1.5">
                  <MessageSquare className="w-4 h-4 text-amber-600" /> Internal Notes (Zero-Trust Practice Only)
                </h3>
                <Button size="sm" variant="outline" onClick={() => setIsNoteModalOpen(true)}>
                  <Plus className="w-3.5 h-3.5 mr-1" /> Add Note
                </Button>
              </div>
              <p className="text-xs text-gray-500">
                These notes are confidential to practice members and never visible to the client on the Client Portal.
              </p>
              {notice.internalNotes ? (
                <div className="text-xs text-gray-800 bg-amber-50/40 border border-amber-200/60 p-3 rounded-lg leading-relaxed whitespace-pre-wrap font-sans">
                  {notice.internalNotes}
                </div>
              ) : (
                <div className="text-xs text-gray-400 italic py-2">No internal notes logged yet.</div>
              )}
            </Card>
          </div>

          {/* Right Column: Key Contacts & Quick Status */}
          <div className="space-y-6">
            <Card className="p-5 bg-white border border-gray-200 space-y-3">
              <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">Client Information</h4>
              <div className="text-xs space-y-1.5">
                <div className="font-bold text-gray-900">{notice.clientName}</div>
                {notice.clientPan && <div className="text-gray-600 font-mono">PAN: {notice.clientPan}</div>}
                {notice.clientGstin && <div className="text-gray-600 font-mono">GSTIN: {notice.clientGstin}</div>}
                {notice.clientEmail && <div className="text-gray-600">Email: {notice.clientEmail}</div>}
                {notice.clientPhone && <div className="text-gray-600">Phone: {notice.clientPhone}</div>}
              </div>
            </Card>

            {notice.hearingDate && (
              <Card className="p-5 bg-purple-50/40 border border-purple-200 space-y-2">
                <h4 className="text-xs font-bold text-purple-900 uppercase tracking-wider flex items-center gap-1.5">
                  <Gavel className="w-4 h-4 text-purple-600" /> Scheduled Hearing
                </h4>
                <div className="text-xs space-y-1 text-purple-800">
                  <div className="font-semibold">Date: {notice.hearingDate} {notice.hearingTime ? `at ${notice.hearingTime}` : ''}</div>
                  <div>Authority: {notice.issuingAuthority || 'Assessment Unit'}</div>
                </div>
              </Card>
            )}
          </div>
        </div>
      )}

      {/* Responses & Maker-Checker Tab */}
      {activeTab === 'responses' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-gray-900">Response Drafts & Maker-Checker Reviews</h3>
              <p className="text-xs text-gray-500">
                Manage written reply drafts, multi-tiered partner sign-off, and legal justifications.
              </p>
            </div>
            <Button
              onClick={() => {
                setDraftForm({
                  responseTitle: `Reply to Notice ${notice.noticeNumber} (v${responses.length + 1})`,
                  responseSummary: '',
                  legalGrounds: '',
                  factsOfCase: '',
                  submitForReview: false,
                });
                setIsDraftModalOpen(true);
              }}
              className="flex items-center gap-1 bg-indigo-600 text-white"
            >
              <Plus className="w-4 h-4" /> Draft New Version
            </Button>
          </div>

          {responses.length === 0 ? (
            <Card className="p-12 text-center bg-white border border-gray-200">
              <Edit3 className="w-10 h-10 mx-auto text-gray-300 mb-2" />
              <h4 className="text-sm font-semibold text-gray-800">No Response Drafts Yet</h4>
              <p className="text-xs text-gray-500 mt-1">Start drafting the written submission for this tax notice.</p>
              <Button
                size="sm"
                onClick={() => setIsDraftModalOpen(true)}
                className="mt-3 bg-indigo-600 text-white"
              >
                Create First Draft (v1)
              </Button>
            </Card>
          ) : (
            <div className="space-y-4">
              {responses.map((resp) => (
                <Card key={resp.id} className="p-5 bg-white border border-gray-200 shadow-sm space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-gray-100 pb-3">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 text-xs font-bold rounded bg-indigo-100 text-indigo-800">
                        Version {resp.version}
                      </span>
                      <h4 className="font-bold text-gray-900 text-sm">{resp.responseTitle}</h4>
                      <span className={clsx('px-2 py-0.5 text-xs font-semibold rounded', {
                        'bg-gray-100 text-gray-800': resp.reviewStatus === 'DRAFT',
                        'bg-orange-100 text-orange-800': resp.reviewStatus === 'PENDING_REVIEW',
                        'bg-amber-100 text-amber-800': resp.reviewStatus === 'REVISION_REQUESTED',
                        'bg-teal-100 text-teal-800': resp.reviewStatus === 'APPROVED_BY_REVIEWER',
                        'bg-emerald-100 text-emerald-800': resp.reviewStatus === 'APPROVED_BY_PARTNER',
                        'bg-blue-100 text-blue-800': resp.reviewStatus === 'SUBMITTED',
                      })}>
                        {resp.reviewStatus.replace(/_/g, ' ')}
                      </span>
                    </div>

                    {/* Maker-Checker Actions */}
                    <div className="flex items-center gap-2">
                      {resp.reviewStatus === 'DRAFT' && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setSelectedResponseForReview(resp);
                            setReviewAction('APPROVE_REVIEW');
                            setIsReviewModalOpen(true);
                          }}
                          className="text-xs text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                        >
                          Submit for Review
                        </Button>
                      )}

                      {resp.reviewStatus === 'PENDING_REVIEW' && (
                        <>
                          <Button
                            size="sm"
                            onClick={() => {
                              setSelectedResponseForReview(resp);
                              setReviewAction('APPROVE_REVIEW');
                              setIsReviewModalOpen(true);
                            }}
                            className="text-xs bg-teal-600 hover:bg-teal-700 text-white"
                          >
                            Approve Review
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                              setSelectedResponseForReview(resp);
                              setReviewAction('REQUEST_REVISION');
                              setIsReviewModalOpen(true);
                            }}
                            className="text-xs text-amber-700 border-amber-300 hover:bg-amber-50"
                          >
                            Request Revision
                          </Button>
                        </>
                      )}

                      {resp.reviewStatus === 'APPROVED_BY_REVIEWER' && (
                        <Button
                          size="sm"
                          onClick={() => {
                            setSelectedResponseForReview(resp);
                            setReviewAction('APPROVE_PARTNER');
                            setIsReviewModalOpen(true);
                          }}
                          className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white"
                        >
                          Partner Sign-Off
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Body */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    {resp.factsOfCase && (
                      <div>
                        <span className="font-semibold text-gray-700 block mb-1">Facts of the Case:</span>
                        <div className="bg-gray-50 p-2.5 rounded border border-gray-100 whitespace-pre-wrap leading-relaxed">
                          {resp.factsOfCase}
                        </div>
                      </div>
                    )}
                    {resp.legalGrounds && (
                      <div>
                        <span className="font-semibold text-gray-700 block mb-1">Legal Grounds & Case Laws:</span>
                        <div className="bg-gray-50 p-2.5 rounded border border-gray-100 whitespace-pre-wrap leading-relaxed">
                          {resp.legalGrounds}
                        </div>
                      </div>
                    )}
                  </div>

                  {resp.reviewComments && (
                    <div className="p-2.5 bg-amber-50/60 border border-amber-200 rounded text-xs text-amber-900">
                      <span className="font-bold">Review Comments:</span> {resp.reviewComments}
                    </div>
                  )}

                  {/* Sign-Off Trail */}
                  <div className="flex flex-wrap items-center gap-4 text-[11px] text-gray-500 pt-2 border-t border-gray-100">
                    <div>Preparer: <span className="font-medium text-gray-700">{resp.preparedByUserName || '—'}</span></div>
                    <div>Reviewer: <span className="font-medium text-gray-700">{resp.reviewedByUserName || '—'}</span></div>
                    <div>Partner: <span className="font-medium text-gray-700">{resp.approvedByUserName || '—'}</span></div>
                    {resp.submittedAt && <div>Filed At: <span className="font-medium text-emerald-700">{new Date(resp.submittedAt).toLocaleString()}</span></div>}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Hearings Tab */}
      {activeTab === 'hearings' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-gray-900">Hearings & Proceedings</h3>
              <p className="text-xs text-gray-500">Virtual VC hearings, personal hearings, and outcome tracking.</p>
            </div>
            <Button
              onClick={() => setIsHearingModalOpen(true)}
              className="flex items-center gap-1 bg-purple-600 hover:bg-purple-700 text-white"
            >
              <Plus className="w-4 h-4" /> Schedule Hearing
            </Button>
          </div>

          {hearings.length === 0 ? (
            <Card className="p-12 text-center bg-white border border-gray-200">
              <Gavel className="w-10 h-10 mx-auto text-gray-300 mb-2" />
              <h4 className="text-sm font-semibold text-gray-800">No Hearings Scheduled</h4>
              <p className="text-xs text-gray-500 mt-1">Record a hearing date if summons or virtual VC link is issued.</p>
            </Card>
          ) : (
            <div className="space-y-3">
              {hearings.map((h) => (
                <Card key={h.id} className="p-4 bg-white border border-gray-200 shadow-sm space-y-2">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 text-xs font-bold rounded bg-purple-100 text-purple-800">
                        {h.hearingMode.replace(/_/g, ' ')}
                      </span>
                      <span className="font-bold text-gray-900 text-sm">
                        {h.hearingDate} {h.hearingTime ? `at ${h.hearingTime}` : ''}
                      </span>
                      <span className={clsx('px-2 py-0.5 text-xs font-semibold rounded', {
                        'bg-blue-100 text-blue-800': h.status === 'SCHEDULED',
                        'bg-amber-100 text-amber-800': h.status === 'ADJOURNED',
                        'bg-emerald-100 text-emerald-800': h.status === 'COMPLETED',
                        'bg-gray-100 text-gray-800': h.status === 'CANCELLED',
                      })}>
                        {h.status}
                      </span>
                    </div>

                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setSelectedHearing(h);
                        setOutcomeForm({
                          status: h.status,
                          proceedingsSummary: h.proceedingsSummary || '',
                          outcomeSummary: h.outcomeSummary || '',
                          nextAction: h.nextAction || '',
                          nextHearingDate: h.nextHearingDate || '',
                        });
                        setIsOutcomeModalOpen(true);
                      }}
                      className="text-xs"
                    >
                      Record Outcome / Summary
                    </Button>
                  </div>

                  <div className="text-xs text-gray-600 space-y-1">
                    {h.authorityName && <div>Authority: <span className="font-medium text-gray-800">{h.authorityName}</span></div>}
                    {h.hearingLink && (
                      <div>
                        Link:{' '}
                        <a href={h.hearingLink} target="_blank" rel="noreferrer" className="text-indigo-600 hover:underline">
                          {h.hearingLink}
                        </a>
                      </div>
                    )}
                    {h.outcomeSummary && (
                      <div className="p-2 bg-gray-50 border rounded text-gray-700 mt-2">
                        <span className="font-bold">Outcome:</span> {h.outcomeSummary}
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Filing & Closure Tab */}
      {activeTab === 'filing' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card className="p-6 bg-white border border-gray-200 space-y-4">
            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider flex items-center gap-1.5">
              <Send className="w-4 h-4 text-emerald-600" /> Portal Filing Details
            </h3>
            {notice.submittedAt ? (
              <div className="space-y-2 text-xs">
                <div>Filing Mode: <span className="font-semibold text-gray-800">{notice.submissionMode}</span></div>
                <div>Acknowledgement Number: <span className="font-mono font-bold text-emerald-700">{notice.portalAcknowledgementNumber || '—'}</span></div>
                <div>Submitted On: <span className="font-semibold text-gray-800">{new Date(notice.submittedAt).toLocaleString()}</span></div>
              </div>
            ) : (
              <div className="text-xs text-gray-500 py-3">
                No portal filing recorded yet. Once response is filed on the portal, click below to record acknowledgement.
              </div>
            )}
            <Button
              size="sm"
              onClick={() => setIsSubmitModalOpen(true)}
              className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs"
            >
              {notice.submittedAt ? 'Update Filing Details' : 'Record Portal Submission'}
            </Button>
          </Card>

          <Card className="p-6 bg-white border border-gray-200 space-y-4">
            <h3 className="text-sm font-bold text-gray-900 uppercase tracking-wider flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-gray-600" /> Notice Closure & Resolution
            </h3>
            {notice.closureDate ? (
              <div className="space-y-2 text-xs">
                <div>Status: <span className="font-bold text-green-700">{notice.status}</span></div>
                <div>Closed On: <span className="font-semibold text-gray-800">{notice.closureDate}</span></div>
                {notice.closureRemarks && <div>Remarks: <p className="bg-gray-50 p-2 rounded text-gray-700 mt-1">{notice.closureRemarks}</p></div>}
              </div>
            ) : (
              <div className="text-xs text-gray-500 py-3">
                Notice case is currently active and open.
              </div>
            )}
            <Button
              size="sm"
              variant="outline"
              onClick={() => setIsCloseModalOpen(true)}
              className="text-xs"
            >
              {notice.closureDate ? 'Update Closure Status' : 'Resolve & Close Case'}
            </Button>
          </Card>
        </div>
      )}

      {/* Tasks Tab */}
      {activeTab === 'tasks' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-gray-900">Linked Tasks ({tasks.length})</h3>
            <Link to={`/tasks?noticeId=${notice.id}`}>
              <Button size="sm" variant="outline" className="text-xs">
                Open in Worklist
              </Button>
            </Link>
          </div>
          {tasks.length === 0 ? (
            <Card className="p-8 text-center text-gray-500 text-xs">No tasks currently linked to this notice.</Card>
          ) : (
            <div className="space-y-2">
              {tasks.map((t) => (
                <Card key={t.id} className="p-3 bg-white border border-gray-200 flex items-center justify-between text-xs">
                  <div>
                    <div className="font-semibold text-gray-900">{t.title}</div>
                    <div className="text-gray-500">Due: {t.dueDate || '—'} | Status: {t.status}</div>
                  </div>
                  <span className="px-2 py-0.5 rounded font-medium bg-gray-100 text-gray-700">{t.priority}</span>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Documents Tab */}
      {activeTab === 'documents' && (
        <div className="space-y-4">
          <h3 className="text-base font-bold text-gray-900">Linked Documents ({documents.length})</h3>
          {documents.length === 0 ? (
            <Card className="p-8 text-center text-gray-500 text-xs">No documents attached yet.</Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {documents.map((d) => (
                <Card key={d.id} className="p-3 bg-white border border-gray-200 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4 text-indigo-600" />
                    <div>
                      <div className="font-semibold text-gray-900">{d.originalFilename || d.filename}</div>
                      <div className="text-gray-400">{d.category || d.fileType || 'Document'}</div>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" onClick={() => window.open(documentApi.downloadUrl(d.id), '_blank')} className="text-xs">
                    View
                  </Button>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Timeline Tab */}
      {activeTab === 'timeline' && (
        <div className="space-y-4">
          <h3 className="text-base font-bold text-gray-900">Immutable Case History & Audit Trail</h3>
          <div className="space-y-3">
            {activities.map((act) => (
              <Card key={act.id} className="p-3 bg-white border border-gray-200 text-xs flex items-start gap-3">
                <History className="w-4 h-4 text-indigo-600 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                  <div className="font-semibold text-gray-900">{act.description}</div>
                  <div className="text-[11px] text-gray-400 mt-0.5">
                    By {act.performerName || 'System'} on {new Date(act.createdAt).toLocaleString()}
                  </div>
                  {act.metadata && (
                    <div className="mt-1 text-[11px] text-gray-600 bg-gray-50 p-1.5 rounded">
                      {act.metadata}
                    </div>
                  )}
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Modals */}
      {/* Draft Response Modal */}
      {isDraftModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Draft Written Response Reply</h3>
              <button onClick={() => setIsDraftModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleCreateDraft} className="p-6 space-y-4 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div>
                <label className="block font-medium text-gray-700 mb-1">Title / Caption *</label>
                <input
                  type="text"
                  value={draftForm.responseTitle}
                  onChange={(e) => setDraftForm({ ...draftForm, responseTitle: e.target.value })}
                  required
                  className="w-full p-2 border rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Facts of the Case</label>
                <textarea
                  rows={4}
                  value={draftForm.factsOfCase || ''}
                  onChange={(e) => setDraftForm({ ...draftForm, factsOfCase: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                  placeholder="Outline client transaction records, filings, returns..."
                />
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Legal Grounds & Statutory Provisions</label>
                <textarea
                  rows={4}
                  value={draftForm.legalGrounds || ''}
                  onChange={(e) => setDraftForm({ ...draftForm, legalGrounds: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                  placeholder="Cite statutory circulars, judicial precedents, ITAT rulings..."
                />
              </div>
              <label className="flex items-center gap-2 cursor-pointer pt-2">
                <input
                  type="checkbox"
                  checked={draftForm.submitForReview ?? false}
                  onChange={(e) => setDraftForm({ ...draftForm, submitForReview: e.target.checked })}
                  className="rounded text-indigo-600"
                />
                <span className="font-medium text-gray-800">Immediately submit draft for internal checker review</span>
              </label>
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsDraftModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-indigo-600 text-white">Save Response Draft</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Review Response Modal */}
      {isReviewModalOpen && selectedResponseForReview && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Maker-Checker Review Action</h3>
              <button onClick={() => setIsReviewModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleReviewAction} className="p-6 space-y-4 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div>
                <label className="block font-medium text-gray-700 mb-1">Review Decision</label>
                <select
                  value={reviewAction}
                  onChange={(e) => setReviewAction(e.target.value as any)}
                  className="w-full p-2 border rounded-lg font-semibold"
                >
                  <option value="APPROVE_REVIEW">Approve Draft (Checker Sign-off)</option>
                  <option value="REQUEST_REVISION">Request Revision (Send back to preparer)</option>
                  <option value="APPROVE_PARTNER">Partner Final Approval (Sign-off for filing)</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">
                  Comments {reviewAction === 'REQUEST_REVISION' ? '(Required)' : '(Optional)'}
                </label>
                <textarea
                  rows={3}
                  value={reviewComments}
                  onChange={(e) => setReviewComments(e.target.value)}
                  required={reviewAction === 'REQUEST_REVISION'}
                  className="w-full p-2 border rounded-lg"
                  placeholder="Enter remarks, requested adjustments, or verification confirmation..."
                />
              </div>
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsReviewModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-indigo-600 text-white">Record Review</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Schedule Hearing Modal */}
      {isHearingModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Schedule Notice Hearing</h3>
              <button onClick={() => setIsHearingModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleScheduleHearing} className="p-6 space-y-3 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-gray-700 mb-1">Hearing Date *</label>
                  <input
                    type="date"
                    value={hearingForm.hearingDate}
                    onChange={(e) => setHearingForm({ ...hearingForm, hearingDate: e.target.value })}
                    required
                    className="w-full p-2 border rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-gray-700 mb-1">Time</label>
                  <input
                    type="text"
                    value={hearingForm.hearingTime || ''}
                    onChange={(e) => setHearingForm({ ...hearingForm, hearingTime: e.target.value })}
                    className="w-full p-2 border rounded-lg"
                  />
                </div>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Hearing Mode</label>
                <select
                  value={hearingForm.hearingMode}
                  onChange={(e) => setHearingForm({ ...hearingForm, hearingMode: e.target.value as any })}
                  className="w-full p-2 border rounded-lg"
                >
                  <option value="VIRTUAL_VC">Virtual VC (Video Conference)</option>
                  <option value="PHYSICAL">Physical Personal Hearing</option>
                  <option value="WRITTEN_SUBMISSION_ONLY">Written Submission Only</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">VC Video Link (If virtual)</label>
                <input
                  type="text"
                  placeholder="https://vc.incometax.gov.in/..."
                  value={hearingForm.hearingLink || ''}
                  onChange={(e) => setHearingForm({ ...hearingForm, hearingLink: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                />
              </div>
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsHearingModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-purple-600 text-white">Save Hearing</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Record Outcome Modal */}
      {isOutcomeModalOpen && selectedHearing && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Record Hearing Outcome</h3>
              <button onClick={() => setIsOutcomeModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleRecordOutcome} className="p-6 space-y-3 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div>
                <label className="block font-medium text-gray-700 mb-1">Hearing Status</label>
                <select
                  value={outcomeForm.status}
                  onChange={(e) => setOutcomeForm({ ...outcomeForm, status: e.target.value as any })}
                  className="w-full p-2 border rounded-lg"
                >
                  <option value="COMPLETED">Completed / Concluded</option>
                  <option value="ADJOURNED">Adjourned to Next Date</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Outcome & Authority Feedback</label>
                <textarea
                  rows={3}
                  value={outcomeForm.outcomeSummary || ''}
                  onChange={(e) => setOutcomeForm({ ...outcomeForm, outcomeSummary: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                  placeholder="Summary of queries raised by the assessing officer..."
                />
              </div>
              {outcomeForm.status === 'ADJOURNED' && (
                <div>
                  <label className="block font-medium text-gray-700 mb-1">Next Hearing Date</label>
                  <input
                    type="date"
                    value={outcomeForm.nextHearingDate || ''}
                    onChange={(e) => setOutcomeForm({ ...outcomeForm, nextHearingDate: e.target.value })}
                    className="w-full p-2 border rounded-lg"
                  />
                </div>
              )}
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsOutcomeModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-purple-600 text-white">Record Outcome</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Record Submission Modal */}
      {isSubmitModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Record Portal Filing Acknowledgement</h3>
              <button onClick={() => setIsSubmitModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleSubmitNotice} className="p-6 space-y-3 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div>
                <label className="block font-medium text-gray-700 mb-1">Filing Portal Mode</label>
                <select
                  value={submitForm.submissionMode}
                  onChange={(e) => setSubmitForm({ ...submitForm, submissionMode: e.target.value as any })}
                  className="w-full p-2 border rounded-lg"
                >
                  <option value="INCOME_TAX_PORTAL">Income Tax e-Filing Portal</option>
                  <option value="GST_PORTAL">GST Common Portal (e-Proceedings)</option>
                  <option value="TRACES_PORTAL">TRACES Portal</option>
                  <option value="PHYSICAL_FILING">Physical Hardcopy Filing</option>
                  <option value="EMAIL_SUBMISSION">Official Email Submission</option>
                  <option value="OTHER">Other Facility</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Portal Acknowledgement / Receipt Number</label>
                <input
                  type="text"
                  placeholder="e.g. ACK-2024-981726"
                  value={submitForm.portalAcknowledgementNumber || ''}
                  onChange={(e) => setSubmitForm({ ...submitForm, portalAcknowledgementNumber: e.target.value })}
                  className="w-full p-2 border rounded-lg font-mono"
                />
              </div>
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsSubmitModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-emerald-600 text-white">Record Submission</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Close Notice Modal */}
      {isCloseModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Resolve or Close Notice Case</h3>
              <button onClick={() => setIsCloseModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleCloseNotice} className="p-6 space-y-3 text-xs">
              {actionError && <div className="p-2 bg-red-50 text-red-700 rounded">{actionError}</div>}
              <div>
                <label className="block font-medium text-gray-700 mb-1">Closure Status</label>
                <select
                  value={closeForm.closureStatus}
                  onChange={(e) => setCloseForm({ ...closeForm, closureStatus: e.target.value as any })}
                  className="w-full p-2 border rounded-lg font-semibold"
                >
                  <option value="RESOLVED">Resolved in Favor / Order Passed</option>
                  <option value="DEMAND_DROPPED">Demand Dropped / Rectified</option>
                  <option value="APPEAL_FILED">Appeal Filed against Order</option>
                  <option value="CLOSED">Closed (Administrative)</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Closure Date</label>
                <input
                  type="date"
                  value={closeForm.closureDate || ''}
                  onChange={(e) => setCloseForm({ ...closeForm, closureDate: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-gray-700 mb-1">Closure Remarks</label>
                <textarea
                  rows={3}
                  value={closeForm.closureRemarks || ''}
                  onChange={(e) => setCloseForm({ ...closeForm, closureRemarks: e.target.value })}
                  className="w-full p-2 border rounded-lg"
                  placeholder="Record order number, reduction amount, or appeal reference..."
                />
              </div>
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsCloseModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-gray-800 text-white">Close Notice Case</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Internal Note Modal */}
      {isNoteModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full flex flex-col">
            <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 text-base">Add Internal Note</h3>
              <button onClick={() => setIsNoteModalOpen(false)} className="text-gray-400 text-lg">&times;</button>
            </div>
            <form onSubmit={handleAddNote} className="p-6 space-y-3 text-xs">
              <textarea
                rows={4}
                value={noteText}
                onChange={(e) => setNoteText(e.target.value)}
                required
                placeholder="Enter internal strategy or case notes..."
                className="w-full p-2.5 border rounded-lg"
              />
              <div className="pt-3 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsNoteModalOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={isActionLoading} className="bg-indigo-600 text-white">Save Note</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
