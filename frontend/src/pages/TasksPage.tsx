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
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { taskApi, clientApi, employeeApi, complianceApi, documentRequestApi } from '../api/endpoints';
import { Task, Client, Employee, WorklistSummary, TaskWorklistParams, TaskStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const TasksPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>('list');
  const [activeTab, setActiveTab] = useState<'WORKLIST' | 'ALL_TASKS'>(
    () => (searchParams.get('tab') as 'WORKLIST' | 'ALL_TASKS') || 'WORKLIST'
  );
  const [isModalOpen, setIsModalOpen] = useState(false);

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

  // Detail / Inspect Task Modal State
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

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
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'TDS' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    status: TaskStatus;
    dueDate: string;
    blockedReason: string;
    complianceId?: string;
    documentRequestId?: string;
  }>({
    title: '',
    description: '',
    clientId: '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    status: 'TODO',
    dueDate: '',
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
    assignedTo: string;
    taskCategory: 'GST' | 'ITR' | 'TDS' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    dueDate: string;
  }>({
    title: '',
    description: '',
    clientId: '',
    assignedTo: '',
    taskCategory: 'ITR',
    priority: 'HIGH',
    dueDate: (() => {
      const d = new Date();
      d.setDate(d.getDate() + 7);
      return d.toISOString().split('T')[0];
    })(),
  });

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
      const [cRes, eRes] = await Promise.allSettled([
        clientApi.getAll({ size: 200 }),
        employeeApi.getAll({ size: 200 }),
      ]);
      if (cRes.status === 'fulfilled' && cRes.value) {
        const cList = Array.isArray(cRes.value) ? cRes.value : (cRes.value?.content || []);
        setClients(cList);
      }
      if (eRes.status === 'fulfilled' && eRes.value) {
        const eList = Array.isArray(eRes.value) ? eRes.value : (eRes.value?.content || []);
        setEmployees(eList);
      }
    } catch (err) {
      console.error('Failed to load metadata for task assignment', err);
    }
  };

  const handleUpdateStatus = async (taskId: string, newStatus: TaskStatus, blockedReason?: string) => {
    try {
      if (newStatus === 'BLOCKED') {
        await taskApi.update(taskId, { status: 'BLOCKED', blockedReason: blockedReason || 'Blocked on client information' });
      } else {
        await taskApi.update(taskId, { status: newStatus, unassign: false });
      }
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
      } else {
        await loadTasks();
      }
    } catch (err) {
      console.error('Failed to update task status', err);
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

    setEditFormData({
      title: task.title,
      description: task.description || '',
      clientId: task.clientId || '',
      assignedTo: matchingEmpId,
      taskCategory: (task.category as any) || 'ITR',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'TODO',
      dueDate: task.dueDate || '',
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
        assignedTo: editFormData.assignedTo || undefined,
        unassign: !editFormData.assignedTo,
        category: editFormData.taskCategory,
        status: editFormData.status,
        priority: editFormData.priority,
        dueDate: editFormData.dueDate || undefined,
        blockedReason: editFormData.status === 'BLOCKED' ? editFormData.blockedReason : undefined,
      });

      alert('Task updated & saved successfully!');
      setIsEditModalOpen(false);
      setEditingTask(null);
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
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
        assignedTo: formData.assignedTo || undefined,
        category: formData.taskCategory,
        priority: formData.priority,
        dueDate: formData.dueDate,
      });

      alert('Task created & assigned successfully!');
      setIsModalOpen(false);
      setFormData({
        title: '',
        description: '',
        clientId: '',
        assignedTo: '',
        taskCategory: 'ITR',
        priority: 'HIGH',
        dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
      });
      if (activeTab === 'WORKLIST') {
        await loadWorklist();
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
      header: 'Task Title & Category',
      accessor: (row) => (
        <div className="flex flex-col max-w-md">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="px-1.5 py-0.5 bg-slate-100 text-slate-700 text-[10px] font-bold rounded border font-mono uppercase">
              {row.category || 'COMPLIANCE'}
            </span>
            <span className="font-bold text-slate-900 text-xs">{row.title}</span>
          </div>

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
      accessor: (row) => {
        const colors: Record<string, string> = {
          URGENT: 'text-rose-700 bg-rose-50 border-rose-200',
          HIGH: 'text-amber-700 bg-amber-50 border-amber-200',
          MEDIUM: 'text-blue-700 bg-blue-50 border-blue-200',
          LOW: 'text-slate-700 bg-slate-50 border-slate-200',
        };
        return (
          <span className={clsx('px-2 py-0.5 text-[10px] font-bold rounded border tracking-wider uppercase', colors[row.priority] || colors.MEDIUM)}>
            {row.priority}
          </span>
        );
      },
    },
    {
      header: 'Deadlines',
      accessor: (row) => {
        const isOverdue = Boolean(row.isOverdue || (row.dueDate && new Date(row.dueDate) < new Date() && row.status !== 'COMPLETED'));
        const isDueToday = Boolean(row.isDueToday);
        return (
          <div className="flex flex-col gap-0.5 font-mono text-xs">
            <div className="flex items-center gap-1">
              {isOverdue && <AlertCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />}
              {isDueToday && <Flame className="w-3.5 h-3.5 text-amber-600 shrink-0" />}
              <span className={clsx('font-bold', isOverdue ? 'text-rose-600' : isDueToday ? 'text-amber-600' : 'text-slate-700')}>
                Internal: {row.dueDate || 'No Due Date'}
              </span>
            </div>
            {row.statutoryDueDate && (
              <span className="text-[10px] text-slate-400">
                Statutory: {row.statutoryDueDate}
              </span>
            )}
          </div>
        );
      },
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5 flex-wrap">
          <button
            onClick={() => {
              setSelectedTask(row);
              setIsDetailModalOpen(true);
            }}
            className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded"
            title="Inspect Details"
          >
            <Eye className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => handleOpenEditModal(row)}
            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 rounded text-xs font-semibold inline-flex items-center gap-1"
            title="Edit & Reassign Task"
          >
            <Edit2 className="w-3 h-3 text-slate-500" /> Edit
          </button>
          {row.status === 'BLOCKED' ? (
            <button
              onClick={() => handleUnblock(row)}
              className="px-2 py-1 bg-amber-600 hover:bg-amber-700 text-white rounded text-xs font-bold shadow-2xs"
            >
              Unblock
            </button>
          ) : (
            <>
              {row.status === 'TODO' && (
                <button
                  onClick={() => handleUpdateStatus(row.id, 'IN_PROGRESS')}
                  className="px-2 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold"
                >
                  Start
                </button>
              )}
              {row.status === 'IN_PROGRESS' && (
                <>
                  <button
                    onClick={() => handleUpdateStatus(row.id, 'UNDER_REVIEW')}
                    className="px-2 py-1 bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 rounded text-xs font-semibold"
                  >
                    Review
                  </button>
                  <button
                    onClick={() => handleOpenBlockModal(row)}
                    className="px-1.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-300 rounded text-xs font-semibold"
                    title="Mark Blocked on Documents"
                  >
                    Block
                  </button>
                </>
              )}
              {row.status === 'UNDER_REVIEW' && (
                <button
                  onClick={() => handleUpdateStatus(row.id, 'COMPLETED')}
                  className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-xs font-semibold inline-flex items-center gap-1"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" /> Approve
                </button>
              )}
              {row.status === 'COMPLETED' && (
                <span className="text-xs text-emerald-600 font-bold inline-flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Done
                </span>
              )}
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">Task & Compliance Hub</h1>
            {user?.email && (
              <span className="px-2.5 py-0.5 bg-brand-50 text-brand-700 text-xs font-bold rounded-full border border-brand-200">
                {user.firstName || user.email.split('@')[0]}
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Real-time practitioner worklist: answers "What needs attention today?" across client deliverables, statutory deadlines, and document requests.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Main Mode Tabs */}
          <div className="bg-slate-100 p-1 rounded-lg border border-slate-200 flex items-center">
            <button
              onClick={() => setActiveTab('WORKLIST')}
              className={clsx(
                'px-3 py-1.5 rounded-md text-xs font-bold transition-all flex items-center gap-1.5',
                activeTab === 'WORKLIST' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              )}
            >
              <Zap className="w-3.5 h-3.5 text-amber-500" /> Unified Worklist
            </button>
            <button
              onClick={() => setActiveTab('ALL_TASKS')}
              className={clsx(
                'px-3 py-1.5 rounded-md text-xs font-bold transition-all flex items-center gap-1.5',
                activeTab === 'ALL_TASKS' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              )}
            >
              <Layers className="w-3.5 h-3.5 text-indigo-500" /> All Tasks & Kanban
            </button>
          </div>

          <Link to="/tasks/bulk">
            <Button variant="outline" leftIcon={<Sparkles className="w-4 h-4 text-brand-600" />}>
              ⚡ Bulk Tasks
            </Button>
          </Link>

          <Button onClick={() => setIsModalOpen(true)} leftIcon={<Plus className="w-4 h-4" />}>
            Create Task
          </Button>
        </div>
      </div>

      {/* =========================================================================
          WORKLIST SUMMARY METRIC CARDS
          ========================================================================= */}
      {activeTab === 'WORKLIST' && worklistSummary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div
            onClick={() => setWorklistBucket('OVERDUE')}
            className={clsx(
              'p-3.5 rounded-xl border transition-all cursor-pointer shadow-2xs',
              worklistBucket === 'OVERDUE' ? 'bg-rose-50 border-rose-300 ring-2 ring-rose-400' : 'bg-white border-slate-200 hover:border-rose-200'
            )}
          >
            <div className="flex items-center justify-between text-rose-600">
              <span className="text-[11px] font-bold uppercase">🚨 Overdue</span>
              <AlertCircle className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-rose-700 mt-1">{worklistSummary.overdueCount}</p>
          </div>

          <div
            onClick={() => setWorklistBucket('DUE_TODAY')}
            className={clsx(
              'p-3.5 rounded-xl border transition-all cursor-pointer shadow-2xs',
              worklistBucket === 'DUE_TODAY' ? 'bg-amber-50 border-amber-300 ring-2 ring-amber-400' : 'bg-white border-slate-200 hover:border-amber-200'
            )}
          >
            <div className="flex items-center justify-between text-amber-600">
              <span className="text-[11px] font-bold uppercase">📅 Due Today</span>
              <Flame className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-amber-700 mt-1">{worklistSummary.dueTodayCount}</p>
          </div>

          <div
            onClick={() => setWorklistBucket('DUE_THIS_WEEK')}
            className={clsx(
              'p-3.5 rounded-xl border transition-all cursor-pointer shadow-2xs',
              worklistBucket === 'DUE_THIS_WEEK' ? 'bg-blue-50 border-blue-300 ring-2 ring-blue-400' : 'bg-white border-slate-200 hover:border-blue-200'
            )}
          >
            <div className="flex items-center justify-between text-blue-600">
              <span className="text-[11px] font-bold uppercase">⏳ Due This Week</span>
              <Clock className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-blue-700 mt-1">{worklistSummary.dueThisWeekCount}</p>
          </div>

          <div
            onClick={() => setWorklistBucket('BLOCKED')}
            className={clsx(
              'p-3.5 rounded-xl border transition-all cursor-pointer shadow-2xs',
              worklistBucket === 'BLOCKED' ? 'bg-orange-50 border-orange-300 ring-2 ring-orange-400' : 'bg-white border-slate-200 hover:border-orange-200'
            )}
          >
            <div className="flex items-center justify-between text-orange-600">
              <span className="text-[11px] font-bold uppercase">🛑 Blocked on Docs</span>
              <ShieldAlert className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-orange-700 mt-1">{worklistSummary.blockedCount}</p>
          </div>

          <div className="p-3.5 rounded-xl border bg-white border-slate-200 shadow-2xs">
            <div className="flex items-center justify-between text-purple-600">
              <span className="text-[11px] font-bold uppercase">⚙️ In Progress</span>
              <Layers className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-purple-700 mt-1">{worklistSummary.inProgressCount}</p>
          </div>

          <div className="p-3.5 rounded-xl border bg-white border-slate-200 shadow-2xs">
            <div className="flex items-center justify-between text-emerald-600">
              <span className="text-[11px] font-bold uppercase">📄 Docs Awaiting</span>
              <FileText className="w-4 h-4" />
            </div>
            <p className="text-2xl font-black text-emerald-700 mt-1">{worklistSummary.documentsWaitingCount}</p>
          </div>
        </div>
      )}

      {/* =========================================================================
          WORKLIST CONTROLS & BUCKETS
          ========================================================================= */}
      {activeTab === 'WORKLIST' ? (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3 flex-wrap">
          {/* Scope Toggle */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setWorklistScope('MY_WORK')}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5',
                worklistScope === 'MY_WORK'
                  ? 'bg-slate-900 text-white shadow-sm'
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
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                )}
              >
                <Building className="w-3.5 h-3.5 text-indigo-400" /> Entire Practice Worklist ({worklistSummary?.teamTasksCount || 0})
              </button>
            )}
          </div>

          {/* Bucket Tabs */}
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
      ) : (
        /* Standard View Filters */
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3 flex-wrap">
          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={() => setTaskScope('MY_TASKS')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                taskScope === 'MY_TASKS'
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <UserCheck className="w-3.5 h-3.5" /> 🎯 {isStaff ? 'My Assigned Deliverables' : 'My Assigned Tasks'}
            </button>

            {!isStaff && (
              <button
                onClick={() => setTaskScope('ALL_TASKS')}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all flex items-center gap-1.5 ${
                  taskScope === 'ALL_TASKS'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
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
              {['ALL', 'ITR', 'GST', 'TDS', 'AUDIT', 'COMPLIANCE', 'BILLING'].map((cat) => (
                <button
                  key={cat}
                  onClick={() => setCategoryFilter(cat)}
                  className={`px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${
                    categoryFilter === cat
                      ? 'bg-slate-900 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Main Table or Kanban */}
      {viewMode === 'list' || activeTab === 'WORKLIST' ? (
        <DataTable
          columns={columns}
          data={tasks}
          isLoading={isLoading}
          searchPlaceholder="Search worklist by client, task title, statutory obligation, doc request..."
        />
      ) : (
        <>
          {/* Mobile Kanban Status Selector Bar */}
          <div className="md:hidden space-y-3">
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
              {(
                [
                  { id: 'TODO', label: 'To Do' },
                  { id: 'IN_PROGRESS', label: 'In Progress' },
                  { id: 'UNDER_REVIEW', label: 'Under Review' },
                  { id: 'COMPLETED', label: 'Completed' },
                ] as const
              ).map((tab) => {
                const count = tasks.filter((t) => t.status === tab.id).length;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setMobileKanbanTab(tab.id)}
                    className={clsx(
                      'px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap flex items-center gap-1.5 shrink-0',
                      mobileKanbanTab === tab.id
                        ? 'bg-slate-900 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    )}
                  >
                    <span>{tab.label}</span>
                    <span
                      className={clsx(
                        'px-1.5 py-0.2 text-[10px] rounded-full font-bold',
                        mobileKanbanTab === tab.id
                          ? 'bg-white/20 text-white'
                          : 'bg-white text-slate-700 border border-slate-200'
                      )}
                    >
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Mobile Active Column Tasks */}
            <div className="bg-slate-100/80 border border-slate-200 rounded-xl p-3.5 space-y-3">
              {tasks.filter((t) => t.status === mobileKanbanTab).length === 0 ? (
                <div className="py-8 text-center text-slate-400 text-xs font-medium">
                  No tasks in {mobileKanbanTab.replace('_', ' ').toLowerCase()}
                </div>
              ) : (
                tasks
                  .filter((t) => t.status === mobileKanbanTab)
                  .map((task) => (
                    <div
                      key={task.id}
                      className="bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs space-y-2.5"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold text-slate-700 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded uppercase font-mono">
                          {task.category || 'TASK'}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono">{task.dueDate}</span>
                      </div>

                      <p className="text-xs font-bold text-slate-900 leading-snug">{task.title}</p>
                      {task.status === 'BLOCKED' && (
                        <p className="text-[10px] text-amber-700 font-semibold bg-amber-50 p-1 rounded border border-amber-200">
                          🛑 {task.blockedReason || 'Blocked'}
                        </p>
                      )}

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-600">
                        <span className="font-semibold truncate max-w-[140px]">{task.clientName || 'General'}</span>
                        <span className="font-medium text-emerald-700 truncate max-w-[120px]">
                          {task.assigneeName || 'Assigned'}
                        </span>
                      </div>

                      <div className="pt-2 flex items-center justify-between gap-1 border-t border-slate-100">
                        <button
                          onClick={() => handleOpenEditModal(task)}
                          className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-medium inline-flex items-center gap-1 border border-slate-200"
                        >
                          <Edit2 className="w-2.5 h-2.5" /> Reassign
                        </button>
                        <select
                          value={task.status}
                          onChange={(e) => handleUpdateStatus(task.id, e.target.value as TaskStatus)}
                          className="bg-slate-50 border border-slate-200 rounded text-[11px] px-2 py-1 font-semibold text-slate-700 cursor-pointer"
                        >
                          <option value="TODO">To Do</option>
                          <option value="IN_PROGRESS">In Progress</option>
                          <option value="UNDER_REVIEW">Under Review</option>
                          <option value="BLOCKED">Blocked</option>
                          <option value="COMPLETED">Completed</option>
                        </select>
                      </div>
                    </div>
                  ))
              )}
            </div>
          </div>

          {/* Desktop/Tablet 4-Column Grid */}
          <div className="hidden md:grid md:grid-cols-4 gap-4">
            {(['TODO', 'IN_PROGRESS', 'UNDER_REVIEW', 'COMPLETED'] as const).map((colStatus) => {
              const colTasks = tasks.filter((t) => t.status === colStatus);
              return (
                <div key={colStatus} className="bg-slate-100/80 border border-slate-200 rounded-xl p-4 flex flex-col min-h-[480px]">
                  <div className="flex items-center justify-between pb-3 border-b border-slate-200 mb-3">
                    <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                      {colStatus.replace('_', ' ')}
                    </span>
                    <span className="w-5 h-5 rounded-full bg-white border border-slate-200 text-slate-700 font-bold text-[10px] flex items-center justify-center">
                      {colTasks.length}
                    </span>
                  </div>

                  <div className="space-y-3 flex-1 overflow-y-auto">
                    {colTasks.map((task) => (
                      <div
                        key={task.id}
                        className="bg-white p-3.5 rounded-lg border border-slate-200 shadow-2xs hover:shadow-md transition-all space-y-2.5"
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-bold text-slate-700 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded uppercase font-mono">
                            {task.category || 'TASK'}
                          </span>
                          <span className="text-[10px] text-slate-500 font-mono">{task.dueDate}</span>
                        </div>

                        <p className="text-xs font-bold text-slate-900 leading-snug">{task.title}</p>
                        {task.status === 'BLOCKED' && (
                          <p className="text-[10px] text-amber-700 font-semibold bg-amber-50 p-1 rounded border border-amber-200">
                            🛑 {task.blockedReason || 'Blocked'}
                          </p>
                        )}

                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-600">
                          <span className="font-semibold truncate max-w-[120px]">{task.clientName || 'General'}</span>
                          <span className="font-medium text-emerald-700 truncate max-w-[100px]">
                            {task.assigneeName || 'Assigned'}
                          </span>
                        </div>

                        <div className="pt-2 flex items-center justify-between gap-1 border-t border-slate-100">
                          <button
                            onClick={() => handleOpenEditModal(task)}
                            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-[11px] font-medium inline-flex items-center gap-1 border border-slate-200"
                          >
                            <Edit2 className="w-2.5 h-2.5" /> Reassign
                          </button>
                          <select
                            value={task.status}
                            onChange={(e) => handleUpdateStatus(task.id, e.target.value as TaskStatus)}
                            className="bg-slate-50 border border-slate-200 rounded text-[11px] px-2 py-1 font-semibold text-slate-700 cursor-pointer"
                          >
                            <option value="TODO">To Do</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="UNDER_REVIEW">Under Review</option>
                            <option value="BLOCKED">Blocked</option>
                            <option value="COMPLETED">Completed</option>
                          </select>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}

      {/* =========================================================================
          TASK DETAIL MODAL (CLIENT -> COMPLIANCE -> TASK -> DOC REQUEST)
          ========================================================================= */}
      {selectedTask && (
        <Modal
          isOpen={isDetailModalOpen}
          onClose={() => {
            setIsDetailModalOpen(false);
            setSelectedTask(null);
          }}
          title={selectedTask.title}
          subtitle={`Task Ref: ${selectedTask.id.substring(0, 8)}`}
          maxWidth="lg"
        >
          <div className="space-y-4 text-xs">
            {/* Relationship Chain Banner */}
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 space-y-2">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Workflow Traceability Chain
              </span>
              <div className="flex items-center gap-2 flex-wrap text-slate-700 font-semibold">
                <span className="px-2 py-1 bg-white border border-slate-300 rounded shadow-2xs inline-flex items-center gap-1">
                  🏢 {selectedTask.clientName || 'General Practice'}
                </span>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                <span className="px-2 py-1 bg-indigo-50 border border-indigo-200 text-indigo-800 rounded shadow-2xs inline-flex items-center gap-1">
                  🏛️ {selectedTask.complianceTitle || selectedTask.category || 'Compliance'}
                </span>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                <span className="px-2 py-1 bg-brand-50 border border-brand-200 text-brand-800 rounded shadow-2xs inline-flex items-center gap-1">
                  ⚡ Task: {selectedTask.status}
                </span>
                {selectedTask.documentRequestNumber && (
                  <>
                    <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                    <span className="px-2 py-1 bg-blue-50 border border-blue-200 text-blue-800 rounded shadow-2xs inline-flex items-center gap-1">
                      📄 Req: {selectedTask.documentRequestNumber}
                    </span>
                  </>
                )}
              </div>
            </div>

            {/* Blocked Alert */}
            {selectedTask.status === 'BLOCKED' && (
              <div className="p-3 bg-amber-50 border border-amber-300 rounded-xl flex items-start gap-2.5">
                <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold text-amber-900">Task Currently Blocked</span>
                  <p className="text-amber-800 mt-0.5">{selectedTask.blockedReason || 'Awaiting client documents or responses.'}</p>
                </div>
              </div>
            )}

            {/* Details Grid */}
            <div className="grid grid-cols-2 gap-3">
              <div className="p-3 bg-white border border-slate-200 rounded-lg">
                <span className="text-[10px] text-slate-400 font-bold uppercase block">Internal Work Deadline</span>
                <span className="text-sm font-black text-slate-900 mt-1 block font-mono">{selectedTask.dueDate || 'None'}</span>
              </div>
              <div className="p-3 bg-white border border-slate-200 rounded-lg">
                <span className="text-[10px] text-slate-400 font-bold uppercase block">Statutory Due Date</span>
                <span className="text-sm font-black text-indigo-700 mt-1 block font-mono">
                  {selectedTask.statutoryDueDate || 'Standard internal task'}
                </span>
              </div>
            </div>

            {/* Document Request Checklist Progress */}
            {selectedTask.documentRequestId && (
              <div className="p-3.5 bg-blue-50/70 border border-blue-200 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-blue-900 flex items-center gap-1.5">
                    <FileText className="w-4 h-4 text-blue-600" /> Linked Document Request #{selectedTask.documentRequestNumber}
                  </span>
                  <Link
                    to="/documents/requests"
                    className="text-xs text-blue-700 hover:text-blue-900 font-bold underline inline-flex items-center gap-1"
                  >
                    Open in Document Center <ExternalLink className="w-3 h-3" />
                  </Link>
                </div>
                <div className="flex items-center gap-3 text-xs text-blue-800 font-medium">
                  <span>Total Items: {selectedTask.documentRequestItemsCount || 0}</span>
                  <span>•</span>
                  <span>Received: {selectedTask.documentRequestReceivedCount || 0}</span>
                  <span>•</span>
                  <span>Status: {selectedTask.documentRequestStatus || 'SENT'}</span>
                </div>
              </div>
            )}

            {/* Description */}
            {selectedTask.description && (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
                <span className="text-[10px] text-slate-400 font-bold uppercase block mb-1">Instructions / Description</span>
                <p className="text-slate-700 whitespace-pre-wrap leading-relaxed">{selectedTask.description}</p>
              </div>
            )}

            <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
              <Button
                variant="outline"
                onClick={() => {
                  setIsDetailModalOpen(false);
                  setSelectedTask(null);
                }}
              >
                Close
              </Button>
              <Button
                onClick={() => {
                  setIsDetailModalOpen(false);
                  handleOpenEditModal(selectedTask);
                }}
              >
                Edit & Reassign
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* =========================================================================
          BLOCK TASK REASON MODAL
          ========================================================================= */}
      <Modal
        isOpen={isBlockModalOpen}
        onClose={() => {
          setIsBlockModalOpen(false);
          setBlockingTask(null);
        }}
        title="Mark Task as Blocked"
        subtitle="Explain what documentation or information is pending from the client"
        maxWidth="md"
      >
        <form onSubmit={handleConfirmBlock} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Blocked Reason *</label>
            <textarea
              rows={3}
              required
              placeholder="e.g. Waiting for Form 16 Part B, 12-month ICICI bank statement PDF, and investment receipts from client"
              value={blockReasonInput}
              onChange={(e) => setBlockReasonInput(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-amber-500"
            />
          </div>
          <p className="text-[11px] text-slate-500">
            Marking this task as blocked will notify the client relationship manager and highlight the blockage on the Unified Worklist.
          </p>
          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button
              variant="outline"
              onClick={() => {
                setIsBlockModalOpen(false);
                setBlockingTask(null);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting} variant="danger">
              Confirm & Block Task
            </Button>
          </div>
        </form>
      </Modal>

      {/* =========================================================================
          CREATE TASK MODAL
          ========================================================================= */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Practice Task"
        subtitle="Assign statutory compliance, audit, or tax return tasks to practice employees"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Task Title *</label>
            <input
              type="text"
              required
              placeholder="e.g. Verify Form 26AS & Draft ITR-2 for Dr. Rajesh Sharma"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Related Client (Optional)</label>
              <select
                value={formData.clientId}
                onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-700">Assign to Staff / Admin</label>
                {myAssigneeId && (
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, assignedTo: myAssigneeId })}
                    className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                  >
                    ⚡ Assign to Me
                  </button>
                )}
              </div>
              <select
                value={formData.assignedTo}
                onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white font-medium text-slate-800"
              >
                <option value="">-- Unassigned / General Pool --</option>
                {assigneeOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.name} {opt.isMe ? '⭐ (You)' : ''} — {opt.designation} ({opt.department})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Task Category</label>
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
                <option value="URGENT">Urgent / Statutory Deadline</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Instructions (Optional)</label>
            <textarea
              rows={3}
              placeholder="Provide specific instructions, check points, or notes for the assigned employee..."
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
              Create & Assign Task
            </Button>
          </div>
        </form>
      </Modal>

      {/* =========================================================================
          EDIT & REASSIGN TASK MODAL
          ========================================================================= */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingTask(null);
        }}
        title="Edit & Reassign Task"
        subtitle="Update assignee, statutory deadline, priority, and workflow status"
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
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Related Client</label>
              <select
                value={editFormData.clientId}
                onChange={(e) => setEditFormData({ ...editFormData, clientId: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="">-- General Practice Task --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-700">Assigned Staff / Admin</label>
                {myAssigneeId && (
                  <button
                    type="button"
                    onClick={() => setEditFormData({ ...editFormData, assignedTo: myAssigneeId })}
                    className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                  >
                    ⚡ Assign to Me
                  </button>
                )}
              </div>
              <select
                value={editFormData.assignedTo}
                onChange={(e) => setEditFormData({ ...editFormData, assignedTo: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white font-medium text-slate-800"
              >
                <option value="">-- Unassigned --</option>
                {assigneeOptions.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {opt.name} {opt.isMe ? '⭐ (You)' : ''} — {opt.designation} ({opt.department})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
              <select
                value={editFormData.taskCategory}
                onChange={(e) => setEditFormData({ ...editFormData, taskCategory: e.target.value as any })}
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
                <option value="URGENT">Urgent / Statutory Deadline</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Workflow Status</label>
              <select
                value={editFormData.status}
                onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as any })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white font-semibold"
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="UNDER_REVIEW">Under Review</option>
                <option value="BLOCKED">🛑 Blocked on Docs</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Internal Due Date</label>
              <input
                type="date"
                value={editFormData.dueDate}
                onChange={(e) => setEditFormData({ ...editFormData, dueDate: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          {editFormData.status === 'BLOCKED' && (
            <div className="p-3 bg-amber-50 border border-amber-300 rounded-lg">
              <label className="block text-xs font-bold text-amber-900 mb-1">Blocked Reason *</label>
              <input
                type="text"
                placeholder="e.g. Waiting for Form 16 and AIS statement from client"
                value={editFormData.blockedReason}
                onChange={(e) => setEditFormData({ ...editFormData, blockedReason: e.target.value })}
                className="w-full text-xs px-3 py-2 border border-amber-300 rounded-lg bg-white"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Instructions</label>
            <textarea
              rows={3}
              placeholder="Provide specific instructions, check points, or notes for the assigned employee..."
              value={editFormData.description}
              onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
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
              Save & Update Assignment
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
