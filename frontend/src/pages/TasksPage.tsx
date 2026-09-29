import React, { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Plus,
  CheckSquare,
  List,
  LayoutGrid,
  AlertCircle,
  Clock,
  Sparkles,
  User,
  Building,
  CheckCircle2,
  Calendar,
  Layers,
  ChevronRight,
  UserCheck,
  Edit2,
  AlertTriangle,
  FileText,
  ShieldAlert,
  Flame,
  Zap,
  ExternalLink,
  Eye,
  XCircle,
  HelpCircle,
  Briefcase,
  Users,
  Timer,
  Check,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { taskApi, clientApi, employeeApi, engagementApi } from '../api/endpoints';
import { Task, Client, Employee, WorklistSummary, TaskWorklistParams, TaskStatus, TeamWorkloadSummary } from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const TasksPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [isClientsLoading, setIsClientsLoading] = useState(false);
  const [clientsError, setClientsError] = useState<string | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [clientEngagements, setClientEngagements] = useState<any[]>([]);
  const [isEngagementsLoading, setIsEngagementsLoading] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>('list');
  const [activeTab, setActiveTab] = useState<'WORKLIST' | 'TEAM_WORKLOAD' | 'ALL_TASKS'>(
    () => (searchParams.get('tab') as 'WORKLIST' | 'TEAM_WORKLOAD' | 'ALL_TASKS') || 'WORKLIST'
  );
  const [isModalOpen, setIsModalOpen] = useState(
    () => searchParams.get('action') === 'new' || searchParams.get('create') === 'true'
  );

  // Worklist specific state
  const [worklistScope, setWorklistScope] = useState<'MY_WORK' | 'TEAM_WORK'>(
    () => (searchParams.get('scope') as 'MY_WORK' | 'TEAM_WORK') || 'MY_WORK'
  );
  const [worklistBucket, setWorklistBucket] = useState<'ALL' | 'OVERDUE' | 'DUE_TODAY' | 'DUE_THIS_WEEK' | 'BLOCKED' | 'COMPLETED'>(
    () => (searchParams.get('bucket') as any) || 'ALL'
  );
  const [worklistAssignee, setWorklistAssignee] = useState<string>(() => searchParams.get('assignedTo') || '');
  const [worklistCategory, setWorklistCategory] = useState<string>(() => searchParams.get('category') || '');
  const [worklistSummary, setWorklistSummary] = useState<WorklistSummary | null>(null);

  // Team Workload State
  const [teamWorkload, setTeamWorkload] = useState<TeamWorkloadSummary[]>([]);

  // Detail / Inspect Task Modal State
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  // Complete Task Modal State (P0.3 Actual Minutes & Completion Notes)
  const [completingTask, setCompletingTask] = useState<Task | null>(null);
  const [actualMinutesInput, setActualMinutesInput] = useState<number | ''>('');
  const [completionNotesInput, setCompletionNotesInput] = useState('');
  const [isCompleteModalOpen, setIsCompleteModalOpen] = useState(false);

  // Block Task Modal State
  const [blockingTask, setBlockingTask] = useState<Task | null>(null);
  const [blockReasonInput, setBlockReasonInput] = useState('');
  const [isBlockModalOpen, setIsBlockModalOpen] = useState(false);

  // Edit / Reassign Task State
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState<{
    title: string;
    description: string;
    clientId: string;
    engagementId: string;
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'TDS' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    status: TaskStatus;
    startDate: string;
    dueDate: string;
    estimatedMinutes: number | '';
    actualMinutes: number | '';
    blockedReason: string;
    complianceId?: string;
    documentRequestId?: string;
  }>({
    title: '',
    description: '',
    clientId: '',
    engagementId: '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    status: 'TODO',
    startDate: '',
    dueDate: '',
    estimatedMinutes: '',
    actualMinutes: '',
    blockedReason: '',
  });

  // Filter States for standard view
  const [taskScope, setTaskScope] = useState<'MY_TASKS' | 'ALL_TASKS'>(
    () => (searchParams.get('assignedTo') || searchParams.get('category') || searchParams.get('status') ? 'ALL_TASKS' : 'MY_TASKS')
  );
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'TODO' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'BLOCKED' | 'COMPLETED'>(
    () => (searchParams.get('status') as any) || 'ALL'
  );
  const [categoryFilter, setCategoryFilter] = useState<string>(() => searchParams.get('category') || 'ALL');
  const [assigneeFilter, setAssigneeFilter] = useState<string>(() => searchParams.get('assignedTo') || 'ALL');
  const [mobileKanbanTab, setMobileKanbanTab] = useState<TaskStatus>('TODO');

  // Form State for creating task
  const [formData, setFormData] = useState<{
    title: string;
    description: string;
    clientId: string;
    engagementId: string;
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'TDS' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    startDate: string;
    dueDate: string;
    estimatedMinutes: number | '';
  }>({
    title: '',
    description: '',
    clientId: searchParams.get('clientId') || '',
    engagementId: searchParams.get('engagementId') || '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    startDate: new Date().toISOString().split('T')[0],
    dueDate: (() => {
      const d = new Date();
      d.setDate(d.getDate() + 7);
      return d.toISOString().split('T')[0];
    })(),
    estimatedMinutes: 60,
  });

  // Synchronize state when searchParams change
  useEffect(() => {
    if (searchParams.get('action') === 'new' || searchParams.get('create') === 'true') {
      const targetClientId = searchParams.get('clientId');
      if (targetClientId) {
        setFormData((prev) => ({ ...prev, clientId: targetClientId }));
        loadClientEngagements(targetClientId);
      }
      setIsModalOpen(true);
    }

    const tab = searchParams.get('tab') as 'WORKLIST' | 'TEAM_WORKLOAD' | 'ALL_TASKS' | null;
    if (tab) setActiveTab(tab);

    const scope = searchParams.get('scope') as 'MY_WORK' | 'TEAM_WORK' | null;
    if (scope) setWorklistScope(scope);

    const bucket = searchParams.get('bucket') as any;
    if (bucket) setWorklistBucket(bucket);

    const assignedTo = searchParams.get('assignedTo');
    if (assignedTo !== null && assignedTo !== undefined) {
      setWorklistAssignee(assignedTo);
      setAssigneeFilter(assignedTo || 'ALL');
      setTaskScope(assignedTo ? 'ALL_TASKS' : 'MY_TASKS');
    }

    const category = searchParams.get('category');
    if (category !== null && category !== undefined) {
      setWorklistCategory(category);
      setCategoryFilter(category || 'ALL');
    }

    const status = searchParams.get('status')?.toUpperCase();
    if (status) {
      if (status === 'OVERDUE') {
        setWorklistBucket('OVERDUE');
      } else if (status === 'COMPLETED') {
        setWorklistBucket('COMPLETED');
        setStatusFilter('COMPLETED');
      } else if (['TODO', 'IN_PROGRESS', 'UNDER_REVIEW', 'BLOCKED'].includes(status)) {
        setStatusFilter(status as any);
      } else if (status === 'PENDING') {
        setStatusFilter('TODO');
      } else if (status === 'ALL') {
        setStatusFilter('ALL');
      }
    }
  }, [searchParams]);

  // Ensure clients & employees are loaded whenever the create or edit modal opens
  useEffect(() => {
    if (isModalOpen || isEditModalOpen) {
      if (clients.length === 0 && !isClientsLoading) {
        loadClientsAndEmployees();
      }
    }
  }, [isModalOpen, isEditModalOpen, clients.length, isClientsLoading]);

  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isFirmAdmin = userRoleCodes.some((r: string) => ['ORG_ADMIN', 'SUPER_ADMIN', 'PARTNER', 'PRACTICE_OWNER', 'PRACTICE_ADMIN'].includes(r));
  const isStaff = userRoleCodes.some((r: string) => ['ARTICLE_ASSISTANT', 'STAFF', 'TRAINEE'].includes(r)) && !isFirmAdmin;

  // Find linked employee for logged in user
  const currentEmployee = useMemo(() => {
    return employees.find(
      (e) =>
        (e.email && user?.email && e.email.toLowerCase() === user.email.toLowerCase()) ||
        (user?.id && (e as any).userId === user.id)
    );
  }, [employees, user]);

  // Unified Assignee List: includes all employees + current user if not in employees list
  const assigneeOptions = useMemo(() => {
    const list: Array<{ id: string; name: string; isMe: boolean; designation: string; department?: string }> = [];

    if (user && !currentEmployee) {
      const myName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
      list.push({
        id: user.id,
        name: myName,
        isMe: true,
        designation: isFirmAdmin ? 'Practice Partner / Admin' : 'User Account',
        department: 'Practice Management',
      });
    }

    employees.forEach((emp) => {
      const isMe = Boolean(
        (emp.email && user?.email && emp.email.toLowerCase() === user.email.toLowerCase()) ||
        (user?.id && (emp as any).userId === user.id)
      );
      const name = emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.email;
      list.push({
        id: emp.id,
        name,
        isMe,
        designation: emp.designation || 'Staff',
        department: emp.department || 'Tax',
      });
    });

    return list;
  }, [employees, user, currentEmployee, isFirmAdmin]);

  // ID to use for "Assign to Me"
  const myAssigneeId = useMemo(() => {
    if (currentEmployee) return currentEmployee.id;
    if (user) return user.id;
    return '';
  }, [currentEmployee, user]);

  useEffect(() => {
    if (activeTab === 'WORKLIST') {
      loadWorklist();
    } else if (activeTab === 'TEAM_WORKLOAD') {
      loadTeamWorkload();
    } else {
      loadTasks();
    }
    loadClientsAndEmployees();
  }, [activeTab, worklistScope, worklistBucket, worklistAssignee, worklistCategory, taskScope, statusFilter, categoryFilter, assigneeFilter]);

  const loadWorklist = async () => {
    try {
      setIsLoading(true);
      const worklistParams: any = {
        scope: worklistScope,
        bucket: worklistBucket,
        size: 100,
      };
      if (worklistAssignee) {
        worklistParams.assignedTo = worklistAssignee;
      }
      if (worklistCategory) {
        worklistParams.taskCategory = worklistCategory;
      }
      const [listRes, summaryRes] = await Promise.allSettled([
        taskApi.getWorklist(worklistParams),
        taskApi.getWorklistSummary(),
      ]);

      if (listRes.status === 'fulfilled' && listRes.value) {
        const list = Array.isArray(listRes.value) ? listRes.value : (listRes.value?.content || []);
        setTasks(list);
      } else {
        setTasks([]);
      }

      if (summaryRes.status === 'fulfilled' && summaryRes.value) {
        setWorklistSummary(summaryRes.value);
      }
    } catch (err) {
      console.error('Failed to load worklist', err);
      setTasks([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadTeamWorkload = async () => {
    try {
      setIsLoading(true);
      const res = await taskApi.getTeamWorkload();
      setTeamWorkload(res || []);
    } catch (err) {
      console.error('Failed to load team workload', err);
      setTeamWorkload([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadTasks = async () => {
    try {
      setIsLoading(true);
      const params: any = { size: 100 };
      if (taskScope === 'MY_TASKS') {
        params.myTasksOnly = true;
      }
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (categoryFilter !== 'ALL') {
        params.taskCategory = categoryFilter;
      }
      if (assigneeFilter !== 'ALL') {
        params.assignedTo = assigneeFilter;
      }

      const res = await taskApi.getAll(params);
      const list = Array.isArray(res) ? res : (res?.content || []);
      setTasks(list);
    } catch (err) {
      console.error('Failed to load tasks', err);
      setTasks([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadClientsAndEmployees = async () => {
    try {
      setIsClientsLoading(true);
      setClientsError(null);
      const [cRes, eRes] = await Promise.allSettled([
        clientApi.getAll({ size: 200 }),
        employeeApi.getAll({ size: 200 }),
      ]);
      if (cRes.status === 'fulfilled' && cRes.value) {
        const cList = Array.isArray(cRes.value) ? cRes.value : (cRes.value?.content || []);
        setClients(cList);
      } else if (cRes.status === 'rejected') {
        console.error('Failed to load clients', cRes.reason);
        setClientsError('Unable to load clients. Please try again.');
      }
      if (eRes.status === 'fulfilled' && eRes.value) {
        const eList = Array.isArray(eRes.value) ? eRes.value : (eRes.value?.content || []);
        setEmployees(eList);
      }
    } catch (err) {
      console.error('Failed to load metadata for task assignment', err);
      setClientsError('Unable to load clients. Please try again.');
    } finally {
      setIsClientsLoading(false);
    }
  };

  const loadClientEngagements = async (clientId: string) => {
    if (!clientId) {
      setClientEngagements([]);
      return;
    }
    try {
      setIsEngagementsLoading(true);
      const res = await engagementApi.getAll({ clientId });
      const list = Array.isArray(res) ? res : (res?.content || []);
      setClientEngagements(list);
    } catch (err) {
      console.error('Failed to load client engagements', err);
      setClientEngagements([]);
    } finally {
      setIsEngagementsLoading(false);
    }
  };

  const handleUpdateStatus = async (taskId: string, newStatus: TaskStatus, blockedReason?: string) => {
    try {
      if (newStatus === 'COMPLETED') {
        const target = tasks.find((t) => t.id === taskId);
        if (target) {
          handleOpenCompleteModal(target);
          return;
        }
      }
      if (newStatus === 'BLOCKED') {
        await taskApi.update(taskId, { status: 'BLOCKED', blockedReason: blockedReason || 'Blocked on client information' });
      } else {
        await taskApi.update(taskId, { status: newStatus, unassign: false });
      }
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else if (activeTab === 'TEAM_WORKLOAD') {
        await loadTeamWorkload();
      } else {
        await loadTasks();
      }
    } catch (err) {
      console.error('Failed to update task status', err);
    }
  };

  const handleQuickPriority = async (taskId: string, priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT') => {
    try {
      await taskApi.updatePriority(taskId, priority);
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else if (activeTab === 'TEAM_WORKLOAD') {
        await loadTeamWorkload();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to update priority: ${err.response?.data?.message || err.message}`);
    }
  };

  const handleOpenCompleteModal = (task: Task) => {
    setCompletingTask(task);
    setActualMinutesInput(task.estimatedMinutes || '');
    setCompletionNotesInput('');
    setIsCompleteModalOpen(true);
  };

  const handleConfirmComplete = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!completingTask) return;
    try {
      setIsSubmitting(true);
      await taskApi.complete(completingTask.id, {
        actualMinutes: actualMinutesInput ? Number(actualMinutesInput) : undefined,
        notes: completionNotesInput.trim() || undefined,
      });
      setIsCompleteModalOpen(false);
      setCompletingTask(null);
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else if (activeTab === 'TEAM_WORKLOAD') {
        await loadTeamWorkload();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to complete task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOpenBlockModal = (task: Task) => {
    setBlockingTask(task);
    setBlockReasonInput(task.blockedReason || 'Waiting for Form 16 / Bank Statements from client');
    setIsBlockModalOpen(true);
  };

  const handleConfirmBlock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!blockingTask) return;
    if (!blockReasonInput.trim()) {
      alert('Please specify a reason why this task is blocked.');
      return;
    }
    try {
      setIsSubmitting(true);
      await taskApi.update(blockingTask.id, {
        status: 'BLOCKED',
        blockedReason: blockReasonInput.trim(),
      });
      setIsBlockModalOpen(false);
      setBlockingTask(null);
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to block task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUnblock = async (task: Task) => {
    try {
      await taskApi.update(task.id, {
        status: 'IN_PROGRESS',
        unassign: false,
      });
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to unblock task: ${err.response?.data?.message || err.message}`);
    }
  };

  const handleOpenEditModal = (task: Task) => {
    setEditingTask(task);
    let matchingEmpId = task.assignedTo || '';
    if (task.assignedTo) {
      const foundEmp = employees.find(
        (e) => e.id === task.assignedTo || (e as any).userId === task.assignedTo || (e.email && task.assigneeEmail && e.email.toLowerCase() === task.assigneeEmail.toLowerCase())
      );
      if (foundEmp) matchingEmpId = foundEmp.id;
    }

    if (task.clientId) {
      loadClientEngagements(task.clientId);
    }

    setEditFormData({
      title: task.title,
      description: task.description || '',
      clientId: task.clientId || '',
      engagementId: task.engagementId || '',
      assignedTo: matchingEmpId,
      taskCategory: (task.taskCategory as any) || (task.category as any) || 'ITR',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'TODO',
      startDate: task.startDate || '',
      dueDate: task.dueDate || '',
      estimatedMinutes: task.estimatedMinutes || '',
      actualMinutes: task.actualMinutes || '',
      blockedReason: task.blockedReason || '',
      complianceId: task.complianceId,
      documentRequestId: task.documentRequestId,
    });
    setIsEditModalOpen(true);
  };

  const handleUpdateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingTask) return;
    if (!editFormData.title.trim()) {
      alert('Please enter a task title.');
      return;
    }

    try {
      setIsSubmitting(true);
      await taskApi.update(editingTask.id, {
        title: editFormData.title.trim(),
        description: editFormData.description.trim() || undefined,
        clientId: editFormData.clientId || undefined,
        engagementId: editFormData.engagementId || undefined,
        assignedTo: editFormData.assignedTo || undefined,
        unassign: !editFormData.assignedTo,
        category: editFormData.taskCategory,
        status: editFormData.status,
        priority: editFormData.priority,
        startDate: editFormData.startDate || undefined,
        dueDate: editFormData.dueDate || undefined,
        estimatedMinutes: editFormData.estimatedMinutes ? Number(editFormData.estimatedMinutes) : undefined,
        actualMinutes: editFormData.actualMinutes ? Number(editFormData.actualMinutes) : undefined,
        blockedReason: editFormData.status === 'BLOCKED' ? editFormData.blockedReason : undefined,
      });

      alert('Task updated & saved successfully!');
      setIsEditModalOpen(false);
      setEditingTask(null);
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else if (activeTab === 'TEAM_WORKLOAD') {
        await loadTeamWorkload();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to update task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim()) {
      alert('Please enter a task title.');
      return;
    }

    try {
      setIsSubmitting(true);
      await taskApi.create({
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        clientId: formData.clientId || undefined,
        engagementId: formData.engagementId || undefined,
        assignedTo: formData.assignedTo || undefined,
        category: formData.taskCategory,
        priority: formData.priority,
        startDate: formData.startDate || undefined,
        dueDate: formData.dueDate,
        estimatedMinutes: formData.estimatedMinutes ? Number(formData.estimatedMinutes) : undefined,
      });

      alert('Task created & assigned successfully!');
      setIsModalOpen(false);
      setFormData({
        title: '',
        description: '',
        clientId: '',
        engagementId: '',
        assignedTo: '',
        taskCategory: 'ITR',
        priority: 'HIGH',
        startDate: new Date().toISOString().split('T')[0],
        dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
        estimatedMinutes: 60,
      });
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else if (activeTab === 'TEAM_WORKLOAD') {
        await loadTeamWorkload();
      } else {
        await loadTasks();
      }
    } catch (err: any) {
      alert(`Failed to create task: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns: Column<Task>[] = [
    {
      header: 'Task Title & Deliverable',
      accessor: (row) => (
        <div className="flex flex-col max-w-md">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="px-1.5 py-0.5 bg-slate-100 text-slate-700 text-[10px] font-bold rounded border font-mono uppercase">
              {row.taskCategory || row.category || 'COMPLIANCE'}
            </span>
            <span className="font-bold text-slate-900 text-xs">{row.title}</span>
          </div>

          {/* Linked Engagement Badge */}
          {row.engagementTitle && (
            <div className="mt-1 flex items-center gap-1 text-[11px] text-teal-700">
              <span className="px-1.5 py-0.2 bg-teal-50 border border-teal-200 rounded font-semibold text-[10px] inline-flex items-center gap-1">
                <Briefcase className="w-2.5 h-2.5" /> {row.engagementTitle}
                {row.engagementCode ? ` (${row.engagementCode})` : ''}
              </span>
            </div>
          )}

          {/* Blocked Warning Banner */}
          {row.status === 'BLOCKED' && (
            <div className="mt-1.5 px-2 py-1 bg-amber-50 border border-amber-300 rounded flex items-center gap-1.5 text-[11px] text-amber-900">
              <ShieldAlert className="w-3.5 h-3.5 text-amber-600 shrink-0" />
              <span className="font-semibold">Blocked:</span>
              <span className="truncate">{row.blockedReason || 'Waiting for client submission'}</span>
            </div>
          )}

          {/* Linked Statutory Obligation Badge */}
          {row.complianceId && (
            <div className="mt-1 flex items-center gap-1 text-[11px] text-indigo-700">
              <span className="px-1.5 py-0.2 bg-indigo-50 border border-indigo-200 rounded font-semibold text-[10px] inline-flex items-center gap-1">
                🏛️ Statutory: {row.complianceTitle || 'Compliance Obligation'}
              </span>
              {row.statutoryDueDate && (
                <span className="text-[10px] text-slate-500 font-mono">
                  (Statutory Due: {row.statutoryDueDate})
                </span>
              )}
            </div>
          )}

          {/* Linked Document Request Progress */}
          {row.documentRequestId && (
            <div className="mt-1 flex items-center gap-1 text-[11px] text-blue-700">
              <span className="px-1.5 py-0.2 bg-blue-50 border border-blue-200 rounded font-semibold text-[10px] inline-flex items-center gap-1">
                📄 Doc Req {row.documentRequestNumber || ''}:{' '}
                <span className="font-bold text-blue-900">
                  {row.documentRequestReceivedCount || 0}/{row.documentRequestItemsCount || 0} docs received
                </span>
              </span>
              <Link
                to="/documents/requests"
                className="text-[10px] text-blue-600 hover:text-blue-800 font-bold underline inline-flex items-center gap-0.5 ml-1"
              >
                View <ExternalLink className="w-2.5 h-2.5" />
              </Link>
            </div>
          )}

          {row.description && (
            <span className="text-[11px] text-slate-500 mt-0.5 line-clamp-1">{row.description}</span>
          )}
        </div>
      ),
    },
    {
      header: 'Client',
      accessor: (row) => (
        <div className="flex items-center gap-1 text-xs">
          <Building className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <span className="font-semibold text-slate-800">{row.clientName || 'Practice General'}</span>
        </div>
      ),
    },
    {
      header: 'Assigned To',
      accessor: (row) => {
        const isMe = row.assigneeEmail && user?.email && row.assigneeEmail.toLowerCase() === user.email.toLowerCase();
        return (
          <div className="flex items-center gap-1.5 text-xs">
            <User className={`w-3.5 h-3.5 shrink-0 ${isMe ? 'text-emerald-600' : 'text-slate-400'}`} />
            <div className="flex flex-col">
              <span className={`font-semibold ${isMe ? 'text-emerald-700 font-bold' : 'text-slate-700'}`}>
                {row.assigneeName || (row.assignedTo ? 'Assigned Staff' : 'Unassigned')}
              </span>
              {isMe && <span className="text-[9px] text-emerald-600 font-mono font-bold">Assigned to You</span>}
            </div>
          </div>
        );
      },
    },
    {
      header: 'Priority',
      accessor: (row) => (
        <select
          value={row.priority}
          onChange={(e) => handleQuickPriority(row.id, e.target.value as any)}
          className={clsx(
            'text-[10px] font-bold px-2 py-0.5 rounded border cursor-pointer uppercase',
            row.priority === 'URGENT'
              ? 'bg-rose-100 text-rose-800 border-rose-300'
              : row.priority === 'HIGH'
              ? 'bg-amber-100 text-amber-800 border-amber-300'
              : row.priority === 'MEDIUM'
              ? 'bg-blue-100 text-blue-800 border-blue-300'
              : 'bg-slate-100 text-slate-700 border-slate-300'
          )}
        >
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="URGENT">Urgent</option>
        </select>
      ),
    },
    {
      header: 'Schedule & Due Date',
      accessor: (row) => {
        const today = new Date().toISOString().split('T')[0];
        const isOverdue = row.isOverdue || (row.dueDate && row.dueDate < today && row.status !== 'COMPLETED' && row.status !== 'CANCELLED');
        const isDueToday = row.isDueToday || (row.dueDate === today && row.status !== 'COMPLETED');

        return (
          <div className="flex flex-col text-xs font-mono">
            {row.startDate && (
              <span className="text-[10px] text-slate-400">Start: {row.startDate}</span>
            )}
            <div className="flex items-center gap-1">
              <Clock className={`w-3.5 h-3.5 ${isOverdue ? 'text-rose-600 animate-pulse' : isDueToday ? 'text-amber-600' : 'text-slate-400'}`} />
              <span className={`font-bold ${isOverdue ? 'text-rose-700 font-extrabold' : isDueToday ? 'text-amber-700' : 'text-slate-700'}`}>
                {row.dueDate || 'No due date'}
              </span>
            </div>
            {isOverdue && <span className="text-[10px] text-rose-600 font-bold">🚨 Overdue</span>}
            {isDueToday && <span className="text-[10px] text-amber-600 font-bold">⚡ Due Today</span>}
          </div>
        );
      },
    },
    {
      header: 'Effort (Est / Act)',
      accessor: (row) => (
        <div className="text-[11px] font-mono">
          <span className="text-slate-700 font-semibold">{row.estimatedMinutes ? `${row.estimatedMinutes}m` : '—'}</span>
          <span className="text-slate-400"> / </span>
          <span className={row.actualMinutes ? 'text-emerald-700 font-bold' : 'text-slate-400'}>
            {row.actualMinutes ? `${row.actualMinutes}m` : '—'}
          </span>
        </div>
      ),
    },
    {
      header: 'Status & Actions',
      accessor: (row) => (
        <div className="flex items-center gap-1.5 flex-wrap">
          <select
            value={row.status}
            onChange={(e) => handleUpdateStatus(row.id, e.target.value as TaskStatus)}
            className="text-xs px-2 py-1 border border-slate-300 rounded-lg bg-white font-semibold text-slate-700 shadow-2xs"
          >
            <option value="TODO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="BLOCKED">🛑 Blocked</option>
            <option value="COMPLETED">Completed</option>
          </select>

          {row.status === 'BLOCKED' ? (
            <button
              onClick={() => handleUnblock(row)}
              className="px-2 py-1 bg-amber-100 hover:bg-amber-200 text-amber-900 rounded text-xs font-bold transition-colors inline-flex items-center gap-1"
            >
              Unblock
            </button>
          ) : row.status !== 'COMPLETED' ? (
            <button
              onClick={() => handleOpenCompleteModal(row)}
              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded text-xs font-bold transition-colors inline-flex items-center gap-1"
            >
              <Check className="w-3 h-3" /> Done
            </button>
          ) : null}

          <button
            onClick={() => {
              setSelectedTask(row);
              setIsDetailModalOpen(true);
            }}
            className="p-1 hover:bg-slate-100 text-slate-500 hover:text-slate-800 rounded transition-colors"
            title="Inspect Task Details"
          >
            <Eye className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={() => handleOpenEditModal(row)}
            className="p-1 hover:bg-slate-100 text-slate-500 hover:text-slate-800 rounded transition-colors"
            title="Edit Task"
          >
            <Edit2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <CheckSquare className="w-7 h-7 text-brand-600" />
            Practice Task & Work Management
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Single unified engine for client deliverables, recurring compliance instances, and team execution.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <Link to="/compliance-calendar">
            <Button variant="outline" className="flex items-center gap-1.5 shadow-xs">
              <Calendar className="w-4 h-4 text-brand-600" />
              Calendar View
            </Button>
          </Link>
          <Link to="/tasks/bulk">
            <Button variant="outline" className="flex items-center gap-1.5 shadow-xs">
              <Layers className="w-4 h-4 text-indigo-600" />
              Bulk Generator
            </Button>
          </Link>
          <Button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 shadow-xs bg-brand-600 hover:bg-brand-700 text-white"
          >
            <Plus className="w-4 h-4" />
            Create Task
          </Button>
        </div>
      </div>

      {/* Main Feature Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-1">
        <button
          onClick={() => setActiveTab('WORKLIST')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold rounded-t-lg transition-all flex items-center gap-2 border-b-2',
            activeTab === 'WORKLIST'
              ? 'border-brand-600 text-brand-700 bg-brand-50/50'
              : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          )}
        >
          <Zap className="w-4 h-4 text-amber-500" />
          Actionable Worklist
          {worklistSummary && (
            <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-brand-100 text-brand-800">
              {worklistScope === 'MY_WORK' ? worklistSummary.myTasksCount : worklistSummary.teamTasksCount}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('TEAM_WORKLOAD')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold rounded-t-lg transition-all flex items-center gap-2 border-b-2',
            activeTab === 'TEAM_WORKLOAD'
              ? 'border-brand-600 text-brand-700 bg-brand-50/50'
              : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          )}
        >
          <Users className="w-4 h-4 text-indigo-600" />
          Team Workload
        </button>

        <button
          onClick={() => setActiveTab('ALL_TASKS')}
          className={clsx(
            'px-4 py-2.5 text-xs font-bold rounded-t-lg transition-all flex items-center gap-2 border-b-2',
            activeTab === 'ALL_TASKS'
              ? 'border-brand-600 text-brand-700 bg-brand-50/50'
              : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          )}
        >
          <List className="w-4 h-4 text-slate-500" />
          All Practice Tasks & Board
        </button>
      </div>

      {/* =========================================================================
          TAB 1: ACTIONABLE WORKLIST
          ========================================================================= */}
      {activeTab === 'WORKLIST' && (
        <div className="space-y-6">
          {/* Worklist Stat Cards */}
          {worklistSummary && (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
              <div
                onClick={() => setWorklistBucket('OVERDUE')}
                className={clsx(
                  'p-3 rounded-xl border transition-all cursor-pointer',
                  worklistBucket === 'OVERDUE'
                    ? 'border-rose-500 bg-rose-100/60 shadow-sm ring-2 ring-rose-400'
                    : 'border-rose-200 bg-rose-50/50 hover:bg-rose-50'
                )}
              >
                <div className="flex items-center justify-between text-rose-800">
                  <span className="text-[11px] font-bold uppercase">🚨 Overdue</span>
                  <AlertTriangle className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-rose-700 mt-1">{worklistSummary.overdueCount}</p>
              </div>

              <div
                onClick={() => setWorklistBucket('DUE_TODAY')}
                className={clsx(
                  'p-3 rounded-xl border transition-all cursor-pointer',
                  worklistBucket === 'DUE_TODAY'
                    ? 'border-amber-500 bg-amber-100/60 shadow-sm ring-2 ring-amber-400'
                    : 'border-amber-200 bg-amber-50/50 hover:bg-amber-50'
                )}
              >
                <div className="flex items-center justify-between text-amber-800">
                  <span className="text-[11px] font-bold uppercase">📅 Due Today</span>
                  <Clock className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-amber-700 mt-1">{worklistSummary.dueTodayCount}</p>
              </div>

              <div
                onClick={() => setWorklistBucket('DUE_THIS_WEEK')}
                className={clsx(
                  'p-3 rounded-xl border transition-all cursor-pointer',
                  worklistBucket === 'DUE_THIS_WEEK'
                    ? 'border-blue-500 bg-blue-100/60 shadow-sm ring-2 ring-blue-400'
                    : 'border-blue-200 bg-blue-50/50 hover:bg-blue-50'
                )}
              >
                <div className="flex items-center justify-between text-blue-800">
                  <span className="text-[11px] font-bold uppercase">⏳ Due This Week</span>
                  <Calendar className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-blue-700 mt-1">{worklistSummary.dueThisWeekCount}</p>
              </div>

              <div
                onClick={() => setWorklistBucket('BLOCKED')}
                className={clsx(
                  'p-3 rounded-xl border transition-all cursor-pointer',
                  worklistBucket === 'BLOCKED'
                    ? 'border-amber-500 bg-amber-100/60 shadow-sm ring-2 ring-amber-400'
                    : 'border-amber-200 bg-amber-50/50 hover:bg-amber-50'
                )}
              >
                <div className="flex items-center justify-between text-amber-800">
                  <span className="text-[11px] font-bold uppercase">🛑 Blocked on Docs</span>
                  <ShieldAlert className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-amber-700 mt-1">{worklistSummary.blockedCount}</p>
              </div>

              <div
                onClick={() => setWorklistBucket('COMPLETED')}
                className={clsx(
                  'p-3 rounded-xl border transition-all cursor-pointer',
                  worklistBucket === 'COMPLETED'
                    ? 'border-emerald-500 bg-emerald-100/60 shadow-sm ring-2 ring-emerald-400'
                    : 'border-emerald-200 bg-emerald-50/50 hover:bg-emerald-50'
                )}
              >
                <div className="flex items-center justify-between text-emerald-800">
                  <span className="text-[11px] font-bold uppercase">✅ Completed Today</span>
                  <CheckCircle2 className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-emerald-700 mt-1">{worklistSummary.completedTodayCount}</p>
              </div>

              <div className="p-3 rounded-xl border border-slate-200 bg-slate-50">
                <div className="flex items-center justify-between text-slate-700">
                  <span className="text-[11px] font-bold uppercase">📄 Docs Awaiting</span>
                  <FileText className="w-4 h-4" />
                </div>
                <p className="text-2xl font-black text-slate-800 mt-1">{worklistSummary.documentsWaitingCount}</p>
              </div>
            </div>
          )}

          {/* Scope and Bucket Filter Strip */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3 flex-wrap">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setWorklistScope('MY_WORK')}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                  worklistScope === 'MY_WORK'
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                <UserCheck className="w-3.5 h-3.5 text-emerald-400" /> My Worklist ({worklistSummary?.myTasksCount || 0})
              </button>

              {!isStaff && (
                <button
                  onClick={() => setWorklistScope('TEAM_WORK')}
                  className={clsx(
                    'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                    worklistScope === 'TEAM_WORK'
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  <Building className="w-3.5 h-3.5 text-indigo-400" /> Entire Practice Worklist ({worklistSummary?.teamTasksCount || 0})
                </button>
              )}
            </div>

            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {[
                { id: 'ALL', label: 'All Active' },
                { id: 'OVERDUE', label: '🚨 Overdue' },
                { id: 'DUE_TODAY', label: '📅 Due Today' },
                { id: 'DUE_THIS_WEEK', label: '⏳ Due This Week' },
                { id: 'BLOCKED', label: '🛑 Blocked' },
                { id: 'COMPLETED', label: '✅ Completed' },
              ].map((bucket) => (
                <button
                  key={bucket.id}
                  onClick={() => setWorklistBucket(bucket.id as any)}
                  className={clsx(
                    'px-2.5 py-1 rounded-md text-[11px] font-bold transition-colors whitespace-nowrap',
                    worklistBucket === bucket.id
                      ? 'bg-brand-600 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  {bucket.label}
                </button>
              ))}
            </div>
          </div>

          <DataTable
            columns={columns}
            data={tasks}
            isLoading={isLoading}
            searchPlaceholder="Search worklist by client, deliverable, engagement, or statutory link..."
          />
        </div>
      )}

      {/* =========================================================================
          TAB 2: TEAM WORKLOAD
          ========================================================================= */}
      {activeTab === 'TEAM_WORKLOAD' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between text-slate-600">
                <span className="text-[11px] font-bold uppercase tracking-wider">Active Staff</span>
                <User className="w-4 h-4 text-brand-600" />
              </div>
              <p className="text-2xl font-black text-slate-900 mt-1">{teamWorkload.length}</p>
              <p className="text-[11px] text-slate-500 mt-0.5">Staff & Article Assistants</p>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between text-slate-600">
                <span className="text-[11px] font-bold uppercase tracking-wider">Total Active Load</span>
                <Layers className="w-4 h-4 text-indigo-600" />
              </div>
              <p className="text-2xl font-black text-indigo-700 mt-1">
                {teamWorkload.reduce((acc, w) => acc + w.todoCount + w.inProgressCount + w.underReviewCount + w.blockedCount, 0)}
              </p>
              <p className="text-[11px] text-slate-500 mt-0.5">Open deliverables across firm</p>
            </div>

            <div className="bg-white p-4 rounded-xl border border-rose-200 shadow-xs bg-rose-50/30">
              <div className="flex items-center justify-between text-rose-800">
                <span className="text-[11px] font-bold uppercase tracking-wider">Total Overdue</span>
                <AlertTriangle className="w-4 h-4 text-rose-600" />
              </div>
              <p className="text-2xl font-black text-rose-700 mt-1">
                {teamWorkload.reduce((acc, w) => acc + w.overdueCount, 0)}
              </p>
              <p className="text-[11px] text-rose-600 mt-0.5 font-medium">Requires partner attention</p>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between text-slate-600">
                <span className="text-[11px] font-bold uppercase tracking-wider">Estimated vs Actual</span>
                <Clock className="w-4 h-4 text-emerald-600" />
              </div>
              <div className="flex items-baseline gap-2 mt-1">
                <p className="text-xl font-black text-slate-900">
                  {(teamWorkload.reduce((acc, w) => acc + (w.totalEstimatedMinutes || 0), 0) / 60).toFixed(1)}h
                </p>
                <span className="text-xs text-slate-400">/</span>
                <p className="text-sm font-bold text-emerald-700">
                  {(teamWorkload.reduce((acc, w) => acc + (w.totalActualMinutes || 0), 0) / 60).toFixed(1)}h spent
                </p>
              </div>
              <p className="text-[11px] text-slate-500 mt-0.5">Total logged effort</p>
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">Team Capacity & Work Allocation</h3>
                <p className="text-xs text-slate-500">Live operational view of tasks assigned per team member</p>
              </div>
              <Button size="sm" variant="outline" onClick={loadTeamWorkload}>
                ↻ Refresh Load
              </Button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200 uppercase tracking-wider text-[10px]">
                  <tr>
                    <th className="px-6 py-3">Team Member</th>
                    <th className="px-4 py-3">Total Assigned</th>
                    <th className="px-4 py-3 text-slate-600">To Do</th>
                    <th className="px-4 py-3 text-blue-600">In Progress</th>
                    <th className="px-4 py-3 text-purple-600">Under Review</th>
                    <th className="px-4 py-3 text-amber-600">Blocked</th>
                    <th className="px-4 py-3 text-emerald-600">Completed</th>
                    <th className="px-4 py-3 text-rose-600">Overdue</th>
                    <th className="px-4 py-3">Est. Time</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {teamWorkload.map((member) => (
                    <tr key={member.employeeId || member.email} className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-3">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-brand-100 text-brand-800 font-bold flex items-center justify-center text-xs">
                            {member.name ? member.name.charAt(0).toUpperCase() : 'U'}
                          </div>
                          <div>
                            <p className="font-bold text-slate-900">{member.name}</p>
                            <p className="text-[11px] text-slate-500">{member.designation || 'Staff'} • {member.department || 'Direct Tax'}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 font-bold text-slate-900">{member.totalAssigned}</td>
                      <td className="px-4 py-3 font-semibold text-slate-600">{member.todoCount}</td>
                      <td className="px-4 py-3 font-semibold text-blue-700">{member.inProgressCount}</td>
                      <td className="px-4 py-3 font-semibold text-purple-700">{member.underReviewCount}</td>
                      <td className="px-4 py-3">
                        {member.blockedCount > 0 ? (
                          <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold text-[10px]">
                            {member.blockedCount} Blocked
                          </span>
                        ) : (
                          <span className="text-slate-400">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 font-semibold text-emerald-700">{member.completedCount}</td>
                      <td className="px-4 py-3">
                        {member.overdueCount > 0 ? (
                          <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 font-bold text-[10px]">
                            🚨 {member.overdueCount} Overdue
                          </span>
                        ) : (
                          <span className="text-slate-400">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 font-mono text-slate-600">
                        {((member.totalEstimatedMinutes || 0) / 60).toFixed(1)} hrs
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            setWorklistAssignee(member.userId || member.employeeId || '');
                            setWorklistScope('TEAM_WORK');
                            setActiveTab('WORKLIST');
                          }}
                        >
                          View Tasks →
                        </Button>
                      </td>
                    </tr>
                  ))}
                  {teamWorkload.length === 0 && !isLoading && (
                    <tr>
                      <td colSpan={10} className="px-6 py-8 text-center text-slate-500">
                        No team workload data available.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 3: ALL TASKS & PRACTICE BOARD
          ========================================================================= */}
      {activeTab === 'ALL_TASKS' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3 flex-wrap">
            <div className="flex items-center gap-2 flex-wrap">
              <button
                onClick={() => setTaskScope('MY_TASKS')}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                  taskScope === 'MY_TASKS'
                    ? 'bg-brand-600 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                <UserCheck className="w-3.5 h-3.5" /> 🎯 {isStaff ? 'My Assigned Deliverables' : 'My Assigned Tasks'}
              </button>

              {!isStaff && (
                <button
                  onClick={() => setTaskScope('ALL_TASKS')}
                  className={clsx(
                    'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                    taskScope === 'ALL_TASKS'
                      ? 'bg-brand-600 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  <Building className="w-3.5 h-3.5" /> 🏢 All Practice Tasks
                </button>
              )}

              {!isStaff && assigneeOptions.length > 0 && (
                <div className="flex items-center gap-1 pl-2 border-l border-slate-200">
                  <span className="text-[11px] font-semibold text-slate-500">Staff:</span>
                  <select
                    value={assigneeFilter}
                    onChange={(e) => setAssigneeFilter(e.target.value)}
                    className="text-xs px-2 py-1 border border-slate-300 rounded-lg bg-white font-medium text-slate-700"
                  >
                    <option value="ALL">All Staff & Assignees</option>
                    {assigneeOptions.map((opt) => (
                      <option key={opt.id} value={opt.id}>
                        {opt.name} {opt.isMe ? '⭐ (You)' : ''} ({opt.designation})
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              <div className="bg-slate-100 p-1 rounded-lg border border-slate-200 flex items-center">
                <button
                  onClick={() => setViewMode('list')}
                  className={clsx('p-1 rounded text-xs font-medium', viewMode === 'list' ? 'bg-white shadow-xs' : 'text-slate-500')}
                >
                  <List className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => setViewMode('kanban')}
                  className={clsx('p-1 rounded text-xs font-medium', viewMode === 'kanban' ? 'bg-white shadow-xs' : 'text-slate-500')}
                >
                  <LayoutGrid className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
                {(['ALL', 'TODO', 'IN_PROGRESS', 'UNDER_REVIEW', 'BLOCKED', 'COMPLETED'] as const).map((status) => (
                  <button
                    key={status}
                    onClick={() => setStatusFilter(status)}
                    className={clsx(
                      'px-2.5 py-1 rounded-md text-[11px] font-bold transition-colors whitespace-nowrap',
                      statusFilter === status
                        ? 'bg-slate-900 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    )}
                  >
                    {status === 'ALL'
                      ? 'All'
                      : status === 'TODO'
                      ? 'To Do'
                      : status === 'IN_PROGRESS'
                      ? 'In Progress'
                      : status === 'UNDER_REVIEW'
                      ? 'Review'
                      : status === 'BLOCKED'
                      ? '🛑 Blocked'
                      : 'Completed'}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <DataTable
            columns={columns}
            data={tasks}
            isLoading={isLoading}
            searchPlaceholder="Search all tasks..."
          />
        </div>
      )}

      {/* =========================================================================
          COMPLETE TASK MODAL (P0.3 ACTUAL TIME & COMPLETION NOTES)
          ========================================================================= */}
      <Modal
        isOpen={isCompleteModalOpen}
        onClose={() => {
          setIsCompleteModalOpen(false);
          setCompletingTask(null);
        }}
        title="Mark Task as Completed"
        subtitle={`Record final completion details for "${completingTask?.title || ''}"`}
        maxWidth="md"
      >
        <form onSubmit={handleConfirmComplete} className="space-y-4">
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg flex items-center gap-3">
            <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0" />
            <div>
              <p className="text-xs font-bold text-emerald-950">{completingTask?.title}</p>
              <p className="text-[11px] text-emerald-700">
                Client: {completingTask?.clientName || 'General Practice'}
                {completingTask?.engagementTitle ? ` • Engagement: ${completingTask.engagementTitle}` : ''}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Estimated Time</label>
              <input
                type="text"
                disabled
                value={completingTask?.estimatedMinutes ? `${completingTask.estimatedMinutes} mins (${(completingTask.estimatedMinutes / 60).toFixed(1)} hrs)` : 'Not set'}
                className="w-full text-xs px-3 py-2 border border-slate-200 bg-slate-100 rounded-lg text-slate-500 font-mono"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Actual Time Spent (Mins)</label>
              <input
                type="number"
                min={0}
                placeholder="e.g. 45"
                value={actualMinutesInput}
                onChange={(e) => setActualMinutesInput(e.target.value ? parseInt(e.target.value, 10) : '')}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Completion Notes / Working Paper References (Optional)</label>
            <textarea
              rows={3}
              placeholder="e.g. Verified against Form 26AS and AIS. All reconciliation differences resolved."
              value={completionNotesInput}
              onChange={(e) => setCompletionNotesInput(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button
              variant="outline"
              onClick={() => {
                setIsCompleteModalOpen(false);
                setCompletingTask(null);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting} className="bg-emerald-600 hover:bg-emerald-700 text-white">
              Confirm & Mark Completed
            </Button>
          </div>
        </form>
      </Modal>

      {/* =========================================================================
          TASK DETAILS & INSPECT MODAL
          ========================================================================= */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setSelectedTask(null);
        }}
        title="Deliverable & Execution Details"
        subtitle={selectedTask?.title}
        maxWidth="lg"
      >
        {selectedTask && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 p-3 bg-slate-50 rounded-lg border border-slate-200">
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Client</p>
                <p className="font-bold text-slate-800">{selectedTask.clientName || 'Practice General'}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Linked Engagement</p>
                <p className="font-bold text-teal-700">
                  {selectedTask.engagementTitle || 'Standalone Task'}
                  {selectedTask.engagementCode ? ` (${selectedTask.engagementCode})` : ''}
                </p>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Category & Priority</p>
                <p className="font-semibold text-slate-700">{selectedTask.taskCategory || selectedTask.category || 'OTHER'} • {selectedTask.priority}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Status</p>
                <StatusBadge status={selectedTask.status} />
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Timeline</p>
                <p className="font-mono text-slate-700">
                  {selectedTask.startDate ? `${selectedTask.startDate} → ` : ''}{selectedTask.dueDate || 'No due date'}
                </p>
              </div>
              <div>
                <p className="text-[10px] font-bold uppercase text-slate-400">Logged Effort</p>
                <p className="font-mono text-slate-700">
                  Est: {selectedTask.estimatedMinutes ? `${selectedTask.estimatedMinutes}m` : '—'} | Act: {selectedTask.actualMinutes ? `${selectedTask.actualMinutes}m` : '—'}
                </p>
              </div>
            </div>

            {selectedTask.completedBy && (
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-900">
                <p className="text-[10px] font-bold uppercase text-emerald-700">Completed Information</p>
                <p className="mt-0.5">
                  Completed by <span className="font-bold">{selectedTask.completedByName || 'Team Member'}</span>
                  {selectedTask.completedAt ? ` on ${new Date(selectedTask.completedAt).toLocaleString()}` : ''}
                </p>
              </div>
            )}

            {selectedTask.blockedReason && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-900">
                <p className="text-[10px] font-bold uppercase text-amber-700">Blocked Reason</p>
                <p className="mt-0.5">{selectedTask.blockedReason}</p>
              </div>
            )}

            {selectedTask.notes && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-800">
                <p className="text-[10px] font-bold uppercase text-slate-500">Notes / Working Papers</p>
                <p className="mt-0.5 whitespace-pre-wrap">{selectedTask.notes}</p>
              </div>
            )}

            <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
              <Button variant="outline" onClick={() => setIsDetailModalOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* =========================================================================
          CREATE STANDALONE TASK MODAL (P0.3 ENGAGEMENT & SCHEDULE SUPPORT)
          ========================================================================= */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Practice Task"
        subtitle="Create a deliverable linked to a client engagement or standalone workflow"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              required
              placeholder="e.g. Prepare Draft Compute of Income or GSTR-1 Verification"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Client (Optional)</label>
              <select
                value={formData.clientId}
                onChange={(e) => {
                  const val = e.target.value;
                  setFormData({ ...formData, clientId: val, engagementId: '' });
                  loadClientEngagements(val);
                }}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
                disabled={isClientsLoading}
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName || c.legalName || c.tradeName || 'Unnamed Client'}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Linked Engagement (Optional)</label>
              <select
                value={formData.engagementId}
                onChange={(e) => setFormData({ ...formData, engagementId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
                disabled={!formData.clientId || isEngagementsLoading}
              >
                <option value="">-- Standalone (No Engagement) --</option>
                {clientEngagements.map((eng) => (
                  <option key={eng.id} value={eng.id}>
                    {eng.name} {eng.engagementCode ? `(${eng.engagementCode})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Assignee</label>
              <select
                value={formData.assignedTo}
                onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- Unassigned / General Pool --</option>
                {assigneeOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.name} {opt.isMe ? '⭐ (You)' : ''} — {opt.designation}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
              <select
                value={formData.taskCategory}
                onChange={(e) => setFormData({ ...formData, taskCategory: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="ITR">Income Tax (ITR)</option>
                <option value="GST">GST Compliance</option>
                <option value="TDS">TDS Return</option>
                <option value="AUDIT">Tax Audit / 3CD</option>
                <option value="COMPLIANCE">ROC / Other Compliance</option>
                <option value="BILLING">Billing & Fees</option>
                <option value="OTHER">Other Assignment</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
              <select
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Date</label>
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date *</label>
              <input
                type="date"
                required
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Est. Minutes</label>
              <input
                type="number"
                min={0}
                placeholder="60"
                value={formData.estimatedMinutes}
                onChange={(e) => setFormData({ ...formData, estimatedMinutes: e.target.value ? parseInt(e.target.value, 10) : '' })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Notes</label>
            <textarea
              rows={2}
              placeholder="Instructions or reference details..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              Create Task
            </Button>
          </div>
        </form>
      </Modal>

      {/* =========================================================================
          EDIT TASK MODAL
          ========================================================================= */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingTask(null);
        }}
        title="Edit & Reassign Task"
        subtitle="Update deliverable metadata, engagement link, and dates"
        maxWidth="lg"
      >
        <form onSubmit={handleUpdateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              required
              value={editFormData.title}
              onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Client</label>
              <select
                value={editFormData.clientId}
                onChange={(e) => {
                  const val = e.target.value;
                  setEditFormData({ ...editFormData, clientId: val, engagementId: '' });
                  loadClientEngagements(val);
                }}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName || c.legalName || 'Client'}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Engagement</label>
              <select
                value={editFormData.engagementId}
                onChange={(e) => setEditFormData({ ...editFormData, engagementId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- Standalone (No Engagement) --</option>
                {clientEngagements.map((eng) => (
                  <option key={eng.id} value={eng.id}>
                    {eng.name} {eng.engagementCode ? `(${eng.engagementCode})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Assignee</label>
              <select
                value={editFormData.assignedTo}
                onChange={(e) => setEditFormData({ ...editFormData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- Unassigned --</option>
                {assigneeOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.name} {opt.isMe ? '⭐ (You)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Status</label>
              <select
                value={editFormData.status}
                onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white font-semibold"
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="BLOCKED">🛑 Blocked</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
              <select
                value={editFormData.priority}
                onChange={(e) => setEditFormData({ ...editFormData, priority: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Date</label>
              <input
                type="date"
                value={editFormData.startDate}
                onChange={(e) => setEditFormData({ ...editFormData, startDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={editFormData.dueDate}
                onChange={(e) => setEditFormData({ ...editFormData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Est. Minutes</label>
              <input
                type="number"
                min={0}
                value={editFormData.estimatedMinutes}
                onChange={(e) => setEditFormData({ ...editFormData, estimatedMinutes: e.target.value ? parseInt(e.target.value, 10) : '' })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Act. Minutes</label>
              <input
                type="number"
                min={0}
                value={editFormData.actualMinutes}
                onChange={(e) => setEditFormData({ ...editFormData, actualMinutes: e.target.value ? parseInt(e.target.value, 10) : '' })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button
              variant="outline"
              onClick={() => {
                setIsEditModalOpen(false);
                setEditingTask(null);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
