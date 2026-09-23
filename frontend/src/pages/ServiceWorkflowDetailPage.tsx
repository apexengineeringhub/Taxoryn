import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Clock,
  AlertTriangle,
  User,
  ShieldAlert,
  Play,
  Pause,
  Send,
  Flag,
  ChevronRight,
  FileText,
  HelpCircle,
  PlusCircle,
  RefreshCw,
  Eye,
  Check,
  Building2,
  UserCheck
} from 'lucide-react';
import { serviceWorkflowApi, clientApi, employeeApi } from '../api/endpoints';
import {
  ClientServiceWorkflow,
  ClientServiceWorkflowStep,
  ClientServicePeriod,
  ServiceWorkflowStatus,
  StepStatus,
  ServiceWorkType,
  Employee
} from '../types';

export const ServiceWorkflowDetailPage: React.FC = () => {
  const { clientId, serviceId } = useParams<{ clientId: string; serviceId: string }>();
  const navigate = useNavigate();

  const [workflow, setWorkflow] = useState<ClientServiceWorkflow | null>(null);
  const [periods, setPeriods] = useState<ClientServicePeriod[]>([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState<string>('');
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  // Modals
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [showWaitingModal, setShowWaitingModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedStepForAction, setSelectedStepForAction] = useState<ClientServiceWorkflowStep | null>(null);
  const [clientActionText, setClientActionText] = useState('');
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [stepNoteText, setStepNoteText] = useState('');

  // Period creation state
  const [newPeriodLabel, setNewPeriodLabel] = useState('');
  const [newPeriodType, setNewPeriodType] = useState<'MONTHLY' | 'QUARTERLY' | 'ANNUAL' | 'EVENT_BASED'>('MONTHLY');
  const [newPeriodDueDate, setNewPeriodDueDate] = useState('');

  useEffect(() => {
    if (serviceId) {
      loadData();
    }
  }, [serviceId]);

  const loadData = async () => {
    if (!serviceId) return;
    setLoading(true);
    setError(null);
    try {
      const [periodsData, activeWf, empsData] = await Promise.all([
        serviceWorkflowApi.getServicePeriods(serviceId),
        serviceWorkflowApi.getActiveWorkflowForService(serviceId).catch(() => null),
        employeeApi.getAll({ page: 0, size: 100 }).catch(() => ({ content: [] }))
      ]);

      setPeriods(periodsData);
      setEmployees(empsData.content || []);

      if (activeWf) {
        setWorkflow(activeWf);
        setSelectedPeriodId(activeWf.periodId);
      } else if (periodsData.length > 0) {
        setSelectedPeriodId(periodsData[0].id);
        if (periodsData[0].hasActiveWorkflow && periodsData[0].activeWorkflowId) {
          const wf = await serviceWorkflowApi.getWorkflowById(periodsData[0].activeWorkflowId);
          setWorkflow(wf);
        }
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load workflow information');
    } finally {
      setLoading(false);
    }
  };

  const handlePeriodChange = async (periodId: string) => {
    setSelectedPeriodId(periodId);
    setWorkflow(null);
    const period = periods.find(p => p.id === periodId);
    if (period && period.hasActiveWorkflow && period.activeWorkflowId) {
      try {
        const wf = await serviceWorkflowApi.getWorkflowById(period.activeWorkflowId);
        setWorkflow(wf);
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleGenerateWorkflow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!serviceId) return;
    setActionLoading(true);
    try {
      let targetPeriodId = selectedPeriodId;
      if (!targetPeriodId && newPeriodLabel) {
        const createdPeriod = await serviceWorkflowApi.createServicePeriod(serviceId, {
          clientServiceId: serviceId,
          periodType: newPeriodType,
          periodLabel: newPeriodLabel.trim(),
          dueDate: newPeriodDueDate || undefined
        });
        targetPeriodId = createdPeriod.id;
      }

      const generated = await serviceWorkflowApi.generateWorkflow(serviceId, {
        clientServiceId: serviceId,
        periodId: targetPeriodId,
        periodLabel: newPeriodLabel || undefined,
        periodType: newPeriodType,
        dueDate: newPeriodDueDate || undefined
      });

      setWorkflow(generated);
      setShowGenerateModal(false);
      await loadData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to generate workflow');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateStepStatus = async (stepId: string, status: StepStatus, notes?: string, clientSummary?: string) => {
    if (!workflow) return;
    setActionLoading(true);
    try {
      await serviceWorkflowApi.updateStepStatus(workflow.id, stepId, {
        status,
        notes,
        clientActionSummary: clientSummary
      });
      const updatedWf = await serviceWorkflowApi.getWorkflowById(workflow.id);
      setWorkflow(updatedWf);
      setShowWaitingModal(false);
      setSelectedStepForAction(null);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to update step status');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAssignWorkflow = async () => {
    if (!workflow) return;
    setActionLoading(true);
    try {
      const updated = await serviceWorkflowApi.assignWorkflow(workflow.id, {
        assignedEmployeeId: selectedAssigneeId || undefined
      });
      setWorkflow(updated);
      setShowAssignModal(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to assign workflow');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdatePriority = async (priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT') => {
    if (!workflow) return;
    try {
      const updated = await serviceWorkflowApi.updateWorkflowPriority(workflow.id, { priority });
      setWorkflow(updated);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to update priority');
    }
  };

  const getStatusBadge = (status: ServiceWorkflowStatus) => {
    switch (status) {
      case 'NOT_STARTED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-800">Not Started</span>;
      case 'IN_PROGRESS':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">In Progress</span>;
      case 'WAITING_FOR_CLIENT':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">Waiting for Client</span>;
      case 'READY_FOR_FILING':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800">Ready for Filing</span>;
      case 'FILED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800">Filed</span>;
      case 'COMPLETED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">Completed</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800">Cancelled</span>;
      default:
        return null;
    }
  };

  const getPriorityBadge = (prio: string) => {
    switch (prio) {
      case 'URGENT':
        return <span className="text-xs font-bold text-red-600 bg-red-50 border border-red-200 px-2 py-0.5 rounded">URGENT</span>;
      case 'HIGH':
        return <span className="text-xs font-semibold text-orange-600 bg-orange-50 border border-orange-200 px-2 py-0.5 rounded">HIGH</span>;
      case 'MEDIUM':
        return <span className="text-xs font-medium text-blue-600 bg-blue-50 border border-blue-200 px-2 py-0.5 rounded">MEDIUM</span>;
      default:
        return <span className="text-xs text-gray-500 bg-gray-50 border border-gray-200 px-2 py-0.5 rounded">LOW</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <RefreshCw className="w-8 h-8 text-indigo-600 animate-spin" />
        <span className="ml-3 text-gray-600 font-medium">Loading service workflow operations...</span>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center space-x-2 text-sm text-gray-500">
        <Link to={`/clients/${clientId}`} className="hover:text-indigo-600 flex items-center">
          <ArrowLeft className="w-4 h-4 mr-1" />
          Back to Client 360
        </Link>
        <span>/</span>
        <span className="text-gray-900 font-medium">Service Operational Workflow</span>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm flex items-center">
          <AlertTriangle className="w-5 h-5 mr-2 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Service Header & Period Selector */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center space-x-3">
              <h1 className="text-2xl font-bold text-gray-900">
                {workflow?.serviceName || 'Client Service Engagement'}
              </h1>
              {workflow && getStatusBadge(workflow.status)}
              {workflow && getPriorityBadge(workflow.priority)}
            </div>
            <p className="text-sm text-gray-500 mt-1 flex items-center gap-2">
              <span>Client: <strong className="text-gray-800">{workflow?.clientName || 'Selected Client'}</strong></span>
              {workflow?.clientPan && <span>• PAN: <strong className="text-gray-800">{workflow.clientPan}</strong></span>}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* Period Selector */}
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Compliance Period:</label>
              <select
                className="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
                value={selectedPeriodId}
                onChange={(e) => handlePeriodChange(e.target.value)}
              >
                {periods.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.periodLabel} {p.status === 'COMPLETED' ? '(Completed)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <button
              onClick={() => setShowGenerateModal(true)}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700"
            >
              <PlusCircle className="w-4 h-4 mr-1.5" />
              New Period / Workflow
            </button>
          </div>
        </div>

        {/* Workflow Overview Banner */}
        {workflow ? (
          <div className="mt-6 pt-6 border-t border-gray-100 grid grid-cols-2 md:grid-cols-5 gap-4">
            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="text-xs text-gray-500 font-medium">Assigned Practitioner</div>
              <div className="text-sm font-semibold text-gray-900 mt-0.5 flex items-center gap-1.5">
                <User className="w-4 h-4 text-indigo-600" />
                <span>{workflow.assignedEmployeeName || 'Unassigned'}</span>
              </div>
              <button
                onClick={() => { setSelectedAssigneeId(workflow.assignedEmployeeId || ''); setShowAssignModal(true); }}
                className="text-xs text-indigo-600 hover:underline mt-1 block"
              >
                Change Assignee
              </button>
            </div>

            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="text-xs text-gray-500 font-medium">Statutory Due Date</div>
              <div className={`text-sm font-semibold mt-0.5 flex items-center gap-1.5 ${workflow.isOverdue ? 'text-red-600' : 'text-gray-900'}`}>
                <Calendar className="w-4 h-4" />
                <span>{workflow.dueDate || 'Not Specified'}</span>
              </div>
              {workflow.isOverdue && <span className="text-xs text-red-500 font-medium">Overdue</span>}
            </div>

            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="text-xs text-gray-500 font-medium">Priority</div>
              <select
                className="text-xs font-semibold bg-transparent border-0 p-0 mt-0.5 text-gray-900 focus:ring-0 cursor-pointer"
                value={workflow.priority}
                onChange={(e) => handleUpdatePriority(e.target.value as any)}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="text-xs text-gray-500 font-medium">Current Step</div>
              <div className="text-sm font-semibold text-indigo-900 mt-0.5 truncate" title={workflow.currentStepName}>
                {workflow.currentStepName || `Step ${workflow.currentStepSequence}`}
              </div>
              <div className="text-xs text-gray-500 mt-0.5">{workflow.completedSteps} of {workflow.totalSteps} steps</div>
            </div>

            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="text-xs text-gray-500 font-medium">Progress</div>
              <div className="flex items-center gap-2 mt-1.5">
                <div className="flex-1 bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-indigo-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${workflow.progressPercentage}%` }}
                  />
                </div>
                <span className="text-xs font-bold text-gray-700">{workflow.progressPercentage}%</span>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-6 pt-6 border-t border-gray-100 text-center py-8">
            <AlertTriangle className="w-8 h-8 text-amber-500 mx-auto mb-2" />
            <p className="text-gray-700 font-semibold">No operational workflow generated for this period.</p>
            <p className="text-sm text-gray-500 mt-1">Generate an automated compliance workflow from standard practice templates.</p>
            <button
              onClick={() => setShowGenerateModal(true)}
              className="mt-4 inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700"
            >
              Generate Practice Workflow
            </button>
          </div>
        )}
      </div>

      {/* Waiting for Client Alert Banner */}
      {workflow?.waitingForClient && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start space-x-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="text-sm font-semibold text-amber-900">Waiting for Client Action</h3>
            <p className="text-sm text-amber-700 mt-0.5">
              {workflow.pendingClientActionSummary || 'This workflow is currently paused awaiting client response or document submission.'}
            </p>
          </div>
        </div>
      )}

      {/* Interactive Workflow Steps Timeline */}
      {workflow && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h2 className="text-base font-semibold text-gray-900">Workflow Execution Steps</h2>
            <span className="text-xs text-gray-500 font-medium">Template: {workflow.templateName || 'Standard'}</span>
          </div>

          <div className="divide-y divide-gray-100">
            {workflow.steps.map((step) => {
              const isCurrent = step.sequence === workflow.currentStepSequence;
              const isCompleted = step.status === 'COMPLETED';
              const isWaiting = step.status === 'WAITING_FOR_CLIENT';

              return (
                <div
                  key={step.id}
                  className={`p-6 transition-colors ${
                    isCurrent ? 'bg-indigo-50/40 border-l-4 border-indigo-600' : isCompleted ? 'bg-white' : 'bg-gray-50/30'
                  }`}
                >
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-start space-x-4">
                      {/* Step Number Badge */}
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm shrink-0 ${
                        isCompleted
                          ? 'bg-emerald-100 text-emerald-700'
                          : isWaiting
                          ? 'bg-amber-100 text-amber-700'
                          : isCurrent
                          ? 'bg-indigo-600 text-white ring-4 ring-indigo-100'
                          : 'bg-gray-200 text-gray-600'
                      }`}>
                        {isCompleted ? <Check className="w-4 h-4" /> : step.sequence}
                      </div>

                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-sm font-bold text-gray-900">{step.name}</h3>
                          {step.mandatory && (
                            <span className="text-[10px] uppercase font-semibold bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
                              Mandatory
                            </span>
                          )}
                          {step.requiresClientInput && (
                            <span className="text-[10px] uppercase font-semibold bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded">
                              Client Input
                            </span>
                          )}
                          {step.requiresReview && (
                            <span className="text-[10px] uppercase font-semibold bg-purple-100 text-purple-800 px-1.5 py-0.5 rounded">
                              Review Required
                            </span>
                          )}
                        </div>

                        {step.description && (
                          <p className="text-xs text-gray-500 mt-1 max-w-2xl">{step.description}</p>
                        )}

                        <div className="flex items-center gap-4 text-xs text-gray-400 mt-2">
                          {step.assignedEmployeeName && (
                            <span className="flex items-center text-gray-600">
                              <User className="w-3.5 h-3.5 mr-1" />
                              {step.assignedEmployeeName}
                            </span>
                          )}
                          {step.dueDate && (
                            <span className="flex items-center text-gray-600">
                              <Calendar className="w-3.5 h-3.5 mr-1" />
                              Target: {step.dueDate}
                            </span>
                          )}
                          {step.completedAt && (
                            <span className="flex items-center text-emerald-600">
                              <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                              Completed on {new Date(step.completedAt).toLocaleDateString()}
                            </span>
                          )}
                        </div>

                        {step.notes && (
                          <div className="mt-2 text-xs bg-gray-100 text-gray-700 p-2 rounded max-w-xl">
                            <strong>Note:</strong> {step.notes}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Step Action Buttons */}
                    <div className="flex items-center gap-2 shrink-0">
                      {!isCompleted ? (
                        <>
                          <button
                            onClick={() => handleUpdateStepStatus(step.id, 'COMPLETED')}
                            disabled={actionLoading}
                            className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-md shadow-sm"
                          >
                            <Check className="w-3.5 h-3.5 mr-1" />
                            Complete Step
                          </button>

                          {step.requiresClientInput && !isWaiting && (
                            <button
                              onClick={() => {
                                setSelectedStepForAction(step);
                                setClientActionText('');
                                setShowWaitingModal(true);
                              }}
                              disabled={actionLoading}
                              className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-amber-800 bg-amber-100 hover:bg-amber-200 rounded-md"
                            >
                              <Pause className="w-3.5 h-3.5 mr-1" />
                              Wait for Client
                            </button>
                          )}

                          {isWaiting && (
                            <button
                              onClick={() => handleUpdateStepStatus(step.id, 'IN_PROGRESS')}
                              disabled={actionLoading}
                              className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-blue-800 bg-blue-100 hover:bg-blue-200 rounded-md"
                            >
                              <Play className="w-3.5 h-3.5 mr-1" />
                              Resume Work
                            </button>
                          )}

                          {!step.mandatory && (
                            <button
                              onClick={() => handleUpdateStepStatus(step.id, 'SKIPPED')}
                              disabled={actionLoading}
                              className="inline-flex items-center px-2.5 py-1.5 text-xs font-medium text-gray-500 hover:text-gray-700"
                            >
                              Skip
                            </button>
                          )}
                        </>
                      ) : (
                        <button
                          onClick={() => handleUpdateStepStatus(step.id, 'IN_PROGRESS')}
                          disabled={actionLoading}
                          className="inline-flex items-center px-2.5 py-1 text-xs text-gray-500 hover:text-indigo-600"
                        >
                          Reopen
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Generate Workflow Modal */}
      {showGenerateModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-gray-900">Generate Compliance Workflow</h3>
            <p className="text-xs text-gray-500">
              Instantiate the operational workflow for this service period based on practice templates.
            </p>

            <form onSubmit={handleGenerateWorkflow} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700">Period Label</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. October 2026, Q3 FY 2026-27"
                  className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                  value={newPeriodLabel}
                  onChange={(e) => setNewPeriodLabel(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-gray-700">Period Type</label>
                  <select
                    className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white"
                    value={newPeriodType}
                    onChange={(e) => setNewPeriodType(e.target.value as any)}
                  >
                    <option value="MONTHLY">Monthly</option>
                    <option value="QUARTERLY">Quarterly</option>
                    <option value="ANNUAL">Annual</option>
                    <option value="EVENT_BASED">Event / Case</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700">Statutory Due Date</label>
                  <input
                    type="date"
                    className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                    value={newPeriodDueDate}
                    onChange={(e) => setNewPeriodDueDate(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowGenerateModal(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 shadow-sm"
                >
                  {actionLoading ? 'Generating...' : 'Generate Workflow'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Waiting For Client Modal */}
      {showWaitingModal && selectedStepForAction && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-gray-900">Pause for Client Action</h3>
            <p className="text-xs text-gray-500">
              Specify the action or document required from the client for step: <strong>{selectedStepForAction.name}</strong>.
            </p>

            <div>
              <label className="block text-xs font-semibold text-gray-700">Pending Action Summary</label>
              <textarea
                rows={3}
                required
                placeholder="e.g., Awaiting signed Form 16, Bank statements folder..."
                className="mt-1 w-full border border-gray-300 rounded-lg p-2.5 text-sm"
                value={clientActionText}
                onChange={(e) => setClientActionText(e.target.value)}
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowWaitingModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={actionLoading || !clientActionText.trim()}
                onClick={() => handleUpdateStepStatus(selectedStepForAction.id, 'WAITING_FOR_CLIENT', undefined, clientActionText.trim())}
                className="px-4 py-2 bg-amber-600 text-white rounded-lg text-sm font-medium hover:bg-amber-700 shadow-sm"
              >
                {actionLoading ? 'Updating...' : 'Mark Waiting for Client'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Assign Workflow Modal */}
      {showAssignModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-lg font-bold text-gray-900">Assign Operational Practitioner</h3>
            <p className="text-xs text-gray-500">
              Select the practice team member responsible for executing this compliance workflow.
            </p>

            <div>
              <label className="block text-xs font-semibold text-gray-700">Practitioner</label>
              <select
                className="mt-1 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white"
                value={selectedAssigneeId}
                onChange={(e) => setSelectedAssigneeId(e.target.value)}
              >
                <option value="">Unassigned</option>
                {employees.map(emp => (
                  <option key={emp.id} value={emp.id}>
                    {emp.firstName} {emp.lastName || ''} ({emp.designation || 'Staff'})
                  </option>
                ))}
              </select>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAssignModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleAssignWorkflow}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 shadow-sm"
              >
                {actionLoading ? 'Saving...' : 'Save Assignment'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default ServiceWorkflowDetailPage;
