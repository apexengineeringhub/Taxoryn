import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  CheckSquare,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Calendar,
  User,
  Filter,
  Search,
  ArrowRight,
  ShieldAlert,
  Send,
  UserCheck,
  Building,
  RefreshCw,
  ExternalLink,
  ChevronRight,
  HelpCircle,
  Play,
  PauseCircle,
  FileCheck,
  Check,
} from 'lucide-react';
import { complianceWorkflowApi, employeeApi } from '../api/endpoints';
import {
  ComplianceWorkflowDto,
  ComplianceWorkbenchSummaryDto,
  ComplianceWorkflowStatus,
  Employee,
} from '../types';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';

export const ComplianceWorkbenchPage: React.FC = () => {
  const navigate = useNavigate();

  const [workflows, setWorkflows] = useState<ComplianceWorkflowDto[]>([]);
  const [summary, setSummary] = useState<ComplianceWorkbenchSummaryDto | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  // Tab & Filters
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [domainFilter, setDomainFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Modals
  const [selectedWorkflow, setSelectedWorkflow] = useState<ComplianceWorkflowDto | null>(null);
  const [isWaitModalOpen, setIsWaitModalOpen] = useState(false);
  const [waitReason, setWaitReason] = useState('');
  const [waitExpectedDate, setWaitExpectedDate] = useState('');

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assignPreparerId, setAssignPreparerId] = useState('');
  const [assignReviewerId, setAssignReviewerId] = useState('');
  const [assignTargetDate, setAssignTargetDate] = useState('');
  const [assignPriority, setAssignPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'CRITICAL'>('HIGH');

  const [isFiledModalOpen, setIsFiledModalOpen] = useState(false);
  const [filedDate, setFiledDate] = useState(new Date().toISOString().split('T')[0]);
  const [filedAckNumber, setFiledAckNumber] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchSummary = useCallback(async () => {
    try {
      const data = await complianceWorkflowApi.getWorkbenchSummary();
      setSummary(data);
    } catch (err) {
      console.error('Failed to load workbench summary', err);
    }
  }, []);

  const fetchWorkflows = useCallback(async () => {
    try {
      setIsLoading(true);
      const params: any = {
        page: currentPage,
        size: 15,
        search: searchTerm.trim() || undefined,
        workflowStatus: statusFilter !== 'ALL' ? statusFilter : undefined,
        priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
      };

      if (activeTab === 'MY_WORK') {
        params.viewType = 'MY_ASSIGNED';
      } else if (activeTab === 'DUE_TODAY') {
        params.viewType = 'DUE_TODAY';
      } else if (activeTab === 'OVERDUE') {
        params.viewType = 'OVERDUE';
      } else if (activeTab === 'WAITING_FOR_CLIENT') {
        params.viewType = 'WAITING_FOR_CLIENT';
      } else if (activeTab === 'UNDER_REVIEW') {
        params.workflowStatus = 'UNDER_REVIEW';
      } else if (activeTab === 'READY_FOR_FILING') {
        params.workflowStatus = 'READY_FOR_FILING';
      } else if (activeTab === 'COMPLETED') {
        params.workflowStatus = 'COMPLETED';
      }

      const res = await complianceWorkflowApi.getWorkbenchWorkflows(params);
      if (res) {
        setWorkflows(res.content || []);
        setTotalElements(res.totalElements || 0);
        setTotalPages(res.totalPages || 0);
      }
    } catch (err) {
      console.error('Failed to fetch workbench workflows', err);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, searchTerm, statusFilter, priorityFilter, activeTab]);

  useEffect(() => {
    fetchSummary();
    employeeApi.getAll({ size: 100 }).then(res => {
      setEmployees(res?.content || []);
    }).catch(err => console.error('Failed to load employees', err));
  }, [fetchSummary]);

  useEffect(() => {
    fetchWorkflows();
  }, [fetchWorkflows]);

  // Actions
  const handleStartWorkflow = async (workflowId: string) => {
    try {
      await complianceWorkflowApi.startWorkflow(workflowId);
      fetchWorkflows();
      fetchSummary();
    } catch (err) {
      console.error('Failed to start workflow', err);
    }
  };

  const handleResumeWorkflow = async (workflowId: string) => {
    try {
      await complianceWorkflowApi.resumeClient(workflowId);
      fetchWorkflows();
      fetchSummary();
    } catch (err) {
      console.error('Failed to resume workflow', err);
    }
  };

  const handleOpenWaitModal = (wf: ComplianceWorkflowDto) => {
    setSelectedWorkflow(wf);
    setWaitReason('');
    setWaitExpectedDate('');
    setIsWaitModalOpen(true);
  };

  const handleSubmitWaitModal = async () => {
    if (!selectedWorkflow || !waitReason) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.waitClient(selectedWorkflow.id, {
        reason: waitReason,
        expectedResponseDate: waitExpectedDate || undefined,
      });
      setIsWaitModalOpen(false);
      fetchWorkflows();
      fetchSummary();
    } catch (err) {
      console.error('Failed to set waiting for client', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenAssignModal = (wf: ComplianceWorkflowDto) => {
    setSelectedWorkflow(wf);
    setAssignPreparerId(wf.assignedEmployeeId || '');
    setAssignReviewerId(wf.reviewerEmployeeId || '');
    setAssignTargetDate(wf.targetDate || '');
    setAssignPriority(wf.priority || 'HIGH');
    setIsAssignModalOpen(true);
  };

  const handleSubmitAssignModal = async () => {
    if (!selectedWorkflow) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.assignWorkflow(selectedWorkflow.id, {
        assignedEmployeeId: assignPreparerId || undefined,
        reviewerEmployeeId: assignReviewerId || undefined,
        priority: assignPriority,
        targetDate: assignTargetDate || undefined,
      });
      setIsAssignModalOpen(false);
      fetchWorkflows();
      fetchSummary();
    } catch (err) {
      console.error('Failed to assign workflow', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenFiledModal = (wf: ComplianceWorkflowDto) => {
    setSelectedWorkflow(wf);
    setFiledDate(new Date().toISOString().split('T')[0]);
    setFiledAckNumber(wf.acknowledgementNumber || '');
    setIsFiledModalOpen(true);
  };

  const handleSubmitFiledModal = async () => {
    if (!selectedWorkflow) return;
    try {
      setIsSubmitting(true);
      await complianceWorkflowApi.markFiled(selectedWorkflow.id, {
        filedDate,
        acknowledgementNumber: filedAckNumber || undefined,
      });
      setIsFiledModalOpen(false);
      fetchWorkflows();
      fetchSummary();
    } catch (err) {
      console.error('Failed to mark filed', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadge = (status: ComplianceWorkflowStatus, waiting: boolean) => {
    if (waiting || status === 'WAITING_FOR_CLIENT') {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-300">
          <Clock className="w-3 h-3 mr-1" /> Waiting for Client
        </span>
      );
    }
    switch (status) {
      case 'CREATED':
      case 'READY':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">Ready</span>;
      case 'IN_PROGRESS':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200">In Progress</span>;
      case 'UNDER_REVIEW':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">Under Review</span>;
      case 'CHANGES_REQUIRED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">Changes Required</span>;
      case 'READY_FOR_FILING':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-100 text-teal-800 border border-teal-200">Ready for Filing</span>;
      case 'FILED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800 border border-indigo-200">Filed</span>;
      case 'ACKNOWLEDGEMENT_PENDING':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800 border border-yellow-200">Ack Pending</span>;
      case 'COMPLETED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">Completed</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-800 border border-gray-200">Cancelled</span>;
      default:
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-800">{status}</span>;
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'CRITICAL':
      case 'URGENT':
        return <span className="text-xs px-2 py-0.5 rounded bg-red-100 text-red-700 font-semibold border border-red-200">{priority}</span>;
      case 'HIGH':
        return <span className="text-xs px-2 py-0.5 rounded bg-orange-100 text-orange-700 font-semibold border border-orange-200">HIGH</span>;
      case 'MEDIUM':
        return <span className="text-xs px-2 py-0.5 rounded bg-blue-100 text-blue-700 font-semibold border border-blue-200">MED</span>;
      default:
        return <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-semibold">LOW</span>;
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
              <CheckSquare className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">Practitioner Compliance Workbench</h1>
              <p className="text-sm text-slate-500">
                Operational execution cockpit for recurring compliance obligations, 9-step preparation checklists, and maker-checker reviews
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Link to="/compliance/calendar">
            <Button variant="outline" className="flex items-center gap-2">
              <Calendar className="w-4 h-4" /> Compliance Calendar
            </Button>
          </Link>
          <Button variant="outline" onClick={() => { fetchWorkflows(); fetchSummary(); }} className="flex items-center gap-2">
            <RefreshCw className="w-4 h-4" /> Refresh
          </Button>
        </div>
      </div>

      {/* Summary Metrics Cockpit */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
          <div className="bg-white p-3 rounded-lg border border-slate-200 shadow-sm text-center">
            <p className="text-xs text-slate-500 font-medium">Active</p>
            <p className="text-xl font-bold text-slate-900 mt-1">{summary.totalActive}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-amber-200 shadow-sm text-center bg-amber-50/40">
            <p className="text-xs text-amber-700 font-medium">Due Today</p>
            <p className="text-xl font-bold text-amber-900 mt-1">{summary.dueToday}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-blue-200 shadow-sm text-center">
            <p className="text-xs text-blue-700 font-medium">This Week</p>
            <p className="text-xl font-bold text-blue-900 mt-1">{summary.dueThisWeek}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-red-200 shadow-sm text-center bg-red-50/40">
            <p className="text-xs text-red-700 font-medium">Overdue</p>
            <p className="text-xl font-bold text-red-900 mt-1">{summary.overdue}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-yellow-200 shadow-sm text-center bg-yellow-50/40">
            <p className="text-xs text-yellow-800 font-medium">Wait Client</p>
            <p className="text-xl font-bold text-yellow-900 mt-1">{summary.waitingForClient}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-purple-200 shadow-sm text-center bg-purple-50/40">
            <p className="text-xs text-purple-700 font-medium">Under Review</p>
            <p className="text-xl font-bold text-purple-900 mt-1">{summary.underReview}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-teal-200 shadow-sm text-center bg-teal-50/40">
            <p className="text-xs text-teal-700 font-medium">Ready Filing</p>
            <p className="text-xl font-bold text-teal-900 mt-1">{summary.readyForFiling}</p>
          </div>
          <div className="bg-white p-3 rounded-lg border border-emerald-200 shadow-sm text-center bg-emerald-50/40">
            <p className="text-xs text-emerald-700 font-medium">Done (Month)</p>
            <p className="text-xl font-bold text-emerald-900 mt-1">{summary.completedThisMonth}</p>
          </div>
        </div>
      )}

      {/* Main Tabs & Filters Card */}
      <Card className="p-0 overflow-hidden shadow-sm border-slate-200">
        {/* Tab Navigation */}
        <div className="border-b border-slate-200 bg-slate-50/50 px-4 pt-2 flex flex-wrap gap-2">
          {[
            { id: 'ALL', label: 'All Workflows' },
            { id: 'MY_WORK', label: `My Work (${summary?.myAssigned || 0})` },
            { id: 'DUE_TODAY', label: `Due Today (${summary?.dueToday || 0})` },
            { id: 'OVERDUE', label: `Overdue (${summary?.overdue || 0})` },
            { id: 'WAITING_FOR_CLIENT', label: `Waiting on Client (${summary?.waitingForClient || 0})` },
            { id: 'UNDER_REVIEW', label: `Under Review (${summary?.underReview || 0})` },
            { id: 'READY_FOR_FILING', label: `Ready for Filing (${summary?.readyForFiling || 0})` },
            { id: 'COMPLETED', label: 'Completed' },
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => { setActiveTab(tab.id); setCurrentPage(0); }}
              className={clsx(
                'px-4 py-2.5 text-xs sm:text-sm font-semibold border-b-2 transition-colors duration-150',
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600 bg-white rounded-t-lg'
                  : 'border-transparent text-slate-600 hover:text-slate-900 hover:border-slate-300'
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Filter Toolbar */}
        <div className="p-4 bg-white border-b border-slate-200 flex flex-col md:flex-row gap-3 items-center justify-between">
          <div className="relative w-full md:w-80">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search client, title, PAN, GSTIN..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
            <select
              value={statusFilter}
              onChange={e => { setStatusFilter(e.target.value); setCurrentPage(0); }}
              className="px-3 py-2 text-xs font-medium border border-slate-200 rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="READY">Ready</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="WAITING_FOR_CLIENT">Waiting for Client</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="CHANGES_REQUIRED">Changes Required</option>
              <option value="READY_FOR_FILING">Ready for Filing</option>
              <option value="FILED">Filed</option>
              <option value="COMPLETED">Completed</option>
            </select>

            <select
              value={priorityFilter}
              onChange={e => { setPriorityFilter(e.target.value); setCurrentPage(0); }}
              className="px-3 py-2 text-xs font-medium border border-slate-200 rounded-lg bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>

        {/* Workflow Table */}
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="py-16 text-center text-slate-500">
              <RefreshCw className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm font-medium">Loading practitioner execution workflows...</p>
            </div>
          ) : workflows.length === 0 ? (
            <div className="py-16 text-center text-slate-500">
              <CheckSquare className="w-12 h-12 mx-auto text-slate-300 mb-3" />
              <p className="text-base font-semibold text-slate-700">No execution workflows found</p>
              <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                Obligations generated in Compliance Calendar automatically populate operational workflows when started.
              </p>
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/75 text-[11px] uppercase tracking-wider font-semibold text-slate-500">
                  <th className="py-3 px-4">Client & Obligation</th>
                  <th className="py-3 px-4">Period & Domain</th>
                  <th className="py-3 px-4">Deadlines (Target vs Statutory)</th>
                  <th className="py-3 px-4">Readiness Checklist</th>
                  <th className="py-3 px-4">Maker / Checker</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-sm">
                {workflows.map(wf => (
                  <tr key={wf.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4">
                      <div>
                        <Link
                          to={`/compliance/workflows/${wf.id}`}
                          className="font-semibold text-indigo-600 hover:text-indigo-800 hover:underline flex items-center gap-1.5"
                        >
                          {wf.obligationTitle || 'Compliance Execution Workflow'}
                          <ExternalLink className="w-3.5 h-3.5 text-indigo-400" />
                        </Link>
                        <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-500">
                          <span className="font-medium text-slate-700 flex items-center gap-1">
                            <Building className="w-3 h-3 text-slate-400" /> {wf.clientName || 'Client'}
                          </span>
                          {wf.pan && <span className="text-[11px] bg-slate-100 px-1.5 py-0.2 rounded text-slate-600">{wf.pan}</span>}
                          {wf.gstin && <span className="text-[11px] bg-slate-100 px-1.5 py-0.2 rounded text-slate-600">{wf.gstin}</span>}
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="text-xs">
                        <span className="font-semibold text-slate-800">{wf.periodLabel || 'Current Period'}</span>
                        <div className="text-[11px] text-slate-500 mt-0.5">{wf.obligationType || 'STATUTORY'}</div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="text-xs space-y-1">
                        <div className="flex items-center gap-1.5 font-medium text-indigo-700">
                          <Clock className="w-3 h-3 text-indigo-500" />
                          <span>Target: {wf.targetDate || 'Not set'}</span>
                        </div>
                        <div className="flex items-center gap-1.5 text-slate-500">
                          <Calendar className="w-3 h-3 text-slate-400" />
                          <span>Statutory: {wf.statutoryDueDate}</span>
                          {wf.isOverdue && (
                            <span className="text-[10px] px-1.5 py-0.2 bg-red-100 text-red-700 rounded font-bold">OVERDUE</span>
                          )}
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="w-36">
                        <div className="flex items-center justify-between text-xs text-slate-600 mb-1">
                          <span className="font-semibold">{wf.completedChecklistSteps}/{wf.totalChecklistSteps} steps</span>
                          <span className="text-[11px] font-bold text-indigo-600">{wf.progressPercentage}%</span>
                        </div>
                        <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                          <div
                            className={clsx(
                              'h-2 rounded-full transition-all duration-300',
                              wf.progressPercentage === 100 ? 'bg-emerald-500' : 'bg-indigo-600'
                            )}
                            style={{ width: `${wf.progressPercentage}%` }}
                          />
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="text-xs space-y-0.5">
                        <div className="flex items-center gap-1 text-slate-700">
                          <User className="w-3 h-3 text-slate-400" />
                          <span>Prep: <strong className="font-medium">{wf.assignedEmployeeName || 'Unassigned'}</strong></span>
                        </div>
                        <div className="flex items-center gap-1 text-slate-500 text-[11px]">
                          <UserCheck className="w-3 h-3 text-slate-400" />
                          <span>Rev: <strong>{wf.reviewerEmployeeName || 'None'}</strong></span>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="space-y-1">
                        <div>{getStatusBadge(wf.workflowStatus, wf.waitingForClient)}</div>
                        <div>{getPriorityBadge(wf.priority)}</div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        {wf.workflowStatus === 'READY' || wf.workflowStatus === 'CREATED' ? (
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => handleStartWorkflow(wf.id)}
                            className="text-xs py-1 px-2.5 flex items-center gap-1"
                          >
                            <Play className="w-3 h-3" /> Start
                          </Button>
                        ) : null}

                        {wf.waitingForClient ? (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleResumeWorkflow(wf.id)}
                            className="text-xs py-1 px-2.5 text-amber-700 border-amber-300 hover:bg-amber-50 flex items-center gap-1"
                          >
                            <Play className="w-3 h-3" /> Resume
                          </Button>
                        ) : wf.workflowStatus === 'IN_PROGRESS' || wf.workflowStatus === 'CHANGES_REQUIRED' ? (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenWaitModal(wf)}
                            className="text-xs py-1 px-2.5 text-yellow-700 border-yellow-300 hover:bg-yellow-50 flex items-center gap-1"
                          >
                            <PauseCircle className="w-3 h-3" /> Wait Client
                          </Button>
                        ) : null}

                        {wf.workflowStatus === 'READY_FOR_FILING' ? (
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={() => handleOpenFiledModal(wf)}
                            className="text-xs py-1 px-2.5 bg-teal-600 hover:bg-teal-700 text-white flex items-center gap-1"
                          >
                            <FileCheck className="w-3 h-3" /> Mark Filed
                          </Button>
                        ) : null}

                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleOpenAssignModal(wf)}
                          className="text-xs py-1 px-2 text-slate-600 hover:text-slate-900"
                          title="Reassign preparer/reviewer"
                        >
                          Assign
                        </Button>

                        <Link to={`/compliance/workflows/${wf.id}`}>
                          <Button size="sm" variant="ghost" className="text-xs py-1 px-2 text-indigo-600 hover:text-indigo-800">
                            Details <ChevronRight className="w-3 h-3 ml-0.5" />
                          </Button>
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-200 bg-slate-50 flex items-center justify-between text-xs text-slate-600">
            <div>
              Showing {workflows.length} of {totalElements} workflows
            </div>
            <div className="flex gap-1">
              <Button
                size="sm"
                variant="outline"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage(p => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* 1. Modal: Waiting for Client */}
      <Modal
        isOpen={isWaitModalOpen}
        onClose={() => setIsWaitModalOpen(false)}
        title="Block Workflow — Waiting for Client Action"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Mark this workflow as paused pending client action (missing documents, signature approval, or tax payment confirmation).
          </p>
          <div>
            <label className="block font-medium text-slate-700 mb-1">Reason for Blocking / Required Inputs *</label>
            <textarea
              rows={3}
              placeholder="e.g. Awaiting purchase register Excel and bank statement for July 2026"
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

      {/* 2. Modal: Assign Workflow */}
      <Modal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        title="Assign Practitioner Preparer & Reviewer"
      >
        <div className="space-y-4 text-sm">
          <div>
            <label className="block font-medium text-slate-700 mb-1">Preparer (Practitioner Staff)</label>
            <select
              value={assignPreparerId}
              onChange={e => setAssignPreparerId(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="">-- Unassigned --</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.fullName || emp.email}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-medium text-slate-700 mb-1">Reviewer (Checker / Partner)</label>
            <select
              value={assignReviewerId}
              onChange={e => setAssignReviewerId(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="">-- No Reviewer --</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.fullName || emp.email}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-medium text-slate-700 mb-1">Internal Target Date</label>
            <input
              type="date"
              value={assignTargetDate}
              onChange={e => setAssignTargetDate(e.target.value)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-medium text-slate-700 mb-1">Execution Priority</label>
            <select
              value={assignPriority}
              onChange={e => setAssignPriority(e.target.value as any)}
              className="w-full p-2.5 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsAssignModalOpen(false)}>Cancel</Button>
            <Button variant="primary" disabled={isSubmitting} onClick={handleSubmitAssignModal}>
              {isSubmitting ? 'Updating...' : 'Save Assignments'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 3. Modal: Mark Filed */}
      <Modal
        isOpen={isFiledModalOpen}
        onClose={() => setIsFiledModalOpen(false)}
        title="Record Government Portal Filing"
      >
        <div className="space-y-4 text-sm">
          <p className="text-slate-600">
            Record completion of statutory portal filing and capture the government acknowledgement ARN.
          </p>
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
            <label className="block font-medium text-slate-700 mb-1">Acknowledgement / ARN / Challan Reference</label>
            <input
              type="text"
              placeholder="e.g. AA2708260192837 or 123456789012345"
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
    </div>
  );
};
