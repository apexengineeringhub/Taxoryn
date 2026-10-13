import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle2,
  Clock,
  AlertCircle,
  Building,
  User,
  Calendar,
  ShieldAlert,
  Send,
  UserCheck,
  Play,
  PauseCircle,
  FileCheck,
  CheckSquare,
  Plus,
  Edit2,
  FileText,
  HelpCircle,
  ExternalLink,
  ChevronRight,
  ListOrdered,
  Layers,
  AlertTriangle,
  Check,
  X,
  RefreshCw,
  ShieldCheck,
  MessageSquare,
  AlertOctagon,
  CornerDownRight,
} from 'lucide-react';
import { complianceWorkflowApi, employeeApi, reviewApi, complianceQcApi } from '../api/endpoints';
import {
  ComplianceWorkflowDetailDto,
  ComplianceWorkflowStatus,
  ComplianceWorkflowChecklistItemDto,
  Employee,
  Task,
  ReviewRequest,
  ComplianceQcStatusDto,
  ComplianceAuthorizationMethod,
  ComplianceQcReviewFindingDto,
  ComplianceQcReviewCycleDto,
} from '../types';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';
import { useAuth } from '../context/AuthContext';
import { hasPermission } from '../utils/permissionUtils';

export const ComplianceWorkflowDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [detail, setDetail] = useState<ComplianceWorkflowDetailDto | null>(null);
  const [review, setReview] = useState<ReviewRequest | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [isWaitModalOpen, setIsWaitModalOpen] = useState(false);
  const [waitReason, setWaitReason] = useState('');
  const [waitExpectedDate, setWaitExpectedDate] = useState('');

  const [isChangesModalOpen, setIsChangesModalOpen] = useState(false);
  const [changesReason, setChangesReason] = useState('');

  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [approveNotes, setApproveNotes] = useState('');

  const [qcStatus, setQcStatus] = useState<ComplianceQcStatusDto | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [findings, setFindings] = useState<ComplianceQcReviewFindingDto[]>([]);
  const [reviewCycles, setReviewCycles] = useState<ComplianceQcReviewCycleDto[]>([]);

  // Finding Modals State
  const [isAddFindingModalOpen, setIsAddFindingModalOpen] = useState(false);
  const [findingCategory, setFindingCategory] = useState('CALCULATION');
  const [findingSeverity, setFindingSeverity] = useState('BLOCKING');
  const [findingDescription, setFindingDescription] = useState('');
  const [findingGuidance, setFindingGuidance] = useState('');
  const [findingRef, setFindingRef] = useState('');

  const [isRespondModalOpen, setIsRespondModalOpen] = useState(false);
  const [selectedFinding, setSelectedFinding] = useState<ComplianceQcReviewFindingDto | null>(null);
  const [preparerResponseText, setPreparerResponseText] = useState('');

  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [resolutionDecision, setResolutionDecision] = useState<'RESOLVED' | 'DISMISSED'>('RESOLVED');
  const [resolutionNotes, setResolutionNotes] = useState('');

  const [isAuthorizeModalOpen, setIsAuthorizeModalOpen] = useState(false);
  const [authMethod, setAuthMethod] = useState<ComplianceAuthorizationMethod>('CLIENT_CONSENT');
  const [authReference, setAuthReference] = useState('');
  const [authNotes, setAuthNotes] = useState('');
  const [authorizedByName, setAuthorizedByName] = useState('');

  const [isFiledModalOpen, setIsFiledModalOpen] = useState(false);
  const [filedDate, setFiledDate] = useState(new Date().toISOString().split('T')[0]);
  const [filedAckNumber, setFiledAckNumber] = useState('');

  const [isCompleteModalOpen, setIsCompleteModalOpen] = useState(false);
  const [completeAckNumber, setCompleteAckNumber] = useState('');
  const [completeNotes, setCompleteNotes] = useState('');

  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false);
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');
  const [taskPriority, setTaskPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'CRITICAL'>('HIGH');
  const [taskDueDate, setTaskDueDate] = useState('');
  const [taskAssigneeId, setTaskAssigneeId] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchWorkflowDetail = useCallback(async () => {
    if (!id) return;
    try {
      setIsLoading(true);
      setError(null);
      const data = await complianceWorkflowApi.getWorkflowById(id);
      setDetail(data);
      try {
        setReview(await reviewApi.getForResource(id));
      } catch {
        setReview(null);
      }
      try {
        setQcStatus(await complianceQcApi.getQcStatus(id));
      } catch {
        setQcStatus(null);
      }
      try {
        const [fList, cList] = await Promise.all([
          complianceQcApi.getReviewFindings(id),
          complianceQcApi.getReviewCycles(id),
        ]);
        setFindings(fList || []);
        setReviewCycles(cList || []);
      } catch {
        setFindings([]);
        setReviewCycles([]);
      }
    } catch (err: any) {
      console.error('Failed to load workflow details', err);
      setError(err?.response?.data?.message || 'Failed to load workflow details');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchWorkflowDetail();
    employeeApi.getAll({ size: 100 }).then(res => {
      setEmployees(res?.content || []);
    }).catch(err => console.error('Failed to load employees', err));
  }, [fetchWorkflowDetail]);

  const handleToggleChecklistItem = async (item: ComplianceWorkflowChecklistItemDto) => {
    if (!id) return;
    try {
      await complianceWorkflowApi.updateChecklistItem(id, item.id, {
        isCompleted: !item.isCompleted,
        notes: item.notes,
      });
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to update checklist item', err);
    }
  };

  const handleStartWorkflow = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.startWorkflow(id);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to start workflow', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResumeWorkflow = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.resumeClient(id);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to resume workflow', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitReview = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      if (review?.status === 'REJECTED') await reviewApi.resubmit(review.id);
      else await reviewApi.submit(id);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to submit workflow for review', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitWaitModal = async () => {
    if (!id || !waitReason) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.waitClient(id, {
        reason: waitReason,
        expectedResponseDate: waitExpectedDate || undefined,
      });
      setIsWaitModalOpen(false);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to set waiting for client', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitChangesModal = async () => {
    if (!id || !changesReason) return;
    try {
      setIsSubmitting(true);
      if (!review) throw new Error('Review request is unavailable');
      await reviewApi.reject(review.id, changesReason);
      setIsChangesModalOpen(false);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to request changes', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitApproveModal = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      if (!review) throw new Error('Review request is unavailable');
      await reviewApi.approve(review.id, approveNotes || undefined);
      setIsApproveModalOpen(false);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to approve workflow', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitAuthorizeModal = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      await complianceQcApi.authorizeFiling(id, {
        authorizationMethod: authMethod,
        authorizationReference: authReference || undefined,
        authorizationNotes: authNotes || undefined,
        authorizedBy: authorizedByName || undefined,
      });
      setIsAuthorizeModalOpen(false);
      fetchWorkflowDetail();
    } catch (err: any) {
      alert(`Failed to record filing authorization: ${err?.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAddFinding = async () => {
    if (!id || !findingDescription.trim()) return;
    try {
      setIsSubmitting(true);
      await complianceQcApi.addReviewFinding(id, {
        category: findingCategory,
        severity: findingSeverity,
        description: findingDescription.trim(),
        correctiveGuidance: findingGuidance.trim() || undefined,
        sourceReference: findingRef.trim() || undefined,
      });
      setIsAddFindingModalOpen(false);
      setFindingDescription('');
      setFindingGuidance('');
      setFindingRef('');
      fetchWorkflowDetail();
    } catch (err: any) {
      alert(`Failed to record review finding: ${err?.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenRespondModal = (f: ComplianceQcReviewFindingDto) => {
    setSelectedFinding(f);
    setPreparerResponseText(f.preparerResponse || '');
    setIsRespondModalOpen(true);
  };

  const handleSubmitResponse = async () => {
    if (!id || !selectedFinding || !preparerResponseText.trim()) return;
    try {
      setIsSubmitting(true);
      await complianceQcApi.respondToFinding(id, selectedFinding.id, {
        response: preparerResponseText.trim(),
      });
      setIsRespondModalOpen(false);
      setSelectedFinding(null);
      setPreparerResponseText('');
      fetchWorkflowDetail();
    } catch (err: any) {
      alert(`Failed to respond to finding: ${err?.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenResolveModal = (f: ComplianceQcReviewFindingDto) => {
    setSelectedFinding(f);
    setResolutionDecision('RESOLVED');
    setResolutionNotes(f.reviewerNotes || '');
    setIsResolveModalOpen(true);
  };

  const handleSubmitResolution = async () => {
    if (!id || !selectedFinding) return;
    try {
      setIsSubmitting(true);
      await complianceQcApi.resolveFinding(id, selectedFinding.id, {
        decision: resolutionDecision,
        reviewerNotes: resolutionNotes.trim() || undefined,
      });
      setIsResolveModalOpen(false);
      setSelectedFinding(null);
      setResolutionNotes('');
      fetchWorkflowDetail();
    } catch (err: any) {
      alert(`Failed to resolve finding: ${err?.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRunValidation = async () => {
    if (!id) return;
    try {
      setIsValidating(true);
      const res = await complianceQcApi.validateForReview(id);
      const updatedStatus = await complianceQcApi.getQcStatus(id);
      setQcStatus(updatedStatus);
      if (res.valid) {
        alert('All quality control validation checks passed. The return is eligible for review submission.');
      } else {
        alert(`Validation found ${res.blockers.length} blocking issues that must be addressed before review.`);
      }
    } catch (err: any) {
      alert(`Validation error: ${err?.response?.data?.message || err.message}`);
    } finally {
      setIsValidating(false);
    }
  };

  const handleSubmitFiledModal = async () => {
    if (!id || !filedDate) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.markFiled(id, {
        filedDate,
        acknowledgementNumber: filedAckNumber || undefined,
      });
      setIsFiledModalOpen(false);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to record filing', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitCompleteModal = async () => {
    if (!id) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.completeWorkflow(id, {
        acknowledgementNumber: completeAckNumber || undefined,
        notes: completeNotes || undefined,
      });
      setIsCompleteModalOpen(false);
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to complete workflow', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateTask = async () => {
    if (!id || !taskTitle) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.createWorkflowTask(id, {
        title: taskTitle,
        description: taskDescription || undefined,
        priority: taskPriority,
        dueDate: taskDueDate || undefined,
        assignedTo: taskAssigneeId || undefined,
      });
      setIsTaskModalOpen(false);
      setTaskTitle('');
      setTaskDescription('');
      fetchWorkflowDetail();
    } catch (err) {
      console.error('Failed to create task', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="py-24 text-center text-slate-500">
        <RefreshCw className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
        <p className="text-base font-medium">Loading execution workflow details...</p>
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="p-8 text-center bg-white rounded-xl border border-red-200">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-3" />
        <h2 className="text-xl font-bold text-slate-900">Unable to load workflow</h2>
        <p className="text-sm text-slate-600 mt-1">{error || 'Workflow not found or access denied.'}</p>
        <Link to="/compliance/workbench" className="mt-4 inline-block">
          <Button variant="outline">Back to Workbench</Button>
        </Link>
      </div>
    );
  }

  const { workflow, checklistItems, linkedTasks } = detail;
  const canSubmitReview = hasPermission(user, ['REVIEW_SUBMIT']);
  const canApproveReview = !!review && review.assignedReviewerId === user?.id && hasPermission(user, ['REVIEW_APPROVE']);
  const canRejectReview = !!review && review.assignedReviewerId === user?.id && hasPermission(user, ['REVIEW_REJECT']);

  const STEPS: { status: ComplianceWorkflowStatus; label: string }[] = [
    { status: 'READY', label: '1. Ready' },
    { status: 'IN_PROGRESS', label: '2. Preparation' },
    { status: 'UNDER_REVIEW', label: '3. Review' },
    { status: 'READY_FOR_FILING', label: '4. Ready Filing' },
    { status: 'FILED', label: '5. Filed' },
    { status: 'COMPLETED', label: '6. Completed' },
  ];

  const getStepIndex = (st: ComplianceWorkflowStatus) => {
    switch (st) {
      case 'CREATED':
      case 'READY': return 0;
      case 'IN_PROGRESS':
      case 'WAITING_FOR_CLIENT':
      case 'CHANGES_REQUIRED': return 1;
      case 'UNDER_REVIEW': return 2;
      case 'READY_FOR_FILING': return 3;
      case 'FILED':
      case 'ACKNOWLEDGEMENT_PENDING': return 4;
      case 'COMPLETED': return 5;
      default: return 0;
    }
  };

  const currentStepIdx = getStepIndex(workflow.workflowStatus);

  return (
    <div className="space-y-6 pb-16">
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <div>
          <Link to="/compliance/workbench" className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-800 mb-2">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Compliance Workbench
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-900">{workflow.obligationTitle}</h1>
            <span className={clsx(
              'px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider',
              workflow.workflowStatus === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
              workflow.workflowStatus === 'FILED' ? 'bg-indigo-100 text-indigo-800 border border-indigo-300' :
              workflow.workflowStatus === 'READY_FOR_FILING' ? 'bg-teal-100 text-teal-800 border border-teal-300' :
              workflow.workflowStatus === 'UNDER_REVIEW' ? 'bg-purple-100 text-purple-800 border border-purple-300' :
              workflow.workflowStatus === 'WAITING_FOR_CLIENT' || workflow.waitingForClient ? 'bg-amber-100 text-amber-800 border border-amber-300' :
              'bg-blue-100 text-blue-800 border border-blue-300'
            )}>
              {workflow.waitingForClient ? 'Waiting on Client' : workflow.workflowStatus.replace(/_/g, ' ')}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-4 mt-2 text-xs text-slate-600">
            <div className="flex items-center gap-1 font-semibold text-slate-800">
              <Building className="w-3.5 h-3.5 text-slate-400" />
              {workflow.clientName}
            </div>
            {workflow.pan && <span>PAN: <strong>{workflow.pan}</strong></span>}
            {workflow.gstin && <span>GSTIN: <strong>{workflow.gstin}</strong></span>}
            <span>Period: <strong>{workflow.periodLabel}</strong></span>
          </div>
        </div>

        {/* Workflow Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          {workflow.workflowStatus === 'READY' || workflow.workflowStatus === 'CREATED' ? (
            <Button variant="primary" onClick={handleStartWorkflow} disabled={isSubmitting} className="flex items-center gap-1.5">
              <Play className="w-4 h-4" /> Start Preparation
            </Button>
          ) : null}

          {workflow.waitingForClient ? (
            <Button variant="outline" onClick={handleResumeWorkflow} disabled={isSubmitting} className="text-amber-700 border-amber-300 bg-amber-50 hover:bg-amber-100 flex items-center gap-1.5">
              <Play className="w-4 h-4" /> Resume Execution
            </Button>
          ) : (workflow.workflowStatus === 'IN_PROGRESS' || workflow.workflowStatus === 'CHANGES_REQUIRED') ? (
            <>
              <Button variant="outline" onClick={() => setIsWaitModalOpen(true)} className="text-yellow-700 border-yellow-300 hover:bg-yellow-50 flex items-center gap-1.5">
                <PauseCircle className="w-4 h-4" /> Wait Client
              </Button>
              {canSubmitReview && (review?.status === 'REJECTED' || !review || review.status === 'CANCELLED' || review.status === 'APPROVED') && (
                <Button variant="primary" onClick={handleSubmitReview} disabled={isSubmitting} className="flex items-center gap-1.5 bg-purple-600 hover:bg-purple-700 text-white">
                  <Send className="w-4 h-4" /> {review?.status === 'REJECTED' ? 'Resubmit for Review' : 'Submit for Review'}
                </Button>
              )}
            </>
          ) : null}

          {workflow.workflowStatus === 'UNDER_REVIEW' && review?.status === 'UNDER_REVIEW' && (canRejectReview || canApproveReview) ? (
            <>
              {canRejectReview && <Button variant="outline" onClick={() => setIsChangesModalOpen(true)} className="text-rose-700 border-rose-300 hover:bg-rose-50 flex items-center gap-1.5">
                <X className="w-4 h-4" /> Request Changes
              </Button>}
              {canApproveReview && <Button variant="primary" onClick={() => setIsApproveModalOpen(true)} className="bg-teal-600 hover:bg-teal-700 text-white flex items-center gap-1.5">
                <Check className="w-4 h-4" /> Approve & Ready Filing
              </Button>}
            </>
          ) : null}

          {workflow.workflowStatus === 'READY_FOR_FILING' ? (
            <>
              {!workflow.isAuthorized ? (
                <Button variant="primary" onClick={() => setIsAuthorizeModalOpen(true)} className="bg-purple-600 hover:bg-purple-700 text-white flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4" /> Authorize Filing
                </Button>
              ) : null}
              <Button
                variant="primary"
                onClick={() => setIsFiledModalOpen(true)}
                disabled={!workflow.isAuthorized}
                title={!workflow.isAuthorized ? 'Filing authorization is required prior to statutory submission' : undefined}
                className={clsx('flex items-center gap-1.5', workflow.isAuthorized ? 'bg-teal-600 hover:bg-teal-700 text-white' : 'bg-slate-300 text-slate-500 cursor-not-allowed')}
              >
                <FileCheck className="w-4 h-4" /> Record Government Filing
              </Button>
            </>
          ) : null}

          {workflow.workflowStatus === 'FILED' || workflow.workflowStatus === 'ACKNOWLEDGEMENT_PENDING' ? (
            <Button variant="primary" onClick={() => setIsCompleteModalOpen(true)} className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4" /> Finalize & Complete
            </Button>
          ) : null}
        </div>
      </div>

      {/* Filing Authorization Status Banner */}
      {workflow.isAuthorized && (
        <div className="p-3 bg-emerald-50 border border-emerald-300 rounded-xl flex items-center justify-between text-xs text-emerald-900 shadow-sm">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>
              <strong>Filing Authorized:</strong> Authorized via <strong>{workflow.authorizationMethod || 'CLIENT_CONSENT'}</strong>
              {workflow.authorizedBy ? ` by ${workflow.authorizedBy}` : ''}
              {workflow.authorizationReference ? ` (Ref: ${workflow.authorizationReference})` : ''}
            </span>
          </div>
          {workflow.authorizedAt && <span className="text-slate-500">{new Date(workflow.authorizedAt).toLocaleDateString()}</span>}
        </div>
      )}
      {!workflow.isAuthorized && workflow.workflowStatus === 'READY_FOR_FILING' && (
        <div className="p-3 bg-purple-50 border border-purple-300 rounded-xl flex items-center justify-between text-xs text-purple-900 shadow-sm">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-purple-600 shrink-0" />
            <span>
              <strong>Pending Authorization:</strong> Computation review is approved, but statutory filing has not been authorized. Record client or practitioner authorization before submitting to portal.
            </span>
          </div>
          <Button variant="outline" size="sm" onClick={() => setIsAuthorizeModalOpen(true)} className="text-purple-700 border-purple-300 bg-white hover:bg-purple-50">
            Authorize Now
          </Button>
        </div>
      )}

      {/* Execution Stepper */}
      <Card className="p-4 border-slate-200">
        <div className="flex items-center justify-between">
          {STEPS.map((step, idx) => {
            const isCompleted = idx < currentStepIdx || workflow.workflowStatus === 'COMPLETED';
            const isCurrent = idx === currentStepIdx && workflow.workflowStatus !== 'COMPLETED';
            return (
              <div key={step.status} className="flex-1 flex flex-col items-center relative text-center">
                <div
                  className={clsx(
                    'w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs z-10 transition-all',
                    isCompleted ? 'bg-emerald-600 text-white shadow' :
                    isCurrent ? 'bg-indigo-600 text-white ring-4 ring-indigo-100 shadow' :
                    'bg-slate-100 text-slate-400 border border-slate-300'
                  )}
                >
                  {isCompleted ? <Check className="w-4 h-4" /> : idx + 1}
                </div>
                <span className={clsx(
                  'text-xs font-semibold mt-1.5',
                  isCompleted ? 'text-emerald-700' :
                  isCurrent ? 'text-indigo-700 font-bold' :
                  'text-slate-400'
                )}>
                  {step.label}
                </span>
                {idx < STEPS.length - 1 && (
                  <div
                    className={clsx(
                      'absolute top-4 left-1/2 w-full h-0.5 -z-0',
                      idx < currentStepIdx ? 'bg-emerald-500' : 'bg-slate-200'
                    )}
                  />
                )}
              </div>
            );
          })}
        </div>
      </Card>

      {/* Waiting on Client Banner */}
      {workflow.waitingForClient && (
        <div className="p-4 bg-amber-50 border border-amber-300 rounded-xl flex items-start justify-between gap-4 text-amber-900 shadow-sm">
          <div className="flex items-start gap-3">
            <Clock className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-bold text-base text-amber-950">Workflow Paused — Waiting on Client Action</h3>
              <p className="text-sm mt-1 text-amber-900 font-medium">
                {workflow.waitingReason || 'Awaiting client documents or approval confirmation.'}
              </p>
              {workflow.expectedResponseDate && (
                <p className="text-xs text-amber-700 mt-1">
                  Expected Client Response Date: <strong>{workflow.expectedResponseDate}</strong>
                </p>
              )}
            </div>
          </div>
          <Button variant="primary" onClick={handleResumeWorkflow} className="bg-amber-600 hover:bg-amber-700 text-white text-xs whitespace-nowrap">
            Client Responded — Resume Execution
          </Button>
        </div>
      )}

      {/* Changes Requested Banner */}
      {workflow.workflowStatus === 'CHANGES_REQUIRED' && workflow.changesRequestedReason && (
        <div className="p-4 bg-rose-50 border border-rose-300 rounded-xl flex items-start gap-3 text-rose-900 shadow-sm">
          <AlertTriangle className="w-6 h-6 text-rose-600 shrink-0 mt-0.5" />
          <div>
            <h3 className="font-bold text-base text-rose-950">Revisions Requested by Reviewer</h3>
            <p className="text-sm mt-1 text-rose-900 font-medium">{workflow.changesRequestedReason}</p>
          </div>
        </div>
      )}

      {review && (
        <Card className="p-5 border-slate-200">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="font-bold text-slate-900">Review & approval</h2>
              <p className="text-sm text-slate-600 mt-1">Status: <strong>{review.status.replace(/_/g, ' ')}</strong> · Assigned reviewer: {workflow.reviewerEmployeeName || 'Assigned reviewer'}</p>
            </div>
            {review.rejectionReason && <p className="text-sm text-rose-700 max-w-xl">Feedback: {review.rejectionReason}</p>}
          </div>
          <ol className="mt-4 space-y-2 border-l-2 border-slate-200 pl-4">
            {review.history.map((action, index) => (
              <li key={`${action.action}-${action.occurredAt}-${index}`} className="text-sm text-slate-700">
                <span className="font-semibold">{action.action.replace(/_/g, ' ')}</span>
                <span className="text-slate-500"> · {new Date(action.occurredAt).toLocaleString()}</span>
                {action.comment && <p className="text-slate-600 mt-0.5">{action.comment}</p>}
              </li>
            ))}
          </ol>
        </Card>
      )}

      {/* Main Grid: 2 Columns (Checklist & Details) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: 9-Step Operational Readiness Checklist */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="p-6 border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <CheckSquare className="w-5 h-5 text-indigo-600" />
                  Readiness & Data Collection Checklist (9 Checkpoints)
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Complete operational milestones to prepare, review, and file the statutory return.
                </p>
              </div>
              <div className="text-right">
                <span className="text-xs font-bold text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded-full border border-indigo-200">
                  {workflow.completedChecklistSteps} of {workflow.totalChecklistSteps} Completed ({workflow.progressPercentage}%)
                </span>
              </div>
            </div>

            <div className="divide-y divide-slate-100 mt-2">
              {checklistItems.map(item => (
                <div key={item.id} className="py-3.5 flex items-start gap-3 hover:bg-slate-50/75 p-2 rounded-lg transition-colors">
                  <button
                    type="button"
                    onClick={() => handleToggleChecklistItem(item)}
                    className={clsx(
                      'mt-0.5 w-5 h-5 rounded border flex items-center justify-center transition-colors',
                      item.isCompleted ? 'bg-emerald-600 border-emerald-600 text-white' : 'border-slate-300 bg-white hover:border-indigo-500'
                    )}
                  >
                    {item.isCompleted && <Check className="w-3.5 h-3.5" />}
                  </button>

                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <h4 className={clsx(
                        'text-sm font-semibold',
                        item.isCompleted ? 'text-slate-500 line-through' : 'text-slate-900'
                      )}>
                        {item.sequenceOrder}. {item.title}
                      </h4>
                      {item.isRequired && <span className="text-[10px] text-slate-400 font-medium uppercase">Required</span>}
                    </div>
                    {item.description && (
                      <p className="text-xs text-slate-500 mt-0.5">{item.description}</p>
                    )}

                    {item.isCompleted && (
                      <div className="text-[11px] text-emerald-700 font-medium mt-1 flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        Completed by {item.completedByName || 'Practitioner'} {item.completedAt ? `on ${new Date(item.completedAt).toLocaleDateString()}` : ''}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* Quality Control Validation & Findings (Phase 31.2) */}
          <Card className="p-6 border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-indigo-600" />
                  Pre-Review Quality Control Validation
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Deterministic readiness evaluation across eligibility, reviewer assignment, separation of duties, and profile requirements.
                </p>
              </div>
              <div className="flex items-center gap-2">
                {qcStatus?.validationSummary && (
                  <span className={clsx(
                    'text-xs font-bold px-2.5 py-1 rounded-full border',
                    qcStatus.validationSummary.valid
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-rose-50 text-rose-700 border-rose-200'
                  )}>
                    {qcStatus.validationSummary.valid ? 'VALIDATION PASSED' : `${qcStatus.validationSummary.blockers.length} BLOCKER(S)`}
                  </span>
                )}
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleRunValidation}
                  disabled={isValidating}
                  className="flex items-center gap-1.5 text-xs text-indigo-700 border-indigo-200 hover:bg-indigo-50"
                >
                  <RefreshCw className={clsx('w-3.5 h-3.5', isValidating && 'animate-spin')} />
                  {isValidating ? 'Validating...' : 'Run Validation'}
                </Button>
              </div>
            </div>

            {/* Validation Findings List */}
            {qcStatus?.validationSummary?.checks && qcStatus.validationSummary.checks.length > 0 ? (
              <div className="divide-y divide-slate-100 mt-2">
                {qcStatus.validationSummary.checks.map(check => (
                  <div key={check.checkKey} className="py-3 flex items-start justify-between gap-4">
                    <div className="flex items-start gap-2.5">
                      <div className="mt-0.5 shrink-0">
                        {check.status === 'PASS' ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        ) : check.status === 'NOT_APPLICABLE' ? (
                          <HelpCircle className="w-4 h-4 text-slate-400" />
                        ) : check.status === 'NOT_EVALUATED' ? (
                          <AlertCircle className="w-4 h-4 text-amber-500" />
                        ) : (
                          <AlertTriangle className={clsx('w-4 h-4', check.severity === 'BLOCKING' ? 'text-rose-600' : 'text-amber-500')} />
                        )}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-sm font-semibold text-slate-900">{check.checkName}</h4>
                          <span className={clsx(
                            'text-[10px] font-bold px-1.5 py-0.2 rounded',
                            check.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' :
                            check.status === 'NOT_APPLICABLE' ? 'bg-slate-100 text-slate-600' :
                            check.status === 'NOT_EVALUATED' ? 'bg-amber-100 text-amber-800' :
                            check.severity === 'BLOCKING' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                          )}>
                            {check.status}
                          </span>
                          {check.required && (
                            <span className="text-[10px] font-bold text-rose-600 uppercase">Blocking</span>
                          )}
                        </div>
                        <p className="text-xs text-slate-600 mt-0.5">{check.message || check.description}</p>
                        {check.correctiveHint && check.status !== 'PASS' && (
                          <p className="text-xs text-indigo-700 font-medium mt-1">
                            <strong>Correction Hint:</strong> {check.correctiveHint}
                          </p>
                        )}
                      </div>
                    </div>
                    {check.sourceReference && (
                      <span className="text-[11px] text-slate-400 font-mono shrink-0">
                        {check.sourceReference.length > 12 ? `${check.sourceReference.substring(0, 10)}...` : check.sourceReference}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-6 text-center text-slate-400 text-xs">
                No quality validation run yet. Click "Run Validation" to evaluate preparation readiness.
              </div>
            )}

            {qcStatus?.validationSummary?.preparationFingerprint && (
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                <span>Rule Version: 1.0</span>
                <span className="font-mono">Fingerprint: {qcStatus.validationSummary.preparationFingerprint}</span>
              </div>
            )}
          </Card>

          {/* Review Findings & Correction Workflow (Phase 31.3) */}
          <Card className="p-6 border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-indigo-600" />
                  Review Findings & Correction Log
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 ml-1">
                    Cycle {workflow.reviewCycle || 1}
                  </span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Track structured reviewer findings, preparer responses, and checker resolutions across review cycles.
                </p>
              </div>
              <div className="flex items-center gap-2">
                {workflow.workflowStatus === 'UNDER_REVIEW' && (canRejectReview || canApproveReview) && (
                  <Button
                    size="sm"
                    variant="primary"
                    onClick={() => setIsAddFindingModalOpen(true)}
                    className="flex items-center gap-1.5 text-xs bg-indigo-600 hover:bg-indigo-700 text-white"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Record Finding
                  </Button>
                )}
              </div>
            </div>

            {/* Findings List */}
            {findings.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                No review findings logged. All checks currently clear for this review cycle.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 mt-2 space-y-3">
                {findings.map(f => (
                  <div key={f.id} className="pt-3 pb-2">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-start gap-2.5">
                        <div className="mt-0.5 shrink-0">
                          {f.status === 'RESOLVED' ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                          ) : f.status === 'DISMISSED' ? (
                            <Check className="w-4 h-4 text-slate-400" />
                          ) : f.status === 'CORRECTED' ? (
                            <Clock className="w-4 h-4 text-blue-600" />
                          ) : (
                            <AlertOctagon className={clsx('w-4 h-4', f.severity === 'BLOCKING' ? 'text-rose-600' : 'text-amber-500')} />
                          )}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
                              {f.category}
                            </span>
                            <span className={clsx(
                              'text-[10px] font-bold px-1.5 py-0.2 rounded uppercase',
                              f.severity === 'BLOCKING' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                            )}>
                              {f.severity}
                            </span>
                            <span className={clsx(
                              'text-[10px] font-bold px-1.5 py-0.2 rounded uppercase',
                              f.status === 'RESOLVED' ? 'bg-emerald-100 text-emerald-800' :
                              f.status === 'CORRECTED' ? 'bg-blue-100 text-blue-800' :
                              f.status === 'DISMISSED' ? 'bg-slate-100 text-slate-600' :
                              'bg-amber-100 text-amber-800'
                            )}>
                              {f.status}
                            </span>
                            <span className="text-[11px] text-slate-400">
                              Cycle {f.reviewCycle} · {new Date(f.createdAt).toLocaleDateString()}
                            </span>
                          </div>

                          <p className="text-sm text-slate-900 font-medium mt-1.5">{f.description}</p>
                          {f.correctiveGuidance && (
                            <p className="text-xs text-indigo-700 font-medium mt-1">
                              <strong>Corrective Guidance:</strong> {f.correctiveGuidance}
                            </p>
                          )}
                          {f.sourceReference && (
                            <p className="text-[11px] text-slate-500 font-mono mt-0.5">
                              Ref: {f.sourceReference}
                            </p>
                          )}

                          {/* Preparer Response Thread */}
                          {f.preparerResponse && (
                            <div className="mt-2.5 p-2.5 bg-blue-50/70 border border-blue-200 rounded-lg text-xs text-blue-950 flex items-start gap-2">
                              <CornerDownRight className="w-3.5 h-3.5 text-blue-600 shrink-0 mt-0.5" />
                              <div>
                                <span className="font-semibold text-blue-900">Preparer Response:</span> {f.preparerResponse}
                                {f.respondedAt && (
                                  <span className="text-[10px] text-blue-600 block mt-0.5">
                                    Responded on {new Date(f.respondedAt).toLocaleString()}
                                  </span>
                                )}
                              </div>
                            </div>
                          )}

                          {/* Reviewer Resolution Thread */}
                          {(f.status === 'RESOLVED' || f.status === 'DISMISSED') && (
                            <div className="mt-2 p-2.5 bg-emerald-50/70 border border-emerald-200 rounded-lg text-xs text-emerald-950 flex items-start gap-2">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />
                              <div>
                                <span className="font-semibold text-emerald-900">Decision ({f.resolutionDecision}):</span> {f.reviewerNotes || 'Marked resolved by reviewer.'}
                                {f.resolvedAt && (
                                  <span className="text-[10px] text-emerald-600 block mt-0.5">
                                    Resolved on {new Date(f.resolvedAt).toLocaleString()}
                                  </span>
                                )}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Actions */}
                      <div className="flex items-center gap-2 shrink-0">
                        {/* Preparer Action: Respond when CHANGES_REQUIRED / IN_PROGRESS and OPEN */}
                        {f.status === 'OPEN' && (workflow.workflowStatus === 'CHANGES_REQUIRED' || workflow.workflowStatus === 'IN_PROGRESS') && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenRespondModal(f)}
                            className="text-xs text-blue-700 border-blue-200 hover:bg-blue-50"
                          >
                            Respond
                          </Button>
                        )}

                        {/* Reviewer Action: Resolve / Dismiss when UNDER_REVIEW and (OPEN or CORRECTED) */}
                        {workflow.workflowStatus === 'UNDER_REVIEW' && (f.status === 'OPEN' || f.status === 'CORRECTED') && (canRejectReview || canApproveReview) && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenResolveModal(f)}
                            className="text-xs text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                          >
                            Resolve / Dismiss
                          </Button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Review Cycles Audit Log */}
            {reviewCycles.length > 1 && (
              <div className="mt-4 pt-4 border-t border-slate-100">
                <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">Review Cycle Audit</h4>
                <div className="flex flex-wrap gap-2">
                  {reviewCycles.map(c => (
                    <div key={c.cycleNumber} className="text-xs px-2.5 py-1 rounded bg-slate-50 border border-slate-200 flex items-center gap-1.5">
                      <span className="font-semibold text-slate-800">Cycle {c.cycleNumber}:</span>
                      <span className="text-slate-500">{c.totalFindings} findings</span>
                      {c.unresolvedFindings > 0 ? (
                        <span className="text-rose-600 font-medium">({c.unresolvedFindings} open)</span>
                      ) : (
                        <span className="text-emerald-600 font-medium">(all resolved)</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>

          {/* Operational Linked Subtasks */}
          <Card className="p-6 border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-200">
              <div>
                <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <ListOrdered className="w-5 h-5 text-indigo-600" />
                  Linked Tasks & Assignments
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Discrete subtasks assigned to staff without exposing unpermitted client portfolio records.
                </p>
              </div>
              <Button size="sm" variant="outline" onClick={() => setIsTaskModalOpen(true)} className="flex items-center gap-1 text-xs">
                <Plus className="w-3.5 h-3.5" /> Add Subtask
              </Button>
            </div>

            <div className="mt-3">
              {linkedTasks.length === 0 ? (
                <div className="py-8 text-center text-slate-400 text-xs">
                  No individual tasks linked. Click "Add Subtask" to assign specific working papers or checks.
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {linkedTasks.map(t => (
                    <div key={t.id} className="py-3 flex items-center justify-between">
                      <div>
                        <h4 className="text-sm font-semibold text-slate-800">{t.title}</h4>
                        {t.description && <p className="text-xs text-slate-500">{t.description}</p>}
                        <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                          <span>Status: <strong>{t.status}</strong></span>
                          <span>Priority: <strong>{t.priority}</strong></span>
                          {t.dueDate && <span>Due: <strong>{t.dueDate}</strong></span>}
                        </div>
                      </div>
                      <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                        {t.status}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card>
        </div>

        {/* Right 1 Col: Context, Dates & Maker-Checker Info */}
        <div className="space-y-6">
          {/* Dual Date Card */}
          <Card className="p-5 border-slate-200">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-indigo-600" />
              Deadlines & Target Timelines
            </h3>

            <div className="space-y-3 text-sm">
              <div className="p-3 bg-indigo-50/70 border border-indigo-200 rounded-lg">
                <p className="text-xs text-indigo-700 font-semibold">Internal Practice Target Date</p>
                <p className="text-base font-bold text-indigo-950 mt-0.5">{workflow.targetDate || 'Not specified'}</p>
                <p className="text-xs text-indigo-600 mt-1">{workflow.targetDaysRemaining} days remaining</p>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
                <p className="text-xs text-slate-500 font-semibold">Statutory Government Deadline</p>
                <p className="text-base font-bold text-slate-800 mt-0.5">{workflow.statutoryDueDate}</p>
                <p className="text-xs text-slate-500 mt-1">{workflow.daysRemaining} days remaining</p>
                {workflow.isOverdue && (
                  <span className="inline-block mt-1 text-xs px-2 py-0.5 bg-red-100 text-red-700 font-bold rounded">
                    STATUTORY OVERDUE
                  </span>
                )}
              </div>
            </div>
          </Card>

          {/* Maker / Checker Review Context */}
          <Card className="p-5 border-slate-200">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <UserCheck className="w-4 h-4 text-indigo-600" />
              Maker-Checker Assignments
            </h3>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Preparer (Maker):</span>
                <span className="font-semibold text-slate-800">{workflow.assignedEmployeeName || 'Unassigned'}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500">Reviewer (Checker):</span>
                <span className="font-semibold text-slate-800">{workflow.reviewerEmployeeName || 'Unassigned'}</span>
              </div>
              {detail.approvedBy && (
                <div className="flex justify-between py-1.5 border-b border-slate-100">
                  <span className="text-slate-500">Approved By:</span>
                  <span className="font-semibold text-emerald-700">{detail.approvedBy}</span>
                </div>
              )}
              {detail.reviewNotes && (
                <div className="p-2.5 bg-slate-50 rounded border border-slate-200">
                  <span className="font-semibold text-slate-700 block mb-1">Review Remarks:</span>
                  <p className="text-slate-600">{detail.reviewNotes}</p>
                </div>
              )}
            </div>
          </Card>

          {/* Government Filing Information */}
          <Card className="p-5 border-slate-200">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <FileCheck className="w-4 h-4 text-indigo-600" />
              Government Filing Metadata
            </h3>

            <div className="space-y-2.5 text-xs">
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Filing Date:</span>
                <span className="font-semibold text-slate-800">{workflow.filedDate || 'Not filed yet'}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Acknowledgement / ARN:</span>
                <span className="font-semibold text-indigo-700">{workflow.acknowledgementNumber || 'Pending'}</span>
              </div>
              {detail.filedBy && (
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span className="text-slate-500">Filed By:</span>
                  <span className="font-semibold text-slate-800">{detail.filedBy}</span>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>

      {/* Modal: Waiting on Client */}
      <Modal
        isOpen={isWaitModalOpen}
        onClose={() => setIsWaitModalOpen(false)}
        title="Block Workflow — Waiting for Client Action"
      >
        <div className="space-y-4 text-sm">
          <div>
            <label className="block font-medium text-slate-700 mb-1">Reason for Blocking / Missing Inputs *</label>
            <textarea
              rows={3}
              placeholder="e.g. Awaiting bank statement Excel and sales invoices for July 2026"
              value={waitReason}
              onChange={e => setWaitReason(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Expected Response Date (Optional)</label>
            <input
              type="date"
              value={waitExpectedDate}
              onChange={e => setWaitExpectedDate(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsWaitModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={!waitReason || isSubmitting} onClick={handleSubmitWaitModal}>
              {isSubmitting ? 'Saving...' : 'Set Waiting State'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Request Changes */}
      <Modal
        isOpen={isChangesModalOpen}
        onClose={() => setIsChangesModalOpen(false)}
        title="Request Revisions / Corrections from Preparer"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Enter the corrections or missing calculations needed before return filing approval.
          </p>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Correction Details *</label>
            <textarea
              rows={4}
              placeholder="e.g. Reverse charge ITC was missed in Table 3.1. Please recalculate with 18% on legal fees."
              value={changesReason}
              onChange={e => setChangesReason(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsChangesModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={!changesReason || isSubmitting} onClick={handleSubmitChangesModal}>
              {isSubmitting ? 'Submitting...' : 'Send Revision Request'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Approve Workflow */}
      <Modal
        isOpen={isApproveModalOpen}
        onClose={() => setIsApproveModalOpen(false)}
        title="Approve Compliance Workflow for Government Filing"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Confirming approval marks the workflow as <strong>READY_FOR_FILING</strong> and authorizes practitioner portal submission.
          </p>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Review Approval Notes (Optional)</label>
            <textarea
              rows={3}
              placeholder="e.g. Working papers and challans verified against ledger."
              value={approveNotes}
              onChange={e => setApproveNotes(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsApproveModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={isSubmitting} onClick={handleSubmitApproveModal}>
              {isSubmitting ? 'Approving...' : 'Approve for Filing'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Authorize Filing (Quality Control Stage) */}
      <Modal
        isOpen={isAuthorizeModalOpen}
        onClose={() => setIsAuthorizeModalOpen(false)}
        title="Authorize Statutory Filing"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Record client confirmation or authorized signatory sign-off permitting official return upload to statutory portal.
          </p>
          <div>
            <label className="block font-medium text-slate-700 mb-1">
              Authorization Method <span className="text-rose-500">*</span>
            </label>
            <select
              value={authMethod}
              onChange={e => setAuthMethod(e.target.value as ComplianceAuthorizationMethod)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="CLIENT_CONSENT">Client Consent (Written / Email / Portal Sign-off)</option>
              <option value="DSC_SIGN_OFF">Digital Signature Certificate (DSC Sign-off)</option>
              <option value="PRACTITIONER_AUTHORIZATION">Practitioner Power-of-Attorney / Authorized Signatory</option>
              <option value="PORTAL_CREDENTIALS">Statutory Portal OTP / EVC Verification</option>
            </select>
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Authorizing Person / Signatory Name</label>
            <input
              type="text"
              placeholder="e.g. Rahul Sharma (Managing Director)"
              value={authorizedByName}
              onChange={e => setAuthorizedByName(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Authorization Reference / Document #</label>
            <input
              type="text"
              placeholder="e.g. EMAIL-CONF-2026-10-18 or DSC-TOKEN-44"
              value={authReference}
              onChange={e => setAuthReference(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Authorization Notes (Optional)</label>
            <textarea
              rows={2}
              placeholder="e.g. Client confirmed liability and verified input tax credit reconciliation."
              value={authNotes}
              onChange={e => setAuthNotes(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsAuthorizeModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={isSubmitting} onClick={handleSubmitAuthorizeModal} className="bg-purple-600 hover:bg-purple-700 text-white">
              {isSubmitting ? 'Recording...' : 'Record Filing Authorization'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Mark Filed */}
      <Modal
        isOpen={isFiledModalOpen}
        onClose={() => setIsFiledModalOpen(false)}
        title="Record Government Portal Filing"
      >
        <div className="space-y-4 text-sm">
          <div>
            <label className="block font-medium text-slate-700 mb-1">Filing Date *</label>
            <input
              type="date"
              value={filedDate}
              onChange={e => setFiledDate(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Acknowledgement Number / ARN</label>
            <input
              type="text"
              placeholder="e.g. AA2708260192837"
              value={filedAckNumber}
              onChange={e => setFiledAckNumber(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsFiledModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={!filedDate || isSubmitting} onClick={handleSubmitFiledModal}>
              {isSubmitting ? 'Recording...' : 'Confirm Filing'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Complete Workflow */}
      <Modal
        isOpen={isCompleteModalOpen}
        onClose={() => setIsCompleteModalOpen(false)}
        title="Complete Compliance Execution Workflow"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Finalize this recurring obligation and mark all steps completed.
          </p>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Acknowledgement Number / ARN</label>
            <input
              type="text"
              placeholder="e.g. AA2708260192837"
              value={completeAckNumber}
              onChange={e => setCompleteAckNumber(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Closing Remarks (Optional)</label>
            <textarea
              rows={2}
              placeholder="e.g. Return filed and deliverable emailed to client."
              value={completeNotes}
              onChange={e => setCompleteNotes(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsCompleteModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={isSubmitting} onClick={handleSubmitCompleteModal}>
              {isSubmitting ? 'Finalizing...' : 'Finalize Workflow'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Create Subtask */}
      <Modal
        isOpen={isTaskModalOpen}
        onClose={() => setIsTaskModalOpen(false)}
        title="Create Operational Subtask for Workflow"
      >
        <div className="space-y-4 text-sm">
          <div>
            <label className="block font-medium text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              placeholder="e.g. Cross-check TDS 26AS with customer sales ledgers"
              value={taskTitle}
              onChange={e => setTaskTitle(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Description (Optional)</label>
            <textarea
              rows={2}
              value={taskDescription}
              onChange={e => setTaskDescription(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-medium text-slate-700 mb-1">Assignee</label>
              <select
                value={taskAssigneeId}
                onChange={e => setTaskAssigneeId(e.target.value)}
                className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="">-- Assign Staff --</option>
                {employees.map(emp => (
                  <option key={emp.id} value={emp.id}>{emp.fullName || emp.email}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block font-medium text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={taskDueDate}
                onChange={e => setTaskDueDate(e.target.value)}
                className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsTaskModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={!taskTitle || isSubmitting} onClick={handleCreateTask}>
              {isSubmitting ? 'Creating...' : 'Create Task'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Add Review Finding (Reviewer) */}
      <Modal
        isOpen={isAddFindingModalOpen}
        onClose={() => setIsAddFindingModalOpen(false)}
        title="Record Quality Review Finding"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600 text-xs">
            Log a structured finding for the preparer. Blocking findings must be resolved before approval.
          </p>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-medium text-slate-700 mb-1">Category *</label>
              <select
                value={findingCategory}
                onChange={e => setFindingCategory(e.target.value)}
                className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="CALCULATION">Calculation / Computation</option>
                <option value="DOCUMENTATION">Documentation / Invoices</option>
                <option value="STATUTORY_RULE">Statutory Rule / Section</option>
                <option value="CLIENT_DATA">Client Master / PAN / GSTIN</option>
                <option value="RECONCILIATION">Reconciliation / 26AS</option>
                <option value="OTHER">Other / General Quality</option>
              </select>
            </div>
            <div>
              <label className="block font-medium text-slate-700 mb-1">Severity *</label>
              <select
                value={findingSeverity}
                onChange={e => setFindingSeverity(e.target.value)}
                className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="BLOCKING">BLOCKING (Halts Approval)</option>
                <option value="ADVISORY">ADVISORY (Recommendation)</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Description *</label>
            <textarea
              rows={3}
              placeholder="e.g. Total eligible ITC does not match table 4(A)(5) of GSTR-2B by Rs. 14,250."
              value={findingDescription}
              onChange={e => setFindingDescription(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Corrective Guidance</label>
            <input
              type="text"
              placeholder="e.g. Reverse ineligible ITC under Rule 42 or re-verify purchase register."
              value={findingGuidance}
              onChange={e => setFindingGuidance(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Source Reference / Field</label>
            <input
              type="text"
              placeholder="e.g. Table 4(A)(5) / Invoice INV-2026-089"
              value={findingRef}
              onChange={e => setFindingRef(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsAddFindingModalOpen(false)}>Cancel</Button>
            <Button
              variant="primary"
              disabled={!findingDescription.trim() || isSubmitting}
              onClick={handleAddFinding}
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
            >
              {isSubmitting ? 'Logging...' : 'Record Finding'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Respond to Finding (Preparer) */}
      <Modal
        isOpen={isRespondModalOpen}
        onClose={() => setIsRespondModalOpen(false)}
        title="Respond to Review Finding"
      >
        <div className="space-y-4 text-sm">
          {selectedFinding && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
              <span className="text-[10px] font-bold uppercase px-1.5 py-0.5 rounded bg-slate-200 text-slate-700">
                {selectedFinding.category} · {selectedFinding.severity}
              </span>
              <p className="text-sm font-semibold text-slate-900 mt-1">{selectedFinding.description}</p>
              {selectedFinding.correctiveGuidance && (
                <p className="text-xs text-indigo-700 mt-0.5">Hint: {selectedFinding.correctiveGuidance}</p>
              )}
            </div>
          )}
          <div>
            <label className="block font-medium text-slate-700 mb-1">Preparer Explanation / Correction *</label>
            <textarea
              rows={3}
              placeholder="e.g. Reconciled purchase ledger. Ineligible ITC of Rs. 14,250 reversed in Table 4(B)(2)."
              value={preparerResponseText}
              onChange={e => setPreparerResponseText(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsRespondModalOpen(false)}>Cancel</Button>
            <Button
              variant="primary"
              disabled={!preparerResponseText.trim() || isSubmitting}
              onClick={handleSubmitResponse}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              {isSubmitting ? 'Submitting...' : 'Submit Response'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal: Resolve / Dismiss Finding (Reviewer) */}
      <Modal
        isOpen={isResolveModalOpen}
        onClose={() => setIsResolveModalOpen(false)}
        title="Resolve / Dismiss Review Finding"
      >
        <div className="space-y-4 text-sm">
          {selectedFinding && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
              <span className="text-[10px] font-bold uppercase px-1.5 py-0.5 rounded bg-slate-200 text-slate-700">
                {selectedFinding.category} · {selectedFinding.severity}
              </span>
              <p className="text-sm font-semibold text-slate-900 mt-1">{selectedFinding.description}</p>
              {selectedFinding.preparerResponse && (
                <p className="text-xs text-blue-800 mt-1">
                  <strong>Preparer Response:</strong> {selectedFinding.preparerResponse}
                </p>
              )}
            </div>
          )}
          <div>
            <label className="block font-medium text-slate-700 mb-1">Resolution Decision *</label>
            <select
              value={resolutionDecision}
              onChange={e => setResolutionDecision(e.target.value as 'RESOLVED' | 'DISMISSED')}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="RESOLVED">RESOLVED (Correction accepted & verified)</option>
              <option value="DISMISSED">DISMISSED (Exception granted or non-issue)</option>
            </select>
          </div>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Reviewer Notes (Optional)</label>
            <textarea
              rows={2}
              placeholder="e.g. Verified revised purchase register and ITC reversal."
              value={resolutionNotes}
              onChange={e => setResolutionNotes(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsResolveModalOpen(false)}>Cancel</Button>
            <Button
              variant="primary"
              disabled={isSubmitting}
              onClick={handleSubmitResolution}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              {isSubmitting ? 'Recording...' : 'Confirm Resolution'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
